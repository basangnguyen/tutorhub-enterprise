package com.mycompany.tutorhub_enterprise.client.ai.tool;

import java.util.Map;

public interface AgentTool {

    String name();

    String description();

    Map<String, String> parameters();

    ToolCallResult execute(ToolCallRequest request);

    default String describeForPrompt() {
        StringBuilder builder = new StringBuilder();
        builder.append(name()).append(": ").append(description());
        if (!parameters().isEmpty()) {
            builder.append("\nParameters:");
            parameters().forEach((key, value) ->
                    builder.append("\n- ").append(key).append(": ").append(value));
        }
        return builder.toString();
    }
}
