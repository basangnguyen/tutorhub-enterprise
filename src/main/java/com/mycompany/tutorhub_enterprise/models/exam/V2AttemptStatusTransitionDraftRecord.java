package com.mycompany.tutorhub_enterprise.models.exam;

import java.sql.Timestamp;

public class V2AttemptStatusTransitionDraftRecord {
    private long id;
    private long submitRecordId;
    private int userId;
    private int examId;
    private int paperId;
    private String attemptId;
    private long transitionDraftId;
    private String payloadHash;
    private String preflightStatus;
    private String realSubmitTransitionDraftStatus;
    private String attemptStatusGateStatus;
    private String attemptStatusTransitionDraftStatus;
    private String fromAttemptStatus;
    private String targetAttemptStatus;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public V2AttemptStatusTransitionDraftRecord() {
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

    public long getTransitionDraftId() { return transitionDraftId; }
    public void setTransitionDraftId(long transitionDraftId) { this.transitionDraftId = transitionDraftId; }

    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }

    public String getPreflightStatus() { return preflightStatus; }
    public void setPreflightStatus(String preflightStatus) { this.preflightStatus = preflightStatus; }

    public String getRealSubmitTransitionDraftStatus() { return realSubmitTransitionDraftStatus; }
    public void setRealSubmitTransitionDraftStatus(String realSubmitTransitionDraftStatus) { this.realSubmitTransitionDraftStatus = realSubmitTransitionDraftStatus; }

    public String getAttemptStatusGateStatus() { return attemptStatusGateStatus; }
    public void setAttemptStatusGateStatus(String attemptStatusGateStatus) { this.attemptStatusGateStatus = attemptStatusGateStatus; }

    public String getAttemptStatusTransitionDraftStatus() { return attemptStatusTransitionDraftStatus; }
    public void setAttemptStatusTransitionDraftStatus(String attemptStatusTransitionDraftStatus) { this.attemptStatusTransitionDraftStatus = attemptStatusTransitionDraftStatus; }

    public String getFromAttemptStatus() { return fromAttemptStatus; }
    public void setFromAttemptStatus(String fromAttemptStatus) { this.fromAttemptStatus = fromAttemptStatus; }

    public String getTargetAttemptStatus() { return targetAttemptStatus; }
    public void setTargetAttemptStatus(String targetAttemptStatus) { this.targetAttemptStatus = targetAttemptStatus; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
