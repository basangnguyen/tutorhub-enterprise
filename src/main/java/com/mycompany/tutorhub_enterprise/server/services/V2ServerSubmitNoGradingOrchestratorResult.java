package com.mycompany.tutorhub_enterprise.server.services;

import java.sql.Timestamp;

public class V2ServerSubmitNoGradingOrchestratorResult {
    private boolean success;
    private String errorCode;
    private Long dryRunRecordId;
    private Long submitRecordId;
    private Long ledgerId;
    private Long closureDraftId;
    private Integer examId;
    private Integer paperId;
    private String attemptId;
    private String payloadHash;
    private String finalStatus;
    private int stepsCompleted;
    private Timestamp createdAt;

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

    public Long getDryRunRecordId() {
        return dryRunRecordId;
    }

    public void setDryRunRecordId(Long dryRunRecordId) {
        this.dryRunRecordId = dryRunRecordId;
    }

    public Long getSubmitRecordId() {
        return submitRecordId;
    }

    public void setSubmitRecordId(Long submitRecordId) {
        this.submitRecordId = submitRecordId;
    }

    public Long getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(Long ledgerId) {
        this.ledgerId = ledgerId;
    }

    public Long getClosureDraftId() {
        return closureDraftId;
    }

    public void setClosureDraftId(Long closureDraftId) {
        this.closureDraftId = closureDraftId;
    }

    public Integer getExamId() {
        return examId;
    }

    public void setExamId(Integer examId) {
        this.examId = examId;
    }

    public Integer getPaperId() {
        return paperId;
    }

    public void setPaperId(Integer paperId) {
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

    public String getFinalStatus() {
        return finalStatus;
    }

    public void setFinalStatus(String finalStatus) {
        this.finalStatus = finalStatus;
    }

    public int getStepsCompleted() {
        return stepsCompleted;
    }

    public void setStepsCompleted(int stepsCompleted) {
        this.stepsCompleted = stepsCompleted;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
