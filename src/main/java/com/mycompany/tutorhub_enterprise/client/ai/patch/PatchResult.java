package com.mycompany.tutorhub_enterprise.client.ai.patch;

public final class PatchResult {

    private final boolean success;
    private final String patchId;
    private final String relativePath;
    private final String message;
    private final String diff;
    private final String backupPath;

    private PatchResult(boolean success, String patchId, String relativePath,
                        String message, String diff, String backupPath) {
        this.success = success;
        this.patchId = patchId == null ? "" : patchId;
        this.relativePath = relativePath == null ? "" : relativePath;
        this.message = message == null ? "" : message;
        this.diff = diff == null ? "" : diff;
        this.backupPath = backupPath == null ? "" : backupPath;
    }

    public static PatchResult success(PatchProposal proposal, String message, String backupPath) {
        return new PatchResult(true, proposal.getId(), proposal.getRelativePath(),
                message, proposal.getDiff(), backupPath);
    }

    public static PatchResult failure(String patchId, String relativePath, String message) {
        return new PatchResult(false, patchId, relativePath, message, "", "");
    }

    public boolean isSuccess() {
        return success;
    }

    public String getPatchId() {
        return patchId;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getMessage() {
        return message;
    }

    public String getDiff() {
        return diff;
    }

    public String getBackupPath() {
        return backupPath;
    }
}
