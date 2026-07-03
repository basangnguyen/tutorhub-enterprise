package com.mycompany.tutorhub_enterprise.client.ai;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public class LangChain4jAiAgentService implements AiAgentService {

    public static final String PROVIDER_KEY = "langchain4j-ollama";

    private final StreamingChatModel model;
    private final String modelName;
    private final String baseUrl;

    public LangChain4jAiAgentService() {
        this(readConfig("tutorhub.ai.ollama.baseUrl", "TUTORHUB_OLLAMA_BASE_URL", "http://localhost:11434"),
                readConfig("tutorhub.ai.ollama.model", "TUTORHUB_OLLAMA_MODEL", "llama3.2"));
    }

    public LangChain4jAiAgentService(String baseUrl, String modelName) {
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.model = OllamaStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.2)
                .timeout(Duration.ofSeconds(90))
                .build();
    }

    @Override
    public AiAgentStreamHandle streamChat(AiAgentRequest request, AiAgentStreamCallback callback) {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        try {
            model.chat(request.getMessage(), new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    if (!cancelled.get()) {
                        callback.onDelta(partialResponse);
                    }
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    if (!cancelled.get()) {
                        callback.onComplete();
                    }
                }

                @Override
                public void onError(Throwable error) {
                    if (!cancelled.get()) {
                        callback.onError(error instanceof Exception
                                ? (Exception) error
                                : new RuntimeException(error));
                    }
                }
            });
        } catch (Exception ex) {
            callback.onError(ex);
        }
        return () -> cancelled.set(true);
    }

    @Override
    public String getProviderName() {
        return "LangChain4j Ollama (" + modelName + " @ " + baseUrl + ")";
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
