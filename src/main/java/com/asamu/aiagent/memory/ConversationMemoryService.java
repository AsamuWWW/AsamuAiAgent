package com.asamu.aiagent.memory;

import com.asamu.aiagent.config.AgentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.stream.Stream;

@Service
public class ConversationMemoryService {

    private static final Logger log = LoggerFactory.getLogger(ConversationMemoryService.class);

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
            try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
                lines.forEach(line -> {
                if (line.isBlank()) {
                    return;
                }
                try {
                    MemoryRecord record = objectMapper.readValue(line, MemoryRecord.class);
                    if (record.userId().equals(userId) && record.sessionId().equals(sessionId)) {
                        records.add(record);
                    }
                } catch (IOException ignored) {
                }
                });
            }
        } catch (IOException ex) {
            log.warn("failed to read long-term memory file: {}", path, ex);
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
        } catch (IOException ex) {
            log.warn("failed to append long-term memory file: {}", path, ex);
        }
    }

    private String toLine(MemoryRecord item) {
        return item.role() + ":" + item.content();
    }

}
