package com.mycompany.tutorhub_enterprise.client.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiConversationMemoryTest {

    @Test
    void compactsOlderMessagesInsteadOfDroppingAllContext() {
        AiConversationMemory memory = new AiConversationMemory();

        for (int i = 0; i < 24; i++) {
            memory.rememberUser("old user message " + i);
            memory.rememberAssistant("old assistant message " + i);
        }

        String context = memory.buildContext();

        assertTrue(memory.hasCompactedSummary());
        assertTrue(context.contains("Compacted earlier conversation"));
        assertTrue(context.contains("Recent conversation"));
        assertTrue(context.contains("old assistant message 23"));
    }
}
