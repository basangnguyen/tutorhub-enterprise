package com.mycompany.tutorhub_enterprise.models.exam;

import java.sql.Timestamp;

public class V2FinalAttemptStatusLedgerRecord {
    private long id;
    private long submitRecordId;
    private int userId;
    private int examId;
    private Integer paperId;
    private String attemptId;
    private String fromStatus;
    private String toStatus;
    private long publicationLedgerId;
    private String payloadHash;
    private String statusUpdateStatus;
    private Timestamp executedAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getSubmitRecordId() { return submitRecordId; }
    public void setSubmitRecordId(long submitRecordId) { this.submitRecordId = submitRecordId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getExamId() { return examId; }
    public void setExamId(int examId) { this.examId = examId; }

    public Integer getPaperId() { return paperId; }
    public void setPaperId(Integer paperId) { this.paperId = paperId; }

    public String getAttemptId() { return attemptId; }
    public void setAttemptId(String attemptId) { this.attemptId = attemptId; }

    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }

    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }

    public long getPublicationLedgerId() { return publicationLedgerId; }
    public void setPublicationLedgerId(long publicationLedgerId) { this.publicationLedgerId = publicationLedgerId; }

    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }

    public String getStatusUpdateStatus() { return statusUpdateStatus; }
    public void setStatusUpdateStatus(String statusUpdateStatus) { this.statusUpdateStatus = statusUpdateStatus; }

    public Timestamp getExecutedAt() { return executedAt; }
    public void setExecutedAt(Timestamp executedAt) { this.executedAt = executedAt; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
