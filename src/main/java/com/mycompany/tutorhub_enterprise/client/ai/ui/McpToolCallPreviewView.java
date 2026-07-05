package com.mycompany.tutorhub_enterprise.client.ai.ui;

import com.google.gson.JsonObject;
import com.mycompany.tutorhub_enterprise.client.ai.mcp.McpToolCallSpec;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;

public final class McpToolCallPreviewView {

    private McpToolCallPreviewView() {
    }

    public static JsonObject proposal(McpToolCallSpec spec) {
        JsonObject json = new JsonObject();
        if (spec == null) {
            json.addProperty("mcpCallId", "");
            json.addProperty("status", "missing");
            json.addProperty("message", "MCP tool call proposal is missing");
            return json;
        }
        json.addProperty("mcpCallId", spec.getId());
        json.addProperty("serverName", spec.getServerName());
        json.addProperty("toolName", spec.getToolName());
        json.addProperty("argumentsJson", spec.getArgumentsJson());
        json.addProperty("reason", spec.getReason());
        json.addProperty("status", "pending");
        json.addProperty("message", "Waiting for user approval");
        json.addProperty("createdAt", spec.getCreatedAt().toString());
        return json;
    }

    public static JsonObject running(String mcpCallId) {
        JsonObject json = new JsonObject();
        json.addProperty("mcpCallId", mcpCallId == null ? "" : mcpCallId);
        json.addProperty("status", "running");
        json.addProperty("message", "MCP tool is running...");
        return json;
    }

    public static JsonObject result(String mcpCallId, ToolCallResult result) {
        JsonObject json = new JsonObject();
        json.addProperty("mcpCallId", mcpCallId == null ? "" : mcpCallId);
        boolean success = result != null && result.isSuccess();
        json.addProperty("status", success ? "completed" : "failed");
        json.addProperty("message", result == null
                ? "MCP tool result is missing"
                : success ? "MCP tool completed successfully" : result.getError());
        if (result != null) {
            json.addProperty("serverName", result.getMetadata().getOrDefault("serverName", ""));
            json.addProperty("toolName", result.getMetadata().getOrDefault("toolName", ""));
            json.addProperty("argumentsJson", result.getMetadata().getOrDefault("argumentsJson", "{}"));
            json.addProperty("output", result.getMetadata().getOrDefault("output", result.getOutput()));
        }
        return json;
    }

    public static JsonObject rejected(McpToolCallSpec spec) {
        JsonObject json = proposal(spec);
        json.addProperty("status", "rejected");
        json.addProperty("message", "MCP tool call rejected by user");
        return json;
    }
}
