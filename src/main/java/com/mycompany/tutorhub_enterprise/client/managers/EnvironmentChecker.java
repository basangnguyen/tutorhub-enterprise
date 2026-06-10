package com.mycompany.tutorhub_enterprise.client.managers;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EnvironmentChecker {

    public boolean checkNetworkConnectivity(String endpoint) {
        try {
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            return (200 <= responseCode && responseCode <= 399);
        } catch (IOException e) {
            System.err.println("Network check failed: " + e.getMessage());
            return false;
        }
    }

    public int checkDisplayCount() {
        try {
            return GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length;
        } catch (Exception e) {
            System.err.println("Error getting display count: " + e.getMessage());
            return 1; // Fallback
        }
    }

    public boolean checkWebcamAvailable(boolean requireWebcam) {
        if (!requireWebcam) {
            return true;
        }
        // In pure Java without external libraries (like OpenCV or WebcamCapture API), 
        // accurately checking webcam is not natively supported.
        // We will do a generic stub or assume true and let the actual webcam module handle it later.
        System.out.println("checkWebcamAvailable: Note: True hardware detection requires external library.");
        return true; 
    }

    public List<String> checkRunningProcesses(List<String> bannedProcessNames) {
        List<String> foundBanned = new ArrayList<>();
        // Java 9+ approach
        ProcessHandle.allProcesses().forEach(process -> {
            Optional<String> commandOptional = process.info().command();
            if (commandOptional.isPresent()) {
                String cmd = commandOptional.get().toLowerCase();
                for (String banned : bannedProcessNames) {
                    if (cmd.contains(banned.toLowerCase())) {
                        foundBanned.add(cmd);
                    }
                }
            }
        });
        return foundBanned;
    }

    public boolean checkForVirtualMachine() {
        // Safe check in Java: Check MAC address, System properties, or hardware model
        // Since we are not calling Rust yet, this is a basic stub/check.
        boolean isVm = false;
        
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface ni = interfaces.nextElement();
                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length >= 3) {
                    String prefix = String.format("%02X:%02X:%02X", mac[0], mac[1], mac[2]).toUpperCase();
                    // VirtualBox and VMware MAC prefixes
                    if (prefix.equals("08:00:27") || prefix.equals("00:05:69") || 
                        prefix.equals("00:0C:29") || prefix.equals("00:1C:14") || 
                        prefix.equals("00:50:56")) {
                        isVm = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("VM MAC check error: " + e.getMessage());
        }
        
        return isVm;
    }
}
