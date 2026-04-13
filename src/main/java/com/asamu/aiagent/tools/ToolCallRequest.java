package com.asamu.aiagent.tools;

import java.util.Map;

public record ToolCallRequest(
        String sessionId,
        String name,
        String role,
        Map<String, String> args
) {
}
