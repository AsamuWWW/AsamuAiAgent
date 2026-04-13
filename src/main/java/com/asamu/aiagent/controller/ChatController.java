package com.asamu.aiagent.controller;

import com.asamu.aiagent.app.ChatApp;
import com.asamu.aiagent.app.ChatTurnResult;
import com.asamu.aiagent.common.ApiResponse;
import com.asamu.aiagent.dto.ChatRequest;
import com.asamu.aiagent.dto.ChatResponse;
import com.asamu.aiagent.dto.ChatStructuredResponse;
import com.asamu.aiagent.idempotency.IdempotencyResult;
import com.asamu.aiagent.idempotency.IdempotencyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public class ChatController {

    private static final long SSE_TIMEOUT_MILLIS = 30_000L;
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String IDEMPOTENCY_REPLAY_HEADER = "X-Idempotent-Replay";

    private final ChatApp chatApp;
    private final IdempotencyService idempotencyService;

    public ChatController(ChatApp chatApp, IdempotencyService idempotencyService) {
        this.chatApp = chatApp;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @Valid @RequestBody ChatRequest request,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey
    ) {
        IdempotencyResult<ApiResponse<ChatResponse>> result = idempotencyService.execute(
                "POST:/chat",
                idempotencyKey,
                request,
                () -> ApiResponse.ok(toResponse(executeTurn(request)))
        );
        return withIdempotencyHeaders(result);
    }

    @PostMapping("/chat/structured")
    public ResponseEntity<ApiResponse<ChatStructuredResponse>> structuredChat(
            @Valid @RequestBody ChatRequest request,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey
    ) {
        IdempotencyResult<ApiResponse<ChatStructuredResponse>> result = idempotencyService.execute(
                "POST:/chat/structured",
                idempotencyKey,
                request,
                () -> {
                    ChatTurnResult turnResult = executeTurn(request);
                    ChatStructuredResponse response = new ChatStructuredResponse(
                            turnResult.userId(),
                            turnResult.sessionId(),
                            turnResult.provider(),
                            turnResult.model(),
                            turnResult.reply(),
                            chatApp.toStructuredAnswer(request.message(), turnResult.reply()),
                            turnResult.citations()
                    );
                    return ApiResponse.ok(response);
                }
        );
        return withIdempotencyHeaders(result);
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

    private ChatResponse toResponse(ChatTurnResult result) {
        return new ChatResponse(
                result.userId(),
                result.sessionId(),
                result.provider(),
                result.model(),
                result.reply(),
                result.toolLogs(),
                result.citations()
        );
    }

    private <T> ResponseEntity<ApiResponse<T>> withIdempotencyHeaders(IdempotencyResult<ApiResponse<T>> result) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(IDEMPOTENCY_REPLAY_HEADER, String.valueOf(result.replayed()));
        return ResponseEntity.ok().headers(headers).body(result.data());
    }

    private void streamResponse(SseEmitter emitter, ChatRequest request) {
        try {
            ChatTurnResult result = executeTurn(request);
            List<Map<String, Object>> events = chatApp.streamEvents(result);
            for (Map<String, Object> event : events) {
                emitter.send(SseEmitter.event().name(String.valueOf(event.get("type"))).data(event.get("payload")));
            }
            emitter.complete();
        } catch (Exception e) {
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
