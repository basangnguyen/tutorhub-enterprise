package com.mycompany.tutorhub_enterprise.server.services;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class V2AttemptStatusTransitionDraftResult {
    private boolean success;
    private boolean ready;
    private boolean idempotent;
    private String errorCode;
    private long draftId;
    private long submitRecordId;
    private int userId;
    private int examId;
    private int paperId;
    private String attemptId;
    private long transitionDraftId;
    private String payloadHash;
    private String preflightStatus;
    private String realSubmitTransitionDraftStatus;
    private String attemptStatusGateStatus;
    private String attemptStatusTransitionDraftStatus;
    private String fromAttemptStatus;
    private String targetAttemptStatus;
    private Timestamp createdAt;
    private List<String> warnings = new ArrayList<>();
    private List<String> blockingReasons = new ArrayList<>();

    public V2AttemptStatusTransitionDraftResult() {
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public boolean isIdempotent() { return idempotent; }
    public void setIdempotent(boolean idempotent) { this.idempotent = idempotent; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public long getDraftId() { return draftId; }
    public void setDraftId(long draftId) { this.draftId = draftId; }

    public long getSubmitRecordId() { return submitRecordId; }
    public void setSubmitRecordId(long submitRecordId) { this.submitRecordId = submitRecordId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getExamId() { return examId; }
    public void setExamId(int examId) { this.examId = examId; }

    public int getPaperId() { return paperId; }
    public void setPaperId(int paperId) { this.paperId = paperId; }

    public String getAttemptId() { return attemptId; }
    public void setAttemptId(String attemptId) { this.attemptId = attemptId; }

    public long getTransitionDraftId() { return transitionDraftId; }
    public void setTransitionDraftId(long transitionDraftId) { this.transitionDraftId = transitionDraftId; }

    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }

    public String getPreflightStatus() { return preflightStatus; }
    public void setPreflightStatus(String preflightStatus) { this.preflightStatus = preflightStatus; }

    public String getRealSubmitTransitionDraftStatus() { return realSubmitTransitionDraftStatus; }
    public void setRealSubmitTransitionDraftStatus(String realSubmitTransitionDraftStatus) { this.realSubmitTransitionDraftStatus = realSubmitTransitionDraftStatus; }

    public String getAttemptStatusGateStatus() { return attemptStatusGateStatus; }
    public void setAttemptStatusGateStatus(String attemptStatusGateStatus) { this.attemptStatusGateStatus = attemptStatusGateStatus; }

    public String getAttemptStatusTransitionDraftStatus() { return attemptStatusTransitionDraftStatus; }
    public void setAttemptStatusTransitionDraftStatus(String attemptStatusTransitionDraftStatus) { this.attemptStatusTransitionDraftStatus = attemptStatusTransitionDraftStatus; }

    public String getFromAttemptStatus() { return fromAttemptStatus; }
    public void setFromAttemptStatus(String fromAttemptStatus) { this.fromAttemptStatus = fromAttemptStatus; }

    public String getTargetAttemptStatus() { return targetAttemptStatus; }
    public void setTargetAttemptStatus(String targetAttemptStatus) { this.targetAttemptStatus = targetAttemptStatus; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public void addWarning(String warning) { this.warnings.add(warning); }

    public List<String> getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(List<String> blockingReasons) { this.blockingReasons = blockingReasons; }
    public void addBlockingReason(String blockingReason) { this.blockingReasons.add(blockingReason); }
}
