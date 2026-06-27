package com.mycompany.tutorhub_enterprise.server.services;

import java.time.Instant;
import java.util.List;

public class V2RustPhysicalMachineSafeProbeGateResult {
    private boolean success;
    private boolean ready;
    private String errorCode;
    private boolean physicalMachineDetected;
    private boolean vmConfirmed;
    private boolean desktopDemoAllowed;
    private boolean probeOnlyAllowed;
    private boolean rustCoreExists;
    private boolean productionFlagsSafe;
    private String preflightStatus;
    private List<String> warnings;
    private List<String> blockingReasons;
    private String checkedAt;

    public V2RustPhysicalMachineSafeProbeGateResult() {
        this.checkedAt = Instant.now().toString();
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public boolean isPhysicalMachineDetected() { return physicalMachineDetected; }
    public void setPhysicalMachineDetected(boolean physicalMachineDetected) { this.physicalMachineDetected = physicalMachineDetected; }
    public boolean isVmConfirmed() { return vmConfirmed; }
    public void setVmConfirmed(boolean vmConfirmed) { this.vmConfirmed = vmConfirmed; }
    public boolean isDesktopDemoAllowed() { return desktopDemoAllowed; }
    public void setDesktopDemoAllowed(boolean desktopDemoAllowed) { this.desktopDemoAllowed = desktopDemoAllowed; }
    public boolean isProbeOnlyAllowed() { return probeOnlyAllowed; }
    public void setProbeOnlyAllowed(boolean probeOnlyAllowed) { this.probeOnlyAllowed = probeOnlyAllowed; }
    public boolean isRustCoreExists() { return rustCoreExists; }
    public void setRustCoreExists(boolean rustCoreExists) { this.rustCoreExists = rustCoreExists; }
    public boolean isProductionFlagsSafe() { return productionFlagsSafe; }
    public void setProductionFlagsSafe(boolean productionFlagsSafe) { this.productionFlagsSafe = productionFlagsSafe; }
    public String getPreflightStatus() { return preflightStatus; }
    public void setPreflightStatus(String preflightStatus) { this.preflightStatus = preflightStatus; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public List<String> getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(List<String> blockingReasons) { this.blockingReasons = blockingReasons; }
    public String getCheckedAt() { return checkedAt; }
    public void setCheckedAt(String checkedAt) { this.checkedAt = checkedAt; }
}
