package com.mycompany.tutorhub_enterprise.server.services;

import java.time.Instant;
import java.util.List;

public class V2DemoNotLockdownFinalDecisionGateResult {
    private boolean success;
    private boolean ready;
    private String decision;
    private String errorCode;
    private boolean probeOnlyMode;
    private String vmEvidenceStatus;
    private String probeStatus;
    private String desktopDemoStatus;
    private String portableIpcStatus;
    private String securityScanStatus;
    private boolean productionFlagsSafe;
    private String decisionStatus;
    private List<String> warnings;
    private List<String> blockingReasons;
    private String checkedAt;

    public V2DemoNotLockdownFinalDecisionGateResult() {
        this.checkedAt = Instant.now().toString();
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public boolean isProbeOnlyMode() { return probeOnlyMode; }
    public void setProbeOnlyMode(boolean probeOnlyMode) { this.probeOnlyMode = probeOnlyMode; }
    public String getVmEvidenceStatus() { return vmEvidenceStatus; }
    public void setVmEvidenceStatus(String vmEvidenceStatus) { this.vmEvidenceStatus = vmEvidenceStatus; }
    public String getProbeStatus() { return probeStatus; }
    public void setProbeStatus(String probeStatus) { this.probeStatus = probeStatus; }
    public String getDesktopDemoStatus() { return desktopDemoStatus; }
    public void setDesktopDemoStatus(String desktopDemoStatus) { this.desktopDemoStatus = desktopDemoStatus; }
    public String getPortableIpcStatus() { return portableIpcStatus; }
    public void setPortableIpcStatus(String portableIpcStatus) { this.portableIpcStatus = portableIpcStatus; }
    public String getSecurityScanStatus() { return securityScanStatus; }
    public void setSecurityScanStatus(String securityScanStatus) { this.securityScanStatus = securityScanStatus; }
    public boolean isProductionFlagsSafe() { return productionFlagsSafe; }
    public void setProductionFlagsSafe(boolean productionFlagsSafe) { this.productionFlagsSafe = productionFlagsSafe; }
    public String getDecisionStatus() { return decisionStatus; }
    public void setDecisionStatus(String decisionStatus) { this.decisionStatus = decisionStatus; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public List<String> getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(List<String> blockingReasons) { this.blockingReasons = blockingReasons; }
    public String getCheckedAt() { return checkedAt; }
    public void setCheckedAt(String checkedAt) { this.checkedAt = checkedAt; }
}
