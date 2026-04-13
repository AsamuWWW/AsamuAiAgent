package com.asamu.aiagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "asamu.ai")
public class AgentProperties {

    private final Model model = new Model();
    private final Memory memory = new Memory();
    private final Security security = new Security();
    private final Tool tool = new Tool();

    public Model getModel() {
        return model;
    }

    public Memory getMemory() {
        return memory;
    }

    public Security getSecurity() {
        return security;
    }

    public Tool getTool() {
        return tool;
    }

    public static class Model {
        private String defaultProvider = "openai-compatible";
        private String defaultModel = "gpt-4o-mini";
        private double defaultTemperature = 0.2;
        private final Map<String, Provider> providers = new HashMap<>();

        public String getDefaultProvider() {
            return defaultProvider;
        }

        public void setDefaultProvider(String defaultProvider) {
            this.defaultProvider = defaultProvider;
        }

        public String getDefaultModel() {
            return defaultModel;
        }

        public void setDefaultModel(String defaultModel) {
            this.defaultModel = defaultModel;
        }

        public double getDefaultTemperature() {
            return defaultTemperature;
        }

        public void setDefaultTemperature(double defaultTemperature) {
            this.defaultTemperature = defaultTemperature;
        }

        public Map<String, Provider> getProviders() {
            return providers;
        }
    }

    public static class Provider {
        private String model = "gpt-4o-mini";
        private double temperature = 0.2;
        private int timeoutMillis = 3000;
        private int retries = 1;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getTimeoutMillis() {
            return timeoutMillis;
        }

        public void setTimeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }

        public int getRetries() {
            return retries;
        }

        public void setRetries(int retries) {
            this.retries = retries;
        }
    }

    public static class Memory {
        private int shortTermLimit = 20;
        private int longTermRecall = 10;
        private String persistenceFile = "/tmp/asamu-ai-agent-memory.jsonl";

        public int getShortTermLimit() {
            return shortTermLimit;
        }

        public void setShortTermLimit(int shortTermLimit) {
            this.shortTermLimit = shortTermLimit;
        }

        public int getLongTermRecall() {
            return longTermRecall;
        }

        public void setLongTermRecall(int longTermRecall) {
            this.longTermRecall = longTermRecall;
        }

        public String getPersistenceFile() {
            return persistenceFile;
        }

        public void setPersistenceFile(String persistenceFile) {
            this.persistenceFile = persistenceFile;
        }
    }

    public static class Security {
        private String apiKey = "";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public static class Tool {
        private String defaultRole = "user";
        private String fileReadBasePath = "/home/runner/work/AsamuAiAgent/AsamuAiAgent";
        private List<String> whitelist = new ArrayList<>(List.of("time.now", "http.get", "file.read"));

        public String getDefaultRole() {
            return defaultRole;
        }

        public void setDefaultRole(String defaultRole) {
            this.defaultRole = defaultRole;
        }

        public String getFileReadBasePath() {
            return fileReadBasePath;
        }

        public void setFileReadBasePath(String fileReadBasePath) {
            this.fileReadBasePath = fileReadBasePath;
        }

        public List<String> getWhitelist() {
            return whitelist;
        }

        public void setWhitelist(List<String> whitelist) {
            this.whitelist = whitelist;
        }
    }
}
