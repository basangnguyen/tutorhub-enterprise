package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.mycompany.tutorhub_enterprise.client.exam.models.*;
import com.mycompany.tutorhub_enterprise.client.exam.services.*;
import net.miginfocom.swing.MigLayout;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * TSELoginPanel – Login screen (Option B: Java Shell).
 * Bám sát ảnh mẫu số 2: logo thật, brand title gradient xanh dương,
 * divider line+diamond, tagline gần, card login sạch bo góc lớn.
 */
public class TSELoginPanel extends KioskBgPanel {

    // ── palette ──────────────────────────────────────────────────
    private static final Color BRAND_BLUE    = Color.decode("#1D4ED8"); // vibrant blue
    private static final Color BRAND_BLUE2   = Color.decode("#2563EB");
    private static final Color GREEN_BTN     = Color.decode("#16A34A");
    private static final Color CARD_BG       = new Color(255, 255, 255, 242);
    private static final Color ICON_NAVY     = Color.decode("#1E3A5F");
    private static final Color FIELD_BORDER  = Color.decode("#D1D5DB");
    private static final Color FIELD_ICON_FG = Color.decode("#9CA3AF");
    private static final Color TAGLINE_FG    = Color.decode("#4B5563");

    private final TSEExamService examService;
    private final java.util.function.Consumer<TSEExamContext> onLoginSuccess;
    private final Runnable onExit;
    private final TSECaptchaService captchaService = new TSECaptchaService();
    private JButton btnLogin;
    private JButton btnRetryConnect;
    private JLabel lblConnStatus;
    private JTextField txtCaptcha;
    private JLabel lblCaptcha;
    private Runnable onRetryConnect;

    public TSELoginPanel(TSEExamService examService, java.util.function.Consumer<TSEExamContext> onLoginSuccess, Runnable onExit) {
        super(new BorderLayout(0, 0));
        this.examService = examService;
        this.onLoginSuccess = onLoginSuccess;
        this.onExit = onExit;

        // Top bar – transparent, icons only
        add(buildTopBar(), BorderLayout.NORTH);

        // Center: branding left (55%) + card right (45%)
        JPanel center = new JPanel(new MigLayout(
                "fill, insets 0", "[55%][45%]", "[grow]"));
        center.setOpaque(false);
        center.add(buildBranding(), "cell 0 0, grow");
        center.add(buildLoginCard(), "cell 1 0, grow");
        add(center, BorderLayout.CENTER);

        // Footer / taskbar – light, translucent
        ExamFooterStatusBar footer = new ExamFooterStatusBar("VIE",
            btn -> TSEInputModeManager.getInstance().toggleMode(),
            (source, comp) -> {
                System.out.println("[TSE_PARENT_FOOTER] Quick Settings requested from LOGIN source=" + source);
                TSEParentHtmlQuickSettingsPopup.showPopup(comp, "LOGIN");
            },
            onExit
        );
        System.out.println("[TSE_PARENT_FOOTER] Footer attached to LOGIN card.");
        TSEInputModeManager.getInstance().addModeChangeListener(() -> {
            footer.setLanguageLabel(TSEInputModeManager.getInstance().getFooterLabel());
        });
        add(footer, BorderLayout.SOUTH);

        // (Preload removed, JCEF popup no longer used for Parent screens)

        // Attach Swing Adapter for Vietnamese input (Opt-in)
        SwingUtilities.invokeLater(() -> TSEInputSwingAdapter.installForOptIn(this));
    }

