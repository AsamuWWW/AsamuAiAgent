package com.asamu.aiagent.dto;

import jakarta.validation.constraints.NotBlank;

public record MultimodalChatRequest(
        String userId,
        String sessionId,
        String provider,
        String model,
        Double temperature,
        String role,
        @NotBlank String message,
        String imageUrl,
        String imageBase64,
        String imageMimeType
) {
}
