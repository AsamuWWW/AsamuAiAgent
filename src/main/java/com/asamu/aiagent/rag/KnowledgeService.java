package com.asamu.aiagent.rag;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KnowledgeService {

    private static final int CHUNK_SIZE = 220;

    private final Map<String, List<KnowledgeDocument>> namespaceStore = new ConcurrentHashMap<>();

    public KnowledgeDocument ingest(String namespace, String title, String content) {
        String docId = UUID.randomUUID().toString();
        List<String> chunks = split(content);
        KnowledgeDocument doc = new KnowledgeDocument(docId, namespace, title, chunks);
        namespaceStore.computeIfAbsent(namespace, key -> new ArrayList<>()).add(doc);
        return doc;
    }

    public List<KnowledgeCitation> query(String namespace, String question, int topK) {
        String[] tokens = question.toLowerCase().split("\\s+");
        List<KnowledgeCitation> result = new ArrayList<>();
        for (KnowledgeDocument document : namespaceStore.getOrDefault(namespace, List.of())) {
            for (String chunk : document.chunks()) {
                int score = score(chunk.toLowerCase(), tokens);
                if (score > 0) {
                    result.add(new KnowledgeCitation(document.id(), document.title(),
                            chunk.substring(0, Math.min(180, chunk.length())), score));
                }
            }
        }
        return result.stream()
                .sorted(Comparator.comparing(KnowledgeCitation::score).reversed())
                .limit(Math.max(1, topK))
                .toList();
    }

    private List<String> split(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < content.length(); i += CHUNK_SIZE) {
            chunks.add(content.substring(i, Math.min(content.length(), i + CHUNK_SIZE)));
        }
        return chunks;
    }

    private int score(String chunk, String[] tokens) {
        int score = 0;
        for (String token : tokens) {
            if (!token.isBlank() && chunk.contains(token)) {
                score++;
            }
        }
        return score;
    }
}
