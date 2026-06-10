package com.mycompany.tutorhub_enterprise.client;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapPickerDialog extends JDialog {

    private String selectedAddress = "";
    private JLabel lblAddress;
    private JButton btnConfirm;
    private boolean confirmed = false;

    public MapPickerDialog(Dialog owner) {
        super(owner, "Chọn Vị Trí", true);
        setUndecorated(true);
        setSize(800, 600);
        setLocationRelativeTo(owner);
        setBackground(new Color(0, 0, 0, 0));

        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.dispose();
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(16, 24, 16, 24));
        JLabel title = new JLabel("Chọn vị trí");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.decode("#111827"));
        header.add(title, BorderLayout.WEST);

        JButton close = new JButton("") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(243, 244, 246));
                    g2.fillOval(0, 0, getWidth(), getHeight());
                }
                g2.setColor(Color.decode("#9CA3AF"));
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(10, 10, 20, 20);
                g2.drawLine(20, 10, 10, 20);
                g2.dispose();
            }
        };
        close.setContentAreaFilled(false);
        close.setBorderPainted(false);
        close.setFocusPainted(false);
        close.setOpaque(false);
        close.setPreferredSize(new Dimension(30, 30));
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addActionListener(e -> dispose());
        header.add(close, BorderLayout.EAST);
        mainPanel.add(header, BorderLayout.NORTH);

        // Map View (JavaFX)
        JFXPanel jfxPanel = new JFXPanel();
        mainPanel.add(jfxPanel, BorderLayout.CENTER);

        Platform.runLater(() -> {
            WebView webView = new WebView();
            WebEngine webEngine = webView.getEngine();
            // Load Google Maps API
            String html = "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
                    + "<style>body { margin: 0; padding: 0; } #map { width: 100vw; height: 100vh; }</style>"
                    + "<link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />"
                    + "<script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>"
                    + "</head><body><div id=\"map\"></div><script>"
                    + "var map, marker;"
                    + "function initMap() {"
                    + "  var initialPos = [10.8231, 106.6297];"
                    + "  map = L.map('map').setView(initialPos, 13);"
                    + "  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: '© OpenStreetMap' }).addTo(map);"
                    + "  marker = L.marker(initialPos).addTo(map);"
                    + "  map.on('click', function(e) {"
                    + "    marker.setLatLng(e.latlng);"
                    + "    javaApp.onMapClick(e.latlng.lat, e.latlng.lng);"
                    + "  });"
                    + "}"
                    + "window.onload = initMap;"
                    + "</script></body></html>";
            
            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    JSObject window = (JSObject) webEngine.executeScript("window");
                    window.setMember("javaApp", new JavaBridge());
                }
            });
            webEngine.loadContent(html);
            jfxPanel.setScene(new Scene(webView));
        });

        // Footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(16, 24, 16, 24));
        
        lblAddress = new JLabel("Chưa chọn vị trí");
        lblAddress.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblAddress.setForeground(Color.decode("#6B7280"));
        footer.add(lblAddress, BorderLayout.CENTER);

        btnConfirm = new JButton("Xác nhận");
        btnConfirm.setBackground(Color.decode("#8B5CF6"));
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setPreferredSize(new Dimension(120, 36));
        btnConfirm.setEnabled(false);
        btnConfirm.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        footer.add(btnConfirm, BorderLayout.EAST);

        mainPanel.add(footer, BorderLayout.SOUTH);
        setContentPane(mainPanel);
    }

    public boolean isConfirmed() { return confirmed; }
    public String getSelectedAddress() { return selectedAddress; }

    public class JavaBridge {
        public void onMapClick(double lat, double lng) {
            new Thread(() -> {
                try {
                    String urlStr = "https://nominatim.openstreetmap.org/reverse?format=json&lat=" + lat + "&lon=" + lng;
                    HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                    conn.setRequestProperty("User-Agent", "TutorHub/1.0");
                    Scanner sc = new Scanner(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder resp = new StringBuilder();
                    while(sc.hasNext()) resp.append(sc.nextLine());
                    sc.close();
                    
                    Matcher m = Pattern.compile("\"display_name\"\\s*:\\s*\"([^\"]+)\"").matcher(resp.toString());
                    if (m.find()) {
                        String addr = m.group(1);
                        SwingUtilities.invokeLater(() -> {
                            selectedAddress = addr;
                            lblAddress.setText(addr);
                            lblAddress.setForeground(Color.decode("#111827"));
                            btnConfirm.setEnabled(true);
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
