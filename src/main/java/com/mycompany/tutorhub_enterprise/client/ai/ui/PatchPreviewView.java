package com.mycompany.tutorhub_enterprise.client.ai.ui;

import com.google.gson.JsonObject;
import com.mycompany.tutorhub_enterprise.client.ai.patch.PatchProposal;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;

public final class PatchPreviewView {

    private PatchPreviewView() {
    }

    public static JsonObject proposal(PatchProposal proposal) {
        JsonObject json = new JsonObject();
        if (proposal == null) {
            json.addProperty("patchId", "");
            json.addProperty("status", "missing");
            json.addProperty("message", "Patch proposal is missing");
            return json;
        }
        json.addProperty("patchId", proposal.getId());
        json.addProperty("relativePath", proposal.getRelativePath());
        json.addProperty("reason", proposal.getReason());
        json.addProperty("diff", proposal.getDiff());
        json.addProperty("status", "pending");
        json.addProperty("message", "Waiting for user approval");
        json.addProperty("createdAt", proposal.getCreatedAt().toString());
        return json;
    }

    public static JsonObject applyResult(String patchId, ToolCallResult result) {
        JsonObject json = new JsonObject();
        json.addProperty("patchId", patchId == null ? "" : patchId);
        boolean success = result != null && result.isSuccess();
        json.addProperty("status", success ? "applied" : "failed");
        json.addProperty("message", result == null
                ? "Patch result is missing"
                : success ? result.getOutput() : result.getError());
        if (result != null) {
            json.addProperty("relativePath", result.getMetadata().getOrDefault("relativePath", ""));
            json.addProperty("backupPath", result.getMetadata().getOrDefault("backupPath", ""));
        }
        return json;
    }

    public static JsonObject rejected(PatchProposal proposal) {
        JsonObject json = proposal(proposal);
        json.addProperty("status", "rejected");
        json.addProperty("message", "Patch rejected by user");
        return json;
    }
}
