package com.asamu.aiagent.memory;

import com.asamu.aiagent.config.AgentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class ConversationMemoryService {

    private final AgentProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, ConcurrentLinkedDeque<MemoryRecord>> shortTerm = new ConcurrentHashMap<>();

    public ConversationMemoryService(AgentProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void append(String userId, String sessionId, String role, String content) {
        MemoryRecord record = new MemoryRecord(userId, sessionId, role, content, OffsetDateTime.now());
        ConcurrentLinkedDeque<MemoryRecord> deque = shortTerm.computeIfAbsent(sessionId, key -> new ConcurrentLinkedDeque<>());
        deque.addLast(record);
        while (deque.size() > properties.getMemory().getShortTermLimit()) {
            deque.pollFirst();
        }
        appendLongTerm(record);
    }

    public MemoryRecall recall(String userId, String sessionId) {
        List<String> recent = shortTerm.getOrDefault(sessionId, new ConcurrentLinkedDeque<>())
                .stream()
                .map(this::toLine)
                .toList();
        List<String> longTerm = readLongTerm(userId, sessionId);
        return new MemoryRecall(recent, longTerm);
    }

    private List<String> readLongTerm(String userId, String sessionId) {
        Path path = Path.of(properties.getMemory().getPersistenceFile());
        if (!Files.exists(path)) {
            return List.of();
        }
        List<MemoryRecord> records = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                MemoryRecord record = objectMapper.readValue(line, MemoryRecord.class);
                if (record.userId().equals(userId) && record.sessionId().equals(sessionId)) {
                    records.add(record);
                }
            }
        } catch (IOException ignored) {
            return List.of();
        }
        return records.stream()
                .sorted(Comparator.comparing(MemoryRecord::at).reversed())
                .limit(properties.getMemory().getLongTermRecall())
                .map(this::toLine)
                .toList();
    }

    private void appendLongTerm(MemoryRecord record) {
        Path path = Path.of(properties.getMemory().getPersistenceFile());
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(objectMapper.writeValueAsString(record));
                writer.newLine();
            }
        } catch (IOException ignored) {
        }
    }

    private String toLine(MemoryRecord item) {
        return item.role() + ":" + item.content();
    }

}
