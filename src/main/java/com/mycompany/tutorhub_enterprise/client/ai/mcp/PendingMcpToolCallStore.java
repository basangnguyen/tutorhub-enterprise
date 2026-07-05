package com.mycompany.tutorhub_enterprise.client.ai.mcp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PendingMcpToolCallStore {

    private static final int MAX_CALLS = 50;

    private final Map<String, McpToolCallSpec> pending = new LinkedHashMap<>();

    public synchronized McpToolCallSpec add(McpToolCallSpec spec) {
        if (spec == null || spec.getId().isBlank()) {
            throw new IllegalArgumentException("MCP tool call spec is required");
        }
        pending.put(spec.getId(), spec);
        trim();
        return spec;
    }

    public synchronized Optional<McpToolCallSpec> find(String callId) {
        if (callId == null || callId.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(pending.get(callId.trim()));
    }

    public synchronized Optional<McpToolCallSpec> remove(String callId) {
        if (callId == null || callId.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(pending.remove(callId.trim()));
    }

    public synchronized List<McpToolCallSpec> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(pending.values()));
    }

    private void trim() {
        while (pending.size() > MAX_CALLS) {
            String firstKey = pending.keySet().iterator().next();
            pending.remove(firstKey);
        }
    }
}
