package com.asamu.aiagent.rag;

public record KnowledgeCitation(
        String documentId,
        String title,
        String snippet,
        int score
) {
}
