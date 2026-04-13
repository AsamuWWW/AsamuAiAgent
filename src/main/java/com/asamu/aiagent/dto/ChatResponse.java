package com.asamu.aiagent.dto;

import com.asamu.aiagent.tools.ToolExecutionLog;

import java.util.List;

public record ChatResponse(
        String userId,
        String sessionId,
        String provider,
        String model,
        String reply,
        List<ToolExecutionLog> toolLogs,
        List<String> citations
) {
}
