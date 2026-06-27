package com.mycompany.tutorhub_enterprise.server.services;

import java.time.Instant;

public class V2StudentSubmitAdapterDryRunResult {
    private boolean success;
    private boolean ready;
    private String errorCode;
    private int userId;
    private int examId;
    private int paperId;
    private String attemptId;
    private String plannedRoute;
    private String adapterStatus;
    private String warnings;
    private String blockingReasons;
    private long checkedAt;

    public V2StudentSubmitAdapterDryRunResult() {
        this.checkedAt = Instant.now().toEpochMilli();
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getExamId() { return examId; }
    public void setExamId(int examId) { this.examId = examId; }

    public int getPaperId() { return paperId; }
    public void setPaperId(int paperId) { this.paperId = paperId; }

    public String getAttemptId() { return attemptId; }
    public void setAttemptId(String attemptId) { this.attemptId = attemptId; }

    public String getPlannedRoute() { return plannedRoute; }
    public void setPlannedRoute(String plannedRoute) { this.plannedRoute = plannedRoute; }

    public String getAdapterStatus() { return adapterStatus; }
    public void setAdapterStatus(String adapterStatus) { this.adapterStatus = adapterStatus; }

    public String getWarnings() { return warnings; }
    public void setWarnings(String warnings) { this.warnings = warnings; }

    public String getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(String blockingReasons) { this.blockingReasons = blockingReasons; }

    public long getCheckedAt() { return checkedAt; }
    public void setCheckedAt(long checkedAt) { this.checkedAt = checkedAt; }
}
