package com.mycompany.tutorhub_enterprise.models.exam;

import java.io.Serializable;

public class ExamPackageOption implements Serializable {
    private static final long serialVersionUID = 1L;

    public int optionId;
    public String optionLabel;
    public String content;
    public int orderIndex;
    
    // IMPORTANT: isCorrect is deliberately EXCLUDED from this model 
    // to prevent cheating and leaking answers to the client.
    
    public ExamPackageOption() {}
}
