package com.mycompany.tutorhub_enterprise.client.exam.ui;

import org.cef.browser.CefBrowser;
import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * TSEBrowserPanel – Component JCEF độc lập để nhúng vào Center Layout của TSEExamShellPanel.
 */
public class TSEBrowserPanel extends JPanel {

    private CefBrowser browser;

    public TSEBrowserPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
    }

    /**
     * Tải URL vào JCEF Browser.
     * Khởi tạo browser nếu chưa có.
     */
    public void loadUrl(String url) {
        if (browser == null) {
            browser = TSEJcefLifecycleManager.getClient().createBrowser(url, false, false);
            add(browser.getUIComponent(), BorderLayout.CENTER);
            revalidate();
            repaint();
        } else {
            browser.loadURL(url);
        }
    }

    /**
     * Tải mã HTML tĩnh để test (Lưu ra file tạm rổi tải URL file).
     */
    public void loadHtml(String html) {
        try {
            File tempHtml = File.createTempFile("tse_test_", ".html");
            tempHtml.deleteOnExit();
            java.nio.file.Files.write(tempHtml.toPath(), html.getBytes("UTF-8"));
            loadUrl(tempHtml.toURI().toString());
        } catch (Exception e) {
            e.printStackTrace();
            add(new JLabel("Lỗi tải HTML test!", SwingConstants.CENTER), BorderLayout.CENTER);
            revalidate();
            repaint();
        }
    }

    public void executeJavaScript(String script) {
        if (browser != null) {
            browser.executeJavaScript(script, browser.getURL(), 0);
        }
    }

    /**
     * Cleanup tài nguyên Browser Component này.
     * Lưu ý: Không dispose toàn bộ CefApp ở đây. CefApp.dispose() chỉ được gọi khi tắt App.
     */
    public void cleanup() {
        if (browser != null) {
            browser.close(true);
            browser = null;
        }
    }
}
