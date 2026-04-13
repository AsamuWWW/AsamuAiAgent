package com.asamu.aiagent.metrics;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

@Service
public class AppMetricsService {

    private final LongAdder chatRequests = new LongAdder();
    private final LongAdder toolCalls = new LongAdder();
    private final LongAdder toolFailures = new LongAdder();

    public void incChatRequests() {
        chatRequests.increment();
    }

    public void incToolCalls() {
        toolCalls.increment();
    }

    public void incToolFailures() {
        toolFailures.increment();
    }

    public Map<String, Long> snapshot() {
        return Map.of(
                "chat.requests", chatRequests.sum(),
                "tool.calls", toolCalls.sum(),
                "tool.failures", toolFailures.sum()
        );
    }
}
