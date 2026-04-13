package com.asamu.aiagent.app;

import com.asamu.aiagent.config.AgentProperties;
import com.asamu.aiagent.dto.StructuredAnswer;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatApp {
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{[\\s\\S]*}");
    private static final int BASE64_PREVIEW_LENGTH = 64;

    private final ToolRegistry toolRegistry;
    private final LlmService llmService;
    private final ConversationMemoryService memoryService;
    private final KnowledgeService knowledgeService;
    private final AppMetricsService metricsService;
    private final AgentProperties properties;
    private final ObjectMapper objectMapper;

    public ChatApp(ToolRegistry toolRegistry,
                   LlmService llmService,
                   ConversationMemoryService memoryService,
                   KnowledgeService knowledgeService,
                   AppMetricsService metricsService,
                   AgentProperties properties,
                   ObjectMapper objectMapper) {
        this.toolRegistry = toolRegistry;
        this.llmService = llmService;
        this.memoryService = memoryService;
        this.knowledgeService = knowledgeService;
        this.metricsService = metricsService;
        this.properties = properties;
        this.objectMapper = objectMapper;
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

    public ChatTurnResult chatMultimodal(String userId,
                                         String sessionId,
                                         String provider,
                                         String model,
                                         Double temperature,
                                         String role,
                                         String message,
                                         String imageUrl,
                                         String imageBase64,
                                         String imageMimeType) {
        String imageHint = buildImageHint(imageUrl, imageBase64, imageMimeType);
        String enhancedMessage = message + "\n\n[image]\n" + imageHint;
        return chat(userId, sessionId, provider, model, temperature, role, enhancedMessage);
    }

    public StructuredAnswer toStructuredAnswer(String userMessage,
                                               String assistantReply) {
        String json = extractJsonObject(assistantReply);
        if (json != null) {
            try {
                JsonNode node = objectMapper.readTree(json);
                String intent = node.path("intent").asText("");
                String summary = node.path("summary").asText("");
                boolean needsFollowUp = node.path("needsFollowUp").asBoolean(false);
                List<String> keywords = new ArrayList<>();
                JsonNode keywordNode = node.path("keywords");
                if (keywordNode.isArray()) {
                    for (JsonNode item : keywordNode) {
                        String keyword = item.asText("");
                        if (!keyword.isBlank()) {
                            keywords.add(keyword);
                        }
                    }
                }
                if (!intent.isBlank() || !summary.isBlank() || !keywords.isEmpty()) {
                    return new StructuredAnswer(
                            intent.isBlank() ? "general" : intent,
                            summary.isBlank() ? truncate(assistantReply, 120) : summary,
                            needsFollowUp,
                            keywords
                    );
                }
            } catch (Exception ignored) {
                // fallback below
            }
        }
        return fallbackStructuredAnswer(userMessage, assistantReply);
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
        if (text.startsWith("/mcp ")) {
            String args = text.substring(5).trim();
            int split = args.indexOf(' ');
            if (split <= 0 || split == args.length() - 1) {
                throw new IllegalArgumentException("Invalid MCP command format. Usage: /mcp <server> <payload>. Example: /mcp mock hello");
            }
            String server = args.substring(0, split).trim();
            String payload = args.substring(split + 1).trim();
            return toolRegistry.invoke(new ToolCallRequest(sessionId, "mcp.call", role, Map.of(
                    "server", server,
                    "payload", payload
            )));
        }
        return null;
    }

    private Map<String, Object> event(String type, Object payload) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("payload", payload);
        return map;
    }

    private String buildImageHint(String imageUrl, String imageBase64, String imageMimeType) {
        String mime = (imageMimeType == null || imageMimeType.isBlank()) ? "image/jpeg" : imageMimeType;
        boolean hasUrl = imageUrl != null && !imageUrl.isBlank();
        boolean hasBase64 = imageBase64 != null && !imageBase64.isBlank();
        if (!hasUrl && !hasBase64) {
            throw new IllegalArgumentException("imageUrl or imageBase64 must be provided");
        }
        if (hasUrl) {
            return "type=url, mime=" + mime + ", value=" + imageUrl.trim();
        }
        String base64Preview = imageBase64.substring(0, Math.min(imageBase64.length(), BASE64_PREVIEW_LENGTH));
        return "type=base64, mime=" + mime + ", value_prefix=" + base64Preview;
    }

    private StructuredAnswer fallbackStructuredAnswer(String userMessage, String assistantReply) {
        String input = userMessage == null ? "" : userMessage;
        String normalized = input.toLowerCase();
        String intent = (normalized.contains("plan")
                || normalized.contains("schedule")
                || normalized.contains("strategy")
                || normalized.contains("步骤")
                || normalized.contains("计划")
                || normalized.contains("方案")) ? "planning" : "general";
        boolean needsFollowUp = input.contains("?") || input.contains("？");
        List<String> keywords = buildKeywords(input);
        return new StructuredAnswer(intent, truncate(assistantReply, 120), needsFollowUp, keywords);
    }

    private List<String> buildKeywords(String input) {
        if (input.isBlank()) {
            return List.of();
        }
        String[] parts = input.replaceAll("[^\\p{L}\\p{N}\\s]", " ").trim().split("\\s+");
        List<String> keywords = new ArrayList<>();
        for (String part : parts) {
            if (part.length() >= 2 && keywords.stream().noneMatch(part::equalsIgnoreCase)) {
                keywords.add(part);
            }
            if (keywords.size() >= 5) {
                break;
            }
        }
        return keywords;
    }

    private String extractJsonObject(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = JSON_OBJECT_PATTERN.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }
}
