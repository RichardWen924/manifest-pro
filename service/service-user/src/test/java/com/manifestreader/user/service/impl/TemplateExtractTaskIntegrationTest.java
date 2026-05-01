package com.manifestreader.user.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manifestreader.user.messaging.TemplateExtractTaskConsumer;
import com.manifestreader.user.messaging.TemplateExtractTaskPublisher;
import com.manifestreader.user.model.dto.TemplateExtractSaveRequest;
import com.manifestreader.user.model.vo.TemplateExtractResultVO;
import com.manifestreader.user.model.vo.TemplateFieldMappingVO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
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
class TemplateExtractTaskIntegrationTest {

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
            STORAGE_DIR = Files.createTempDirectory("manifest-template-extract-it-storage-");
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
    private TemplateExtractTaskConsumer templateExtractTaskConsumer;

    @MockBean
    private TemplateExtractTaskPublisher templateExtractTaskPublisher;

    @SpyBean
    private UserTemplateServiceImpl userTemplateService;

    @BeforeEach
    void setUpSchema() throws Exception {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new FileSystemResource(PROJECT_ROOT.resolve("zfile/sql/V1__init_schema.sql")));
        populator.addScript(new FileSystemResource(PROJECT_ROOT.resolve("zfile/sql/V2__auth_identity_baseline.sql")));
        populator.addScript(new FileSystemResource(PROJECT_ROOT.resolve("zfile/sql/V4__bill_parse_task_async_enhance.sql")));
        populator.addScript(new FileSystemResource(PROJECT_ROOT.resolve("zfile/sql/V5__async_task_extensions.sql")));
        populator.execute(jdbcTemplate.getDataSource());

        jdbcTemplate.update("DELETE FROM tpl_field_mapping");
        jdbcTemplate.update("DELETE FROM tpl_template_version");
        jdbcTemplate.update("DELETE FROM tpl_template");
        jdbcTemplate.update("DELETE FROM bl_parse_task");
        jdbcTemplate.update("DELETE FROM file_asset");
        jdbcTemplate.update("""
                INSERT INTO sys_company (id, company_code, company_name, company_abbr, status, vip_status, deleted)
                VALUES (2, 'COMPANY_2', 'Test Company', 'TC', 1, 0, 0)
                ON DUPLICATE KEY UPDATE company_name = VALUES(company_name)
                """);

        doAnswer(invocation -> {
            templateExtractTaskConsumer.consume(invocation.getArgument(0));
            return null;
        }).when(templateExtractTaskPublisher).publish(any());

