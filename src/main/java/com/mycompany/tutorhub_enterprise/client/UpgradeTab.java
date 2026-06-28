package com.mycompany.tutorhub_enterprise.client;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * UpgradeTab — màn hình Nâng cấp tài khoản TutorHub Enterprise
 *
 * Kiến trúc: Swing JPanel → JFXPanel → JavaFX WebView → upgrade.html
 * Bridge JS→Java: window.TutorHubUpgrade (selectPlan, goBack, toggleBilling)
 *
 * Logic gốc được giữ nguyên:
 *  - setOnBackListener(Runnable) → nút Back gọi listener này
 *  - selectPlan → mở UpgradeDialog(parentFrame, planName, amount)
 */
public class UpgradeTab extends JPanel {

    private Runnable onBackListener;
    private WebEngine webEngine;

    // Strong reference bắt buộc — GC sẽ thu hồi bridge nếu không lưu field
    private JavaBridge javaBridge;

    public UpgradeTab() {
        setLayout(new BorderLayout());
        setBackground(Color.decode("#F8FAFC"));
        initJavaFXPanel();
    }

    // ──────────────────────────────────────────────────────────────
    //  Khởi tạo
    // ──────────────────────────────────────────────────────────────

    private void initJavaFXPanel() {
        try {
            // JFXPanel phải tạo trên EDT — constructor chạy đúng thread
            JFXPanel jfxPanel = new JFXPanel();
            jfxPanel.setBackground(Color.decode("#F8FAFC"));
            add(jfxPanel, BorderLayout.CENTER);

            // Tiếp tục trên JavaFX Application Thread
            Platform.runLater(() -> setupWebView(jfxPanel));

        } catch (ExceptionInInitializerError | NoClassDefFoundError | Exception ex) {
            // Fallback an toàn nếu JavaFX không có trong classpath
            add(buildFallbackPanel(), BorderLayout.CENTER);
        }
    }

    private void setupWebView(JFXPanel jfxPanel) {
        try {
            WebView webView = new WebView();
            webView.setZoom(1.0);
            webEngine = webView.getEngine();

            // Tắt JS alert để tránh block UI
            webEngine.setOnAlert(event -> {});

            Scene scene = new Scene(webView);
            scene.setFill(javafx.scene.paint.Color.web("#F8FAFC"));
            jfxPanel.setScene(scene);

            // Copy resource từ JAR → temp dir, rồi load bằng file:// URL
            Path tempDir = copyResourcesToTemp();
            if (tempDir != null) {
                String htmlUrl = tempDir.resolve("upgrade.html").toUri().toString();
                webEngine.load(htmlUrl);

                // Inject bridge sau khi page load xong
                webEngine.getLoadWorker().stateProperty().addListener(
                    (obs, oldState, newState) -> {
                        if (newState == Worker.State.SUCCEEDED) {
                            injectJavaBridge();
                        }
                    }
                );
            }
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> {
                removeAll();
                add(buildFallbackPanel(), BorderLayout.CENTER);
                revalidate();
                repaint();
            });
        }
    }

    /**
     * Copy upgrade.html / upgrade.css / upgrade.js từ JAR classpath
     * ra một thư mục temp. Cần thiết vì WebView không resolve relative
     * paths trong jar:// URLs đúng cách.
     */
    private Path copyResourcesToTemp() {
        try {
            Path tempDir = Files.createTempDirectory("tutorhub-upgrade-");
            tempDir.toFile().deleteOnExit();

            for (String resName : new String[]{"upgrade.html", "upgrade.css", "upgrade.js"}) {
                InputStream stream = getClass().getResourceAsStream("/upgrade-web/" + resName);
                if (stream != null) {
                    Path dest = tempDir.resolve(resName);
                    Files.copy(stream, dest, StandardCopyOption.REPLACE_EXISTING);
                    dest.toFile().deleteOnExit();
                    stream.close();
                }
            }
            return tempDir;
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * Expose JavaBridge vào window.TutorHubUpgrade trong JavaScript.
     * Phải chạy trên JavaFX Application Thread.
     */
    private void injectJavaBridge() {
        try {
            javaBridge = new JavaBridge(); // lưu strong ref — không được để local var
            JSObject window = (JSObject) webEngine.executeScript("window");
            window.setMember("TutorHubUpgrade", javaBridge);
            // Báo JS bridge đã ready (xử lý race condition load)
            webEngine.executeScript(
                "if (typeof window._onBridgeReady === 'function') window._onBridgeReady();"
            );
        } catch (Exception ex) {
            // Non-critical — UI vẫn hiển thị, chỉ nút CTA không gọi được Java
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Java Bridge — được gọi từ JavaScript qua window.TutorHubUpgrade
    // ──────────────────────────────────────────────────────────────

    public class JavaBridge {

        /**
         * Gọi khi user click nút nâng cấp gói.
         * Mở UpgradeDialog — giữ nguyên logic gốc.
         *
         * @param planName "Basic" | "Premium" | "VIP"
         * @param amount   Số tiền VND (ví dụ: 129000 cho gói tháng, 1238400 cho gói năm)
         */
        public void selectPlan(String planName, double amount) {
            SwingUtilities.invokeLater(() -> {
                if (amount > 0) {
                    Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(UpgradeTab.this);
                    UpgradeDialog upgradeModal = new UpgradeDialog(parentFrame, planName, amount);
                    upgradeModal.setVisible(true);
                }
            });
        }

        /**
         * Gọi khi user click nút Back trên WebView.
         * Kích hoạt onBackListener đã đăng ký từ bên ngoài.
         */
        public void goBack() {
            SwingUtilities.invokeLater(() -> {
                if (onBackListener != null) onBackListener.run();
            });
        }

        /**
         * Gọi khi user chuyển billing toggle.
         * Dự phòng — hook cho backend billing cycle sau này.
         *
         * @param billingType "monthly" | "yearly"
         */
        public void toggleBilling(String billingType) {
            // Reserved for backend integration
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Fallback nếu JavaFX không available
    // ──────────────────────────────────────────────────────────────

    private JPanel buildFallbackPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.decode("#F8FAFC"));

        JLabel label = new JLabel(
            "<html><div style='text-align:center;padding:40px'>"
            + "<p style='font-size:18px;font-weight:bold;color:#111827;'>"
            + "Nâng cấp tài khoản</p>"
            + "<p style='color:#667085;font-size:13px;'>"
            + "JavaFX WebView không khả dụng.<br>"
            + "Vui lòng liên hệ hỗ trợ TutorHub để được tư vấn gói phù hợp.</p>"
            + "</div></html>",
            SwingConstants.CENTER
        );
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panel.add(label, BorderLayout.CENTER);

        JButton btnBack = new JButton("← Quay lại");
        btnBack.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnBack.addActionListener(e -> { if (onBackListener != null) onBackListener.run(); });

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setOpaque(false);
        south.setBorder(new EmptyBorder(0, 0, 24, 0));
        south.add(btnBack);
        panel.add(south, BorderLayout.SOUTH);

        return panel;
    }

    // ──────────────────────────────────────────────────────────────
    //  Public API
    // ──────────────────────────────────────────────────────────────

    /** Đăng ký callback được gọi khi user click nút Back. */
    public void setOnBackListener(Runnable listener) {
        this.onBackListener = listener;
    }
}
