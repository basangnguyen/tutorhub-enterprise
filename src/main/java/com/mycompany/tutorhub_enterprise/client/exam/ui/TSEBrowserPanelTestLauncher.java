package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * TSEBrowserPanelTestLauncher – Launcher test độc lập cho TSEBrowserPanel.
 * Đảm bảo JCEF khởi tạo, render HTML và dọn dẹp sạch sẽ.
 */
public class TSEBrowserPanelTestLauncher {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
                UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
            } catch (Exception ignored) {}

            JFrame frame = new JFrame("TSE Browser Panel Test");
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);

            TSEBrowserPanel browserPanel = new TSEBrowserPanel();
            frame.add(browserPanel, BorderLayout.CENTER);

            // Test với HTML đơn giản
            String testHtml = "<html><body style='font-family: sans-serif; padding: 20px;'>" +
                    "<h1 style='color: #1D4ED8;'>TSE Browser Test</h1>" +
                    "<p>JCEF đã khởi tạo thành công và đang render nội dung này độc lập.</p>" +
                    "</body></html>";
            
            browserPanel.loadHtml(testHtml);

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    // Dọn dẹp an toàn khi đóng cửa sổ test
                    browserPanel.cleanup();
                    TSEJcefLifecycleManager.cleanup();
                    frame.dispose();
                }
            });

            frame.setVisible(true);
        });
    }
}