        doAnswer(invocation -> {
            String extractId = invocation.getArgument(3, String.class);
            String blankTemplateDownloadUrl = invocation.getArgument(4, String.class);
            String previewUrl = invocation.getArgument(5, String.class);

            Path blankFile = STORAGE_DIR.resolve("template-extract/2/" + extractId + "/blank/async-blank.docx");
            Files.createDirectories(blankFile.getParent());
            Files.writeString(blankFile, "async-blank-content");
            Path previewFile = STORAGE_DIR.resolve("template-extract/2/" + extractId + "/preview/async-preview.pdf");
            Files.createDirectories(previewFile.getParent());
            Files.writeString(previewFile, "async-preview-content");

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
                    910L,
                    2L,
                    "TEMPLATE_EXTRACT_BLANK",
                    "异步空白模板.docx",
                    "source-template.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    Files.size(blankFile),
                    "LOCAL",
                    "local",
                    "template-extract/2/" + extractId + "/blank/async-blank.docx",
                    extractId,
                    1,
                    3L,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    0
            );
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
                    911L,
                    2L,
                    "TEMPLATE_EXTRACT_PREVIEW",
                    "异步预览.pdf",
                    "source-template.docx",
                    "application/pdf",
                    Files.size(previewFile),
                    "LOCAL",
                    "local",
                    "template-extract/2/" + extractId + "/preview/async-preview.pdf",
                    "preview-hash-001",
                    1,
                    3L,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    0
            );
            return new UserTemplateServiceImpl.TemplateExtractExecutionResult(
                    new TemplateExtractResultVO(
                            extractId,
                            "source-template.docx",
                            1,
                            "GENERATED",
                            "模板字段提取完成",
                            blankTemplateDownloadUrl,
                            previewUrl,
                            "application/pdf",
                            List.of(new TemplateFieldMappingVO("提单号", "bl_no", "STRING", "提单号")),
                            "raw-template-text"
                    ),
                    910L,
                    911L
            );
        }).when(userTemplateService).executeTemplateExtraction(any(), any(Long.class), any(Long.class), any(String.class), any(String.class), any(String.class));
    }

    @Test
    void asyncTemplateExtractFlowSupportsDownloadPreviewAndSave() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "source-template.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "source-template-content".getBytes()
        );

        MvcResult submitResult = mockMvc.perform(multipart("/user/templates/extract/tasks")
                        .file(file)
                        .header("X-Company-Id", "2")
                        .header("X-User-Id", "3")
                        .header("X-Trace-Id", "trace-template-extract-it-001"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode submitJson = objectMapper.readTree(submitResult.getResponse().getContentAsString());
        String taskNo = submitJson.path("data").path("taskNo").asText();

        JsonNode finalTask = null;
        for (int i = 0; i < 20; i++) {
            MvcResult taskResult = mockMvc.perform(get("/user/templates/extract/tasks/{taskNo}", taskNo)
                            .header("X-Company-Id", "2")
                            .header("X-User-Id", "3")
                            .header("X-Trace-Id", "trace-template-extract-it-001"))
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
        org.assertj.core.api.Assertions.assertThat(finalTask.path("result").path("extractId").asText()).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(finalTask.path("result").path("mappings").get(0).path("placeholderKey").asText())
                .isEqualTo("bl_no");

        mockMvc.perform(get("/user/templates/extract/tasks/{taskNo}/blank-template", taskNo)
                        .header("X-Company-Id", "2")
                        .header("X-User-Id", "3")
                        .header("X-Trace-Id", "trace-template-extract-it-001"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename*=UTF-8''%E5%BC%82%E6%AD%A5%E7%A9%BA%E7%99%BD%E6%A8%A1%E6%9D%BF.docx"
                ));

        mockMvc.perform(get("/user/templates/extract/tasks/{taskNo}/preview", taskNo)
                        .header("X-Company-Id", "2")
                        .header("X-User-Id", "3")
                        .header("X-Trace-Id", "trace-template-extract-it-001"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        "inline; filename*=UTF-8''%E5%BC%82%E6%AD%A5%E9%A2%84%E8%A7%88.pdf"
                ));

        TemplateExtractSaveRequest saveRequest = new TemplateExtractSaveRequest(
                taskNo,
                "source-template.docx",
                true,
                "Async Template",
                "BILL_DOCX",
                null,
                "raw-template-text"
        );

        MvcResult saveResult = mockMvc.perform(post("/user/templates/extract/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(saveRequest))
                        .header("X-Company-Id", "2")
                        .header("X-User-Id", "3")
                        .header("X-Trace-Id", "trace-template-extract-it-001"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode saveJson = objectMapper.readTree(saveResult.getResponse().getContentAsString()).path("data");
        org.assertj.core.api.Assertions.assertThat(saveJson.path("templateSaved").asBoolean()).isTrue();
        org.assertj.core.api.Assertions.assertThat(saveJson.path("fieldCount").asInt()).isEqualTo(1);

        Integer templateCount = jdbcTemplate.queryForObject("select count(1) from tpl_template", Integer.class);
        Integer versionCount = jdbcTemplate.queryForObject("select count(1) from tpl_template_version", Integer.class);
        Integer mappingCount = jdbcTemplate.queryForObject("select count(1) from tpl_field_mapping", Integer.class);

        org.assertj.core.api.Assertions.assertThat(templateCount).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(versionCount).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(mappingCount).isEqualTo(1);
    }

    @Test
    void previewOnlyTemplateCanBeSavedWithoutDocxAsset() throws Exception {
        TemplateExtractSaveRequest saveRequest = new TemplateExtractSaveRequest(
                "preview-only-extract-id",
                "preview-source.xlsx",
                true,
                "Preview Template",
                "BILL_PREVIEW",
                List.of(new com.manifestreader.user.model.dto.TemplateFieldMappingSaveRequest(
                        "提单号",
                        "bl_no",
                        "STRING",
                        "提单号",
                        1
                )),
                "raw-preview-text"
        );

        MvcResult saveResult = mockMvc.perform(post("/user/templates/extract/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(saveRequest))
                        .header("X-Company-Id", "2")
                        .header("X-User-Id", "3")
                        .header("X-Trace-Id", "trace-template-preview-save-001"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode saveJson = objectMapper.readTree(saveResult.getResponse().getContentAsString()).path("data");
        org.assertj.core.api.Assertions.assertThat(saveJson.path("templateSaved").asBoolean()).isTrue();
        org.assertj.core.api.Assertions.assertThat(saveJson.path("message").asText()).contains("预览模板");

        Integer templateCount = jdbcTemplate.queryForObject("select count(1) from tpl_template", Integer.class);
        Integer versionCount = jdbcTemplate.queryForObject("select count(1) from tpl_template_version", Integer.class);
        Integer mappingCount = jdbcTemplate.queryForObject("select count(1) from tpl_field_mapping", Integer.class);
        String contentFormat = jdbcTemplate.queryForObject("select content_format from tpl_template_version limit 1", String.class);
        Long fileAssetId = jdbcTemplate.queryForObject("select file_asset_id from tpl_template_version limit 1", Long.class);
        String assetContentType = jdbcTemplate.queryForObject("select content_type from file_asset limit 1", String.class);

        org.assertj.core.api.Assertions.assertThat(templateCount).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(versionCount).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(mappingCount).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(contentFormat).isEqualTo("PREVIEW");
        org.assertj.core.api.Assertions.assertThat(fileAssetId).isNotNull();
        org.assertj.core.api.Assertions.assertThat(assetContentType).isEqualTo("application/json");
    }
}
