package com.mycompany.tutorhub_enterprise.client.quizhub.model;

import java.util.List;

/** 1 câu trả lời trong 1 lượt làm bài (QuizHubAttempt.answers). */
public class QuizHubAnswerRecord {

    private String questionId;
    private List<Integer> selected;
    private boolean correct;
    private long timeMs;

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
}
