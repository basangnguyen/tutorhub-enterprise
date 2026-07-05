package com.mycompany.tutorhub_enterprise.client.ai.mcp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.Instant;
import java.util.UUID;

public final class McpToolCallSpec {

    private final String id;
    private final String serverName;
    private final String toolName;
    private final JsonObject arguments;
    private final String reason;
    private final Instant createdAt;

    public McpToolCallSpec(String serverName, String toolName, String argumentsJson, String reason) {
        this(UUID.randomUUID().toString(), serverName, toolName, parseArguments(argumentsJson), reason, Instant.now());
    }

    public McpToolCallSpec(String id, String serverName, String toolName,
                           JsonObject arguments, String reason, Instant createdAt) {
        this.id = cleanOrDefault(id, UUID.randomUUID().toString());
        this.serverName = cleanOrDefault(serverName, "");
        this.toolName = cleanOrDefault(toolName, "");
        this.arguments = arguments == null ? new JsonObject() : arguments.deepCopy();
        this.reason = cleanOrDefault(reason, "AI proposed MCP tool call");
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        validate();
    }

    public String getId() {
        return id;
    }

    public String getServerName() {
        return serverName;
    }

    public String getToolName() {
        return toolName;
    }

    public JsonObject getArguments() {
        return arguments.deepCopy();
    }

    public String getArgumentsJson() {
        return arguments.toString();
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private void validate() {
        if (serverName.isBlank()) {
            throw new IllegalArgumentException("MCP server name is required");
        }
        if (toolName.isBlank()) {
            throw new IllegalArgumentException("MCP tool name is required");
        }
    }

    private static JsonObject parseArguments(String argumentsJson) {
        String clean = argumentsJson == null || argumentsJson.trim().isEmpty()
                ? "{}"
                : argumentsJson.trim();
        try {
            if (!JsonParser.parseString(clean).isJsonObject()) {
                throw new IllegalArgumentException("MCP tool arguments must be a JSON object");
            }
            return JsonParser.parseString(clean).getAsJsonObject();
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid MCP tool arguments JSON: " + ex.getMessage());
        }
    }

    private static String cleanOrDefault(String value, String defaultValue) {
        String clean = value == null ? "" : value.trim();
        return clean.isEmpty() ? defaultValue : clean;
    }
}
