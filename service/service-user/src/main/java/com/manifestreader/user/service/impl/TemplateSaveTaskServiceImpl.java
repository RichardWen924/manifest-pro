package com.manifestreader.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.model.entity.BlParseTaskEntity;
import com.manifestreader.user.mapper.UserBillParseTaskMapper;
import com.manifestreader.user.messaging.TemplateSaveTaskMessage;
import com.manifestreader.user.messaging.TemplateSaveTaskPublisher;
import com.manifestreader.user.model.dto.TemplateExtractSaveRequest;
import com.manifestreader.user.model.vo.TemplateExtractSaveResultVO;
import com.manifestreader.user.model.vo.TemplateSaveTaskSubmitVO;
import com.manifestreader.user.model.vo.TemplateSaveTaskVO;
import com.manifestreader.user.service.TemplateSaveTaskService;
import com.manifestreader.user.support.UserRequestContext;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TemplateSaveTaskServiceImpl implements TemplateSaveTaskService {

    private static final DateTimeFormatter TASK_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String TASK_TYPE = "TEMPLATE_SAVE";

    private final UserBillParseTaskMapper taskMapper;
    private final TemplateSaveTaskPublisher taskPublisher;
    private final UserTemplateServiceImpl templateService;
    private final UserRequestContext userRequestContext;
    private final ObjectMapper objectMapper;

    public TemplateSaveTaskServiceImpl(
            UserBillParseTaskMapper taskMapper,
            TemplateSaveTaskPublisher taskPublisher,
            UserTemplateServiceImpl templateService,
            UserRequestContext userRequestContext,
            ObjectMapper objectMapper
    ) {
        this.taskMapper = taskMapper;
        this.taskPublisher = taskPublisher;
        this.templateService = templateService;
        this.userRequestContext = userRequestContext;
        this.objectMapper = objectMapper;
    }

    @Override
    public TemplateSaveTaskSubmitVO submitTask(TemplateExtractSaveRequest request) {
        Long companyId = userRequestContext.currentCompanyId();
        Long userId = userRequestContext.currentUserId();
        String traceId = userRequestContext.currentTraceId();
        String taskNo = generateTaskNo();

        BlParseTaskEntity entity = new BlParseTaskEntity();
        entity.setTaskNo(taskNo);
        entity.setCompanyId(companyId);
        entity.setTaskType(TASK_TYPE);
        entity.setTaskStatus("PENDING");
        entity.setEngineType("INTERNAL");
        entity.setFileName(limitText(request.fileName(), 255));
        entity.setFileHash(limitText(request.extractId(), 64));
        entity.setRequestPayload(writeJson(request));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        taskMapper.insert(entity);

        try {
            taskPublisher.publish(new TemplateSaveTaskMessage(taskNo, companyId, userId, traceId));
        } catch (RuntimeException ex) {
            markTaskFailed(entity, userId, "任务投递失败: " + ex.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "模板保存任务投递失败，请稍后重试");
        }
        return new TemplateSaveTaskSubmitVO(taskNo, "PENDING", "模板保存任务已提交");
    }

    @Override
    public TemplateSaveTaskVO getTask(String taskNo) {
        return toTaskVO(findTaskEntity(taskNo));
    }

    @Override
    public void processTask(TemplateSaveTaskMessage message) {
        BlParseTaskEntity entity = findTaskEntity(message.taskNo());
        entity.setTaskStatus("RUNNING");
        entity.setStartedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(message.userId());
        taskMapper.updateById(entity);

        try {
            TemplateExtractSaveRequest request = objectMapper.readValue(entity.getRequestPayload(), TemplateExtractSaveRequest.class);
            TemplateExtractSaveResultVO result = templateService.saveGeneratedTemplate(request, message.companyId(), message.userId());
            entity.setTaskStatus("SUCCESS");
            entity.setResultPayload(writeJson(result));
            entity.setErrorMessage(null);
            entity.setFinishedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setUpdatedBy(message.userId());
            taskMapper.updateById(entity);
        } catch (Exception ex) {
            markTaskFailed(entity, message.userId(), ex.getMessage());
        }
    }

    private TemplateSaveTaskVO toTaskVO(BlParseTaskEntity entity) {
        TemplateExtractSaveResultVO result = null;
        if (StringUtils.hasText(entity.getResultPayload())) {
            try {
                result = objectMapper.readValue(entity.getResultPayload(), TemplateExtractSaveResultVO.class);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Read template save task payload failed", ex);
            }
        }
        return new TemplateSaveTaskVO(
                entity.getTaskNo(),
                entity.getTaskStatus(),
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
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "模板保存任务不存在");
        }
        return entity;
    }

    private void markTaskFailed(BlParseTaskEntity entity, Long userId, String errorMessage) {
        entity.setTaskStatus("FAILED");
        entity.setErrorMessage(limitText(safeText(errorMessage, "模板保存任务执行失败"), 1000));
        entity.setFinishedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(userId);
        taskMapper.updateById(entity);
    }

    private String generateTaskNo() {
        return "TPL-SAVE-" + TASK_NO_TIME.format(LocalDateTime.now()) + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Write template save task payload failed", ex);
        }
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String limitText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
