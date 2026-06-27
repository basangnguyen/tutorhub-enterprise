package com.mycompany.tutorhub_enterprise.server.services;

import java.util.ArrayList;
import java.util.List;

public class V2StudentSubmitAdapterWiringResult {
    private boolean success;
    private boolean ready;
    private String errorCode;
    private int userId;
    private int examId;
    private int paperId;
    private String attemptId;
    private String currentSubmitAction;
    private String resolvedRoute;
    private boolean v2DefaultEnabled;
    private String wiringStatus;
    private List<String> warnings = new ArrayList<>();
    private List<String> blockingReasons = new ArrayList<>();
    private long checkedAt;

    public V2StudentSubmitAdapterWiringResult() {
        this.checkedAt = System.currentTimeMillis();
    }

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

    public String getCurrentSubmitAction() {
        return currentSubmitAction;
    }

    public void setCurrentSubmitAction(String currentSubmitAction) {
        this.currentSubmitAction = currentSubmitAction;
    }

    public String getResolvedRoute() {
        return resolvedRoute;
    }

    public void setResolvedRoute(String resolvedRoute) {
        this.resolvedRoute = resolvedRoute;
    }

    public boolean isV2DefaultEnabled() {
        return v2DefaultEnabled;
    }

    public void setV2DefaultEnabled(boolean v2DefaultEnabled) {
        this.v2DefaultEnabled = v2DefaultEnabled;
    }

    public String getWiringStatus() {
        return wiringStatus;
    }

    public void setWiringStatus(String wiringStatus) {
        this.wiringStatus = wiringStatus;
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

    public long getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(long checkedAt) {
        this.checkedAt = checkedAt;
    }
}
