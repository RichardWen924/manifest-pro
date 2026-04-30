# Progress Log

## Session: 2026-04-29

### Phase 1: Requirements & Discovery
- **Status:** complete
- **Started:** 2026-04-29 11:12:19 CST
- Actions taken:
  - 读取 `planning-with-files` 模板与规则。
  - 确认项目根目录尚未存在规划文件。
  - 基于前序仓库探索结果，整理当前航运项目可复用的主业务链路和技术基础。
  - 明确本轮只做消息驱动改造规划，不进入代码实施。
- Files created/modified:
  - `task_plan.md`（created）
  - `findings.md`（created）
  - `progress.md`（created）

### Phase 2: Planning & Structure
- **Status:** complete
- Actions taken:
  - 确定以“单证解析与导出”作为消息驱动改造主线。
  - 确定 Redis、RabbitMQ、ES、Kibana 在整体方案中的职责分工。
  - 确定高并发设计落点为上传解析、导出生成、幂等去重、削峰填谷、限流保护。
- Files created/modified:
  - `task_plan.md`
  - `findings.md`
  - `progress.md`

### Phase 3: Delivery
- **Status:** in_progress
- Actions taken:
  - 准备向用户交付规划文件位置、当前结论和下一步建议。
- Files created/modified:
  - `task_plan.md`
  - `findings.md`
  - `progress.md`

### Phase 4: Message-Driven Implementation
- **Status:** complete
- Actions taken:
  - 新建 `codex/message-driven-core` 分支，避免直接在 `main` 上实施。
  - 按 TDD 先新增 `BillParseTaskServiceImplTest`，覆盖“提交任务”和“消费成功落状态”两个核心行为。
  - 为 `service-user` 引入 `spring-boot-starter-amqp` 和测试依赖。
  - 补齐 `bl_parse_task` 的 Java 实体、Mapper、任务服务、RabbitMQ 发布消费、Redis 缓存与请求上下文支持。
  - 在 `BillController` / `BillService` 中新增异步任务提交与查询接口，并让“确认保存提取结果”支持从异步任务结果回退取数。
  - 新增 `V4__bill_parse_task_async_enhance.sql`，补齐任务表增强字段。
- Files created/modified:
  - `model/src/main/java/com/manifestreader/model/entity/BlParseTaskEntity.java`
  - `service/service-user/pom.xml`
  - `service/service-user/src/main/java/com/manifestreader/user/controller/bill/BillController.java`
  - `service/service-user/src/main/java/com/manifestreader/user/service/BillService.java`
  - `service/service-user/src/main/java/com/manifestreader/user/service/impl/BillServiceImpl.java`
  - `service/service-user/src/main/java/com/manifestreader/user/service/impl/BillParseTaskServiceImpl.java`
  - `service/service-user/src/main/java/com/manifestreader/user/service/BillParseTaskService.java`
  - `service/service-user/src/main/java/com/manifestreader/user/messaging/*`
  - `service/service-user/src/main/java/com/manifestreader/user/cache/*`
  - `service/service-user/src/main/java/com/manifestreader/user/support/*`
  - `service/service-user/src/main/resources/dev.yml`
  - `service/service-user/src/test/java/com/manifestreader/user/service/impl/BillParseTaskServiceImplTest.java`
  - `zfile/sql/V4__bill_parse_task_async_enhance.sql`

### Phase 5: Backend Verification
- **Status:** complete
- Actions taken:
  - 依据 `senior-backend` 技能从 API 契约、数据迁移、消息可靠性和验证证据四个维度复核改造结果。
  - 重新执行 `./mvnw -pl service/service-user -am -Dtest=BillParseTaskServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`，确认测试仍然通过。
  - 检查控制器接口、任务服务实现和 SQL 迁移脚本，识别当前实现的剩余后端风险。
  - 完成可靠性修复：改为持久化文件资产驱动异步消费，补充发布失败和 `RUNNING` 状态处理。
  - 新增 `BillParseTaskIntegrationTest`，完成真实 HTTP + MySQL + Redis + Spring 消费链路连调。
