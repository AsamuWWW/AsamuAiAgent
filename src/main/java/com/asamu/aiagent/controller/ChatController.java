package com.asamu.aiagent.controller;

import com.asamu.aiagent.app.ChatApp;
import com.asamu.aiagent.app.ChatTurnResult;
import com.asamu.aiagent.common.ApiResponse;
import com.asamu.aiagent.dto.ChatRequest;
import com.asamu.aiagent.dto.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public class ChatController {

    private static final long SSE_TIMEOUT_MILLIS = 30_000L;

    private final ChatApp chatApp;

    public ChatController(ChatApp chatApp) {
        this.chatApp = chatApp;
    }

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        ChatTurnResult result = executeTurn(request);
        return ApiResponse.ok(new ChatResponse(
                result.userId(),
                result.sessionId(),
                result.provider(),
                result.model(),
                result.reply(),
                result.toolLogs(),
                result.citations()
        ));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        CompletableFuture.runAsync(() -> streamResponse(emitter, request));
        return emitter;
    }

    private ChatTurnResult executeTurn(ChatRequest request) {
        return chatApp.chat(
                request.userId(),
                request.sessionId(),
                request.provider(),
                request.model(),
                request.temperature(),
                request.role(),
                request.message()
        );
    }

    private void streamResponse(SseEmitter emitter, ChatRequest request) {
        try {
            ChatTurnResult result = executeTurn(request);
            List<Map<String, Object>> events = chatApp.streamEvents(result);
            for (Map<String, Object> event : events) {
                emitter.send(SseEmitter.event().name(String.valueOf(event.get("type"))).data(event.get("payload")));
            }
            emitter.complete();
        } catch (IOException e) {
            sendErrorEvent(emitter, e.getMessage());
        } catch (RuntimeException e) {
            sendErrorEvent(emitter, e.getMessage());
        }
    }

    private void sendErrorEvent(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(Map.of("message", message)));
            emitter.complete();
        } catch (IOException ioException) {
            emitter.completeWithError(ioException);
        }
    }
}
