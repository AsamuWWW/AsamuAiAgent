# Asamu AI Agent

基于 `keweizhuazuki/kewei-ai-agent` 对标复现的后端基线工程（Spring Boot 版本）。

## 当前已实现能力（MVP）
- 同步聊天接口（`POST /api/chat`）
- 流式聊天接口（`POST /api/chat/stream`，SSE）
- Agent 任务启动/续跑（`POST /api/agent/start`、`POST /api/agent/{taskId}/resume`）
- 内存会话管理（聊天会话 + Agent 任务会话）
- 基础工具注册示例（`time.now`）
- 健康检查（`GET /api/health`）

> 说明：这是对标能力的工程起步版本，后续可继续扩展 RAG、多模型、MCP、持久化记忆等模块。

## 快速启动

### 1) 环境要求
- JDK 17+
- Maven 3.9+

### 2) 运行测试
```bash
mvn test
```

### 3) 启动服务
```bash
mvn spring-boot:run
```

默认地址：`http://localhost:8123/api`

## 目录结构
```text
src/main/java/com/asamu/aiagent
├── agent        # Agent 任务会话与续跑
├── app          # 聊天应用编排
├── common       # 通用响应结构
├── controller   # API 控制器
├── dto          # 请求/响应模型
└── tools        # 工具注册与调用
```

## 对标规划文档
- [项目分析](docs/PROJECT_ANALYSIS.md)
- [项目规划](docs/PROJECT_PLAN.md)
- [项目起始文件](docs/PROJECT_STARTER.md)
