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
import com.manifestreader.user.messaging.TemplateExtractTaskMessage;
import com.manifestreader.user.messaging.TemplateExtractTaskPublisher;
import com.manifestreader.user.model.vo.BlankTemplateFile;
import com.manifestreader.user.model.vo.TemplateExtractResultVO;
import com.manifestreader.user.model.vo.TemplateExtractTaskSubmitVO;
import com.manifestreader.user.model.vo.TemplateExtractTaskVO;
import com.manifestreader.user.service.TemplateExtractTaskService;
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
public class TemplateExtractTaskServiceImpl implements TemplateExtractTaskService {

    private static final DateTimeFormatter TASK_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String TASK_TYPE = "TEMPLATE_EXTRACT";

    private final UserBillParseTaskMapper taskMapper;
    private final UserFileAssetMapper fileAssetMapper;
    private final TemplateExtractTaskPublisher taskPublisher;
    private final UserTemplateServiceImpl templateService;
    private final UserRequestContext userRequestContext;
    private final ObjectMapper objectMapper;
    private final ObjectStorageService objectStorageService;

    public TemplateExtractTaskServiceImpl(
            UserBillParseTaskMapper taskMapper,
            UserFileAssetMapper fileAssetMapper,
            TemplateExtractTaskPublisher taskPublisher,
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
    public TemplateExtractTaskSubmitVO submitTask(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "请上传模板样本文件");
        }
        Long companyId = userRequestContext.currentCompanyId();
        Long userId = userRequestContext.currentUserId();
        String traceId = userRequestContext.currentTraceId();
        String taskNo = generateTaskNo();
        String fileName = safeFileName(file.getOriginalFilename());
        String fileHash = hashFile(file);
        FileAssetEntity sourceFile = persistSourceFile(file, companyId, userId, fileHash);

        BlParseTaskEntity entity = new BlParseTaskEntity();
        entity.setTaskNo(taskNo);
        entity.setCompanyId(companyId);
        entity.setSourceFileId(sourceFile.getId());
        entity.setTaskType(TASK_TYPE);
        entity.setTaskStatus("PENDING");
        entity.setEngineType("DIFY");
        entity.setRequestPayload(writeJson(new TaskRequestPayload(sourceFile.getId(), fileHash, fileName, traceId)));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setFileName(fileName);
        entity.setFileHash(fileHash);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        taskMapper.insert(entity);

        try {
            taskPublisher.publish(new TemplateExtractTaskMessage(
                    taskNo,
                    sourceFile.getId(),
                    fileName,
                    safeContentType(file.getContentType()),
                    fileHash,
                    companyId,
                    userId,
                    traceId
            ));
        } catch (RuntimeException ex) {
            markTaskFailed(entity, userId, "任务投递失败: " + ex.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "模板提取任务投递失败，请稍后重试");
        }
        return new TemplateExtractTaskSubmitVO(taskNo, "PENDING", "模板提取任务已提交");
    }

    @Override
    public TemplateExtractTaskVO getTask(String taskNo) {
        return toTaskVO(findTaskEntity(taskNo));
    }

    @Override
    public BlankTemplateFile getBlankTemplate(String taskNo) {
        return templateService.getBlankTemplate(taskNo);
    }

    @Override
    public BlankTemplateFile getBlankTemplatePreview(String taskNo) {
        return templateService.getBlankTemplatePreview(taskNo);
    }

    @Override
    public void processTask(TemplateExtractTaskMessage message) {
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
            UserTemplateServiceImpl.TemplateExtractExecutionResult executionResult =
                    templateService.executeTemplateExtraction(
                            file,
                            message.companyId(),
                            message.userId(),
                            message.fileHash(),
                            "/user/templates/extract/tasks/" + message.taskNo() + "/blank-template",
                            "/user/templates/extract/tasks/" + message.taskNo() + "/preview"
                    );

            entity.setTaskStatus("SUCCESS");
            entity.setResultFileId(executionResult.blankTemplateFileId());
            entity.setResultPayload(writeJson(new TaskResultPayload(executionResult.result(), executionResult.previewFileId())));
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

    private TemplateExtractTaskVO toTaskVO(BlParseTaskEntity entity) {
        TemplateExtractResultVO result = null;
        if (StringUtils.hasText(entity.getResultPayload())) {
            try {
                result = objectMapper.readValue(entity.getResultPayload(), TaskResultPayload.class).result();
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Read template extract task payload failed", ex);
            }
        }
        return new TemplateExtractTaskVO(
                entity.getTaskNo(),
                entity.getTaskStatus(),
                entity.getFileName(),
                entity.getErrorMessage(),
                result,
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getFinishedAt()
        );
    }

    private BlParseTaskEntity findTaskEntity(String taskNo) {
        BlParseTaskEntity entity = taskMapper.selectOne(new LambdaQueryWrapper<BlParseTaskEntity>()
                .eq(BlParseTaskEntity::getTaskNo, taskNo)
                .eq(BlParseTaskEntity::getTaskType, TASK_TYPE)
                .last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "模板提取任务不存在");
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
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "模板提取源文件不存在");
        }
        return fileAssetEntity;
    }

    private FileAssetEntity persistSourceFile(MultipartFile file, Long companyId, Long userId, String fileHash) {
        try {
            Path tempFile = Files.createTempFile("template-extract-source-", ".upload");
            file.transferTo(tempFile);
            StoredObject stored = objectStorageService.put(
                    "template-extract/source/" + companyId + "/" + System.currentTimeMillis() + "-" + safeFileName(file.getOriginalFilename()),
                    tempFile,
                    safeContentType(file.getContentType())
            );
            FileAssetEntity entity = new FileAssetEntity();
            entity.setCompanyId(companyId);
            entity.setBizType("TEMPLATE_EXTRACT_SOURCE");
            entity.setFileName(limitText(safeFileName(file.getOriginalFilename()), 255));
            entity.setOriginalName(limitText(safeFileName(file.getOriginalFilename()), 255));
            entity.setContentType(safeContentType(file.getContentType()));
            entity.setFileSize(stored.size());
            entity.setStorageType(stored.storageType());
            entity.setBucketName(stored.bucketName());
            entity.setObjectKey(limitText(stored.objectKey(), 255));
            entity.setFileHash(fileHash);
            entity.setStatus(1);
            entity.setCreatedBy(userId);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setDeleted(0);
            fileAssetMapper.insert(entity);
            deleteQuietly(tempFile);
            return entity;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "模板提取源文件持久化失败");
        }
    }

    private void markTaskFailed(BlParseTaskEntity entity, Long userId, String errorMessage) {
        entity.setTaskStatus("FAILED");
        entity.setErrorMessage(limitText(safeText(errorMessage, "模板提取任务执行失败"), 1000));
        entity.setFinishedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(userId);
        taskMapper.updateById(entity);
    }

    private String generateTaskNo() {
        return "TPL-EXTRACT-" + TASK_NO_TIME.format(LocalDateTime.now()) + "-" + Integer.toHexString((int) (Math.random() * 65535));
    }

    private String safeFileName(String fileName) {
        return safeText(fileName, "template-extract-file");
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String safeContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType : "application/octet-stream";
    }

    private String limitText(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String hashFile(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(file.getBytes()));
        } catch (NoSuchAlgorithmException | IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "模板样本文件指纹生成失败");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Write template extract task payload failed", ex);
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
            String fileHash,
            String fileName,
            String traceId
    ) {
    }

    private record TaskResultPayload(
            TemplateExtractResultVO result,
            Long previewFileId
    ) {
    }
}
