package com.mycompany.tutorhub_enterprise.client.ai;

public final class AiAgentServiceFactory {

    private static final String DEFAULT_PROVIDER = "lavie";

    private AiAgentServiceFactory() {
    }

    public static AiAgentService createDefault() {
        String provider = readConfig("tutorhub.ai.provider", "TUTORHUB_AI_PROVIDER", DEFAULT_PROVIDER)
                .trim()
                .toLowerCase();
        if (LangChain4jAiAgentService.PROVIDER_KEY.equals(provider)
                || "ollama".equals(provider)
                || "langchain4j".equals(provider)) {
            return new LangChain4jAiAgentService();
        }
        return new LavieAiService();
    }

    private static String readConfig(String propertyName, String envName, String defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.trim().isEmpty()) {
            return propertyValue;
        }
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue;
        }
        return defaultValue;
    }
}
