package com.mycompany.tutorhub_enterprise.client.ai.mcp;

public final class McpToolCallResult {

    private final String callId;
    private final String serverName;
    private final String toolName;
    private final boolean success;
    private final String output;
    private final String error;

    private McpToolCallResult(String callId, String serverName, String toolName,
                              boolean success, String output, String error) {
        this.callId = clean(callId);
        this.serverName = clean(serverName);
        this.toolName = clean(toolName);
        this.success = success;
        this.output = output == null ? "" : output;
        this.error = error == null ? "" : error;
    }

    public static McpToolCallResult success(McpToolCallSpec spec, String output) {
        return new McpToolCallResult(spec == null ? "" : spec.getId(),
                spec == null ? "" : spec.getServerName(),
                spec == null ? "" : spec.getToolName(),
                true,
                output,
                "");
    }

    public static McpToolCallResult failure(McpToolCallSpec spec, String error) {
        return new McpToolCallResult(spec == null ? "" : spec.getId(),
                spec == null ? "" : spec.getServerName(),
                spec == null ? "" : spec.getToolName(),
                false,
                "",
                error);
    }

    public String getCallId() {
        return callId;
    }

    public String getServerName() {
        return serverName;
    }

    public String getToolName() {
        return toolName;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
