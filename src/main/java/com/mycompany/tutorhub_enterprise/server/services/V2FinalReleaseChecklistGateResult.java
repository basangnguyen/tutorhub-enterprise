package com.mycompany.tutorhub_enterprise.server.services;

import java.time.Instant;
import java.util.List;

public class V2FinalReleaseChecklistGateResult {
    private boolean success;
    private boolean ready;
    private String errorCode;
    private boolean productionFlagsSafe;
    private boolean legacySubmitIntact;
    private boolean runtimeAdapterReady;
    private boolean executionBridgeReady;
    private boolean fallbackGuardReady;
    private boolean releaseCandidateReady;
    private boolean vmSmokePlanExists;
    private boolean debugScriptsAudited;
    private String finalReleaseStatus;
    private List<String> warnings;
    private List<String> blockingReasons;
    private Instant checkedAt;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public boolean isProductionFlagsSafe() {
        return productionFlagsSafe;
    }

    public void setProductionFlagsSafe(boolean productionFlagsSafe) {
        this.productionFlagsSafe = productionFlagsSafe;
    }

    public boolean isLegacySubmitIntact() {
        return legacySubmitIntact;
    }

    public void setLegacySubmitIntact(boolean legacySubmitIntact) {
        this.legacySubmitIntact = legacySubmitIntact;
    }

    public boolean isRuntimeAdapterReady() {
        return runtimeAdapterReady;
    }

    public void setRuntimeAdapterReady(boolean runtimeAdapterReady) {
        this.runtimeAdapterReady = runtimeAdapterReady;
    }

    public boolean isExecutionBridgeReady() {
        return executionBridgeReady;
    }

    public void setExecutionBridgeReady(boolean executionBridgeReady) {
        this.executionBridgeReady = executionBridgeReady;
    }

    public boolean isFallbackGuardReady() {
        return fallbackGuardReady;
    }

    public void setFallbackGuardReady(boolean fallbackGuardReady) {
        this.fallbackGuardReady = fallbackGuardReady;
    }

    public boolean isReleaseCandidateReady() {
        return releaseCandidateReady;
    }

    public void setReleaseCandidateReady(boolean releaseCandidateReady) {
        this.releaseCandidateReady = releaseCandidateReady;
    }

    public boolean isVmSmokePlanExists() {
        return vmSmokePlanExists;
    }

    public void setVmSmokePlanExists(boolean vmSmokePlanExists) {
        this.vmSmokePlanExists = vmSmokePlanExists;
    }

    public boolean isDebugScriptsAudited() {
        return debugScriptsAudited;
    }

    public void setDebugScriptsAudited(boolean debugScriptsAudited) {
        this.debugScriptsAudited = debugScriptsAudited;
    }

    public String getFinalReleaseStatus() {
        return finalReleaseStatus;
    }

    public void setFinalReleaseStatus(String finalReleaseStatus) {
        this.finalReleaseStatus = finalReleaseStatus;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<String> getBlockingReasons() {
        return blockingReasons;
    }

    public void setBlockingReasons(List<String> blockingReasons) {
        this.blockingReasons = blockingReasons;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }
}