- Files created/modified:
  - `progress.md`

### Phase 6: Local Startup Hardening
- **Status:** complete
- Actions taken:
  - 复盘 `service-user` 启动方式，确认根因之一是从聚合根 POM 直接执行 `spring-boot:run` 会因为找不到主类而失败。
  - 验证标准启动链路：先使用 `./mvnw -pl service/service-user -am package -DskipTests` 完整打包，再以可执行 JAR 启动 `service-user`。
  - 确认本地实际可用的基础依赖为 MySQL、Redis、MinIO，RabbitMQ 未启动。
  - 将 `BillParseTaskConsumer` 的 `@RabbitListener` 改为支持配置化 `autoStartup`，避免本地未安装 RabbitMQ 时持续重连刷错。
  - 调整 `dev.yml`，默认关闭 RabbitMQ listener 和 Rabbit 健康检查，并将对象存储默认切换为本地模式，降低本地启动门槛。
  - 使用 `java -jar ... --spring.profiles.active=dev` 重新验证启动，并确认 `GET /actuator/health` 返回 `UP`。
- Files created/modified:
  - `service/service-user/src/main/java/com/manifestreader/user/messaging/BillParseTaskConsumer.java`
  - `service/service-user/src/main/resources/dev.yml`
  - `progress.md`

### Phase 7: RabbitMQ Local Deployment
- **Status:** complete
- Actions taken:
  - 新增根目录 `docker-compose.yml`，自动部署 `rabbitmq:3.13-management`，开放 `5672/15672` 并附带健康检查。
  - 补充本地联调说明文档，约定开发环境下 RabbitMQ 的启停、管理台访问和服务启动开关。
  - 实际验证 RabbitMQ 容器可启动、管理台可访问，`service-user` 在开启 MQ 相关开关后能正常连接队列。
- Files created/modified:
  - `docker-compose.yml`
  - `docs/backend-rabbitmq-local.md`
  - `service/service-user/src/main/resources/dev.yml`

### Phase 8: Async Template Export
- **Status:** complete
- Actions taken:
  - 扩展 `bl_parse_task` 任务模型，新增 `task_type/biz_id/output_format/result_file_id` 字段并补充 `V5__async_task_extensions.sql`。
  - 为模板导出新增 RabbitMQ 发布消费、任务服务、任务查询和任务文件下载接口。
  - 将 `UserTemplateServiceImpl` 的同步导出逻辑抽成可复用执行内核，供同步接口和异步任务共同调用。
  - 新增 `TemplateExportTaskServiceImplTest` 与 `TemplateExportTaskIntegrationTest`，覆盖任务提交、消费成功、状态查询和文件下载。
- Files created/modified:
  - `model/src/main/java/com/manifestreader/model/entity/BlParseTaskEntity.java`
  - `service/service-user/src/main/java/com/manifestreader/user/controller/template/UserTemplateController.java`
  - `service/service-user/src/main/java/com/manifestreader/user/config/BillParseMessagingConfig.java`
  - `service/service-user/src/main/java/com/manifestreader/user/service/TemplateExportTaskService.java`
  - `service/service-user/src/main/java/com/manifestreader/user/service/impl/TemplateExportTaskServiceImpl.java`
  - `service/service-user/src/main/java/com/manifestreader/user/service/impl/UserTemplateServiceImpl.java`
  - `service/service-user/src/main/java/com/manifestreader/user/messaging/*TemplateExport*`
  - `service/service-user/src/main/java/com/manifestreader/user/model/vo/TemplateExportTask*.java`
  - `service/service-user/src/test/java/com/manifestreader/user/service/impl/TemplateExportTaskServiceImplTest.java`
  - `service/service-user/src/test/java/com/manifestreader/user/service/impl/TemplateExportTaskIntegrationTest.java`
  - `zfile/sql/V5__async_task_extensions.sql`

