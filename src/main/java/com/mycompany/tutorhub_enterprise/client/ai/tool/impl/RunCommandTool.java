package com.mycompany.tutorhub_enterprise.client.ai.tool.impl;

import com.mycompany.tutorhub_enterprise.client.ai.command.CommandPolicy;
import com.mycompany.tutorhub_enterprise.client.ai.command.CommandResult;
import com.mycompany.tutorhub_enterprise.client.ai.command.CommandRunner;
import com.mycompany.tutorhub_enterprise.client.ai.command.CommandSpec;
import com.mycompany.tutorhub_enterprise.client.ai.command.PendingCommandStore;
import com.mycompany.tutorhub_enterprise.client.ai.permission.AuditLog;
import com.mycompany.tutorhub_enterprise.client.ai.permission.PermissionDecision;
import com.mycompany.tutorhub_enterprise.client.ai.permission.PermissionPolicy;
import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;
import com.mycompany.tutorhub_enterprise.client.ai.tool.AgentTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RunCommandTool implements AgentTool {

    private final CommandRunner commandRunner;
    private final PendingCommandStore pendingCommandStore;
    private final PermissionPolicy permissionPolicy;
    private final AuditLog auditLog;

    public RunCommandTool(WorkspaceBoundary boundary, PendingCommandStore pendingCommandStore,
                          PermissionPolicy permissionPolicy, AuditLog auditLog) {
        this.commandRunner = new CommandRunner(boundary, new CommandPolicy());
        this.pendingCommandStore = pendingCommandStore;
        this.permissionPolicy = permissionPolicy == null ? PermissionPolicy.phase8Defaults() : permissionPolicy;
        this.auditLog = auditLog == null ? new AuditLog() : auditLog;
    }

    @Override
    public String name() {
        return "run_command";
    }

    @Override
    public String description() {
        return "Run an approved pending command inside the selected workspace. Requires explicit UI approval.";
    }

    @Override
    public Map<String, String> parameters() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("commandId", "Required pending command ID.");
        params.put("approved", "Must be true. This is set only by the approval UI.");
        return params;
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        String commandId = request.getArgument("commandId", "").trim();
        boolean approved = request.getBooleanArgument("approved", false);
        PermissionDecision decision = permissionPolicy.decide(name(), approved);
        if (decision != PermissionDecision.ALLOW) {
            auditLog.record("run_command", commandId, "", decision.name(), "User approval required");
            return ToolCallResult.failure("User approval is required before running command " + commandId);
        }

        CommandSpec spec = pendingCommandStore.find(commandId).orElse(null);
        if (spec == null) {
            auditLog.record("run_command", commandId, "", "missing", "Command not found");
            return ToolCallResult.failure("Command not found or already resolved: " + commandId);
        }

        CommandResult result = commandRunner.run(spec);
        pendingCommandStore.remove(commandId);
        auditLog.record("run_command", commandId, spec.getWorkingDirectory(),
                result.isSuccess() ? "success" : "failed",
                result.isSuccess() ? "exit 0" : result.getError());
        return toToolResult(result);
    }

    static ToolCallResult toToolResult(CommandResult result) {
        ToolCallResult toolResult = result.isSuccess()
                ? ToolCallResult.success(result.getOutput())
                : ToolCallResult.failure(result.getError().isBlank() ? result.getOutput() : result.getError());
        return toolResult
                .withMetadata("commandId", result.getCommandId())
                .withMetadata("commandLine", result.getCommandLine())
                .withMetadata("exitCode", String.valueOf(result.getExitCode()))
                .withMetadata("timedOut", String.valueOf(result.isTimedOut()))
                .withMetadata("durationMillis", String.valueOf(result.getDurationMillis()))
                .withMetadata("output", result.getOutput())
                .withMetadata("status", result.isSuccess() ? "completed" : "failed");
    }
}
