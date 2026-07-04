package com.mycompany.tutorhub_enterprise.client.ai.agent;

import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentsMdLoaderTest {

    @TempDir
    Path workspace;

    @Test
    void loadsAgentsMdAsUtf8ProjectInstructions() throws Exception {
        Files.writeString(workspace.resolve("AGENTS.md"),
                "Tra loi bang tieng Viet.\nKhong hardcode token.\n",
                StandardCharsets.UTF_8);

        AgentsMdLoader.ProjectInstructionSnapshot snapshot =
                AgentsMdLoader.loadForWorkspace(new WorkspaceBoundary(workspace));

        assertEquals(1, snapshot.getCount());
        assertTrue(snapshot.hasInstructions());
        assertTrue(snapshot.getContext().contains("AGENTS.md"));
        assertTrue(snapshot.getContext().contains("Khong hardcode token"));
    }

    @Test
    void returnsEmptySnapshotWhenAgentsMdIsMissing() throws Exception {
        AgentsMdLoader.ProjectInstructionSnapshot snapshot =
                AgentsMdLoader.loadForWorkspace(new WorkspaceBoundary(workspace));

        assertEquals(0, snapshot.getCount());
        assertFalse(snapshot.hasInstructions());
    }
}
