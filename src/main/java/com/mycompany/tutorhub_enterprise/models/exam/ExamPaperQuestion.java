package com.mycompany.tutorhub_enterprise.models.exam;

import java.sql.Timestamp;

public class ExamPaperQuestion {
    public int id;
    public int paperId;
    public int questionId;
    public float score;
    public int orderIndex;
    public boolean required;
    public Timestamp createdAt;

    public ExamPaperQuestion() {
    }
}
