package com.mycompany.tutorhub_enterprise.client.ai;

public final class AiAgentProviderConfig {

    public static final String PROVIDER_LAVIE = "lavie";
    public static final String PROVIDER_OLLAMA = "langchain4j-ollama";
    public static final String PROVIDER_OPENAI_COMPATIBLE = "openai-compatible";
    public static final String DEFAULT_OLLAMA_BASE_URL = "http://localhost:11434";
    public static final String DEFAULT_OLLAMA_MODEL = "llama3.2";
    public static final String DEFAULT_OPENAI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai/";
    public static final String DEFAULT_OPENAI_MODEL = "gemini-2.5-flash";

    private final String provider;
    private final String ollamaBaseUrl;
    private final String ollamaModel;
    private final String openAiBaseUrl;
    private final String openAiModel;
    private final String openAiApiKey;

    private AiAgentProviderConfig(String provider, String ollamaBaseUrl, String ollamaModel,
                                  String openAiBaseUrl, String openAiModel, String openAiApiKey) {
        this.provider = normalizeProvider(provider);
        this.ollamaBaseUrl = cleanOrDefault(ollamaBaseUrl, DEFAULT_OLLAMA_BASE_URL);
        this.ollamaModel = cleanOrDefault(ollamaModel, DEFAULT_OLLAMA_MODEL);
        this.openAiBaseUrl = cleanOrDefault(openAiBaseUrl, DEFAULT_OPENAI_BASE_URL);
        this.openAiModel = cleanOrDefault(openAiModel, DEFAULT_OPENAI_MODEL);
        this.openAiApiKey = clean(openAiApiKey);
    }

    public static AiAgentProviderConfig defaults() {
        return new AiAgentProviderConfig(PROVIDER_LAVIE, DEFAULT_OLLAMA_BASE_URL, DEFAULT_OLLAMA_MODEL,
                DEFAULT_OPENAI_BASE_URL, DEFAULT_OPENAI_MODEL, "");
    }

    public static AiAgentProviderConfig of(String provider, String ollamaBaseUrl, String ollamaModel) {
        return new AiAgentProviderConfig(provider, ollamaBaseUrl, ollamaModel,
                DEFAULT_OPENAI_BASE_URL, DEFAULT_OPENAI_MODEL, "");
    }

    public static AiAgentProviderConfig of(String provider, String ollamaBaseUrl, String ollamaModel,
                                           String openAiBaseUrl, String openAiModel, String openAiApiKey) {
        return new AiAgentProviderConfig(provider, ollamaBaseUrl, ollamaModel,
                openAiBaseUrl, openAiModel, openAiApiKey);
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

    public String getOpenAiBaseUrl() {
        return openAiBaseUrl;
    }

    public String getOpenAiModel() {
        return openAiModel;
    }

    public String getOpenAiApiKey() {
        return openAiApiKey;
    }

    public boolean isOllama() {
        return PROVIDER_OLLAMA.equals(provider);
    }

    public boolean isOpenAiCompatible() {
        return PROVIDER_OPENAI_COMPATIBLE.equals(provider);
    }

    public String getDisplayName() {
        if (isOllama()) {
            return "LangChain4j Ollama";
        }
        if (isOpenAiCompatible()) {
            return "OpenAI-compatible";
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
        if (PROVIDER_OPENAI_COMPATIBLE.equals(normalized)
                || "openai".equals(normalized)
                || "openai-compatible".equals(normalized)
                || "openai_compatible".equals(normalized)) {
            return PROVIDER_OPENAI_COMPATIBLE;
        }
        return PROVIDER_LAVIE;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String cleanOrDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }
}
