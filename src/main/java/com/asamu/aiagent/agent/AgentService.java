package com.asamu.aiagent.agent;

import com.asamu.aiagent.dto.AgentTaskResponse;
import com.asamu.aiagent.dto.agent.AgentStepResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AgentService {

    private final AgentSessionStore sessionStore;

    public AgentService(AgentSessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    public AgentTaskResponse start(String userId, String task) {
        String taskId = UUID.randomUUID().toString();
        List<AgentStep> steps = decompose(task);
        if (!steps.isEmpty()) {
            steps.get(0).markRunning("已开始");
        }
        AgentSession session = new AgentSession(taskId, resolveUserId(userId), task, steps);
        sessionStore.save(session);
        return toResponse(session);
    }

    public AgentTaskResponse resume(String taskId, String answer) {
        AgentSession session = getOrThrow(taskId);
        session.advanceWithFeedback(answer);
        return toResponse(session);
    }

    public AgentTaskResponse getTask(String taskId) {
        return toResponse(getOrThrow(taskId));
    }

    public AgentTaskResponse retryFailedStep(String taskId) {
        AgentSession session = getOrThrow(taskId);
        session.retryFailedStep();
        return toResponse(session);
    }

    public AgentTaskResponse waitForUser(String taskId, String question) {
        AgentSession session = getOrThrow(taskId);
        session.waitForUser(question);
        return toResponse(session);
    }

    private AgentSession getOrThrow(String taskId) {
        return sessionStore.find(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "task not found"));
    }

    private List<AgentStep> decompose(String task) {
        List<String> parsed = new ArrayList<>();
        for (String split : task.split("[，,。;；]")) {
            if (!split.isBlank()) {
                parsed.add(split.trim());
            }
        }
        if (parsed.isEmpty()) {
            parsed = List.of("理解任务目标", "制定执行步骤", "输出阶段结果");
        }
        List<AgentStep> result = new ArrayList<>();
        for (int i = 0; i < parsed.size(); i++) {
            result.add(new AgentStep("step-" + (i + 1), parsed.get(i), AgentStepStatus.PENDING));
        }
        return result;
    }

    private AgentTaskResponse toResponse(AgentSession session) {
        List<AgentStepResponse> stepResponses = session.getSteps().stream()
                .map(step -> new AgentStepResponse(
                        step.getId(),
                        step.getTitle(),
                        step.getStatus().name(),
                        step.getDetail(),
                        step.getAttempts(),
                        step.getUpdatedAt().toString()))
                .toList();
        List<String> todos = stepResponses.stream()
                .map(step -> ("DONE".equals(step.status()) ? "[x] " : "[ ] ") + step.title())
                .toList();
        return new AgentTaskResponse(
                session.getTaskId(),
                session.getStatus().name(),
                stepResponses,
                todos,
                session.getLatestMessage()
        );
    }

    private String resolveUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return "anonymous";
        }
        return userId;
    }
}
