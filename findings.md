# Findings & Decisions

## Requirements
- 用户要求先对主业务做消息驱动改造规划。
- 规划必须结合当前航运项目，而不是另起一个与现有系统脱节的新业务岛。
- 技术重点需要覆盖 `Redis`、消息队列、`Elasticsearch`、`Kibana` 以及高并发设计。
- 规划要能服务后续实施，并且适合用于“大厂后端实习”场景展示。
- 用户现在要求形成指导文档，并最终落地成一个集成“商城、新闻、提单导出”等能力的综合性平台。
- 用户明确希望参考 `黑马商城`、`黑马头条`、`黑马点评` 的源码思路。
- 用户现阶段进一步收窄范围，只先完成“商城端”，新闻社区和综合门户后置。
- 用户进一步明确：商城的管理归到管理端进行管理。
- 用户进一步补充：用户端可以管理自己的上架和接单，管理端负责统筹管理与审核。

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
- 当前根目录 `docker-compose.yml` 只纳入了 `RabbitMQ`，`MinIO` 仍是通过应用配置预留，未统一进同一 compose 项目。
- 当前“模板保存”接口 `POST /user/templates/extract/save` 仍为同步保存，不会产生 RabbitMQ 消息。
- Docker Desktop 中当前 compose 项目分组已经存在 `manifestreader -> rabbitmq`，用户希望 MinIO 也出现在同一分组下并列管理。
- MinIO 已被纳入根目录 compose 项目，当前 `manifest-reader-minio` 与 `manifest-reader-rabbitmq` 均处于 healthy 状态。
- 模板保存已纳入 `TEMPLATE_SAVE` 异步任务，前端提交后轮询任务状态，不再等待同步保存完成。
- Nacos 已纳入根目录 compose 项目，用于本地服务发现和后续微服务治理演进。
- 本地 JVM 启动时需要设置 `NACOS_DISCOVERY_IP=127.0.0.1`，否则 Nacos 可能注册 LAN IP，导致服务名调用出现 502。
- 五个后端服务的 `dev.yml` 已导入本地 Nacos Config，DataId 统一为 `manifest-reader-<service>-dev.yml`。
- `service-admin` 原先存在空 Feign 接口，管理端账单查询仍依赖本地 mock 数据，不是真正的跨服务调用。
- Nacos 中如果残留旧实例或 LAN IP 实例，Spring Cloud LoadBalancer 会随机选中旧地址，表现为偶发 502 或 Feign 调用失败。
- `黑马头条` 的公开项目说明显示其核心形态是 `SpringBoot + SpringCloud + Nacos + Gateway` 的资讯类微服务平台，适合作为“新闻社区”和“平台治理”参考。
- `黑马商城` 的公开仓库组织页显示存在 `hmall` 微服务版本，适合作为“交易链路、搜索、订单、MQ 解耦”参考。
- `黑马点评` 更适合作为 Redis 高并发玩法参考，例如点赞、热点、Feed 流、抢单、缓存穿透/击穿治理，而不适合作为当前项目的主业务模型照搬。
- 对当前项目来说，“商城”应抽象成“货运需求市场”，而不是传统实物电商。
- 对当前项目来说，“头条”应抽象成“行业资讯社区”，而不是泛内容平台。
- 当前执行范围已经收窄为“商城端优先完整交付”，`service-news` 只保留在远期蓝图中。
- 商城的前台与后台职责需要显式拆开：用户侧负责交易参与，管理端负责运营管理。
- 更准确的拆分应为：用户端负责“我的上架 / 我的接单 / 我的履约查看”，管理端负责“审核 / 统计 / 统筹监管 / 人工干预”。
- 商城前后台已经出现稳定的跨服务契约，公共 DTO / VO 继续散落在业务服务里会放大后续 Feign 和前端改造成本。
- 当前商城用户入口最适合收口在 `service-user` 的 `/user/market/**` 下，这样能与现有工作台接口命名保持一致。
- 当前 `service-user` 的全模块异步任务集成测试有 4 个基线失败，集中在提单解析、模板提取、模板导出成功态断言，不应与本轮商城入口层回归混在一起。
- 商城后端现在已经具备“发布 -> 审核 -> 报价 -> 接单 -> 开工 -> 完结”的基本闭环，同时支持需求取消和报价撤回。

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
| MinIO 统一纳入根目录 `docker-compose.yml`，但保持与 RabbitMQ 分离的独立 container | 统一项目分组和启动体验，同时避免把两个基础设施进程混塞进一个 container |
| Nacos 统一纳入根目录 `docker-compose.yml`，同样保持独立 container | 让本地环境具备服务发现能力，同时符合一容器一主进程的基础设施实践 |
| `service-user -> service-llm-task` 优先走 Nacos 服务发现，保留 `LLM_TASK_BASE_URL` 直连开关 | 兼顾微服务演进和本地排障效率 |
| Spring Cloud Alibaba 使用 `2025.0.0.0` 而不是 `2025.1.0.0` | 当前项目是 Spring Boot 3.5.13，`2025.0.0.0` 与 Boot 3.5 运行期兼容 |
| Nacos Config 使用 `${spring.application.name}-${spring.profiles.active}.yml` 作为 DataId | 统一配置中心命名，后续可自然扩展 `test`、`prod` 等环境 |
| Nacos Config import 使用 `optional:` 并保留 classpath `dev.yml` fallback | Nacos 暂不可用时本地服务仍可启动，降低开发环境耦合 |
| 本地 Nacos auth 关闭时客户端用户名密码默认留空 | 避免客户端向未启用认证的 Nacos 发送默认用户导致无效鉴权噪声 |
| 模板保存异步化继续复用 `bl_parse_task` 任务中心，而不是新起第二套任务表 | 可以沿用已有任务状态、消息发布、失败重试和查询模式 |
| `TEMPLATE_SAVE` 任务需要区分 `BILL_DOCX` 与 `BILL_PREVIEW` 两种保存策略 | 一类依赖 DOCX 资产，一类只需保存字段映射与预览资产，不能混用同一强约束 |
| 异步保存消费端显式传递 `companyId/userId` 到保存内核 | MQ 消费线程没有 HTTP 请求上下文，不能依赖请求头 fallback |
| `service-user` 只负责保存任务入口和查询，`service-llm-task` 负责保存任务投递与消费 | 保持用户服务轻量，将 LLM/模板重任务集中到任务服务 |
| 服务间同步控制面调用统一使用 Feign + Nacos 服务名 | 避免服务之间写死本地端口，为后续独立部署、扩容和网关负载均衡打基础 |
| `service-admin` 通过 `manifest-reader-user` Feign 查询用户账单摘要 | 管理端不再伪造用户侧业务数据，服务边界更清晰 |
| 网关默认使用 `lb://manifest-reader-*` 路由 | 入口流量通过 Nacos 发现后端实例，后续可自然支持多实例 |
| 异步重任务不改成同步 Feign 执行 | Feign 只提交任务和查状态，RabbitMQ 继续承担削峰、排队、重试和消费者隔离 |
| 综合平台优先级按照“货运需求交易 > 资讯社区 > 统一前端门户”排序 | 先做业务价值和技术亮点最强的链路，再做内容和门户聚合，交付风险更低 |
| 当前执行计划只先完成商城端 | 综合平台蓝图保留，但当前交付物只围绕 `service-market` 和商城页面闭环展开 |
| 商城管理统一落到管理端 | 后续审核、统计、履约管理、上下架/人工干预等入口应由 `service-admin` 承接 |
| 用户端保留“我的商品/需求管理”和“我的接单管理” | 这部分是交易参与者的日常操作，应保留在前台体验中 |
| `service-market` 适合作为新增交易服务 | 避免把需求发布、报价、接单、订单履约继续堆在 `service-user` 中 |
| `service-news` 适合作为新增资讯服务 | 便于隔离新闻抓取、评论、点赞、收藏、搜索等社区能力 |
| 提单导出、模板提取、模板保存继续作为平台核心生产力能力存在 | 这是当前项目最有辨识度的主业务，不能被新功能边缘化 |
| `service-market` 第一版采用“同步建单 + MQ 后续事件”模式 | 核心接单事务保持简单可靠，同时仍然具备异步化和削峰扩展点 |
| 市场相关表默认显式使用 `utf8mb4` | 货运需求标题、备注、港口等字段天然包含中文，不能依赖数据库默认字符集 |

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
| 模板保存同步逻辑在 `BILL_PREVIEW` 场景下曾因没有 DOCX 资产而保存失败 | 已通过生成轻量预览 JSON 资产兜底修复，同步逻辑已可用，为异步化提供了可复用保存内核 |
| 旧 `minio_new` 容器与 compose MinIO 端口冲突 | 已停止旧容器但保留数据，compose MinIO 使用旧 external volume 接管数据 |
| 本机 Mockito inline mock maker 无法 attach 到 Oracle JDK 21 VM | 已在 `service-user` 测试资源中切换为 subclass mock maker，恢复本地测试稳定性 |
| 本地 Nacos auth 关闭但客户端默认用户名为 `nacos` 时出现用户不存在日志 | 已将 `NACOS_USERNAME` / `NACOS_PASSWORD` 默认值调整为空，仅认证开启时显式配置 |
| 旧 `service-user` / `service-llm-task` 进程仍注册在 Nacos，导致 Feign 偶发打到旧实例失败 | 停止旧进程，仅保留 `127.0.0.1:18082` 与 `127.0.0.1:18084` 健康实例后，真实 Feign 调用恢复稳定 |
| `service-market` 首次真实 HTTP 联调时，`freight_demand` 表不存在 | 已通过 JDBC 执行 `V6__freight_market_init.sql` 到本地 `manifest_refactor` 库，随后发布/报价/接单链路打通 |
| `18085` 端口在本机同时被前端开发服务器占用 IPv4 | 后端 smoke 改到 `18086` 并绑定 `127.0.0.1`，避免前端页面干扰后端接口验证 |
| 综合平台目标过大，若并行推进商城、新闻、门户会稀释当前交付质量 | 已将当前计划正式收窄为“只先完成商城端” |
| 商城前端原先只有提单/模板工作台，市场页虽然插入模板但缺少完整方法闭环 | 已补齐真实 `/user/market/**` 调用、状态格式化、发布/报价/履约动作和移动端样式，并通过前端生产构建验证 |
| 管理端虽然已经有商城审核接口，但前端尚未承接，导致“管理归管理端”只停留在接口层 | 已新增 `frontend/admin` 审核工作台，并补充本地 mock fallback 与构建验证 |

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
  - `/Users/richard/CodeFile/Project/manifestReader/service/service-user/src/main/java/com/manifestreader/user/service/impl/RemoteTemplateSaveTaskService.java`
  - `/Users/richard/CodeFile/Project/manifestReader/service/service-llm-task/src/main/java/com/manifestreader/user/service/impl/TemplateSaveTaskServiceImpl.java`
  - `/Users/richard/CodeFile/Project/manifestReader/frontend/client/src/App.vue`

## Visual/Browser Findings
- 本轮未使用浏览器或图像资料。
