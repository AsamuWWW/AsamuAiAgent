package com.asamu.aiagent.tools;

import com.asamu.aiagent.config.AgentProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

@Component
public class ToolRegistry {

    private static final int LOG_LIMIT = 20;

    private final AgentProperties properties;
    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentLinkedDeque<ToolExecutionLog>> logs = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    public ToolRegistry(AgentProperties properties) {
        this.properties = properties;
        registerBuiltinTools();
    }

    public ToolExecutionResult invoke(ToolCallRequest request) {
        long start = System.currentTimeMillis();
        ToolDefinition def = tools.get(request.name());
        if (def == null) {
            return fail(request, "tool not found", start, 0);
        }
        if (!properties.getTool().getWhitelist().contains(def.name())) {
            return fail(request, "tool blocked by whitelist", start, 0);
        }
        if (!def.allowedRoles().contains(request.role())) {
            return fail(request, "permission denied for role " + request.role(), start, 0);
        }
        for (String requiredArg : def.requiredArgs()) {
            String value = request.args().get(requiredArg);
            if (value == null || value.isBlank()) {
                return fail(request, "missing arg: " + requiredArg, start, 0);
            }
        }

        RuntimeException error = null;
        int attempts = 0;
        for (int i = 0; i <= def.retries(); i++) {
            attempts++;
            try {
                String output = CompletableFuture.supplyAsync(() -> def.executor().apply(request))
                        .get(def.timeoutMillis(), TimeUnit.MILLISECONDS);
                long duration = System.currentTimeMillis() - start;
                ToolExecutionResult result = new ToolExecutionResult(true, output, null, attempts, duration);
                recordLog(request, true, output, duration, attempts);
                return result;
            } catch (Exception ex) {
                error = new RuntimeException(ex);
            }
        }
        long duration = System.currentTimeMillis() - start;
        String detail = error == null ? "tool execution failed" : Optional.ofNullable(error.getMessage()).orElse("tool execution failed");
        ToolExecutionResult result = new ToolExecutionResult(false, null, detail, attempts, duration);
        recordLog(request, false, detail, duration, attempts);
        return result;
    }

    public List<ToolExecutionLog> recentLogs(String sessionId) {
        return new ArrayList<>(logs.getOrDefault(sessionId, new ConcurrentLinkedDeque<>()));
    }

    private ToolExecutionResult fail(ToolCallRequest request, String detail, long start, int attempts) {
        long duration = System.currentTimeMillis() - start;
        recordLog(request, false, detail, duration, attempts);
        return new ToolExecutionResult(false, null, detail, attempts, duration);
    }

    private void recordLog(ToolCallRequest request, boolean success, String detail, long duration, int attempts) {
        ConcurrentLinkedDeque<ToolExecutionLog> sessionLogs = logs.computeIfAbsent(request.sessionId(), key -> new ConcurrentLinkedDeque<>());
        sessionLogs.addLast(new ToolExecutionLog(request.name(), request.role(), success, detail, duration, attempts, OffsetDateTime.now(ZoneId.of("Asia/Shanghai"))));
        while (sessionLogs.size() > LOG_LIMIT) {
            sessionLogs.pollFirst();
        }
    }

    private void registerBuiltinTools() {
        tools.put("time.now", new ToolDefinition(
                "time.now",
                Set.of(),
                Set.of("user", "admin"),
                1000,
                0,
                req -> OffsetDateTime.now(ZoneId.of("Asia/Shanghai")).toString()
        ));

        tools.put("http.get", new ToolDefinition(
                "http.get",
                Set.of("url"),
                Set.of("user", "admin"),
                3500,
                1,
                req -> httpGet(req.args().get("url"))
        ));

        tools.put("file.read", new ToolDefinition(
                "file.read",
                Set.of("path"),
                Set.of("admin"),
                1500,
                0,
                req -> safeReadFile(req.args().get("path"))
        ));
    }

    private String httpGet(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("url must start with http:// or https://");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = response.body() == null ? "" : response.body();
            return "status=" + response.statusCode() + ", body=" + body.substring(0, Math.min(300, body.length()));
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("http.get failed", e);
        }
    }

    private String safeReadFile(String rawPath) {
        try {
            Path base = Path.of(properties.getTool().getFileReadBasePath()).toRealPath();
            Path target = base.resolve(rawPath).normalize().toRealPath();
            if (!target.startsWith(base)) {
                throw new IllegalArgumentException("path out of allowed base directory");
            }
            String content = Files.readString(target, StandardCharsets.UTF_8);
            return content.substring(0, Math.min(content.length(), 1200));
        } catch (IOException ex) {
            throw new RuntimeException("file.read failed", ex);
        }
    }
}
