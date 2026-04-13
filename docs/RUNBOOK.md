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

## 幂等请求
- 对关键 `POST` 接口可传请求头：`Idempotency-Key: <unique-key>`
- 同 key + 同 payload：返回缓存结果，响应头 `X-Idempotent-Replay: true`
- 同 key + 不同 payload：返回 `409`（错误码 `40901`）

## 安全
- 通过 `asamu.ai.security.api-key` 启用 API Key 校验
- 请求头：`X-API-Key: <value>`

## OpenAPI
- 稳定接口契约：`docs/openapi.yaml`

## Docker
```bash
docker build -t asamu-ai-agent .
docker run --rm -p 8123:8123 asamu-ai-agent
```
