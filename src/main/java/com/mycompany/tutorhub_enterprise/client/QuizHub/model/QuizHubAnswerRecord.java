package com.mycompany.tutorhub_enterprise.client.quizhub.model;

import java.util.ArrayList;
import java.util.List;

/** 1 câu trả lời trong 1 lượt làm bài (QuizHubAttempt.answers). */
public class QuizHubAnswerRecord {

    private String questionId;
    private List<Integer> selected;
    private boolean correct;
    private long timeMs;
    private long responseMs;
    private boolean timedOut;
    private int accuracyPoints;
    private int maxAccuracyPoints;
    private int sessionScore;
    private int baseScore;
    private int speedBonus;
    private int streakBonus;
    private int streakBefore;
    private int streakAfter;
    private double comboMultiplier;
    private double powerMultiplier = 1.0d;
    private List<String> powerUpsUsed = new ArrayList<>();

    public QuizHubAnswerRecord() {
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public List<Integer> getSelected() {
        return selected;
    }

    public void setSelected(List<Integer> selected) {
        this.selected = selected;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public long getTimeMs() {
        return timeMs;
    }

    public void setTimeMs(long timeMs) {
        this.timeMs = timeMs;
    }

    public long getResponseMs() {
        return responseMs;
    }

    public void setResponseMs(long responseMs) {
        this.responseMs = responseMs;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
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

    public int getBaseScore() {
        return baseScore;
    }

    public void setBaseScore(int baseScore) {
        this.baseScore = baseScore;
    }

    public int getSpeedBonus() {
        return speedBonus;
    }

    public void setSpeedBonus(int speedBonus) {
        this.speedBonus = speedBonus;
    }

    public int getStreakBonus() {
        return streakBonus;
    }

    public void setStreakBonus(int streakBonus) {
        this.streakBonus = streakBonus;
    }

    public int getStreakBefore() {
        return streakBefore;
    }

    public void setStreakBefore(int streakBefore) {
        this.streakBefore = streakBefore;
    }

    public int getStreakAfter() {
        return streakAfter;
    }

    public void setStreakAfter(int streakAfter) {
        this.streakAfter = streakAfter;
    }

    public double getComboMultiplier() {
        return comboMultiplier;
    }

    public void setComboMultiplier(double comboMultiplier) {
        this.comboMultiplier = comboMultiplier;
    }

    public double getPowerMultiplier() {
        return powerMultiplier;
    }

    public void setPowerMultiplier(double powerMultiplier) {
        this.powerMultiplier = powerMultiplier;
    }

    public List<String> getPowerUpsUsed() {
        return powerUpsUsed;
    }

    public void setPowerUpsUsed(List<String> powerUpsUsed) {
        this.powerUpsUsed = powerUpsUsed;
    }
}
