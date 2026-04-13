package com.asamu.aiagent.agent;

import java.util.ArrayList;
import java.util.List;

public class AgentSession {

    private final String taskId;
    private final String userId;
    private final String task;
    private final List<AgentStep> steps;
    private AgentTaskStatus status;
    private String latestMessage;

    public AgentSession(String taskId, String userId, String task, List<AgentStep> steps) {
        this.taskId = taskId;
        this.userId = userId;
        this.task = task;
        this.steps = new ArrayList<>(steps);
        this.status = AgentTaskStatus.RUNNING;
        this.latestMessage = "任务已启动";
    }

    public synchronized void advanceWithFeedback(String answer) {
        AgentStep current = currentStep();
        if (current == null) {
            status = AgentTaskStatus.DONE;
            latestMessage = "任务已完成";
            return;
        }
        current.markDone("用户反馈: " + answer);
        AgentStep next = currentStep();
        if (next == null) {
            status = AgentTaskStatus.DONE;
            latestMessage = "任务执行完成";
            return;
        }
        next.markRunning("继续执行");
        status = AgentTaskStatus.RUNNING;
        latestMessage = "已推进到下一步骤";
    }

    public synchronized void waitForUser(String question) {
        AgentStep current = currentStep();
        if (current != null) {
            current.markWaiting(question);
            status = AgentTaskStatus.WAITING;
            latestMessage = question;
        }
    }

    public synchronized void failCurrentStep(String reason) {
        AgentStep current = currentStep();
        if (current != null) {
            current.markFailed(reason);
            status = AgentTaskStatus.FAILED;
            latestMessage = reason;
        }
    }

    public synchronized void retryFailedStep() {
        AgentStep failed = steps.stream().filter(step -> step.getStatus() == AgentStepStatus.FAILED).findFirst().orElse(null);
        if (failed == null) {
            throw new IllegalStateException("no failed step to retry");
        }
        failed.markRunning("重试中");
        status = AgentTaskStatus.RUNNING;
        latestMessage = "已触发失败步骤重试";
    }

    public synchronized List<AgentStep> getSteps() {
        return List.copyOf(steps);
    }

    public String getTaskId() {
        return taskId;
    }

    public String getUserId() {
        return userId;
    }

    public String getTask() {
        return task;
    }

    public synchronized AgentTaskStatus getStatus() {
        return status;
    }

    public synchronized String getLatestMessage() {
        return latestMessage;
    }

    private AgentStep currentStep() {
        for (AgentStep step : steps) {
            if (step.getStatus() == AgentStepStatus.PENDING || step.getStatus() == AgentStepStatus.RUNNING || step.getStatus() == AgentStepStatus.WAITING || step.getStatus() == AgentStepStatus.FAILED) {
                return step;
            }
        }
        return null;
    }
}
