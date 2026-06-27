package com.mycompany.tutorhub_enterprise.server.services;

import java.time.Instant;
import java.util.List;

public class V2DemoRcTrialCompletionGateResult {
    private boolean success;
    private boolean ready;
    private String decision;
    private String errorCode;
    private boolean docsReady;
    private String testAccountStatus;
    private String manualTrialStatus;
    private boolean mavenTestsPass;
    private boolean portableBuildPass;
    private boolean cargoTestsPass;
    private boolean rustProbePass;
    private boolean productionFlagsSafe;
    private String desktopDemoStatus;
    private String lockdownStatus;
    private boolean repoStatusRecorded;
    private List<String> warnings;
    private List<String> blockingReasons;
    private String checkedAt;

    public V2DemoRcTrialCompletionGateResult() {
        this.checkedAt = Instant.now().toString();
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public boolean isDocsReady() { return docsReady; }
    public void setDocsReady(boolean docsReady) { this.docsReady = docsReady; }
    public String getTestAccountStatus() { return testAccountStatus; }
    public void setTestAccountStatus(String testAccountStatus) { this.testAccountStatus = testAccountStatus; }
    public String getManualTrialStatus() { return manualTrialStatus; }
    public void setManualTrialStatus(String manualTrialStatus) { this.manualTrialStatus = manualTrialStatus; }
    public boolean isMavenTestsPass() { return mavenTestsPass; }
    public void setMavenTestsPass(boolean mavenTestsPass) { this.mavenTestsPass = mavenTestsPass; }
    public boolean isPortableBuildPass() { return portableBuildPass; }
    public void setPortableBuildPass(boolean portableBuildPass) { this.portableBuildPass = portableBuildPass; }
    public boolean isCargoTestsPass() { return cargoTestsPass; }
    public void setCargoTestsPass(boolean cargoTestsPass) { this.cargoTestsPass = cargoTestsPass; }
    public boolean isRustProbePass() { return rustProbePass; }
    public void setRustProbePass(boolean rustProbePass) { this.rustProbePass = rustProbePass; }
    public boolean isProductionFlagsSafe() { return productionFlagsSafe; }
    public void setProductionFlagsSafe(boolean productionFlagsSafe) { this.productionFlagsSafe = productionFlagsSafe; }
    public String getDesktopDemoStatus() { return desktopDemoStatus; }
    public void setDesktopDemoStatus(String desktopDemoStatus) { this.desktopDemoStatus = desktopDemoStatus; }
    public String getLockdownStatus() { return lockdownStatus; }
    public void setLockdownStatus(String lockdownStatus) { this.lockdownStatus = lockdownStatus; }
    public boolean isRepoStatusRecorded() { return repoStatusRecorded; }
    public void setRepoStatusRecorded(boolean repoStatusRecorded) { this.repoStatusRecorded = repoStatusRecorded; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public List<String> getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(List<String> blockingReasons) { this.blockingReasons = blockingReasons; }
    public String getCheckedAt() { return checkedAt; }
    public void setCheckedAt(String checkedAt) { this.checkedAt = checkedAt; }
}
