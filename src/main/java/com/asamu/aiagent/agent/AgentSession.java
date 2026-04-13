package com.asamu.aiagent.agent;

import java.util.ArrayList;
import java.util.List;

public class AgentSession {

    private final String taskId;
    private final List<String> todos;
    private String status;
    private String latestMessage;

    public AgentSession(String taskId, List<String> todos, String status, String latestMessage) {
        this.taskId = taskId;
        this.todos = new ArrayList<>(todos);
        this.status = status;
        this.latestMessage = latestMessage;
    }

    public String getTaskId() {
        return taskId;
    }

    public synchronized List<String> getTodos() {
        return List.copyOf(todos);
    }

    public String getStatus() {
        return status;
    }

    public String getLatestMessage() {
        return latestMessage;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLatestMessage(String latestMessage) {
        this.latestMessage = latestMessage;
    }

    public synchronized void completeStep(String stepText) {
        for (int i = 0; i < todos.size(); i++) {
            String todo = todos.get(i);
            if (todo.contains("[ ]")) {
                todos.set(i, todo.replace("[ ]", "[x]") + " -> " + stepText);
                return;
            }
        }
    }
}
