package com.asamu.aiagent.dto.knowledge;

import jakarta.validation.constraints.NotBlank;

public record KnowledgeIngestRequest(
        @NotBlank String namespace,
        @NotBlank String title,
        @NotBlank String content
) {
}
