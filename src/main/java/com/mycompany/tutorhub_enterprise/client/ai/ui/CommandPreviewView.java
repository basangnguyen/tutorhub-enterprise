package com.mycompany.tutorhub_enterprise.client.ai.ui;

import com.google.gson.JsonObject;
import com.mycompany.tutorhub_enterprise.client.ai.command.CommandSpec;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;

public final class CommandPreviewView {

    private CommandPreviewView() {
    }

    public static JsonObject proposal(CommandSpec command) {
        JsonObject json = new JsonObject();
        if (command == null) {
            json.addProperty("commandId", "");
            json.addProperty("status", "missing");
            json.addProperty("message", "Command proposal is missing");
            return json;
        }
        json.addProperty("commandId", command.getId());
        json.addProperty("commandLine", command.getCommandLine());
        json.addProperty("workingDirectory", command.getWorkingDirectory());
        json.addProperty("timeoutSeconds", command.getTimeoutSeconds());
        json.addProperty("reason", command.getReason());
        json.addProperty("status", "pending");
        json.addProperty("message", "Waiting for user approval");
        json.addProperty("createdAt", command.getCreatedAt().toString());
        return json;
    }

    public static JsonObject running(String commandId) {
        JsonObject json = new JsonObject();
        json.addProperty("commandId", commandId == null ? "" : commandId);
        json.addProperty("status", "running");
        json.addProperty("message", "Command is running...");
        return json;
    }

    public static JsonObject result(String commandId, ToolCallResult result) {
        JsonObject json = new JsonObject();
        json.addProperty("commandId", commandId == null ? "" : commandId);
        boolean success = result != null && result.isSuccess();
        json.addProperty("status", success ? "completed" : "failed");
        json.addProperty("message", result == null
                ? "Command result is missing"
                : success ? "Command completed successfully" : result.getError());
        if (result != null) {
            json.addProperty("commandLine", result.getMetadata().getOrDefault("commandLine", ""));
            json.addProperty("exitCode", result.getMetadata().getOrDefault("exitCode", ""));
            json.addProperty("timedOut", result.getMetadata().getOrDefault("timedOut", "false"));
            json.addProperty("durationMillis", result.getMetadata().getOrDefault("durationMillis", ""));
            json.addProperty("output", result.getMetadata().getOrDefault("output", result.getOutput()));
        }
        return json;
    }

    public static JsonObject rejected(CommandSpec command) {
        JsonObject json = proposal(command);
        json.addProperty("status", "rejected");
        json.addProperty("message", "Command rejected by user");
        return json;
    }
}
