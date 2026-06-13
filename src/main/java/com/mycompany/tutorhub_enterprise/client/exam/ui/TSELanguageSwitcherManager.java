package com.mycompany.tutorhub_enterprise.client.exam.ui;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Point;

public class TSELanguageSwitcherManager {

    public static void showLanguageSwitcherDOM(JComponent anchorComponent, TSEBrowserPanel browserPanel, String activeMode) {
        if (anchorComponent == null || browserPanel == null) return;
        
        Point pt = SwingUtilities.convertPoint(anchorComponent, 0, 0, browserPanel);
        int anchorX = pt.x + anchorComponent.getWidth() / 2;
        int anchorY = pt.y;

        String json = String.format("{anchorX: %d, anchorY: %d, activeMode: '%s'}", anchorX, anchorY, escapeJsString(activeMode));

        try {
            String jsCode = "if (window.TSETrayFlyout) { window.TSETrayFlyout.showLanguageSwitcher(" + json + "); }";
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
