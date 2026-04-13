package com.asamu.aiagent.controller;

import com.asamu.aiagent.app.ChatApp;
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

@RestController
public class ChatController {

    private final ChatApp chatApp;

    public ChatController(ChatApp chatApp) {
        this.chatApp = chatApp;
    }

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String sessionId = chatApp.ensureSessionId(request.sessionId());
        String reply = chatApp.chat(sessionId, request.message());
        return ApiResponse.ok(new ChatResponse(sessionId, reply));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request) {
        String sessionId = chatApp.ensureSessionId(request.sessionId());
        SseEmitter emitter = new SseEmitter(30_000L);
        try {
            for (String chunk : chatApp.streamChunks(sessionId, request.message())) {
                emitter.send(SseEmitter.event().name("message").data(chunk));
            }
            emitter.send(SseEmitter.event().name("done").data("done"));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }
}
