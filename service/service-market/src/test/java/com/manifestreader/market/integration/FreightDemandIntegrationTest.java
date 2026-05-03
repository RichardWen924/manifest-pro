package com.manifestreader.market.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manifestreader.market.messaging.FreightDemandAcceptedConsumer;
import com.manifestreader.market.messaging.FreightDemandAcceptedPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class FreightDemandIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:5.7")
            .withDatabaseName("manifest_refactor")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:6-alpine")
            .withExposedPorts(6379);

    static final Path PROJECT_ROOT = locateProjectRoot();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FreightDemandAcceptedConsumer acceptedConsumer;

    @MockBean
    private FreightDemandAcceptedPublisher acceptedPublisher;

    @BeforeEach
    void setUpSchema() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new FileSystemResource(PROJECT_ROOT.resolve("zfile/sql/V1__init_schema.sql")));
        populator.addScript(new FileSystemResource(PROJECT_ROOT.resolve("zfile/sql/V6__freight_market_init.sql")));
        populator.addScript(new FileSystemResource(PROJECT_ROOT.resolve("zfile/sql/V7__freight_market_audit.sql")));
        populator.execute(jdbcTemplate.getDataSource());

        doAnswer(invocation -> {
            acceptedConsumer.consume(invocation.getArgument(0));
            return null;
        }).when(acceptedPublisher).publish(any());
    }

    @Test
    void acceptQuoteCreatesTimelineThroughMqConsumer() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/market/demands")
                        .contentType("application/json")
                        .header("X-Company-Id", "2")
                        .header("X-User-Id", "3")
                        .content("""
                                {
                                  "title":"上海到汉堡拼箱",
                                  "goodsName":"家具",
                                  "departurePort":"SHANGHAI",
                                  "destinationPort":"HAMBURG",
                                  "quantity":6,
                                  "quantityUnit":"CBM",
                                  "budgetAmount":3200,
                                  "currencyCode":"CNY",
                                  "attachmentFileIds":[]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        Long demandId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        assertThat(createBody.path("success").asBoolean()).isTrue();
        assertThat(demandId).isPositive();

        MvcResult publicPageBeforeAudit = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/market/demands/page")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(objectMapper.readTree(publicPageBeforeAudit.getResponse().getContentAsString())
                .path("data").path("records").isEmpty()).isTrue();

        MvcResult myDemandPage = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/market/demands/mine/page")
                        .header("X-Company-Id", "2")
                        .header("X-User-Id", "3")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(objectMapper.readTree(myDemandPage.getResponse().getContentAsString())
                .path("data").path("records").size()).isEqualTo(1);

        MvcResult auditResult = mockMvc.perform(post("/internal/market/admin/demands/{id}/audit", demandId)
                        .contentType("application/json")
                        .header("X-User-Id", "99")
                        .content("""
                                {
                                  "auditStatus":"APPROVED",
                                  "auditRemark":"审核通过"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode auditBody = objectMapper.readTree(auditResult.getResponse().getContentAsString());
        assertThat(auditBody.path("success").asBoolean()).isTrue();
        assertThat(auditBody.path("data").path("auditStatus").asText()).isEqualTo("APPROVED");

        MvcResult quoteResult = mockMvc.perform(post("/market/demands/{id}/quotes", demandId)
                        .contentType("application/json")
                        .header("X-Company-Id", "9")
                        .header("X-User-Id", "10")
                        .content("""
                                {
                                  "priceAmount":3000,
                                  "currencyCode":"CNY",
                                  "estimatedDays":9,
                                  "serviceNote":"可接单"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        Long quoteId = objectMapper.readTree(quoteResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        JsonNode quoteBody = objectMapper.readTree(quoteResult.getResponse().getContentAsString());
        assertThat(quoteBody.path("success").asBoolean()).isTrue();
        assertThat(quoteId).isPositive();

        MvcResult acceptResult = mockMvc.perform(post("/market/demands/{id}/accept", demandId)
                        .contentType("application/json")
                        .header("X-Company-Id", "2")
                        .header("X-User-Id", "3")
                        .content("{\"quoteId\":" + quoteId + "}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode acceptBody = objectMapper.readTree(acceptResult.getResponse().getContentAsString());
        assertThat(acceptBody.path("success").asBoolean()).isTrue();
        long orderId = acceptBody.path("data").path("id").asLong();

        MvcResult myAcceptedOrders = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/market/orders/mine/page")
                        .header("X-Company-Id", "9")
                        .header("X-User-Id", "10")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(objectMapper.readTree(myAcceptedOrders.getResponse().getContentAsString())
                .path("data").path("records").size()).isEqualTo(1);

        MvcResult startResult = mockMvc.perform(post("/market/orders/{id}/start", orderId)
                        .header("X-Company-Id", "9")
                        .header("X-User-Id", "10"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode startBody = objectMapper.readTree(startResult.getResponse().getContentAsString());
        assertThat(startBody.path("success").asBoolean()).isTrue();
        assertThat(startBody.path("data").path("orderStatus").asText()).isEqualTo("IN_PROGRESS");

        MvcResult completeResult = mockMvc.perform(post("/market/orders/{id}/complete", orderId)
                        .header("X-Company-Id", "2")
                        .header("X-User-Id", "3"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode completeBody = objectMapper.readTree(completeResult.getResponse().getContentAsString());
        assertThat(completeBody.path("success").asBoolean()).isTrue();
        assertThat(completeBody.path("data").path("orderStatus").asText()).isEqualTo("COMPLETED");

        Integer timelineCount = jdbcTemplate.queryForObject(
                "select count(1) from freight_order_timeline",
                Integer.class
        );

        org.assertj.core.api.Assertions.assertThat(timelineCount).isEqualTo(3);

        String demandStatus = jdbcTemplate.queryForObject(
                "select demand_status from freight_demand where id = ?",
                String.class,
                demandId
        );
        assertThat(demandStatus).isEqualTo("COMPLETED");
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
}
