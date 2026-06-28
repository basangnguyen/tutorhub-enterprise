package com.mycompany.tutorhub_enterprise.client.quizhub.model;

/**
 * Bản rút gọn của QuizHubDeck, dùng cho màn hình chọn đề / quản lý đề
 * (không kèm toàn bộ câu hỏi, chỉ thông tin hiển thị trên thẻ đề).
 */
public class QuizHubDeckSummary {

    private String id;
    private String title;
    private String description;
    private String subject;
    private String color;
    private int questionCount;
    private String createdAt;
    private String updatedAt;
    private String source;

    public QuizHubDeckSummary() {
    }

    public static QuizHubDeckSummary from(QuizHubDeck deck) {
        QuizHubDeckSummary summary = new QuizHubDeckSummary();
        summary.id = deck.getId();
        summary.title = deck.getTitle();
        summary.description = deck.getDescription();
        summary.subject = deck.getSubject();
        summary.color = deck.getColor();
        summary.questionCount = deck.getQuestionCount();
        summary.createdAt = deck.getCreatedAt();
        summary.updatedAt = deck.getUpdatedAt();
        summary.source = deck.getSource();
        return summary;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
