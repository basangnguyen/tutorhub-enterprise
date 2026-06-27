package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

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
    private final TSEQuickSettingsTrayCluster cluster;
    private Timer statusTimer;

    private QuickSettingsStateStore stateStore;
    private ClockService clockService;
    private BatteryService batteryService;
    private NetworkService networkService;
    private TSESecurityPolicy currentPolicy;
    private JLabel lblClock;

    public ExamFooterStatusBar(Runnable onExit) {
        this("VIE", null, null, onExit);
    }

    public ExamFooterStatusBar(String languageLabel, 
            Consumer<JButton> onLanguageClicked, 
            BiConsumer<String, JComponent> onQuickSettingsClicked,
            Runnable onExitRequest) {
        setLayout(new MigLayout("insets 4 16, fillx, aligny center", "push[right]", "[30!]"));
        setBackground(BAR_BG);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_TOP));

        JPanel icons = new JPanel(new MigLayout("insets 0, gap 4, aligny center"));
        icons.setOpaque(false);

        btnLanguage = dimTextButton(languageLabel);
        btnLanguage.setPreferredSize(new Dimension(46, 28));
        btnLanguage.setMinimumSize(new Dimension(46, 28));
        btnLanguage.setMaximumSize(new Dimension(46, 28));
        btnLanguage.setHorizontalAlignment(SwingConstants.CENTER);
        btnLanguage.addActionListener(e -> {
            if (onLanguageClicked != null) {
                onLanguageClicked.accept(btnLanguage);
            }
        });
        icons.add(btnLanguage, "w 46!, h 28!");

        // The unified Tray Cluster
        cluster = new TSEQuickSettingsTrayCluster(comp -> {
            if (onQuickSettingsClicked != null) {
                onQuickSettingsClicked.accept("cluster", comp);
            }
        });
        icons.add(cluster, "h 28!");

        // Clock initialization
        currentPolicy = TSESecurityPolicy.forLogin(); // default policy
        stateStore = new QuickSettingsStateStore(currentPolicy);
        clockService = new ClockService(stateStore);
        batteryService = new BatteryService(stateStore);
        networkService = new NetworkService(stateStore);
        
        lblClock = new JLabel("--:--");
        lblClock.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblClock.setForeground(ICON_COLOR);
        lblClock.setToolTipText("Loading date...");
        
        stateStore.addListener(snapshot -> {
            SwingUtilities.invokeLater(() -> {
                if (currentPolicy.getClockMode() == TSESecurityPolicy.ClockMode.SHOW) {
                    lblClock.setText(snapshot.clockTime);
                    lblClock.setToolTipText(snapshot.clockDate);
                    lblClock.setVisible(true);
                } else {
                    lblClock.setVisible(false);
                }
                
                String tooltip = snapshot.hasBattery ? (snapshot.batteryCharging ? "Đang sạc (" + snapshot.batteryPercent + "%)" : snapshot.batteryPercent + "%") : "Không phát hiện pin";
                cluster.updateBattery(snapshot.hasBattery, snapshot.batteryCharging, snapshot.batteryPercent, tooltip);

                boolean hasWifi = !"NO_ADAPTER".equals(snapshot.wifiStatus) && !"DISABLED".equals(snapshot.wifiStatus) && !"ERROR".equals(snapshot.wifiStatus);
                boolean isWifiConnected = "CONNECTED".equals(snapshot.wifiStatus);
                String wifiTooltip = isWifiConnected ? "Đã kết nối: " + snapshot.wifiSsid + " (" + snapshot.wifiSignal + "%)" :
                        "DISABLED".equals(snapshot.wifiStatus) ? "WiFi bị vô hiệu hóa" :
                        "NO_ADAPTER".equals(snapshot.wifiStatus) ? "Không tìm thấy card WiFi" : "Đã ngắt kết nối WiFi";
                if (snapshot.wifiError != null) {
                    wifiTooltip = snapshot.wifiError;
                }
                cluster.updateWifi(hasWifi, isWifiConnected, wifiTooltip);
            });
        });
        
        clockService.initialize();
        batteryService.initialize();
        networkService.initialize();
        
        icons.add(lblClock, "gapleft 8, gapright 8, h 28!");

        // Power button
        btnPower = new JButton(ExamLoginMockPanel.loadSVG("images/exam/icons/power.svg", 16, POWER_RED));
        setupHoverEffect(btnPower);
        btnPower.setContentAreaFilled(false);
        btnPower.setBorderPainted(false);
        btnPower.setFocusPainted(false);
        btnPower.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPower.setPreferredSize(new Dimension(36, 28));
        btnPower.setMinimumSize(new Dimension(36, 28));
        btnPower.setMaximumSize(new Dimension(36, 28));
        btnPower.addActionListener(e -> {
            if (onExitRequest == null) {
                return;
            }
            onExitRequest.run();
        });
        icons.add(btnPower, "w 36!, h 28!");

        add(icons, "cell 0 0");
        
        System.out.println("[TSE_FOOTER] QuickSettings click handler attached=" + (onQuickSettingsClicked != null));
        
        startStatusPolling();
    }

    private void startStatusPolling() {
        updateStatus(); // Initial update
        statusTimer = new Timer(2000, e -> updateStatus()); // Every 2 seconds for faster response
        statusTimer.start();
    }

    private void updateStatus() {

        // Run volume check
        new SwingWorker<TSEVolumeStatus, Void>() {
            @Override
            protected TSEVolumeStatus doInBackground() {
                return TSEVolumeController.getStatus();
            }
            @Override
            protected void done() {
                try {
                    TSEVolumeStatus volStatus = get();
                    String tooltip = volStatus.supported ? (volStatus.muted ? "Muted" : "Volume: " + volStatus.percent + "%") : "No Audio Device";
                    cluster.updateVolume(volStatus.supported, volStatus.muted, volStatus.percent, tooltip);
                } catch (Exception e) {}
            }
        }.execute();
    }

    public void stopStatusPolling() {
        if (statusTimer != null) {
            statusTimer.stop();
            System.out.println("[TSE_PARENT_FOOTER] Status polling stopped.");
        }
        if (clockService != null) {
            clockService.terminate();
            clockService = null;
        }
        if (batteryService != null) {
            batteryService.terminate();
            batteryService = null;
        }
        if (networkService != null) {
            networkService.terminate();
            networkService = null;
        }
        if (stateStore != null) {
            stateStore.shutdown();
            stateStore = null;
        }
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
        JButton button = new JButton(text) {
            private boolean hovered = false;
            private boolean active = false;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(java.awt.event.MouseEvent e) { hovered = false; repaint(); }
                    @Override public void mousePressed(java.awt.event.MouseEvent e) { active = true; repaint(); }
                    @Override public void mouseReleased(java.awt.event.MouseEvent e) { active = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (active) {
                    g2.setColor(new Color(255, 255, 255, 50));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                } else if (hovered) {
                    g2.setColor(new Color(255, 255, 255, 25));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(ICON_COLOR);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        button.setIcon(null);
        return button;
    }

    private JButton createIconButton(String svgPath, int size) {
        JButton btn = new JButton(new FlatSVGIcon(svgPath, size, size)) {
            private boolean hovered = false;
            private boolean active = false;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(java.awt.event.MouseEvent e) { hovered = false; repaint(); }
                    @Override public void mousePressed(java.awt.event.MouseEvent e) { active = true; repaint(); }
                    @Override public void mouseReleased(java.awt.event.MouseEvent e) { active = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (active) {
                    g2.setColor(new Color(255, 255, 255, 50));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                } else if (hovered) {
                    g2.setColor(new Color(255, 255, 255, 25));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        return btn;
    }

    private void setupHoverEffect(JButton btn) {
        // Obsolete, replaced by inline listeners above
    }

    private JLabel dimSvgLabel(String svgPath) {
        JLabel lbl = new JLabel(ExamLoginMockPanel.loadSVG(svgPath, 16, ICON_COLOR));
        return lbl;
    }
}
