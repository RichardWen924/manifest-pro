# Task Plan: 综合航运平台改造规划

## Goal
当前执行目标收敛为：优先把 `manifestReader` 的“货运需求商城端”完整做完。在保留提单解析、模板提取、模板导出等核心能力的前提下，优先完成货运需求发布、报价、接单、履约扩展、商城端接口与页面闭环，以及配套的微服务治理、异步化和高并发设计。新闻社区和综合门户保留为后续阶段，不进入当前交付范围。

## Current Phase
Phase 19 planned

## Phases
### Phase 1: Requirements & Discovery
- [x] 明确用户目标：先做主业务消息驱动改造规划
- [x] 明确约束：结合现有航运项目，不新增割裂的电商孤岛模块
- [x] 记录现有系统结构与可复用能力
- [x] 将发现写入 `findings.md`
- **Status:** complete

### Phase 2: Planning & Structure
- [x] 确定主链路：文件上传 -> 异步解析 -> 结构化入库 -> 导出 -> 通知 -> 检索
- [x] 确定技术栈分工：Redis / MQ / ES / Kibana / 监控
- [x] 确定高并发场景与优先级
- [x] 形成阶段化改造路线
- **Status:** complete

### Phase 3: Message-Driven Blueprint
- [x] 细化事件模型、任务中心、消息主题与消费职责
- [x] 细化缓存、幂等、锁、限流设计
- [ ] 细化 ES 同步与日志采集链路
- **Status:** in_progress

### Phase 4: Execution Plan
- [x] 将蓝图拆成可实施的开发阶段
- [x] 标注每阶段涉及表结构、接口、服务改动
- [x] 标注验证方式与压测方式
- **Status:** complete

### Phase 5: Delivery
- [x] 审阅规划文件一致性
- [x] 向用户交付规划摘要与下一步建议
- **Status:** complete

### Phase 11: MinIO Compose Consolidation Plan
- [x] 将 MinIO 纳入根目录 `docker-compose.yml`
- [x] 统一 RabbitMQ / MinIO 的本地启动、健康检查和持久化卷
- [x] 统一 `service-user` / `service-llm-task` 的 dev 环境对象存储配置
- [x] 补充本地联调文档与验证步骤
- **Status:** complete

### Phase 12: Async Template Save Plan
- [x] 将 `POST /user/templates/extract/save` 改为异步任务提交
- [x] 为模板保存新增 `TEMPLATE_SAVE` 任务类型、消息模型和队列
- [x] 将模板定义、版本、字段映射、文件资产写入迁移到消费端
- [x] 为 `DOCX` / `PREVIEW` 两类模板定义不同的异步保存策略
- [x] 新增任务状态查询接口与前端轮询方案
- **Status:** complete

### Phase 13: Nacos Service Discovery
- [x] 将 Nacos 纳入根目录 `docker-compose.yml`
- [x] 为 `service-user` / `service-llm-task` / `gateway` 接入 Nacos Discovery
- [x] 将 `service-user -> service-llm-task` 的 Feign 调用改为优先走服务名发现
- [x] 本地启动 Nacos 并验证服务注册
- [x] 使用真实接口验证 Feign 通过 Nacos 调用任务中心
- **Status:** complete

### Phase 14: Nacos Config Center
- [x] 为 `gateway`、`service-user`、`service-llm-task`、`service-auth`、`service-admin` 接入 Nacos Config
- [x] 将各服务本地 `dev.yml` 导入 Nacos，形成统一 DataId 规则
- [x] 保留 classpath `dev.yml` 作为 optional fallback，降低本地启动风险
- [x] 重新打包核心后端模块，验证配置中心依赖兼容
- [x] 启动 `service-user` / `service-llm-task` 并验证从 Nacos 拉取配置
- [x] 使用真实保存任务验证 `service-user -> Nacos -> service-llm-task -> RabbitMQ` 链路
- **Status:** complete

### Phase 15: Service-wide Feign & Discovery Governance
- [x] 将 `service-auth`、`service-admin` 纳入 Nacos Discovery
- [x] 将网关默认路由从固定 HTTP 地址改为 `lb://` 服务发现路由
- [x] 将 admin 侧空 Feign 接口补齐为真实 `AuthFeignClient` / `UserFeignClient`
- [x] 新增 user 内部账单分页接口，供 admin 通过 Feign 查询用户侧业务数据
- [x] 使用真实 HTTP 验证 `service-admin -> manifest-reader-user` Feign 链路
- [x] 增加 Feign / Discovery 契约测试，防止后续退回固定地址或空接口
- **Status:** complete

