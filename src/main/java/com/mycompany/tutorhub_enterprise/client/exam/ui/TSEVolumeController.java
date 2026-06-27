package com.mycompany.tutorhub_enterprise.client.exam.ui;

public class TSEVolumeController {

    private static final VolumeService service = new VolumeService();

    // Default constructor is used in TSEParentHtmlQuickSettingsPopup
    public TSEVolumeController() {
    }

    public static TSEVolumeStatus getStatus() {
        return service.getStatus();
    }

    public static int getLastNonZeroVolume() {
        return service.getLastNonZeroVolume();
    }

    public static TSEVolumeStatus setVolume(int percent) {
        return service.setVolume(percent, null);
    }

    public static TSEVolumeStatus setMuted(boolean muted) {
        return service.setMuted(muted, null);
    }

    public static void shutdownNowNoBlock() {
        service.terminate();
    }
}
