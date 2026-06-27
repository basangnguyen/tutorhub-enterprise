package com.mycompany.tutorhub_enterprise.models.exam;

import java.sql.Timestamp;

public class V2OfficialResultDraftRecord {
    private long id;
    private long scoreDraftId;
    private long submitRecordId;
    private int userId;
    private int examId;
    private int paperId;
    private String attemptId;
    private String payloadHash;
    private int totalQuestions;
    private int answeredQuestions;
    private int unansweredQuestions;
    private int correctCount;
    private int incorrectCount;
    private double rawScore;
    private double maxScore;
    private double percentage;
    private String scoreDraftStatus;
    private String scoreDraftAuditStatus;
    private String officialResultDraftStatus;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getScoreDraftId() { return scoreDraftId; }
    public void setScoreDraftId(long scoreDraftId) { this.scoreDraftId = scoreDraftId; }
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
    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public int getAnsweredQuestions() { return answeredQuestions; }
    public void setAnsweredQuestions(int answeredQuestions) { this.answeredQuestions = answeredQuestions; }
    public int getUnansweredQuestions() { return unansweredQuestions; }
    public void setUnansweredQuestions(int unansweredQuestions) { this.unansweredQuestions = unansweredQuestions; }
    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
    public int getIncorrectCount() { return incorrectCount; }
    public void setIncorrectCount(int incorrectCount) { this.incorrectCount = incorrectCount; }
    public double getRawScore() { return rawScore; }
    public void setRawScore(double rawScore) { this.rawScore = rawScore; }
    public double getMaxScore() { return maxScore; }
    public void setMaxScore(double maxScore) { this.maxScore = maxScore; }
    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
    public String getScoreDraftStatus() { return scoreDraftStatus; }
    public void setScoreDraftStatus(String scoreDraftStatus) { this.scoreDraftStatus = scoreDraftStatus; }
    public String getScoreDraftAuditStatus() { return scoreDraftAuditStatus; }
    public void setScoreDraftAuditStatus(String scoreDraftAuditStatus) { this.scoreDraftAuditStatus = scoreDraftAuditStatus; }
    public String getOfficialResultDraftStatus() { return officialResultDraftStatus; }
    public void setOfficialResultDraftStatus(String officialResultDraftStatus) { this.officialResultDraftStatus = officialResultDraftStatus; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
