package com.mycompany.tutorhub_enterprise.client.ai.tool.impl;

import com.mycompany.tutorhub_enterprise.client.ai.mcp.McpHttpClient;
import com.mycompany.tutorhub_enterprise.client.ai.mcp.McpServerConfig;
import com.mycompany.tutorhub_enterprise.client.ai.mcp.McpServerRegistry;
import com.mycompany.tutorhub_enterprise.client.ai.mcp.McpToolCallResult;
import com.mycompany.tutorhub_enterprise.client.ai.mcp.McpToolCallSpec;
import com.mycompany.tutorhub_enterprise.client.ai.mcp.PendingMcpToolCallStore;
import com.mycompany.tutorhub_enterprise.client.ai.permission.AuditLog;
import com.mycompany.tutorhub_enterprise.client.ai.permission.PermissionDecision;
import com.mycompany.tutorhub_enterprise.client.ai.permission.PermissionPolicy;
import com.mycompany.tutorhub_enterprise.client.ai.tool.AgentTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RunMcpToolCallTool implements AgentTool {

    private final McpServerRegistry registry;
    private final PendingMcpToolCallStore pendingStore;
    private final PermissionPolicy permissionPolicy;
    private final AuditLog auditLog;
    private final McpHttpClient client;

    public RunMcpToolCallTool(McpServerRegistry registry, PendingMcpToolCallStore pendingStore,
                              PermissionPolicy permissionPolicy, AuditLog auditLog) {
        this(registry, pendingStore, permissionPolicy, auditLog, new McpHttpClient());
    }

    public RunMcpToolCallTool(McpServerRegistry registry, PendingMcpToolCallStore pendingStore,
                              PermissionPolicy permissionPolicy, AuditLog auditLog, McpHttpClient client) {
        this.registry = registry;
        this.pendingStore = pendingStore;
        this.permissionPolicy = permissionPolicy == null ? PermissionPolicy.phase101Defaults() : permissionPolicy;
        this.auditLog = auditLog == null ? new AuditLog() : auditLog;
        this.client = client == null ? new McpHttpClient() : client;
    }

    @Override
    public String name() {
        return "run_mcp_tool_call";
    }

    @Override
    public String description() {
        return "Run an approved pending external MCP tool call. Requires explicit UI approval.";
    }

    @Override
    public Map<String, String> parameters() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mcpCallId", "Required pending MCP call ID.");
        params.put("approved", "Must be true. This is set only by the approval UI.");
        return params;
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        String callId = request.getArgument("mcpCallId", "").trim();
        boolean approved = request.getBooleanArgument("approved", false);
        PermissionDecision decision = permissionPolicy.decide(name(), approved);
        if (decision != PermissionDecision.ALLOW) {
            auditLog.record("run_mcp_tool_call", callId, "", decision.name(), "User approval required");
            return ToolCallResult.failure("User approval is required before calling external MCP tool " + callId);
        }

        McpToolCallSpec spec = pendingStore.find(callId).orElse(null);
        if (spec == null) {
            auditLog.record("run_mcp_tool_call", callId, "", "missing", "MCP call not found");
            return ToolCallResult.failure("MCP call not found or already resolved: " + callId);
        }
        McpServerConfig server = findServer(spec.getServerName());
        if (server == null) {
            auditLog.record("run_mcp_tool_call", callId, spec.getServerName(), "missing", "MCP server not found");
            return ToolCallResult.failure("Configured MCP server not found: " + spec.getServerName());
        }

        try {
            McpToolCallResult result = client.callTool(server, spec);
            pendingStore.remove(callId);
            auditLog.record("run_mcp_tool_call", callId, spec.getServerName() + "/" + spec.getToolName(),
                    result.isSuccess() ? "success" : "failed",
                    result.isSuccess() ? "MCP tool completed" : result.getError());
            return toToolResult(result, spec);
        } catch (Exception ex) {
            pendingStore.remove(callId);
            auditLog.record("run_mcp_tool_call", callId, spec.getServerName() + "/" + spec.getToolName(),
                    "failed", ex.getMessage());
            return toToolResult(McpToolCallResult.failure(spec, ex.getMessage()), spec);
        }
    }

    static ToolCallResult toToolResult(McpToolCallResult result, McpToolCallSpec spec) {
        boolean success = result != null && result.isSuccess();
        ToolCallResult toolResult = success
                ? ToolCallResult.success(result.getOutput())
                : ToolCallResult.failure(result == null ? "MCP tool result is missing" : result.getError());
        return toolResult
                .withMetadata("mcpCallId", spec == null ? "" : spec.getId())
                .withMetadata("serverName", spec == null ? "" : spec.getServerName())
                .withMetadata("toolName", spec == null ? "" : spec.getToolName())
                .withMetadata("argumentsJson", spec == null ? "{}" : spec.getArgumentsJson())
                .withMetadata("output", result == null ? "" : result.getOutput())
                .withMetadata("status", success ? "completed" : "failed");
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
