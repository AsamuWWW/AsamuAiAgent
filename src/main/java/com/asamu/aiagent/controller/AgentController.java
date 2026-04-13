package com.asamu.aiagent.controller;

import com.asamu.aiagent.agent.AgentService;
import com.asamu.aiagent.common.ApiResponse;
import com.asamu.aiagent.dto.AgentResumeRequest;
import com.asamu.aiagent.dto.AgentStartRequest;
import com.asamu.aiagent.dto.AgentTaskResponse;
import com.asamu.aiagent.idempotency.IdempotencyResult;
import com.asamu.aiagent.idempotency.IdempotencyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String IDEMPOTENCY_REPLAY_HEADER = "X-Idempotent-Replay";

    private final AgentService agentService;
    private final IdempotencyService idempotencyService;

    public AgentController(AgentService agentService, IdempotencyService idempotencyService) {
        this.agentService = agentService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/agent/start")
    public ResponseEntity<ApiResponse<AgentTaskResponse>> start(
            @Valid @RequestBody AgentStartRequest request,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey
    ) {
        IdempotencyResult<ApiResponse<AgentTaskResponse>> result = idempotencyService.execute(
                "POST:/agent/start",
                idempotencyKey,
                request,
                () -> ApiResponse.ok(agentService.start(request.userId(), request.task()))
        );
        return withIdempotencyHeaders(result);
    }

    @PostMapping("/agent/{taskId}/resume")
    public ResponseEntity<ApiResponse<AgentTaskResponse>> resume(
            @PathVariable String taskId,
            @Valid @RequestBody AgentResumeRequest request,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey
    ) {
        IdempotencyResult<ApiResponse<AgentTaskResponse>> result = idempotencyService.execute(
                "POST:/agent/" + taskId + "/resume",
                idempotencyKey,
                request,
                () -> ApiResponse.ok(agentService.resume(taskId, request.answer()))
        );
        return withIdempotencyHeaders(result);
    }

    @GetMapping("/agent/{taskId}")
    public ApiResponse<AgentTaskResponse> taskDetail(@PathVariable String taskId) {
        return ApiResponse.ok(agentService.getTask(taskId));
    }

    @PostMapping("/agent/{taskId}/retry")
    public ResponseEntity<ApiResponse<AgentTaskResponse>> retryFailedStep(
            @PathVariable String taskId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey
    ) {
        IdempotencyResult<ApiResponse<AgentTaskResponse>> result = idempotencyService.execute(
                "POST:/agent/" + taskId + "/retry",
                idempotencyKey,
                new RetryRequest(),
                () -> ApiResponse.ok(agentService.retryFailedStep(taskId))
        );
        return withIdempotencyHeaders(result);
    }

    @PostMapping("/agent/{taskId}/wait")
    public ResponseEntity<ApiResponse<AgentTaskResponse>> waitForUser(
            @PathVariable String taskId,
            @RequestParam String question,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey
    ) {
        IdempotencyResult<ApiResponse<AgentTaskResponse>> result = idempotencyService.execute(
                "POST:/agent/" + taskId + "/wait",
                idempotencyKey,
                new WaitRequest(question),
                () -> ApiResponse.ok(agentService.waitForUser(taskId, question))
        );
        return withIdempotencyHeaders(result);
    }

    private ResponseEntity<ApiResponse<AgentTaskResponse>> withIdempotencyHeaders(IdempotencyResult<ApiResponse<AgentTaskResponse>> result) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(IDEMPOTENCY_REPLAY_HEADER, String.valueOf(result.replayed()));
        return ResponseEntity.ok().headers(headers).body(result.data());
    }

    private record RetryRequest() {
    }

    private record WaitRequest(String question) {
    }
}
