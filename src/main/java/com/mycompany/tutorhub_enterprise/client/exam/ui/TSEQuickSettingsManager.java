package com.mycompany.tutorhub_enterprise.client.exam.ui;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Point;

public class TSEQuickSettingsManager {
    private static int lastAnchorX = 0;
    private static int lastAnchorY = 0;
    private static boolean lastInputTestEnabled = false;

    private static final QuickSettingsStateStore stateStore;
    private static final QuickSettingsController controller;
    
    private static final ClockService clockService;
    private static final BatteryService batteryService;
    private static final VolumeService volumeService;
    private static final NetworkService networkService;
    private static final BrightnessService brightnessService;

    static {
        TSESecurityPolicy policy = TSESecurityPolicy.forExam();
        stateStore = new QuickSettingsStateStore(policy);
        controller = new QuickSettingsController(stateStore);
        controller.applyPolicy(policy);

        clockService = new ClockService(stateStore);
        batteryService = new BatteryService(stateStore);
        volumeService = new VolumeService(stateStore);
        networkService = new NetworkService(stateStore);
        brightnessService = new BrightnessService(stateStore);
    }

    private static boolean servicesStarted = false;

    private static synchronized void ensureServicesStarted() {
        if (!servicesStarted) {
            System.out.println("[TSE_QS_EXAM] Starting background services for Exam Quick Settings");
            clockService.initialize();
            batteryService.initialize();
            volumeService.initialize();
            networkService.initialize();
            brightnessService.initialize();
            controller.setNativeServices(volumeService, brightnessService);
            servicesStarted = true;
        }
    }

    public static void showQuickSettingsDOM(JComponent anchorComponent, TSEBrowserPanel browserPanel, boolean inputTestEnabled) {
        if (anchorComponent == null || browserPanel == null) return;
        
        Point pt = null;
        try {
            pt = SwingUtilities.convertPoint(anchorComponent, 0, 0, browserPanel);
        } catch (Exception e) {
            // Components not in same window
        }
        
        int x = 0, y = 0;
        if (pt != null) {
            x = pt.x + anchorComponent.getWidth() / 2;
            y = pt.y;
        } else {
            // Default to center-bottom of browser panel for disconnected popups
            x = browserPanel.getWidth() / 2;
            y = browserPanel.getHeight();
        }
        
        showQuickSettingsDOMAt(x, y, browserPanel, inputTestEnabled);
    }

    public static void showQuickSettingsDOMAt(int anchorX, int anchorY, TSEBrowserPanel browserPanel, boolean inputTestEnabled) {
        lastInputTestEnabled = inputTestEnabled;
        if (browserPanel == null) return;
        
        lastAnchorX = anchorX;
        lastAnchorY = anchorY;

        ensureServicesStarted();
        
        // Let's request a refresh to the controller
        controller.requestRefresh();

        System.out.println("[TSE_QS_EXAM] showQuickSettingsDOMAt getSnapshot");
        String snapshotJson = controller.getSnapshot().toJson();
        String payload = String.format("{\"anchorX\": %d, \"anchorY\": %d, \"inputTestEnabled\": %b, \"snapshot\": %s}",
            anchorX, anchorY, inputTestEnabled, snapshotJson);
        
        sendPayloadToJS(browserPanel, payload);
    }
    
    public static void refreshWifi(TSEBrowserPanel browserPanel) {
        if (browserPanel == null) return;
        
        ensureServicesStarted();
        controller.requestRefresh();
        
        System.out.println("[TSE_QS_EXAM] refreshWifi getSnapshot");
        String snapshotJson = controller.getSnapshot().toJson();
        String payload = String.format("{\"anchorX\": %d, \"anchorY\": %d, \"inputTestEnabled\": %b, \"snapshot\": %s}",
            lastAnchorX, lastAnchorY, lastInputTestEnabled, snapshotJson);
        
        sendPayloadToJS(browserPanel, payload);
    }

