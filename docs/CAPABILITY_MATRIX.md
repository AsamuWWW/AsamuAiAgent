# 复现能力矩阵（范围冻结）

| 领域 | Must Have（本轮已实现） | Optional（后续增强） | Not Now |
|---|---|---|---|
| 对话 | 同步 chat / SSE chat，结构化输出，多模态图片输入，模型路由参数 | 富媒体理解增强 | 复杂函数调用协议兼容 |
| 模型层 | Provider 抽象（openai-compatible/local）+ 配置化超时重试 | 真实外部模型接入 | 自动模型评测路由 |
| 工具系统 | time/http/file.read/mcp.call，参数校验、权限、超时重试、日志 | Tool marketplace | 任意系统命令执行 |
| Agent | 步骤状态机、任务查询、失败重试、等待用户输入 | 自动任务拆解优化 | 图形化流程编排 |
| 记忆 | 短期内存 + 长期持久化（jsonl）按 user/session 隔离 | 数据库存储（MySQL/PG） | 跨租户共享记忆 |
| RAG | 文档入库、分块、检索、引用返回 | 向量数据库 | 在线重排/混合检索 |
| API协议 | 统一错误码，SSE 标准事件 | OpenAPI 自动化发布 | GraphQL |
| 安全治理 | API Key、工具白名单、审计日志（工具执行轨迹） | JWT、RBAC | SSO |
| 可观测 | 指标统计接口、结构化事件输出 | tracing/metrics 平台接入 | APM 深度分析 |
| 交付运维 | Dockerfile、CI、环境分层配置、runbook | k8s Helm | 多区域容灾 |
