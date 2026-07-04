package com.mycompany.tutorhub_enterprise.client.ai;

public final class AiAgentServiceFactory {

    private AiAgentServiceFactory() {
    }

    public static AiAgentService createDefault() {
        return create(loadDefaultConfig());
    }

    public static AiAgentService create(AiAgentProviderConfig config) {
        AiAgentProviderConfig effectiveConfig = config == null ? loadDefaultConfig() : config;
        if (effectiveConfig.isOllama()) {
            return new LangChain4jAiAgentService(
                    effectiveConfig.getOllamaBaseUrl(),
                    effectiveConfig.getOllamaModel());
        }
        return new LavieAiService();
    }

    public static AiAgentProviderConfig loadDefaultConfig() {
        AiAgentProviderConfig savedConfig = AiAgentSettingsStore.load();
        return AiAgentProviderConfig.of(
                readConfig("tutorhub.ai.provider", "TUTORHUB_AI_PROVIDER", savedConfig.getProvider()),
                readConfig("tutorhub.ai.ollama.baseUrl", "TUTORHUB_OLLAMA_BASE_URL", savedConfig.getOllamaBaseUrl()),
                readConfig("tutorhub.ai.ollama.model", "TUTORHUB_OLLAMA_MODEL", savedConfig.getOllamaModel()));
    }

    private static String readConfig(String propertyName, String envName, String defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.trim().isEmpty()) {
            return propertyValue.trim();
        }
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }
        return defaultValue;
    }
}
