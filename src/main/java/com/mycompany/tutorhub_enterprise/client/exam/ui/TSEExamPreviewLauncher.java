package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;

import com.mycompany.tutorhub_enterprise.client.exam.services.*;

/**
 * TSEExamPreviewLauncher – Launcher test độc lập cho flow UI Phase 1.
 * Đóng vai trò adapter chạy dummy flow:
 * Login -> Config List -> Password Dialog -> Exam Shell -> Submit Dialog -> dispose()
 */
public class TSEExamPreviewLauncher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
                UIManager.put("defaultFont",      new Font("Segoe UI", Font.PLAIN, 13));
                UIManager.put("Button.arc",        8);
                UIManager.put("Component.arc",     8);
                UIManager.put("TextComponent.arc", 6);
            } catch (Exception ignored) {}

            TSEExamService mockService = new MockTSEExamService();
            TSEExamShellFrame frame = new TSEExamShellFrame(mockService);
            
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    System.out.println("[TSELauncher] Frame disposed. Cleaning up JCEF...");
                    TSEJcefLifecycleManager.cleanup();
                    System.out.println("[TSELauncher] JCEF cleanup complete.");
                }
            });
            
            frame.setVisible(true);
        });
    }
}
