package com.asamu.aiagent.app;

import com.asamu.aiagent.config.AgentProperties;
import com.asamu.aiagent.llm.LlmResponse;
import com.asamu.aiagent.llm.LlmService;
import com.asamu.aiagent.memory.ConversationMemoryService;
import com.asamu.aiagent.memory.MemoryRecall;
import com.asamu.aiagent.metrics.AppMetricsService;
import com.asamu.aiagent.rag.KnowledgeCitation;
import com.asamu.aiagent.rag.KnowledgeService;
import com.asamu.aiagent.tools.ToolCallRequest;
import com.asamu.aiagent.tools.ToolExecutionLog;
import com.asamu.aiagent.tools.ToolExecutionResult;
import com.asamu.aiagent.tools.ToolRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChatApp {

    private final ToolRegistry toolRegistry;
    private final LlmService llmService;
    private final ConversationMemoryService memoryService;
    private final KnowledgeService knowledgeService;
    private final AppMetricsService metricsService;
    private final AgentProperties properties;

    public ChatApp(ToolRegistry toolRegistry,
                   LlmService llmService,
                   ConversationMemoryService memoryService,
                   KnowledgeService knowledgeService,
                   AppMetricsService metricsService,
                   AgentProperties properties) {
        this.toolRegistry = toolRegistry;
        this.llmService = llmService;
        this.memoryService = memoryService;
        this.knowledgeService = knowledgeService;
        this.metricsService = metricsService;
        this.properties = properties;
    }

    public String ensureSessionId(String sessionId) {
        return (sessionId == null || sessionId.isBlank()) ? UUID.randomUUID().toString() : sessionId;
    }

    public String ensureUserId(String userId) {
        return (userId == null || userId.isBlank()) ? "anonymous" : userId;
    }

    public ChatTurnResult chat(String userId,
                               String sessionId,
                               String provider,
                               String model,
                               Double temperature,
                               String role,
                               String message) {
        metricsService.incChatRequests();
        String resolvedUserId = ensureUserId(userId);
        String resolvedSessionId = ensureSessionId(sessionId);
        String resolvedRole = (role == null || role.isBlank()) ? properties.getTool().getDefaultRole() : role;

        memoryService.append(resolvedUserId, resolvedSessionId, "user", message);

        ToolExecutionResult toolResult = maybeInvokeTool(resolvedSessionId, resolvedRole, message);
        if (toolResult != null) {
            metricsService.incToolCalls();
            String reply = toolResult.success() ? toolResult.output() : "工具执行失败: " + toolResult.error();
            if (!toolResult.success()) {
                metricsService.incToolFailures();
            }
            memoryService.append(resolvedUserId, resolvedSessionId, "assistant", reply);
            return new ChatTurnResult(
                    resolvedUserId,
                    resolvedSessionId,
                    provider == null || provider.isBlank() ? properties.getModel().getDefaultProvider() : provider,
                    model == null || model.isBlank() ? properties.getModel().getDefaultModel() : model,
                    reply,
                    toolRegistry.recentLogs(resolvedSessionId),
                    List.of()
            );
        }

        MemoryRecall recall = memoryService.recall(resolvedUserId, resolvedSessionId);
        List<KnowledgeCitation> citations = knowledgeService.query("default", message, 3);
        List<String> context = new ArrayList<>();
        context.addAll(recall.shortTerm());
        context.addAll(recall.longTerm());
        context.addAll(citations.stream().map(KnowledgeCitation::snippet).toList());

        LlmResponse llm = llmService.chat(provider, model, temperature, resolvedSessionId, resolvedUserId, message, context);
        String reply = llm.text();
        memoryService.append(resolvedUserId, resolvedSessionId, "assistant", reply);

        return new ChatTurnResult(
                resolvedUserId,
                resolvedSessionId,
                llm.provider(),
                llm.model(),
                reply,
                toolRegistry.recentLogs(resolvedSessionId),
                citations.stream().map(citation -> citation.title() + ":" + citation.snippet()).toList()
        );
    }

    public List<Map<String, Object>> streamEvents(ChatTurnResult result) {
        List<Map<String, Object>> events = new ArrayList<>();
        events.add(event("step", Map.of("message", "接收请求", "sessionId", result.sessionId())));
        if (!result.toolLogs().isEmpty()) {
            ToolExecutionLog lastLog = result.toolLogs().get(result.toolLogs().size() - 1);
            events.add(event("tool", lastLog));
        }
        events.add(event("token", Map.of("content", result.reply())));
        events.add(event("done", Map.of("provider", result.provider(), "model", result.model())));
        return events;
    }

    private ToolExecutionResult maybeInvokeTool(String sessionId, String role, String message) {
        String text = message == null ? "" : message.trim();
        if ("/time".equalsIgnoreCase(text)) {
            return toolRegistry.invoke(new ToolCallRequest(sessionId, "time.now", role, Map.of()));
        }
        if (text.startsWith("/http ")) {
            String url = text.substring(6).trim();
            return toolRegistry.invoke(new ToolCallRequest(sessionId, "http.get", role, Map.of("url", url)));
        }
        if (text.startsWith("/read ")) {
            String path = text.substring(6).trim();
            return toolRegistry.invoke(new ToolCallRequest(sessionId, "file.read", role, Map.of("path", path)));
        }
        return null;
    }

    private Map<String, Object> event(String type, Object payload) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("payload", payload);
        return map;
    }
}
