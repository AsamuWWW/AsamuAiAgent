package com.asamu.aiagent.dto;

import java.util.List;

public record AgentTaskResponse(
        String taskId,
        String status,
        List<String> todos,
        String latestMessage
) {
}
