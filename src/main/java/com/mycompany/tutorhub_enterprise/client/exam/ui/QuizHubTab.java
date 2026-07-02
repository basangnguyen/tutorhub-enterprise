package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.mycompany.tutorhub_enterprise.client.quizhub.bridge.QuizHubBridge;
import com.mycompany.tutorhub_enterprise.client.quizhub.bridge.QuizHubCefRouterHandler;
import com.mycompany.tutorhub_enterprise.client.quizhub.service.QuizHubAttemptService;
import com.mycompany.tutorhub_enterprise.client.quizhub.service.QuizHubDeckService;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefMessageRouterHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URL;

public class QuizHubTab extends JPanel {

    private CefBrowser browser;

    public QuizHubTab() {
        System.out.println("[QUIZHUB] QuizHubTab initialized");
        setLayout(new BorderLayout());

        initJcefBrowser();
    }

    private void initJcefBrowser() {
        try {
            URL url = getClass().getResource("/tse/quiz.html");
            if (url == null) {
                System.err.println("[QUIZHUB][ERROR] Cannot find /tse/quiz.html");
                add(new JLabel("Lỗi: Không tìm thấy file /tse/quiz.html", SwingConstants.CENTER), BorderLayout.CENTER);
                return;
            }

            System.out.println("[QUIZHUB] Initializing JCEF Browser for quiz.html");
            
            // JcefManager from existing architecture
            browser = com.mycompany.tutorhub_enterprise.client.JcefManager.getClient().createBrowser(url.toExternalForm(), false, false);

            // Register Bridge
            CefMessageRouter.CefMessageRouterConfig config = new CefMessageRouter.CefMessageRouterConfig("cefQuery", "cefQueryCancel");
            CefMessageRouter router = CefMessageRouter.create(config);
            
            QuizHubBridge bridge = new QuizHubBridge(new QuizHubDeckService(), new QuizHubAttemptService());
            CefMessageRouterHandler handler = new QuizHubCefRouterHandler(bridge);
            router.addHandler(handler, true);
            
            com.mycompany.tutorhub_enterprise.client.JcefManager.getClient().addMessageRouter(router);

            add(browser.getUIComponent(), BorderLayout.CENTER);
            System.out.println("[QUIZHUB] Loaded quiz.html in JCEF");

        } catch (Exception e) {
            System.err.println("[QUIZHUB][ERROR] Failed to load JCEF webview: " + e.getMessage());
            add(new JLabel("Lỗi: " + e.getMessage(), SwingConstants.CENTER), BorderLayout.CENTER);
        }
    }
}
