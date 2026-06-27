package com.mycompany.tutorhub_enterprise.server.services;

import java.time.Instant;

public class V2StudentSubmitUiWiringReadinessResult {
    private boolean success;
    private boolean ready;
    private String errorCode;
    private String wiringStatus;
    private String warnings;
    private String blockingReasons;
    private long checkedAt;

    public V2StudentSubmitUiWiringReadinessResult() {
        this.checkedAt = Instant.now().toEpochMilli();
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getWiringStatus() { return wiringStatus; }
    public void setWiringStatus(String wiringStatus) { this.wiringStatus = wiringStatus; }

    public String getWarnings() { return warnings; }
    public void setWarnings(String warnings) { this.warnings = warnings; }

    public String getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(String blockingReasons) { this.blockingReasons = blockingReasons; }

    public long getCheckedAt() { return checkedAt; }
    public void setCheckedAt(long checkedAt) { this.checkedAt = checkedAt; }
}
