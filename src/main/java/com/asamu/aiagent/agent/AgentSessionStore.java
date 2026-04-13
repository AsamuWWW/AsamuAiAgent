package com.asamu.aiagent.agent;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentSessionStore {

    private final Map<String, AgentSession> sessions = new ConcurrentHashMap<>();

    public void save(AgentSession session) {
        sessions.put(session.getTaskId(), session);
    }

    public Optional<AgentSession> find(String taskId) {
        return Optional.ofNullable(sessions.get(taskId));
    }
}
