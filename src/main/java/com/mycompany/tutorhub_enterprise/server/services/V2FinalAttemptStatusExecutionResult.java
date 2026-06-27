package com.mycompany.tutorhub_enterprise.server.services;

public class V2FinalAttemptStatusExecutionResult {
    private boolean success;
    private boolean ready;
    private boolean idempotent;
    private String errorCode;
    
    private long executionLedgerId;
    private String executionStatus;
    private String actualAttemptStatus;
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    
    public boolean isIdempotent() { return idempotent; }
    public void setIdempotent(boolean idempotent) { this.idempotent = idempotent; }
    
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    
    public long getExecutionLedgerId() { return executionLedgerId; }
    public void setExecutionLedgerId(long executionLedgerId) { this.executionLedgerId = executionLedgerId; }
    
    public String getExecutionStatus() { return executionStatus; }
    public void setExecutionStatus(String executionStatus) { this.executionStatus = executionStatus; }
    
    public String getActualAttemptStatus() { return actualAttemptStatus; }
    public void setActualAttemptStatus(String actualAttemptStatus) { this.actualAttemptStatus = actualAttemptStatus; }
}
