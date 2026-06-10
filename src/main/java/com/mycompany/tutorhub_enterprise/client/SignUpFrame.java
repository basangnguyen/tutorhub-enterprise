package com.mycompany.tutorhub_enterprise.client;

import com.formdev.flatlaf.FlatLightLaf;
import com.mycompany.tutorhub_enterprise.models.auth.AuthResponse;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SignUpFrame extends JFrame {

    // Inherit colors from LoginFrame
    private final Color PRIMARY_GREEN = Color.decode("#10B981");
    private final Color TEXT_MAIN = Color.decode("#111827");
    private final Color TEXT_SUB = Color.decode("#6B7280");
    private final Color BORDER_COLOR = Color.decode("#E5E7EB");

    private JTextField txtEmail, txtName, txtOtp;
    private JPasswordField txtPassword;
    private RoundedButton btnGetOtp, btnRegister;
    private JCheckBox chkTerms;
    private Timer otpTimer;
    private int countdown = 60;

    public SignUpFrame() {
        setTitle("TutorHub Enterprise - Sign up");
        setSize(1000, 650); 
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
        // LEFT PANEL (SYNCED WITH LOGIN FRAME)
        // ==========================================================
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(Color.decode("#F4F5F7")); 
        leftPanel.setBounds(0, 0, 650, 650);
        leftPanel.setLayout(null);

        JButton badgeBtn = new JButton("TutorHub Enterprise");
        badgeBtn.setBounds(240, 60, 170, 30);
        badgeBtn.setBackground(Color.decode("#FFFFFF"));
        badgeBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        badgeBtn.setForeground(PRIMARY_GREEN);
        badgeBtn.putClientProperty("JButton.buttonType", "roundRect");
        badgeBtn.setEnabled(false); 
        leftPanel.add(badgeBtn);

        JLabel lblMainTitle = new JLabel("AI-Powered Tutoring Platform", SwingConstants.CENTER);
        lblMainTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblMainTitle.setForeground(TEXT_MAIN);
        lblMainTitle.setBounds(0, 110, 650, 40);
        leftPanel.add(lblMainTitle);

        JLabel lblSubTitle = new JLabel("Manage classes, match tutors, and track schedules in one intelligent platform.", SwingConstants.CENTER);
        lblSubTitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lblSubTitle.setForeground(TEXT_SUB);
        lblSubTitle.setBounds(0, 150, 650, 30);
        leftPanel.add(lblSubTitle);

        SignUpImageSlider imageSlider = new SignUpImageSlider();
        imageSlider.setBounds(35, 210, 580, 360);
        leftPanel.add(imageSlider);

        add(leftPanel);

        // ==========================================================
        // RIGHT PANEL (EMAIL SIGN UP FORM)
        // ==========================================================
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setBounds(650, 0, 350, 650);
        rightPanel.setLayout(null);

        // --- APP ICON & BRAND (SYNCED WITH LOGIN) ---
        JPanel brandPanel = new JPanel(null);
        brandPanel.setOpaque(false);
        brandPanel.setBounds(30, 55, 200, 40); 
        
        JLabel lblAppIcon = new JLabel();
        setLocalIcon(lblAppIcon, "/images/logomoi.png", 36, 36); 
        lblAppIcon.setBounds(0, 0, 36, 36);
        brandPanel.add(lblAppIcon);
        
        JLabel lblLogo = new JLabel("TutorHub");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblLogo.setForeground(TEXT_MAIN);
        lblLogo.setBounds(45, 0, 150, 22);
        brandPanel.add(lblLogo);
        
        JLabel lblBrandSub = new JLabel("Tutoring Platform");
        lblBrandSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblBrandSub.setForeground(TEXT_SUB);
        lblBrandSub.setBounds(47, 22, 150, 15);
        brandPanel.add(lblBrandSub);
        
        rightPanel.add(brandPanel);

        // Log In Button
        JLabel lblLogin = new JLabel("Log In");
        lblLogin.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblLogin.setForeground(PRIMARY_GREEN); 
        lblLogin.setBounds(260, 60, 60, 30); 
        lblLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rightPanel.add(lblLogin);
        lblLogin.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose(); 
            }
        });

        // Form Title
        JLabel lblFormTitle = new JLabel("Create an Account");
        lblFormTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblFormTitle.setForeground(TEXT_MAIN);
        lblFormTitle.setBounds(30, 110, 290, 35);
        rightPanel.add(lblFormTitle);

        // --- INPUT FIELDS (48px HEIGHT) ---
        
        // 1. Full Name
        txtName = new JTextField();
        txtName.setBounds(30, 155, 290, 48);
        txtName.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtName.putClientProperty("JTextField.placeholderText", "Full Name");
        rightPanel.add(txtName);

        // 2. Email & Get Code Button
        txtEmail = new JTextField();
        txtEmail.setBounds(30, 220, 175, 48);
        txtEmail.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtEmail.putClientProperty("JTextField.placeholderText", "Email Address");
        rightPanel.add(txtEmail);

        btnGetOtp = new RoundedButton("Get Code", Color.decode("#F3F4F6"), TEXT_MAIN, BORDER_COLOR, 48);
        btnGetOtp.setBounds(215, 220, 105, 48);
        btnGetOtp.setFont(new Font("Segoe UI", Font.BOLD, 13));
        rightPanel.add(btnGetOtp);

        // 3. OTP Code
        txtOtp = new JTextField();
        txtOtp.setBounds(30, 285, 290, 48);
        txtOtp.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txtOtp.setHorizontalAlignment(JTextField.CENTER);
        txtOtp.putClientProperty("JTextField.placeholderText", "- Enter OTP Code -");
        rightPanel.add(txtOtp);

        // 4. Password
        txtPassword = new JPasswordField();
        txtPassword.setBounds(30, 350, 290, 48);
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPassword.putClientProperty("JTextField.placeholderText", "Password");
        rightPanel.add(txtPassword);

        // Terms and Conditions
        chkTerms = new JCheckBox("<html>I have read and agree to the <font color='#10B981'>User<br>Agreement</font> and <font color='#10B981'>Privacy Policy</font></html>");
        chkTerms.setBounds(25, 410, 300, 40);
        chkTerms.setBackground(Color.WHITE);
        chkTerms.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        chkTerms.setForeground(TEXT_SUB);
        chkTerms.setFocusPainted(false);
        rightPanel.add(chkTerms);

        // Register Button
        btnRegister = new RoundedButton("Create Account", PRIMARY_GREEN, Color.WHITE, null, 48);
        btnRegister.setBounds(30, 465, 290, 48);
        btnRegister.setFont(new Font("Segoe UI", Font.BOLD, 16));
        rightPanel.add(btnRegister);

        add(rightPanel);

        // ==========================================
        // EVENT LISTENERS
        // ==========================================
        btnGetOtp.addActionListener(e -> yeuCauGuiOtp());
        btnRegister.addActionListener(e -> xuLyDangKy());
        
        btnRegister.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnRegister.setBgColor(Color.decode("#059669")); } 
            public void mouseExited(MouseEvent e) { btnRegister.setBgColor(PRIMARY_GREEN); }
        });
    }

    // ==========================================
    // BUSINESS LOGIC
    // ==========================================
    private void yeuCauGuiOtp() {
        String email = txtEmail.getText().trim();
        
        if (email.isEmpty() || !email.contains("@") || !email.contains(".")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address!", "Invalid Email", JOptionPane.ERROR_MESSAGE);
            return;
        }

        startCountdown();

        new Thread(() -> {
            try {
                AuthResponse response = new AuthClient().requestRegistrationOtp(email);

                SwingUtilities.invokeLater(() -> {
                    if (response.isSuccess()) {
                        JOptionPane.showMessageDialog(this, "OTP code has been sent to your inbox!\nPlease check your email (including Spam folder).", "Check Email", JOptionPane.INFORMATION_MESSAGE);
                        txtOtp.requestFocus();
                    } else {
                        JOptionPane.showMessageDialog(this, response.getMessage(), "Server Error", JOptionPane.ERROR_MESSAGE);
                        stopCountdown(); 
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Cannot connect to the Server!", "Network Error", JOptionPane.ERROR_MESSAGE);
                    stopCountdown();
                });
            }
        }).start();
    }

    private void xuLyDangKy() {
        if (!chkTerms.isSelected()) {
            JOptionPane.showMessageDialog(this, "You must agree to the Terms of Use and Privacy Policy!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String email = txtEmail.getText().trim();
        String otp = txtOtp.getText().trim();
        String pass = new String(txtPassword.getPassword());
        String name = txtName.getText().trim();

        if (email.isEmpty() || otp.isEmpty() || pass.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields!", "Missing Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnRegister.setText("Processing...");

        new Thread(() -> {
            try {
                AuthResponse response = new AuthClient().register(email, otp, pass, name);

                SwingUtilities.invokeLater(() -> {
                    btnRegister.setText("Create Account");
                    if (response.isSuccess()) {
                        JOptionPane.showMessageDialog(this, "Registration successful! Please log in.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dispose(); 
                    } else {
                        JOptionPane.showMessageDialog(this, response.getMessage(), "Failed", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    btnRegister.setText("Create Account");
                    JOptionPane.showMessageDialog(this, "Cannot connect to the Server!", "Network Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void startCountdown() {
        btnGetOtp.setEnabled(false);
        countdown = 60;
        otpTimer = new Timer(1000, e -> {
            countdown--;
            btnGetOtp.setText(countdown + "s");
            if (countdown <= 0) {
                stopCountdown();
            }
        });
        otpTimer.start();
    }

    private void stopCountdown() {
        if (otpTimer != null) otpTimer.stop();
        btnGetOtp.setEnabled(true);
        btnGetOtp.setText("Get Code");
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
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.drawImage(srcImg, 0, 0, width, height, null);
                g2.dispose();
                
                ImageIcon scaled = new ImageIcon(resizedImg);
                if (comp instanceof JLabel) ((JLabel) comp).setIcon(scaled);
                else if (comp instanceof JButton) ((JButton) comp).setIcon(scaled);
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
        SwingUtilities.invokeLater(() -> new SignUpFrame().setVisible(true));
    }

    // ==========================================================
    // CLASS CUSTOM: ROUNDED BUTTON
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
    // CLASS: IMAGE SLIDER (SYNCED FROM LOGIN FRAME)
    // ==========================================================
    class SignUpImageSlider extends JPanel {
        private List<Image> images = new ArrayList<>();
        private int currentIndex = 0, nextIndex = 1;
        private float alpha = 1.0f;
        private Timer slideTimer, fadeTimer;

        public SignUpImageSlider() {
            setOpaque(false);
            setLayout(null); 
            
            
            for (int i = 1; i <= 3; i++) {
                try {
                    URL url = getClass().getResource("/images/slide_login/login" + i + ".png");
                    if (url != null) images.add(new ImageIcon(url).getImage());
                } catch (Exception e) {}
            }

            CircleButton btnPrev = new CircleButton("<");
            CircleButton btnNext = new CircleButton(">");
            
            btnPrev.setBounds(10, 160, 35, 35);
            btnNext.setBounds(535, 160, 35, 35);
            
            btnPrev.addActionListener(e -> changeSlide(-1));
            btnNext.addActionListener(e -> changeSlide(1));
            
            add(btnPrev); add(btnNext);

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
            alpha = 0.0f; slideTimer.restart(); fadeTimer.start();
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

            if (!images.isEmpty()) {
                drawImageContain(g2, images.get(currentIndex), getWidth(), getHeight(), 1.0f);
                if (alpha < 1.0f) drawImageContain(g2, images.get(nextIndex), getWidth(), getHeight(), alpha);
            } else {
                g2.setColor(Color.GRAY);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                g2.drawString("Slide image failed to load", 100, getHeight() / 2);
            }
            
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

        class CircleButton extends JButton {
            public CircleButton(String text) {
                super(text); setContentAreaFilled(false); setFocusPainted(false); setBorderPainted(false);
                setForeground(Color.DARK_GRAY); setFont(new Font("Segoe UI", Font.BOLD, 16));
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 180)); 
                g2.fillOval(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
                g2.dispose();
            }
        }
    }
}
