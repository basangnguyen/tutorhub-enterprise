package com.mycompany.tutorhub_enterprise.models.exam;

import java.sql.Timestamp;

public class V2AttemptFinalizationLedgerRecord {
    private long id;
    private long submitRecordId;
    private int userId;
    private int examId;
    private int paperId;
    private String attemptId;
    private String payloadHash;
    private String previousSubmitStatus;
    private String finalizationStatus;
    private String finalizationMode;
    private String source;
    private Timestamp createdAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSubmitRecordId() {
        return submitRecordId;
    }

    public void setSubmitRecordId(long submitRecordId) {
        this.submitRecordId = submitRecordId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getExamId() {
        return examId;
    }

    public void setExamId(int examId) {
        this.examId = examId;
    }

    public int getPaperId() {
        return paperId;
    }

    public void setPaperId(int paperId) {
        this.paperId = paperId;
    }

    public String getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(String attemptId) {
        this.attemptId = attemptId;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public String getPreviousSubmitStatus() {
        return previousSubmitStatus;
    }

    public void setPreviousSubmitStatus(String previousSubmitStatus) {
        this.previousSubmitStatus = previousSubmitStatus;
    }

    public String getFinalizationStatus() {
        return finalizationStatus;
    }

    public void setFinalizationStatus(String finalizationStatus) {
        this.finalizationStatus = finalizationStatus;
    }

    public String getFinalizationMode() {
        return finalizationMode;
    }

    public void setFinalizationMode(String finalizationMode) {
        this.finalizationMode = finalizationMode;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
