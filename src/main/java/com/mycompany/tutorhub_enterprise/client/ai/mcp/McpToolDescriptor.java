package com.mycompany.tutorhub_enterprise.client.ai.mcp;

public final class McpToolDescriptor {

    private final String serverName;
    private final String name;
    private final String description;

    public McpToolDescriptor(String serverName, String name, String description) {
        this.serverName = clean(serverName);
        this.name = clean(name);
        this.description = clean(description);
    }

    public String getServerName() {
        return serverName;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
