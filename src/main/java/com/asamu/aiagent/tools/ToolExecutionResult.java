package com.asamu.aiagent.tools;

public record ToolExecutionResult(
        boolean success,
        String output,
        String error,
        int attempts,
        long durationMillis
) {
}
