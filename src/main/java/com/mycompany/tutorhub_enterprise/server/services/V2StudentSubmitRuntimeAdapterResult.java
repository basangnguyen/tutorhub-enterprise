package com.mycompany.tutorhub_enterprise.server.services;

import java.time.Instant;
import java.util.List;

public class V2StudentSubmitRuntimeAdapterResult {
    private boolean success;
    private boolean ready;
    private String errorCode;
    private int userId;
    private String examId;
    private String paperId;
    private String attemptId;
    private String runtimeRoute;
    private boolean v2DefaultEnabled;
    private String runtimeAdapterStatus;
    private List<String> warnings;
    private List<String> blockingReasons;
    private Instant checkedAt;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getExamId() { return examId; }
    public void setExamId(String examId) { this.examId = examId; }

    public String getPaperId() { return paperId; }
    public void setPaperId(String paperId) { this.paperId = paperId; }

    public String getAttemptId() { return attemptId; }
    public void setAttemptId(String attemptId) { this.attemptId = attemptId; }

    public String getRuntimeRoute() { return runtimeRoute; }
    public void setRuntimeRoute(String runtimeRoute) { this.runtimeRoute = runtimeRoute; }

    public boolean isV2DefaultEnabled() { return v2DefaultEnabled; }
    public void setV2DefaultEnabled(boolean v2DefaultEnabled) { this.v2DefaultEnabled = v2DefaultEnabled; }

    public String getRuntimeAdapterStatus() { return runtimeAdapterStatus; }
    public void setRuntimeAdapterStatus(String runtimeAdapterStatus) { this.runtimeAdapterStatus = runtimeAdapterStatus; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public List<String> getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(List<String> blockingReasons) { this.blockingReasons = blockingReasons; }

    public Instant getCheckedAt() { return checkedAt; }
    public void setCheckedAt(Instant checkedAt) { this.checkedAt = checkedAt; }
}
