package com.manifestreader.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manifestreader.model.entity.BlParseTaskEntity;
import com.manifestreader.user.mapper.UserBillParseTaskMapper;
import com.manifestreader.user.messaging.TemplateSaveTaskMessage;
import com.manifestreader.user.messaging.TemplateSaveTaskPublisher;
import com.manifestreader.user.model.dto.TemplateExtractSaveRequest;
import com.manifestreader.user.model.dto.TemplateFieldMappingSaveRequest;
import com.manifestreader.user.model.vo.TemplateExtractSaveResultVO;
import com.manifestreader.user.model.vo.TemplateSaveTaskSubmitVO;
import com.manifestreader.user.support.UserRequestContext;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplateSaveTaskServiceImplTest {

    @Mock
    private UserBillParseTaskMapper taskMapper;

    @Mock
    private TemplateSaveTaskPublisher taskPublisher;

    @Mock
    private UserTemplateServiceImpl templateService;

    @Mock
    private UserRequestContext userRequestContext;

    @InjectMocks
    private TemplateSaveTaskServiceImpl service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void submitSaveTaskCreatesPendingTaskAndPublishesMessage() {
        TemplateExtractSaveRequest request = previewRequest();
        when(userRequestContext.currentCompanyId()).thenReturn(2L);
        when(userRequestContext.currentUserId()).thenReturn(3L);
        when(userRequestContext.currentTraceId()).thenReturn("trace-save-001");

        service = new TemplateSaveTaskServiceImpl(taskMapper, taskPublisher, templateService, userRequestContext, objectMapper);

        TemplateSaveTaskSubmitVO response = service.submitTask(request);

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.taskNo()).isNotBlank();
        verify(taskMapper).insert(argThat((BlParseTaskEntity task) ->
                "TEMPLATE_SAVE".equals(task.getTaskType())
                        && "PENDING".equals(task.getTaskStatus())
                        && "preview-source.xlsx".equals(task.getFileName())
                        && "preview-extract-id".equals(task.getFileHash())
                        && task.getRequestPayload().contains("Preview Template")));
        verify(taskPublisher).publish(argThat(message ->
                response.taskNo().equals(message.taskNo())
                        && Long.valueOf(2L).equals(message.companyId())
                        && Long.valueOf(3L).equals(message.userId())));
    }

    @Test
    void processSaveTaskMarksSuccessAndStoresResultPayload() {
        BlParseTaskEntity taskEntity = new BlParseTaskEntity();
        taskEntity.setId(9001L);
        taskEntity.setTaskNo("TPL-SAVE-001");
        taskEntity.setCompanyId(2L);
        taskEntity.setTaskType("TEMPLATE_SAVE");
        taskEntity.setTaskStatus("PENDING");
        taskEntity.setRequestPayload(writeJson(previewRequest()));
        taskEntity.setCreatedAt(LocalDateTime.now());
        when(taskMapper.selectOne(any())).thenReturn(taskEntity);
        when(templateService.saveGeneratedTemplate(any(), org.mockito.ArgumentMatchers.eq(2L), org.mockito.ArgumentMatchers.eq(3L))).thenReturn(new TemplateExtractSaveResultVO(
                11L,
                12L,
                1,
                true,
                "模板定义已保存"
        ));

        service = new TemplateSaveTaskServiceImpl(taskMapper, taskPublisher, templateService, userRequestContext, objectMapper);

        service.processTask(new TemplateSaveTaskMessage("TPL-SAVE-001", 2L, 3L, "trace-save-001"));

        ArgumentCaptor<BlParseTaskEntity> taskCaptor = ArgumentCaptor.forClass(BlParseTaskEntity.class);
        verify(taskMapper, org.mockito.Mockito.times(2)).updateById(taskCaptor.capture());
        BlParseTaskEntity updated = taskCaptor.getAllValues().get(1);
        assertThat(updated.getTaskStatus()).isEqualTo("SUCCESS");
        assertThat(updated.getResultPayload()).contains("模板定义已保存");
    }

    private TemplateExtractSaveRequest previewRequest() {
        return new TemplateExtractSaveRequest(
                "preview-extract-id",
                "preview-source.xlsx",
                true,
                "Preview Template",
                "BILL_PREVIEW",
                List.of(new TemplateFieldMappingSaveRequest("提单号", "bl_no", "STRING", "提单号", 1)),
                "raw-preview-text"
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