### Phase 16: Integrated Platform Blueprint
- [x] 明确新平台目标：商城 + 资讯 + 提单导出一体化
- [x] 参考黑马商城 / 黑马头条 / 黑马点评，抽取可复用的架构模式
- [x] 确定综合平台的优先级排序与分阶段实施顺序
- [x] 输出指导文档与设计蓝图
- [x] 等待用户确认书面 spec 后进入实现
- **Status:** complete

### Phase 17: Marketplace Backend Implementation Plan
- [x] 将综合平台拆分为独立可执行子计划
- [x] 先输出 `service-market` 后端首期实施计划
- [x] 明确新模块、SQL、Redis、RabbitMQ、Nacos、网关改动范围
- [x] 明确测试、联调与文档更新步骤
- [x] 用户确认后进入编码执行
- **Status:** complete

### Phase 18: Marketplace Backend First Slice Delivery
- [x] 新增 `service-market` 微服务并接入网关路由
- [x] 完成货运需求发布、列表、详情接口
- [x] 完成报价提交、报价列表、接单下单接口
- [x] 完成 Redis 防重接单与 RabbitMQ 后续事件
- [x] 修复市场表字符集为 `utf8mb4`，支持中文需求标题
- [x] 完成单测、集成测试、打包验证和本地服务 smoke
- [x] 更新本地运行文档与阶段记录
- **Status:** complete

### Phase 19: Marketplace-only Scope Reset
- [x] 将当前执行目标从“综合平台并行推进”调整为“商城端优先完整交付”
- [x] 明确新闻社区与综合门户全部顺延，不进入当前执行范围
- [x] 将后续阶段重排为“商城后端完善 -> 商城前端重构 -> 商城联调验收”
- [x] 同步更新指导文档、实施计划和阶段记录
- **Status:** complete

### Phase 20: Marketplace Backend Completion
- [ ] 完成商城端剩余后端能力：
- [x] 用户端商品/货运需求我的发布管理
- [x] 用户端接单、我的接单管理
- [x] 履约状态流转
- [x] 需求取消/完结
- [x] 报价撤回/拒绝
- [ ] 商城搜索、筛选、排序
- [ ] 商城管理视角与统计接口
- [x] 将商城管理能力明确落到 `service-admin`
- [x] 增加 `service-admin -> service-market` Feign 管理接口
- [x] 增加管理端审核流：待审核、审核通过、审核驳回
- [x] 增加 `service-user -> service-market` Feign 用户入口改造
- [x] 商城共享 DTO / VO 下沉到 `model`
- [x] 前端用户端 / 管理端商城 API 调用层补齐
- [ ] 增补 Redis 热点缓存、限流、排行榜和补偿策略
- **Status:** in_progress

### Phase 21: Marketplace Frontend Completion
- [x] 重构商城首页、需求列表、详情、报价、接单、履约页面
- [x] 接通真实 `service-market` 接口，移除页面级 mock
- [x] 统一商城端导航、状态反馈、任务提示和移动端适配
- [x] 管理端新增商城审核管理页面并接通 `/admin/market/**`
- **Status:** complete

### Phase 22: Marketplace Integration Acceptance
- [ ] 完成商城端全链路联调
- [ ] 完成 RabbitMQ / Redis / Nacos 本地运行回归
- [ ] 输出商城端交付文档与演示路径
- **Status:** in_progress

