package com.manifestreader.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.model.entity.BlParseTaskEntity;
import com.manifestreader.model.entity.FileAssetEntity;
import com.manifestreader.user.mapper.UserBillParseTaskMapper;
import com.manifestreader.user.mapper.UserFileAssetMapper;
import com.manifestreader.user.messaging.TemplateExportTaskMessage;
import com.manifestreader.user.messaging.TemplateExportTaskPublisher;
import com.manifestreader.user.model.vo.ExportedTemplateFile;
import com.manifestreader.user.model.vo.TemplateExportResultVO;
import com.manifestreader.user.model.vo.TemplateExportTaskSubmitVO;
import com.manifestreader.user.model.vo.TemplateExportTaskVO;
import com.manifestreader.user.service.TemplateExportTaskService;
import com.manifestreader.user.storage.ObjectStorageService;
import com.manifestreader.user.storage.StoredObject;
import com.manifestreader.user.support.PathMultipartFile;
import com.manifestreader.user.support.UserRequestContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TemplateExportTaskServiceImpl implements TemplateExportTaskService {

    private static final DateTimeFormatter TASK_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String TASK_TYPE = "TEMPLATE_EXPORT";

    private final UserBillParseTaskMapper taskMapper;
    private final UserFileAssetMapper fileAssetMapper;
    private final TemplateExportTaskPublisher taskPublisher;
    private final UserTemplateServiceImpl templateService;
    private final UserRequestContext userRequestContext;
    private final ObjectMapper objectMapper;
    private final ObjectStorageService objectStorageService;

    public TemplateExportTaskServiceImpl(
            UserBillParseTaskMapper taskMapper,
            UserFileAssetMapper fileAssetMapper,
            TemplateExportTaskPublisher taskPublisher,
            UserTemplateServiceImpl templateService,
            UserRequestContext userRequestContext,
            ObjectMapper objectMapper,
            ObjectStorageService objectStorageService
    ) {
        this.taskMapper = taskMapper;
        this.fileAssetMapper = fileAssetMapper;
        this.taskPublisher = taskPublisher;
        this.templateService = templateService;
        this.userRequestContext = userRequestContext;
        this.objectMapper = objectMapper;
        this.objectStorageService = objectStorageService;
    }

    @Override
    public TemplateExportTaskSubmitVO submitTask(Long templateId, String outputFormat, MultipartFile file) {
        if (templateId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "请选择模板");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "请上传需要提取数据的目标文件");
        }
        Long companyId = userRequestContext.currentCompanyId();
        Long userId = userRequestContext.currentUserId();
        String traceId = userRequestContext.currentTraceId();
        String normalizedFormat = normalizeOutputFormat(outputFormat);
        String taskNo = generateTaskNo();
        String fileName = safeFileName(file.getOriginalFilename());
        FileAssetEntity sourceFile = persistSourceFile(file, companyId, userId);

        BlParseTaskEntity entity = new BlParseTaskEntity();
        entity.setTaskNo(taskNo);
        entity.setCompanyId(companyId);
        entity.setSourceFileId(sourceFile.getId());
        entity.setTaskType(TASK_TYPE);
        entity.setTaskStatus("PENDING");
        entity.setEngineType("DIFY");
        entity.setBizId(templateId);
        entity.setOutputFormat(normalizedFormat);
        entity.setFileName(fileName);
        entity.setFileHash(hashFile(file));
        entity.setRequestPayload(writeJson(new TaskRequestPayload(sourceFile.getId(), templateId, normalizedFormat, fileName, traceId)));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        taskMapper.insert(entity);

        try {
            taskPublisher.publish(new TemplateExportTaskMessage(
                    taskNo,
                    sourceFile.getId(),
                    templateId,
                    normalizedFormat,
                    companyId,
                    userId,
                    traceId
            ));
        } catch (RuntimeException ex) {
            markTaskFailed(entity, userId, "任务投递失败: " + ex.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "模板导出任务投递失败，请稍后重试");
        }
        return new TemplateExportTaskSubmitVO(taskNo, "PENDING", "模板导出任务已提交");
    }

    @Override
    public TemplateExportTaskVO getTask(String taskNo) {
        BlParseTaskEntity entity = findTaskEntity(taskNo);
        return toTaskVO(entity);
    }

    @Override
    public ExportedTemplateFile getExportedFile(String taskNo) {
        BlParseTaskEntity entity = findTaskEntity(taskNo);
        if (!"SUCCESS".equals(entity.getTaskStatus()) || entity.getResultFileId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "导出任务尚未完成");
        }
        FileAssetEntity fileAsset = fileAssetMapper.selectById(entity.getResultFileId());
        if (fileAsset == null || !StringUtils.hasText(fileAsset.getObjectKey())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "导出文件不存在或已过期");
        }
        Path path = objectStorageService.downloadToTemp(fileAsset.getBucketName(), fileAsset.getObjectKey(), fileAsset.getFileName());
        return new ExportedTemplateFile(fileAsset.getFileName(), fileAsset.getContentType(), path);
    }

    @Override
    public void processTask(TemplateExportTaskMessage message) {
        BlParseTaskEntity entity = findTaskEntity(message.taskNo());
        entity.setTaskStatus("RUNNING");
        entity.setStartedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(message.userId());
        taskMapper.updateById(entity);

        FileAssetEntity sourceFile = findSourceFile(message.sourceFileId(), message.companyId());
        Path tempFile = objectStorageService.downloadToTemp(sourceFile.getBucketName(), sourceFile.getObjectKey(), sourceFile.getFileName());
        try {
            MultipartFile file = new PathMultipartFile(tempFile, sourceFile.getFileName(), sourceFile.getContentType());
            UserTemplateServiceImpl.TemplateExportExecutionResult executionResult =
                    templateService.executeTemplateExport(message.templateId(), message.outputFormat(), file, message.companyId(), message.userId());
            entity.setTaskStatus("SUCCESS");
            entity.setResultPayload(writeJson(executionResult.result()));
            entity.setResultFileId(executionResult.fileAssetId());
            entity.setErrorMessage(null);
            entity.setFinishedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setUpdatedBy(message.userId());
            taskMapper.updateById(entity);
        } catch (Exception ex) {
            markTaskFailed(entity, message.userId(), ex.getMessage());
        } finally {
            deleteQuietly(tempFile);
        }
    }

    private BlParseTaskEntity findTaskEntity(String taskNo) {
        BlParseTaskEntity entity = taskMapper.selectOne(new LambdaQueryWrapper<BlParseTaskEntity>()
                .eq(BlParseTaskEntity::getTaskNo, taskNo)
                .eq(BlParseTaskEntity::getTaskType, TASK_TYPE)
                .last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "导出任务不存在");
        }
        return entity;
    }

    private FileAssetEntity findSourceFile(Long sourceFileId, Long companyId) {
        FileAssetEntity fileAssetEntity = fileAssetMapper.selectOne(new LambdaQueryWrapper<FileAssetEntity>()
                .eq(FileAssetEntity::getId, sourceFileId)
                .eq(FileAssetEntity::getCompanyId, companyId)
                .eq(FileAssetEntity::getDeleted, 0)
                .last("LIMIT 1"));
        if (fileAssetEntity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "导出源文件不存在");
        }
        return fileAssetEntity;
    }

    private TemplateExportTaskVO toTaskVO(BlParseTaskEntity entity) {
        TemplateExportResultVO result = null;
        if (StringUtils.hasText(entity.getResultPayload())) {
            try {
                result = objectMapper.readValue(entity.getResultPayload(), TemplateExportResultVO.class);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Read export task payload failed", ex);
            }
        }
        return new TemplateExportTaskVO(
                entity.getTaskNo(),
                entity.getTaskStatus(),
                entity.getBizId(),
                entity.getOutputFormat(),
                entity.getErrorMessage(),
                result,
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getFinishedAt()
        );
    }

    private FileAssetEntity persistSourceFile(MultipartFile file, Long companyId, Long userId) {
        try {
            Path tempFile = Files.createTempFile("template-export-source-", ".upload");
            file.transferTo(tempFile);
            StoredObject stored = objectStorageService.put(
                    "template-export/source/" + companyId + "/" + System.currentTimeMillis() + "-" + safeFileName(file.getOriginalFilename()),
                    tempFile,
                    safeContentType(file.getContentType())
            );
            FileAssetEntity entity = new FileAssetEntity();
            entity.setCompanyId(companyId);
            entity.setBizType("TEMPLATE_EXPORT_SOURCE");
            entity.setFileName(limitText(safeFileName(file.getOriginalFilename()), 255));
            entity.setOriginalName(limitText(safeFileName(file.getOriginalFilename()), 255));
            entity.setContentType(safeContentType(file.getContentType()));
            entity.setFileSize(stored.size());
            entity.setStorageType(stored.storageType());
            entity.setBucketName(stored.bucketName());
            entity.setObjectKey(limitText(stored.objectKey(), 255));
            entity.setFileHash(hashFile(file));
            entity.setStatus(1);
            entity.setCreatedBy(userId);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setDeleted(0);
            fileAssetMapper.insert(entity);
            deleteQuietly(tempFile);
            return entity;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "导出源文件持久化失败");
        }
    }

    private void markTaskFailed(BlParseTaskEntity entity, Long userId, String errorMessage) {
        entity.setTaskStatus("FAILED");
        entity.setErrorMessage(limitText(safeText(errorMessage, "导出任务执行失败"), 1000));
        entity.setFinishedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(userId);
        taskMapper.updateById(entity);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Write export task payload failed", ex);
        }
    }

    private String generateTaskNo() {
        return "TPL-EXPORT-" + TASK_NO_TIME.format(LocalDateTime.now()) + "-" + Integer.toHexString((int) (Math.random() * 65535));
    }

    private String safeFileName(String fileName) {
        return safeText(fileName, "template-export-file");
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String limitText(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String safeContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType : "application/octet-stream";
    }

    private String normalizeOutputFormat(String outputFormat) {
        String value = StringUtils.hasText(outputFormat) ? outputFormat.trim().toUpperCase() : "DOCX";
        if (!"DOCX".equals(value) && !"PDF".equals(value)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "导出格式仅支持 DOCX 或 PDF");
        }
        return value;
    }

    private String hashFile(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(file.getBytes()));
        } catch (NoSuchAlgorithmException | IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "文件指纹生成失败");
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
            Path parent = path.getParent();
            if (parent != null) {
                Files.deleteIfExists(parent);
            }
        } catch (IOException ignored) {
        }
    }

    private record TaskRequestPayload(
            Long sourceFileId,
            Long templateId,
            String outputFormat,
            String fileName,
            String traceId
    ) {
    }
}
