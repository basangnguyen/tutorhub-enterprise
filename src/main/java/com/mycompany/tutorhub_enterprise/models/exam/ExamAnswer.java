package com.mycompany.tutorhub_enterprise.models.exam;

import java.io.Serializable;
import java.sql.Timestamp;

public class ExamAnswer implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public int id;
    public int sessionId;
    public int questionId;
    public String answerData; // JSON string
    
    public Boolean isCorrect; // Use Boolean to allow null
    public Float score;       // Use Float to allow null
    
    public int timeSpentSec;
    public int changeCount;
    public Timestamp lastUpdated;

    public ExamAnswer() {}
}
