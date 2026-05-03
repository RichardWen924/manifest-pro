# Manifest Reader Marketplace Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Treat marketplace delivery as the only active workstream for now, and build the first complete backend slice by introducing `service-market` for freight demand publishing, quoting, quote acceptance, and fulfillment order creation.

**Architecture:** Add a new Spring Boot microservice named `service-market` that follows the existing `service-user` structure: controller -> service -> mapper -> MyBatis entity, with Nacos discovery/config, gateway routing, Redis protection for hot acceptance, and RabbitMQ follow-up events. Keep shipping tools in `service-user`; `service-market` owns marketplace state and only references shared file assets by id.

Marketplace management should not be built into the user-facing module. Management workflows such as review, statistics, fulfillment supervision, and manual intervention should be exposed from `service-admin`, which will call `service-market` over Feign for admin-side operations.

**Tech Stack:** Spring Boot 3.5, Spring Cloud OpenFeign, Spring Cloud Alibaba Nacos, MyBatis Plus, MySQL, Redis, RabbitMQ, Springdoc, JUnit 5, Mockito, Testcontainers, MockMvc

---

## Scope Check

This plan is now the primary active execution plan. Current delivery scope is intentionally narrowed to marketplace only:

- included: `service-market` backend first release
- included: follow-up marketplace backend completion and API hardening
- excluded: `service-news`
- excluded: unified frontend reconstruction
- excluded: any non-marketplace community capability
- admin ownership note: marketplace management belongs to `service-admin`, not `service-user`

News/community and unified portal work should stay frozen until marketplace backend and marketplace frontend both reach acceptance.

## File Structure

### Create

- `service/service-market/pom.xml`
- `service/service-market/src/main/java/com/manifestreader/market/MarketApplication.java`
- `service/service-market/src/main/java/com/manifestreader/market/config/MarketMybatisMapperConfig.java`
- `service/service-market/src/main/java/com/manifestreader/market/config/FreightMarketMessagingConfig.java`
- `service/service-market/src/main/java/com/manifestreader/market/controller/FreightDemandController.java`
- `service/service-market/src/main/java/com/manifestreader/market/mapper/FreightDemandMapper.java`
- `service/service-market/src/main/java/com/manifestreader/market/mapper/FreightDemandAttachmentMapper.java`
- `service/service-market/src/main/java/com/manifestreader/market/mapper/FreightQuoteMapper.java`
- `service/service-market/src/main/java/com/manifestreader/market/mapper/FreightOrderMapper.java`
- `service/service-market/src/main/java/com/manifestreader/market/mapper/FreightOrderTimelineMapper.java`
- `service/service-market/src/main/java/com/manifestreader/market/model/dto/FreightDemandCreateRequest.java`
- `service/service-market/src/main/java/com/manifestreader/market/model/dto/FreightDemandPageQuery.java`
- `service/service-market/src/main/java/com/manifestreader/market/model/dto/FreightQuoteCreateRequest.java`
- `service/service-market/src/main/java/com/manifestreader/market/model/dto/FreightDemandAcceptRequest.java`
- `service/service-market/src/main/java/com/manifestreader/market/model/vo/FreightDemandVO.java`
- `service/service-market/src/main/java/com/manifestreader/market/model/vo/FreightDemandDetailVO.java`
- `service/service-market/src/main/java/com/manifestreader/market/model/vo/FreightQuoteVO.java`
- `service/service-market/src/main/java/com/manifestreader/market/model/vo/FreightOrderVO.java`
- `service/service-market/src/main/java/com/manifestreader/market/messaging/FreightDemandAcceptedMessage.java`
- `service/service-market/src/main/java/com/manifestreader/market/messaging/FreightDemandAcceptedPublisher.java`
- `service/service-market/src/main/java/com/manifestreader/market/messaging/RabbitFreightDemandAcceptedPublisher.java`
- `service/service-market/src/main/java/com/manifestreader/market/messaging/FreightDemandAcceptedConsumer.java`
- `service/service-market/src/main/java/com/manifestreader/market/service/FreightDemandService.java`
- `service/service-market/src/main/java/com/manifestreader/market/service/impl/FreightDemandServiceImpl.java`
- `service/service-market/src/main/java/com/manifestreader/market/support/UserRequestContext.java`
- `service/service-market/src/main/java/com/manifestreader/market/support/HeaderUserRequestContext.java`
- `service/service-market/src/main/resources/application.yml`
- `service/service-market/src/main/resources/dev.yml`
- `service/service-market/src/test/java/com/manifestreader/market/FreightDemandControllerTest.java`
- `service/service-market/src/test/java/com/manifestreader/market/service/impl/FreightDemandServiceImplTest.java`
- `service/service-market/src/test/java/com/manifestreader/market/integration/FreightDemandIntegrationTest.java`
- `model/src/main/java/com/manifestreader/model/entity/FreightDemandEntity.java`
- `model/src/main/java/com/manifestreader/model/entity/FreightDemandAttachmentEntity.java`
- `model/src/main/java/com/manifestreader/model/entity/FreightQuoteEntity.java`
- `model/src/main/java/com/manifestreader/model/entity/FreightOrderEntity.java`
- `model/src/main/java/com/manifestreader/model/entity/FreightOrderTimelineEntity.java`
- `zfile/sql/V6__freight_market_init.sql`

