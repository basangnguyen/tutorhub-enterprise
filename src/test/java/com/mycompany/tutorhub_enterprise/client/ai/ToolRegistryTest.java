package com.mycompany.tutorhub_enterprise.client.ai;

import com.mycompany.tutorhub_enterprise.client.ai.tool.AgentTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolRegistry;
import com.mycompany.tutorhub_enterprise.client.ai.command.PendingCommandStore;
import com.mycompany.tutorhub_enterprise.client.ai.patch.PendingPatchStore;
import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolRegistryTest {

    @TempDir
    Path workspace;

    @Test
    void convertsUnexpectedToolRuntimeExceptionToFailureResult() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new AgentTool() {
            @Override
            public String name() {
                return "broken";
            }

            @Override
            public String description() {
                return "Broken test tool";
            }

            @Override
            public Map<String, String> parameters() {
                return Map.of();
            }

            @Override
            public ToolCallResult execute(ToolCallRequest request) {
                throw new IllegalStateException("boom");
            }
        });

        ToolCallResult result = registry.execute(ToolCallRequest.of("broken", Map.of()));

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("boom"));
    }

    @Test
    void phase9DefaultsIncludeMemoryTool() throws Exception {
        AiLongTermMemoryStore store = new AiLongTermMemoryStore("phase9_registry_user", "phase9_registry_conversation");
        store.clear();

        ToolRegistry registry = ToolRegistry.phase9AgentDefaults(
                new WorkspaceBoundary(workspace),
                new PendingPatchStore(),
                new PendingCommandStore(),
                store);

        assertTrue(registry.find("remember_note").isPresent());
        assertTrue(registry.find("propose_command").isPresent());

        store.clear();
    }
}
