package com.asamu.aiagent.llm;

import org.springframework.stereotype.Component;

@Component
public class LocalModelProvider implements LlmProvider {

    @Override
    public boolean supports(String providerName) {
        return "local".equalsIgnoreCase(providerName);
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        String text = "[local] 已处理: " + request.message();
        return new LlmResponse("local", request.model(), text, Math.max(12, request.message().length() / 2));
    }
}
