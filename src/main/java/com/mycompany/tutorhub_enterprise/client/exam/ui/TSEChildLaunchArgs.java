package com.mycompany.tutorhub_enterprise.client.exam.ui;

public class TSEChildLaunchArgs {
    public enum Mode {
        LEGACY, V2_DEBUG, INVALID
    }

    private Mode mode = Mode.INVALID;
    private String legacyContextPath;
    private String legacyOutputPath;
    private String legacyKeyBase64;
    
    private String v2HandoffMetaPath;
    private String v2HandoffEncPath;
    private boolean v2DebugOnly;
    private String errorMessage;

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }

    public String getLegacyContextPath() { return legacyContextPath; }
    public void setLegacyContextPath(String legacyContextPath) { this.legacyContextPath = legacyContextPath; }

    public String getLegacyOutputPath() { return legacyOutputPath; }
    public void setLegacyOutputPath(String legacyOutputPath) { this.legacyOutputPath = legacyOutputPath; }

    public String getLegacyKeyBase64() { return legacyKeyBase64; }
    public void setLegacyKeyBase64(String legacyKeyBase64) { this.legacyKeyBase64 = legacyKeyBase64; }

    public String getV2HandoffMetaPath() { return v2HandoffMetaPath; }
    public void setV2HandoffMetaPath(String v2HandoffMetaPath) { this.v2HandoffMetaPath = v2HandoffMetaPath; }

    public String getV2HandoffEncPath() { return v2HandoffEncPath; }
    public void setV2HandoffEncPath(String v2HandoffEncPath) { this.v2HandoffEncPath = v2HandoffEncPath; }

    public boolean isV2DebugOnly() { return v2DebugOnly; }
    public void setV2DebugOnly(boolean v2DebugOnly) { this.v2DebugOnly = v2DebugOnly; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
