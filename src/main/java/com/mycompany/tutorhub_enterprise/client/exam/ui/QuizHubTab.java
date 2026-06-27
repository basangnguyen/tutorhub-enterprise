package com.mycompany.tutorhub_enterprise.client.exam.ui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class QuizHubTab extends JPanel {

    private JFXPanel jfxPanel;

    public QuizHubTab() {
        System.out.println("[QUIZHUB] QuizHubTab initialized");
        setLayout(new BorderLayout());
        
        jfxPanel = new JFXPanel();
        add(jfxPanel, BorderLayout.CENTER);

        Platform.runLater(this::initWebView);
    }

    private void initWebView() {
        try {
            WebView webView = new WebView();
            jfxPanel.setScene(new Scene(webView));

            URL url = getClass().getResource("/tse/quiz.html");
            if (url == null) {
                System.err.println("[QUIZHUB][ERROR] Cannot find /tse/quiz.html");
                SwingUtilities.invokeLater(() -> {
                    removeAll();
                    add(new JLabel("Lỗi: Không tìm thấy file /tse/quiz.html", SwingConstants.CENTER), BorderLayout.CENTER);
                    revalidate();
                    repaint();
                });
                return;
            }

            System.out.println("[QUIZHUB] Loading /tse/quiz.html");
            webView.getEngine().load(url.toExternalForm());
            System.out.println("[QUIZHUB] Loaded quiz.html");

        } catch (Exception e) {
            System.err.println("[QUIZHUB][ERROR] Failed to load webview: " + e.getMessage());
            SwingUtilities.invokeLater(() -> {
                removeAll();
                add(new JLabel("Lỗi: " + e.getMessage(), SwingConstants.CENTER), BorderLayout.CENTER);
                revalidate();
                repaint();
            });
        }
    }
}