### Phase 9: Async Template Extract
- **Status:** complete
- Actions taken:
  - 为模板提取新增 RabbitMQ 发布消费、任务服务、任务查询、空白模板下载与预览接口。
  - 将同步模板提取重构为可复用执行内核，并把空白模板/预览文件持久化成对象存储资产，避免只存在 JVM 内存。
  - 让 `saveGeneratedTemplate` 支持直接消费异步提取任务结果，实现“异步提取 -> 预览/下载 -> 保存模板”的完整闭环。
  - 新增 `TemplateExtractTaskIntegrationTest`，覆盖提交任务、状态成功、空白模板下载、预览和模板保存。
- Files created/modified:
  - `service/service-user/src/main/java/com/manifestreader/user/controller/template/UserTemplateController.java`
  - `service/service-user/src/main/java/com/manifestreader/user/config/BillParseMessagingConfig.java`
  - `service/service-user/src/main/java/com/manifestreader/user/service/TemplateExtractTaskService.java`
  - `service/service-user/src/main/java/com/manifestreader/user/service/impl/TemplateExtractTaskServiceImpl.java`
  - `service/service-user/src/main/java/com/manifestreader/user/service/impl/UserTemplateServiceImpl.java`
  - `service/service-user/src/main/java/com/manifestreader/user/messaging/*TemplateExtract*`
  - `service/service-user/src/main/java/com/manifestreader/user/model/vo/TemplateExtractTask*.java`
  - `service/service-user/src/test/java/com/manifestreader/user/service/impl/TemplateExtractTaskIntegrationTest.java`

### Phase 10: SQL Repair & LLM Task Service Split
- **Status:** complete
- Actions taken:
  - 定位本地模板提取“不进 RabbitMQ”的真实根因是 `bl_parse_task` 缺少 `task_type/file_hash` 等迁移字段，而不是 MQ 资源缺失。
  - 使用本地 JDBC 脚本实际执行 `V4__bill_parse_task_async_enhance.sql` 和 `V5__async_task_extensions.sql`，补齐任务表结构。
  - 新增 `service-llm-task` 微服务模块，承接提单解析、模板提取、模板导出三类异步任务的提交、消费和状态查询。
  - 在 `service-user` 中新增 Feign 客户端和远程任务服务实现，把异步任务入口和查询统一代理到 `service-llm-task`。
  - 调整 `service-user` 默认配置，关闭本地 listener，让 MQ 消费职责收敛到 `service-llm-task`。
  - 完成双服务本地联调：`service-llm-task` 启动于 `8084`，`service-user` 启动于 `8082`，模板提取请求可经由 Feign 成功入队并进入 MQ 消费。
- Files created/modified:
  - `service/pom.xml`
  - `service/service-llm-task/pom.xml`
  - `service/service-llm-task/src/main/java/com/manifestreader/llmtask/LlmTaskApplication.java`
  - `service/service-llm-task/src/main/java/com/manifestreader/llmtask/controller/InternalBillTaskController.java`
  - `service/service-llm-task/src/main/java/com/manifestreader/llmtask/controller/InternalTemplateTaskController.java`
  - `service/service-llm-task/src/main/resources/application.yml`
  - `service/service-llm-task/src/main/resources/dev.yml`
  - `service/service-user/src/main/java/com/manifestreader/user/feign/LlmTaskFeignClient.java`
  - `service/service-user/src/main/java/com/manifestreader/user/service/impl/RemoteBillParseTaskService.java`
  - `service/service-user/src/main/java/com/manifestreader/user/service/impl/RemoteTemplateExportTaskService.java`
  - `service/service-user/src/main/java/com/manifestreader/user/service/impl/RemoteTemplateExtractTaskService.java`
  - `service/service-user/src/main/resources/application.yml`
  - `zfile/sql/V4__bill_parse_task_async_enhance.sql`
  - `zfile/sql/V5__async_task_extensions.sql`

