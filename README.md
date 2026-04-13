# Asamu AI Agent

基于 Spring Boot 的 AI Agent 后端演进版本，已完成从 MVP 到“可扩展骨架”的第一轮实现。

## 当前能力
- 同步聊天与 SSE 事件流（token/step/tool/done/error）
- LLM Provider 抽象与配置化路由（openai-compatible / local）
- 工具系统（参数校验、权限控制、超时、重试、执行日志）
- Agent 任务状态机（步骤状态、任务查询、等待用户、失败重试）
- 短期 + 长期记忆（按 user/session 隔离）
- 基础 RAG（文档入库、分块检索、引用返回）
- 统一错误码响应、API Key 鉴权、基础指标接口
- POST 接口幂等支持（`Idempotency-Key` + `X-Idempotent-Replay`）
- Dockerfile、CI 工作流、运行手册

## 快速开始
### 环境要求
- JDK 17+
- Maven 3.9+

### 测试
```bash
mvn test
```

### 启动
```bash
mvn spring-boot:run
```

默认地址：`http://localhost:8123/api`

## 文档
- [项目分析](docs/PROJECT_ANALYSIS.md)
- [项目规划](docs/PROJECT_PLAN.md)
- [项目起始文件](docs/PROJECT_STARTER.md)
- [复现能力矩阵](docs/CAPABILITY_MATRIX.md)
- [运行手册](docs/RUNBOOK.md)
- [OpenAPI 契约](docs/openapi.yaml)
