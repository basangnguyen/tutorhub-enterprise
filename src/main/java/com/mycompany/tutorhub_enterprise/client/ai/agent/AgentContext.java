package com.mycompany.tutorhub_enterprise.client.ai.agent;

import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AgentContext {

    private final String userId;
    private final String conversationId;
    private final ToolRegistry toolRegistry;
    private final String projectInstructions;
    private final String conversationContext;
    private final String longTermMemoryContext;
    private final List<AgentToolInvocation> toolInvocations = new ArrayList<>();

    private AgentContext(Builder builder) {
        this.userId = builder.userId;
        this.conversationId = builder.conversationId;
        this.toolRegistry = builder.toolRegistry;
        this.projectInstructions = builder.projectInstructions;
        this.conversationContext = builder.conversationContext;
        this.longTermMemoryContext = builder.longTermMemoryContext;
    }

    public static Builder builder(ToolRegistry toolRegistry) {
        return new Builder(toolRegistry);
    }

    public String getUserId() {
        return userId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }

    public String getProjectInstructions() {
        return projectInstructions;
    }

    public String getConversationContext() {
        return conversationContext;
    }

    public String getLongTermMemoryContext() {
        return longTermMemoryContext;
    }

    public void addToolInvocation(AgentToolInvocation invocation) {
        if (invocation != null) {
            toolInvocations.add(invocation);
        }
    }

    public List<AgentToolInvocation> getToolInvocations() {
        return Collections.unmodifiableList(toolInvocations);
    }

    public static final class Builder {
        private final ToolRegistry toolRegistry;
        private String userId = "tutorhub_desktop";
        private String conversationId = "tutorhub_agent";
        private String projectInstructions = "";
        private String conversationContext = "";
        private String longTermMemoryContext = "";

        private Builder(ToolRegistry toolRegistry) {
            if (toolRegistry == null) {
                throw new IllegalArgumentException("Tool registry is required");
            }
            this.toolRegistry = toolRegistry;
        }

        public Builder userId(String userId) {
            if (userId != null && !userId.trim().isEmpty()) {
                this.userId = userId.trim();
            }
            return this;
        }

        public Builder conversationId(String conversationId) {
            if (conversationId != null && !conversationId.trim().isEmpty()) {
                this.conversationId = conversationId.trim();
            }
            return this;
        }

        public Builder projectInstructions(String projectInstructions) {
            this.projectInstructions = clean(projectInstructions);
            return this;
        }

        public Builder conversationContext(String conversationContext) {
            this.conversationContext = clean(conversationContext);
            return this;
        }

        public Builder longTermMemoryContext(String longTermMemoryContext) {
            this.longTermMemoryContext = clean(longTermMemoryContext);
            return this;
        }

        public AgentContext build() {
            return new AgentContext(this);
        }

        private String clean(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
