package com.mycompany.tutorhub_enterprise.client.exam.ui;

public class TSESecurityPolicy {

    public enum WifiMode {
        DISABLED, READ_ONLY, SCAN_ONLY, WHITELIST_CONNECT, PROFILE_CONNECT
    }

    public enum BrightnessMode {
        DISABLED, READ_ONLY, WRITABLE_IF_SUPPORTED
    }

    public enum VolumeMode {
        DISABLED, WRITABLE, MUTE_ONLY
    }

    public enum BatteryMode {
        READ_ONLY
    }

    public enum ClockMode {
        SHOW, HIDE
    }

    public enum InputModePolicy {
        ENABLED, DISABLED
    }

    public enum RefreshPolicy {
        ENABLED, DISABLED_DURING_SUBMIT
    }

    public enum ExitPolicy {
        ALLOWED, BLOCKED
    }

    private final WifiMode wifiMode;
    private final BrightnessMode brightnessMode;
    private final VolumeMode volumeMode;
    private final BatteryMode batteryMode;
    private final ClockMode clockMode;
    private final InputModePolicy inputModePolicy;
    private final RefreshPolicy refreshPolicy;
    private final ExitPolicy exitPolicy;

    private TSESecurityPolicy(WifiMode wifiMode, BrightnessMode brightnessMode, VolumeMode volumeMode,
                              BatteryMode batteryMode, ClockMode clockMode, InputModePolicy inputModePolicy,
                              RefreshPolicy refreshPolicy, ExitPolicy exitPolicy) {
        this.wifiMode = wifiMode;
        this.brightnessMode = brightnessMode;
        this.volumeMode = volumeMode;
        this.batteryMode = batteryMode;
        this.clockMode = clockMode;
        this.inputModePolicy = inputModePolicy;
        this.refreshPolicy = refreshPolicy;
        this.exitPolicy = exitPolicy;
    }

    public static TSESecurityPolicy forLogin() {
        return new TSESecurityPolicy(
                WifiMode.READ_ONLY, // or SCAN_ONLY
                BrightnessMode.WRITABLE_IF_SUPPORTED,
                VolumeMode.WRITABLE,
                BatteryMode.READ_ONLY,
                ClockMode.SHOW,
                InputModePolicy.ENABLED,
                RefreshPolicy.ENABLED,
                ExitPolicy.ALLOWED
        );
    }

    public static TSESecurityPolicy forConfig() {
        return new TSESecurityPolicy(
                WifiMode.READ_ONLY,
                BrightnessMode.WRITABLE_IF_SUPPORTED,
                VolumeMode.WRITABLE,
                BatteryMode.READ_ONLY,
                ClockMode.SHOW,
                InputModePolicy.ENABLED,
                RefreshPolicy.ENABLED,
                ExitPolicy.ALLOWED
        );
    }

    public static TSESecurityPolicy forExam() {
        return new TSESecurityPolicy(
                WifiMode.READ_ONLY,
                BrightnessMode.WRITABLE_IF_SUPPORTED,
                VolumeMode.WRITABLE,
                BatteryMode.READ_ONLY,
                ClockMode.SHOW,
                InputModePolicy.ENABLED,
                RefreshPolicy.DISABLED_DURING_SUBMIT,
                ExitPolicy.BLOCKED
        );
    }

    public static TSESecurityPolicy forDebug() {
        return new TSESecurityPolicy(
                WifiMode.SCAN_ONLY,
                BrightnessMode.WRITABLE_IF_SUPPORTED,
                VolumeMode.WRITABLE,
                BatteryMode.READ_ONLY,
                ClockMode.SHOW,
                InputModePolicy.ENABLED,
                RefreshPolicy.ENABLED,
                ExitPolicy.ALLOWED
        );
    }

    // Helper Methods
    public boolean canSetVolume() {
        return volumeMode == VolumeMode.WRITABLE;
    }

    public boolean canSetBrightness() {
        return brightnessMode == BrightnessMode.WRITABLE_IF_SUPPORTED;
    }

    public boolean canConnectWifi() {
        return wifiMode == WifiMode.WHITELIST_CONNECT || wifiMode == WifiMode.PROFILE_CONNECT;
    }

    public boolean canRefresh() {
        return refreshPolicy == RefreshPolicy.ENABLED;
    }

    // Getters
    public WifiMode getWifiMode() { return wifiMode; }
    public BrightnessMode getBrightnessMode() { return brightnessMode; }
    public VolumeMode getVolumeMode() { return volumeMode; }
    public BatteryMode getBatteryMode() { return batteryMode; }
    public ClockMode getClockMode() { return clockMode; }
    public InputModePolicy getInputModePolicy() { return inputModePolicy; }
    public RefreshPolicy getRefreshPolicy() { return refreshPolicy; }
    public ExitPolicy getExitPolicy() { return exitPolicy; }
}
