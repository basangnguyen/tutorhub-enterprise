package com.mycompany.tutorhub_enterprise.server.services;

public class V2RustPortableIpcProbeOnlyVerificationResult {
    private boolean success;
    private boolean ready;
    private String errorCode;
    private String portableIpcStatus;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getPortableIpcStatus() { return portableIpcStatus; }
    public void setPortableIpcStatus(String portableIpcStatus) { this.portableIpcStatus = portableIpcStatus; }
}
