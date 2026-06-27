package com.mycompany.tutorhub_enterprise.server.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class V2StudentSubmitE2EHarnessResult {
    private boolean success;
    private boolean ready;
    private String errorCode;
    private String harnessStatus;
    private boolean v2DefaultEnabledInTestOnly;
    private boolean runtimeAdapterReady;
    private boolean executionBridgeReady;
    private boolean fallbackGuardReady;
    private boolean payloadContractValid;
    private List<String> warnings = new ArrayList<>();
    private List<String> blockingReasons = new ArrayList<>();
    private Instant checkedAt = Instant.now();

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getHarnessStatus() { return harnessStatus; }
    public void setHarnessStatus(String harnessStatus) { this.harnessStatus = harnessStatus; }

    public boolean isV2DefaultEnabledInTestOnly() { return v2DefaultEnabledInTestOnly; }
    public void setV2DefaultEnabledInTestOnly(boolean v2DefaultEnabledInTestOnly) { this.v2DefaultEnabledInTestOnly = v2DefaultEnabledInTestOnly; }

    public boolean isRuntimeAdapterReady() { return runtimeAdapterReady; }
    public void setRuntimeAdapterReady(boolean runtimeAdapterReady) { this.runtimeAdapterReady = runtimeAdapterReady; }

    public boolean isExecutionBridgeReady() { return executionBridgeReady; }
    public void setExecutionBridgeReady(boolean executionBridgeReady) { this.executionBridgeReady = executionBridgeReady; }

    public boolean isFallbackGuardReady() { return fallbackGuardReady; }
    public void setFallbackGuardReady(boolean fallbackGuardReady) { this.fallbackGuardReady = fallbackGuardReady; }

    public boolean isPayloadContractValid() { return payloadContractValid; }
    public void setPayloadContractValid(boolean payloadContractValid) { this.payloadContractValid = payloadContractValid; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public List<String> getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(List<String> blockingReasons) { this.blockingReasons = blockingReasons; }

    public Instant getCheckedAt() { return checkedAt; }
    public void setCheckedAt(Instant checkedAt) { this.checkedAt = checkedAt; }

    public static class Builder {
        private final V2StudentSubmitE2EHarnessResult result = new V2StudentSubmitE2EHarnessResult();

        public Builder success(boolean success) { result.setSuccess(success); return this; }
        public Builder ready(boolean ready) { result.setReady(ready); return this; }
        public Builder errorCode(String errorCode) { result.setErrorCode(errorCode); return this; }
        public Builder harnessStatus(String harnessStatus) { result.setHarnessStatus(harnessStatus); return this; }
        public Builder v2DefaultEnabledInTestOnly(boolean value) { result.setV2DefaultEnabledInTestOnly(value); return this; }
        public Builder runtimeAdapterReady(boolean value) { result.setRuntimeAdapterReady(value); return this; }
        public Builder executionBridgeReady(boolean value) { result.setExecutionBridgeReady(value); return this; }
        public Builder fallbackGuardReady(boolean value) { result.setFallbackGuardReady(value); return this; }
        public Builder payloadContractValid(boolean value) { result.setPayloadContractValid(value); return this; }
        public Builder addWarning(String warning) { result.getWarnings().add(warning); return this; }
        public Builder addBlockingReason(String reason) { result.getBlockingReasons().add(reason); return this; }

        public V2StudentSubmitE2EHarnessResult build() { return result; }
    }
}
