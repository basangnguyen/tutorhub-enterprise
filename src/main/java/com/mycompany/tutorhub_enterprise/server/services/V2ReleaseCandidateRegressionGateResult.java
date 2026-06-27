package com.mycompany.tutorhub_enterprise.server.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class V2ReleaseCandidateRegressionGateResult {
    private boolean success;
    private boolean ready;
    private String errorCode;
    private boolean productionFlagsSafe;
    private boolean legacySubmitIntact;
    private boolean networkServiceUnchanged;
    private boolean submitButtonUnchanged;
    private boolean e2eHarnessReady;
    private boolean fallbackGuardVerified;
    private boolean portableBuildPass;
    private String regressionStatus;
    private List<String> warnings = new ArrayList<>();
    private List<String> blockingReasons = new ArrayList<>();
    private Instant checkedAt = Instant.now();

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public boolean isProductionFlagsSafe() { return productionFlagsSafe; }
    public void setProductionFlagsSafe(boolean productionFlagsSafe) { this.productionFlagsSafe = productionFlagsSafe; }

    public boolean isLegacySubmitIntact() { return legacySubmitIntact; }
    public void setLegacySubmitIntact(boolean legacySubmitIntact) { this.legacySubmitIntact = legacySubmitIntact; }

    public boolean isNetworkServiceUnchanged() { return networkServiceUnchanged; }
    public void setNetworkServiceUnchanged(boolean networkServiceUnchanged) { this.networkServiceUnchanged = networkServiceUnchanged; }

    public boolean isSubmitButtonUnchanged() { return submitButtonUnchanged; }
    public void setSubmitButtonUnchanged(boolean submitButtonUnchanged) { this.submitButtonUnchanged = submitButtonUnchanged; }

    public boolean isE2eHarnessReady() { return e2eHarnessReady; }
    public void setE2eHarnessReady(boolean e2eHarnessReady) { this.e2eHarnessReady = e2eHarnessReady; }

    public boolean isFallbackGuardVerified() { return fallbackGuardVerified; }
    public void setFallbackGuardVerified(boolean fallbackGuardVerified) { this.fallbackGuardVerified = fallbackGuardVerified; }

    public boolean isPortableBuildPass() { return portableBuildPass; }
    public void setPortableBuildPass(boolean portableBuildPass) { this.portableBuildPass = portableBuildPass; }

    public String getRegressionStatus() { return regressionStatus; }
    public void setRegressionStatus(String regressionStatus) { this.regressionStatus = regressionStatus; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public List<String> getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(List<String> blockingReasons) { this.blockingReasons = blockingReasons; }

    public Instant getCheckedAt() { return checkedAt; }
    public void setCheckedAt(Instant checkedAt) { this.checkedAt = checkedAt; }

    public static class Builder {
        private final V2ReleaseCandidateRegressionGateResult result = new V2ReleaseCandidateRegressionGateResult();

        public Builder success(boolean success) { result.setSuccess(success); return this; }
        public Builder ready(boolean ready) { result.setReady(ready); return this; }
        public Builder errorCode(String errorCode) { result.setErrorCode(errorCode); return this; }
        public Builder productionFlagsSafe(boolean value) { result.setProductionFlagsSafe(value); return this; }
        public Builder legacySubmitIntact(boolean value) { result.setLegacySubmitIntact(value); return this; }
        public Builder networkServiceUnchanged(boolean value) { result.setNetworkServiceUnchanged(value); return this; }
        public Builder submitButtonUnchanged(boolean value) { result.setSubmitButtonUnchanged(value); return this; }
        public Builder e2eHarnessReady(boolean value) { result.setE2eHarnessReady(value); return this; }
        public Builder fallbackGuardVerified(boolean value) { result.setFallbackGuardVerified(value); return this; }
        public Builder portableBuildPass(boolean value) { result.setPortableBuildPass(value); return this; }
        public Builder regressionStatus(String status) { result.setRegressionStatus(status); return this; }
        public Builder addWarning(String warning) { result.getWarnings().add(warning); return this; }
        public Builder addBlockingReason(String reason) { result.getBlockingReasons().add(reason); return this; }

        public V2ReleaseCandidateRegressionGateResult build() { return result; }
    }
}
