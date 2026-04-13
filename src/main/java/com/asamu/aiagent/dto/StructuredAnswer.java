package com.asamu.aiagent.dto;

import java.util.List;

public record StructuredAnswer(
        String intent,
        String summary,
        boolean needsFollowUp,
        List<String> keywords
) {
}
