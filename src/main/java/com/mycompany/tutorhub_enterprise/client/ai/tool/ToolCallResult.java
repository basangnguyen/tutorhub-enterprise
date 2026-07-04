package com.mycompany.tutorhub_enterprise.client.ai.tool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ToolCallResult {

    private final boolean success;
    private final String output;
    private final String error;
    private final Map<String, String> metadata;

    private ToolCallResult(boolean success, String output, String error, Map<String, String> metadata) {
        this.success = success;
        this.output = output == null ? "" : output;
        this.error = error == null ? "" : error;
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }

    public static ToolCallResult success(String output) {
        return new ToolCallResult(true, output, "", Collections.emptyMap());
    }

    public static ToolCallResult failure(String error) {
        return new ToolCallResult(false, "", error, Collections.emptyMap());
    }

    public ToolCallResult withMetadata(String key, String value) {
        Map<String, String> next = new LinkedHashMap<>(metadata);
        if (key != null && !key.trim().isEmpty() && value != null) {
            next.put(key.trim(), value);
        }
        return new ToolCallResult(success, output, error, next);
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

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
