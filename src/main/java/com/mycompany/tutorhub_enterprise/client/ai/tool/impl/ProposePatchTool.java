package com.mycompany.tutorhub_enterprise.client.ai.tool.impl;

import com.mycompany.tutorhub_enterprise.client.ai.patch.PatchEngine;
import com.mycompany.tutorhub_enterprise.client.ai.patch.PatchProposal;
import com.mycompany.tutorhub_enterprise.client.ai.patch.PendingPatchStore;
import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;
import com.mycompany.tutorhub_enterprise.client.ai.tool.AgentTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ProposePatchTool implements AgentTool {

    private final PatchEngine patchEngine;
    private final PendingPatchStore pendingPatchStore;

    public ProposePatchTool(WorkspaceBoundary boundary, PendingPatchStore pendingPatchStore) {
        this.patchEngine = new PatchEngine(boundary);
        this.pendingPatchStore = pendingPatchStore;
    }

    @Override
    public String name() {
        return "propose_patch";
    }

    @Override
    public String description() {
        return "Create a pending diff proposal for one existing UTF-8 file. Does not write files. User approval is required before apply.";
    }

    @Override
    public Map<String, String> parameters() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("path", "Required relative path to an existing UTF-8 file inside the workspace.");
        params.put("mode", "Optional: str_replace or rewrite. Defaults to str_replace.");
        params.put("oldText", "Required for str_replace. Exact old text block to replace; must match once.");
        params.put("newText", "Required for str_replace. Replacement text.");
        params.put("content", "Required for rewrite. Full new file content. Only allowed for files <= 400 lines.");
        params.put("reason", "Short reason shown to the user in the approval UI.");
        return params;
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        try {
            String mode = request.getArgument("mode", "str_replace").trim();
            String reason = request.getArgument("reason", "AI proposed code change");
            PatchProposal proposal;
            if ("rewrite".equalsIgnoreCase(mode)) {
                proposal = patchEngine.proposeRewrite(
                        request.getArgument("path"),
                        request.getArgument("content", ""),
                        reason);
            } else {
                proposal = patchEngine.proposeStrReplace(
                        request.getArgument("path"),
                        request.getArgument("oldText", ""),
                        request.getArgument("newText", ""),
                        reason);
            }
            pendingPatchStore.add(proposal);
            String output = "Patch proposal created and waiting for user approval.\n"
                    + "Patch ID: " + proposal.getId() + "\n"
                    + "File: " + proposal.getRelativePath() + "\n"
                    + "Reason: " + proposal.getReason() + "\n\n"
                    + "```diff\n" + proposal.getDiff() + "```";
            return ToolCallResult.success(output)
                    .withMetadata("patchId", proposal.getId())
                    .withMetadata("relativePath", proposal.getRelativePath())
                    .withMetadata("reason", proposal.getReason())
                    .withMetadata("approvalRequired", "true")
                    .withMetadata("diff", proposal.getDiff());
        } catch (Exception ex) {
            return ToolCallResult.failure(ex.getMessage());
        }
    }
}
