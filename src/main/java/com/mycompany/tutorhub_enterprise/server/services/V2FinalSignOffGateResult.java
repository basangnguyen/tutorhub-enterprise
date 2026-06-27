package com.mycompany.tutorhub_enterprise.server.services;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public class V2FinalSignOffGateResult implements Serializable {
    private boolean success;
    private boolean ready;
    private String errorCode;
    
    private boolean mavenTestsPass;
    private boolean portableBuildPass;
    private boolean releaseManifestExists;
    private boolean vmSmokePacketExists;
    private boolean vmSmokeExecuted;
    private String vmSmokeStatus;
    
    private boolean finalReleaseChecklistReady;
    private boolean productionFlagsSafe;
    private boolean legacySubmitIntact;
    private boolean debugCleanupPolicyExists;
    
    private String finalSignOffStatus;
    private List<String> warnings;
    private List<String> blockingReasons;
    private Instant checkedAt;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this. ready = ready; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public boolean isMavenTestsPass() { return mavenTestsPass; }
    public void setMavenTestsPass(boolean mavenTestsPass) { this.mavenTestsPass = mavenTestsPass; }

    public boolean isPortableBuildPass() { return portableBuildPass; }
    public void setPortableBuildPass(boolean portableBuildPass) { this.portableBuildPass = portableBuildPass; }

    public boolean isReleaseManifestExists() { return releaseManifestExists; }
    public void setReleaseManifestExists(boolean releaseManifestExists) { this.releaseManifestExists = releaseManifestExists; }

    public boolean isVmSmokePacketExists() { return vmSmokePacketExists; }
    public void setVmSmokePacketExists(boolean vmSmokePacketExists) { this.vmSmokePacketExists = vmSmokePacketExists; }

    public boolean isVmSmokeExecuted() { return vmSmokeExecuted; }
    public void setVmSmokeExecuted(boolean vmSmokeExecuted) { this.vmSmokeExecuted = vmSmokeExecuted; }

    public String getVmSmokeStatus() { return vmSmokeStatus; }
    public void setVmSmokeStatus(String vmSmokeStatus) { this.vmSmokeStatus = vmSmokeStatus; }

    public boolean isFinalReleaseChecklistReady() { return finalReleaseChecklistReady; }
    public void setFinalReleaseChecklistReady(boolean finalReleaseChecklistReady) { this.finalReleaseChecklistReady = finalReleaseChecklistReady; }

    public boolean isProductionFlagsSafe() { return productionFlagsSafe; }
    public void setProductionFlagsSafe(boolean productionFlagsSafe) { this.productionFlagsSafe = productionFlagsSafe; }

    public boolean isLegacySubmitIntact() { return legacySubmitIntact; }
    public void setLegacySubmitIntact(boolean legacySubmitIntact) { this.legacySubmitIntact = legacySubmitIntact; }

    public boolean isDebugCleanupPolicyExists() { return debugCleanupPolicyExists; }
    public void setDebugCleanupPolicyExists(boolean debugCleanupPolicyExists) { this.debugCleanupPolicyExists = debugCleanupPolicyExists; }

    public String getFinalSignOffStatus() { return finalSignOffStatus; }
    public void setFinalSignOffStatus(String finalSignOffStatus) { this.finalSignOffStatus = finalSignOffStatus; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public List<String> getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(List<String> blockingReasons) { this.blockingReasons = blockingReasons; }

    public Instant getCheckedAt() { return checkedAt; }
    public void setCheckedAt(Instant checkedAt) { this.checkedAt = checkedAt; }
}
