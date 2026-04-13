package com.asamu.aiagent.dto;

import jakarta.validation.constraints.NotBlank;

public record AgentStartRequest(
        String userId,
        @NotBlank String task
) {
}
