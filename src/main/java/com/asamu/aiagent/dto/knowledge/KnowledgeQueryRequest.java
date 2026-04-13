package com.asamu.aiagent.dto.knowledge;

import jakarta.validation.constraints.NotBlank;

public record KnowledgeQueryRequest(
        @NotBlank String namespace,
        @NotBlank String question,
        Integer topK
) {
}
