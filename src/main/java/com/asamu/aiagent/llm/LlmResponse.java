package com.asamu.aiagent.llm;

public record LlmResponse(
        String provider,
        String model,
        String text,
        int tokens
) {
}
