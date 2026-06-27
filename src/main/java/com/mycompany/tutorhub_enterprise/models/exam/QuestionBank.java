package com.mycompany.tutorhub_enterprise.models.exam;

import java.io.Serializable;
import java.sql.Timestamp;

public class QuestionBank implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public int id;
    public String name;
    public String description;
    public int creatorId;
    public Timestamp createdAt;
    public Timestamp updatedAt;

    public QuestionBank() {}
}
