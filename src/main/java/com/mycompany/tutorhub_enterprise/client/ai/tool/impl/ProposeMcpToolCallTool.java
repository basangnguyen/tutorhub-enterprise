package com.mycompany.tutorhub_enterprise.client.ai.tool.impl;

import com.mycompany.tutorhub_enterprise.client.ai.mcp.McpServerConfig;
import com.mycompany.tutorhub_enterprise.client.ai.mcp.McpServerRegistry;
import com.mycompany.tutorhub_enterprise.client.ai.mcp.McpToolCallSpec;
import com.mycompany.tutorhub_enterprise.client.ai.mcp.PendingMcpToolCallStore;
import com.mycompany.tutorhub_enterprise.client.ai.tool.AgentTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ProposeMcpToolCallTool implements AgentTool {

    private final McpServerRegistry registry;
    private final PendingMcpToolCallStore pendingStore;

    public ProposeMcpToolCallTool(McpServerRegistry registry, PendingMcpToolCallStore pendingStore) {
        this.registry = registry;
        this.pendingStore = pendingStore;
    }

    @Override
    public String name() {
        return "propose_mcp_tool_call";
    }

    @Override
    public String description() {
        return "Create a pending external MCP tool call proposal. Does not call the external server. User approval is required before execution.";
    }

    @Override
    public Map<String, String> parameters() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("serverName", "Required configured MCP server name.");
        params.put("toolName", "Required MCP tool name from mcp_list_tools.");
        params.put("argumentsJson", "Required JSON object string for MCP tool arguments.");
        params.put("reason", "Short reason shown to the user.");
        return params;
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        try {
            String serverName = request.getArgument("serverName", "").trim();
            if (findServer(serverName) == null) {
                return ToolCallResult.failure("Configured MCP server not found: " + serverName);
            }
            McpToolCallSpec spec = new McpToolCallSpec(
                    serverName,
                    request.getArgument("toolName", ""),
                    request.getArgument("argumentsJson", "{}"),
                    request.getArgument("reason", "AI proposed MCP tool call"));
            pendingStore.add(spec);
            String output = "MCP tool call proposal created and waiting for user approval.\n"
                    + "MCP call ID: " + spec.getId() + "\n"
                    + "Server: " + spec.getServerName() + "\n"
                    + "Tool: " + spec.getToolName() + "\n"
                    + "Arguments: " + spec.getArgumentsJson() + "\n"
                    + "Reason: " + spec.getReason();
            return ToolCallResult.success(output)
                    .withMetadata("mcpCallId", spec.getId())
                    .withMetadata("serverName", spec.getServerName())
                    .withMetadata("toolName", spec.getToolName())
                    .withMetadata("argumentsJson", spec.getArgumentsJson())
                    .withMetadata("reason", spec.getReason())
                    .withMetadata("approvalRequired", "true");
        } catch (Exception ex) {
            return ToolCallResult.failure(ex.getMessage());
        }
    }

    private McpServerConfig findServer(String serverName) {
        if (registry == null || serverName == null || serverName.isBlank()) {
            return null;
        }
        for (McpServerConfig server : registry.getServers()) {
            if (serverName.equals(server.getName())) {
                return server;
            }
        }
        return null;
    }
}
