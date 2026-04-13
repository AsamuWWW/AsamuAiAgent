package com.asamu.aiagent.app;

import com.asamu.aiagent.tools.ToolRegistry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class ChatApp {

    private static final int MAX_HISTORY_MESSAGES = 20;

    private final ToolRegistry toolRegistry;
    private final Map<String, ConcurrentLinkedDeque<String>> memory = new ConcurrentHashMap<>();

    public ChatApp(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public String ensureSessionId(String sessionId) {
        return (sessionId == null || sessionId.isBlank()) ? UUID.randomUUID().toString() : sessionId;
    }

    public String chat(String sessionId, String message) {
        String sid = ensureSessionId(sessionId);
        ConcurrentLinkedDeque<String> messages = memory.computeIfAbsent(sid, k -> new ConcurrentLinkedDeque<>());
        messages.addLast("user:" + message);
        while (messages.size() > MAX_HISTORY_MESSAGES) {
            messages.removeFirst();
        }

        if ("/time".equalsIgnoreCase(message.trim())) {
            String now = toolRegistry.invoke("time.now").orElse("unknown");
            String reply = "当前时间: " + now;
            messages.addLast("assistant:" + reply);
            return reply;
        }

        long historySize = messages.stream().filter(it -> it.startsWith("user:")).count();
        String reply = "AsamuAgent 已收到：" + message + "（当前会话第 " + historySize + " 条提问）";
        messages.addLast("assistant:" + reply);
        return reply;
    }

    public List<String> streamChunks(String sessionId, String message) {
        String reply = chat(sessionId, message);
        return List.of("正在分析请求...", "调用基础能力...", reply);
    }
}
