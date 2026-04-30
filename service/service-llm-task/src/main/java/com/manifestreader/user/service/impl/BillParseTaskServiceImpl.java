package com.manifestreader.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.model.entity.BlParseTaskEntity;
import com.manifestreader.model.entity.FileAssetEntity;
import com.manifestreader.user.cache.BillParseTaskCacheService;
import com.manifestreader.user.dify.DifyTemplateExportParser;
import com.manifestreader.user.dify.DifyWorkflowClient;
import com.manifestreader.user.mapper.UserBillParseTaskMapper;
import com.manifestreader.user.mapper.UserFileAssetMapper;
import com.manifestreader.user.messaging.BillParseTaskMessage;
import com.manifestreader.user.messaging.BillParseTaskPublisher;
import com.manifestreader.user.model.vo.BillExtractResultVO;
import com.manifestreader.user.model.vo.BillExtractTaskSubmitVO;
import com.manifestreader.user.model.vo.BillExtractTaskVO;
import com.manifestreader.user.service.BillParseTaskService;
import com.manifestreader.user.support.PathMultipartFile;
import com.manifestreader.user.support.UserRequestContext;
import com.manifestreader.user.storage.ObjectStorageService;
import com.manifestreader.user.storage.StoredObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BillParseTaskServiceImpl implements BillParseTaskService {

    private static final DateTimeFormatter TASK_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final UserBillParseTaskMapper taskMapper;
    private final UserFileAssetMapper fileAssetMapper;
    private final DifyWorkflowClient difyWorkflowClient;
    private final DifyTemplateExportParser exportParser;
    private final BillParseTaskPublisher taskPublisher;
    private final BillParseTaskCacheService cacheService;
    private final UserRequestContext userRequestContext;
    private final ObjectMapper objectMapper;
    private final ObjectStorageService objectStorageService;

    public BillParseTaskServiceImpl(
            UserBillParseTaskMapper taskMapper,
            UserFileAssetMapper fileAssetMapper,
            DifyWorkflowClient difyWorkflowClient,
            DifyTemplateExportParser exportParser,
            BillParseTaskPublisher taskPublisher,
            BillParseTaskCacheService cacheService,
            UserRequestContext userRequestContext,
            ObjectMapper objectMapper,
            ObjectStorageService objectStorageService
    ) {
        this.taskMapper = taskMapper;
        this.fileAssetMapper = fileAssetMapper;
        this.difyWorkflowClient = difyWorkflowClient;
        this.exportParser = exportParser;
        this.taskPublisher = taskPublisher;
        this.cacheService = cacheService;
        this.userRequestContext = userRequestContext;
        this.objectMapper = objectMapper;
        this.objectStorageService = objectStorageService;
    }

    @Override
    public BillExtractTaskSubmitVO submitExtractTask(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "请上传需要提取的提单文件");
        }
        String fileHash = hashFile(file);
        Long companyId = userRequestContext.currentCompanyId();
        Optional<String> existingTaskNo = cacheService.findTaskNoByFileHash(companyId, fileHash);
        if (existingTaskNo.isPresent()) {
            BillExtractTaskVO existingTask = getTask(existingTaskNo.get());
            if (!"FAILED".equals(existingTask.status())) {
                return new BillExtractTaskSubmitVO(existingTask.taskNo(), existingTask.status(), "已复用同文件的解析任务");
            }
        }

        String taskNo = generateTaskNo();
        String fileName = safeFileName(file.getOriginalFilename());
        LocalDateTime now = LocalDateTime.now();
        FileAssetEntity sourceFile = persistSourceFile(file, fileHash);

        BlParseTaskEntity entity = new BlParseTaskEntity();
        entity.setTaskNo(taskNo);
        entity.setCompanyId(companyId);
        entity.setSourceFileId(sourceFile.getId());
        entity.setTaskStatus("PENDING");
        entity.setEngineType("DIFY");
        entity.setRequestPayload(writeJson(new TaskRequestPayload(sourceFile.getId(), fileHash, fileName, userRequestContext.currentTraceId())));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setFileName(fileName);
        entity.setFileHash(fileHash);
        entity.setCreatedBy(userRequestContext.currentUserId());
        entity.setUpdatedBy(userRequestContext.currentUserId());
        taskMapper.insert(entity);

        BillExtractTaskVO snapshot = new BillExtractTaskVO(taskNo, "PENDING", fileName, "解析任务已提交，等待消费", null, null, now, null, null);
        cacheService.bindFileHash(companyId, fileHash, taskNo);
        cacheService.cacheTask(snapshot);
        try {
            taskPublisher.publish(new BillParseTaskMessage(
                    taskNo,
                    sourceFile.getId(),
                    fileName,
                    safeContentType(file.getContentType()),
                    fileHash,
                    companyId,
                    userRequestContext.currentUserId(),
                    userRequestContext.currentTraceId()
            ));
        } catch (RuntimeException ex) {
            markTaskFailed(entity, userRequestContext.currentUserId(), "任务投递失败: " + ex.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "提单解析任务投递失败，请稍后重试");
        }
        return new BillExtractTaskSubmitVO(taskNo, "PENDING", "解析任务已提交");
    }

    @Override
    public BillExtractTaskVO getTask(String taskNo) {
        Optional<BillExtractTaskVO> cached = cacheService.getTask(taskNo);
        if (cached.isPresent()) {
            return cached.get();
        }
        BlParseTaskEntity entity = findTaskEntity(taskNo);
        BillExtractTaskVO task = toTaskVO(entity);
        cacheService.cacheTask(task);
        return task;
    }

    @Override
    public BillExtractResultVO resolveResult(String taskNo) {
        BillExtractTaskVO task = getTask(taskNo);
        return task.result();
    }

    @Override
    public void processTask(BillParseTaskMessage message) {
        BlParseTaskEntity entity = findTaskEntity(message.taskNo());
        entity.setTaskStatus("RUNNING");
        entity.setStartedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(message.userId());
        taskMapper.updateById(entity);
        cacheService.cacheTask(new BillExtractTaskVO(
                entity.getTaskNo(),
                entity.getTaskStatus(),
                entity.getFileName(),
                "解析进行中",
                null,
                null,
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getFinishedAt()
        ));

        TaskRequestPayload payload = readRequestPayload(entity.getRequestPayload());
        FileAssetEntity sourceFile = findSourceFile(message.sourceFileId(), message.companyId());
        Path tempFile = objectStorageService.downloadToTemp(sourceFile.getBucketName(), sourceFile.getObjectKey(), sourceFile.getFileName());
        try {
            MultipartFile file = new PathMultipartFile(tempFile, message.fileName(), sourceFile.getContentType());
            String difyResponse = difyWorkflowClient.runBillExtraction(file);
            DifyTemplateExportParser.ParsedExportFields parsed = exportParser.parse(difyResponse);
            BillExtractResultVO result = toResult(message.fileName(), parsed);

            entity.setTaskStatus("SUCCESS");
            entity.setResultPayload(writeJson(result));
            entity.setErrorMessage(null);
            entity.setFinishedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setUpdatedBy(message.userId());
            taskMapper.updateById(entity);
            cacheService.cacheTask(new BillExtractTaskVO(
                    entity.getTaskNo(),
                    entity.getTaskStatus(),
                    entity.getFileName(),
                    "解析完成",
                    null,
                    result,
                    entity.getCreatedAt(),
                    entity.getStartedAt(),
                    entity.getFinishedAt()
            ));
        } catch (Exception ex) {
            markTaskFailed(entity, message.userId(), ex.getMessage());
        } finally {
            deleteQuietly(tempFile);
        }
    }

    private BlParseTaskEntity findTaskEntity(String taskNo) {
        BlParseTaskEntity entity = taskMapper.selectOne(new LambdaQueryWrapper<BlParseTaskEntity>()
                .eq(BlParseTaskEntity::getTaskNo, taskNo)
                .last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "解析任务不存在");
        }
        return entity;
    }

    private BillExtractTaskVO toTaskVO(BlParseTaskEntity entity) {
        BillExtractResultVO result = null;
        if (StringUtils.hasText(entity.getResultPayload())) {
            try {
                result = objectMapper.readValue(entity.getResultPayload(), BillExtractResultVO.class);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Read task result payload failed", ex);
            }
        }
        String message = switch (entity.getTaskStatus()) {
            case "SUCCESS" -> "解析完成";
            case "FAILED" -> "解析失败";
            default -> "解析进行中";
        };
        return new BillExtractTaskVO(
                entity.getTaskNo(),
                entity.getTaskStatus(),
                entity.getFileName(),
                message,
                entity.getErrorMessage(),
                result,
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getFinishedAt()
        );
    }

    private BillExtractResultVO toResult(String fileName, DifyTemplateExportParser.ParsedExportFields parsed) {
        Map<String, Object> fields = parsed.fields();
        List<com.manifestreader.user.model.vo.BillExtractFieldVO> fieldList = fields.entrySet().stream()
                .map(entry -> new com.manifestreader.user.model.vo.BillExtractFieldVO(entry.getKey(), entry.getValue()))
                .toList();
        return new BillExtractResultVO(
                UUID.randomUUID().toString(),
                fileName,
                fields.size(),
                fields,
                fieldList,
                parsed.rawText(),
                "SUCCESS",
                "提单解析完成"
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Serialize task payload failed", ex);
        }
    }

    private TaskRequestPayload readRequestPayload(String payload) {
        try {
            return objectMapper.readValue(payload, TaskRequestPayload.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Read task request payload failed", ex);
        }
    }

    private FileAssetEntity persistSourceFile(MultipartFile file, String fileHash) {
        try {
            String fileName = safeFileName(file.getOriginalFilename());
            Path tempFile = Files.createTempFile("bill-source-", "-" + fileName);
            file.transferTo(tempFile);
            try {
                String objectKey = buildObjectKey(fileName);
                StoredObject storedObject = objectStorageService.put(objectKey, tempFile, safeContentType(file.getContentType()));
                FileAssetEntity entity = new FileAssetEntity();
                entity.setCompanyId(userRequestContext.currentCompanyId());
                entity.setBizType("BILL_PARSE_SOURCE");
                entity.setFileName(fileName);
                entity.setOriginalName(fileName);
                entity.setContentType(safeContentType(file.getContentType()));
                entity.setFileSize(storedObject.size());
                entity.setStorageType(storedObject.storageType());
                entity.setBucketName(storedObject.bucketName());
                entity.setObjectKey(storedObject.objectKey());
                entity.setFileHash(fileHash);
                entity.setStatus(1);
                entity.setCreatedBy(userRequestContext.currentUserId());
                entity.setCreatedAt(LocalDateTime.now());
                entity.setUpdatedAt(LocalDateTime.now());
                entity.setDeleted(0);
                fileAssetMapper.insert(entity);
                return entity;
            } finally {
                deleteQuietly(tempFile);
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "保存待解析源文件失败");
        }
    }

    private String hashFile(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(file.getBytes());
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "计算文件摘要失败");
        }
    }

    private String generateTaskNo() {
        return "PARSE-" + LocalDateTime.now().format(TASK_NO_TIME) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String safeFileName(String originalFilename) {
        return StringUtils.hasText(originalFilename) ? Path.of(originalFilename).getFileName().toString() : "bill-upload.bin";
    }

    private String safeContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType : "application/octet-stream";
    }

    private String buildObjectKey(String fileName) {
        return limitText("bill-parse-source/" + userRequestContext.currentCompanyId() + "/"
                + LocalDateTime.now().format(TASK_NO_TIME) + "/" + UUID.randomUUID() + "/" + fileName, 240);
    }

    private String valueToString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String limitText(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private FileAssetEntity findSourceFile(Long sourceFileId, Long companyId) {
        FileAssetEntity entity = fileAssetMapper.selectOne(new LambdaQueryWrapper<FileAssetEntity>()
                .eq(FileAssetEntity::getId, sourceFileId)
                .eq(FileAssetEntity::getCompanyId, companyId)
                .eq(FileAssetEntity::getDeleted, 0)
                .last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "任务源文件不存在");
        }
        return entity;
    }

    private void markTaskFailed(BlParseTaskEntity entity, Long userId, String errorMessage) {
        entity.setTaskStatus("FAILED");
        entity.setErrorMessage(limitText(errorMessage, 500));
        entity.setFinishedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(userId);
        taskMapper.updateById(entity);
        cacheService.cacheTask(new BillExtractTaskVO(
                entity.getTaskNo(),
                entity.getTaskStatus(),
                entity.getFileName(),
                "解析失败",
                entity.getErrorMessage(),
                null,
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getFinishedAt()
        ));
    }

    private record TaskRequestPayload(
            Long sourceFileId,
            String fileHash,
            String fileName,
            String traceId
    ) {
    }
}
