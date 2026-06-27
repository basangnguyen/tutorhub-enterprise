package com.mycompany.tutorhub_enterprise.models.exam;

import java.io.Serializable;
import java.sql.Timestamp;

public class Question implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public int id;
    public int examId;
    public String questionType; // MCQ, ESSAY, FILL_BLANK, CODE
    public String category;
    public String difficulty;
    public float points;
    
    public String content; // JSON string
    public String explanation;
    public int sortOrder;
    public Timestamp createdAt;

    // New fields for Phase 2: Question Bank integration
    public int bankId;
    public float defaultScore;
    public int createdBy;
    public Timestamp updatedAt;

    public Question() {}
}
