package com.mycompany.tutorhub_enterprise.client.exam.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TSENetworkStatusProvider {

    public static class NetworkStatus {
        public boolean hasWifiAdapter;
        public boolean isConnected;
        public String ssid;
        public int signalPercent;
        public String tooltip;
    }

    public static class WifiNetwork {
        public String ssid;
        public int signal;
        public String auth;
        public boolean isConnected;
    }

    public static NetworkStatus getStatus() {
        NetworkStatus status = new NetworkStatus();
        status.hasWifiAdapter = false;
        status.isConnected = false;
        status.ssid = "";
        status.signalPercent = 0;
        status.tooltip = "Không có kết nối mạng";

        try {
            ProcessBuilder pb = new ProcessBuilder("netsh", "wlan", "show", "interfaces");
            pb.redirectErrorStream(true);
            Process p = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String lower = line.toLowerCase();
                    if (lower.contains("there is no wireless interface") || lower.contains("wireless autoconfig service") || lower.contains("not running")) {
                        status.hasWifiAdapter = false;
                        status.tooltip = "Không tìm thấy card WiFi";
                        return status;
                    }
                    
                    if (line.trim().startsWith("State") || line.trim().startsWith("Trạng thái")) {
                        status.hasWifiAdapter = true; // Since we got a state
                        if (line.contains("connected") || line.contains("đã kết nối")) {
                            status.isConnected = true;
                        }
                    }
                    if (line.trim().startsWith("SSID")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) {
                            String val = parts[1].trim();
                            if (!val.isEmpty()) {
                                status.ssid = val;
                            }
                        }
                    }
                    if (line.trim().startsWith("Signal") || line.trim().startsWith("Tín hiệu")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) {
                            String val = parts[1].trim().replace("%", "");
                            try {
                                status.signalPercent = Integer.parseInt(val);
                            } catch (Exception e) {}
                        }
                    }
                }
            }
            p.waitFor();

            if (status.hasWifiAdapter) {
                if (status.isConnected && !status.ssid.isEmpty()) {
                    status.tooltip = "Đã kết nối: " + status.ssid + " (" + status.signalPercent + "%)";
                } else {
                    status.tooltip = "Đã ngắt kết nối WiFi";
                }
            }
        } catch (Exception e) {
            System.err.println("[TSE_NETWORK] Error reading wifi status: " + e.getMessage());
            status.tooltip = "Lỗi đọc trạng thái mạng";
        }
        return status;
    }

    public static List<WifiNetwork> scanNetworks() {
        List<WifiNetwork> networks = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("netsh", "wlan", "show", "networks", "mode=bssid");
            pb.redirectErrorStream(true);
            Process p = pb.start();

            WifiNetwork currentNetwork = null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("SSID")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) {
                            String ssid = parts[1].trim();
                            if (!ssid.isEmpty()) {
                                currentNetwork = new WifiNetwork();
                                currentNetwork.ssid = ssid;
                                currentNetwork.signal = 0;
                                networks.add(currentNetwork);
                            }
                        }
                    } else if (currentNetwork != null) {
                        if (line.trim().startsWith("Authentication") || line.trim().startsWith("Xác thực")) {
                            String[] parts = line.split(":", 2);
                            if (parts.length > 1) {
                                currentNetwork.auth = parts[1].trim();
                            }
                        } else if (line.trim().startsWith("Signal") || line.trim().startsWith("Tín hiệu")) {
                            String[] parts = line.split(":", 2);
                            if (parts.length > 1) {
                                String val = parts[1].trim().replace("%", "");
                                try {
                                    int sig = Integer.parseInt(val);
                                    if (sig > currentNetwork.signal) {
                                        currentNetwork.signal = sig; // Keep highest signal BSSID
                                    }
                                } catch (Exception e) {}
                            }
                        }
                    }
                }
            }
            p.waitFor();
            
            // Mark the currently connected one
            NetworkStatus status = getStatus();
            if (status.isConnected && status.ssid != null) {
                for (WifiNetwork net : networks) {
                    if (status.ssid.equals(net.ssid)) {
                        net.isConnected = true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[TSE_NETWORK] Error scanning networks: " + e.getMessage());
        }
        return networks;
    }
}
