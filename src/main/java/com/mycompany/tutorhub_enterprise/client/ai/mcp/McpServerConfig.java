package com.mycompany.tutorhub_enterprise.client.ai.mcp;

public final class McpServerConfig {

    private final String name;
    private final String endpointUrl;

    public McpServerConfig(String name, String endpointUrl) {
        this.name = cleanOrDefault(name, "mcp-server");
        this.endpointUrl = cleanOrDefault(endpointUrl, "");
        if (this.endpointUrl.isEmpty()) {
            throw new IllegalArgumentException("MCP endpoint URL is required");
        }
    }

    public String getName() {
        return name;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    private static String cleanOrDefault(String value, String defaultValue) {
        String clean = value == null ? "" : value.trim();
        return clean.isEmpty() ? defaultValue : clean;
    }
}
