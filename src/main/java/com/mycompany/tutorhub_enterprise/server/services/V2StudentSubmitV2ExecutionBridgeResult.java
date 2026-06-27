package com.mycompany.tutorhub_enterprise.server.services;

import java.time.Instant;
import java.util.List;

public class V2StudentSubmitV2ExecutionBridgeResult {
    private boolean success;
    private boolean ready;
    private boolean executed;
    private boolean idempotent;
    private String errorCode;
    private int userId;
    private String examId;
    private String paperId;
    private String attemptId;
    private Long submitRecordId;
    private Long examResultId;
    private String finalStatus;
    private String bridgeStatus;
    private int executedStepCount;
    private List<String> warnings;
    private List<String> blockingReasons;
    private Instant executedAt;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public boolean isExecuted() { return executed; }
    public void setExecuted(boolean executed) { this.executed = executed; }

    public boolean isIdempotent() { return idempotent; }
    public void setIdempotent(boolean idempotent) { this.idempotent = idempotent; }

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

    public Long getSubmitRecordId() { return submitRecordId; }
    public void setSubmitRecordId(Long submitRecordId) { this.submitRecordId = submitRecordId; }

    public Long getExamResultId() { return examResultId; }
    public void setExamResultId(Long examResultId) { this.examResultId = examResultId; }

    public String getFinalStatus() { return finalStatus; }
    public void setFinalStatus(String finalStatus) { this.finalStatus = finalStatus; }

    public String getBridgeStatus() { return bridgeStatus; }
    public void setBridgeStatus(String bridgeStatus) { this.bridgeStatus = bridgeStatus; }

    public int getExecutedStepCount() { return executedStepCount; }
    public void setExecutedStepCount(int executedStepCount) { this.executedStepCount = executedStepCount; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public List<String> getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(List<String> blockingReasons) { this.blockingReasons = blockingReasons; }

    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant executedAt) { this.executedAt = executedAt; }
}
