package com.asamu.aiagent.dto;

import jakarta.validation.constraints.NotBlank;

public record AgentResumeRequest(@NotBlank String answer) {
}
