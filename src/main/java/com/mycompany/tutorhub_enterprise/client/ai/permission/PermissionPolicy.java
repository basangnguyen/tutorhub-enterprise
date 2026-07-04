package com.mycompany.tutorhub_enterprise.client.ai.permission;

import java.util.ArrayList;
import java.util.List;

public final class PermissionPolicy {

    private final List<PermissionRule> denyRules = new ArrayList<>();
    private final List<PermissionRule> askRules = new ArrayList<>();
    private final List<PermissionRule> allowRules = new ArrayList<>();

    public static PermissionPolicy phase7Defaults() {
        PermissionPolicy policy = new PermissionPolicy();
        policy.deny("run_command", "Command execution is not available in Phase 7.");
        policy.deny("shell", "Shell execution is not available in Phase 7.");
        policy.allow("list_files", "Read-only workspace listing.");
        policy.allow("read_file", "Read-only file inspection.");
        policy.allow("search_text", "Read-only code search.");
        policy.allow("get_project_info", "Read-only project summary.");
        policy.allow("propose_patch", "Creates a pending diff only; does not write files.");
        policy.ask("apply_patch", "Writes to disk and requires explicit user approval.");
        return policy;
    }

    public static PermissionPolicy phase8Defaults() {
        PermissionPolicy policy = phase7DefaultsWithoutCommandDeny();
        policy.allow("git_status", "Runs fixed read-only git inspection commands.");
        policy.allow("propose_command", "Creates a pending command proposal only; does not execute.");
        policy.ask("run_command", "Executes an approved command inside the workspace.");
        return policy;
    }

    public static PermissionPolicy phase9Defaults() {
        PermissionPolicy policy = phase8Defaults();
        policy.allow("remember_note", "Stores a filtered long-term memory note.");
        return policy;
    }

    private static PermissionPolicy phase7DefaultsWithoutCommandDeny() {
        PermissionPolicy policy = new PermissionPolicy();
        policy.deny("shell", "Shell execution is not available.");
        policy.allow("list_files", "Read-only workspace listing.");
        policy.allow("read_file", "Read-only file inspection.");
        policy.allow("search_text", "Read-only code search.");
        policy.allow("get_project_info", "Read-only project summary.");
        policy.allow("propose_patch", "Creates a pending diff only; does not write files.");
        policy.ask("apply_patch", "Writes to disk and requires explicit user approval.");
        return policy;
    }

    public PermissionPolicy deny(String toolName, String reason) {
        denyRules.add(new PermissionRule(toolName, PermissionDecision.DENY, reason));
        return this;
    }

    public PermissionPolicy ask(String toolName, String reason) {
        askRules.add(new PermissionRule(toolName, PermissionDecision.ASK, reason));
        return this;
    }

    public PermissionPolicy allow(String toolName, String reason) {
        allowRules.add(new PermissionRule(toolName, PermissionDecision.ALLOW, reason));
        return this;
    }

    public PermissionDecision decide(String toolName, boolean userApproved) {
        String safeTool = toolName == null ? "" : toolName.trim();
        if (matches(denyRules, safeTool)) {
            return PermissionDecision.DENY;
        }
        if (matches(askRules, safeTool)) {
            return userApproved ? PermissionDecision.ALLOW : PermissionDecision.ASK;
        }
        if (matches(allowRules, safeTool)) {
            return PermissionDecision.ALLOW;
        }
        return PermissionDecision.ASK;
    }

    private boolean matches(List<PermissionRule> rules, String toolName) {
        return rules.stream().anyMatch(rule -> rule.matches(toolName));
    }
}
