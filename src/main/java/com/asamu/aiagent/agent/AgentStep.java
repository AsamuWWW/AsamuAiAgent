package com.asamu.aiagent.agent;

import java.time.OffsetDateTime;

public class AgentStep {

    private final String id;
    private final String title;
    private AgentStepStatus status;
    private String detail;
    private int attempts;
    private OffsetDateTime updatedAt;

    public AgentStep(String id, String title, AgentStepStatus status) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.detail = "";
        this.attempts = 0;
        this.updatedAt = OffsetDateTime.now();
    }

    public synchronized void markDone(String detail) {
        this.status = AgentStepStatus.DONE;
        this.detail = detail;
        this.updatedAt = OffsetDateTime.now();
    }

    public synchronized void markWaiting(String detail) {
        this.status = AgentStepStatus.WAITING;
        this.detail = detail;
        this.updatedAt = OffsetDateTime.now();
    }

    public synchronized void markFailed(String detail) {
        this.status = AgentStepStatus.FAILED;
        this.detail = detail;
        this.updatedAt = OffsetDateTime.now();
        this.attempts++;
    }

    public synchronized void markRunning(String detail) {
        this.status = AgentStepStatus.RUNNING;
        this.detail = detail;
        this.updatedAt = OffsetDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public synchronized AgentStepStatus getStatus() {
        return status;
    }

    public synchronized String getDetail() {
        return detail;
    }

    public synchronized int getAttempts() {
        return attempts;
    }

    public synchronized OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