## Key Questions
1. 如何在不破坏现有业务的前提下，把同步链路改成消息驱动？
2. Redis 在本项目里最值得承担哪些职责，而不是只作为“技术点”出现？
3. 哪些高并发场景能自然贴合航运单证业务，而不是生搬硬套秒杀？
4. ES 和 Kibana 应该如何同时服务业务检索与日志分析？
5. 综合平台里，应该先做交易闭环、资讯闭环，还是先做统一平台骨架？

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| 不新增独立点评/新闻主模块 | 用户要求结合当前航运项目，避免项目叙事割裂 |
| 主业务链路围绕单证解析与导出改造成消息驱动 | 现有项目已经具备 MinIO、Dify、提单/模板模型，最适合做异步闭环 |
| MQ 优先选 RabbitMQ | 足够支撑异步解耦、削峰填谷、重试和死信，学习与实现成本更适中 |
| Redis 优先承担缓存、幂等、分布式锁、任务状态、限流职责 | 与高并发设计直接相关，且最容易形成可讲的面试亮点 |
| ES 同时服务业务搜索和日志分析 | 避免“只上日志栈”，让技术引入更有业务价值 |
| 第一阶段先改造提单异步解析，再沿同一任务骨架扩展模板导出与模板提取 | 可以复用任务中心、MQ、对象存储与状态流转能力，逐步减少同步重逻辑 |
| 连调测试先采用“真实 HTTP + MySQL + Redis + Spring 消费链路”，暂不强绑外部 Rabbit 容器 | 当前环境下 Rabbit 镜像首次拉取不稳定，但主业务异步编排已经可以通过测试注入方式验证完整链路 |
| 本地 RabbitMQ 优先用 Docker Compose 部署 `rabbitmq:3.13-management` | 部署与联调门槛低，便于展示队列、消费者和堆积情况 |
| Nacos 作为独立 container 纳入同一 compose 项目，而不是塞进 RabbitMQ/MinIO container | 保持基础设施进程隔离，同时满足 Docker Desktop 同项目分组管理 |
| Feign 优先使用 Nacos 服务发现，`LLM_TASK_BASE_URL` 仅作为调试兜底 | 更接近后续微服务拆分形态，同时保留本地直连排障能力 |
| 本地 JVM 启动时将 `NACOS_DISCOVERY_IP` 固定为 `127.0.0.1` | 避免 Nacos 自动注册 LAN IP 后，本机服务发现链路出现 502 |
| Nacos Config 使用 `${spring.application.name}-${spring.profiles.active}.yml` 作为 DataId | 与 Spring Cloud Alibaba 默认约定接近，便于后续多环境扩展 |
| 本地 Nacos Config import 使用 `optional:` 并保留 classpath `dev.yml` | Nacos 不可用时仍能启动本地服务，避免配置中心成为开发单点阻塞 |
| 本地 Nacos auth 关闭时，客户端用户名密码默认留空 | 避免客户端向未启用认证的 Nacos 发送默认用户导致无效鉴权噪声 |
| 服务间同步调用统一优先使用 Feign + Nacos 服务名 | 让 admin/user/auth/llm-task 的控制面调用具备微服务拆分形态，避免写死本地端口 |
| 高耗时 LLM 提取、导出、保存继续走 RabbitMQ 异步链路 | Feign 只负责提交任务和查询状态，避免突发流量直接压垮 LLM 处理链路 |
| 当前迭代只做商城端完成，新闻社区与综合门户顺延 | 先把最贴近主业务、最容易形成完整闭环的部分真正做完，避免范围扩散 |
| `service-market` 第一阶段只做“同步主事务 + MQ 后续事件”，不急于拆更多协作服务 | 先把交易闭环和高并发亮点跑通，再演进履约、通知、搜索等扩展能力 |
| 市场表建表脚本显式使用 `utf8mb4` | 避免中文货运标题在 MySQL 默认 latin1 环境下插入失败 |
| 商城管理归管理端负责 | 前台用户侧只保留发布、浏览、报价、接单等能力，审核、统计、履约管理统一落到 `service-admin` |
| 用户端可以管理自己的上架和接单 | “我的发布”“我的接单”属于前台交易操作，不属于后台运营管理 |
| 商城跨服务共享 DTO / VO 统一下沉到 `model` | 减少 `service-market`、`service-user`、`service-admin` 之间的契约漂移，降低 Feign 改造成本 |

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
| `task_plan.md/findings.md/progress.md` 不存在 | 1 | 按 `planning-with-files` 规范初始化三份规划文件 |
| 本机没有 `mvn` 命令 | 1 | 改用仓库自带 `./mvnw` 执行 Maven 构建 |
| `-pl service/service-user` 未联动编译 `model` 新实体 | 1 | 使用 `-am` 联动依赖模块一起编译测试 |
| `BaseMapper.insert` 测试断言命中重载歧义 | 1 | 在 `argThat` 中显式标注 `BlParseTaskEntity` 类型 |
| Testcontainers 集成测试首次拉取 RabbitMQ 镜像时遇到 Docker Registry `EOF` | 1 | 调整为更轻量的本地连调路径，保留 MySQL/Redis 容器并用测试注入替代外部 Rabbit 投递 |
| 独立 `minio_new` 容器占用 `9000/9001` 端口 | 1 | 停止旧容器但保留数据卷，compose MinIO 复用旧 volume 并健康启动 |
| Mockito inline mock maker 在本机 JDK 21 无法 attach | 1 | 增加测试资源切换到 subclass mock maker，目标单测通过 |
| `spring-cloud-alibaba 2025.1.0.0` 与当前 Spring Boot 3.5.13 运行期不兼容 | 1 | 切换到适配 Boot 3.5 的 `2025.0.0.0`，并执行 `clean package` 清理旧 fat jar 依赖 |
| Nacos 默认注册 LAN IP 后，`service-user` 通过服务发现调用任务中心返回 502 | 1 | 本地启动增加 `NACOS_DISCOVERY_IP=127.0.0.1`，真实接口验证通过 |
| 本地 Nacos auth 关闭但客户端默认用户名为 `nacos` 时出现用户不存在日志 | 1 | 将 `NACOS_USERNAME` / `NACOS_PASSWORD` 默认值调整为空，仅认证开启时显式配置 |
| `freight_demand` 等市场表未显式指定字符集，真实中文请求在 MySQL 中插入失败 | 1 | 为 `V6__freight_market_init.sql` 全部表显式增加 `DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci` |
| 本机 `18085` 端口同时被前端 Vite 占用 IPv4，后端首次 smoke 命中前端页面 | 1 | 后端 smoke 改用 `18086` 且绑定 `127.0.0.1`，避免前后端端口混用 |
| 商城用户入口改造后，如果不抽共享模型，会出现多服务维护重复 DTO/VO | 1 | 将货运市场公共请求/响应对象统一迁移到 `model` 模块 |
| `service-user` 全模块异步任务集成测试存在基线失败，阻塞“全绿”收口 | 1 | 本轮先完成商城相关定向测试与聚合打包验证，并把异步任务失败单独登记为后续可靠性修复项 |

