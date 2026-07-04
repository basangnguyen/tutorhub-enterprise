package com.mycompany.tutorhub_enterprise.client.ai.tool.impl;

import com.mycompany.tutorhub_enterprise.client.ai.command.CommandParser;
import com.mycompany.tutorhub_enterprise.client.ai.command.CommandPolicy;
import com.mycompany.tutorhub_enterprise.client.ai.command.CommandSpec;
import com.mycompany.tutorhub_enterprise.client.ai.command.PendingCommandStore;
import com.mycompany.tutorhub_enterprise.client.ai.tool.AgentTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ProposeCommandTool implements AgentTool {

    private final PendingCommandStore pendingCommandStore;
    private final CommandPolicy commandPolicy = new CommandPolicy();

    public ProposeCommandTool(PendingCommandStore pendingCommandStore) {
        this.pendingCommandStore = pendingCommandStore;
    }

    @Override
    public String name() {
        return "propose_command";
    }

    @Override
    public String description() {
        return "Create a pending command proposal. Does not execute. User approval is required before running.";
    }

    @Override
    public Map<String, String> parameters() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("command", "Required single command line. Shell operators are blocked.");
        params.put("workingDirectory", "Optional relative directory inside workspace. Defaults to root.");
        params.put("timeoutSeconds", "Optional timeout from 5 to 300 seconds. Defaults to 120.");
        params.put("reason", "Short reason shown to the user.");
        return params;
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        try {
            String commandLine = request.getArgument("command", "").trim();
            CommandSpec spec = new CommandSpec(
                    CommandParser.parse(commandLine),
                    request.getArgument("workingDirectory", "."),
                    request.getIntArgument("timeoutSeconds", 120, 5, 300),
                    request.getArgument("reason", "AI proposed command"));
            commandPolicy.validate(spec);
            pendingCommandStore.add(spec);
            String output = "Command proposal created and waiting for user approval.\n"
                    + "Command ID: " + spec.getId() + "\n"
                    + "Command: " + spec.getCommandLine() + "\n"
                    + "Working directory: " + spec.getWorkingDirectory() + "\n"
                    + "Timeout: " + spec.getTimeoutSeconds() + "s\n"
                    + "Reason: " + spec.getReason();
            return ToolCallResult.success(output)
                    .withMetadata("commandId", spec.getId())
                    .withMetadata("commandLine", spec.getCommandLine())
                    .withMetadata("workingDirectory", spec.getWorkingDirectory())
                    .withMetadata("timeoutSeconds", String.valueOf(spec.getTimeoutSeconds()))
                    .withMetadata("reason", spec.getReason())
                    .withMetadata("approvalRequired", "true");
        } catch (Exception ex) {
            return ToolCallResult.failure(ex.getMessage());
        }
    }
}
