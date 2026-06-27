package com.mycompany.tutorhub_enterprise.client.exam.ui;

public class TSEV2SubmitDryRunMeta {
    private String schemaVersion;
    private String flow;
    private String mode;
    private int examId;
    private int paperId;
    private String attemptId;
    private String packageHash;
    private int questionCount;
    private int answeredCount;
    private int unansweredCount;
    private boolean complete;
    private String payloadHash;
    private String encryptedFileSha256;
    private String encFileName;
    private String createdAt;
    private String updatedAt;

    public TSEV2SubmitDryRunMeta() {
        this.schemaVersion = "1.0";
        this.flow = "PAPER_START_V2";
        this.mode = "DEBUG_DRY_RUN";
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getFlow() {
        return flow;
    }

    public void setFlow(String flow) {
        this.flow = flow;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
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

    public String getPackageHash() {
        return packageHash;
    }

    public void setPackageHash(String packageHash) {
        this.packageHash = packageHash;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
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

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public String getEncryptedFileSha256() {
        return encryptedFileSha256;
    }

    public void setEncryptedFileSha256(String encryptedFileSha256) {
        this.encryptedFileSha256 = encryptedFileSha256;
    }

    public String getEncFileName() {
        return encFileName;
    }

    public void setEncFileName(String encFileName) {
        this.encFileName = encFileName;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
