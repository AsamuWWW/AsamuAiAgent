package com.asamu.aiagent.dto.agent;

public record AgentStepResponse(
        String id,
        String title,
        String status,
        String detail,
        int attempts,
        String updatedAt
) {
}
