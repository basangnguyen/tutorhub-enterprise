package com.mycompany.tutorhub_enterprise.client.ai.tool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ToolCallRequest {

    private final String toolName;
    private final Map<String, String> arguments;

    public ToolCallRequest(String toolName, Map<String, String> arguments) {
        this.toolName = toolName == null ? "" : toolName.trim();
        Map<String, String> safeArguments = new LinkedHashMap<>();
        if (arguments != null) {
            arguments.forEach((key, value) -> {
                if (key != null && !key.trim().isEmpty() && value != null) {
                    safeArguments.put(key.trim(), value);
                }
            });
        }
        this.arguments = Collections.unmodifiableMap(safeArguments);
    }

    public static ToolCallRequest of(String toolName, Map<String, String> arguments) {
        return new ToolCallRequest(toolName, arguments);
    }

    public String getToolName() {
        return toolName;
    }

    public Map<String, String> getArguments() {
        return arguments;
    }

    public String getArgument(String key) {
        return getArgument(key, "");
    }

    public String getArgument(String key, String defaultValue) {
        if (key == null) {
            return defaultValue;
        }
        String value = arguments.get(key);
        return value == null ? defaultValue : value;
    }

    public int getIntArgument(String key, int defaultValue, int min, int max) {
        String raw = getArgument(key, "");
        if (raw.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public boolean getBooleanArgument(String key, boolean defaultValue) {
        String raw = getArgument(key, "");
        if (raw.trim().isEmpty()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(raw.trim())
                || "1".equals(raw.trim())
                || "yes".equalsIgnoreCase(raw.trim())
                || "on".equalsIgnoreCase(raw.trim());
    }
}
