package com.mycompany.tutorhub_enterprise.models.exam;

import java.io.Serializable;
import java.sql.Timestamp;

public class Exam implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public int id;
    public int creatorId;
    public String title;
    public String description;
    public int durationMins;
    
    public Timestamp openAt;
    public Timestamp closeAt;
    
    public String securityConfig; // JSON string
    public String status;
    
    public Timestamp createdAt;
    public Timestamp updatedAt;
    
    public Integer paperId;

    public Exam() {}

    public Exam(int id, int creatorId, String title, String description, int durationMins, String status) {
        this.id = id;
        this.creatorId = creatorId;
        this.title = title;
        this.description = description;
        this.durationMins = durationMins;
        this.status = status;
    }
}
