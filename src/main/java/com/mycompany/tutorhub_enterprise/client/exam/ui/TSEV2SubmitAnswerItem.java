package com.mycompany.tutorhub_enterprise.client.exam.ui;

public class TSEV2SubmitAnswerItem {
    private int questionId;
    private int selectedOptionId;
    private String answeredAt;

    public TSEV2SubmitAnswerItem() {
    }

    public TSEV2SubmitAnswerItem(int questionId, int selectedOptionId, String answeredAt) {
        this.questionId = questionId;
        this.selectedOptionId = selectedOptionId;
        this.answeredAt = answeredAt;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public int getSelectedOptionId() {
        return selectedOptionId;
    }

    public void setSelectedOptionId(int selectedOptionId) {
        this.selectedOptionId = selectedOptionId;
    }

    public String getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(String answeredAt) {
        this.answeredAt = answeredAt;
    }
}
