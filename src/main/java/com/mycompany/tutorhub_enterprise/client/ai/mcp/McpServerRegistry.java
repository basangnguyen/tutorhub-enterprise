package com.mycompany.tutorhub_enterprise.client.ai.mcp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class McpServerRegistry {

    public static final String ENV_ENDPOINTS = "TUTORHUB_MCP_HTTP_ENDPOINTS";
    public static final String PROPERTY_ENDPOINTS = "tutorhub.mcp.httpEndpoints";

    private final List<McpServerConfig> servers;

    public McpServerRegistry(List<McpServerConfig> servers) {
        this.servers = servers == null ? new ArrayList<>() : new ArrayList<>(servers);
    }

    public static McpServerRegistry fromEnvironment() {
        String configured = System.getProperty(PROPERTY_ENDPOINTS);
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getenv(ENV_ENDPOINTS);
        }
        return parse(configured);
    }

    public static McpServerRegistry parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new McpServerRegistry(Collections.emptyList());
        }
        List<McpServerConfig> servers = new ArrayList<>();
        String[] parts = value.split(";");
        int index = 1;
        for (String part : parts) {
            String clean = part == null ? "" : part.trim();
            if (clean.isEmpty()) {
                continue;
            }
            String name = "mcp-" + index;
            String url = clean;
            int equals = clean.indexOf('=');
            if (equals > 0) {
                name = clean.substring(0, equals).trim();
                url = clean.substring(equals + 1).trim();
            }
            if (!url.isEmpty()) {
                servers.add(new McpServerConfig(name, url));
                index++;
            }
        }
        return new McpServerRegistry(servers);
    }

    public List<McpServerConfig> getServers() {
        return Collections.unmodifiableList(servers);
    }

    public boolean isEmpty() {
        return servers.isEmpty();
    }
}