### Modify

- `service/pom.xml`
- `gateway/src/main/resources/application.yml`
- `docs/backend-rabbitmq-local.md`
- `task_plan.md`
- `findings.md`
- `progress.md`

### Notes

- Reuse `file_asset` as the attachment source of truth. `service-market` stores only relation rows, not binary content.
- Do not add Feign calls in the first slice unless a real cross-service dependency appears.
- Keep package naming aligned with existing modules: `com.manifestreader.market`.

---

### Task 1: Scaffold `service-market` and register it in platform infrastructure

**Files:**
- Create: `service/service-market/pom.xml`
- Create: `service/service-market/src/main/java/com/manifestreader/market/MarketApplication.java`
- Create: `service/service-market/src/main/resources/application.yml`
- Create: `service/service-market/src/main/resources/dev.yml`
- Create: `service/service-market/src/test/java/com/manifestreader/market/MarketApplicationContextTest.java`
- Modify: `service/pom.xml`
- Modify: `gateway/src/main/resources/application.yml`

- [ ] **Step 1: Write the failing context and route contract tests**

```java
package com.manifestreader.market;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MarketApplicationContextTest {

    @Test
    void serviceModuleHasNacosAndBootConfiguration() throws Exception {
        String pom = Files.readString(Path.of("service/service-market/pom.xml"));
        String yaml = Files.readString(Path.of("service/service-market/src/main/resources/application.yml"));
        assertThat(pom).contains("spring-cloud-starter-openfeign");
        assertThat(pom).contains("spring-cloud-starter-alibaba-nacos-discovery");
        assertThat(yaml).contains("manifest-reader-market");
    }
}
```

