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
            new Thread(() -> {
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
            }).start();
        }
    }
    
    public static void refreshWifi(TSEBrowserPanel browserPanel) {
        if (browserPanel == null) return;
        
        isScanning = true;
        String payload = buildPayload(lastAnchorX, lastAnchorY, null);
        sendPayloadToJS(browserPanel, payload);
        
        new Thread(() -> {
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
        }).start();
    }

    private static String buildPayload(int anchorX, int anchorY, List<TSENetworkStatusProvider.WifiNetwork> networks) {
        TSEBatteryStatusProvider.BatteryStatus batStatus = TSEBatteryStatusProvider.getStatus();
        String statusText = batStatus.hasBattery ? (batStatus.isCharging ? "Đang sạc" : "Đang dùng pin") : "Không phát hiện pin";
        
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

        return String.format("{anchorX: %d, anchorY: %d, hasBattery: %b, percent: %d, statusText: '%s', wifiLoading: %b, currentSsid: '%s', networks: %s}",
            anchorX, anchorY, batStatus.hasBattery, batStatus.percent, escapeJsString(statusText), wifiLoading, escapeJsString(currentSsid), nArr.toString());
    }

    private static void sendPayloadToJS(TSEBrowserPanel browserPanel, String jsonPayload) {
        try {
            String jsCode = "if (window.TSETrayFlyout) { window.TSETrayFlyout.showQuickSettings(" + jsonPayload + "); }";
            browserPanel.executeJavaScript(jsCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static String escapeJsString(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'").replace("\n", "\\n").replace("\r", "");
    }
}
