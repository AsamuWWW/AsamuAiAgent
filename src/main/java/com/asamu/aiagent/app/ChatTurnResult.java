package com.asamu.aiagent.app;

import com.asamu.aiagent.tools.ToolExecutionLog;

import java.util.List;

public record ChatTurnResult(
        String userId,
        String sessionId,
        String provider,
        String model,
        String reply,
        List<ToolExecutionLog> toolLogs,
        List<String> citations
) {
}
