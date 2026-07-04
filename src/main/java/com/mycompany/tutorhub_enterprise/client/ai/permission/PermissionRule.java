package com.mycompany.tutorhub_enterprise.client.ai.permission;

public final class PermissionRule {

    private final String toolName;
    private final PermissionDecision decision;
    private final String reason;

    public PermissionRule(String toolName, PermissionDecision decision, String reason) {
        this.toolName = toolName == null ? "*" : toolName.trim();
        this.decision = decision == null ? PermissionDecision.ASK : decision;
        this.reason = reason == null ? "" : reason;
    }

    public boolean matches(String candidateToolName) {
        return "*".equals(toolName) || toolName.equals(candidateToolName);
    }

    public String getToolName() {
        return toolName;
    }

    public PermissionDecision getDecision() {
        return decision;
    }

    public String getReason() {
        return reason;
    }
}
