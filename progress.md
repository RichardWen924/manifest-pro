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

## 5-Question Reboot Check
| Question | Answer |
|----------|--------|
| Where am I? | 第一阶段消息驱动改造已完成，并且已经通过单测和连调验证 |
| Where am I going? | 继续扩展模板导出异步化、ES 搜索同步、任务监控与前端接线，同时保持本地开发启动体验稳定 |
| What's the goal? | 将航运主业务逐步改造成带 Redis 与 MQ 的消息驱动架构 |
| What have I learned? | 持久化文件资产 + DB/Redis 状态组合能显著提升异步解析链路可靠性，Rabbit 能力也需要为本地 dev 启动提供降级开关 |
| What have I done? | 已完成可靠性修复、任务链路代码实现、单测验证、HTTP/DB/缓存/消费连调，以及 user 服务本地启动加固 |
