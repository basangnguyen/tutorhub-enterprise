package com.mycompany.tutorhub_enterprise.client.exam.ui;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Point;
import java.util.List;

public class TSEQuickSettingsManager {
    private static boolean isScanning = false;
    private static long lastScanTime = 0;
    private static List<TSENetworkStatusProvider.WifiNetwork> cachedNetworks = null;
    private static int lastAnchorX = 0;
    private static int lastAnchorY = 0;

    public static void showQuickSettingsDOM(JComponent anchorComponent, TSEBrowserPanel browserPanel) {
        if (anchorComponent == null || browserPanel == null) return;
        
        Point pt = SwingUtilities.convertPoint(anchorComponent, 0, 0, browserPanel);
        lastAnchorX = pt.x + anchorComponent.getWidth() / 2;
        lastAnchorY = pt.y;

        boolean shouldScan = cachedNetworks == null || (System.currentTimeMillis() - lastScanTime > 30000);
        
        String payload = buildPayload(lastAnchorX, lastAnchorY, shouldScan ? null : cachedNetworks);
        sendPayloadToJS(browserPanel, payload);

        if (shouldScan && !isScanning) {
            isScanning = true;
            Thread t = new Thread(() -> {
                try {
                    List<TSENetworkStatusProvider.WifiNetwork> networks = TSENetworkStatusProvider.scanNetworks();
                    cachedNetworks = networks;
                    lastScanTime = System.currentTimeMillis();
                    
                    SwingUtilities.invokeLater(() -> {
                        String updatePayload = buildPayload(lastAnchorX, lastAnchorY, networks);
                        sendPayloadToJS(browserPanel, updatePayload);
                        isScanning = false;
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    isScanning = false;
                }
            }, "TSE-WifiScanner");
            t.setDaemon(true);
            t.start();
        }
    }
    
    public static void refreshWifi(TSEBrowserPanel browserPanel) {
        if (browserPanel == null) return;
        
        isScanning = true;
        String payload = buildPayload(lastAnchorX, lastAnchorY, null);
        sendPayloadToJS(browserPanel, payload);
        
        Thread t = new Thread(() -> {
            try {
                List<TSENetworkStatusProvider.WifiNetwork> networks = TSENetworkStatusProvider.scanNetworks();
                cachedNetworks = networks;
                lastScanTime = System.currentTimeMillis();
                
                SwingUtilities.invokeLater(() -> {
                    String updatePayload = buildPayload(lastAnchorX, lastAnchorY, networks);
                    sendPayloadToJS(browserPanel, updatePayload);
                    isScanning = false;
                });
            } catch (Exception e) {
                e.printStackTrace();
                isScanning = false;
            }
        }, "TSE-WifiRefresher");
        t.setDaemon(true);
        t.start();
    }

    private static final TSEBrightnessController brightnessController = new TSEBrightnessController();

    private static String buildPayload(int anchorX, int anchorY, List<TSENetworkStatusProvider.WifiNetwork> networks) {
        TSEBatteryStatusProvider.BatteryStatus batStatus = TSEBatteryStatusProvider.getStatus();
        String statusText = batStatus.hasBattery ? (batStatus.isCharging ? "Đang sạc" : "Đang dùng pin") : "Không phát hiện pin";
        
        TSEBrightnessStatus brightStatus = brightnessController.getStatus();
        
        String currentSsid = "Không kết nối";
        StringBuilder nArr = new StringBuilder("[");
        boolean wifiLoading = (networks == null);
        
        if (networks != null) {
            for (int i = 0; i < networks.size(); i++) {
                TSENetworkStatusProvider.WifiNetwork n = networks.get(i);
                if (n.isConnected) currentSsid = n.ssid;
                else nArr.append("\"").append(escapeJsString(n.ssid)).append("\"").append(i < networks.size()-1 ? "," : "");
            }
            if (nArr.toString().endsWith(",")) nArr.setLength(nArr.length() - 1);
        }
        nArr.append("]");

        return String.format("{anchorX: %d, anchorY: %d, hasBattery: %b, percent: %d, statusText: '%s', wifiLoading: %b, currentSsid: '%s', networks: %s, brightnessSupported: %b, brightnessPercent: %d}",
            anchorX, anchorY, batStatus.hasBattery, batStatus.percent, escapeJsString(statusText), wifiLoading, escapeJsString(currentSsid), nArr.toString(), brightStatus.supported, brightStatus.percent);
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

    public static synchronized void setBrightness(int percent, java.util.function.Consumer<String> jsCallback) {
        if (brightnessSetInProgress) {
            System.out.println("[TSE_BRIGHTNESS] Set in progress, queued latest value: " + percent);
            pendingBrightnessValue = percent;
            pendingCallback = jsCallback;
            return;
        }

        brightnessSetInProgress = true;
        
        Thread t = new Thread(() -> {
            processBrightnessSet(percent, jsCallback);
        }, "TSE-BrightnessSetter");
        t.setDaemon(true);
        t.start();
    }

    private static void processBrightnessSet(int percent, java.util.function.Consumer<String> jsCallback) {
        TSEBrightnessStatus status = brightnessController.setBrightness(percent);
        
        if (status.supported && status.writable && !"ERROR".equals(status.method) && !"TIMEOUT".equals(status.method)) {
            if (jsCallback != null) jsCallback.accept("SUCCESS");
        } else {
            if (jsCallback != null) jsCallback.accept("ERROR");
        }

        synchronized (TSEQuickSettingsManager.class) {
            if (pendingBrightnessValue != null) {
                int nextVal = pendingBrightnessValue;
                java.util.function.Consumer<String> nextCb = pendingCallback;
                pendingBrightnessValue = null;
                pendingCallback = null;
                System.out.println("[TSE_BRIGHTNESS] Processing queued brightness value: " + nextVal);
                Thread t = new Thread(() -> {
                    processBrightnessSet(nextVal, nextCb);
                }, "TSE-BrightnessQueueProcessor");
                t.setDaemon(true);
                t.start();
            } else {
                brightnessSetInProgress = false;
            }
        }
    }

    public static void shutdownNowNoBlock() {
        brightnessController.shutdownNowNoBlock();
    }

    private static String escapeJsString(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'").replace("\n", "\\n").replace("\r", "");
    }
}
