package com.mycompany.tutorhub_enterprise.client.ai;

public interface AiAgentStreamCallback {
    void onDelta(String delta);
    void onComplete();
    void onError(Exception error);
    default void onAudio(String audioUrl) {}
}
