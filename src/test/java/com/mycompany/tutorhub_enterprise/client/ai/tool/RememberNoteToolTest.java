package com.mycompany.tutorhub_enterprise.client.ai.tool;

import com.mycompany.tutorhub_enterprise.client.ai.AiLongTermMemoryStore;
import com.mycompany.tutorhub_enterprise.client.ai.tool.impl.RememberNoteTool;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RememberNoteToolTest {

    @Test
    void savesFilteredLongTermMemoryNote() {
        AiLongTermMemoryStore store = new AiLongTermMemoryStore("phase9_tool_user", "phase9_tool_conversation");
        store.clear();
        RememberNoteTool tool = new RememberNoteTool(store);

        ToolCallResult result = tool.execute(ToolCallRequest.of("remember_note", Map.of(
                "note", "This project uses Java Swing and JCEF for the AI chat surface.",
                "source", "unit-test"
        )));

        assertTrue(result.isSuccess());
        assertEquals("true", result.getMetadata().get("memorySaved"));
        assertEquals(1, store.snapshot().getCount());

        store.clear();
    }
}
