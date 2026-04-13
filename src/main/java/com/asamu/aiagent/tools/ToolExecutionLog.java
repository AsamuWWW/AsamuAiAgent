package com.asamu.aiagent.tools;

import java.time.OffsetDateTime;

public record ToolExecutionLog(
        String tool,
        String role,
        boolean success,
        String detail,
        long durationMillis,
        int attempts,
        OffsetDateTime at
) {
}
