package com.mycompany.tutorhub_enterprise.client.ai.tool.impl;

import com.mycompany.tutorhub_enterprise.client.ai.patch.PatchEngine;
import com.mycompany.tutorhub_enterprise.client.ai.patch.PatchProposal;
import com.mycompany.tutorhub_enterprise.client.ai.patch.PatchResult;
import com.mycompany.tutorhub_enterprise.client.ai.patch.PendingPatchStore;
import com.mycompany.tutorhub_enterprise.client.ai.permission.AuditLog;
import com.mycompany.tutorhub_enterprise.client.ai.permission.PermissionDecision;
import com.mycompany.tutorhub_enterprise.client.ai.permission.PermissionPolicy;
import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;
import com.mycompany.tutorhub_enterprise.client.ai.tool.AgentTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ApplyPatchTool implements AgentTool {

    private final PatchEngine patchEngine;
    private final PendingPatchStore pendingPatchStore;
    private final PermissionPolicy permissionPolicy;
    private final AuditLog auditLog;

    public ApplyPatchTool(WorkspaceBoundary boundary, PendingPatchStore pendingPatchStore,
                          PermissionPolicy permissionPolicy, AuditLog auditLog) {
        this.patchEngine = new PatchEngine(boundary);
        this.pendingPatchStore = pendingPatchStore;
        this.permissionPolicy = permissionPolicy == null ? PermissionPolicy.phase7Defaults() : permissionPolicy;
        this.auditLog = auditLog == null ? new AuditLog() : auditLog;
    }

    @Override
    public String name() {
        return "apply_patch";
    }

    @Override
    public String description() {
        return "Apply an already proposed patch after explicit user approval. Writes to disk.";
    }

    @Override
    public Map<String, String> parameters() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("patchId", "Required pending patch ID.");
        params.put("approved", "Must be true. This is set only by the approval UI.");
        return params;
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        String patchId = request.getArgument("patchId").trim();
        boolean approved = request.getBooleanArgument("approved", false);
        PermissionDecision decision = permissionPolicy.decide(name(), approved);
        if (decision != PermissionDecision.ALLOW) {
            auditLog.record("apply_patch", patchId, "", decision.name(), "User approval required");
            return ToolCallResult.failure("User approval is required before applying patch " + patchId);
        }

        PatchProposal proposal = pendingPatchStore.find(patchId).orElse(null);
        if (proposal == null) {
            auditLog.record("apply_patch", patchId, "", "missing", "Patch not found");
            return ToolCallResult.failure("Patch not found or already resolved: " + patchId);
        }

        PatchResult result = patchEngine.apply(proposal);
        if (result.isSuccess()) {
            pendingPatchStore.remove(patchId);
            auditLog.record("apply_patch", patchId, result.getRelativePath(), "applied", result.getMessage());
            return ToolCallResult.success(result.getMessage())
                    .withMetadata("patchId", result.getPatchId())
                    .withMetadata("relativePath", result.getRelativePath())
                    .withMetadata("backupPath", result.getBackupPath())
                    .withMetadata("status", "applied");
        }

        auditLog.record("apply_patch", patchId, result.getRelativePath(), "failed", result.getMessage());
        return ToolCallResult.failure(result.getMessage())
                .withMetadata("patchId", patchId)
                .withMetadata("status", "failed");
    }
}
