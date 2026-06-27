package com.mycompany.tutorhub_enterprise.client.exam.ui;

import java.util.List;

public class TSEV2SubmitPayload {
    private String payloadVersion;
    private String flow;
    private int examId;
    private int paperId;
    private String attemptId;
    private String packageHash;
    private int questionCount;
    private int answeredCount;
    private int unansweredCount;
    private boolean complete;
    private String draftSnapshotHash;
    private String payloadHash;
    private String preparedAt;
    private List<TSEV2SubmitAnswerItem> answers;

    public TSEV2SubmitPayload() {
    }

    public String getPayloadVersion() {
        return payloadVersion;
    }

    public void setPayloadVersion(String payloadVersion) {
        this.payloadVersion = payloadVersion;
    }

    public String getFlow() {
        return flow;
    }

    public void setFlow(String flow) {
        this.flow = flow;
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

    public String getDraftSnapshotHash() {
        return draftSnapshotHash;
    }

    public void setDraftSnapshotHash(String draftSnapshotHash) {
        this.draftSnapshotHash = draftSnapshotHash;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public String getPreparedAt() {
        return preparedAt;
    }

    public void setPreparedAt(String preparedAt) {
        this.preparedAt = preparedAt;
    }

    public List<TSEV2SubmitAnswerItem> getAnswers() {
        return answers;
    }

    public void setAnswers(List<TSEV2SubmitAnswerItem> answers) {
        this.answers = answers;
    }
}