## Test Results
| Test | Input | Expected | Actual | Status |
|------|-------|----------|--------|--------|
| 规划文件存在性检查 | `ls task_plan.md findings.md progress.md` | 若不存在则初始化 | 初始检查发现三文件均不存在，已完成创建 | ✓ |
| 提单异步任务单测 | `./mvnw -pl service/service-user -am -Dtest=BillParseTaskServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` | 任务提交和消费测试通过 | 2 个测试全部通过 | ✓ |
| 提单异步任务复验 | `./mvnw -pl service/service-user -am -Dtest=BillParseTaskServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` | 改造后复验仍通过 | 2 个测试全部通过 | ✓ |
| 提单异步链路连调 | `./mvnw -pl service/service-user -am -Dtest=BillParseTaskIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` | HTTP 提交、DB 持久化、Redis 状态、消费结果查询全部通过 | 1 个集成测试通过，构建成功 | ✓ |
| user 服务标准打包 | `./mvnw -pl service/service-user -am package -DskipTests` | 产出可执行 JAR | 构建成功，生成 `service-user-0.0.1-SNAPSHOT.jar` | ✓ |
| user 服务本地启动 | `java -jar service/service-user/target/service-user-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev` | 本地依赖不完整时仍能启动 Web 服务 | 成功启动在 `8082`，Rabbit listener 未自动拉起 | ✓ |
| user 服务健康检查 | `curl -s http://127.0.0.1:8082/actuator/health` | 返回健康状态 | 返回 `{"status":"UP"}` | ✓ |
| RabbitMQ Compose 配置检查 | `docker compose config` | 配置合法 | 校验通过 | ✓ |
| RabbitMQ 本地启动 | `docker compose up -d rabbitmq` | Broker 容器健康可用 | 管理版 RabbitMQ 成功启动并健康 | ✓ |
| 模板导出任务单测 | `./mvnw -pl service/service-user -am -Dtest=TemplateExportTaskServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` | 提交与消费测试通过 | 2 个测试全部通过 | ✓ |
| 模板导出控制器测试 | `./mvnw -pl service/service-user -am -Dtest=UserTemplateControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` | 中文文件名响应头编码正确 | 3 个测试全部通过 | ✓ |
| 模板导出链路连调 | `./mvnw -pl service/service-user -am -Dtest=TemplateExportTaskIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` | HTTP 提交、DB 落库、消息消费、任务下载全部通过 | 1 个集成测试通过 | ✓ |
| 模板提取链路连调 | `./mvnw -pl service/service-user -am -Dtest=TemplateExtractTaskIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` | HTTP 提交、任务成功、模板下载/预览/保存全部通过 | 1 个集成测试通过 | ✓ |
| 模板异步链路组合验证 | `./mvnw -pl service/service-user -am -Dtest=UserTemplateControllerTest,TemplateExportTaskServiceImplTest,TemplateExportTaskIntegrationTest,TemplateExtractTaskIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` | 控制器、导出、提取任务链路一起通过 | 7 个测试全部通过 | ✓ |
| 最新整包构建 | `./mvnw -pl service/service-user -am package -DskipTests` | 模板异步化完成后仍可正常打包 | 构建成功 | ✓ |
| 任务中心双模块编译 | `./mvnw -pl service/service-llm-task,service/service-user -am -DskipTests compile` | `service-user` 与 `service-llm-task` 一起编译通过 | 构建成功 | ✓ |
| 任务中心双模块打包 | `./mvnw -pl service/service-llm-task,service/service-user -am package -DskipTests` | 两个服务都能生成可执行 JAR | 构建成功 | ✓ |
| llm-task 健康检查 | `curl -s http://127.0.0.1:8084/actuator/health` | 任务中心服务健康 | 返回 `{"status":"UP"}` | ✓ |
| user 健康检查（Feign 模式） | `curl -s http://127.0.0.1:8082/actuator/health` | 用户服务健康 | 返回 `{"status":"UP"}` | ✓ |
| Phase D 模板提取提交 | `curl -s -X POST ... http://127.0.0.1:8082/user/templates/extract/tasks` | 请求由 `service-user` 代理到 `service-llm-task` 并成功提交任务 | 返回 `taskNo=TPL-EXTRACT-20260430180534-affc`，状态 `PENDING` | ✓ |
| Phase D 模板提取查询 | `curl -s ... http://127.0.0.1:8082/user/templates/extract/tasks/TPL-EXTRACT-20260430180534-affc` | 可经由 `service-user` 查询远端任务状态 | 返回状态 `RUNNING` | ✓ |
| RabbitMQ 队列消费观测 | `curl -s -u guest:guest http://127.0.0.1:15672/api/queues/%2F/template.extract.queue` | 提交后可见 `publish/deliver` 增加且存在活跃消费者 | `publish=2`、`deliver=2`、`messages_unacknowledged=1` | ✓ |

