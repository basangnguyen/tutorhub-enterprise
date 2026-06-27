package com.mycompany.tutorhub_enterprise.server.services;

import java.time.Instant;
import java.util.List;

public class V2DemoPackageFreezeGateResult {
    private boolean success;
    private boolean ready;
    private String decision;
    private String errorCode;
    private boolean trialManifestExists;
    private boolean trialChecklistExists;
    private boolean releaseNotesExists;
    private boolean internalDemoPacketExists;
    private boolean studentSubmitTrialEvidenceExists;
    private boolean regressionSecurityRecheckPass;
    private boolean demoHandoffGateReady;
    private String desktopDemoStatus;
    private boolean productionFlagsSafe;
    private boolean repoStatusDirty;
    private List<String> warnings;
    private List<String> blockingReasons;
    private String checkedAt;

    public V2DemoPackageFreezeGateResult() {
        this.checkedAt = Instant.now().toString();
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public boolean isTrialManifestExists() { return trialManifestExists; }
    public void setTrialManifestExists(boolean trialManifestExists) { this.trialManifestExists = trialManifestExists; }
    public boolean isTrialChecklistExists() { return trialChecklistExists; }
    public void setTrialChecklistExists(boolean trialChecklistExists) { this.trialChecklistExists = trialChecklistExists; }
    public boolean isReleaseNotesExists() { return releaseNotesExists; }
    public void setReleaseNotesExists(boolean releaseNotesExists) { this.releaseNotesExists = releaseNotesExists; }
    public boolean isInternalDemoPacketExists() { return internalDemoPacketExists; }
    public void setInternalDemoPacketExists(boolean internalDemoPacketExists) { this.internalDemoPacketExists = internalDemoPacketExists; }
    public boolean isStudentSubmitTrialEvidenceExists() { return studentSubmitTrialEvidenceExists; }
    public void setStudentSubmitTrialEvidenceExists(boolean studentSubmitTrialEvidenceExists) { this.studentSubmitTrialEvidenceExists = studentSubmitTrialEvidenceExists; }
    public boolean isRegressionSecurityRecheckPass() { return regressionSecurityRecheckPass; }
    public void setRegressionSecurityRecheckPass(boolean regressionSecurityRecheckPass) { this.regressionSecurityRecheckPass = regressionSecurityRecheckPass; }
    public boolean isDemoHandoffGateReady() { return demoHandoffGateReady; }
    public void setDemoHandoffGateReady(boolean demoHandoffGateReady) { this.demoHandoffGateReady = demoHandoffGateReady; }
    public String getDesktopDemoStatus() { return desktopDemoStatus; }
    public void setDesktopDemoStatus(String desktopDemoStatus) { this.desktopDemoStatus = desktopDemoStatus; }
    public boolean isProductionFlagsSafe() { return productionFlagsSafe; }
    public void setProductionFlagsSafe(boolean productionFlagsSafe) { this.productionFlagsSafe = productionFlagsSafe; }
    public boolean isRepoStatusDirty() { return repoStatusDirty; }
    public void setRepoStatusDirty(boolean repoStatusDirty) { this.repoStatusDirty = repoStatusDirty; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public List<String> getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(List<String> blockingReasons) { this.blockingReasons = blockingReasons; }
    public String getCheckedAt() { return checkedAt; }
    public void setCheckedAt(String checkedAt) { this.checkedAt = checkedAt; }
}
