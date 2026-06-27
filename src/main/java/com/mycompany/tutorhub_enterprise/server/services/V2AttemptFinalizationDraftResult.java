package com.mycompany.tutorhub_enterprise.server.services;

import java.sql.Timestamp;

public class V2AttemptFinalizationDraftResult {
    private boolean success;
    private String errorCode;
    private long submitRecordId;
    private int examId;
    private int paperId;
    private String attemptId;
    private String payloadHash;
    private String previousStatus;
    private String newStatus;
    private String finalizationMode;
    private Timestamp createdAt;

    public V2AttemptFinalizationDraftResult(boolean success, String errorCode) {
        this.success = success;
        this.errorCode = errorCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public long getSubmitRecordId() {
        return submitRecordId;
    }

    public void setSubmitRecordId(long submitRecordId) {
        this.submitRecordId = submitRecordId;
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

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getFinalizationMode() {
        return finalizationMode;
    }

    public void setFinalizationMode(String finalizationMode) {
        this.finalizationMode = finalizationMode;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
