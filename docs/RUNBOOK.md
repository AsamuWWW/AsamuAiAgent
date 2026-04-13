# 运行手册（最小可用）

## 本地运行
```bash
mvn test
mvn spring-boot:run
```

默认地址：`http://localhost:8123/api`

## 关键接口
- `POST /chat`
- `POST /chat/stream`
- `POST /agent/start`
- `POST /agent/{taskId}/resume`
- `GET /agent/{taskId}`
- `POST /agent/{taskId}/retry`
- `POST /knowledge/documents`
- `POST /knowledge/query`
- `GET /metrics`

## 安全
- 通过 `asamu.ai.security.api-key` 启用 API Key 校验
- 请求头：`X-API-Key: <value>`

## Docker
```bash
docker build -t asamu-ai-agent .
docker run --rm -p 8123:8123 asamu-ai-agent
```
