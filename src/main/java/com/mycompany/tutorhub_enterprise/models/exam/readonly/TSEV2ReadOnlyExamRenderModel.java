package com.mycompany.tutorhub_enterprise.models.exam.readonly;

import java.util.List;
import java.util.ArrayList;

public class TSEV2ReadOnlyExamRenderModel {
    private int examId;
    private int paperId;
    private String attemptId;
    private String deadlineAt;
    private int questionCount;
    private double totalScore;
    private String packageHash;
    private List<TSEV2ReadOnlyQuestionView> questions = new ArrayList<>();

    public TSEV2ReadOnlyExamRenderModel() {}

    public int getExamId() { return examId; }
    public void setExamId(int examId) { this.examId = examId; }

    public int getPaperId() { return paperId; }
    public void setPaperId(int paperId) { this.paperId = paperId; }

    public String getAttemptId() { return attemptId; }
    public void setAttemptId(String attemptId) { this.attemptId = attemptId; }

    public String getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(String deadlineAt) { this.deadlineAt = deadlineAt; }

    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }

    public double getTotalScore() { return totalScore; }
    public void setTotalScore(double totalScore) { this.totalScore = totalScore; }

    public String getPackageHash() { return packageHash; }
    public void setPackageHash(String packageHash) { this.packageHash = packageHash; }

    public List<TSEV2ReadOnlyQuestionView> getQuestions() { return questions; }
    public void setQuestions(List<TSEV2ReadOnlyQuestionView> questions) { this.questions = questions; }
}
