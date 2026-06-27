package com.mycompany.tutorhub_enterprise.server.services;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class V2RealSubmitReadinessOrchestratorResult {
    private boolean success;
    private boolean ready;
    private String errorCode;
    private int userId;
    private int examId;
    private int paperId;
    private String attemptId;
    private long submitRecordId;
    private String payloadHash;
    private boolean preflightReady;
    private boolean transitionDraftReady;
    private boolean attemptStatusGateReady;
    private boolean attemptStatusTransitionDraftReady;
    private String preflightStatus;
    private String transitionDraftStatus;
    private String attemptStatusGateStatus;
    private String attemptStatusTransitionDraftStatus;
    private String readinessStatus;
    private List<String> warnings = new ArrayList<>();
    private List<String> blockingReasons = new ArrayList<>();
    private Timestamp checkedAt;

    public V2RealSubmitReadinessOrchestratorResult() {
        this.checkedAt = new Timestamp(System.currentTimeMillis());
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getExamId() { return examId; }
    public void setExamId(int examId) { this.examId = examId; }

    public int getPaperId() { return paperId; }
    public void setPaperId(int paperId) { this.paperId = paperId; }

    public String getAttemptId() { return attemptId; }
    public void setAttemptId(String attemptId) { this.attemptId = attemptId; }

    public long getSubmitRecordId() { return submitRecordId; }
    public void setSubmitRecordId(long submitRecordId) { this.submitRecordId = submitRecordId; }

    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }

    public boolean isPreflightReady() { return preflightReady; }
    public void setPreflightReady(boolean preflightReady) { this.preflightReady = preflightReady; }

    public boolean isTransitionDraftReady() { return transitionDraftReady; }
    public void setTransitionDraftReady(boolean transitionDraftReady) { this.transitionDraftReady = transitionDraftReady; }

    public boolean isAttemptStatusGateReady() { return attemptStatusGateReady; }
    public void setAttemptStatusGateReady(boolean attemptStatusGateReady) { this.attemptStatusGateReady = attemptStatusGateReady; }

    public boolean isAttemptStatusTransitionDraftReady() { return attemptStatusTransitionDraftReady; }
    public void setAttemptStatusTransitionDraftReady(boolean attemptStatusTransitionDraftReady) { this.attemptStatusTransitionDraftReady = attemptStatusTransitionDraftReady; }

    public String getPreflightStatus() { return preflightStatus; }
    public void setPreflightStatus(String preflightStatus) { this.preflightStatus = preflightStatus; }

    public String getTransitionDraftStatus() { return transitionDraftStatus; }
    public void setTransitionDraftStatus(String transitionDraftStatus) { this.transitionDraftStatus = transitionDraftStatus; }

    public String getAttemptStatusGateStatus() { return attemptStatusGateStatus; }
    public void setAttemptStatusGateStatus(String attemptStatusGateStatus) { this.attemptStatusGateStatus = attemptStatusGateStatus; }

    public String getAttemptStatusTransitionDraftStatus() { return attemptStatusTransitionDraftStatus; }
    public void setAttemptStatusTransitionDraftStatus(String attemptStatusTransitionDraftStatus) { this.attemptStatusTransitionDraftStatus = attemptStatusTransitionDraftStatus; }

    public String getReadinessStatus() { return readinessStatus; }
    public void setReadinessStatus(String readinessStatus) { this.readinessStatus = readinessStatus; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public void addWarning(String warning) { this.warnings.add(warning); }

    public List<String> getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(List<String> blockingReasons) { this.blockingReasons = blockingReasons; }
    public void addBlockingReason(String blockingReason) { this.blockingReasons.add(blockingReason); }

    public Timestamp getCheckedAt() { return checkedAt; }
    public void setCheckedAt(Timestamp checkedAt) { this.checkedAt = checkedAt; }
}
