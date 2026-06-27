package com.mycompany.tutorhub_enterprise.models.exam;

import java.sql.Timestamp;

public class ExamPaper {
    public int id;
    public String title;
    public String description;
    public int creatorId;
    public String status; // DRAFT, PUBLISHED, ARCHIVED
    public float totalScore;
    public Timestamp createdAt;
    public Timestamp updatedAt;

    public ExamPaper() {
    }
}
