package com.mycompany.tutorhub_enterprise.server.services;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class V2ManualCandidateScoreOfficialDraftExecutionResult {
    private boolean success;
    private boolean ready;
    private boolean idempotent;
    private String errorCode;
    private int userId;
    private int examId;
    private int paperId;
    private String attemptId;
    private long submitRecordId;
    private long scoreDraftId;
    private long officialResultDraftId;
    private String draftExecutionStatus;
    private Timestamp executedAt;
    
    private List<String> warnings = new ArrayList<>();
    private List<String> blockingReasons = new ArrayList<>();

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public boolean isIdempotent() { return idempotent; }
    public void setIdempotent(boolean idempotent) { this.idempotent = idempotent; }

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

    public long getScoreDraftId() { return scoreDraftId; }
    public void setScoreDraftId(long scoreDraftId) { this.scoreDraftId = scoreDraftId; }

    public long getOfficialResultDraftId() { return officialResultDraftId; }
    public void setOfficialResultDraftId(long officialResultDraftId) { this.officialResultDraftId = officialResultDraftId; }

    public String getDraftExecutionStatus() { return draftExecutionStatus; }
    public void setDraftExecutionStatus(String draftExecutionStatus) { this.draftExecutionStatus = draftExecutionStatus; }

    public Timestamp getExecutedAt() { return executedAt; }
    public void setExecutedAt(Timestamp executedAt) { this.executedAt = executedAt; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public void addWarning(String warning) { this.warnings.add(warning); }

    public List<String> getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(List<String> blockingReasons) { this.blockingReasons = blockingReasons; }
    public void addBlockingReason(String reason) { this.blockingReasons.add(reason); }
}
