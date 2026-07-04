package com.mycompany.tutorhub_enterprise.client.ai.tool.impl;

import com.mycompany.tutorhub_enterprise.client.ai.AiLongTermMemoryStore;
import com.mycompany.tutorhub_enterprise.client.ai.tool.AgentTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RememberNoteTool implements AgentTool {

    private final AiLongTermMemoryStore memoryStore;

    public RememberNoteTool(AiLongTermMemoryStore memoryStore) {
        if (memoryStore == null) {
            throw new IllegalArgumentException("Memory store is required");
        }
        this.memoryStore = memoryStore;
    }

    @Override
    public String name() {
        return "remember_note";
    }

    @Override
    public String description() {
        return "Save a concise long-term memory note about stable user preferences or durable project facts. Never store secrets, credentials, source code, or transient observations.";
    }

    @Override
    public Map<String, String> parameters() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("note", "Required concise memory note, maximum about 700 characters.");
        params.put("source", "Optional short reason or source label for the memory note.");
        return params;
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        String note = request == null ? "" : request.getArgument("note", "");
        String source = request == null ? "" : request.getArgument("source", "agent");
        AiLongTermMemoryStore.MemoryWriteResult result = memoryStore.addAuto(note, source);
        if (!result.isSaved()) {
            return ToolCallResult.success("Memory note skipped: " + result.getMessage())
                    .withMetadata("memorySaved", "false")
                    .withMetadata("memoryCount", String.valueOf(result.getSnapshot().getCount()));
        }
        return ToolCallResult.success("Memory note saved. Total long-term memory items: "
                        + result.getSnapshot().getCount())
                .withMetadata("memorySaved", "true")
                .withMetadata("memoryCount", String.valueOf(result.getSnapshot().getCount()));
    }
}
