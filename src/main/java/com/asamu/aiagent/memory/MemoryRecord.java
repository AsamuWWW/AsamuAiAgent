package com.asamu.aiagent.memory;

import java.time.OffsetDateTime;

public record MemoryRecord(
        String userId,
        String sessionId,
        String role,
        String content,
        OffsetDateTime at
) {
}
