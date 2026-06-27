package com.mycompany.tutorhub_enterprise.server.services;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class V2ResultPublicationReadinessResult {
    private boolean success;
    private boolean ready;
    private String errorCode;
    private long officialResultDraftId;
    private long scoreDraftId;
    private long submitRecordId;
    private int userId;
    private int examId;
    private int paperId;
    private String attemptId;
    private String payloadHash;
    private String attemptStatus;
    private String scoreDraftStatus;
    private String scoreDraftAuditStatus;
    private String officialResultDraftStatus;
    private String publicationReadinessStatus;
    private Timestamp checkedAt;
    private List<String> warnings = new ArrayList<>();
    private List<String> blockingReasons = new ArrayList<>();

    public V2ResultPublicationReadinessResult() {
        this.checkedAt = new Timestamp(System.currentTimeMillis());
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public long getOfficialResultDraftId() { return officialResultDraftId; }
    public void setOfficialResultDraftId(long officialResultDraftId) { this.officialResultDraftId = officialResultDraftId; }
    public long getScoreDraftId() { return scoreDraftId; }
    public void setScoreDraftId(long scoreDraftId) { this.scoreDraftId = scoreDraftId; }
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
    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }
    public String getAttemptStatus() { return attemptStatus; }
    public void setAttemptStatus(String attemptStatus) { this.attemptStatus = attemptStatus; }
    public String getScoreDraftStatus() { return scoreDraftStatus; }
    public void setScoreDraftStatus(String scoreDraftStatus) { this.scoreDraftStatus = scoreDraftStatus; }
    public String getScoreDraftAuditStatus() { return scoreDraftAuditStatus; }
    public void setScoreDraftAuditStatus(String scoreDraftAuditStatus) { this.scoreDraftAuditStatus = scoreDraftAuditStatus; }
    public String getOfficialResultDraftStatus() { return officialResultDraftStatus; }
    public void setOfficialResultDraftStatus(String officialResultDraftStatus) { this.officialResultDraftStatus = officialResultDraftStatus; }
    public String getPublicationReadinessStatus() { return publicationReadinessStatus; }
    public void setPublicationReadinessStatus(String publicationReadinessStatus) { this.publicationReadinessStatus = publicationReadinessStatus; }
    public Timestamp getCheckedAt() { return checkedAt; }
    public void setCheckedAt(Timestamp checkedAt) { this.checkedAt = checkedAt; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public void addWarning(String warning) { this.warnings.add(warning); }
    public List<String> getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(List<String> blockingReasons) { this.blockingReasons = blockingReasons; }
    public void addBlockingReason(String blockingReason) { this.blockingReasons.add(blockingReason); }
}
