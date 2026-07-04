package com.mycompany.tutorhub_enterprise.client.ai.tool;

import com.mycompany.tutorhub_enterprise.client.ai.AiLongTermMemoryStore;
import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;
import com.mycompany.tutorhub_enterprise.client.ai.command.PendingCommandStore;
import com.mycompany.tutorhub_enterprise.client.ai.patch.PendingPatchStore;
import com.mycompany.tutorhub_enterprise.client.ai.tool.impl.GitStatusTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.impl.GetProjectInfoTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.impl.ListFilesTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.impl.ProposeCommandTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.impl.ProposePatchTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.impl.ReadFileTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.impl.RememberNoteTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.impl.SearchTextTool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ToolRegistry {

    private final Map<String, AgentTool> tools = new LinkedHashMap<>();

    public static ToolRegistry readOnlyDefaults(WorkspaceBoundary boundary) {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new ListFilesTool(boundary));
        registry.register(new ReadFileTool(boundary));
        registry.register(new SearchTextTool(boundary));
        registry.register(new GetProjectInfoTool(boundary));
        return registry;
    }

    public static ToolRegistry phase7AgentDefaults(WorkspaceBoundary boundary, PendingPatchStore pendingPatchStore) {
        ToolRegistry registry = readOnlyDefaults(boundary);
        registry.register(new ProposePatchTool(boundary, pendingPatchStore));
        return registry;
    }

    public static ToolRegistry phase8AgentDefaults(WorkspaceBoundary boundary,
                                                   PendingPatchStore pendingPatchStore,
                                                   PendingCommandStore pendingCommandStore) {
        ToolRegistry registry = phase7AgentDefaults(boundary, pendingPatchStore);
        registry.register(new GitStatusTool(boundary));
        registry.register(new ProposeCommandTool(pendingCommandStore));
        return registry;
    }

    public static ToolRegistry phase9AgentDefaults(WorkspaceBoundary boundary,
                                                   PendingPatchStore pendingPatchStore,
                                                   PendingCommandStore pendingCommandStore,
                                                   AiLongTermMemoryStore memoryStore) {
        ToolRegistry registry = phase8AgentDefaults(boundary, pendingPatchStore, pendingCommandStore);
        registry.register(new RememberNoteTool(memoryStore));
        return registry;
    }

    public ToolRegistry register(AgentTool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Tool is required");
        }
        String name = tool.name();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tool name is required");
        }
        String key = name.trim();
        if (tools.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate tool name: " + key);
        }
        tools.put(key, tool);
        return this;
    }

    public Optional<AgentTool> find(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(tools.get(name.trim()));
    }

    public ToolCallResult execute(ToolCallRequest request) {
        if (request == null || request.getToolName().isEmpty()) {
            return ToolCallResult.failure("Tool name is required");
        }
        return find(request.getToolName())
                .map(tool -> {
                    try {
                        return tool.execute(request);
                    } catch (RuntimeException ex) {
                        return ToolCallResult.failure("Tool failed: " + ex.getMessage());
                    }
                })
                .orElseGet(() -> ToolCallResult.failure("Unknown tool: " + request.getToolName()));
    }

    public Collection<AgentTool> getTools() {
        return Collections.unmodifiableList(new ArrayList<>(tools.values()));
    }

    public String describeToolsForPrompt() {
        StringBuilder builder = new StringBuilder();
        for (AgentTool tool : tools.values()) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append(tool.describeForPrompt());
        }
        return builder.toString();
    }
}
