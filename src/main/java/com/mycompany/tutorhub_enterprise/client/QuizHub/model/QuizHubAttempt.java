package com.mycompany.tutorhub_enterprise.client.quizhub.model;

import java.util.ArrayList;
import java.util.List;

/** 1 lượt làm bài (Ôn tập hoặc Thi thử) của 1 deck. mode: "study" | "exam". */
public class QuizHubAttempt {

    private String id;
    private String deckId;
    private String mode;
    private String startedAt;
    private String finishedAt;
    private List<QuizHubAnswerRecord> answers = new ArrayList<>();
    private int correctCount;
    private int totalCount;
    private double score;
    private long durationSeconds;
    private int accuracyPoints;
    private int maxAccuracyPoints;
    private int sessionScore;
    private int bestStreak;
    private double averageResponseMs;
    private long timeLimitMs;
    private int questionLimit;
    private String topicFilter;
    private String difficultyFilter;
    private boolean soundEnabled;
    private boolean motionEnabled;
    private boolean powerupsEnabled;
    private List<QuizHubPowerUpEvent> powerUpEvents = new ArrayList<>();

    public QuizHubAttempt() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeckId() {
        return deckId;
    }

    public void setDeckId(String deckId) {
        this.deckId = deckId;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
    }

    public List<QuizHubAnswerRecord> getAnswers() {
        return answers;
    }

    public void setAnswers(List<QuizHubAnswerRecord> answers) {
        this.answers = answers;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(int correctCount) {
        this.correctCount = correctCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public int getAccuracyPoints() {
        return accuracyPoints;
    }

    public void setAccuracyPoints(int accuracyPoints) {
        this.accuracyPoints = accuracyPoints;
    }

    public int getMaxAccuracyPoints() {
        return maxAccuracyPoints;
    }

    public void setMaxAccuracyPoints(int maxAccuracyPoints) {
        this.maxAccuracyPoints = maxAccuracyPoints;
    }

    public int getSessionScore() {
        return sessionScore;
    }

    public void setSessionScore(int sessionScore) {
        this.sessionScore = sessionScore;
    }

    public int getBestStreak() {
        return bestStreak;
    }

    public void setBestStreak(int bestStreak) {
        this.bestStreak = bestStreak;
    }

    public double getAverageResponseMs() {
        return averageResponseMs;
    }

    public void setAverageResponseMs(double averageResponseMs) {
        this.averageResponseMs = averageResponseMs;
    }

    public long getTimeLimitMs() {
        return timeLimitMs;
    }

    public void setTimeLimitMs(long timeLimitMs) {
        this.timeLimitMs = timeLimitMs;
    }

    public int getQuestionLimit() {
        return questionLimit;
    }

    public void setQuestionLimit(int questionLimit) {
        this.questionLimit = questionLimit;
    }

    public String getTopicFilter() {
        return topicFilter;
    }

    public void setTopicFilter(String topicFilter) {
        this.topicFilter = topicFilter;
    }

    public String getDifficultyFilter() {
        return difficultyFilter;
    }

    public void setDifficultyFilter(String difficultyFilter) {
        this.difficultyFilter = difficultyFilter;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    public boolean isMotionEnabled() {
        return motionEnabled;
    }

    public void setMotionEnabled(boolean motionEnabled) {
        this.motionEnabled = motionEnabled;
    }

    public boolean isPowerupsEnabled() {
        return powerupsEnabled;
    }

    public void setPowerupsEnabled(boolean powerupsEnabled) {
        this.powerupsEnabled = powerupsEnabled;
    }

    public List<QuizHubPowerUpEvent> getPowerUpEvents() {
        return powerUpEvents;
    }

    public void setPowerUpEvents(List<QuizHubPowerUpEvent> powerUpEvents) {
        this.powerUpEvents = powerUpEvents;
    }
}
