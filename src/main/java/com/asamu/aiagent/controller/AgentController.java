package com.asamu.aiagent.controller;

import com.asamu.aiagent.agent.AgentService;
import com.asamu.aiagent.common.ApiResponse;
import com.asamu.aiagent.dto.AgentResumeRequest;
import com.asamu.aiagent.dto.AgentStartRequest;
import com.asamu.aiagent.dto.AgentTaskResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/agent/start")
    public ApiResponse<AgentTaskResponse> start(@Valid @RequestBody AgentStartRequest request) {
        return ApiResponse.ok(agentService.start(request.task()));
    }

    @PostMapping("/agent/{taskId}/resume")
    public ApiResponse<AgentTaskResponse> resume(
            @PathVariable String taskId,
            @Valid @RequestBody AgentResumeRequest request
    ) {
        return ApiResponse.ok(agentService.resume(taskId, request.answer()));
    }
}
