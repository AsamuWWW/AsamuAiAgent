package com.asamu.aiagent.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(String sessionId, @NotBlank String message) {
}
