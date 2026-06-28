package com.mycompany.tutorhub_enterprise.client.quizhub.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 1 đề/bộ câu hỏi hoàn chỉnh — đơn vị lưu trữ chính (1 file JSON = 1 deck).
 * source: "builtin" (có sẵn trong quiz.html) hoặc "excel_import" (nhập từ Excel).
 */
public class QuizHubDeck {

    private String id;
    private String title;
    private String description;
    private String subject;
    private String color;
    private String source;
    private String createdAt;
    private String updatedAt;
    private QuizHubDeckOptions defaultOptions;
    private List<QuizHubQuestion> questions = new ArrayList<>();

    public QuizHubDeck() {
    }

    public int getQuestionCount() {
        return questions == null ? 0 : questions.size();
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
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

    public QuizHubDeckOptions getDefaultOptions() {
        return defaultOptions;
    }

    public void setDefaultOptions(QuizHubDeckOptions defaultOptions) {
        this.defaultOptions = defaultOptions;
    }

    public List<QuizHubQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuizHubQuestion> questions) {
        this.questions = questions;
    }
}
