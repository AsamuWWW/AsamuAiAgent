package com.asamu.aiagent.agent;

import com.asamu.aiagent.dto.AgentTaskResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class AgentService {

    private final AgentSessionStore sessionStore;

    public AgentService(AgentSessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    public AgentTaskResponse start(String task) {
        String taskId = UUID.randomUUID().toString();
        List<String> todos = List.of(
                "[ ] 理解任务目标",
                "[ ] 制定执行步骤",
                "[ ] 输出阶段结果"
        );
        AgentSession session = new AgentSession(taskId, todos, "RUNNING", "任务已启动: " + task);
        sessionStore.save(session);
        return toResponse(session);
    }

    public AgentTaskResponse resume(String taskId, String answer) {
        AgentSession session = sessionStore.find(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "task not found"));
        session.completeStep("用户补充: " + answer);
        session.setLatestMessage("收到补充信息，继续执行: " + answer);
        if (session.getTodos().stream().noneMatch(it -> it.contains("[ ]"))) {
            session.setStatus("DONE");
            session.setLatestMessage("任务执行完成");
        }
        return toResponse(session);
    }

    private AgentTaskResponse toResponse(AgentSession session) {
        return new AgentTaskResponse(
                session.getTaskId(),
                session.getStatus(),
                session.getTodos(),
                session.getLatestMessage()
        );
    }
}
