package com.asamu.aiagent.llm;

import com.asamu.aiagent.config.AgentProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class LlmService {

    private final List<LlmProvider> providers;
    private final AgentProperties properties;

    public LlmService(List<LlmProvider> providers, AgentProperties properties) {
        this.providers = providers;
        this.properties = properties;
    }

    public LlmResponse chat(String providerOverride,
                            String modelOverride,
                            Double temperatureOverride,
                            String sessionId,
                            String userId,
                            String message,
                            List<String> recalledMemory) {
        String providerName = normalizeProvider(providerOverride);
        AgentProperties.Provider providerConfig = resolveProviderConfig(providerName);
        String model = (modelOverride == null || modelOverride.isBlank()) ? providerConfig.getModel() : modelOverride;
        double temperature = temperatureOverride == null ? providerConfig.getTemperature() : temperatureOverride;

        LlmProvider provider = providers.stream()
                .filter(p -> p.supports(providerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported provider: " + providerName));

        LlmRequest request = new LlmRequest(sessionId, userId, message, model, temperature, recalledMemory);
        RuntimeException lastError = null;
        for (int attempt = 0; attempt <= providerConfig.getRetries(); attempt++) {
            try {
                return CompletableFuture.supplyAsync(() -> provider.generate(request))
                        .get(providerConfig.getTimeoutMillis(), TimeUnit.MILLISECONDS);
            } catch (Exception ex) {
                lastError = new RuntimeException("provider call failed after " + Duration.ofMillis(providerConfig.getTimeoutMillis()), ex);
            }
        }
        throw lastError == null ? new RuntimeException("provider call failed") : lastError;
    }

    private String normalizeProvider(String providerOverride) {
        if (providerOverride != null && !providerOverride.isBlank()) {
            return providerOverride;
        }
        return properties.getModel().getDefaultProvider();
    }

    private AgentProperties.Provider resolveProviderConfig(String providerName) {
        Map<String, AgentProperties.Provider> map = properties.getModel().getProviders();
        AgentProperties.Provider configured = map.get(providerName);
        if (configured != null) {
            return configured;
        }
        AgentProperties.Provider fallback = new AgentProperties.Provider();
        fallback.setModel(properties.getModel().getDefaultModel());
        fallback.setTemperature(properties.getModel().getDefaultTemperature());
        return fallback;
    }
}
