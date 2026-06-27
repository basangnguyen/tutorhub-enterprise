package com.mycompany.tutorhub_enterprise.server.services;

import java.util.ArrayList;
import java.util.List;

public class V2StudentSubmitRegressionGateResult {
    private boolean success;
    private boolean ready;
    private String errorCode;
    private boolean legacySubmitIntact;
    private boolean v2DefaultEnabled;
    private boolean adapterWiringReady;
    private boolean legacyFallbackReady;
    private boolean portableBuildRequired;
    private String regressionStatus;
    private List<String> warnings = new ArrayList<>();
    private List<String> blockingReasons = new ArrayList<>();
    private long checkedAt;

    public V2StudentSubmitRegressionGateResult() {
        this.checkedAt = System.currentTimeMillis();
    }

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

    public boolean isLegacySubmitIntact() {
        return legacySubmitIntact;
    }

    public void setLegacySubmitIntact(boolean legacySubmitIntact) {
        this.legacySubmitIntact = legacySubmitIntact;
    }

    public boolean isV2DefaultEnabled() {
        return v2DefaultEnabled;
    }

    public void setV2DefaultEnabled(boolean v2DefaultEnabled) {
        this.v2DefaultEnabled = v2DefaultEnabled;
    }

    public boolean isAdapterWiringReady() {
        return adapterWiringReady;
    }

    public void setAdapterWiringReady(boolean adapterWiringReady) {
        this.adapterWiringReady = adapterWiringReady;
    }

    public boolean isLegacyFallbackReady() {
        return legacyFallbackReady;
    }

    public void setLegacyFallbackReady(boolean legacyFallbackReady) {
        this.legacyFallbackReady = legacyFallbackReady;
    }

    public boolean isPortableBuildRequired() {
        return portableBuildRequired;
    }

    public void setPortableBuildRequired(boolean portableBuildRequired) {
        this.portableBuildRequired = portableBuildRequired;
    }

    public String getRegressionStatus() {
        return regressionStatus;
    }

    public void setRegressionStatus(String regressionStatus) {
        this.regressionStatus = regressionStatus;
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

    public long getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(long checkedAt) {
        this.checkedAt = checkedAt;
    }
}
