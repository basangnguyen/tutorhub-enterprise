package com.mycompany.tutorhub_enterprise.client.exam.ui;

import java.util.ArrayList;
import java.util.List;

public class TSEV2AnswerDraftSnapshot {

    private String snapshotVersion;
    private String flow;
    private int examId;
    private int paperId;
    private String attemptId;
    private String packageHash;
    private int questionCount;
    private int answeredCount;
    private String createdAt;
    private String updatedAt;
    private List<TSEV2AnswerDraftItem> answers = new ArrayList<>();
    private String snapshotHash;

    public String getSnapshotVersion() {
        return snapshotVersion;
    }

    public void setSnapshotVersion(String snapshotVersion) {
        this.snapshotVersion = snapshotVersion;
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

    public List<TSEV2AnswerDraftItem> getAnswers() {
        return answers;
    }

    public void setAnswers(List<TSEV2AnswerDraftItem> answers) {
        this.answers = answers == null ? new ArrayList<>() : new ArrayList<>(answers);
    }

    public String getSnapshotHash() {
        return snapshotHash;
    }

    public void setSnapshotHash(String snapshotHash) {
        this.snapshotHash = snapshotHash;
    }
}
