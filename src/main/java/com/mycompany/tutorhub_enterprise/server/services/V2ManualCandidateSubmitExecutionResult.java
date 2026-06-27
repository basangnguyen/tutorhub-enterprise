package com.mycompany.tutorhub_enterprise.server.services;

import java.util.ArrayList;
import java.util.List;

public class V2ManualCandidateSubmitExecutionResult {
    private boolean success;
    private boolean ready;
    private boolean executed;
    private boolean idempotent;
    private String errorCode;
    
    private int userId;
    private int examId;
    private int paperId;
    private String attemptId;
    
    private long manualExecutionLedgerId;
    private String executionStatus;
    private String executionMode;
    
    private List<String> warnings = new ArrayList<>();
    private List<String> blockingReasons = new ArrayList<>();
    
    private java.sql.Timestamp checkedAt;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public boolean isExecuted() { return executed; }
    public void setExecuted(boolean executed) { this.executed = executed; }

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

    public long getManualExecutionLedgerId() { return manualExecutionLedgerId; }
    public void setManualExecutionLedgerId(long manualExecutionLedgerId) { this.manualExecutionLedgerId = manualExecutionLedgerId; }

    public String getExecutionStatus() { return executionStatus; }
    public void setExecutionStatus(String executionStatus) { this.executionStatus = executionStatus; }

    public String getExecutionMode() { return executionMode; }
    public void setExecutionMode(String executionMode) { this.executionMode = executionMode; }

    public List<String> getWarnings() { return warnings; }
    public void addWarning(String warning) { this.warnings.add(warning); }

    public List<String> getBlockingReasons() { return blockingReasons; }
    public void addBlockingReason(String blockingReason) { this.blockingReasons.add(blockingReason); }

    public java.sql.Timestamp getCheckedAt() { return checkedAt; }
    public void setCheckedAt(java.sql.Timestamp checkedAt) { this.checkedAt = checkedAt; }
}
