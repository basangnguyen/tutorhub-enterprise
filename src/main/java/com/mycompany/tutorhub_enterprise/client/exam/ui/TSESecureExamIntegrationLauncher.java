package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.mycompany.tutorhub_enterprise.client.NetworkManager;
import com.mycompany.tutorhub_enterprise.client.exam.services.NetworkTSEExamService;
import com.mycompany.tutorhub_enterprise.client.exam.services.SecureNetworkTSEExamService;
import com.mycompany.tutorhub_enterprise.client.exam.services.TSEExamService;

import javax.swing.*;
import java.awt.*;

/**
 * TSESecureExamIntegrationLauncher – Launcher tích hợp TSE Exam với Rust Lockdown (Soft Lock Mode).
 * Flow:
 * Kết nối Server -> Login -> Lấy Config -> Start (LOCK qua IPC) -> Load JCEF -> Submit -> UNLOCK -> Đóng app.
 */
public class TSESecureExamIntegrationLauncher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
                UIManager.put("defaultFont",      new Font("Segoe UI", Font.PLAIN, 13));
                UIManager.put("Button.arc",        8);
                UIManager.put("Component.arc",     8);
                UIManager.put("TextComponent.arc", 6);
            } catch (Exception ignored) {}

            System.out.println("[TSESecureLauncher] Đang kết nối tới server (localhost:7860)...");
            
            new Thread(() -> {
                try {
                    NetworkManager.getInstance().connect("localhost", 7860);
                    System.out.println("[TSESecureLauncher] Đã kết nối server thành công.");
                    
                    SwingUtilities.invokeLater(() -> {
                        // Khởi tạo Network service chuẩn
                        TSEExamService baseService = new NetworkTSEExamService();
                        // Wrap bởi Secure Service (Lockdown)
                        SecureNetworkTSEExamService secureService = new SecureNetworkTSEExamService(baseService);
                        
                        TSEExamShellFrame frame = new TSEExamShellFrame(secureService);
                        
                        frame.addWindowListener(new java.awt.event.WindowAdapter() {
                            @Override
                            public void windowClosed(java.awt.event.WindowEvent e) {
                                System.out.println("[TSESecureLauncher] Frame closed. Triggering Secure Cleanup...");
                                secureService.cleanupOnClose();
                                
                                System.out.println("[TSESecureLauncher] Cleaning up JCEF...");
                                TSEJcefLifecycleManager.cleanup();
                                
                                // Ngắt kết nối NetworkManager
                                NetworkManager.getInstance().disconnect();
                                System.out.println("[TSESecureLauncher] Ngắt kết nối mạng thành công.");
                            }
                        });
                        
                        frame.setVisible(true);
                    });
                } catch (Exception e) {
                    System.err.println("[TSESecureLauncher] Không thể kết nối tới server: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();
        });
    }
}
