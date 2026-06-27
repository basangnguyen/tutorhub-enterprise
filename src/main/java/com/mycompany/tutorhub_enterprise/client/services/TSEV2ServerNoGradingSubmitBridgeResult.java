package com.mycompany.tutorhub_enterprise.client.services;

public class TSEV2ServerNoGradingSubmitBridgeResult {
    private boolean success;
    private String errorCode;
    private long closureDraftId;
    private long ledgerId;
    private long submitRecordId;
    private String payloadHash;
    private String closureStatus;
    private String finalStatus;
    private boolean idempotent;
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    
    public long getClosureDraftId() { return closureDraftId; }
    public void setClosureDraftId(long closureDraftId) { this.closureDraftId = closureDraftId; }
    
    public long getLedgerId() { return ledgerId; }
    public void setLedgerId(long ledgerId) { this.ledgerId = ledgerId; }
    
    public long getSubmitRecordId() { return submitRecordId; }
    public void setSubmitRecordId(long submitRecordId) { this.submitRecordId = submitRecordId; }
    
    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }
    
    public String getClosureStatus() { return closureStatus; }
    public void setClosureStatus(String closureStatus) { this.closureStatus = closureStatus; }

    public String getFinalStatus() { return finalStatus; }
    public void setFinalStatus(String finalStatus) { this.finalStatus = finalStatus; }

    public boolean isIdempotent() { return idempotent; }
    public void setIdempotent(boolean idempotent) { this.idempotent = idempotent; }
}
