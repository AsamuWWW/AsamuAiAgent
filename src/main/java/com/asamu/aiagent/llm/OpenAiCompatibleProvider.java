package com.asamu.aiagent.llm;

import org.springframework.stereotype.Component;

@Component
public class OpenAiCompatibleProvider implements LlmProvider {

    @Override
    public boolean supports(String providerName) {
        return "openai-compatible".equalsIgnoreCase(providerName);
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        String text = "[openai-compatible] 已处理: " + request.message();
        return new LlmResponse("openai-compatible", request.model(), text, Math.max(20, request.message().length()));
    }
}
