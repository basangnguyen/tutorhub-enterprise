package com.mycompany.tutorhub_enterprise.models.exam;

import java.io.Serializable;

public class QuestionOption implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public int id;
    public int questionId;
    public String optionLabel;
    public String content;
    public boolean isCorrect;
    public int orderIndex;

    public QuestionOption() {}
}