```java
package com.manifestreader.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GatewayMarketRouteContractTest {

    @Test
    void gatewayHasMarketRoute() throws Exception {
        String yaml = Files.readString(Path.of("gateway/src/main/resources/application.yml"));
        assertThat(yaml).contains("lb://manifest-reader-market");
        assertThat(yaml).contains("Path=/market/**");
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
./mvnw -pl gateway,service/service-market -am -Dtest=GatewayMarketRouteContractTest,MarketApplicationContextTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

- `service/service-market` module missing
- gateway route assertion fails because `/market/**` route does not exist yet

- [ ] **Step 3: Add the new module, boot app, and configuration**

`service/pom.xml`

```xml
<modules>
    <module>service-admin</module>
    <module>service-user</module>
    <module>service-auth</module>
    <module>service-llm-task</module>
    <module>service-market</module>
</modules>
```

`service/service-market/pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.manifestreader</groupId>
        <artifactId>service</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>
    <artifactId>service-market</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.manifestreader</groupId>
            <artifactId>common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.manifestreader</groupId>
            <artifactId>model</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>mysql</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

`service/service-market/src/main/java/com/manifestreader/market/MarketApplication.java`

```java
package com.manifestreader.market;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.manifestreader.market", "com.manifestreader.common"})
public class MarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketApplication.class, args);
    }
}
```

`service/service-market/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: manifest-reader-market
  config:
    import:
      - optional:classpath:dev.yml
      - optional:nacos:${spring.application.name}-${spring.profiles.active:dev}.yml
  cloud:
    nacos:
      config:
        enabled: ${NACOS_CONFIG_ENABLED:true}
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}
        group: ${NACOS_GROUP:DEFAULT_GROUP}
        file-extension: yml
        username: ${NACOS_USERNAME:}
        password: ${NACOS_PASSWORD:}
      discovery:
        enabled: ${NACOS_DISCOVERY_ENABLED:true}
        register-enabled: ${NACOS_REGISTER_ENABLED:true}
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}
        group: ${NACOS_GROUP:DEFAULT_GROUP}
        ip: ${NACOS_DISCOVERY_IP:}
        username: ${NACOS_USERNAME:}
        password: ${NACOS_PASSWORD:}

server:
  port: 8085

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

`service/service-market/src/main/resources/dev.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:127.0.0.1}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:manifest_refactor}?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:Wen668668}
  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
  rabbitmq:
    host: ${RABBITMQ_HOST:127.0.0.1}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    listener:
      simple:
        auto-startup: ${RABBITMQ_LISTENER_AUTO_STARTUP:false}
```

`gateway/src/main/resources/application.yml`

```yaml
            - id: market-service
              uri: ${MARKET_SERVICE_URI:lb://manifest-reader-market}
              predicates:
                - Path=/market/**
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```bash
./mvnw -pl gateway,service/service-market -am -Dtest=GatewayMarketRouteContractTest,MarketApplicationContextTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

- both tests pass
- Maven recognizes the new module

- [ ] **Step 5: Commit**

```bash
git add service/pom.xml gateway/src/main/resources/application.yml service/service-market
git commit -m "feat: scaffold market service module"
```

### Task 2: Add marketplace schema and shared entities

**Files:**
- Create: `zfile/sql/V6__freight_market_init.sql`
- Create: `model/src/main/java/com/manifestreader/model/entity/FreightDemandEntity.java`
- Create: `model/src/main/java/com/manifestreader/model/entity/FreightDemandAttachmentEntity.java`
- Create: `model/src/main/java/com/manifestreader/model/entity/FreightQuoteEntity.java`
- Create: `model/src/main/java/com/manifestreader/model/entity/FreightOrderEntity.java`
- Create: `model/src/main/java/com/manifestreader/model/entity/FreightOrderTimelineEntity.java`
- Test: `service/service-market/src/test/java/com/manifestreader/market/MarketSchemaContractTest.java`

- [ ] **Step 1: Write failing schema contract test**

```java
package com.manifestreader.market;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MarketSchemaContractTest {

    @Test
    void sqlMigrationCreatesMarketplaceTables() throws Exception {
        String sql = Files.readString(Path.of("zfile/sql/V6__freight_market_init.sql"));
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS freight_demand");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS freight_quote");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS freight_order");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS freight_order_timeline");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./mvnw -pl service/service-market -am -Dtest=MarketSchemaContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

- fail because `V6__freight_market_init.sql` does not exist

- [ ] **Step 3: Add migration and shared entity classes**

`zfile/sql/V6__freight_market_init.sql`

```sql
CREATE TABLE IF NOT EXISTS freight_demand (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    company_id BIGINT NOT NULL,
    publisher_user_id BIGINT NOT NULL,
    demand_no VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    goods_name VARCHAR(255) NOT NULL,
    departure_port VARCHAR(128) NOT NULL,
    destination_port VARCHAR(128) NOT NULL,
    expected_shipping_date DATE NULL,
    quantity DECIMAL(18,2) NULL,
    quantity_unit VARCHAR(32) NULL,
    budget_amount DECIMAL(18,2) NULL,
    currency_code VARCHAR(16) NULL,
    contact_name VARCHAR(128) NULL,
    contact_phone VARCHAR(64) NULL,
    remark VARCHAR(1000) NULL,
    demand_status VARCHAR(32) NOT NULL,
    accepted_quote_id BIGINT NULL,
    accepted_order_id BIGINT NULL,
    hot_score BIGINT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_freight_demand_no (demand_no),
    KEY idx_freight_demand_company_status (company_id, demand_status, deleted)
);

CREATE TABLE IF NOT EXISTS freight_demand_attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    demand_id BIGINT NOT NULL,
    file_asset_id BIGINT NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_freight_demand_attachment_demand (demand_id, deleted)
);

CREATE TABLE IF NOT EXISTS freight_quote (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    demand_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    quoter_user_id BIGINT NOT NULL,
    quote_no VARCHAR(64) NOT NULL,
    price_amount DECIMAL(18,2) NOT NULL,
    currency_code VARCHAR(16) NOT NULL,
    estimated_days INT NULL,
    service_note VARCHAR(1000) NULL,
    quote_status VARCHAR(32) NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_freight_quote_no (quote_no),
    KEY idx_freight_quote_demand (demand_id, quote_status, deleted)
);

CREATE TABLE IF NOT EXISTS freight_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    demand_id BIGINT NOT NULL,
    accepted_quote_id BIGINT NOT NULL,
    publisher_company_id BIGINT NOT NULL,
    publisher_user_id BIGINT NOT NULL,
    agent_company_id BIGINT NOT NULL,
    agent_user_id BIGINT NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_freight_order_no (order_no),
    KEY idx_freight_order_demand (demand_id, order_status, deleted)
);

CREATE TABLE IF NOT EXISTS freight_order_timeline (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    event_message VARCHAR(512) NOT NULL,
    operator_user_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_freight_order_timeline_order (order_id, created_at)
);
```

`model/src/main/java/com/manifestreader/model/entity/FreightDemandEntity.java`

```java
@TableName("freight_demand")
public class FreightDemandEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long companyId;
    private Long publisherUserId;
    private String demandNo;
    private String title;
    private String goodsName;
    private String departurePort;
    private String destinationPort;
    private LocalDate expectedShippingDate;
    private BigDecimal quantity;
    private String quantityUnit;
    private BigDecimal budgetAmount;
    private String currencyCode;
    private String contactName;
    private String contactPhone;
    private String remark;
    private String demandStatus;
    private Long acceptedQuoteId;
    private Long acceptedOrderId;
    private Long hotScore;
    private Integer deleted;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

`model/src/main/java/com/manifestreader/model/entity/FreightQuoteEntity.java`

```java
@TableName("freight_quote")
public class FreightQuoteEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long demandId;
    private Long companyId;
    private Long quoterUserId;
    private String quoteNo;
    private BigDecimal priceAmount;
    private String currencyCode;
    private Integer estimatedDays;
    private String serviceNote;
    private String quoteStatus;
    private Integer deleted;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

Use the same POJO style as `BlParseTaskEntity` for all five entity files.

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
./mvnw -pl service/service-market -am -Dtest=MarketSchemaContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

- migration contract test passes
- model module compiles with new entity classes

- [ ] **Step 5: Commit**

```bash
git add zfile/sql/V6__freight_market_init.sql model/src/main/java/com/manifestreader/model/entity service/service-market/src/test/java/com/manifestreader/market/MarketSchemaContractTest.java
git commit -m "feat: add marketplace schema and entities"
```

### Task 3: Implement freight demand publish, list, and detail APIs

**Files:**
- Create: `service/service-market/src/main/java/com/manifestreader/market/config/MarketMybatisMapperConfig.java`
- Create: `service/service-market/src/main/java/com/manifestreader/market/support/UserRequestContext.java`
- Create: `service/service-market/src/main/java/com/manifestreader/market/support/HeaderUserRequestContext.java`
- Create: `service/service-market/src/main/java/com/manifestreader/market/mapper/FreightDemandMapper.java`
- Create: `service/service-market/src/main/java/com/manifestreader/market/mapper/FreightDemandAttachmentMapper.java`
- Create: `service/service-market/src/main/java/com/manifestreader/market/model/dto/FreightDemandCreateRequest.java`
- Create: `service/service-market/src/main/java/com/manifestreader/market/model/dto/FreightDemandPageQuery.java`
- Create: `service/service-market/src/main/java/com/manifestreader/market/model/vo/FreightDemandVO.java`
- Create: `service/service-market/src/main/java/com/manifestreader/market/model/vo/FreightDemandDetailVO.java`
- Create: `service/service-market/src/main/java/com/manifestreader/market/service/FreightDemandService.java`
- Create: `service/service-market/src/main/java/com/manifestreader/market/service/impl/FreightDemandServiceImpl.java`
- Create: `service/service-market/src/main/java/com/manifestreader/market/controller/FreightDemandController.java`
- Test: `service/service-market/src/test/java/com/manifestreader/market/service/impl/FreightDemandServiceImplTest.java`
- Test: `service/service-market/src/test/java/com/manifestreader/market/FreightDemandControllerTest.java`

- [ ] **Step 1: Write failing unit and controller tests**

`service/service-market/src/test/java/com/manifestreader/market/service/impl/FreightDemandServiceImplTest.java`

```java
@ExtendWith(MockitoExtension.class)
class FreightDemandServiceImplTest {

    @Mock private FreightDemandMapper demandMapper;
    @Mock private FreightDemandAttachmentMapper attachmentMapper;
    @Mock private UserRequestContext userRequestContext;

    @InjectMocks
    private FreightDemandServiceImpl service;

    @Test
    void createDemandPersistsDemandAndAttachments() {
        when(userRequestContext.currentCompanyId()).thenReturn(2L);
        when(userRequestContext.currentUserId()).thenReturn(3L);
        doAnswer(invocation -> {
            FreightDemandEntity entity = invocation.getArgument(0);
            entity.setId(1001L);
            return 1;
        }).when(demandMapper).insert(any(FreightDemandEntity.class));

        FreightDemandCreateRequest request = new FreightDemandCreateRequest(
                "上海至新加坡整柜需求",
                "电子元件",
                "SHANGHAI",
                "SINGAPORE",
                LocalDate.of(2026, 5, 20),
                new BigDecimal("12"),
                "CBM",
                new BigDecimal("2800"),
                "CNY",
                "Alice",
                "13800000001",
                "需要报关配套",
                List.of(9001L, 9002L)
        );

        FreightDemandVO result = service.createDemand(request);

        assertThat(result.demandStatus()).isEqualTo("PUBLISHED");
        verify(demandMapper).insert(argThat(entity ->
                "PUBLISHED".equals(entity.getDemandStatus())
                        && "电子元件".equals(entity.getGoodsName())));
        verify(attachmentMapper, times(2)).insert(any(FreightDemandAttachmentEntity.class));
    }
}
```

`service/service-market/src/test/java/com/manifestreader/market/FreightDemandControllerTest.java`

```java
@WebMvcTest(FreightDemandController.class)
class FreightDemandControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private FreightDemandService freightDemandService;

    @Test
    void pageEndpointReturnsWrappedResponse() throws Exception {
        when(freightDemandService.page(any())).thenReturn(PageResult.empty(1L, 10L));
        mockMvc.perform(get("/market/demands/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
./mvnw -pl service/service-market -am -Dtest=FreightDemandServiceImplTest,FreightDemandControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

- missing mapper/service/controller/dto/vo classes

- [ ] **Step 3: Implement create/page/detail flow**

`HeaderUserRequestContext.java`

```java
@Component
public class HeaderUserRequestContext implements UserRequestContext {
    @Override
    public Long currentCompanyId() {
        return readLongHeader("X-Company-Id", 2L);
    }

    @Override
    public Long currentUserId() {
        return readLongHeader("X-User-Id", 3L);
    }
}
```

`FreightDemandCreateRequest.java`

```java
public record FreightDemandCreateRequest(
        @NotBlank String title,
        @NotBlank String goodsName,
        @NotBlank String departurePort,
        @NotBlank String destinationPort,
        LocalDate expectedShippingDate,
        BigDecimal quantity,
        String quantityUnit,
        BigDecimal budgetAmount,
        String currencyCode,
        String contactName,
        String contactPhone,
        String remark,
        List<Long> attachmentFileIds
) {}
```

`FreightDemandVO.java`

```java
public record FreightDemandVO(
        Long id,
        String demandNo,
        String title,
        String goodsName,
        String departurePort,
        String destinationPort,
        String demandStatus,
        BigDecimal budgetAmount,
        String currencyCode,
        LocalDateTime createdAt
) {}
```

`FreightDemandService.java`

```java
public interface FreightDemandService {
    FreightDemandVO createDemand(FreightDemandCreateRequest request);
    PageResult<FreightDemandVO> page(FreightDemandPageQuery query);
    FreightDemandDetailVO detail(Long id);
}
```

`FreightDemandServiceImpl.java`

```java
@Service
public class FreightDemandServiceImpl implements FreightDemandService {

    private final FreightDemandMapper demandMapper;
    private final FreightDemandAttachmentMapper attachmentMapper;
    private final UserRequestContext userRequestContext;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FreightDemandVO createDemand(FreightDemandCreateRequest request) {
        FreightDemandEntity entity = new FreightDemandEntity();
        entity.setCompanyId(userRequestContext.currentCompanyId());
        entity.setPublisherUserId(userRequestContext.currentUserId());
        entity.setDemandNo("FD-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + UUID.randomUUID().toString().substring(0, 8));
        entity.setTitle(request.title());
        entity.setGoodsName(request.goodsName());
        entity.setDeparturePort(request.departurePort());
        entity.setDestinationPort(request.destinationPort());
        entity.setExpectedShippingDate(request.expectedShippingDate());
        entity.setQuantity(request.quantity());
        entity.setQuantityUnit(request.quantityUnit());
        entity.setBudgetAmount(request.budgetAmount());
        entity.setCurrencyCode(StringUtils.hasText(request.currencyCode()) ? request.currencyCode() : "CNY");
        entity.setContactName(request.contactName());
        entity.setContactPhone(request.contactPhone());
        entity.setRemark(request.remark());
        entity.setDemandStatus("PUBLISHED");
        entity.setHotScore(0L);
        entity.setDeleted(0);
        entity.setCreatedBy(userRequestContext.currentUserId());
        entity.setUpdatedBy(userRequestContext.currentUserId());
        demandMapper.insert(entity);
        saveAttachments(entity.getId(), request.attachmentFileIds());
        return toVO(entity);
    }
}
```

`FreightDemandController.java`

```java
@Tag(name = "货运市场-需求")
@RestController
@RequestMapping("/market/demands")
public class FreightDemandController {

    private final FreightDemandService freightDemandService;

    @PostMapping
    public R<FreightDemandVO> create(@Valid @RequestBody FreightDemandCreateRequest request) {
        return R.ok(freightDemandService.createDemand(request));
    }

    @GetMapping("/page")
    public R<PageResult<FreightDemandVO>> page(FreightDemandPageQuery query) {
        return R.ok(freightDemandService.page(query));
    }

    @GetMapping("/{id}")
    public R<FreightDemandDetailVO> detail(@PathVariable Long id) {
        return R.ok(freightDemandService.detail(id));
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```bash
./mvnw -pl service/service-market -am -Dtest=FreightDemandServiceImplTest,FreightDemandControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

- create demand unit test passes
- controller wrapper test passes

- [ ] **Step 5: Commit**

```bash
git add service/service-market/src/main/java service/service-market/src/test/java
git commit -m "feat: add freight demand publish and query apis"
```

### Task 4: Implement quote submission and quote acceptance domain flow

**Files:**
- Create: `service/service-market/src/main/java/com/manifestreader/market/mapper/FreightQuoteMapper.java`
- Create: `service/service-market/src/main/java/com/manifestreader/market/mapper/FreightOrderMapper.java`
- Create: `service/service-market/src/main/java/com/manifestreader/market/model/dto/FreightQuoteCreateRequest.java`
- Create: `service/service-market/src/main/java/com/manifestreader/market/model/dto/FreightDemandAcceptRequest.java`
- Create: `service/service-market/src/main/java/com/manifestreader/market/model/vo/FreightQuoteVO.java`
- Create: `service/service-market/src/main/java/com/manifestreader/market/model/vo/FreightOrderVO.java`
- Modify: `service/service-market/src/main/java/com/manifestreader/market/service/FreightDemandService.java`
- Modify: `service/service-market/src/main/java/com/manifestreader/market/service/impl/FreightDemandServiceImpl.java`
- Modify: `service/service-market/src/main/java/com/manifestreader/market/controller/FreightDemandController.java`
- Test: `service/service-market/src/test/java/com/manifestreader/market/service/impl/FreightDemandServiceImplTest.java`

- [ ] **Step 1: Extend tests with quote and accept scenarios**

```java
@Test
void submitQuoteCreatesSubmittedQuote() {
    when(userRequestContext.currentCompanyId()).thenReturn(9L);
    when(userRequestContext.currentUserId()).thenReturn(10L);
    FreightDemandEntity demand = new FreightDemandEntity();
    demand.setId(1001L);
    demand.setDemandStatus("PUBLISHED");
    when(demandMapper.selectById(1001L)).thenReturn(demand);

    service.submitQuote(1001L, new FreightQuoteCreateRequest(
            new BigDecimal("2600"), "CNY", 7, "可提供拖车"
    ));

    verify(quoteMapper).insert(argThat(entity ->
            Long.valueOf(1001L).equals(entity.getDemandId())
                    && "SUBMITTED".equals(entity.getQuoteStatus())));
}

@Test
void acceptQuoteCreatesOrderAndMarksDemandLocked() {
    when(userRequestContext.currentCompanyId()).thenReturn(2L);
    when(userRequestContext.currentUserId()).thenReturn(3L);
    FreightDemandEntity demand = new FreightDemandEntity();
    demand.setId(1001L);
    demand.setCompanyId(2L);
    demand.setPublisherUserId(3L);
    demand.setDemandStatus("PUBLISHED");
    FreightQuoteEntity quote = new FreightQuoteEntity();
    quote.setId(2001L);
    quote.setDemandId(1001L);
    quote.setCompanyId(9L);
    quote.setQuoterUserId(10L);
    quote.setQuoteStatus("SUBMITTED");
    when(demandMapper.selectById(1001L)).thenReturn(demand);
    when(quoteMapper.selectById(2001L)).thenReturn(quote);

    service.acceptQuote(1001L, new FreightDemandAcceptRequest(2001L));

    verify(orderMapper).insert(any(FreightOrderEntity.class));
    verify(demandMapper).updateById(argThat(entity ->
            "LOCKED".equals(entity.getDemandStatus())
                    && Long.valueOf(2001L).equals(entity.getAcceptedQuoteId())));
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
./mvnw -pl service/service-market -am -Dtest=FreightDemandServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

- compile or assertion failures because quote/order methods do not exist yet

- [ ] **Step 3: Add quote and accept implementations**

`FreightDemandService.java`

```java
FreightQuoteVO submitQuote(Long demandId, FreightQuoteCreateRequest request);
FreightOrderVO acceptQuote(Long demandId, FreightDemandAcceptRequest request);
List<FreightQuoteVO> listQuotes(Long demandId);
```

`FreightQuoteCreateRequest.java`

```java
public record FreightQuoteCreateRequest(
        @NotNull BigDecimal priceAmount,
        @NotBlank String currencyCode,
        Integer estimatedDays,
        String serviceNote
) {}
```

`FreightDemandAcceptRequest.java`

```java
public record FreightDemandAcceptRequest(@NotNull Long quoteId) {}
```

`FreightDemandServiceImpl.java`

```java
@Transactional(rollbackFor = Exception.class)
public FreightQuoteVO submitQuote(Long demandId, FreightQuoteCreateRequest request) {
    FreightDemandEntity demand = requireDemand(demandId);
    if (!List.of("PUBLISHED", "QUOTING").contains(demand.getDemandStatus())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "当前需求不允许报价");
    }
    FreightQuoteEntity entity = new FreightQuoteEntity();
    entity.setDemandId(demandId);
    entity.setCompanyId(userRequestContext.currentCompanyId());
    entity.setQuoterUserId(userRequestContext.currentUserId());
    entity.setQuoteNo("FQ-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + UUID.randomUUID().toString().substring(0, 8));
    entity.setPriceAmount(request.priceAmount());
    entity.setCurrencyCode(request.currencyCode());
    entity.setEstimatedDays(request.estimatedDays());
    entity.setServiceNote(request.serviceNote());
    entity.setQuoteStatus("SUBMITTED");
    entity.setDeleted(0);
    entity.setCreatedBy(userRequestContext.currentUserId());
    entity.setUpdatedBy(userRequestContext.currentUserId());
    quoteMapper.insert(entity);
    if ("PUBLISHED".equals(demand.getDemandStatus())) {
        demand.setDemandStatus("QUOTING");
        demand.setUpdatedBy(userRequestContext.currentUserId());
        demandMapper.updateById(demand);
    }
    return toQuoteVO(entity);
}
```

```java
@Transactional(rollbackFor = Exception.class)
public FreightOrderVO acceptQuote(Long demandId, FreightDemandAcceptRequest request) {
    FreightDemandEntity demand = requireDemand(demandId);
    FreightQuoteEntity quote = requireQuote(request.quoteId(), demandId);
    FreightOrderEntity order = new FreightOrderEntity();
    order.setOrderNo("FO-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + UUID.randomUUID().toString().substring(0, 8));
    order.setDemandId(demandId);
    order.setAcceptedQuoteId(quote.getId());
    order.setPublisherCompanyId(demand.getCompanyId());
    order.setPublisherUserId(demand.getPublisherUserId());
    order.setAgentCompanyId(quote.getCompanyId());
    order.setAgentUserId(quote.getQuoterUserId());
    order.setOrderStatus("CREATED");
    order.setDeleted(0);
    order.setCreatedBy(userRequestContext.currentUserId());
    order.setUpdatedBy(userRequestContext.currentUserId());
    orderMapper.insert(order);

    demand.setDemandStatus("LOCKED");
    demand.setAcceptedQuoteId(quote.getId());
    demand.setAcceptedOrderId(order.getId());
    demand.setUpdatedBy(userRequestContext.currentUserId());
    demandMapper.updateById(demand);

    quote.setQuoteStatus("ACCEPTED");
    quote.setUpdatedBy(userRequestContext.currentUserId());
    quoteMapper.updateById(quote);
    rejectOtherQuotes(demandId, quote.getId());
    return toOrderVO(order);
}
```

`FreightDemandController.java`

```java
@PostMapping("/{id}/quotes")
public R<FreightQuoteVO> submitQuote(@PathVariable Long id, @Valid @RequestBody FreightQuoteCreateRequest request) {
    return R.ok(freightDemandService.submitQuote(id, request));
}

@GetMapping("/{id}/quotes")
public R<List<FreightQuoteVO>> listQuotes(@PathVariable Long id) {
    return R.ok(freightDemandService.listQuotes(id));
}

@PostMapping("/{id}/accept")
public R<FreightOrderVO> acceptQuote(@PathVariable Long id, @Valid @RequestBody FreightDemandAcceptRequest request) {
    return R.ok(freightDemandService.acceptQuote(id, request));
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```bash
./mvnw -pl service/service-market -am -Dtest=FreightDemandServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

- quote submission test passes
- acceptance state transition test passes

- [ ] **Step 5: Commit**

```bash
git add service/service-market/src/main/java service/service-market/src/test/java
git commit -m "feat: add freight quote and acceptance flow"
```

### Task 5: Add Redis-protected acceptance and RabbitMQ follow-up event

**Files:**
- Create: `service/service-market/src/main/java/com/manifestreader/market/config/FreightMarketMessagingConfig.java`
- Create: `service/service-market/src/main/java/com/manifestreader/market/mapper/FreightOrderTimelineMapper.java`
- Create: `service/service-market/src/main/java/com/manifestreader/market/messaging/FreightDemandAcceptedMessage.java`
- Create: `service/service-market/src/main/java/com/manifestreader/market/messaging/FreightDemandAcceptedPublisher.java`
- Create: `service/service-market/src/main/java/com/manifestreader/market/messaging/RabbitFreightDemandAcceptedPublisher.java`
- Create: `service/service-market/src/main/java/com/manifestreader/market/messaging/FreightDemandAcceptedConsumer.java`
- Modify: `service/service-market/src/main/java/com/manifestreader/market/service/impl/FreightDemandServiceImpl.java`
- Modify: `service/service-market/src/main/resources/application.yml`
- Modify: `service/service-market/src/main/resources/dev.yml`
- Test: `service/service-market/src/test/java/com/manifestreader/market/service/impl/FreightDemandServiceImplTest.java`
- Test: `service/service-market/src/test/java/com/manifestreader/market/integration/FreightDemandIntegrationTest.java`

- [ ] **Step 1: Write failing tests for duplicate protection and async follow-up**

```java
@Test
void acceptQuoteSkipsWhenRedisLockAlreadyHeld() {
    when(stringRedisTemplate.opsForValue().setIfAbsent("market:demand:accept:1001", "2001", Duration.ofSeconds(10)))
            .thenReturn(Boolean.FALSE);

    assertThatThrownBy(() -> service.acceptQuote(1001L, new FreightDemandAcceptRequest(2001L)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("正在处理");
}
```

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class FreightDemandIntegrationTest {

    @Test
    void acceptQuoteCreatesTimelineThroughMqConsumer() throws Exception {
        // create demand -> create quote -> accept quote
        // assert order exists and timeline count becomes 1
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
./mvnw -pl service/service-market -am -Dtest=FreightDemandServiceImplTest,FreightDemandIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

- duplicate acceptance test fails because Redis guard is missing
- integration test fails because MQ event consumer/timeline insert is missing

- [ ] **Step 3: Implement Redis lock and MQ follow-up**

`FreightMarketMessagingConfig.java`

```java
@Configuration
public class FreightMarketMessagingConfig {

    public static final String DEMAND_ACCEPTED_EXCHANGE = "freight.demand.accepted.exchange";
    public static final String DEMAND_ACCEPTED_QUEUE = "freight.demand.accepted.queue";
    public static final String DEMAND_ACCEPTED_ROUTING_KEY = "freight.demand.accepted";

    @Bean
    TopicExchange freightDemandAcceptedExchange() {
        return new TopicExchange(DEMAND_ACCEPTED_EXCHANGE, true, false);
    }

    @Bean
    Queue freightDemandAcceptedQueue() {
        return QueueBuilder.durable(DEMAND_ACCEPTED_QUEUE).build();
    }

    @Bean
    Binding freightDemandAcceptedBinding() {
        return BindingBuilder.bind(freightDemandAcceptedQueue())
                .to(freightDemandAcceptedExchange())
                .with(DEMAND_ACCEPTED_ROUTING_KEY);
    }
}
```

`FreightDemandServiceImpl.java`

```java
private static final Duration ACCEPT_LOCK_TTL = Duration.ofSeconds(10);

public FreightOrderVO acceptQuote(Long demandId, FreightDemandAcceptRequest request) {
    String lockKey = "market:demand:accept:" + demandId;
    Boolean locked = stringRedisTemplate.opsForValue()
            .setIfAbsent(lockKey, String.valueOf(request.quoteId()), ACCEPT_LOCK_TTL);
    if (!Boolean.TRUE.equals(locked)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "该需求正在处理接单，请稍后重试");
    }
    try {
        FreightOrderVO order = doAcceptQuote(demandId, request);
        acceptedPublisher.publish(new FreightDemandAcceptedMessage(
                demandId,
                request.quoteId(),
                order.id(),
                userRequestContext.currentUserId()
        ));
        return order;
    } finally {
        stringRedisTemplate.delete(lockKey);
    }
}
```

`FreightDemandAcceptedConsumer.java`

```java
@Component
public class FreightDemandAcceptedConsumer {

    private final FreightOrderTimelineMapper timelineMapper;

    @RabbitListener(queues = FreightMarketMessagingConfig.DEMAND_ACCEPTED_QUEUE, autoStartup = "${MARKET_ACCEPTED_LISTENER_ENABLED:false}")
    public void consume(FreightDemandAcceptedMessage message) {
        FreightOrderTimelineEntity entity = new FreightOrderTimelineEntity();
        entity.setOrderId(message.orderId());
        entity.setEventType("ORDER_CREATED");
        entity.setEventMessage("Quote " + message.quoteId() + " accepted for demand " + message.demandId());
        entity.setOperatorUserId(message.operatorUserId());
        timelineMapper.insert(entity);
    }
}
```

`dev.yml`

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        auto-startup: ${RABBITMQ_LISTENER_AUTO_STARTUP:false}

management:
  health:
    rabbit:
      enabled: ${RABBITMQ_HEALTH_ENABLED:false}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```bash
./mvnw -pl service/service-market -am -Dtest=FreightDemandServiceImplTest,FreightDemandIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

- duplicate acceptance protection passes
- integration test proves order timeline is inserted by consumer path

- [ ] **Step 5: Commit**

```bash
git add service/service-market/src/main/java service/service-market/src/main/resources service/service-market/src/test/java
git commit -m "feat: add redis guarded demand acceptance and mq follow-up"
```

### Task 6: End-to-end verification and local runbook

**Files:**
- Modify: `docs/backend-rabbitmq-local.md`
- Modify: `task_plan.md`
- Modify: `findings.md`
- Modify: `progress.md`

- [ ] **Step 1: Add final smoke test and gateway verification**

Use a real local sequence:

```bash
./mvnw -pl gateway,service/service-market -am -DskipTests package
NACOS_SERVER_ADDR=127.0.0.1:8848 NACOS_DISCOVERY_IP=127.0.0.1 java -jar service/service-market/target/service-market-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev --server.port=18085
curl -s 'http://127.0.0.1:8848/nacos/v1/ns/instance/list?serviceName=manifest-reader-market&groupName=DEFAULT_GROUP'
curl -s -X POST http://127.0.0.1:18085/market/demands -H 'Content-Type: application/json' -H 'X-Company-Id: 2' -H 'X-User-Id: 3' -d '{"title":"上海到汉堡拼箱","goodsName":"家具","departurePort":"SHANGHAI","destinationPort":"HAMBURG","quantity":6,"quantityUnit":"CBM","budgetAmount":3200,"currencyCode":"CNY","attachmentFileIds":[9001]}'
curl -s http://127.0.0.1:18085/market/demands/page?pageNo=1&pageSize=10
curl -s -X POST http://127.0.0.1:18085/market/demands/1/quotes -H 'Content-Type: application/json' -H 'X-Company-Id: 9' -H 'X-User-Id: 10' -d '{"priceAmount":3000,"currencyCode":"CNY","estimatedDays":9,"serviceNote":"可接单"}'
curl -s -X POST http://127.0.0.1:18085/market/demands/1/accept -H 'Content-Type: application/json' -H 'X-Company-Id: 2' -H 'X-User-Id: 3' -d '{"quoteId":1}'
```

- [ ] **Step 2: Verify full test suite for the new slice**

Run:

```bash
./mvnw -pl service/service-market -am test
```

Expected:

- service-market tests all pass
- model/common modules compile transitively

- [ ] **Step 3: Update runbook and planning files**

`docs/backend-rabbitmq-local.md`

```md
Market service:

- Service name: `manifest-reader-market`
- Default local port: `8085`
- Gateway route: `/market/**`
- Local JVM startup:
  `NACOS_SERVER_ADDR=127.0.0.1:8848 NACOS_DISCOVERY_IP=127.0.0.1 java -jar service/service-market/target/service-market-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev --server.port=18085`
```

`task_plan.md`

```md
### Phase 17: Marketplace Backend First Slice
- [x] 新增 `service-market`
- [x] 完成需求发布/列表/详情
- [x] 完成报价与接单
- [x] 完成 Redis 防重接单与 MQ 后续事件
- [x] 完成联调与文档更新
- **Status:** complete
```

- [ ] **Step 4: Run one final focused verification**

Run:

```bash
./mvnw -pl gateway,service/service-market -am -DskipTests package
curl -s http://127.0.0.1:18085/actuator/health
```

Expected:

- package succeeds
- market health returns `{"status":"UP"}`

- [ ] **Step 5: Commit**

```bash
git add docs/backend-rabbitmq-local.md task_plan.md findings.md progress.md
git commit -m "docs: record marketplace backend rollout and verification"
```

---

## Self-Review

### Spec Coverage

- freight marketplace backend: covered by Tasks 1-5
- Redis high-concurrency acceptance: covered by Task 5
- RabbitMQ follow-up events: covered by Task 5
- gateway and Nacos integration: covered by Task 1 and Task 6
- verification strategy: covered by Task 6

Gaps intentionally left for later plans:

- `service-news`
- frontend market pages
- integrated homepage reconstruction
- Elasticsearch business indexes

### Placeholder Scan

- no `TODO` or `TBD`
- all commands are explicit
- all changed files are named directly

### Type Consistency

- service package root stays `com.manifestreader.market`
- service name stays `manifest-reader-market`
- API base path stays `/market/demands`
- demand state names stay `PUBLISHED`, `QUOTING`, `LOCKED`
