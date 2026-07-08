package com.mycompany.tutorhub_enterprise.client.drive;

import com.mycompany.tutorhub_enterprise.client.JcefManager;
import com.mycompany.tutorhub_enterprise.models.DriveFileModel;
import org.cef.browser.CefBrowser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class DrivePreviewDialog extends JDialog {

    private CefBrowser browser;
    
    // Các định dạng Google Docs hỗ trợ đọc online
    private static final List<String> OFFICE_EXTENSIONS = Arrays.asList(
            "doc", "docx", "xls", "xlsx", "ppt", "pptx"
    );

    public DrivePreviewDialog(Window owner, DriveFileModel file, Consumer<DriveFileModel> downloadAction) {
        super(owner, "Xem trước - " + file.getName(), ModalityType.APPLICATION_MODAL);
        
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 220)); // Nền đen mờ toàn màn hình
        setLayout(new BorderLayout());

        // Header chứa thông tin file và nút điều khiển
        JPanel topBar = createTopBar(file, downloadAction);
        
        // Vùng trung tâm: Render bằng JCEF
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(10, 50, 40, 50));
        
        String url = file.getFileUrl();
        if (url == null || url.isEmpty()) {
            showError(centerPanel, "Tệp không hợp lệ hoặc chưa được tải lên hoàn tất.");
        } else {
            boolean isHttp = url.startsWith("http://") || url.startsWith("https://");
            if (!isHttp) {
                try {
                    url = new java.io.File(url).toURI().toString();
                } catch (Exception e) {}
            }
            
            String ext = file.getFileType() != null ? file.getFileType().toLowerCase() : "";
            
            // Nếu là Office Document và file public -> Dùng Google Docs Viewer
            if (OFFICE_EXTENSIONS.contains(ext)) {
                if (isHttp) {
                    try {
                        String finalUrl = url;
                        // Nếu file lưu trên S3/B2, tạo Presigned URL để mở khóa bucket Private cho Microsoft
                        if ("B2_AND_LOCAL".equalsIgnoreCase(file.getSourceLocation()) || "MINIO".equalsIgnoreCase(file.getSourceLocation())) {
                            String presignedUrl = com.mycompany.tutorhub_enterprise.server.CloudStorageService.getInstance().generatePresignedUrl(url, 15);
                            if (presignedUrl != null) {
                                finalUrl = presignedUrl;
                            }
                        }
                        // Thủ thuật: Microsoft Office Web Viewer yêu cầu URL phải KẾT THÚC bằng đuôi file (.xlsx, .docx...)
                        // Các Presigned URL của S3 thường kết thúc bằng chữ ký (?X-Amz-Signature=...).
                        // Ta thêm fragment "#/file.ext" vào cuối (trước khi URL encode) để đánh lừa bộ kiểm tra của Microsoft.
                        // Fragment này sẽ không được gửi tới máy chủ S3 nên không làm hỏng chữ ký.
                        finalUrl = finalUrl + "#/file." + ext;
                        url = "https://view.officeapps.live.com/op/embed.aspx?src=" + URLEncoder.encode(finalUrl, "UTF-8");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    showError(centerPanel, "Định dạng Office (" + ext + ") lưu cục bộ hiện chưa hỗ trợ xem offline.");
                    url = null;
                }
            }
            
            if (url != null) {
                try {
                    browser = JcefManager.getClient().createBrowser(url, false, false);
                    centerPanel.add(browser.getUIComponent(), BorderLayout.CENTER);
                } catch (Exception ex) {
                    showError(centerPanel, "Lỗi khởi tạo trình duyệt Chromium: " + ex.getMessage());
                }
            }
        }
        
        add(topBar, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        
        setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
    }
    
    private JPanel createTopBar(DriveFileModel file, Consumer<DriveFileModel> downloadAction) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(20, 50, 10, 50));
        
        // Trái: Tên file
        JLabel lblTitle = new JLabel(file.getName());
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);
        
        // Phải: Các nút hành động
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        actionPanel.setOpaque(false);
        
        JButton btnDownload = createHeaderButton("↓ Tải xuống", new Color(37, 99, 235), new Color(59, 130, 246));
        btnDownload.addActionListener(e -> {
            if (downloadAction != null) downloadAction.accept(file);
        });
        
        JButton btnExit = createHeaderButton("Thoát (Esc)", new Color(220, 38, 38), new Color(239, 68, 68));
        btnExit.addActionListener(e -> closeDialog());
        
        actionPanel.add(btnDownload);
        actionPanel.add(btnExit);
        
        bar.add(lblTitle, BorderLayout.WEST);
        bar.add(actionPanel, BorderLayout.EAST);
        
        return bar;
    }
    
    private JButton createHeaderButton(String text, Color baseColor, Color hoverColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(baseColor.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(hoverColor);
                } else {
                    g2.setColor(baseColor);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(new EmptyBorder(8, 20, 8, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
    
    private void showError(JPanel panel, String message) {
        JLabel err = new JLabel(message);
        err.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        err.setForeground(Color.WHITE);
        err.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(err, BorderLayout.CENTER);
    }
    
    private void closeDialog() {
        if (browser != null) {
            // Mở một trang trống để ngắt media đang chạy
            browser.loadURL("about:blank");
        }
        dispose();
    }
    
    // Bắt phím ESC để thoát
    @Override
    protected JRootPane createRootPane() {
        JRootPane rootPane = super.createRootPane();
        KeyStroke stroke = KeyStroke.getKeyStroke("ESCAPE");
        rootPane.registerKeyboardAction(e -> closeDialog(), stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        return rootPane;
    }
}
