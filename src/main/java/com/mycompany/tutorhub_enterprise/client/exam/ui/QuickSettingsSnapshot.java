package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class QuickSettingsSnapshot {
    private static final Gson GSON = new GsonBuilder().create();

    public final int snapshotVersion;
    public final long timestamp;

    public final String wifiStatus;
    public final String wifiSsid;
    public final int wifiSignal;
    public final String wifiMode;
    public final String wifiError;

    public final boolean volumeSupported;
    public final boolean volumeWritable;
    public final int volumePercent;
    public final boolean volumeMuted;
    public final int volumeVersion;
    public final boolean volumePending;
    public final String volumeError;

    public final boolean brightnessSupported;
    public final boolean brightnessWritable;
    public final int brightnessPercent;
    public final int brightnessVersion;
    public final boolean brightnessPending;
    public final String brightnessError;

    public final boolean hasBattery;
    public final int batteryPercent;
    public final boolean batteryCharging;
    public final boolean batteryLow;
    public final boolean batteryCritical;

    public final String clockTime;
    public final String clockDate;

    public final String inputMode;

    public final boolean refreshEnabled;
    public final boolean exitAllowed;

    private QuickSettingsSnapshot(Builder builder) {
        this.snapshotVersion = builder.snapshotVersion;
        this.timestamp = builder.timestamp;
        
        this.wifiStatus = builder.wifiStatus;
        this.wifiSsid = builder.wifiSsid;
        this.wifiSignal = builder.wifiSignal;
        this.wifiMode = builder.wifiMode;
        this.wifiError = builder.wifiError;

        this.volumeSupported = builder.volumeSupported;
        this.volumeWritable = builder.volumeWritable;
        this.volumePercent = builder.volumePercent;
        this.volumeMuted = builder.volumeMuted;
        this.volumeVersion = builder.volumeVersion;
        this.volumePending = builder.volumePending;
        this.volumeError = builder.volumeError;

        this.brightnessSupported = builder.brightnessSupported;
        this.brightnessWritable = builder.brightnessWritable;
        this.brightnessPercent = builder.brightnessPercent;
        this.brightnessVersion = builder.brightnessVersion;
        this.brightnessPending = builder.brightnessPending;
        this.brightnessError = builder.brightnessError;

        this.hasBattery = builder.hasBattery;
        this.batteryPercent = builder.batteryPercent;
        this.batteryCharging = builder.batteryCharging;
        this.batteryLow = builder.batteryLow;
        this.batteryCritical = builder.batteryCritical;

        this.clockTime = builder.clockTime;
        this.clockDate = builder.clockDate;

        this.inputMode = builder.inputMode;

        this.refreshEnabled = builder.refreshEnabled;
        this.exitAllowed = builder.exitAllowed;
    }

    public static QuickSettingsSnapshot initial(TSESecurityPolicy policy) {
        return new Builder()
                .setWifiMode(policy.getWifiMode().name())
                .setRefreshEnabled(policy.getRefreshPolicy() == TSESecurityPolicy.RefreshPolicy.ENABLED)
                .setExitAllowed(policy.getExitPolicy() == TSESecurityPolicy.ExitPolicy.ALLOWED)
                .build();
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static class Builder {
        private int snapshotVersion = 0;
        private long timestamp = System.currentTimeMillis();

        private String wifiStatus = "disconnected";
        private String wifiSsid = "";
        private int wifiSignal = 0;
        private String wifiMode = "READ_ONLY";
        private String wifiError = null;

        private boolean volumeSupported = false;
        private boolean volumeWritable = false;
        private int volumePercent = 0;
        private boolean volumeMuted = false;
        private int volumeVersion = 0;
        private boolean volumePending = false;
        private String volumeError = null;

        private boolean brightnessSupported = false;
        private boolean brightnessWritable = false;
        private int brightnessPercent = 0;
        private int brightnessVersion = 0;
        private boolean brightnessPending = false;
        private String brightnessError = null;

        private boolean hasBattery = false;
        private int batteryPercent = 0;
        private boolean batteryCharging = false;
        private boolean batteryLow = false;
        private boolean batteryCritical = false;

        private String clockTime = "";
        private String clockDate = "";

        private String inputMode = "en";

        private boolean refreshEnabled = true;
        private boolean exitAllowed = true;

        public Builder() {}
        
        public Builder(QuickSettingsSnapshot copy) {
            this.snapshotVersion = copy.snapshotVersion;
            
            this.wifiStatus = copy.wifiStatus;
            this.wifiSsid = copy.wifiSsid;
            this.wifiSignal = copy.wifiSignal;
            this.wifiMode = copy.wifiMode;
            this.wifiError = copy.wifiError;
            
            this.volumeSupported = copy.volumeSupported;
            this.volumeWritable = copy.volumeWritable;
            this.volumePercent = copy.volumePercent;
            this.volumeMuted = copy.volumeMuted;
            this.volumeVersion = copy.volumeVersion;
            this.volumePending = copy.volumePending;
            this.volumeError = copy.volumeError;
            
            this.brightnessSupported = copy.brightnessSupported;
            this.brightnessWritable = copy.brightnessWritable;
            this.brightnessPercent = copy.brightnessPercent;
            this.brightnessVersion = copy.brightnessVersion;
            this.brightnessPending = copy.brightnessPending;
            this.brightnessError = copy.brightnessError;
            
            this.hasBattery = copy.hasBattery;
            this.batteryPercent = copy.batteryPercent;
            this.batteryCharging = copy.batteryCharging;
            this.batteryLow = copy.batteryLow;
            this.batteryCritical = copy.batteryCritical;
            
            this.clockTime = copy.clockTime;
            this.clockDate = copy.clockDate;
            
            this.inputMode = copy.inputMode;
            
            this.refreshEnabled = copy.refreshEnabled;
            this.exitAllowed = copy.exitAllowed;
        }

        private int clampPercent(int value) {
            return Math.max(0, Math.min(100, value));
        }

        private String nullSafe(String value) {
            return value == null ? "" : value;
        }

        public Builder setSnapshotVersion(int snapshotVersion) { this.snapshotVersion = snapshotVersion; return this; }
        public Builder setTimestamp(long timestamp) { this.timestamp = timestamp; return this; }

        public Builder setWifiStatus(String wifiStatus) { this.wifiStatus = nullSafe(wifiStatus); return this; }
        public Builder setWifiSsid(String wifiSsid) { this.wifiSsid = nullSafe(wifiSsid); return this; }
        public Builder setWifiSignal(int wifiSignal) { this.wifiSignal = clampPercent(wifiSignal); return this; }
        public Builder setWifiMode(String wifiMode) { this.wifiMode = nullSafe(wifiMode); return this; }
        public Builder setWifiError(String wifiError) { this.wifiError = wifiError; return this; }

        public Builder setVolumeSupported(boolean volumeSupported) { this.volumeSupported = volumeSupported; return this; }
        public Builder setVolumeWritable(boolean volumeWritable) { this.volumeWritable = volumeWritable; return this; }
        public Builder setVolumePercent(int volumePercent) { this.volumePercent = clampPercent(volumePercent); return this; }
        public Builder setVolumeMuted(boolean volumeMuted) { this.volumeMuted = volumeMuted; return this; }
        public Builder setVolumeVersion(int volumeVersion) { this.volumeVersion = volumeVersion; return this; }
        public Builder setVolumePending(boolean volumePending) { this.volumePending = volumePending; return this; }
        public Builder setVolumeError(String volumeError) { this.volumeError = volumeError; return this; }

        public Builder setBrightnessSupported(boolean brightnessSupported) { this.brightnessSupported = brightnessSupported; return this; }
        public Builder setBrightnessWritable(boolean brightnessWritable) { this.brightnessWritable = brightnessWritable; return this; }
        public Builder setBrightnessPercent(int brightnessPercent) { this.brightnessPercent = clampPercent(brightnessPercent); return this; }
        public Builder setBrightnessVersion(int brightnessVersion) { this.brightnessVersion = brightnessVersion; return this; }
        public Builder setBrightnessPending(boolean brightnessPending) { this.brightnessPending = brightnessPending; return this; }
        public Builder setBrightnessError(String brightnessError) { this.brightnessError = brightnessError; return this; }

        public Builder setHasBattery(boolean hasBattery) { this.hasBattery = hasBattery; return this; }
        public Builder setBatteryPercent(int batteryPercent) { this.batteryPercent = clampPercent(batteryPercent); return this; }
        public Builder setBatteryCharging(boolean batteryCharging) { this.batteryCharging = batteryCharging; return this; }
        public Builder setBatteryLow(boolean batteryLow) { this.batteryLow = batteryLow; return this; }
        public Builder setBatteryCritical(boolean batteryCritical) { this.batteryCritical = batteryCritical; return this; }

        public Builder setClockTime(String clockTime) { this.clockTime = nullSafe(clockTime); return this; }
        public Builder setClockDate(String clockDate) { this.clockDate = nullSafe(clockDate); return this; }

        public Builder setInputMode(String inputMode) { this.inputMode = nullSafe(inputMode); return this; }

        public Builder setRefreshEnabled(boolean refreshEnabled) { this.refreshEnabled = refreshEnabled; return this; }
        public Builder setExitAllowed(boolean exitAllowed) { this.exitAllowed = exitAllowed; return this; }

        public QuickSettingsSnapshot build() {
            return new QuickSettingsSnapshot(this);
        }
    }
}
