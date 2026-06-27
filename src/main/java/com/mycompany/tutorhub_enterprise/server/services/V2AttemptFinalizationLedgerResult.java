package com.mycompany.tutorhub_enterprise.server.services;

import java.sql.Timestamp;

public class V2AttemptFinalizationLedgerResult {
    private boolean success;
    private String errorCode;
    private Long ledgerId;
    private Long submitRecordId;
    private Integer examId;
    private Integer paperId;
    private String attemptId;
    private String payloadHash;
    private String previousSubmitStatus;
    private String finalizationStatus;
    private String finalizationMode;
    private Timestamp createdAt;
    private boolean idempotent;

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

    public Long getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(Long ledgerId) {
        this.ledgerId = ledgerId;
    }

    public Long getSubmitRecordId() {
        return submitRecordId;
    }

    public void setSubmitRecordId(Long submitRecordId) {
        this.submitRecordId = submitRecordId;
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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isIdempotent() {
        return idempotent;
    }

    public void setIdempotent(boolean idempotent) {
        this.idempotent = idempotent;
    }
}
