package com.manifestreader.user.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manifestreader.user.dify.DifyWorkflowClient;
import com.manifestreader.user.messaging.BillParseTaskConsumer;
import com.manifestreader.user.messaging.BillParseTaskPublisher;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.FileSystemResource;
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
class BillParseTaskIntegrationTest {

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
            STORAGE_DIR = Files.createTempDirectory("manifest-it-storage-");
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
    private BillParseTaskConsumer billParseTaskConsumer;

    @MockBean
    private DifyWorkflowClient difyWorkflowClient;

    @MockBean
    private BillParseTaskPublisher billParseTaskPublisher;

    @BeforeEach
    void setUpSchema() throws Exception {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new FileSystemResource(PROJECT_ROOT.resolve("zfile/sql/V1__init_schema.sql")));
        populator.addScript(new FileSystemResource(PROJECT_ROOT.resolve("zfile/sql/V2__auth_identity_baseline.sql")));
        populator.addScript(new FileSystemResource(PROJECT_ROOT.resolve("zfile/sql/V4__bill_parse_task_async_enhance.sql")));
        populator.execute(jdbcTemplate.getDataSource());
        jdbcTemplate.update("""
                INSERT INTO sys_company (id, company_code, company_name, company_abbr, status, vip_status, deleted)
                VALUES (2, 'COMPANY_2', 'Test Company', 'TC', 1, 0, 0)
                ON DUPLICATE KEY UPDATE company_name = VALUES(company_name)
                """);
        when(difyWorkflowClient.runBillExtraction(any())).thenReturn("""
                {"bl_no":"BL-IT-0001","booking_no":"BOOK-IT-0001","port_of_loading":"SHANGHAI"}
                """);
        doAnswer(invocation -> {
            billParseTaskConsumer.consume(invocation.getArgument(0));
            return null;
        }).when(billParseTaskPublisher).publish(any());
    }

    @Test
    void asyncBillParseFlowWorksAcrossHttpDbMqAndRedis() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "integration-bill.txt",
                "text/plain",
                "integration-bill-content".getBytes()
        );

        MvcResult submitResult = mockMvc.perform(multipart("/user/bills/extract/tasks")
                        .file(file)
                        .header("X-Company-Id", "2")
                        .header("X-User-Id", "3")
                        .header("X-Trace-Id", "trace-it-001"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode submitJson = objectMapper.readTree(submitResult.getResponse().getContentAsString());
        String taskNo = submitJson.path("data").path("taskNo").asText();

        JsonNode finalTask = null;
        for (int i = 0; i < 20; i++) {
            MvcResult taskResult = mockMvc.perform(get("/user/bills/extract/tasks/{taskNo}", taskNo)
                            .header("X-Company-Id", "2")
                            .header("X-User-Id", "3")
                            .header("X-Trace-Id", "trace-it-001"))
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
        org.assertj.core.api.Assertions.assertThat(finalTask.path("result").path("fields").path("bl_no").asText()).isEqualTo("BL-IT-0001");

        String dbStatus = jdbcTemplate.queryForObject(
                "select task_status from bl_parse_task where task_no = ?",
                String.class,
                taskNo
        );
        Long sourceFileId = jdbcTemplate.queryForObject(
                "select source_file_id from bl_parse_task where task_no = ?",
                Long.class,
                taskNo
        );
        Integer fileCount = jdbcTemplate.queryForObject(
                "select count(1) from file_asset where id = ? and deleted = 0",
                Integer.class,
                sourceFileId
        );

        org.assertj.core.api.Assertions.assertThat(dbStatus).isEqualTo("SUCCESS");
        org.assertj.core.api.Assertions.assertThat(sourceFileId).isNotNull();
        org.assertj.core.api.Assertions.assertThat(fileCount).isEqualTo(1);
    }
}
