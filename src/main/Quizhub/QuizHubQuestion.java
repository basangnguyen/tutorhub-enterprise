package com.mycompany.tutorhub_enterprise.client.quizhub.model;

import java.util.List;
import java.util.Map;

/**
 * 1 câu hỏi trắc nghiệm. correct là danh sách CHỈ SỐ (0-based) các đáp án đúng trong options
 * (1 phần tử = câu 1 đáp án, nhiều phần tử = câu nhiều đáp án đúng).
 * id có dạng ổn định: "<deckId>#row-<sốDòngExcel>".
 */
public class QuizHubQuestion {

    private String id;
    private String deckId;
    private String text;
    private List<String> options;
    private List<Integer> correct;
    private String explanation;
    private Map<Integer, String> wrongExplanations;
    private String topic;
    private String difficulty;
    private String imageUrl;
    private int sourceRow;

    public QuizHubQuestion() {
    }

    public boolean isMultiCorrect() {
        return correct != null && correct.size() > 1;
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public List<Integer> getCorrect() {
        return correct;
    }

    public void setCorrect(List<Integer> correct) {
        this.correct = correct;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Map<Integer, String> getWrongExplanations() {
        return wrongExplanations;
    }

    public void setWrongExplanations(Map<Integer, String> wrongExplanations) {
        this.wrongExplanations = wrongExplanations;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getSourceRow() {
        return sourceRow;
    }

    public void setSourceRow(int sourceRow) {
        this.sourceRow = sourceRow;
    }
}
