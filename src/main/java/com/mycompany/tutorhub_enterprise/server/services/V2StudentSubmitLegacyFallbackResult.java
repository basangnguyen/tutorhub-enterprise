package com.mycompany.tutorhub_enterprise.server.services;

import java.util.ArrayList;
import java.util.List;

public class V2StudentSubmitLegacyFallbackResult {
    private boolean success;
    private boolean ready;
    private String errorCode;
    private boolean fallbackAvailable;
    private String fallbackTarget;
    private boolean wouldUseFallback;
    private String fallbackReason;
    private List<String> warnings = new ArrayList<>();
    private List<String> blockingReasons = new ArrayList<>();
    private long checkedAt;

    public V2StudentSubmitLegacyFallbackResult() {
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

    public boolean isFallbackAvailable() {
        return fallbackAvailable;
    }

    public void setFallbackAvailable(boolean fallbackAvailable) {
        this.fallbackAvailable = fallbackAvailable;
    }

    public String getFallbackTarget() {
        return fallbackTarget;
    }

    public void setFallbackTarget(String fallbackTarget) {
        this.fallbackTarget = fallbackTarget;
    }

    public boolean isWouldUseFallback() {
        return wouldUseFallback;
    }

    public void setWouldUseFallback(boolean wouldUseFallback) {
        this.wouldUseFallback = wouldUseFallback;
    }

    public String getFallbackReason() {
        return fallbackReason;
    }

    public void setFallbackReason(String fallbackReason) {
        this.fallbackReason = fallbackReason;
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