    // ── top bar ───────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(
                new MigLayout("insets 10 16, fillx, aligny center", "[left]push[right]"));
        bar.setOpaque(false);

        JButton btnRefresh = iconBtn("images/exam/icons/refresh-cw.svg", 22);
        bar.add(btnRefresh, "cell 0 0");

        JPanel right = new JPanel(new MigLayout("insets 0, gap 0"));
        right.setOpaque(false);
        JButton btnHelp = iconBtn("images/exam/icons/circle-help.svg", 20);
        btnHelp.setToolTipText("Thông tin ứng dụng");
        btnHelp.addActionListener(e -> TSEAboutDialog.showDialog(this, "Đăng nhập hệ thống"));
        right.add(btnHelp);
        bar.add(right, "cell 1 0");
        return bar;
    }

    // ── branding (left half) ──────────────────────────────────────
    private JPanel buildBranding() {
        // Shift branding up by changing vertical align to 42%
        JPanel outer = new JPanel(new MigLayout("fill, align 55% 42%"));
        outer.setOpaque(false);

        JPanel col = new JPanel(new MigLayout("wrap 1, align center center, insets 0, gapy 0"));
        col.setOpaque(false);

        // Logo
        LogoIconLabel logo = new LogoIconLabel(100);

        // ─── Line 1: "TUTORHUB SECURE EXAM" — uppercase bold, gradient, stroke, shadow ───
        JLabel lblFullName = new JLabel("TUTORHUB SECURE EXAM", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int textW = fm.stringWidth(getText());
                int x = (getWidth() - textW) / 2;
                int y = fm.getAscent() + (getHeight() - fm.getHeight()) / 2;

                // Move origin to the text baseline start
                g2.translate(x, y);

                // We use TextLayout to get the exact shape for stroke and shadow
                java.awt.font.TextLayout tl = new java.awt.font.TextLayout(getText(), getFont(), g2.getFontRenderContext());

                // 1. Very light luxurious drop shadow
                g2.setColor(new Color(15, 23, 42, 25)); // very light opacity
                g2.fill(tl.getOutline(java.awt.geom.AffineTransform.getTranslateInstance(0, 2)));

                // 2. Stroke / Outline (White luxurious border)
                g2.setColor(new Color(255, 255, 255, 220));
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(tl.getOutline(null));

                // 3. Fill gradient (Navy to bright blue)
                GradientPaint gp = new GradientPaint(
                        0, -fm.getAscent(), Color.decode("#1D4ED8"),
                        textW, fm.getDescent(), Color.decode("#3B82F6"));
                g2.setPaint(gp);
                g2.fill(tl.getOutline(null));

                g2.dispose();
            }
        };
        lblFullName.setFont(new Font("Segoe UI", Font.BOLD, 36));
        lblFullName.setForeground(BRAND_BLUE);
        lblFullName.setPreferredSize(new Dimension(480, 56));

        // ─── Divider: ── ◆ ── ───
        JPanel divRow = new JPanel(new MigLayout("insets 0, align center center", "[120!][12!][120!]"));
        divRow.setOpaque(false);
        divRow.add(buildDivLine(), "h 2!, w 120!, aligny center");
        JLabel diamond = new JLabel(ExamIconFactory.diamondIcon(8, BRAND_BLUE2));
        diamond.setPreferredSize(new Dimension(12, 12));
        divRow.add(diamond, "w 12!, h 12!, aligny center");
        divRow.add(buildDivLine(), "h 2!, w 120!, aligny center");

        // ─── Tagline — larger, darker, uppercase ───
        JLabel tagline = new JLabel(
                "<html><div style='text-align:center'>NỀN TẢNG TỔ CHỨC THI NHANH CHÓNG,<br>"
                + "BẢO MẬT VÀ CHUYÊN NGHIỆP</div></html>",
                SwingConstants.CENTER);
        tagline.setFont(new Font("Segoe UI", Font.BOLD, 17));
        tagline.setForeground(Color.decode("#374151"));
        tagline.setPreferredSize(new Dimension(480, 60));

        col.add(logo,        "align center, w 100!, h 100!, gapbottom 12");
        col.add(lblFullName, "align center, gapbottom 8");
        col.add(divRow,      "align center, gapbottom 12");
        col.add(tagline,     "align center");

        outer.add(col, "align 55% 35%");
        return outer;
    }

    private JPanel buildDivLine() {
        JPanel line = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(BRAND_BLUE2);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
                g2.dispose();
            }
        };
        line.setOpaque(false);
        return line;
    }

    // ── login card (right half) ───────────────────────────────────
    private JPanel buildLoginCard() {
        // Shift card up to align with logo on the left (vertical align 10%)
        JPanel wrapper = new JPanel(new MigLayout("fill, align center 10%"));
        wrapper.setOpaque(false);

        // Card with rounded corners + shadow simulation via border layering
        JPanel card = new JPanel(new MigLayout(
                "wrap 1, insets 36 44 36 44, gapy 14", "[340!]")) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // subtle multi-layer shadow
                for (int i = 4; i > 0; i--) {
                    g2.setColor(new Color(0, 0, 0, 8 - i));
                    g2.fillRoundRect(i, i + 2, getWidth() - i * 2, getHeight() - i * 2, 32, 32);
                }
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 32, 32));
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };

        // Card title
        JLabel lblTitle = new JLabel("ĐĂNG NHẬP HỆ THỐNG", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(BRAND_BLUE2);
        card.add(lblTitle, "growx, gapbottom 6");

        // Fields
        JTextField txtUsername = new JTextField();
        JPasswordField txtPassword = new JPasswordField();

        // Username field – FlatLaf inline style
        card.add(buildFlatField("images/exam/icons/user.svg", "Tên đăng nhập", false, null, txtUsername), "growx, h 44!");

        // Password field with eye icon
        card.add(buildFlatField("images/exam/icons/lock.svg", "Mật khẩu", true, "images/exam/icons/eye.svg", txtPassword), "growx, h 44!");

        // Captcha row: [mã bảo mật field] [captcha display] [refresh btn]
        card.add(buildCaptchaRow(), "growx, h 44!");

        // Status label
        lblConnStatus = new JLabel("Đang kết nối tới Server...", SwingConstants.CENTER);
        lblConnStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblConnStatus.setForeground(Color.decode("#4B5563"));
        card.add(lblConnStatus, "growx, gapbottom 4");

        // Login button
        btnLogin = new JButton("  Đăng nhập");
        btnLogin.setEnabled(false); // Initially disabled until connected
        btnLogin.setIcon(loadSVG("images/exam/icons/lock.svg", 16, Color.WHITE));
        btnLogin.setIconTextGap(6);
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnLogin.setBackground(GREEN_BTN);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setOpaque(true);
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        btnLogin.addActionListener(e -> {
            String u = txtUsername.getText().trim();
            String p = new String(txtPassword.getPassword());

            if (!captchaService.verify(txtCaptcha != null ? txtCaptcha.getText() : "")) {
                lblConnStatus.setText("Mã bảo mật không đúng. Vui lòng nhập lại.");
                lblConnStatus.setForeground(Color.decode("#DC2626"));
                refreshCaptcha();
                if (txtCaptcha != null) {
                    txtCaptcha.requestFocusInWindow();
                }
                return;
            }

            btnLogin.setEnabled(false);
            btnLogin.setText("  Đang đăng nhập...");
            
            examService.login(u, p).thenAccept(res -> {
                SwingUtilities.invokeLater(() -> {
                    btnLogin.setEnabled(true);
                    if (!(res.success && res.context != null)) {
                        refreshCaptcha();
                    }
                    btnLogin.setText("  Đăng nhập");
                    if (res.success && res.context != null) {
                        if (onLoginSuccess != null) onLoginSuccess.accept(res.context);
                    } else {
                        JOptionPane.showMessageDialog(TSELoginPanel.this, res.message, "Lỗi đăng nhập", JOptionPane.ERROR_MESSAGE);
                    }
                });
            });
        });
        
        // Rounded button via UI override – FlatLaf supports putClientProperty
        btnLogin.putClientProperty("JButton.buttonType", "roundRect");
        btnLogin.putClientProperty("JComponent.roundRect", true);
        card.add(btnLogin, "growx, h 48!, gaptop 4");

        // Retry Connect Button
        btnRetryConnect = new JButton("Thử kết nối lại");
        btnRetryConnect.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnRetryConnect.setBackground(Color.decode("#DC2626")); // Red button
        btnRetryConnect.setForeground(Color.WHITE);
        btnRetryConnect.setFocusPainted(false);
        btnRetryConnect.setBorderPainted(false);
        btnRetryConnect.setOpaque(true);
        btnRetryConnect.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRetryConnect.putClientProperty("JButton.buttonType", "roundRect");
        btnRetryConnect.putClientProperty("JComponent.roundRect", true);
        btnRetryConnect.setVisible(false); // Initially hidden
        btnRetryConnect.addActionListener(e -> {
            btnRetryConnect.setVisible(false);
            lblConnStatus.setText("Đang kết nối tới Server...");
            lblConnStatus.setForeground(Color.decode("#4B5563"));
            if (onRetryConnect != null) onRetryConnect.run();
        });
        card.add(btnRetryConnect, "growx, h 40!, gaptop 4");

        wrapper.add(card);
        return wrapper;
    }

    public void setConnectionStatus(String statusText, boolean canLogin, boolean showRetry) {
        SwingUtilities.invokeLater(() -> {
            lblConnStatus.setText(statusText);
            if (canLogin) {
                btnLogin.setEnabled(true);
            } else {
                btnLogin.setEnabled(false);
            }
            btnRetryConnect.setVisible(showRetry);
            
            if (showRetry) {
                lblConnStatus.setForeground(Color.decode("#DC2626")); // Red for error
            } else if (canLogin) {
                lblConnStatus.setForeground(Color.decode("#16A34A")); // Green for success
            } else {
                lblConnStatus.setForeground(Color.decode("#4B5563")); // Gray for connecting
            }
        });
    }

    public void setOnRetryConnect(Runnable onRetry) {
        this.onRetryConnect = onRetry;
    }

    /** FlatLaf-style single-row field with leading icon, optional trailing icon. */
    private JPanel buildFlatField(String iconPath, String placeholder, boolean isPassword, String trailingIconPath, JTextField field) {
        JPanel wrap = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(FIELD_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };

        JPanel inner = new JPanel(new MigLayout("insets 0 8 0 8, fill", "[20!][grow][20!]"));
        inner.setOpaque(false);

        // Leading icon
        JLabel leadIcon = new JLabel(loadSVG(iconPath, 16, FIELD_ICON_FG));
        inner.add(leadIcon, "w 20!, growy, gapright 4");

        // Text field
        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.putClientProperty("JTextField.placeholderText", placeholder);
        if ("images/exam/icons/shield.svg".equals(iconPath) && txtCaptcha == null) {
            txtCaptcha = field;
        }
        if (isPassword) {
            ((JPasswordField) field).setEchoChar('\u25CF');
        }
        inner.add(field, "grow, growy");

        // Trailing icon (optional)
        if (trailingIconPath != null) {
            if (field instanceof JPasswordField passwordField) {
                JButton toggle = buildPasswordToggleButton(passwordField, trailingIconPath);
                inner.add(toggle, "w 20!, h 28!, growy, gapleft 4");
            } else {
                JLabel trail = new JLabel(loadSVG(trailingIconPath, 16, FIELD_ICON_FG));
                inner.add(trail, "w 20!, growy, gapleft 4");
            }
        } else {
            inner.add(new JLabel(), "w 20!, growy");
        }

        wrap.add(inner, BorderLayout.CENTER);
        return wrap;
    }

    /** Captcha row: [shield + mã bảo mật input] | [captcha numbers] | [refresh] */
    private JPanel buildCaptchaRow() {
        JPanel row = new JPanel(new MigLayout("insets 0, fillx, gap 6", "[grow][120!][36!]"));
        row.setOpaque(false);

        // Left: mã bảo mật field
        JPanel captchaField = buildFlatField("images/exam/icons/shield.svg", "Mã bảo mật", false, null, new JTextField());
        row.add(captchaField, "h 44!, grow");

        // Center: captcha display box
        JLabel lblCap = new JLabel("7 2 7 7 7 2", SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(235, 242, 255));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(Color.decode("#C7D2FE"));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-1, getHeight()-1, 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        lblCaptcha = lblCap;
        lblCaptcha.setText(captchaService.getDisplayText());
        lblCap.setFont(new Font("Courier New", Font.BOLD, 17));
        lblCap.setForeground(BRAND_BLUE2);
        row.add(lblCap, "h 44!");

        // Right: refresh button
        JButton btnRef = new JButton(loadSVG("images/exam/icons/refresh-cw.svg", 18, BRAND_BLUE2)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(219, 234, 254));
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnRef.setContentAreaFilled(false);
        btnRef.setBorderPainted(false);
        btnRef.setFocusPainted(false);
        btnRef.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRef.setToolTipText("Đổi mã bảo mật");
        btnRef.addActionListener(e -> refreshCaptcha());
        row.add(btnRef, "h 44!, w 36!");

        return row;
    }

    private JButton buildPasswordToggleButton(JPasswordField passwordField, String visibleIconPath) {
        JButton button = new JButton(loadSVG(visibleIconPath, 16, FIELD_ICON_FG));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText("Hiện mật khẩu");

        final char hiddenEcho = passwordField.getEchoChar();
        final boolean[] showing = { false };
        button.addActionListener(e -> {
            showing[0] = !showing[0];
            passwordField.setEchoChar(showing[0] ? (char) 0 : hiddenEcho);
            button.setIcon(loadSVG(
                    showing[0] ? "images/exam/icons/eye-off.svg" : visibleIconPath,
                    16,
                    FIELD_ICON_FG));
            button.setToolTipText(showing[0] ? "Ẩn mật khẩu" : "Hiện mật khẩu");
            passwordField.requestFocusInWindow();
        });
        return button;
    }

    private void refreshCaptcha() {
        captchaService.regenerate();
        if (lblCaptcha != null) {
            lblCaptcha.setText(captchaService.getDisplayText());
            lblCaptcha.repaint();
        }
        if (txtCaptcha != null) {
            txtCaptcha.setText("");
        }
    }

    private void showQuickSettingsSwingPopup(JComponent anchor) {
        // Obsolete, now using JCEF Popup
    }

    private JPanel buildTaskbar() {
        // Logic moved to constructor using ExamFooterStatusBar
        return new JPanel();
    }

    // ── static helpers ────────────────────────────────────────────
    static JButton iconBtn(String svgPath, int size) {
        JButton btn = new JButton(loadSVG(svgPath, size, ICON_NAVY));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    static JLabel taskbarLabel(String text, String svgPath) {
        JLabel lbl = new JLabel();
        if (text != null) lbl.setText(text);
        if (svgPath != null) lbl.setIcon(loadSVG(svgPath, 18, ICON_NAVY));
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(ICON_NAVY);
        return lbl;
    }

    static FlatSVGIcon loadSVG(String path, int size, Color color) {
        FlatSVGIcon icon = new FlatSVGIcon(path, size, size);
        if (color != null) {
            icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
        }
        return icon;
    }
}
