package com.manifestreader.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manifestreader.model.entity.BlParseTaskEntity;
import com.manifestreader.model.entity.FileAssetEntity;
import com.manifestreader.user.mapper.UserBillParseTaskMapper;
import com.manifestreader.user.mapper.UserFileAssetMapper;
import com.manifestreader.user.messaging.TemplateExportTaskMessage;
import com.manifestreader.user.messaging.TemplateExportTaskPublisher;
import com.manifestreader.user.model.vo.TemplateExportResultVO;
import com.manifestreader.user.model.vo.TemplateExportTaskSubmitVO;
import com.manifestreader.user.storage.ObjectStorageService;
import com.manifestreader.user.storage.StoredObject;
import com.manifestreader.user.support.UserRequestContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class TemplateExportTaskServiceImplTest {

    @Mock
    private UserBillParseTaskMapper taskMapper;

    @Mock
    private UserFileAssetMapper fileAssetMapper;

    @Mock
    private TemplateExportTaskPublisher taskPublisher;

    @Mock
    private UserTemplateServiceImpl templateService;

    @Mock
    private UserRequestContext userRequestContext;

    @Mock
    private ObjectStorageService objectStorageService;

    @InjectMocks
    private TemplateExportTaskServiceImpl service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void submitExportTaskCreatesPendingTaskAndPublishesMessage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample-export.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "export-content".getBytes()
        );
        when(userRequestContext.currentCompanyId()).thenReturn(2L);
        when(userRequestContext.currentUserId()).thenReturn(3L);
        when(userRequestContext.currentTraceId()).thenReturn("trace-export-001");
        when(objectStorageService.put(any(), any(), any())).thenReturn(new StoredObject("LOCAL", "local", "export/source.docx", file.getSize()));
        doAnswer(invocation -> {
            FileAssetEntity entity = invocation.getArgument(0);
            entity.setId(3001L);
            return 1;
        }).when(fileAssetMapper).insert(any(FileAssetEntity.class));
        doAnswer(invocation -> {
            BlParseTaskEntity entity = invocation.getArgument(0);
            entity.setId(2001L);
            entity.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(taskMapper).insert(any(BlParseTaskEntity.class));

        service = new TemplateExportTaskServiceImpl(
                taskMapper,
                fileAssetMapper,
                taskPublisher,
                templateService,
                userRequestContext,
                objectMapper,
                objectStorageService
        );

        TemplateExportTaskSubmitVO response = service.submitTask(11L, "PDF", file);

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.taskNo()).isNotBlank();
        verify(taskMapper).insert(argThat((BlParseTaskEntity task) ->
                "TEMPLATE_EXPORT".equals(task.getTaskType())
                        && "PENDING".equals(task.getTaskStatus())
                        && "sample-export.docx".equals(task.getFileName())
                        && "PDF".equals(task.getOutputFormat())
                        && Long.valueOf(11L).equals(task.getBizId())
                        && Long.valueOf(3001L).equals(task.getSourceFileId())));
        verify(taskPublisher).publish(argThat(message ->
                response.taskNo().equals(message.taskNo())
                        && Long.valueOf(3001L).equals(message.sourceFileId())
                        && Long.valueOf(11L).equals(message.templateId())
                        && "PDF".equals(message.outputFormat())));
    }

    @Test
    void processTaskMarksSuccessAndStoresResultPayload() throws Exception {
        Path tempFile = Files.createTempFile("template-export-task-", ".docx");
        Files.writeString(tempFile, "export-source");
        FileAssetEntity sourceFile = new FileAssetEntity();
        sourceFile.setId(3001L);
        sourceFile.setCompanyId(2L);
        sourceFile.setFileName("sample-export.docx");
        sourceFile.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        sourceFile.setBucketName("local");
        sourceFile.setObjectKey("export/source.docx");
        sourceFile.setDeleted(0);
        BlParseTaskEntity taskEntity = new BlParseTaskEntity();
        taskEntity.setId(2001L);
        taskEntity.setTaskNo("TASK-EXPORT-001");
        taskEntity.setCompanyId(2L);
        taskEntity.setTaskType("TEMPLATE_EXPORT");
        taskEntity.setTaskStatus("PENDING");
        taskEntity.setFileName("sample-export.docx");
        taskEntity.setSourceFileId(3001L);
        taskEntity.setBizId(11L);
        taskEntity.setOutputFormat("PDF");
        taskEntity.setRequestPayload("""
                {"sourceFileId":3001,"templateId":11,"outputFormat":"PDF","fileName":"sample-export.docx","traceId":"trace-export-001"}
                """);
        when(taskMapper.selectOne(any())).thenReturn(taskEntity);
        when(fileAssetMapper.selectOne(any())).thenReturn(sourceFile);
        when(objectStorageService.downloadToTemp(any(), any(), any())).thenReturn(tempFile);
        when(templateService.executeTemplateExport(any(Long.class), any(String.class), any(MultipartFile.class), any(Long.class), any(Long.class)))
                .thenReturn(new UserTemplateServiceImpl.TemplateExportExecutionResult(
                        new TemplateExportResultVO(
                                "export-001",
                                11L,
                                "测试模板",
                                "测试模板-export.pdf",
                                "PDF",
                                "/user/templates/export/export-001/download",
                                Map.of("bl_no", "BL-001"),
                                List.of(),
                                "{\"bl_no\":\"BL-001\"}",
                                "GENERATED",
                                "模板导出完成"
                        ),
                        4001L
                ));

        service = new TemplateExportTaskServiceImpl(
                taskMapper,
                fileAssetMapper,
                taskPublisher,
                templateService,
                userRequestContext,
                objectMapper,
                objectStorageService
        );

        service.processTask(new TemplateExportTaskMessage(
                "TASK-EXPORT-001",
                3001L,
                11L,
                "PDF",
                2L,
                3L,
                "trace-export-001"
        ));

        ArgumentCaptor<BlParseTaskEntity> taskCaptor = ArgumentCaptor.forClass(BlParseTaskEntity.class);
        verify(taskMapper, org.mockito.Mockito.times(2)).updateById(taskCaptor.capture());
        BlParseTaskEntity updated = taskCaptor.getAllValues().get(1);
        assertThat(updated.getTaskStatus()).isEqualTo("SUCCESS");
        assertThat(updated.getResultFileId()).isEqualTo(4001L);
        assertThat(updated.getResultPayload()).contains("export-001");
    }
}
