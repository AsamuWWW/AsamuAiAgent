package com.asamu.aiagent.controller;

import com.asamu.aiagent.agent.AgentService;
import com.asamu.aiagent.common.ApiResponse;
import com.asamu.aiagent.dto.AgentResumeRequest;
import com.asamu.aiagent.dto.AgentStartRequest;
import com.asamu.aiagent.dto.AgentTaskResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/agent/start")
    public ApiResponse<AgentTaskResponse> start(@Valid @RequestBody AgentStartRequest request) {
        return ApiResponse.ok(agentService.start(request.userId(), request.task()));
    }

    @PostMapping("/agent/{taskId}/resume")
    public ApiResponse<AgentTaskResponse> resume(
            @PathVariable String taskId,
            @Valid @RequestBody AgentResumeRequest request
    ) {
        return ApiResponse.ok(agentService.resume(taskId, request.answer()));
    }

    @GetMapping("/agent/{taskId}")
    public ApiResponse<AgentTaskResponse> taskDetail(@PathVariable String taskId) {
        return ApiResponse.ok(agentService.getTask(taskId));
    }

    @PostMapping("/agent/{taskId}/retry")
    public ApiResponse<AgentTaskResponse> retryFailedStep(@PathVariable String taskId) {
        return ApiResponse.ok(agentService.retryFailedStep(taskId));
    }

    @PostMapping("/agent/{taskId}/wait")
    public ApiResponse<AgentTaskResponse> waitForUser(@PathVariable String taskId, @RequestParam String question) {
        return ApiResponse.ok(agentService.waitForUser(taskId, question));
    }
}
