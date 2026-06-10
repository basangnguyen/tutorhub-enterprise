package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Timer;
import java.util.TimerTask;

public class TSEJCEFDesktopSandboxTest {
    public static void main(String[] args) {
        System.out.println("[TSE_SANDBOX] Java sandbox started");
        
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
            } catch (Exception ignored) {}

            JFrame frame = new JFrame("JCEF Secure Desktop Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);

            // Initialize JCEF with explicit simple HTML
            System.out.println("[TSE_SANDBOX] JCEF initialized");
            TSEBrowserPanel browserPanel = new TSEBrowserPanel();
            browserPanel.loadHtml(
                "<html><body style='font-family: Arial; padding: 50px; text-align: center;'>" +
                "<h1>JCEF Secure Desktop Test</h1>" +
                "<p>If you see this, JCEF successfully rendered on the Alternate Desktop!</p>" +
                "<button onclick=\"alert('Button clicked! Mouse and UI are working.')\" style='padding: 10px 20px; font-size: 16px;'>Click Test</button>" +
                "</body></html>"
            );

            frame.add(browserPanel, BorderLayout.CENTER);

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    System.out.println("[TSE_SANDBOX] Frame disposed. Cleaning up JCEF...");
                    TSEJcefLifecycleManager.cleanup();
                    System.out.println("[TSE_SANDBOX] cleanup completed");
                    System.exit(0);
                }
            });

            frame.setVisible(true);
            System.out.println("[TSE_SANDBOX] HTML loaded and JFrame visible");

            // Auto close after 15 seconds to ensure we don't leak process
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    System.out.println("[TSE_SANDBOX] 15 seconds timeout reached, auto-closing.");
                    SwingUtilities.invokeLater(frame::dispose);
                }
            }, 15000);
        });
    }
}
