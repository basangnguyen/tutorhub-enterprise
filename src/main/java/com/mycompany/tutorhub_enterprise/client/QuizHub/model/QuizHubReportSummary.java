package com.mycompany.tutorhub_enterprise.client.quizhub.model;

/** Local report summary for a deck, derived from saved attempts. */
public class QuizHubReportSummary {

    private String deckId;
    private int attemptCount;
    private int gameAttemptCount;
    private int bestSessionScore;
    private double bestAccuracyPercent;
    private double averageAccuracyPercent;
    private double averageResponseMs;
    private int bestStreak;
    private String lastAttemptAt;

    public String getDeckId() {
        return deckId;
    }

    public void setDeckId(String deckId) {
        this.deckId = deckId;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public int getGameAttemptCount() {
        return gameAttemptCount;
    }

    public void setGameAttemptCount(int gameAttemptCount) {
        this.gameAttemptCount = gameAttemptCount;
    }

    public int getBestSessionScore() {
        return bestSessionScore;
    }

    public void setBestSessionScore(int bestSessionScore) {
        this.bestSessionScore = bestSessionScore;
    }

    public double getBestAccuracyPercent() {
        return bestAccuracyPercent;
    }

    public void setBestAccuracyPercent(double bestAccuracyPercent) {
        this.bestAccuracyPercent = bestAccuracyPercent;
    }

    public double getAverageAccuracyPercent() {
        return averageAccuracyPercent;
    }

    public void setAverageAccuracyPercent(double averageAccuracyPercent) {
        this.averageAccuracyPercent = averageAccuracyPercent;
    }

    public double getAverageResponseMs() {
        return averageResponseMs;
    }

    public void setAverageResponseMs(double averageResponseMs) {
        this.averageResponseMs = averageResponseMs;
    }

    public int getBestStreak() {
        return bestStreak;
    }

    public void setBestStreak(int bestStreak) {
        this.bestStreak = bestStreak;
    }

    public String getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(String lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }
}