## Notes
- 当前已完成 RabbitMQ 本地部署基建，支持开发环境快速启停与管理台访问。
- 当前已完成三条消息驱动链路：提单异步解析、模板异步导出、模板异步提取。
- 模板提取已支持从异步任务结果下载空白模板、预览模板，并直接保存为正式模板定义。
- 当前已完成 Phase D 第一版：新增 `service-llm-task` 独立微服务，`service-user` 通过 Feign 转发异步任务提交与状态查询。
- 当前已完成基础设施线：MinIO 与 RabbitMQ 统一纳入根目录 compose 项目，Docker Desktop 可在同一项目分组下管理。
- 当前已完成业务异步线：模板保存纳入 `TEMPLATE_SAVE` 任务中心，形成“提取 -> 保存 -> 导出”完整异步链路。
- 当前已完成服务治理线第一步：Nacos 作为本地服务发现中心，`service-user` 可通过服务名调用 `service-llm-task`。
- 当前已完成配置中心线第一步：五个后端服务的 `dev.yml` 已导入 Nacos Config，服务启动时可从 Nacos 拉取配置。
- 当前已完成服务间 Feign 治理第一步：网关使用 Nacos 负载均衡路由，admin 可通过 Feign 调用 user/auth，user 可通过 Feign 调用 auth/llm-task。
- 当前已完成综合平台第一块真实业务：`service-market` 已支持需求发布、报价、接单和订单创建。
- 当前已完成市场高并发第一版：接单时使用 Redis 短锁防重复，接单成功后通过 RabbitMQ 异步写订单时间线。
- 当前计划已调整为只先完成商城端，`service-news` 和综合门户不进入本轮范围。
- 商城管理端职责已经明确：后续商城审核、统计、履约管理全部进入 `service-admin`。
- 当前已完成商城前台第一版：`frontend/client` 已接通真实商城接口，支持浏览、发布、报价、接单开工与需求完结。
- 当前已完成商城管理端第一版：`frontend/admin` 已接通审核分页与审核动作，支持统一审核货运需求。
- 下一轮聚焦商城端后续能力、前端重构和联调验收。
