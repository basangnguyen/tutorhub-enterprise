package com.mycompany.tutorhub_enterprise.server.services;

import java.util.ArrayList;
import java.util.List;

public class V2RealSubmitPreflightResult {
    private boolean success;
    private boolean ready;
    private String errorCode;
    private int userId;
    private int examId;
    private int paperId;
    private String attemptId;
    private long submitRecordId;
    private long ledgerId;
    private long closureDraftId;
    private String payloadHash;
    private String currentSubmitStatus;
    private String closureStatus;
    private String preflightStatus;
    private String checkedAt;
    private List<String> blockingReasons;
    private List<String> warnings;

    public V2RealSubmitPreflightResult() {
        this.blockingReasons = new ArrayList<>();
        this.warnings = new ArrayList<>();
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

    public long getSubmitRecordId() {
        return submitRecordId;
    }

    public void setSubmitRecordId(long submitRecordId) {
        this.submitRecordId = submitRecordId;
    }

    public long getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(long ledgerId) {
        this.ledgerId = ledgerId;
    }

    public long getClosureDraftId() {
        return closureDraftId;
    }

    public void setClosureDraftId(long closureDraftId) {
        this.closureDraftId = closureDraftId;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public String getCurrentSubmitStatus() {
        return currentSubmitStatus;
    }

    public void setCurrentSubmitStatus(String currentSubmitStatus) {
        this.currentSubmitStatus = currentSubmitStatus;
    }

    public String getClosureStatus() {
        return closureStatus;
    }

    public void setClosureStatus(String closureStatus) {
        this.closureStatus = closureStatus;
    }

    public String getPreflightStatus() {
        return preflightStatus;
    }

    public void setPreflightStatus(String preflightStatus) {
        this.preflightStatus = preflightStatus;
    }

    public String getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(String checkedAt) {
        this.checkedAt = checkedAt;
    }

    public List<String> getBlockingReasons() {
        return blockingReasons;
    }

    public void setBlockingReasons(List<String> blockingReasons) {
        this.blockingReasons = blockingReasons;
    }
    
    public void addBlockingReason(String reason) {
        this.blockingReasons.add(reason);
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
    
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}
