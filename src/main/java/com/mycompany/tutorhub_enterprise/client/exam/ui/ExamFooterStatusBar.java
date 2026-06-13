package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import java.awt.*;

/**
 * ExamFooterStatusBar – Thin bottom status bar for exam taking screen.
 *
 * Design:
 *  - Light translucent bar, ~32px tall
 *  - Push-right layout: VIE / wifi / volume / BatteryStatusIcon / power
 *  - BatteryStatusIcon drawn via Graphics2D (no static icon)
 *  - Power button calls onExit (dispose, never System.exit)
 *
 * The bar is pure Swing, sits outside JCEF.
 */
public class ExamFooterStatusBar extends JPanel {

    private static final Color BAR_BG     = new Color(228, 231, 235); // 10% darker gray, fully opaque
    private static final Color BORDER_TOP = new Color(0, 0, 0, 12);
    private static final Color ICON_COLOR = Color.decode("#1E3A5F");
    private static final Color POWER_RED  = Color.decode("#C62828");

    private final JButton btnLanguage;
    private final JButton btnPower;
    private final JButton btnWifi;
    private final BatteryStatusIcon battery;
    private Timer statusTimer;

    public ExamFooterStatusBar(Runnable onExit) {
        this("VIE", null, null, onExit);
    }

    public ExamFooterStatusBar(String languageLabel, 
            java.util.function.Consumer<javax.swing.JButton> onLanguageClicked, 
            java.util.function.Consumer<javax.swing.JComponent> onQuickSettingsClicked,
            Runnable onExitRequest) {
        setLayout(new MigLayout("insets 4 16, fillx, aligny center", "push[right]", "[24!]"));
        setBackground(BAR_BG);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_TOP));

        JPanel icons = new JPanel(new MigLayout("insets 0, gap 14, aligny center"));
        icons.setOpaque(false);

        // Language label
        btnLanguage = dimTextButton(languageLabel);
        btnLanguage.addActionListener(e -> {
            if (onLanguageClicked != null) {
                onLanguageClicked.accept(btnLanguage);
            }
        });
        icons.add(btnLanguage);

        // Wifi icon button
        btnWifi = new JButton(new FlatSVGIcon("images/exam/icons/wifi.svg", 16, 16));
        btnWifi.setContentAreaFilled(false);
        btnWifi.setBorderPainted(false);
        btnWifi.setFocusPainted(false);
        btnWifi.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnWifi.setMargin(new Insets(0, 0, 0, 0));
        btnWifi.addActionListener(e -> {
            System.out.println("[TSE_CONTROL] WiFi click.");
            if (onQuickSettingsClicked != null) {
                onQuickSettingsClicked.accept(btnWifi);
            }
        });
        icons.add(btnWifi);

        // Volume icon
        JButton btnVolume = new JButton(new FlatSVGIcon("images/exam/icons/volume-2.svg", 16, 16));
        btnVolume.setContentAreaFilled(false);
        btnVolume.setBorderPainted(false);
        btnVolume.setFocusPainted(false);
        btnVolume.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolume.setMargin(new Insets(0, 0, 0, 0));
        btnVolume.addActionListener(e -> {
            System.out.println("[TSE_CONTROL] Volume click.");
            if (onQuickSettingsClicked != null) {
                onQuickSettingsClicked.accept(btnVolume);
            }
        });
        icons.add(btnVolume);

        // Battery – custom Graphics2D component
        battery = new BatteryStatusIcon();
        battery.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        battery.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                System.out.println("[TSE_CONTROL] Battery click.");
                if (onQuickSettingsClicked != null) {
                    onQuickSettingsClicked.accept(battery);
                }
            }
        });
        icons.add(battery, "w 28!, h 14!, aligny center");

        // Power button
        btnPower = new JButton(ExamLoginMockPanel.loadSVG("images/exam/icons/power.svg", 16, POWER_RED));
        btnPower.setContentAreaFilled(false);
        btnPower.setBorderPainted(false);
        btnPower.setFocusPainted(false);
        btnPower.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPower.addActionListener(e -> {
            if (onExitRequest == null) {
                return;
            }
            onExitRequest.run();
        });
        icons.add(btnPower);

        add(icons, "cell 0 0");
        
        System.out.println("[TSE_FOOTER] QuickSettings click handler attached=" + (onQuickSettingsClicked != null));
        
        startStatusPolling();
    }

    private void startStatusPolling() {
        updateStatus(); // Initial update
        statusTimer = new Timer(20000, e -> updateStatus()); // Every 20 seconds
        statusTimer.start();
    }

    private void updateStatus() {
        // Run network check in a background thread to prevent UI freezing
        new SwingWorker<TSENetworkStatusProvider.NetworkStatus, Void>() {
            @Override
            protected TSENetworkStatusProvider.NetworkStatus doInBackground() {
                return TSENetworkStatusProvider.getStatus();
            }

            @Override
            protected void done() {
                try {
                    TSENetworkStatusProvider.NetworkStatus netStatus = get();
                    if (!netStatus.hasWifiAdapter) {
                        btnWifi.setIcon(new FlatSVGIcon("images/exam/icons/wifi-off.svg", 16, 16));
                    } else if (!netStatus.isConnected) {
                        btnWifi.setIcon(new FlatSVGIcon("images/exam/icons/wifi-off.svg", 16, 16));
                    } else {
                        btnWifi.setIcon(new FlatSVGIcon("images/exam/icons/wifi.svg", 16, 16));
                    }
                    btnWifi.setToolTipText(netStatus.tooltip);
                } catch (Exception e) {}
            }
        }.execute();

        // Run battery check in background
        new SwingWorker<TSEBatteryStatusProvider.BatteryStatus, Void>() {
            @Override
            protected TSEBatteryStatusProvider.BatteryStatus doInBackground() {
                return TSEBatteryStatusProvider.getStatus();
            }

            @Override
            protected void done() {
                try {
                    TSEBatteryStatusProvider.BatteryStatus batStatus = get();
                    if (batStatus.hasBattery) {
                        battery.setVisible(true);
                        battery.setBatteryPercent(batStatus.percent);
                        battery.setCharging(batStatus.isCharging);
                        battery.setToolTipText(batStatus.tooltip);
                        battery.repaint();
                    } else {
                        // AC Only or Unknown
                        battery.setVisible(true);
                        battery.setBatteryPercent(100);
                        battery.setCharging(true);
                        battery.setToolTipText(batStatus.tooltip);
                        battery.repaint();
                    }
                } catch (Exception e) {}
            }
        }.execute();
    }

    public void applyLanguage(TSELanguageManager languageManager) {
        setLanguageLabel(languageManager.getFooterLabel());
        btnLanguage.setToolTipText(languageManager.text("language.tooltip"));
        btnPower.setToolTipText(languageManager.text("power.tooltip"));
    }

    public void applyInputMode(TSEInputModeManager inputModeManager, TSELanguageManager languageManager) {
        setLanguageLabel(inputModeManager.getFooterLabel());
        btnLanguage.setToolTipText(languageManager.text("language.tooltip"));
        btnPower.setToolTipText(languageManager.text("power.tooltip"));
    }

    public void setLanguageLabel(String text) {
        btnLanguage.setText(text);
        btnLanguage.revalidate();
        btnLanguage.repaint();
    }

    private JButton dimTextButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(ICON_COLOR);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(0, 0, 0, 0));
        return button;
    }

    private JLabel dimSvgLabel(String svgPath) {
        JLabel lbl = new JLabel(ExamLoginMockPanel.loadSVG(svgPath, 16, ICON_COLOR));
        return lbl;
    }
}
