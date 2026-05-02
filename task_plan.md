# Task Plan: 主业务消息驱动改造规划

## Goal
为 `manifestReader` 制定一份围绕主业务链路的消息驱动改造计划，使项目在保留航运单证业务连续性的前提下，引入 `Redis`、`RabbitMQ`、`Elasticsearch`、`Kibana` 和高并发设计能力。

## Current Phase
Phase 15 complete

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

## Key Questions
1. 如何在不破坏现有业务的前提下，把同步链路改成消息驱动？
2. Redis 在本项目里最值得承担哪些职责，而不是只作为“技术点”出现？
3. 哪些高并发场景能自然贴合航运单证业务，而不是生搬硬套秒杀？
4. ES 和 Kibana 应该如何同时服务业务检索与日志分析？

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
- 下一轮可进入 Redis 限流/任务热点缓存增强，或补充 RabbitMQ 死信、重试、补偿和监控指标。
