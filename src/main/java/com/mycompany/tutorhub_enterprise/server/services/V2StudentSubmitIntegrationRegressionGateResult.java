package com.mycompany.tutorhub_enterprise.server.services;

public class V2StudentSubmitIntegrationRegressionGateResult {
    private boolean success;
    private boolean ready;
    private String regressionStatus;
    private boolean defaultV2Enabled;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public String getRegressionStatus() { return regressionStatus; }
    public void setRegressionStatus(String regressionStatus) { this.regressionStatus = regressionStatus; }

    public boolean isDefaultV2Enabled() { return defaultV2Enabled; }
    public void setDefaultV2Enabled(boolean defaultV2Enabled) { this.defaultV2Enabled = defaultV2Enabled; }
}
