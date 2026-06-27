package com.mycompany.tutorhub_enterprise.server.services;

import java.time.Instant;

public class V2StudentFlowCutoverMappingResult {
    private boolean success;
    private boolean ready;
    private String errorCode;
    private String currentSubmitAction;
    private boolean legacySubmitDetected;
    private boolean v2ManualSubmitDetected;
    private boolean defaultV2Enabled;
    private String mappingStatus;
    private String warnings;
    private String blockingReasons;
    private long checkedAt;

    public V2StudentFlowCutoverMappingResult() {
        this.checkedAt = Instant.now().toEpochMilli();
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getCurrentSubmitAction() { return currentSubmitAction; }
    public void setCurrentSubmitAction(String currentSubmitAction) { this.currentSubmitAction = currentSubmitAction; }

    public boolean isLegacySubmitDetected() { return legacySubmitDetected; }
    public void setLegacySubmitDetected(boolean legacySubmitDetected) { this.legacySubmitDetected = legacySubmitDetected; }

    public boolean isV2ManualSubmitDetected() { return v2ManualSubmitDetected; }
    public void setV2ManualSubmitDetected(boolean v2ManualSubmitDetected) { this.v2ManualSubmitDetected = v2ManualSubmitDetected; }

    public boolean isDefaultV2Enabled() { return defaultV2Enabled; }
    public void setDefaultV2Enabled(boolean defaultV2Enabled) { this.defaultV2Enabled = defaultV2Enabled; }

    public String getMappingStatus() { return mappingStatus; }
    public void setMappingStatus(String mappingStatus) { this.mappingStatus = mappingStatus; }

    public String getWarnings() { return warnings; }
    public void setWarnings(String warnings) { this.warnings = warnings; }

    public String getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(String blockingReasons) { this.blockingReasons = blockingReasons; }

    public long getCheckedAt() { return checkedAt; }
    public void setCheckedAt(long checkedAt) { this.checkedAt = checkedAt; }
}
