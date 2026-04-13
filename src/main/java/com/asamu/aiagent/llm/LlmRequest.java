package com.asamu.aiagent.llm;

import java.util.List;

public record LlmRequest(
        String sessionId,
        String userId,
        String message,
        String model,
        double temperature,
        List<String> recalledMemory
) {
}
