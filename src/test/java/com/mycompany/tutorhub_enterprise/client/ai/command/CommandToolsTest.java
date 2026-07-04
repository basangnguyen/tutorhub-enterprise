package com.mycompany.tutorhub_enterprise.client.ai.command;

import com.mycompany.tutorhub_enterprise.client.ai.permission.AuditLog;
import com.mycompany.tutorhub_enterprise.client.ai.permission.PermissionPolicy;
import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;
import com.mycompany.tutorhub_enterprise.client.ai.tool.impl.ProposeCommandTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.impl.RunCommandTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandToolsTest {

    @TempDir
    Path workspace;

    @Test
    void proposeCommandDoesNotRunAndRunCommandRequiresApproval() throws Exception {
        String javaExe = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java").toString();
        PendingCommandStore store = new PendingCommandStore();
        ProposeCommandTool propose = new ProposeCommandTool(store);
        RunCommandTool run = new RunCommandTool(new WorkspaceBoundary(workspace), store,
                PermissionPolicy.phase8Defaults(), new AuditLog(workspace.resolve("audit.log")));

        ToolCallResult proposed = propose.execute(ToolCallRequest.of("propose_command", Map.of(
                "command", "\"" + javaExe + "\" -version",
                "reason", "test java version",
                "timeoutSeconds", "30")));
        String commandId = proposed.getMetadata().get("commandId");

        assertTrue(proposed.isSuccess());
        assertTrue(store.find(commandId).isPresent());

        ToolCallResult denied = run.execute(ToolCallRequest.of("run_command", Map.of("commandId", commandId)));
        assertFalse(denied.isSuccess());
        assertTrue(store.find(commandId).isPresent());

        ToolCallResult completed = run.execute(ToolCallRequest.of("run_command",
                Map.of("commandId", commandId, "approved", "true")));
        assertTrue(completed.isSuccess(), completed.getError());
        assertTrue(completed.getMetadata().get("output").toLowerCase().contains("version"));
        assertTrue(store.find(commandId).isEmpty());
    }
}
