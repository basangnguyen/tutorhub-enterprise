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
}
