package com.mycompany.tutorhub_enterprise.models.exam;

import java.sql.Timestamp;

public class V2ManualCandidateExecutionLedgerRecord {
    private long id;
    private String attemptId;
    private long submitRecordId;
    private int userId;
    private int examId;
    private int paperId;
    private long publicationLedgerId;
    private long finalStatusLedgerId;
    private String executionStatus;
    private String executionMode;
    private String errorCode;
    private String blockingReasonSummary;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getAttemptId() { return attemptId; }
    public void setAttemptId(String attemptId) { this.attemptId = attemptId; }

    public long getSubmitRecordId() { return submitRecordId; }
    public void setSubmitRecordId(long submitRecordId) { this.submitRecordId = submitRecordId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getExamId() { return examId; }
    public void setExamId(int examId) { this.examId = examId; }

    public int getPaperId() { return paperId; }
    public void setPaperId(int paperId) { this.paperId = paperId; }

    public long getPublicationLedgerId() { return publicationLedgerId; }
    public void setPublicationLedgerId(long publicationLedgerId) { this.publicationLedgerId = publicationLedgerId; }

    public long getFinalStatusLedgerId() { return finalStatusLedgerId; }
    public void setFinalStatusLedgerId(long finalStatusLedgerId) { this.finalStatusLedgerId = finalStatusLedgerId; }

    public String getExecutionStatus() { return executionStatus; }
    public void setExecutionStatus(String executionStatus) { this.executionStatus = executionStatus; }

    public String getExecutionMode() { return executionMode; }
    public void setExecutionMode(String executionMode) { this.executionMode = executionMode; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getBlockingReasonSummary() { return blockingReasonSummary; }
    public void setBlockingReasonSummary(String blockingReasonSummary) { this.blockingReasonSummary = blockingReasonSummary; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
