package com.mycompany.tutorhub_enterprise.client.ai.agent;

import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;

public final class AgentToolInvocation {

    private final int turnIndex;
    private final ToolCallRequest request;
    private final ToolCallResult result;

    public AgentToolInvocation(int turnIndex, ToolCallRequest request, ToolCallResult result) {
        this.turnIndex = turnIndex;
        this.request = request;
        this.result = result;
    }

    public int getTurnIndex() {
        return turnIndex;
    }

    public ToolCallRequest getRequest() {
        return request;
    }

    public ToolCallResult getResult() {
        return result;
    }
}
