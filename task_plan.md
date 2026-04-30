# Task Plan: 主业务消息驱动改造规划

## Goal
为 `manifestReader` 制定一份围绕主业务链路的消息驱动改造计划，使项目在保留航运单证业务连续性的前提下，引入 `Redis`、`RabbitMQ`、`Elasticsearch`、`Kibana` 和高并发设计能力。

## Current Phase
Phase 10

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

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
| `task_plan.md/findings.md/progress.md` 不存在 | 1 | 按 `planning-with-files` 规范初始化三份规划文件 |
| 本机没有 `mvn` 命令 | 1 | 改用仓库自带 `./mvnw` 执行 Maven 构建 |
| `-pl service/service-user` 未联动编译 `model` 新实体 | 1 | 使用 `-am` 联动依赖模块一起编译测试 |
| `BaseMapper.insert` 测试断言命中重载歧义 | 1 | 在 `argThat` 中显式标注 `BlParseTaskEntity` 类型 |
| Testcontainers 集成测试首次拉取 RabbitMQ 镜像时遇到 Docker Registry `EOF` | 1 | 调整为更轻量的本地连调路径，保留 MySQL/Redis 容器并用测试注入替代外部 Rabbit 投递 |

## Notes
- 当前已完成 RabbitMQ 本地部署基建，支持开发环境快速启停与管理台访问。
- 当前已完成三条消息驱动链路：提单异步解析、模板异步导出、模板异步提取。
- 模板提取已支持从异步任务结果下载空白模板、预览模板，并直接保存为正式模板定义。
- 当前已完成 Phase D 第一版：新增 `service-llm-task` 独立微服务，`service-user` 通过 Feign 转发异步任务提交与状态查询。
- 下一轮可继续收敛共享代码边界，并把 `service-admin` 也接到 `service-llm-task` 任务中心。
