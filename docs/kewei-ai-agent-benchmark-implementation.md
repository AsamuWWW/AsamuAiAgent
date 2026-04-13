# kewei-ai-agent 对标工程实施文档

## 1. 项目系统剖析

### 1.1 架构设计
- **API 层**：以 `AiController` 为核心，提供同步对话、流式对话、SSE、多模态图片上传、Manus 续跑接口。
- **应用层**：`LoveApp` 统一封装聊天、RAG、Tools、MCP、结构化输出能力。
- **Agent 层**：`KeweiManus` + `ManusSessionService` + `ManusSessionStore` 形成多步任务执行、追问、续跑、Todo 快照闭环。
- **基础设施层**：`AiModelPrimaryConfig`、`ChatMemoryConfig`、`LoveAppVectorStoreConfig`、`QueryPreprocessor` 支撑模型/记忆/RAG 基础能力。
- **扩展层**：`ToolRegistration` 统一工具注册，`OpenClawResearchTool` 对接外部调研代理，MCP 服务作为外部能力节点。

### 1.2 核心模块
- **会话与交互**：同步、SSE、SseEmitter 三种输出形态并行。
- **状态与记忆**：会话历史支持 file/mysql/redis 可切换。
- **RAG 链路**：文档加载、向量化、查询预处理、检索增强。
- **工具编排**：统一注册并按任务域裁剪工具子集。
- **多步 Agent**：任务拆解、Todo 状态同步、用户补充信息后续跑。

### 1.3 技术栈
- 后端：Java 21、Spring Boot 4、Spring AI 2.0.0-M2、Maven
- 数据：MySQL、Redis、PostgreSQL + pgvector
- 模型与协议：Ollama、DashScope、MCP Client
- 前端：Vue3、Vite、Pinia、axios

### 1.4 实现逻辑要点
- 多模型并存，通过 `BeanFactoryPostProcessor` 强制 Ollama 为默认主模型。
- 会话记忆通过配置切换 file/mysql/redis 三种实现。
- Agent 执行链路：任务域识别 → 工具子集选择 → 执行 → 追问补充 → 继续执行。
- RAG 支持查询改写、压缩、扩展后再检索增强问答。
- 工程治理包括统一响应包装、全局异常处理、分层测试目录。

---

## 2. 对标项目分阶段工作流程

### 阶段 A：目标定义与基线搭建
- **目标**：明确 MVP 范围与验收标准。
- **关键任务**：用例分级（Chat/RAG/Tool/Agent/MCP）、接口契约、技术选型冻结。
- **交付物**：需求规格、系统蓝图、里程碑清单。

### 阶段 B：基础平台层
- **目标**：完成可维护后端骨架。
- **关键任务**：分层目录、统一响应/异常、配置体系、日志规范、健康检查。
- **交付物**：可启动服务、基础 API、配置模板。

### 阶段 C：模型与记忆能力
- **目标**：实现可切换模型与会话记忆。
- **关键任务**：多模型装配策略、ChatMemory 三实现、会话 ID 贯通。
- **交付物**：聊天 API（sync/stream/SSE）、记忆切换验收记录。

### 阶段 D：RAG 能力
- **目标**：完成知识库问答闭环。
- **关键任务**：文档加载、向量入库、查询预处理、检索增强策略。
- **交付物**：RAG API、召回与回答质量测试结果。

### 阶段 E：Tools + MCP 能力
- **目标**：统一工具编排并接入外部能力。
- **关键任务**：Tool Registry、工具权限边界、MCP 客户端/服务配置。
- **交付物**：工具调用接口、MCP 联调报告、失败重试策略。

### 阶段 F：多步 Agent（Manus 对标）
- **目标**：形成任务分解、追问、续跑、Todo 同步机制。
- **关键任务**：Agent 状态机、Session Store、AskUserQuestion/TodoWrite/Terminate 流程。
- **交付物**：多步任务演示链路、状态可观测输出。

### 阶段 G：前后端联调与工程收口
- **目标**：达到可演示、可测试、可交付。
- **关键任务**：前端联调、端到端测试、文档完善、部署脚本。
- **交付物**：演示环境、测试报告、部署手册、发布说明。

---

## 3. 日志文件管理方案

### 3.1 主日志文件
- 路径：`/home/runner/work/AsamuAiAgent/AsamuAiAgent/docs/agent-task-log.md`
- 定位：任务实施过程的单一事实源。

### 3.2 记录维度（每条必填）
1. 时间戳
2. 阶段
3. 操作类型（分析/开发/测试/修复/决策）
4. 变更对象（模块/文件/API）
5. 输入摘要
6. 输出摘要
7. 状态（成功/失败/阻塞）
8. 下一步与负责人

### 3.3 维护规则
- 每完成关键动作立即追加日志。
- 每日结束追加“日报汇总”。
- 阻塞项单独标记并追加解除记录。
- 日志不得记录敏感密钥与凭据。

### 3.4 最小日志模板
```text
[time] [phase] [type] action
inputs: ...
outputs: ...
status: ...
next: ...
owner: ...
```
