package com.asamu.aiagent.dto;

import java.util.List;

public record ChatStructuredResponse(
        String userId,
        String sessionId,
        String provider,
        String model,
        String reply,
        StructuredAnswer structured,
        List<String> citations
) {
}
