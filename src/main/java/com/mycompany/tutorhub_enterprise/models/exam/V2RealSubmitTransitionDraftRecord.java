package com.mycompany.tutorhub_enterprise.models.exam;

import java.sql.Timestamp;

public class V2RealSubmitTransitionDraftRecord {
    private long id;
    private long submitRecordId;
    private int userId;
    private int examId;
    private int paperId;
    private String attemptId;
    private long ledgerId;
    private long closureDraftId;
    private String payloadHash;
    private String preflightStatus;
    private String transitionDraftStatus;
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

    public int getPaperId() { return paperId; }
    public void setPaperId(int paperId) { this.paperId = paperId; }

    public String getAttemptId() { return attemptId; }
    public void setAttemptId(String attemptId) { this.attemptId = attemptId; }

    public long getLedgerId() { return ledgerId; }
    public void setLedgerId(long ledgerId) { this.ledgerId = ledgerId; }

    public long getClosureDraftId() { return closureDraftId; }
    public void setClosureDraftId(long closureDraftId) { this.closureDraftId = closureDraftId; }

    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }

    public String getPreflightStatus() { return preflightStatus; }
    public void setPreflightStatus(String preflightStatus) { this.preflightStatus = preflightStatus; }

    public String getTransitionDraftStatus() { return transitionDraftStatus; }
    public void setTransitionDraftStatus(String transitionDraftStatus) { this.transitionDraftStatus = transitionDraftStatus; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
