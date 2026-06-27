package com.mycompany.tutorhub_enterprise.server.services;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class V2ScoreDraftResult {
    private boolean success;
    private boolean ready;
    private String errorCode;
    private long scoreDraftId;
    private int userId;
    private int examId;
    private int paperId;
    private String attemptId;
    private long submitRecordId;
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
    private Timestamp createdAt;
    private List<String> warnings = new ArrayList<>();
    private List<String> blockingReasons = new ArrayList<>();

    public V2ScoreDraftResult() {
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public long getScoreDraftId() { return scoreDraftId; }
    public void setScoreDraftId(long scoreDraftId) { this.scoreDraftId = scoreDraftId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getExamId() { return examId; }
    public void setExamId(int examId) { this.examId = examId; }

    public int getPaperId() { return paperId; }
    public void setPaperId(int paperId) { this.paperId = paperId; }

    public String getAttemptId() { return attemptId; }
    public void setAttemptId(String attemptId) { this.attemptId = attemptId; }

    public long getSubmitRecordId() { return submitRecordId; }
    public void setSubmitRecordId(long submitRecordId) { this.submitRecordId = submitRecordId; }

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

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public void addWarning(String warning) { this.warnings.add(warning); }

    public List<String> getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(List<String> blockingReasons) { this.blockingReasons = blockingReasons; }
    public void addBlockingReason(String blockingReason) { this.blockingReasons.add(blockingReason); }
}