## Error Log
| Timestamp | Error | Attempt | Resolution |
|-----------|-------|---------|------------|
| 2026-04-29 11:12:19 CST | 规划文件不存在 | 1 | 依据技能模板初始化三份规划文件 |
| 2026-04-29 11:17:51 CST | `mvn: command not found` | 1 | 改用 `./mvnw` |
| 2026-04-29 11:24:23 CST | 单模块构建找不到 `BlParseTaskEntity` | 1 | 使用 `-am` 联动依赖模块编译 |
| 2026-04-29 11:24:51 CST | `BaseMapper.insert` Mockito 断言重载歧义 | 1 | 在测试中显式标注 `BlParseTaskEntity` 类型 |
| 2026-04-29 14:15:07 CST | RabbitMQ Testcontainers 镜像拉取出现 Docker Registry `EOF` | 1 | 保留生产 MQ 代码路径，测试改为更稳定的本地消费直连连调方案 |
| 2026-04-29 14:27:28 CST | 在根 POM 执行 `spring-boot:run` 找不到主类 | 1 | 改为先从根工程 `-pl service/service-user -am package`，再用可执行 JAR 启动 |
| 2026-04-29 14:32:59 CST | 本地未启动 RabbitMQ 导致 `actuator/health` 为 `DOWN` | 1 | `dev` 环境默认关闭 Rabbit listener 与 Rabbit 健康检查 |
| 2026-04-30 12:15:49 CST | 模板提取集成测试里预览资产 `file_hash` 超出字段长度 | 1 | 缩短测试数据中的 `file_hash` 值后重跑通过 |
| 2026-04-30 17:41:03 CST | 本地库 `bl_parse_task` 缺少 `task_type` 导致模板提取请求在发 MQ 前插入失败 | 1 | 使用 JDBC 执行 `V4` 与 `V5` 迁移，补齐字段后请求可成功入队 |
| 2026-04-30 18:02:14 CST | `service-llm-task` 编译缺少 MinIO 依赖 | 1 | 为新模块补充 `io.minio:minio` 依赖后重新编译通过 |
| 2026-04-30 18:03:21 CST | `service-llm-task` 仅扫描 `com.manifestreader.llmtask`，导致任务实现和 Mapper 无法注入 | 1 | 将 `scanBasePackages` 扩大到 `com.manifestreader` 后服务成功启动 |

## 5-Question Reboot Check
| Question | Answer |
|----------|--------|
| Where am I? | RabbitMQ 本地部署、三条异步任务链路，以及 `service-llm-task` 微服务拆分第一版都已完成并联调通过 |
| Where am I going? | 下一步继续收敛共享代码、补齐 `service-admin` 对任务中心的调用，并清理跨服务复制代码 |
| What's the goal? | 将航运主业务逐步改造成带 Redis 与 MQ 的消息驱动架构，并最终沉淀成独立任务中心服务 |
| What have I learned? | 统一任务模型能支撑平滑服务拆分，而先修复数据库迁移和启动边界，再拆 Feign/消费职责，会显著降低微服务演进风险 |
| What have I done? | 已完成本地 MQ 部署、SQL 迁移修复、模板导出/提取异步化、`service-llm-task` 拆分、双服务编译打包和 Feign+MQ 联调验证 |
