package com.mycompany.tutorhub_enterprise.client.ai.patch;

import com.mycompany.tutorhub_enterprise.client.ai.permission.AuditLog;
import com.mycompany.tutorhub_enterprise.client.ai.permission.PermissionPolicy;
import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;
import com.mycompany.tutorhub_enterprise.client.ai.tool.impl.ApplyPatchTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.impl.ProposePatchTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatchToolsTest {

    @TempDir
    Path workspace;

    @Test
    void proposePatchDoesNotWriteAndApplyRequiresApproval() throws Exception {
        Path file = workspace.resolve("App.java");
        Files.writeString(file, "class App {}\n");
        WorkspaceBoundary boundary = new WorkspaceBoundary(workspace);
        PendingPatchStore store = new PendingPatchStore();
        ProposePatchTool propose = new ProposePatchTool(boundary, store);
        ApplyPatchTool apply = new ApplyPatchTool(boundary, store, PermissionPolicy.phase7Defaults(),
                new AuditLog(workspace.resolve("audit.log")));

        ToolCallResult proposed = propose.execute(ToolCallRequest.of("propose_patch", Map.of(
                "path", "App.java",
                "oldText", "class App {}",
                "newText", "class App { int value = 1; }",
                "reason", "test patch")));
        String patchId = proposed.getMetadata().get("patchId");

        assertTrue(proposed.isSuccess());
        assertEquals("class App {}\n", Files.readString(file));
        assertTrue(store.find(patchId).isPresent());

        ToolCallResult denied = apply.execute(ToolCallRequest.of("apply_patch", Map.of("patchId", patchId)));
        assertFalse(denied.isSuccess());
        assertEquals("class App {}\n", Files.readString(file));

        ToolCallResult applied = apply.execute(ToolCallRequest.of("apply_patch",
                Map.of("patchId", patchId, "approved", "true")));
        assertTrue(applied.isSuccess());
        assertEquals("class App { int value = 1; }\n", Files.readString(file));
        assertTrue(store.find(patchId).isEmpty());
    }
}
