package com.mycompany.tutorhub_enterprise.models.exam;

import java.io.Serializable;
import java.sql.Timestamp;

public class ExamSession implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public int id;
    public int examId;
    public int userId;
    public String status;
    
    public Timestamp startedAt;
    public Timestamp submittedAt;
    public int durationUsed;
    
    public float totalScore;
    public float maxScore;
    public boolean autoGraded;
    
    public int violationCount;
    public float trustScoreAvg;
    public String tekHash;
    public String clientInfo;
    public String questionOrder; // JSON string array
    
    public Timestamp createdAt;

    public ExamSession() {}
}
