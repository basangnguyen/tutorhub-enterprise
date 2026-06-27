package com.mycompany.tutorhub_enterprise.server.services;

public class V2SubmitDryRunPersistenceResult {
    private boolean success;
    private String errorCode;
    private long recordId;
    private int examId;
    private int paperId;
    private String attemptId;
    private String payloadHash;
    private int answeredCount;
    private int unansweredCount;
    private boolean complete;
    private String persistedAt;

    public V2SubmitDryRunPersistenceResult() {
    }

    public static V2SubmitDryRunPersistenceResult error(String errorCode) {
        V2SubmitDryRunPersistenceResult result = new V2SubmitDryRunPersistenceResult();
        result.setSuccess(false);
        result.setErrorCode(errorCode);
        return result;
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

    public long getRecordId() {
        return recordId;
    }

    public void setRecordId(long recordId) {
        this.recordId = recordId;
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

    public int getAnsweredCount() {
        return answeredCount;
    }

    public void setAnsweredCount(int answeredCount) {
        this.answeredCount = answeredCount;
    }

    public int getUnansweredCount() {
        return unansweredCount;
    }

    public void setUnansweredCount(int unansweredCount) {
        this.unansweredCount = unansweredCount;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public String getPersistedAt() {
        return persistedAt;
    }

    public void setPersistedAt(String persistedAt) {
        this.persistedAt = persistedAt;
    }
}
