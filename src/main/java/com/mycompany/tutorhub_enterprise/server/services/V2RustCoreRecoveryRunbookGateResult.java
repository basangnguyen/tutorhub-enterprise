package com.mycompany.tutorhub_enterprise.server.services;

public class V2RustCoreRecoveryRunbookGateResult {
    private boolean ready;
    private String errorCode;
    private boolean success;

    public V2RustCoreRecoveryRunbookGateResult(boolean ready, String errorCode, boolean success) {
        this.ready = ready;
        this.errorCode = errorCode;
        this.success = success;
    }

    public boolean isReady() { return ready; }
    public String getErrorCode() { return errorCode; }
    public boolean isSuccess() { return success; }
}
