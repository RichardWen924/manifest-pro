# Findings & Decisions

## Requirements
- 用户要求先对主业务做消息驱动改造规划。
- 规划必须结合当前航运项目，而不是另起一个与现有系统脱节的新业务岛。
- 技术重点需要覆盖 `Redis`、消息队列、`Elasticsearch`、`Kibana` 以及高并发设计。
- 规划要能服务后续实施，并且适合用于“大厂后端实习”场景展示。

## Research Findings
- 当前项目是 Maven 多模块结构，核心模块为 `gateway`、`service-auth`、`service-admin`、`service-user`、`common`、`model`。
- 已有业务能力包括 JWT 鉴权、MinIO 文件存储、Dify 工作流调用、提单与模板相关实体模型。
- 当前更偏“同步业务服务 + 局部缓存/内存状态”，尚未形成统一任务中心和消息驱动链路。
- `service-user` 中存在文件上传、提单抽取、模板导出等天然适合异步化的高耗时场景。
- 模板提取链路原先依赖 JVM 内存中的 `extractResultCache` / `blankTemplateCache`，在多实例或重启场景下无法稳定支撑异步闭环。
- 项目已接入 Redis 依赖与基础配置，但尚未把 Redis 用作核心架构能力。
- 仓库中未见测试代码，说明后续实施要把验证方案显式纳入规划。
- 第一阶段最适合落地的改造点是 `service-user` 的提单解析链路，因为它本身已经依赖 Dify 和文件上传，天然具备异步化价值。
- 将“任务源文件”从本地临时路径改成 `file_asset + object storage` 持久化引用后，异步消费不再依赖提交请求所在实例的本地磁盘。

## Technical Decisions
| Decision | Rationale |
|----------|-----------|
| 主业务链路定义为“上传 -> 解析 -> 入库 -> 导出 -> 通知 -> 搜索” | 能最大化复用现有提单、模板、文件、Dify 能力 |
| 采用任务中心承载异步状态 | 便于统一管理解析、导出、通知、ES 同步等任务 |
| RabbitMQ 承担事件分发、削峰与重试 | 与当前项目复杂度匹配，也适合展示消息驱动设计 |
| Redis 用于缓存、幂等、锁、限流和任务状态 | 与高并发主线直接绑定，业务价值明确 |
| ES 建立业务索引与日志索引双用途 | 让搜索和分析都能落到实际场景 |
| Kibana 用于异常排查、审计追踪、流量分析 | 可以直观展示链路追踪与问题定位能力 |
| 第一阶段保留原同步 `/extract` 接口，同时新增异步任务接口 | 降低改造风险，给前端和平滑迁移留缓冲 |
| 任务状态先采用“DB 持久化 + Redis 热缓存”的组合 | 既能查询历史状态，也能支撑高频轮询 |
| 发布链路失败时即时将任务标记为 `FAILED` | 避免保留“永远 PENDING”的脏任务状态 |
| 消费开始时立即落 `RUNNING` 状态并缓存 | 让轮询端看到更真实的中间状态，便于后续监控与补偿 |
| 连调验证使用 MySQL/Redis Testcontainers + MockMvc + Spring 消费直连 | 在当前环境里比强依赖外部 Rabbit 镜像更稳定，仍能覆盖 HTTP/DB/缓存/消费主链路 |
| 模板提取和模板导出都沿用 `bl_parse_task` 统一任务表扩展 `task_type` | 先复用任务中心骨架，降低第二阶段拆服务前的模型迁移成本 |
| 空白模板、预览文件、导出文件都落成 `file_asset + object storage` | 避免结果只存在内存或本地临时目录，支持异步消费和后续跨服务读取 |
| Phase D 先采用 `Feign + 独立 task service + RabbitMQ`，暂不引入 Dubbo | 当前项目仍以 HTTP 控制面 + MQ 异步执行面为主，复杂度更可控 |
| `service-user` 仅保留任务入口、查询和文件下载编排，MQ listener 收敛到 `service-llm-task` | 先把高并发调用和 LLM 负载集中到独立服务，降低 `service-user` 职责膨胀 |

