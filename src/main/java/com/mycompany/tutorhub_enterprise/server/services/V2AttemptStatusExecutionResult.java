package com.mycompany.tutorhub_enterprise.server.services;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class V2AttemptStatusExecutionResult {
    private boolean success;
    private boolean ready;
    private boolean idempotent;
    private String errorCode;
    private long executionId;
    private long submitRecordId;
    private int userId;
    private int examId;
    private int paperId;
    private String attemptId;
    private String payloadHash;
    private String fromAttemptStatus;
    private String targetAttemptStatus;
    private String actualAttemptStatus;
    private String readinessStatus;
    private String transitionDraftStatus;
    private String executionStatus;
    private Timestamp executedAt;
    private List<String> warnings = new ArrayList<>();
    private List<String> blockingReasons = new ArrayList<>();

    public V2AttemptStatusExecutionResult() {
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public boolean isIdempotent() { return idempotent; }
    public void setIdempotent(boolean idempotent) { this.idempotent = idempotent; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public long getExecutionId() { return executionId; }
    public void setExecutionId(long executionId) { this.executionId = executionId; }

    public long getSubmitRecordId() { return submitRecordId; }
    public void setSubmitRecordId(long submitRecordId) { this.submitRecordId = submitRecordId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getExamId() { return examId; }
    public void setExamId(int examId) { this.examId = examId; }

    public int getPaperId() { return paperId; }
    public void setPaperId(int paperId) { this.paperId = paperId; }

    public String getAttemptId() { return attemptId; }
    public void setAttemptId(String attemptId) { this.attemptId = attemptId; }

    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }

    public String getFromAttemptStatus() { return fromAttemptStatus; }
    public void setFromAttemptStatus(String fromAttemptStatus) { this.fromAttemptStatus = fromAttemptStatus; }

    public String getTargetAttemptStatus() { return targetAttemptStatus; }
    public void setTargetAttemptStatus(String targetAttemptStatus) { this.targetAttemptStatus = targetAttemptStatus; }

    public String getActualAttemptStatus() { return actualAttemptStatus; }
    public void setActualAttemptStatus(String actualAttemptStatus) { this.actualAttemptStatus = actualAttemptStatus; }

    public String getReadinessStatus() { return readinessStatus; }
    public void setReadinessStatus(String readinessStatus) { this.readinessStatus = readinessStatus; }

    public String getTransitionDraftStatus() { return transitionDraftStatus; }
    public void setTransitionDraftStatus(String transitionDraftStatus) { this.transitionDraftStatus = transitionDraftStatus; }

    public String getExecutionStatus() { return executionStatus; }
    public void setExecutionStatus(String executionStatus) { this.executionStatus = executionStatus; }

    public Timestamp getExecutedAt() { return executedAt; }
    public void setExecutedAt(Timestamp executedAt) { this.executedAt = executedAt; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public void addWarning(String warning) { this.warnings.add(warning); }

    public List<String> getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(List<String> blockingReasons) { this.blockingReasons = blockingReasons; }
    public void addBlockingReason(String blockingReason) { this.blockingReasons.add(blockingReason); }
}
