package com.mycompany.tutorhub_enterprise.client.ai.tool.impl;

import com.mycompany.tutorhub_enterprise.client.ai.mcp.McpHttpClient;
import com.mycompany.tutorhub_enterprise.client.ai.mcp.McpServerConfig;
import com.mycompany.tutorhub_enterprise.client.ai.mcp.McpServerRegistry;
import com.mycompany.tutorhub_enterprise.client.ai.mcp.McpToolDescriptor;
import com.mycompany.tutorhub_enterprise.client.ai.tool.AgentTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class McpListToolsTool implements AgentTool {

    private final McpServerRegistry registry;
    private final McpHttpClient client;

    public McpListToolsTool(McpServerRegistry registry) {
        this(registry, new McpHttpClient());
    }

    public McpListToolsTool(McpServerRegistry registry, McpHttpClient client) {
        this.registry = registry == null ? new McpServerRegistry(List.of()) : registry;
        this.client = client == null ? new McpHttpClient() : client;
    }

    @Override
    public String name() {
        return "mcp_list_tools";
    }

    @Override
    public String description() {
        return "List tools exposed by configured MCP HTTP JSON-RPC servers. This is discovery only and does not call external MCP tools.";
    }

    @Override
    public Map<String, String> parameters() {
        return new LinkedHashMap<>();
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        if (registry.isEmpty()) {
            return ToolCallResult.success("No MCP HTTP endpoints configured. Set "
                    + McpServerRegistry.ENV_ENDPOINTS
                    + " as name=https://server/mcp;name2=http://localhost:port/mcp to enable discovery.");
        }
        StringBuilder output = new StringBuilder();
        for (McpServerConfig server : registry.getServers()) {
            try {
                List<McpToolDescriptor> tools = client.listTools(server);
                output.append("Server ").append(server.getName()).append(" (")
                        .append(server.getEndpointUrl()).append(")\n");
                if (tools.isEmpty()) {
                    output.append("- No tools returned.\n\n");
                    continue;
                }
                for (McpToolDescriptor tool : tools) {
                    output.append("- ").append(tool.getName());
                    if (!tool.getDescription().isBlank()) {
                        output.append(": ").append(tool.getDescription());
                    }
                    output.append('\n');
                }
                output.append('\n');
            } catch (Exception ex) {
                output.append("Server ").append(server.getName()).append(" failed: ")
                        .append(ex.getMessage()).append("\n\n");
            }
        }
        return ToolCallResult.success(output.toString().trim());
    }
}
