package com.mycompany.tutorhub_enterprise.server.services;

import java.util.List;

public class V2RustLockdownCoreProbeResult {
    private boolean success;
    private boolean ready;
    private boolean osWindows;
    private boolean vmLikely;
    private boolean inputDesktopDetected;
    private boolean canOpenInputDesktop;
    private boolean canCreateDesktop;
    private String errorCode;
    private List<String> warnings;
    private String checkedAt;

    public V2RustLockdownCoreProbeResult() {}

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public boolean isOsWindows() { return osWindows; }
    public void setOsWindows(boolean osWindows) { this.osWindows = osWindows; }

    public boolean isVmLikely() { return vmLikely; }
    public void setVmLikely(boolean vmLikely) { this.vmLikely = vmLikely; }

    public boolean isInputDesktopDetected() { return inputDesktopDetected; }
    public void setInputDesktopDetected(boolean inputDesktopDetected) { this.inputDesktopDetected = inputDesktopDetected; }

    public boolean isCanOpenInputDesktop() { return canOpenInputDesktop; }
    public void setCanOpenInputDesktop(boolean canOpenInputDesktop) { this.canOpenInputDesktop = canOpenInputDesktop; }

    public boolean isCanCreateDesktop() { return canCreateDesktop; }
    public void setCanCreateDesktop(boolean canCreateDesktop) { this.canCreateDesktop = canCreateDesktop; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public String getCheckedAt() { return checkedAt; }
    public void setCheckedAt(String checkedAt) { this.checkedAt = checkedAt; }
}
