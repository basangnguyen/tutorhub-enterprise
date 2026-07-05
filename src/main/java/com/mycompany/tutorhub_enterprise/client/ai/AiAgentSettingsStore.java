package com.mycompany.tutorhub_enterprise.client.ai;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class AiAgentSettingsStore {

    private static final String KEY_PROVIDER = "provider";
    private static final String KEY_OLLAMA_BASE_URL = "ollamaBaseUrl";
    private static final String KEY_OLLAMA_MODEL = "ollamaModel";
    private static final String KEY_OPENAI_BASE_URL = "openAiBaseUrl";
    private static final String KEY_OPENAI_MODEL = "openAiModel";

    private AiAgentSettingsStore() {
    }

    public static AiAgentProviderConfig load() {
        Preferences prefs = preferences();
        return AiAgentProviderConfig.of(
                prefs.get(KEY_PROVIDER, AiAgentProviderConfig.PROVIDER_LAVIE),
                prefs.get(KEY_OLLAMA_BASE_URL, AiAgentProviderConfig.DEFAULT_OLLAMA_BASE_URL),
                prefs.get(KEY_OLLAMA_MODEL, AiAgentProviderConfig.DEFAULT_OLLAMA_MODEL),
                prefs.get(KEY_OPENAI_BASE_URL, AiAgentProviderConfig.DEFAULT_OPENAI_BASE_URL),
                prefs.get(KEY_OPENAI_MODEL, AiAgentProviderConfig.DEFAULT_OPENAI_MODEL),
                "");
    }

    public static void save(AiAgentProviderConfig config) {
        if (config == null) {
            return;
        }
        Preferences prefs = preferences();
        prefs.put(KEY_PROVIDER, config.getProvider());
        prefs.put(KEY_OLLAMA_BASE_URL, config.getOllamaBaseUrl());
        prefs.put(KEY_OLLAMA_MODEL, config.getOllamaModel());
        prefs.put(KEY_OPENAI_BASE_URL, config.getOpenAiBaseUrl());
        prefs.put(KEY_OPENAI_MODEL, config.getOpenAiModel());
        flushQuietly(prefs);
    }

    public static void reset() {
        Preferences prefs = preferences();
        prefs.remove(KEY_PROVIDER);
        prefs.remove(KEY_OLLAMA_BASE_URL);
        prefs.remove(KEY_OLLAMA_MODEL);
        prefs.remove(KEY_OPENAI_BASE_URL);
        prefs.remove(KEY_OPENAI_MODEL);
        flushQuietly(prefs);
    }

    private static Preferences preferences() {
        return Preferences.userNodeForPackage(AiAgentSettingsStore.class).node("agent-provider");
    }

    private static void flushQuietly(Preferences prefs) {
        try {
            prefs.flush();
        } catch (BackingStoreException ignored) {
            // Preferences is a convenience store; the runtime config is already applied in memory.
        }
    }
}
