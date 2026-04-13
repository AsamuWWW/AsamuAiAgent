package com.asamu.aiagent.idempotency;

public record IdempotencyResult<T>(T data, boolean replayed) {
}
