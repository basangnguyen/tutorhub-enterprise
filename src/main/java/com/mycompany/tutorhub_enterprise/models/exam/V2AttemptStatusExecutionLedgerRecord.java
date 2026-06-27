package com.mycompany.tutorhub_enterprise.models.exam;

import java.sql.Timestamp;

public class V2AttemptStatusExecutionLedgerRecord {
    private long id;
    private long submitRecordId;
    private int userId;
    private int examId;
    private int paperId;
    private String attemptId;
    private long attemptStatusTransitionDraftId;
    private String payloadHash;
    private String fromAttemptStatus;
    private String targetAttemptStatus;
    private String actualAttemptStatus;
    private String executionStatus;
    private Timestamp executedAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public V2AttemptStatusExecutionLedgerRecord() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

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

    public long getAttemptStatusTransitionDraftId() { return attemptStatusTransitionDraftId; }
    public void setAttemptStatusTransitionDraftId(long attemptStatusTransitionDraftId) { this.attemptStatusTransitionDraftId = attemptStatusTransitionDraftId; }

    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }

    public String getFromAttemptStatus() { return fromAttemptStatus; }
    public void setFromAttemptStatus(String fromAttemptStatus) { this.fromAttemptStatus = fromAttemptStatus; }

    public String getTargetAttemptStatus() { return targetAttemptStatus; }
    public void setTargetAttemptStatus(String targetAttemptStatus) { this.targetAttemptStatus = targetAttemptStatus; }

    public String getActualAttemptStatus() { return actualAttemptStatus; }
    public void setActualAttemptStatus(String actualAttemptStatus) { this.actualAttemptStatus = actualAttemptStatus; }

    public String getExecutionStatus() { return executionStatus; }
    public void setExecutionStatus(String executionStatus) { this.executionStatus = executionStatus; }

    public Timestamp getExecutedAt() { return executedAt; }
    public void setExecutedAt(Timestamp executedAt) { this.executedAt = executedAt; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
