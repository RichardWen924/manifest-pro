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
import com.manifestreader.user.cache.BillParseTaskCacheService;
import com.manifestreader.user.dify.DifyTemplateExportParser;
import com.manifestreader.user.dify.DifyWorkflowClient;
import com.manifestreader.user.mapper.UserBillParseTaskMapper;
import com.manifestreader.user.mapper.UserFileAssetMapper;
import com.manifestreader.user.messaging.BillParseTaskMessage;
import com.manifestreader.user.messaging.BillParseTaskPublisher;
import com.manifestreader.user.model.vo.BillExtractTaskSubmitVO;
import com.manifestreader.user.storage.ObjectStorageService;
import com.manifestreader.user.storage.StoredObject;
import com.manifestreader.user.support.UserRequestContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class BillParseTaskServiceImplTest {

    @Mock
    private UserBillParseTaskMapper taskMapper;

    @Mock
    private UserFileAssetMapper fileAssetMapper;

    @Mock
    private DifyWorkflowClient difyWorkflowClient;

    @Mock
    private DifyTemplateExportParser exportParser;

    @Mock
    private BillParseTaskPublisher taskPublisher;

    @Mock
    private BillParseTaskCacheService cacheService;

    @Mock
    private UserRequestContext userRequestContext;

    @Mock
    private ObjectStorageService objectStorageService;

    @InjectMocks
    private BillParseTaskServiceImpl service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void submitExtractTaskCreatesPendingTaskAndPublishesMessage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample-bill.txt",
                "text/plain",
                "bill-content".getBytes()
        );
        when(userRequestContext.currentCompanyId()).thenReturn(2L);
        when(userRequestContext.currentUserId()).thenReturn(3L);
        when(userRequestContext.currentTraceId()).thenReturn("trace-001");
        when(cacheService.findTaskNoByFileHash(any(), any())).thenReturn(Optional.empty());
        when(objectStorageService.put(any(), any(), any())).thenReturn(new StoredObject("LOCAL", "local", "bill/object/key.txt", file.getSize()));
        doAnswer(invocation -> {
            FileAssetEntity entity = invocation.getArgument(0);
            entity.setId(2001L);
            return 1;
        }).when(fileAssetMapper).insert(any(FileAssetEntity.class));
        doAnswer(invocation -> {
            BlParseTaskEntity entity = invocation.getArgument(0);
            entity.setId(1001L);
            entity.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(taskMapper).insert(any(BlParseTaskEntity.class));

        service = new BillParseTaskServiceImpl(
                taskMapper,
                fileAssetMapper,
                difyWorkflowClient,
                exportParser,
                taskPublisher,
                cacheService,
                userRequestContext,
                objectMapper,
                objectStorageService
        );

        BillExtractTaskSubmitVO response = service.submitExtractTask(file);

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.taskNo()).isNotBlank();
        verify(taskMapper).insert(argThat((BlParseTaskEntity task) ->
                "PENDING".equals(task.getTaskStatus())
                        && "DIFY".equals(task.getEngineType())
                        && "sample-bill.txt".equals(task.getFileName())
                        && Long.valueOf(2L).equals(task.getCompanyId())
                        && Long.valueOf(3L).equals(task.getCreatedBy())));
        verify(cacheService).cacheTask(argThat(task ->
                response.taskNo().equals(task.taskNo())
                        && "PENDING".equals(task.status())
                        && "sample-bill.txt".equals(task.fileName())));
        verify(taskPublisher).publish(argThat(message ->
                response.taskNo().equals(message.taskNo())
                        && Long.valueOf(2001L).equals(message.sourceFileId())
                        && "sample-bill.txt".equals(message.fileName())
                        && Long.valueOf(2L).equals(message.companyId())));
    }

    @Test
    void processTaskMarksSuccessAndCachesParsedResult() throws Exception {
        Path tempFile = Files.createTempFile("bill-parse-task-", ".txt");
        Files.writeString(tempFile, "bill-content");
        FileAssetEntity fileAssetEntity = new FileAssetEntity();
        fileAssetEntity.setId(2001L);
        fileAssetEntity.setCompanyId(2L);
        fileAssetEntity.setFileName("sample-bill.txt");
        fileAssetEntity.setContentType("text/plain");
        fileAssetEntity.setBucketName("local");
        fileAssetEntity.setObjectKey("bill/object/key.txt");
        fileAssetEntity.setDeleted(0);
        BlParseTaskEntity taskEntity = new BlParseTaskEntity();
        taskEntity.setId(1001L);
        taskEntity.setTaskNo("TASK-001");
        taskEntity.setCompanyId(2L);
        taskEntity.setTaskStatus("PENDING");
        taskEntity.setEngineType("DIFY");
        taskEntity.setFileName("sample-bill.txt");
        taskEntity.setSourceFileId(2001L);
        taskEntity.setFileHash("hash-001");
        taskEntity.setRequestPayload("""
                {"sourceFileId":2001,"fileHash":"hash-001","fileName":"sample-bill.txt","traceId":"trace-001"}
                """);
        when(taskMapper.selectOne(any())).thenReturn(taskEntity);
        when(fileAssetMapper.selectOne(any())).thenReturn(fileAssetEntity);
        when(objectStorageService.downloadToTemp(any(), any(), any())).thenReturn(tempFile);
        when(difyWorkflowClient.runBillExtraction(any())).thenReturn("""
                {"bl_no":"BL-2026-0001","booking_no":"BOOK-001"}
                """);
        when(exportParser.parse(any())).thenReturn(new DifyTemplateExportParser.ParsedExportFields(
                Map.of("bl_no", "BL-2026-0001", "booking_no", "BOOK-001"),
                "{\"bl_no\":\"BL-2026-0001\"}"
        ));

        service = new BillParseTaskServiceImpl(
                taskMapper,
                fileAssetMapper,
                difyWorkflowClient,
                exportParser,
                taskPublisher,
                cacheService,
                userRequestContext,
                objectMapper,
                objectStorageService
        );

        service.processTask(new BillParseTaskMessage(
                "TASK-001",
                2001L,
                "sample-bill.txt",
                "text/plain",
                "hash-001",
                2L,
                3L,
                "trace-001"
        ));

        ArgumentCaptor<BlParseTaskEntity> taskCaptor = ArgumentCaptor.forClass(BlParseTaskEntity.class);
        verify(taskMapper, org.mockito.Mockito.times(2)).updateById(taskCaptor.capture());
        BlParseTaskEntity updated = taskCaptor.getAllValues().get(1);
        assertThat(updated.getTaskStatus()).isEqualTo("SUCCESS");
        assertThat(updated.getResultPayload()).contains("BL-2026-0001");
        verify(cacheService).cacheTask(argThat(task ->
                "TASK-001".equals(task.taskNo())
                        && "SUCCESS".equals(task.status())
                        && task.result() != null
                        && "BL-2026-0001".equals(task.result().fields().get("bl_no"))));
    }
}
