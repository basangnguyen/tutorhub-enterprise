package com.mycompany.tutorhub_enterprise.client.ai.tool.impl;

import com.mycompany.tutorhub_enterprise.client.ai.command.CommandRunner;
import com.mycompany.tutorhub_enterprise.client.ai.command.CommandPolicy;
import com.mycompany.tutorhub_enterprise.client.ai.command.CommandResult;
import com.mycompany.tutorhub_enterprise.client.ai.command.CommandSpec;
import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;
import com.mycompany.tutorhub_enterprise.client.ai.tool.AgentTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GitStatusTool implements AgentTool {

    private final CommandRunner commandRunner;

    public GitStatusTool(WorkspaceBoundary boundary) {
        this.commandRunner = new CommandRunner(boundary, new CommandPolicy());
    }

    @Override
    public String name() {
        return "git_status";
    }

    @Override
    public String description() {
        return "Run fixed read-only git status and diff-stat commands inside the workspace.";
    }

    @Override
    public Map<String, String> parameters() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("workingDirectory", "Optional relative directory inside workspace. Defaults to root.");
        return params;
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        String workingDirectory = request.getArgument("workingDirectory", ".");
        CommandResult status = commandRunner.run(new CommandSpec(
                List.of("git", "status", "--short"), workingDirectory, 30, "read git status"));
        CommandResult diff = commandRunner.run(new CommandSpec(
                List.of("git", "diff", "--stat"), workingDirectory, 30, "read git diff stat"));
        String output = "git status --short\n"
                + safeOutput(status)
                + "\n\ngit diff --stat\n"
                + safeOutput(diff);
        if (!status.isSuccess() && !diff.isSuccess()) {
            return ToolCallResult.failure(output)
                    .withMetadata("status", "failed");
        }
        return ToolCallResult.success(output)
                .withMetadata("status", "completed");
    }

    private String safeOutput(CommandResult result) {
        if (result == null) {
            return "(no result)";
        }
        String output = result.getOutput().isBlank() ? result.getError() : result.getOutput();
        return output == null || output.isBlank() ? "(empty)" : output.trim();
    }
}
