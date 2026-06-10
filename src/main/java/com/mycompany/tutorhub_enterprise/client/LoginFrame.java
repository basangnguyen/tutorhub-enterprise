package com.mycompany.tutorhub_enterprise.client;

import com.formdev.flatlaf.FlatLightLaf;
import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.models.auth.AuthResponse;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.tutorhub_enterprise.utils.AutoUpdater;

public class LoginFrame extends JFrame {

    public static final String CURRENT_VERSION = "1.0.9";

    private final Color PRIMARY_GREEN = Color.decode("#10B981");
    private final Color FB_BLUE = Color.decode("#1877F2");
    private final Color TEXT_MAIN = Color.decode("#111827");
    private final Color TEXT_SUB = Color.decode("#6B7280");
    private final Color BORDER_COLOR = Color.decode("#E5E7EB");

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private RoundedButton btnLogin; 

    public LoginFrame() {
        setTitle("TutorHub Enterprise - Login");
        setSize(1000, 650); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(null); 

        // ==========================================================
        // THIẾT LẬP ICON CHO CỬA SỔ VÀ THANH TASKBAR
        // ==========================================================
        try {
            java.net.URL iconURL = getClass().getResource("/images/logomoi.png");
            if (iconURL != null) {
                Image appIcon = new ImageIcon(iconURL).getImage();
                this.setIconImage(appIcon);
            }
        } catch (Exception e) {
            System.err.println("Lỗi load icon cho Taskbar: " + e.getMessage());
        }

        // ==========================================================
        // TỰ ĐỘNG KIỂM TRA CẬP NHẬT
        // ==========================================================
        AutoUpdater.checkUpdate(CURRENT_VERSION, this);

        // ==========================================================
        // PANEL BÊN TRÁI (SLIDER ẢNH & THÔNG TIN)
        // ==========================================================
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(Color.decode("#F4F5F7")); 
        leftPanel.setBounds(0, 0, 650, 650);
        leftPanel.setLayout(null);

        JButton badgeBtn = new JButton("TutorHub Enterprise");
        // Đã nới rộng nút để chứa thêm icon và căn giữa lại
        badgeBtn.setBounds(225, 60, 200, 32); 
        badgeBtn.setBackground(Color.decode("#FFFFFF"));
        badgeBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        badgeBtn.setForeground(PRIMARY_GREEN);
        badgeBtn.putClientProperty("JButton.buttonType", "roundRect");
        // Thêm icon nhỏ vào trước nút Badge
        setLocalIcon(badgeBtn, "/images/logomoi.png", 16, 16); 
        badgeBtn.setIconTextGap(8); // Khoảng cách giữa icon và chữ
        badgeBtn.setEnabled(false); 
        leftPanel.add(badgeBtn);

        // Tạo hiệu ứng đổ bóng (Drop Shadow) cho tiêu đề thêm lôi cuốn và thanh lịch
        JLabel lblMainTitle = new JLabel("<html><b><font color='#10B981'>AI-Powered</font></b> Tutoring Platform</html>", SwingConstants.CENTER) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                // Vẽ lớp bóng mờ phía sau
                g2.translate(2, 2);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
                super.paint(g2);
                
                // Vẽ text thật sắc nét đè lên trên
                g2.translate(-2, -2);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                super.paint(g2);
                g2.dispose();
            }
        };
        lblMainTitle.setFont(new Font("Segoe UI", Font.BOLD, 30));
        lblMainTitle.setForeground(TEXT_MAIN);
        lblMainTitle.setBounds(0, 110, 650, 45);
        leftPanel.add(lblMainTitle);

        JLabel lblSubTitle = new JLabel("Manage classes, match tutors, and track schedules in one intelligent platform.", SwingConstants.CENTER);
        lblSubTitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lblSubTitle.setForeground(TEXT_SUB);
        lblSubTitle.setBounds(0, 155, 650, 30);
        leftPanel.add(lblSubTitle);

        // Gọi Slider có tích hợp sẵn nút bấm chuyển ảnh
        LoginImageSlider imageSlider = new LoginImageSlider();
        imageSlider.setBounds(35, 210, 580, 360);
        leftPanel.add(imageSlider);

        add(leftPanel);

        // ==========================================================
        // PANEL BÊN PHẢI (FORM ĐĂNG NHẬP)
        // ==========================================================
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setBounds(650, 0, 350, 650);
        rightPanel.setLayout(null);

        // Nới rộng panel chứa logo để phù hợp với kích thước logo mới
        JPanel brandPanel = new JPanel(null);
        brandPanel.setOpaque(false);
        brandPanel.setBounds(30, 45, 250, 55); 
        
        JLabel lblAppIcon = new JLabel();
        // Tăng kích thước logo từ 36 lên 48, hàm setLocalIcon của bạn đã bao gồm khử răng cưa rất sắc nét
        setLocalIcon(lblAppIcon, "/images/logomoi.png", 48, 48); 
        lblAppIcon.setBounds(0, 0, 48, 48);
        brandPanel.add(lblAppIcon);
        
        JLabel lblLogo = new JLabel("TutorHub");
        // Tăng font chữ logo cho cân đối với icon
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 26)); 
        lblLogo.setForeground(TEXT_MAIN);
        lblLogo.setBounds(58, 2, 150, 26);
        brandPanel.add(lblLogo);
        
        JLabel lblBrandSub = new JLabel("Tutoring Platform");
        lblBrandSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblBrandSub.setForeground(TEXT_SUB);
        lblBrandSub.setBounds(60, 30, 150, 16);
        brandPanel.add(lblBrandSub);
        
        rightPanel.add(brandPanel);

        JLabel lblSignUp = new JLabel("Sign Up");
        lblSignUp.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblSignUp.setForeground(PRIMARY_GREEN); 
        lblSignUp.setBounds(260, 60, 60, 30); 
        lblSignUp.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rightPanel.add(lblSignUp);
        lblSignUp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new SignUpFrame().setVisible(true); 
            }
        });

        txtUsername = new JTextField();
        txtUsername.setBounds(30, 140, 290, 48); 
        txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtUsername.putClientProperty("JTextField.placeholderText", "Enter email or username");
        rightPanel.add(txtUsername);

        txtPassword = new JPasswordField();
        txtPassword.setBounds(30, 205, 290, 48);
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPassword.putClientProperty("JTextField.placeholderText", "Enter the password");
        rightPanel.add(txtPassword);

        btnLogin = new RoundedButton("Log In", PRIMARY_GREEN, Color.WHITE, null, 48);
        btnLogin.setBounds(30, 280, 290, 48);
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 16));
        rightPanel.add(btnLogin);

        JCheckBox chkAutoLogin = new JCheckBox("Auto Login");
        chkAutoLogin.setBounds(30, 335, 100, 20);
        chkAutoLogin.setBackground(Color.WHITE);
        chkAutoLogin.setForeground(TEXT_SUB);
        chkAutoLogin.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        rightPanel.add(chkAutoLogin);

        JCheckBox chkRemember = new JCheckBox("Remember Password");
        chkRemember.setBounds(175, 335, 145, 20);
        chkRemember.setBackground(Color.WHITE);
        chkRemember.setForeground(TEXT_SUB);
        chkRemember.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        rightPanel.add(chkRemember);

        JLabel lblOr = new JLabel("------------------ Or ------------------", SwingConstants.CENTER);
        lblOr.setForeground(Color.decode("#D1D5DB"));
        lblOr.setBounds(30, 385, 290, 20);
        rightPanel.add(lblOr);

        RoundedButton btnSms = new RoundedButton("Log in with SMS", Color.decode("#ECFDF5"), PRIMARY_GREEN, BORDER_COLOR, 48);
        btnSms.setBounds(30, 420, 290, 44);
        btnSms.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSms.setIconTextGap(12);
        rightPanel.add(btnSms);

        RoundedButton btnFacebook = new RoundedButton("Log in with Facebook", Color.decode("#F0F2F5"), FB_BLUE, null, 44);
        setLocalIcon(btnFacebook, "/images/icon/facebook.svg", 22, 22); 
        btnFacebook.setBounds(30, 475, 290, 44);
        btnFacebook.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnFacebook.setIconTextGap(12); 
        rightPanel.add(btnFacebook);

        RoundedButton btnGoogle = new RoundedButton("Log in with Google", Color.WHITE, TEXT_MAIN, BORDER_COLOR, 44);
        setLocalIcon(btnGoogle, "/images/icon/google.svg", 22, 22);
        btnGoogle.setBounds(30, 530, 290, 44);
        btnGoogle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnGoogle.setIconTextGap(12);
        rightPanel.add(btnGoogle);

        // Sự kiện Quên mật khẩu
        JLabel lblForget = new JLabel("Forget Password", SwingConstants.CENTER);
        lblForget.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblForget.setForeground(TEXT_SUB);
        lblForget.setBounds(30, 585, 290, 20);
        lblForget.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rightPanel.add(lblForget);
        
        lblForget.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new ForgotPasswordDialog(LoginFrame.this).setVisible(true);
            }
        });

        add(rightPanel);

        // ==========================================================
        // XỬ LÝ SỰ KIỆN NÚT
        // ==========================================================
        btnLogin.addActionListener(e -> xuLyDangNhap());

        btnLogin.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnLogin.setBgColor(Color.decode("#059669")); } 
            public void mouseExited(MouseEvent e) { btnLogin.setBgColor(PRIMARY_GREEN); }
        });
        
        btnSms.addActionListener(e -> new SmsLoginDialog(LoginFrame.this).setVisible(true));
        btnFacebook.addActionListener(e -> showSocialLoginUnavailable("Facebook"));
        btnGoogle.addActionListener(e -> showSocialLoginUnavailable("Google"));
    }

    private void showSocialLoginUnavailable(String provider) {
        JOptionPane.showMessageDialog(
                this,
                provider + " login dang duoc nang cap sang OAuth that. Vui long dung email/password trong luc nay.",
                "Social login",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

   private void xuLyDangNhap() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tài khoản và mật khẩu!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnLogin.setText("Logging in...");

        new Thread(() -> {
            boolean isDbSuccess = false;
            String message = "Không thể kết nối đến Máy chủ!";
            int uid = 1; // ID mặc định nếu có lỗi tách

            try {
                AuthResponse res = new AuthClient().login(username, password);

                isDbSuccess = res.isSuccess();
                message = res.getMessage();
                
                // Tách lấy ID thực tế từ Server trả về (DASHBOARD_GO|id)
                String dashboardPayload = res.getDashboardPayload();
                if (isDbSuccess && dashboardPayload != null && dashboardPayload.contains("|")) {
                    try {
                        uid = Integer.parseInt(dashboardPayload.split("\\|")[1]);
                    } catch (Exception ignored) {}
                }
            } catch (Exception ex) {
                System.err.println("[SOCKET ERROR] " + ex.getMessage());
            }

            if (!isDbSuccess) {
                final String finalMsg = message;
                SwingUtilities.invokeLater(() -> {
                    btnLogin.setText("Log In");
                    JOptionPane.showMessageDialog(this, finalMsg, "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
                return;
            }

            final int finalUid = uid;
            SwingUtilities.invokeLater(() -> {
                btnLogin.setText("Log In");
                dispose(); 
                // Truyền ID thật và username vào Dashboard
                new MainDashboard(finalUid, username).setVisible(true); 
            });

        }).start();
    }

    void openDashboardFromAuth(AuthResponse res, String fallbackName) {
        int uid = 1;
        if (res != null && res.getDashboardPayload() != null && res.getDashboardPayload().contains("|")) {
            try {
                uid = Integer.parseInt(res.getDashboardPayload().split("\\|")[1]);
            } catch (Exception ignored) {}
        }

        dispose();
        new MainDashboard(uid, fallbackName).setVisible(true);
    }
    
    private void xuLyDangNhapOAuth(String name, String email) {
        btnLogin.setText("Đang xác thực OAuth...");
        new Thread(() -> {
            try {
                NetworkManager net = NetworkManager.getInstance();
                if (!net.isConnected()) net.connect("localhost", 8888);
                
                net.sendPacket(new Packet("LOGIN_OAUTH", email + "|" + name));
                Packet res = net.receivePacket();

                SwingUtilities.invokeLater(() -> {
                    btnLogin.setText("Log In");
                    if (res.success) {
                        dispose(); 
                        // Tạm cấp ID = 1 cho người dùng đăng nhập MXH
                        new MainDashboard(1, name).setVisible(true); 
                    } else {
                        JOptionPane.showMessageDialog(this, res.message, "Lỗi xác thực", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception ex) {
                // Trả khối catch về đúng chức năng thông báo lỗi mạng
                SwingUtilities.invokeLater(() -> {
                    btnLogin.setText("Log In");
                    JOptionPane.showMessageDialog(this, "Không thể kết nối Máy chủ!", "Lỗi mạng", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
    
    private void setLocalIcon(Component comp, String path, int width, int height) {
        try {
            if (path.endsWith(".svg")) {
                String svgPath = path.startsWith("/") ? path.substring(1) : path;
                com.formdev.flatlaf.extras.FlatSVGIcon icon = new com.formdev.flatlaf.extras.FlatSVGIcon(svgPath, width, height);
                if (comp instanceof JLabel) ((JLabel) comp).setIcon(icon);
                else if (comp instanceof JButton) ((JButton) comp).setIcon(icon);
                return;
            }
            URL url = getClass().getResource(path);
            if (url != null) {
                Image srcImg = new ImageIcon(url).getImage();
                java.awt.image.BufferedImage resizedImg = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = resizedImg.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.drawImage(srcImg, 0, 0, width, height, null);
                g2.dispose();
                
                ImageIcon scaled = new ImageIcon(resizedImg);
                if (comp instanceof JLabel) ((JLabel) comp).setIcon(scaled);
                else if (comp instanceof JButton) ((JButton) comp).setIcon(scaled);
            } else {
                System.err.println("Lỗi: Không tìm thấy icon tại " + path);
            }
        } catch (Exception e) {}
    }

    public static void main(String[] args) {
        try { 
            UIManager.put("TextComponent.arc", 18);
            UIManager.put("Component.focusWidth", 1); 
            UIManager.put("Component.innerFocusWidth", 0);
            UIManager.setLookAndFeel(new FlatLightLaf()); 
        } catch (Exception ex) {}
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }

    // ==========================================================
    // CLASS CUSTOM: VẼ NÚT BẤM BO TRÒN MỀM MẠI 100%
    // ==========================================================
    class RoundedButton extends JButton {
        private Color bgColor, borderColor;
        private int radius;

        public RoundedButton(String text, Color bgColor, Color fgColor, Color borderColor, int radius) {
            super(text);
            this.bgColor = bgColor; this.borderColor = borderColor; this.radius = radius;
            setForeground(fgColor); setContentAreaFilled(false); setFocusPainted(false);
            setBorderPainted(false); setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (bgColor != null) {
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            }
            g2.dispose();
            super.paintComponent(g); 
        }

        @Override
        protected void paintBorder(Graphics g) {
            if (borderColor != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(borderColor); g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, radius, radius);
                g2.dispose();
            }
        }

        public void setBgColor(Color color) { this.bgColor = color; repaint(); }
    }

    // ==========================================================
    // CLASS HỖ TRỢ VẼ SLIDE ẢNH Ở MÀN HÌNH ĐĂNG NHẬP
    // ==========================================================
    class LoginImageSlider extends JPanel {
        private List<Image> images = new ArrayList<>();
        private int currentIndex = 0, nextIndex = 1;
        private float alpha = 1.0f;
        private Timer slideTimer, fadeTimer;

        public LoginImageSlider() {
            setOpaque(false);
            setLayout(null); // Cho phép chèn nút bấm tự do đè lên ảnh
            
            for (int i = 1; i <= 3; i++) {
                try {
                    URL url = getClass().getResource("/images/slide_login/login" + i + ".png");
                    if (url != null) images.add(new ImageIcon(url).getImage());
                } catch (Exception e) {}
            }

            // --- TẠO 2 NÚT NEXT & PREV CHUYỂN SLIDE ---
            CircleButton btnPrev = new CircleButton("<");
            CircleButton btnNext = new CircleButton(">");
            
            // Canh lề 2 bên, giữa theo chiều dọc (Chiều cao panel là 360)
            btnPrev.setBounds(10, 160, 35, 35);
            btnNext.setBounds(535, 160, 35, 35);
            
            btnPrev.addActionListener(e -> changeSlide(-1));
            btnNext.addActionListener(e -> changeSlide(1));
            
            add(btnPrev);
            add(btnNext);

            slideTimer = new Timer(4000, e -> changeSlide(1));
            fadeTimer = new Timer(30, e -> {
                alpha += 0.05f;
                if (alpha >= 1.0f) { alpha = 1.0f; currentIndex = nextIndex; fadeTimer.stop(); }
                repaint();
            });

            if (images.size() > 1) slideTimer.start();
        }

        private void changeSlide(int direction) {
            if (images.size() <= 1 || fadeTimer.isRunning()) return;
            nextIndex = (currentIndex + direction + images.size()) % images.size();
            alpha = 0.0f;
            slideTimer.restart(); // Reset thời gian tự động chuyển
            fadeTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            Shape clip = new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 24, 24);
            g2.setClip(clip);

            g2.setColor(Color.decode("#E5E7EB"));
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (images.isEmpty()) {
                g2.setColor(Color.GRAY);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                g2.drawString("Chưa tải được ảnh Slide", 100, getHeight() / 2);
            } else {
                drawImageContain(g2, images.get(currentIndex), getWidth(), getHeight(), 1.0f);
                if (alpha < 1.0f) drawImageContain(g2, images.get(nextIndex), getWidth(), getHeight(), alpha);
            }
            
            // Xóa clip để có thể vẽ đè lên trên (vẽ border & chấm tròn)
            g2.setClip(null);

            if (images.size() > 1) {
                int dotW = 8, spc = 6;
                int startX = (getWidth() - ((images.size() * dotW) + ((images.size() - 1) * spc))) / 2;
                int currentX = startX;
                for (int i = 0; i < images.size(); i++) {
                    g2.setColor((i == (alpha < 0.5f ? currentIndex : nextIndex)) ? PRIMARY_GREEN : Color.decode("#9CA3AF")); 
                    g2.fillOval(currentX, getHeight() - 25, dotW, dotW);
                    currentX += dotW + spc;
                }
            }
            g2.dispose();
        }

        private void drawImageContain(Graphics2D g2, Image img, int w, int h, float alphaVal) {
            if (img == null) return;
            double scale = Math.min((double) w / img.getWidth(null), (double) h / img.getHeight(null));
            int dw = (int) (img.getWidth(null) * scale), dh = (int) (img.getHeight(null) * scale);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaVal));
            g2.drawImage(img, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
        }

        // Class nút tròn bán trong suốt (Semi-transparent Circle Button)
        class CircleButton extends JButton {
            public CircleButton(String text) {
                super(text);
                setContentAreaFilled(false); setFocusPainted(false); setBorderPainted(false);
                setForeground(Color.DARK_GRAY); setFont(new Font("Segoe UI", Font.BOLD, 16));
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 180)); // Trắng mờ
                g2.fillOval(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
                g2.dispose();
            }
        }
    }
}

class SmsLoginDialog extends JDialog {
    private final LoginFrame parentFrame;
    private final Color PRIMARY_GREEN = Color.decode("#10B981");
    private final Color TEXT_MAIN = Color.decode("#111827");
    private final Color TEXT_SUB = Color.decode("#6B7280");
    private final Color ERROR = Color.decode("#DC2626");

    private JTextField txtPhone;
    private JTextField txtOtp;
    private JLabel lblStatus;
    private JButton btnSendOtp;
    private JButton btnVerify;
    private Timer otpTimer;
    private int countdown = 60;

    SmsLoginDialog(LoginFrame parent) {
        super(parent, "SMS Login", true);
        this.parentFrame = parent;
        setSize(430, 455);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(null);
        getContentPane().setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("Đăng nhập bằng SMS", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(TEXT_MAIN);
        lblTitle.setBounds(0, 28, 430, 34);
        add(lblTitle);

        JLabel lblSub = new JLabel("Dùng số điện thoại đã xác minh trong Hồ sơ", SwingConstants.CENTER);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(TEXT_SUB);
        lblSub.setBounds(0, 66, 430, 22);
        add(lblSub);

        JLabel lblPhone = new JLabel("Số điện thoại");
        lblPhone.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPhone.setForeground(TEXT_MAIN);
        lblPhone.setBounds(42, 112, 180, 20);
        add(lblPhone);

        txtPhone = new JTextField();
        txtPhone.setBounds(42, 138, 346, 44);
        txtPhone.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPhone.putClientProperty("JTextField.placeholderText", "0912345678 hoặc +84912345678");
        txtPhone.putClientProperty("JComponent.arc", 14);
        InputFilters.installPhoneFilter(txtPhone);
        add(txtPhone);

        JLabel lblOtp = new JLabel("Mã OTP");
        lblOtp.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblOtp.setForeground(TEXT_MAIN);
        lblOtp.setBounds(42, 198, 180, 20);
        add(lblOtp);

        txtOtp = new JTextField();
        txtOtp.setBounds(42, 224, 206, 44);
        txtOtp.setFont(new Font("Segoe UI", Font.BOLD, 18));
        txtOtp.setHorizontalAlignment(JTextField.CENTER);
        txtOtp.putClientProperty("JTextField.placeholderText", "123456");
        txtOtp.putClientProperty("JComponent.arc", 14);
        InputFilters.installOtpFilter(txtOtp);
        add(txtOtp);

        btnSendOtp = new JButton("Gửi mã");
        btnSendOtp.setBounds(258, 224, 130, 44);
        btnSendOtp.setBackground(Color.decode("#F3F4F6"));
        btnSendOtp.setForeground(TEXT_MAIN);
        btnSendOtp.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSendOtp.putClientProperty("JComponent.arc", 14);
        btnSendOtp.setFocusPainted(false);
        btnSendOtp.setCursor(new Cursor(Cursor.HAND_CURSOR));
        add(btnSendOtp);

        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(TEXT_SUB);
        lblStatus.setBounds(42, 276, 346, 24);
        add(lblStatus);

        btnVerify = new JButton("Xác minh và đăng nhập");
        btnVerify.setBounds(42, 318, 346, 48);
        btnVerify.setBackground(PRIMARY_GREEN);
        btnVerify.setForeground(Color.WHITE);
        btnVerify.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnVerify.putClientProperty("JComponent.arc", 999);
        btnVerify.setFocusPainted(false);
        btnVerify.setCursor(new Cursor(Cursor.HAND_CURSOR));
        add(btnVerify);

        JButton btnCancel = new JButton("Hủy");
        btnCancel.setBounds(165, 372, 100, 28);
        btnCancel.setContentAreaFilled(false);
        btnCancel.setBorderPainted(false);
        btnCancel.setForeground(TEXT_SUB);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        add(btnCancel);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { stopCooldown("Gửi mã"); }
            @Override public void windowClosing(java.awt.event.WindowEvent e) { stopCooldown("Gửi mã"); }
        });
        btnCancel.addActionListener(e -> dispose());
        btnSendOtp.addActionListener(e -> requestSmsOtp());
        btnVerify.addActionListener(e -> verifySmsOtp());
    }

    private void requestSmsOtp() {
        String phone = txtPhone.getText().trim();
        if (!InputFilters.isValidPhone(phone)) {
            setStatus("Nhập số điện thoại hợp lệ trước khi gửi mã.", ERROR);
            txtPhone.requestFocus();
            return;
        }

        setStatus("Đang gửi mã xác minh...", TEXT_SUB);
        startCooldown();

        new Thread(() -> {
            try {
                AuthResponse res = new AuthClient().requestSmsLoginOtp(phone);

                SwingUtilities.invokeLater(() -> {
                    if (res.isSuccess()) {
                        setStatus("Mã OTP đã được gửi. Nhập 6 số để đăng nhập.", PRIMARY_GREEN);
                        txtOtp.requestFocus();
                    } else {
                        stopCooldown("Gửi mã");
                        setStatus(res.getMessage(), ERROR);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    stopCooldown("Gửi mã");
                    setStatus("Không kết nối được server. Vui lòng thử lại.", ERROR);
                });
            }
        }).start();
    }

    private void verifySmsOtp() {
        String phone = txtPhone.getText().trim();
        String otp = txtOtp.getText().trim();
        if (!InputFilters.isValidPhone(phone)) {
            setStatus("Số điện thoại chưa hợp lệ.", ERROR);
            txtPhone.requestFocus();
            return;
        }
        if (!InputFilters.isValidOtp(otp)) {
            setStatus("Mã OTP phải gồm đúng 6 chữ số.", ERROR);
            txtOtp.requestFocus();
            return;
        }

        btnVerify.setEnabled(false);
        btnVerify.setText("Đang xác minh...");
        setStatus("Đang kiểm tra mã OTP...", TEXT_SUB);

        new Thread(() -> {
            try {
                AuthResponse res = new AuthClient().verifySmsLogin(phone, otp);

                SwingUtilities.invokeLater(() -> {
                    btnVerify.setEnabled(true);
                    btnVerify.setText("Xác minh và đăng nhập");
                    if (res.isSuccess()) {
                        stopCooldown("Gửi mã");
                        dispose();
                        parentFrame.openDashboardFromAuth(res, phone);
                    } else {
                        setStatus(res.getMessage(), ERROR);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    btnVerify.setEnabled(true);
                    btnVerify.setText("Xác minh và đăng nhập");
                    setStatus("Không kết nối được server. Vui lòng thử lại.", ERROR);
                });
            }
        }).start();
    }

    private void startCooldown() {
        if (otpTimer != null) {
            otpTimer.stop();
        }
        btnSendOtp.setEnabled(false);
        countdown = 60;
        btnSendOtp.setText(countdown + "s");
        otpTimer = new Timer(1000, e -> {
            countdown--;
            btnSendOtp.setText(countdown + "s");
            if (countdown <= 0) {
                stopCooldown("Gửi lại");
            }
        });
        otpTimer.start();
    }

    private void stopCooldown(String text) {
        if (otpTimer != null) {
            otpTimer.stop();
        }
        btnSendOtp.setEnabled(true);
        btnSendOtp.setText(text);
    }

    private void setStatus(String message, Color color) {
        lblStatus.setText(message == null || message.trim().isEmpty() ? " " : message);
        lblStatus.setForeground(color);
    }
}

// ==========================================================
// CLASS DIALOG: FORGOT PASSWORD (ENGLISH VERSION)
// ==========================================================
class ForgotPasswordDialog extends JDialog {

    private final Color PRIMARY_GREEN = Color.decode("#10B981");
    private final Color TEXT_MAIN = Color.decode("#111827");
    private final Color TEXT_SUB = Color.decode("#6B7280");

    private JTextField txtEmail, txtOtp;
    private JPasswordField txtNewPass;
    private JButton btnGetOtp, btnReset;
    private Timer otpTimer;
    private int countdown = 60;

    public ForgotPasswordDialog(JFrame parent) {
        super(parent, "Recover Password", true);
        setSize(450, 530);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(null);
        getContentPane().setBackground(Color.WHITE);

        // Title
        JLabel lblTitle = new JLabel("Recover Password", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(TEXT_MAIN);
        lblTitle.setBounds(0, 30, 450, 35);
        add(lblTitle);

        // Subtitle
        JLabel lblSub = new JLabel("Enter your registered email to receive an OTP code", SwingConstants.CENTER);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSub.setForeground(TEXT_SUB);
        lblSub.setBounds(0, 65, 450, 20);
        add(lblSub);

        // ==========================================
        // INPUT FIELDS WITH LABELS
        // ==========================================
        
        // 1. Email Label & Field
        JLabel lblEmail = new JLabel("Email Address");
        lblEmail.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblEmail.setForeground(TEXT_MAIN);
        lblEmail.setBounds(40, 110, 300, 20);
        add(lblEmail);

        txtEmail = new JTextField();
        txtEmail.setBounds(40, 135, 360, 45);
        txtEmail.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtEmail.putClientProperty("JTextField.placeholderText", "e.g. admin@tutorhub.com");
        txtEmail.putClientProperty("JComponent.arc", 15);
        add(txtEmail);

        // 2. OTP Label & Field + Get Code Button
        JLabel lblOtp = new JLabel("Authentication Code (OTP)");
        lblOtp.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblOtp.setForeground(TEXT_MAIN);
        lblOtp.setBounds(40, 195, 200, 20);
        add(lblOtp);

        txtOtp = new JTextField();
        txtOtp.setBounds(40, 220, 220, 45);
        txtOtp.setFont(new Font("Segoe UI", Font.BOLD, 16));
        txtOtp.setHorizontalAlignment(JTextField.CENTER);
        txtOtp.putClientProperty("JTextField.placeholderText", "123456");
        txtOtp.putClientProperty("JComponent.arc", 15);
        add(txtOtp);

        btnGetOtp = new JButton("Get Code");
        btnGetOtp.setBounds(270, 220, 130, 45);
        btnGetOtp.setBackground(Color.decode("#F3F4F6"));
        btnGetOtp.setForeground(TEXT_MAIN);
        btnGetOtp.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnGetOtp.putClientProperty("JComponent.arc", 15);
        btnGetOtp.setFocusPainted(false);
        btnGetOtp.setCursor(new Cursor(Cursor.HAND_CURSOR));
        add(btnGetOtp);

        // 3. Password Label & Field
        JLabel lblPass = new JLabel("New Password");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPass.setForeground(TEXT_MAIN);
        lblPass.setBounds(40, 280, 300, 20);
        add(lblPass);

        txtNewPass = new JPasswordField();
        txtNewPass.setBounds(40, 305, 360, 45);
        txtNewPass.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtNewPass.putClientProperty("JTextField.placeholderText", "Enter new password");
        txtNewPass.putClientProperty("JComponent.arc", 15);
        add(txtNewPass);

        // ==========================================
        // ACTION BUTTONS
        // ==========================================
        
        // Reset Button
        btnReset = new JButton("Reset Password");
        btnReset.setBounds(40, 380, 360, 48);
        btnReset.setBackground(PRIMARY_GREEN); 
        btnReset.setForeground(Color.WHITE);
        btnReset.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnReset.putClientProperty("JComponent.arc", 999); 
        btnReset.setFocusPainted(false);
        btnReset.setCursor(new Cursor(Cursor.HAND_CURSOR));
        add(btnReset);

        // Back Button
        JButton btnCancel = new JButton("Get back");
        btnCancel.setBounds(175, 440, 100, 30);
        btnCancel.setContentAreaFilled(false);
        btnCancel.setBorderPainted(false);
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnCancel.setForeground(TEXT_SUB);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        add(btnCancel);

        // Events
        btnCancel.addActionListener(e -> dispose());
        btnGetOtp.addActionListener(e -> yeuCauGuiOtp());
        btnReset.addActionListener(e -> xuLyDoiMatKhau());
    }

    private void yeuCauGuiOtp() {
        String email = txtEmail.getText().trim();
        if (email.isEmpty() || !email.contains("@")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address!", "Invalid Email", JOptionPane.ERROR_MESSAGE);
            return;
        }

        btnGetOtp.setEnabled(false);
        countdown = 60;
        otpTimer = new Timer(1000, e -> {
            countdown--;
            btnGetOtp.setText(countdown + "s");
            if (countdown <= 0) {
                otpTimer.stop();
                btnGetOtp.setEnabled(true);
                btnGetOtp.setText("Resend");
            }
        });
        otpTimer.start();

        new Thread(() -> {
            try {
                AuthResponse res = new AuthClient().requestPasswordResetOtp(email);
                
                SwingUtilities.invokeLater(() -> {
                    if (!res.isSuccess()) {
                        JOptionPane.showMessageDialog(this, res.getMessage(), "Server Error", JOptionPane.ERROR_MESSAGE);
                        otpTimer.stop(); 
                        btnGetOtp.setEnabled(true); 
                        btnGetOtp.setText("Get Code");
                    } else {
                        JOptionPane.showMessageDialog(this, "OTP has been sent to your email.\nPlease check your inbox!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        txtOtp.requestFocus();
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Connection failed!", "Error", JOptionPane.ERROR_MESSAGE);
                    otpTimer.stop(); 
                    btnGetOtp.setEnabled(true); 
                    btnGetOtp.setText("Get Code");
                });
            }
        }).start();
    }

    private void xuLyDoiMatKhau() {
        String email = txtEmail.getText().trim();
        String otp = txtOtp.getText().trim();
        String newPass = new String(txtNewPass.getPassword());

        if (email.isEmpty() || otp.isEmpty() || newPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnReset.setText("Processing...");
        new Thread(() -> {
            try {
                AuthResponse res = new AuthClient().resetPassword(email, otp, newPass);

                SwingUtilities.invokeLater(() -> {
                    btnReset.setText("Reset Password");
                    if (res.isSuccess()) {
                        JOptionPane.showMessageDialog(this, res.getMessage(), "Success", JOptionPane.INFORMATION_MESSAGE);
                        dispose(); 
                    } else {
                        JOptionPane.showMessageDialog(this, res.getMessage(), "Failed", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    btnReset.setText("Reset Password");
                    JOptionPane.showMessageDialog(this, "Connection to server failed!", "Network Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
}
