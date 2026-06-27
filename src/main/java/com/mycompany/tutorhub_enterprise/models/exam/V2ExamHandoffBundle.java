package com.mycompany.tutorhub_enterprise.models.exam;

import java.util.List;

public class V2ExamHandoffBundle {
    public String handoffVersion = "tutorhub_exam_handoff_v2";
    public String flow; // PAPER_START_V2
    public int examId;
    public int paperId;
    public String attemptId;
    public transient String sessionToken; // MUST NOT BE SERIALIZED TO JSON
    public boolean sessionTokenPresent;
    public String sessionTokenMasked;
    public String sessionTokenHash;
    public String deadlineAt;
    public int durationMinutes;
    public String packageHash;
    public int questionCount;
    public float totalScore;
    public List<V2ExamHandoffQuestion> questions;
    public String clientBuild;
    public String createdAt;
}
