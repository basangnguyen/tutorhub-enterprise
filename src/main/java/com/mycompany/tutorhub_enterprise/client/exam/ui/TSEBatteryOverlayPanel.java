package com.mycompany.tutorhub_enterprise.client.exam.ui;

import javax.swing.*;
import java.awt.*;

public class TSEBatteryOverlayPanel {

    private static TSEAnchoredTrayPopup currentPopup = null;

    public static void showBatteryPanelDOM(Component trigger, TSEBrowserPanel browserPanel) {
        if (trigger == null || browserPanel == null) return;
        Point triggerLoc = SwingUtilities.convertPoint(trigger.getParent(), trigger.getLocation(), browserPanel);
        
        System.out.println("[TSE_TRAY_DOM] Anchor x=" + triggerLoc.x + ", y=" + triggerLoc.y);
        
        TSEBatteryStatusProvider.BatteryStatus batStatus = TSEBatteryStatusProvider.getStatus();
        
        String statusText = batStatus.hasBattery ? (batStatus.isCharging ? "Đang sạc" : "Đang dùng pin") : "Không phát hiện pin";
        
        String payload = String.format("{anchorX: %d, anchorY: %d, title: 'Trạng thái Pin', percent: %d, charging: %b, hasBattery: %b, statusText: '%s'}",
            triggerLoc.x + trigger.getWidth() / 2, triggerLoc.y, batStatus.percent, batStatus.isCharging, batStatus.hasBattery, statusText);
            
        browserPanel.executeJavaScript("if (window.TSETrayFlyout) { window.TSETrayFlyout.showBattery(" + payload + "); } else { console.log('[TSE_TRAY_DOM] Cannot show flyout: JCEF/JS bridge not ready.'); }");
    }

    public static void showBatteryPanel(Component trigger, JLayeredPane layeredPane) {
        if (currentPopup != null) {
            currentPopup.hidePopup();
            currentPopup = null;
        }

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout(0, 10));
        contentPanel.setOpaque(false);
        contentPanel.setPreferredSize(new Dimension(240, 100));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Trạng thái Pin");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        contentPanel.add(header, BorderLayout.NORTH);

        // Content
        JPanel infoContainer = new JPanel();
        infoContainer.setLayout(new BoxLayout(infoContainer, BoxLayout.Y_AXIS));
        infoContainer.setOpaque(false);
        infoContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        TSEBatteryStatusProvider.BatteryStatus batStatus = TSEBatteryStatusProvider.getStatus();

        if (batStatus.hasBattery) {
            JLabel pct = new JLabel("Pin: " + batStatus.percent + "%");
            pct.setFont(new Font("Segoe UI", Font.BOLD, 22));
            pct.setForeground(Color.WHITE);
            
            JLabel status = new JLabel(batStatus.isCharging ? "Đang sạc" : "Đang dùng pin");
            status.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            status.setForeground(new Color(180, 180, 180));
            
            infoContainer.add(pct);
            infoContainer.add(Box.createVerticalStrut(4));
            infoContainer.add(status);
        } else {
            JLabel pct = new JLabel("Nguồn điện: AC");
            pct.setFont(new Font("Segoe UI", Font.BOLD, 20));
            pct.setForeground(Color.WHITE);
            
            JLabel status = new JLabel("Không phát hiện pin");
            status.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            status.setForeground(new Color(180, 180, 180));
            
            infoContainer.add(pct);
            infoContainer.add(Box.createVerticalStrut(4));
            infoContainer.add(status);
        }

        contentPanel.add(infoContainer, BorderLayout.CENTER);

        TSEAnchoredTrayPopup popup = new TSEAnchoredTrayPopup(contentPanel, layeredPane);
        currentPopup = popup;
        popup.showPopup(trigger);
    }
}
