package com.mycompany.tutorhub_enterprise.client.ai;

public final class AiAgentProviderConfig {

    public static final String PROVIDER_LAVIE = "lavie";
    public static final String PROVIDER_OLLAMA = "langchain4j-ollama";
    public static final String DEFAULT_OLLAMA_BASE_URL = "http://localhost:11434";
    public static final String DEFAULT_OLLAMA_MODEL = "llama3.2";

    private final String provider;
    private final String ollamaBaseUrl;
    private final String ollamaModel;

    private AiAgentProviderConfig(String provider, String ollamaBaseUrl, String ollamaModel) {
        this.provider = normalizeProvider(provider);
        this.ollamaBaseUrl = cleanOrDefault(ollamaBaseUrl, DEFAULT_OLLAMA_BASE_URL);
        this.ollamaModel = cleanOrDefault(ollamaModel, DEFAULT_OLLAMA_MODEL);
    }

    public static AiAgentProviderConfig defaults() {
        return new AiAgentProviderConfig(PROVIDER_LAVIE, DEFAULT_OLLAMA_BASE_URL, DEFAULT_OLLAMA_MODEL);
    }

    public static AiAgentProviderConfig of(String provider, String ollamaBaseUrl, String ollamaModel) {
        return new AiAgentProviderConfig(provider, ollamaBaseUrl, ollamaModel);
    }

    public String getProvider() {
        return provider;
    }

    public String getOllamaBaseUrl() {
        return ollamaBaseUrl;
    }

    public String getOllamaModel() {
        return ollamaModel;
    }

    public boolean isOllama() {
        return PROVIDER_OLLAMA.equals(provider);
    }

    public String getDisplayName() {
        if (isOllama()) {
            return "LangChain4j Ollama";
        }
        return "Lavie / Hugging Face";
    }

    private static String normalizeProvider(String provider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase();
        if (PROVIDER_OLLAMA.equals(normalized)
                || "ollama".equals(normalized)
                || "langchain4j".equals(normalized)) {
            return PROVIDER_OLLAMA;
        }
        return PROVIDER_LAVIE;
    }

    private static String cleanOrDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }
}
