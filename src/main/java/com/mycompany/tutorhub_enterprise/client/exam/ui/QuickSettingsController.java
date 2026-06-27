package com.mycompany.tutorhub_enterprise.client.exam.ui;

public class QuickSettingsController {

    private final QuickSettingsStateStore stateStore;
    private TSESecurityPolicy currentPolicy;
    
    private VolumeService volumeService;
    private BrightnessService brightnessService;

    public QuickSettingsController(QuickSettingsStateStore stateStore) {
        this.stateStore = stateStore;
        this.currentPolicy = TSESecurityPolicy.forLogin(); // Default
    }

    public void setNativeServices(VolumeService volume, BrightnessService brightness) {
        this.volumeService = volume;
        this.brightnessService = brightness;
    }

    public QuickSettingsSnapshot getSnapshot() {
        return stateStore.getSnapshot();
    }

    public void applyPolicy(TSESecurityPolicy policy) {
        this.currentPolicy = policy;
        stateStore.updatePolicy(policy);
    }

    public void requestRefresh() {
        if (!currentPolicy.canRefresh()) {
            System.out.println("[TSE_QS_CTRL] Refresh disabled by policy.");
            return;
        }
        System.out.println("[TSE_QS_CONTROLLER] requestRefresh");
        
        if (volumeService != null) {
            TSEVolumeStatus volStat = volumeService.getStatus();
            stateStore.updateVolume(volStat.supported, volStat.writable, volStat.percent, volStat.muted, stateStore.getSnapshot().volumeVersion, false, volStat.message);
        }

        if (brightnessService != null) {
            brightnessService.refreshNow();
        }

        // if (networkService != null) {
        //     networkService.refreshNow();
        // }

        // if (batteryService != null) {
        //     batteryService.refreshNow();
        // }
    }

    public void setVolume(int percent, String requestId) {
        QuickSettingsSnapshot current = stateStore.getSnapshot();
        if (!currentPolicy.canSetVolume()) {
            System.out.println("[TSE_QS_CTRL] Volume change disabled by policy.");
            stateStore.updateVolume(
                current.volumeSupported, current.volumeWritable, current.volumePercent, 
                current.volumeMuted, current.volumeVersion + 1, false, 
                "Chức năng bị khóa trong phòng thi"
            );
            return;
        }
        System.out.println("[TSE_QS_CTRL] setVolume to " + percent + " for req " + requestId);
        TSEVolumeStatus result = null;
        if (volumeService != null) {
            result = volumeService.setVolume(percent, requestId);
        } else {
            result = TSEVolumeController.setVolume(percent);
        }
        
        if (result != null) {
            stateStore.updateVolume(result.supported, result.writable, result.percent, result.muted, current.volumeVersion + 1, false, null);
        } else {
            stateStore.updateVolume(current.volumeSupported, current.volumeWritable, percent, current.volumeMuted, current.volumeVersion + 1, false, "Lỗi cập nhật âm lượng");
        }
    }

    public void setMuted(boolean muted, String requestId) {
        QuickSettingsSnapshot current = stateStore.getSnapshot();
        if (!currentPolicy.canSetVolume()) {
            System.out.println("[TSE_QS_CTRL] Mute change disabled by policy.");
            stateStore.updateVolume(
                current.volumeSupported, current.volumeWritable, current.volumePercent, 
                current.volumeMuted, current.volumeVersion + 1, false, 
                "Chức năng bị khóa trong phòng thi"
            );
            return;
        }
        System.out.println("[TSE_QS_CTRL] setMuted to " + muted + " for req " + requestId);
        TSEVolumeStatus result = null;
        if (volumeService != null) {
            result = volumeService.setMuted(muted, requestId);
        } else {
            result = TSEVolumeController.setMuted(muted);
        }
        
        if (result != null) {
            stateStore.updateVolume(result.supported, result.writable, result.percent, result.muted, current.volumeVersion + 1, false, null);
        } else {
            stateStore.updateVolume(current.volumeSupported, current.volumeWritable, current.volumePercent, muted, current.volumeVersion + 1, false, "Lỗi cập nhật âm lượng");
        }
    }

    public void setBrightness(int percent, String requestId) {
        QuickSettingsSnapshot current = stateStore.getSnapshot();
        if (!currentPolicy.canSetBrightness()) {
            System.out.println("[TSE_QS_CTRL] Brightness change disabled by policy.");
            stateStore.updateBrightness(
                current.brightnessSupported, false, current.brightnessPercent, 
                current.brightnessVersion + 1, false, 
                "Chức năng bị khóa trong phòng thi"
            );
            return;
        }
        System.out.println("[TSE_QS_CTRL] setBrightness to " + percent + " for req " + requestId);
        System.out.println("[TSE_QS_CONTROLLER] setBrightness percent=" + percent 
            + ", brightnessServiceInjected=" + (brightnessService != null));
            
        TSEBrightnessStatus result = null;
        if (brightnessService != null) {
            result = brightnessService.setBrightness(percent, requestId);
        } else {
            System.out.println("[TSE_QS_CONTROLLER] WARNING: brightnessService not injected, fallback legacy facade");
            TSEBrightnessController legacyController = new TSEBrightnessController();
            result = legacyController.setBrightness(percent);
        }
        
        if (result != null) {
            stateStore.updateBrightness(result.supported, result.writable, result.percent, current.brightnessVersion + 1, false, null);
        } else {
            stateStore.updateBrightness(current.brightnessSupported, current.brightnessWritable, percent, current.brightnessVersion + 1, false, "Lỗi cập nhật độ sáng");
        }
    }

    public void setInputMode(String mode) {
        if (currentPolicy.getInputModePolicy() == TSESecurityPolicy.InputModePolicy.DISABLED) {
            System.out.println("[TSE_QS_CTRL] Input mode change disabled by policy.");
            return;
        }
        System.out.println("[TSE_QS_CTRL] setInputMode to " + mode + " (skeleton)");
        stateStore.updateInputMode(mode);
    }

    public void shutdown() {
        System.out.println("[TSE_QS_CTRL] shutdown called");
        stateStore.shutdown();
        // TODO: Shutdown native services
    }
}
