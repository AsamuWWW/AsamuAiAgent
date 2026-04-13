package com.asamu.aiagent.llm;

public interface LlmProvider {

    boolean supports(String providerName);

    LlmResponse generate(LlmRequest request);
}
