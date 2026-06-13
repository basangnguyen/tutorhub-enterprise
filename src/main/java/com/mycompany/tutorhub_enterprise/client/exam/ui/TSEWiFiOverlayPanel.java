package com.mycompany.tutorhub_enterprise.client.exam.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class TSEWiFiOverlayPanel {

    private static TSEAnchoredTrayPopup currentPopup = null;
    private static long lastScanTime = 0;

    public static void showWifiPanelDOM(Component trigger, TSEBrowserPanel browserPanel) {
        if (trigger == null || browserPanel == null) return;
        Point loc = trigger.getLocationOnScreen();
        // Since browserPanel is full screen center, screen coords are fine, or we can use SwingUtilities.convertPoint
        // Wait, better to send the bounds relative to screen or relative to the window.
        // Actually, JCEF coordinates usually match screen coordinates if the frame is undecorated,
        // or they match frame client area. Let's convert to browserPanel coordinates.
        Point triggerLoc = SwingUtilities.convertPoint(trigger.getParent(), trigger.getLocation(), browserPanel);
        
        System.out.println("[TSE_TRAY_DOM] Anchor x=" + triggerLoc.x + ", y=" + triggerLoc.y);
        
        new SwingWorker<List<TSENetworkStatusProvider.WifiNetwork>, Void>() {
            @Override
            protected List<TSENetworkStatusProvider.WifiNetwork> doInBackground() {
                return TSENetworkStatusProvider.scanNetworks();
            }

            @Override
            protected void done() {
                try {
                    List<TSENetworkStatusProvider.WifiNetwork> networks = get();
                    String currentSsid = "Không kết nối";
                    StringBuilder nArr = new StringBuilder("[");
                    for (int i = 0; i < networks.size(); i++) {
                        TSENetworkStatusProvider.WifiNetwork n = networks.get(i);
                        if (n.isConnected) currentSsid = n.ssid;
                        else nArr.append("\"").append(escapeJsString(n.ssid)).append("\"").append(i < networks.size()-1 ? "," : "");
                    }
                    if (nArr.toString().endsWith(",")) nArr.setLength(nArr.length() - 1);
                    nArr.append("]");
                    
                    String payload = String.format("{anchorX: %d, anchorY: %d, title: 'Mạng WiFi', currentSsid: '%s', networks: %s, readonly: true}",
                        triggerLoc.x + trigger.getWidth() / 2, triggerLoc.y, escapeJsString(currentSsid), nArr.toString());
                        
                    browserPanel.executeJavaScript("if (window.TSETrayFlyout) { window.TSETrayFlyout.showWifi(" + payload + "); } else { console.log('[TSE_TRAY_DOM] Cannot show flyout: JCEF/JS bridge not ready.'); }");
                } catch (Exception e) {
                    System.err.println("[TSE_WIFI] Render failed: " + e.getMessage());
                }
            }
        }.execute();
    }
    
    private static String escapeJsString(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'").replace("\n", "\\n").replace("\r", "");
    }

    public static void showWifiPanel(Component trigger, JLayeredPane layeredPane) {
        if (currentPopup != null) {
            currentPopup.hidePopup();
            currentPopup = null;
        }

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout(0, 10));
        contentPanel.setOpaque(false);
        contentPanel.setPreferredSize(new Dimension(320, 360));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Mạng WiFi");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.setBackground(new Color(60, 60, 60));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        btnRefresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.add(btnRefresh, BorderLayout.EAST);

        contentPanel.add(header, BorderLayout.NORTH);

        // List Container
        JPanel listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setOpaque(false);

        JLabel loading = new JLabel("Đang tìm mạng WiFi...");
        loading.setForeground(new Color(150, 150, 150));
        loading.setAlignmentX(Component.CENTER_ALIGNMENT);
        listContainer.add(Box.createVerticalStrut(20));
        listContainer.add(loading);

        JScrollPane scrollPane = new JScrollPane(listContainer);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        TSEAnchoredTrayPopup popup = new TSEAnchoredTrayPopup(contentPanel, layeredPane);
        currentPopup = popup;

        btnRefresh.addActionListener(e -> {
            long now = System.currentTimeMillis();
            if (now - lastScanTime < 3000) {
                System.out.println("[TSE_WIFI] Scan ignored: debounce active.");
                return;
            }
            System.out.println("[TSE_WIFI] Scan requested.");
            listContainer.removeAll();
            listContainer.add(Box.createVerticalStrut(20));
            listContainer.add(loading);
            listContainer.revalidate();
            listContainer.repaint();
            scanAndRender(listContainer);
        });

        popup.showPopup(trigger);
        scanAndRender(listContainer);
    }

    private static void scanAndRender(JPanel listContainer) {
        lastScanTime = System.currentTimeMillis();
        new SwingWorker<List<TSENetworkStatusProvider.WifiNetwork>, Void>() {
            @Override
            protected List<TSENetworkStatusProvider.WifiNetwork> doInBackground() throws Exception {
                return TSENetworkStatusProvider.scanNetworks();
            }

            @Override
            protected void done() {
                try {
                    List<TSENetworkStatusProvider.WifiNetwork> networks = get();
                    listContainer.removeAll();

                    if (networks.isEmpty()) {
                        JLabel empty = new JLabel("Không tìm thấy mạng WiFi nào.");
                        empty.setForeground(new Color(150, 150, 150));
                        empty.setAlignmentX(Component.CENTER_ALIGNMENT);
                        listContainer.add(Box.createVerticalStrut(20));
                        listContainer.add(empty);
                    } else {
                        for (TSENetworkStatusProvider.WifiNetwork net : networks) {
                            listContainer.add(createNetworkItem(net));
                            listContainer.add(Box.createVerticalStrut(4));
                        }
                    }
                    listContainer.revalidate();
                    listContainer.repaint();
                } catch (Exception e) {
                    System.err.println("[TSE_WIFI] UI Render failed: " + e.getMessage());
                }
            }
        }.execute();
    }

    private static JPanel createNetworkItem(TSENetworkStatusProvider.WifiNetwork net) {
        JPanel item = new JPanel(new BorderLayout(10, 0));
        item.setOpaque(true);
        item.setBackground(net.isConnected ? new Color(50, 80, 120) : new Color(40, 40, 40));
        item.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        item.setMaximumSize(new Dimension(300, 60));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Signal icon (mock)
        JPanel signalIcon = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        signalIcon.setOpaque(false);
        Color iconColor = net.signal > 66 ? new Color(34, 197, 94) : (net.signal > 33 ? new Color(234, 179, 8) : new Color(148, 163, 184));
        
        for (int i = 1; i <= 4; i++) {
            JPanel bar = new JPanel();
            bar.setPreferredSize(new Dimension(4, i * 4 + 2));
            bar.setBackground(net.signal >= (i * 25 - 15) ? iconColor : new Color(80, 80, 80));
            signalIcon.add(bar);
        }
        // Wrapper to bottom align the bars
        JPanel signalWrapper = new JPanel(new GridBagLayout());
        signalWrapper.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.SOUTH;
        signalWrapper.add(signalIcon, gbc);
        item.add(signalWrapper, BorderLayout.WEST);

        // Texts
        JPanel texts = new JPanel(new GridLayout(2, 1));
        texts.setOpaque(false);
        
        JLabel ssidLabel = new JLabel(net.ssid + (net.isConnected ? " (Đang kết nối)" : ""));
        ssidLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        ssidLabel.setForeground(Color.WHITE);
        texts.add(ssidLabel);
        
        JLabel authLabel = new JLabel(net.auth);
        authLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        authLabel.setForeground(new Color(180, 180, 180));
        texts.add(authLabel);
        
        item.add(texts, BorderLayout.CENTER);

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Cannot use JOptionPane per user request. We just do a sysout or show an internal msg.
                System.out.println("[TSE_WIFI] Read-only mode: Cannot connect to " + net.ssid);
                authLabel.setText("Chế độ chỉ xem, không thể kết nối");
                authLabel.setForeground(new Color(250, 100, 100));
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!net.isConnected) {
                    item.setBackground(new Color(60, 60, 60));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!net.isConnected) {
                    item.setBackground(new Color(40, 40, 40));
                }
            }
        });

        return item;
    }
}
