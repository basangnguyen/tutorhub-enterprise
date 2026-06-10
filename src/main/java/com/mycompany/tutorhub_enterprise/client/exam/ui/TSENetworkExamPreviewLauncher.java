package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.mycompany.tutorhub_enterprise.client.NetworkManager;
import com.mycompany.tutorhub_enterprise.client.exam.services.NetworkTSEExamService;
import com.mycompany.tutorhub_enterprise.client.exam.services.TSEExamService;

import javax.swing.*;
import java.awt.*;

/**
 * TSENetworkExamPreviewLauncher – Launcher test E2E cho flow UI Phase 1 với Server Mock.
 * Flow:
 * Kết nối Server -> Login qua AUTH_LOGIN -> Lấy Config (TSE_GET_CONFIG_LIST) -> Start (EXAM_START_REQUEST)
 * -> Load JCEF -> Submit (EXAM_SUBMIT) -> Đóng app không treo.
 */
public class TSENetworkExamPreviewLauncher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
                UIManager.put("defaultFont",      new Font("Segoe UI", Font.PLAIN, 13));
                UIManager.put("Button.arc",        8);
                UIManager.put("Component.arc",     8);
                UIManager.put("TextComponent.arc", 6);
            } catch (Exception ignored) {}

            System.out.println("[TSENetworkLauncher] Đang kết nối tới server (localhost:7860)...");
            
            // Connect to server (ensure server is running locally)
            new Thread(() -> {
                try {
                    NetworkManager.getInstance().connect("localhost", 7860);
                    System.out.println("[TSENetworkLauncher] Đã kết nối server thành công.");
                    
                    SwingUtilities.invokeLater(() -> {
                        TSEExamService networkService = new NetworkTSEExamService();
                        TSEExamShellFrame frame = new TSEExamShellFrame(networkService);
                        
                        frame.addWindowListener(new java.awt.event.WindowAdapter() {
                            @Override
                            public void windowClosed(java.awt.event.WindowEvent e) {
                                System.out.println("[TSENetworkLauncher] Frame disposed. Cleaning up JCEF...");
                                TSEJcefLifecycleManager.cleanup();
                                System.out.println("[TSENetworkLauncher] JCEF cleanup complete.");
                                
                                // Ngắt kết nối NetworkManager
                                NetworkManager.getInstance().disconnect();
                                System.out.println("[TSENetworkLauncher] Ngắt kết nối mạng thành công.");
                            }
                        });
                        
                        frame.setVisible(true);
                    });
                } catch (Exception e) {
                    System.err.println("[TSENetworkLauncher] Không thể kết nối tới server: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();
        });
    }
}