    private static void sendPayloadToJS(TSEBrowserPanel browserPanel, String jsonPayload) {
        try {
            String jsCode = "if (window.TSETrayFlyout) { window.TSETrayFlyout.showQuickSettings(" + jsonPayload + "); }";
            browserPanel.executeJavaScript(jsCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static boolean brightnessSetInProgress = false;
    private static Integer pendingBrightnessValue = null;
    private static java.util.function.Consumer<String> pendingCallback = null;
    
    private static boolean volumeSetInProgress = false;
    private static Integer pendingVolumeValue = null;
    private static Boolean pendingMuteValue = null;
    private static java.util.function.Consumer<String> pendingVolumeCallback = null;

    public static synchronized void setBrightness(int percent, java.util.function.Consumer<String> jsCallback) {
        if (brightnessSetInProgress) {
            pendingBrightnessValue = percent;
            pendingCallback = jsCallback;
            return;
        }
        brightnessSetInProgress = true;
        
        Thread t = new Thread(() -> {
            int currentPercent = percent;
            java.util.function.Consumer<String> currentCallback = jsCallback;

            while (true) {
                System.out.println("[TSE_QS_EXAM] setBrightness percent=" + currentPercent);
                controller.setBrightness(currentPercent, "exam-brightness-" + System.currentTimeMillis());
                if (currentCallback != null) {
                    currentCallback.accept("SUCCESS");
                }

                synchronized (TSEQuickSettingsManager.class) {
                    if (pendingBrightnessValue == null) {
                        pendingCallback = null;
                        brightnessSetInProgress = false;
                        return;
                    }

                    currentPercent = pendingBrightnessValue;
                    currentCallback = pendingCallback;
                    pendingBrightnessValue = null;
                    pendingCallback = null;
                }
            }
        }, "TSE-BrightnessSetter");
        t.setDaemon(true);
        t.start();
    }

    public static synchronized void setVolume(int percent, java.util.function.Consumer<String> jsCallback) {
        if (volumeSetInProgress) {
            pendingVolumeValue = percent;
            pendingVolumeCallback = jsCallback;
            return;
        }
        volumeSetInProgress = true;
        
        Thread t = new Thread(() -> {
            controller.setVolume(percent, "exam-volume");
            if (jsCallback != null) jsCallback.accept("SUCCESS");
            
            processNextVolume();
        }, "TSE-VolumeSetter");
        t.setDaemon(true);
        t.start();
    }

    public static synchronized void setMuted(boolean muted, java.util.function.Consumer<String> jsCallback) {
        if (volumeSetInProgress) {
            pendingMuteValue = muted;
            pendingVolumeCallback = jsCallback;
            return;
        }
        volumeSetInProgress = true;
        
        Thread t = new Thread(() -> {
            controller.setMuted(muted, "exam-mute");
            if (jsCallback != null) jsCallback.accept("SUCCESS");
            
            processNextVolume();
        }, "TSE-VolumeMuter");
        t.setDaemon(true);
        t.start();
    }
    
    private static synchronized void processNextVolume() {
        if (pendingVolumeValue != null) {
            int nextVal = pendingVolumeValue;
            java.util.function.Consumer<String> nextCb = pendingVolumeCallback;
            pendingVolumeValue = null;
            pendingMuteValue = null;
            pendingVolumeCallback = null;
            volumeSetInProgress = false;
            setVolume(nextVal, nextCb);
        } else if (pendingMuteValue != null) {
            boolean nextMute = pendingMuteValue;
            java.util.function.Consumer<String> nextCb = pendingVolumeCallback;
            pendingVolumeValue = null;
            pendingMuteValue = null;
            pendingVolumeCallback = null;
            volumeSetInProgress = false;
            setMuted(nextMute, nextCb);
        } else {
            volumeSetInProgress = false;
        }
    }

    public static void shutdownNowNoBlock() {
        System.out.println("[TSE_QUICK_SETTINGS] shutdownNowNoBlock called.");
        if (servicesStarted) {
            clockService.terminate();
            batteryService.terminate();
            volumeService.terminate();
            networkService.terminate();
            brightnessService.terminate();
        }
        controller.shutdown();
    }
}