## Issues Encountered
| Issue | Resolution |
|-------|------------|
| 规划文件尚未初始化 | 已在项目根目录创建 `task_plan.md`、`findings.md`、`progress.md` |
| 用户先前提到点评/新闻/秒杀方向，但最终要求结合现有航运主线 | 统一收敛为“航运单证平台消息驱动升级”，只借鉴其技术栈设计思路 |
| 项目原有 `bl_parse_task` 只有 SQL 草案，没有 Java 实体/Mapper/服务闭环 | 已补齐实体、Mapper、任务服务、消息发布消费、Redis 缓存和控制器接口 |
| 模块化 Maven 工程在单模块验证时不会自动带上新依赖模块源码 | 改为 `./mvnw -pl service/service-user -am ...` 联动验证 |
| RabbitMQ 容器首次镜像拉取受外部 Docker Registry 网络波动影响 | 已保留外部 MQ 代码实现，同时将测试验证改为更稳定的本地连调路径 |
| 模板提取链路原有保存模板逻辑只认内存缓存中的 `extractId` | 已让保存逻辑支持从异步任务结果回退取数，确保 `taskNo` 也能走完整闭环 |
| 本地数据库未执行最新迁移，导致异步模板提取在入库阶段失败而不是 MQ 阶段失败 | 已执行 `V4__bill_parse_task_async_enhance.sql` 和 `V5__async_task_extensions.sql`，现在可正常提交并入队 |
| `service-llm-task` 为了尽快完成拆分，当前复用了部分 `service-user` 代码到新模块，存在后续收敛共享库的空间 | 第一版目标是先打通微服务边界和联调链路，后续再考虑抽取 `task-core` 或共享 starter |

## Resources
- 项目根目录：`/Users/richard/CodeFile/Project/manifestReader`
- 规划文件：
  - `/Users/richard/CodeFile/Project/manifestReader/task_plan.md`
  - `/Users/richard/CodeFile/Project/manifestReader/findings.md`
  - `/Users/richard/CodeFile/Project/manifestReader/progress.md`
- 关键新增文件：
  - `/Users/richard/CodeFile/Project/manifestReader/service/service-user/src/main/java/com/manifestreader/user/service/impl/BillParseTaskServiceImpl.java`
  - `/Users/richard/CodeFile/Project/manifestReader/service/service-user/src/main/java/com/manifestreader/user/messaging/BillParseTaskConsumer.java`
  - `/Users/richard/CodeFile/Project/manifestReader/service/service-user/src/main/java/com/manifestreader/user/cache/RedisBillParseTaskCacheService.java`
  - `/Users/richard/CodeFile/Project/manifestReader/zfile/sql/V4__bill_parse_task_async_enhance.sql`
  - `/Users/richard/CodeFile/Project/manifestReader/service/service-user/src/test/java/com/manifestreader/user/integration/BillParseTaskIntegrationTest.java`
  - `/Users/richard/CodeFile/Project/manifestReader/docker-compose.yml`
  - `/Users/richard/CodeFile/Project/manifestReader/docs/backend-rabbitmq-local.md`
  - `/Users/richard/CodeFile/Project/manifestReader/service/service-user/src/main/java/com/manifestreader/user/service/impl/TemplateExportTaskServiceImpl.java`
  - `/Users/richard/CodeFile/Project/manifestReader/service/service-user/src/main/java/com/manifestreader/user/service/impl/TemplateExtractTaskServiceImpl.java`
  - `/Users/richard/CodeFile/Project/manifestReader/service/service-user/src/test/java/com/manifestreader/user/service/impl/TemplateExportTaskIntegrationTest.java`
  - `/Users/richard/CodeFile/Project/manifestReader/service/service-user/src/test/java/com/manifestreader/user/service/impl/TemplateExtractTaskIntegrationTest.java`
  - `/Users/richard/CodeFile/Project/manifestReader/service/service-llm-task/src/main/java/com/manifestreader/llmtask/LlmTaskApplication.java`
  - `/Users/richard/CodeFile/Project/manifestReader/service/service-llm-task/src/main/java/com/manifestreader/llmtask/controller/InternalTemplateTaskController.java`
  - `/Users/richard/CodeFile/Project/manifestReader/service/service-llm-task/src/main/java/com/manifestreader/llmtask/controller/InternalBillTaskController.java`
  - `/Users/richard/CodeFile/Project/manifestReader/service/service-user/src/main/java/com/manifestreader/user/feign/LlmTaskFeignClient.java`

## Visual/Browser Findings
- 本轮未使用浏览器或图像资料。
