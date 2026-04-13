package com.asamu.aiagent.dto;

import jakarta.validation.constraints.NotBlank;

public record AgentStartRequest(@NotBlank String task) {
}
