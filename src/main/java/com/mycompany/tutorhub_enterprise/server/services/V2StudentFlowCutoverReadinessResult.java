package com.mycompany.tutorhub_enterprise.server.services;

import java.util.List;

public class V2StudentFlowCutoverReadinessResult {
    private boolean success;
    private boolean ready;
    private String errorCode;
    private String cutoverReadinessStatus;
    private List<String> warnings;
    private List<String> blockingReasons;
    private String checkedAt;

    public V2StudentFlowCutoverReadinessResult() {}

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

    public String getCutoverReadinessStatus() {
        return cutoverReadinessStatus;
    }

    public void setCutoverReadinessStatus(String cutoverReadinessStatus) {
        this.cutoverReadinessStatus = cutoverReadinessStatus;
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

    public String getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(String checkedAt) {
        this.checkedAt = checkedAt;
    }
}
