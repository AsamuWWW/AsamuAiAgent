package com.asamu.aiagent.rag;

import java.util.List;

public record KnowledgeDocument(
        String id,
        String namespace,
        String title,
        List<String> chunks
) {
}
