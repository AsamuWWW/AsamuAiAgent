package com.asamu.aiagent.tools;

import java.util.Set;
import java.util.function.Function;

public record ToolDefinition(
        String name,
        Set<String> requiredArgs,
        Set<String> allowedRoles,
        int timeoutMillis,
        int retries,
        Function<ToolCallRequest, String> executor
) {
}
