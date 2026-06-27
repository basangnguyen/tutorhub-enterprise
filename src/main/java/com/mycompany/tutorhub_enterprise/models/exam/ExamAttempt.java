package com.mycompany.tutorhub_enterprise.models.exam;

import java.sql.Timestamp;

public class ExamAttempt {
    private String id;
    private int examId;
    private int paperId;
    private int userId;
    private int attemptNo;
    private String status;
    private Timestamp deadlineAt;
    private String sessionTokenHash;
    private String packageHash;
    private Timestamp startedAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public ExamAttempt() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getExamId() { return examId; }
    public void setExamId(int examId) { this.examId = examId; }

    public int getPaperId() { return paperId; }
    public void setPaperId(int paperId) { this.paperId = paperId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getAttemptNo() { return attemptNo; }
    public void setAttemptNo(int attemptNo) { this.attemptNo = attemptNo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(Timestamp deadlineAt) { this.deadlineAt = deadlineAt; }

    public String getSessionTokenHash() { return sessionTokenHash; }
    public void setSessionTokenHash(String sessionTokenHash) { this.sessionTokenHash = sessionTokenHash; }

    public String getPackageHash() { return packageHash; }
    public void setPackageHash(String packageHash) { this.packageHash = packageHash; }

    public Timestamp getStartedAt() { return startedAt; }
    public void setStartedAt(Timestamp startedAt) { this.startedAt = startedAt; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
