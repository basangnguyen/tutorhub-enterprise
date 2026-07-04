package com.mycompany.tutorhub_enterprise.client.ai.agent;

import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;

public final class AgentModelResponse {

    public enum Type {
        FINAL,
        TOOL_CALL
    }

    private final Type type;
    private final String finalAnswer;
    private final ToolCallRequest toolCall;
    private final String rawText;

    private AgentModelResponse(Type type, String finalAnswer, ToolCallRequest toolCall, String rawText) {
        this.type = type;
        this.finalAnswer = finalAnswer == null ? "" : finalAnswer;
        this.toolCall = toolCall;
        this.rawText = rawText == null ? "" : rawText;
    }

    public static AgentModelResponse finalAnswer(String answer, String rawText) {
        return new AgentModelResponse(Type.FINAL, answer, null, rawText);
    }

    public static AgentModelResponse toolCall(ToolCallRequest toolCall, String rawText) {
        return new AgentModelResponse(Type.TOOL_CALL, "", toolCall, rawText);
    }

    public boolean isToolCall() {
        return type == Type.TOOL_CALL;
    }

    public Type getType() {
        return type;
    }

    public String getFinalAnswer() {
        return finalAnswer;
    }

    public ToolCallRequest getToolCall() {
        return toolCall;
    }

    public String getRawText() {
        return rawText;
    }
}
