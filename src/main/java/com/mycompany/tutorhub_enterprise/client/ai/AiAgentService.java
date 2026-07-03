package com.mycompany.tutorhub_enterprise.client.ai;

public interface AiAgentService {
    AiAgentStreamHandle streamChat(AiAgentRequest request, AiAgentStreamCallback callback);

    default String getProviderName() {
        return "AI Agent";
    }
}
