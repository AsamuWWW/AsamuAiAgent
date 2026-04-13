package com.asamu.aiagent.dto;

import com.asamu.aiagent.tools.ToolExecutionLog;

import java.util.List;

public record MultimodalChatResponse(
        String userId,
        String sessionId,
        String provider,
        String model,
        String reply,
        String imageSource,
        String imageMimeType,
        List<ToolExecutionLog> toolLogs,
        List<String> citations
) {
}
