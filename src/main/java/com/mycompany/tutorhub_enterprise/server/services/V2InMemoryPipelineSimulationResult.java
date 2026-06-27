package com.mycompany.tutorhub_enterprise.server.services;

import java.time.LocalDateTime;
import java.util.List;

public class V2InMemoryPipelineSimulationResult {
    private boolean success;
    private boolean ready;
    private boolean simulated;
    private String errorCode;
    private int userId;
    private int examId;
    private int paperId;
    private String attemptId;
    private int answerCount;
    private int questionCount;
    private int plannedStepCount;
    private String simulationStatus;
    private List<String> plannedSteps;
    private List<String> warnings;
    private List<String> blockingReasons;
    private LocalDateTime checkedAt;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isSimulated() {
        return simulated;
    }

    public void setSimulated(boolean simulated) {
        this.simulated = simulated;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getExamId() {
        return examId;
    }

    public void setExamId(int examId) {
        this.examId = examId;
    }

    public int getPaperId() {
        return paperId;
    }

    public void setPaperId(int paperId) {
        this.paperId = paperId;
    }

    public String getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(String attemptId) {
        this.attemptId = attemptId;
    }

    public int getAnswerCount() {
        return answerCount;
    }

    public void setAnswerCount(int answerCount) {
        this.answerCount = answerCount;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }

    public int getPlannedStepCount() {
        return plannedStepCount;
    }

    public void setPlannedStepCount(int plannedStepCount) {
        this.plannedStepCount = plannedStepCount;
    }

    public String getSimulationStatus() {
        return simulationStatus;
    }

    public void setSimulationStatus(String simulationStatus) {
        this.simulationStatus = simulationStatus;
    }

    public List<String> getPlannedSteps() {
        return plannedSteps;
    }

    public void setPlannedSteps(List<String> plannedSteps) {
        this.plannedSteps = plannedSteps;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<String> getBlockingReasons() {
        return blockingReasons;
    }

    public void setBlockingReasons(List<String> blockingReasons) {
        this.blockingReasons = blockingReasons;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }
}
