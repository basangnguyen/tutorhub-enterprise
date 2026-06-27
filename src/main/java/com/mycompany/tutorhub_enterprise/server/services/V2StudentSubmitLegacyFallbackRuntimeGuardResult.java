package com.mycompany.tutorhub_enterprise.server.services;

import java.time.Instant;
import java.util.List;

public class V2StudentSubmitLegacyFallbackRuntimeGuardResult {
    private boolean success;
    private boolean ready;
    private boolean fallbackAllowed;
    private boolean fallbackForbidden;
    private String fallbackReason;
    private String failureZone;
    private String targetRoute;
    private List<String> warnings;
    private List<String> blockingReasons;
    private Instant checkedAt;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public boolean isFallbackAllowed() { return fallbackAllowed; }
    public void setFallbackAllowed(boolean fallbackAllowed) { this.fallbackAllowed = fallbackAllowed; }

    public boolean isFallbackForbidden() { return fallbackForbidden; }
    public void setFallbackForbidden(boolean fallbackForbidden) { this.fallbackForbidden = fallbackForbidden; }

    public String getFallbackReason() { return fallbackReason; }
    public void setFallbackReason(String fallbackReason) { this.fallbackReason = fallbackReason; }

    public String getFailureZone() { return failureZone; }
    public void setFailureZone(String failureZone) { this.failureZone = failureZone; }

    public String getTargetRoute() { return targetRoute; }
    public void setTargetRoute(String targetRoute) { this.targetRoute = targetRoute; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public List<String> getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(List<String> blockingReasons) { this.blockingReasons = blockingReasons; }

    public Instant getCheckedAt() { return checkedAt; }
    public void setCheckedAt(Instant checkedAt) { this.checkedAt = checkedAt; }
}
