package com.manifestreader.user.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manifestreader.user.messaging.TemplateExportTaskConsumer;
import com.manifestreader.user.messaging.TemplateExportTaskPublisher;
import com.manifestreader.user.model.vo.TemplateExportResultVO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TemplateExportTaskIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:5.7")
            .withDatabaseName("manifest_refactor")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:6-alpine")
            .withExposedPorts(6379);

    static final Path STORAGE_DIR;
    static final Path PROJECT_ROOT = locateProjectRoot();

    static {
        try {
            STORAGE_DIR = Files.createTempDirectory("manifest-template-export-it-storage-");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Path locateProjectRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("zfile/sql/V1__init_schema.sql"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate project root for SQL scripts");
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> "false");
        registry.add("manifest.storage.type", () -> "local");
        registry.add("manifest.storage.local-base-path", () -> STORAGE_DIR.toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TemplateExportTaskConsumer templateExportTaskConsumer;

    @MockBean
    private TemplateExportTaskPublisher templateExportTaskPublisher;

    @MockBean
    private UserTemplateServiceImpl userTemplateService;

    @BeforeEach
    void setUpSchema() throws Exception {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new FileSystemResource(PROJECT_ROOT.resolve("zfile/sql/V1__init_schema.sql")));
        populator.addScript(new FileSystemResource(PROJECT_ROOT.resolve("zfile/sql/V2__auth_identity_baseline.sql")));
        populator.addScript(new FileSystemResource(PROJECT_ROOT.resolve("zfile/sql/V4__bill_parse_task_async_enhance.sql")));
        populator.addScript(new FileSystemResource(PROJECT_ROOT.resolve("zfile/sql/V5__async_task_extensions.sql")));
        populator.execute(jdbcTemplate.getDataSource());

        jdbcTemplate.update("DELETE FROM bl_parse_task");
        jdbcTemplate.update("DELETE FROM file_asset");
        jdbcTemplate.update("""
                INSERT INTO sys_company (id, company_code, company_name, company_abbr, status, vip_status, deleted)
                VALUES (2, 'COMPANY_2', 'Test Company', 'TC', 1, 0, 0)
                ON DUPLICATE KEY UPDATE company_name = VALUES(company_name)
                """);

        doAnswer(invocation -> {
            templateExportTaskConsumer.consume(invocation.getArgument(0));
            return null;
        }).when(templateExportTaskPublisher).publish(any());

        doAnswer(invocation -> {
            Long templateId = invocation.getArgument(0, Long.class);
            String outputFormat = invocation.getArgument(1, String.class);
            Path exportedFile = STORAGE_DIR.resolve("exports/2/async-export-result.docx");
            Files.createDirectories(exportedFile.getParent());
            Files.writeString(exportedFile, "async-export-result-content");
            jdbcTemplate.update("""
                    INSERT INTO file_asset (
                        id, company_id, biz_type, file_name, original_name, content_type, file_size,
                        storage_type, bucket_name, object_key, file_hash, status, created_by, created_at, updated_at, deleted
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        file_name = VALUES(file_name),
                        original_name = VALUES(original_name),
                        content_type = VALUES(content_type),
                        file_size = VALUES(file_size),
                        storage_type = VALUES(storage_type),
                        bucket_name = VALUES(bucket_name),
                        object_key = VALUES(object_key),
                        file_hash = VALUES(file_hash),
                        updated_at = VALUES(updated_at),
                        deleted = VALUES(deleted)
                    """,
                    900L,
                    2L,
                    "TEMPLATE_EXPORT",
                    "异步电放模板.docx",
                    "source-bill.txt",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    Files.size(exportedFile),
                    "LOCAL",
                    "local",
                    "exports/2/async-export-result.docx",
                    "export-hash-001",
                    1,
                    3L,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    0
            );
            return new UserTemplateServiceImpl.TemplateExportExecutionResult(
                    new TemplateExportResultVO(
                            "export-it-001",
                            templateId,
                            "Integration Template",
                            "异步电放模板.docx",
                            outputFormat,
                            "/user/templates/export/tasks/download/export-it-001",
                            Map.of("bl_no", "BL-EXPORT-0001"),
                            List.of(),
                            "raw-export-text",
                            "GENERATED",
                            "模板导出完成"
                    ),
                    900L
            );
        }).when(userTemplateService).executeTemplateExport(any(Long.class), any(String.class), any(), any(Long.class), any(Long.class));
    }

    @Test
    void asyncTemplateExportFlowWorksAcrossHttpDbMqAndDownload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "source-bill.txt",
                "text/plain",
                "source bill content".getBytes()
        );

        MvcResult submitResult = mockMvc.perform(multipart("/user/templates/export/tasks")
                        .file(file)
                        .param("templateId", "11")
                        .param("outputFormat", "DOCX")
                        .header("X-Company-Id", "2")
                        .header("X-User-Id", "3")
                        .header("X-Trace-Id", "trace-template-export-it-001"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode submitJson = objectMapper.readTree(submitResult.getResponse().getContentAsString());
        String taskNo = submitJson.path("data").path("taskNo").asText();

        JsonNode finalTask = null;
        for (int i = 0; i < 20; i++) {
            MvcResult taskResult = mockMvc.perform(get("/user/templates/export/tasks/{taskNo}", taskNo)
                            .header("X-Company-Id", "2")
                            .header("X-User-Id", "3")
                            .header("X-Trace-Id", "trace-template-export-it-001"))
                    .andExpect(status().isOk())
                    .andReturn();
            finalTask = objectMapper.readTree(taskResult.getResponse().getContentAsString()).path("data");
            if ("SUCCESS".equals(finalTask.path("status").asText())) {
                break;
            }
            Thread.sleep(300);
        }

        org.assertj.core.api.Assertions.assertThat(finalTask).isNotNull();
        org.assertj.core.api.Assertions.assertThat(finalTask.path("status").asText()).isEqualTo("SUCCESS");
        org.assertj.core.api.Assertions.assertThat(finalTask.path("result").path("extractedFields").path("bl_no").asText())
                .isEqualTo("BL-EXPORT-0001");

        String dbStatus = jdbcTemplate.queryForObject(
                "select task_status from bl_parse_task where task_no = ?",
                String.class,
                taskNo
        );
        Long resultFileId = jdbcTemplate.queryForObject(
                "select result_file_id from bl_parse_task where task_no = ?",
                Long.class,
                taskNo
        );

        org.assertj.core.api.Assertions.assertThat(dbStatus).isEqualTo("SUCCESS");
        org.assertj.core.api.Assertions.assertThat(resultFileId).isEqualTo(900L);

        MvcResult downloadResult = mockMvc.perform(get("/user/templates/export/tasks/{taskNo}/download", taskNo)
                        .header("X-Company-Id", "2")
                        .header("X-User-Id", "3")
                        .header("X-Trace-Id", "trace-template-export-it-001"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename*=UTF-8''%E5%BC%82%E6%AD%A5%E7%94%B5%E6%94%BE%E6%A8%A1%E6%9D%BF.docx"
                ))
                .andReturn();

        org.assertj.core.api.Assertions.assertThat(downloadResult.getResponse().getContentAsString())
                .isEqualTo("async-export-result-content");
    }
}
