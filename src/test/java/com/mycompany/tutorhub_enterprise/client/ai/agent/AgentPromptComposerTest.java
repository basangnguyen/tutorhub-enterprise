package com.mycompany.tutorhub_enterprise.client.ai.agent;

import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentPromptComposerTest {

    @TempDir
    Path workspace;

    @Test
    void includesProjectInstructionsAndMemoryContext() throws Exception {
        ToolRegistry registry = ToolRegistry.readOnlyDefaults(new WorkspaceBoundary(workspace));
        AgentContext context = AgentContext.builder(registry)
                .projectInstructions("Use small scoped changes.")
                .conversationContext("User asked for Phase 9.")
                .longTermMemoryContext("User prefers Vietnamese.")
                .build();

        String prompt = AgentPromptComposer.compose("Continue", context, AgentConfig.defaults());

        assertTrue(prompt.contains("Phase 10"));
        assertTrue(prompt.contains("Project instructions from AGENTS.md"));
        assertTrue(prompt.contains("Use small scoped changes."));
        assertTrue(prompt.contains("Long-term memory"));
        assertTrue(prompt.contains("User prefers Vietnamese."));
        assertTrue(prompt.contains("Recent conversation context"));
        assertTrue(prompt.contains("mcp_list_tools"));
        assertTrue(prompt.contains("propose_mcp_tool_call"));
    }
}
