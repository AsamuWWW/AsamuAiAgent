package com.asamu.aiagent.dto;

import com.asamu.aiagent.dto.agent.AgentStepResponse;

import java.util.List;

public record AgentTaskResponse(
        String taskId,
        String status,
        List<AgentStepResponse> steps,
        List<String> todos,
        String latestMessage
) {
}
