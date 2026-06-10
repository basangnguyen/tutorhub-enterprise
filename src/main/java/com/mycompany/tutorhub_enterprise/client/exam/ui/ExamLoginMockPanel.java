package com.mycompany.tutorhub_enterprise.client.exam.ui;

import net.miginfocom.swing.MigLayout;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * ExamLoginMockPanel – Login screen (Option B: Java Shell).
 * Bám sát ảnh mẫu số 2: logo thật, brand title gradient xanh dương,
 * divider line+diamond, tagline gần, card login sạch bo góc lớn.
 */
public class ExamLoginMockPanel extends KioskBgPanel {

    // ── palette ──────────────────────────────────────────────────
    private static final Color BRAND_BLUE    = Color.decode("#1D4ED8"); // vibrant blue
    private static final Color BRAND_BLUE2   = Color.decode("#2563EB");
    private static final Color GREEN_BTN     = Color.decode("#16A34A");
    private static final Color CARD_BG       = new Color(255, 255, 255, 242);
    private static final Color ICON_NAVY     = Color.decode("#1E3A5F");
    private static final Color FIELD_BORDER  = Color.decode("#D1D5DB");
    private static final Color FIELD_ICON_FG = Color.decode("#9CA3AF");
    private static final Color TAGLINE_FG    = Color.decode("#4B5563");

    private final Runnable onExit;

    public ExamLoginMockPanel(Runnable onLogin, Runnable onExit) {
        super(new BorderLayout(0, 0));
        this.onExit = onExit;

        // Top bar – transparent, icons only
        add(buildTopBar(), BorderLayout.NORTH);

        // Center: branding left (55%) + card right (45%)
        JPanel center = new JPanel(new MigLayout(
                "fill, insets 0", "[55%][45%]", "[grow]"));
        center.setOpaque(false);
        center.add(buildBranding(), "cell 0 0, grow");
        center.add(buildLoginCard(onLogin), "cell 1 0, grow");
        add(center, BorderLayout.CENTER);

        // Footer / taskbar – light, translucent
        add(buildTaskbar(), BorderLayout.SOUTH);
    }

    // ── top bar ───────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(
                new MigLayout("insets 10 16, fillx, aligny center", "[left]push[right]"));
        bar.setOpaque(false);

        JButton btnRefresh = iconBtn("images/exam/icons/refresh-cw.svg", 22);
        bar.add(btnRefresh, "cell 0 0");

        JPanel right = new JPanel(new MigLayout("insets 0, gap 10"));
        right.setOpaque(false);
        right.add(iconBtn("images/exam/icons/circle-help.svg", 20));
        right.add(iconBtn("images/exam/icons/sun.svg", 20));
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
    private JPanel buildLoginCard(Runnable onLogin) {
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

        // Username field – FlatLaf inline style
        card.add(buildFlatField("images/exam/icons/user.svg", "Tên đăng nhập", false, null), "growx, h 44!");

        // Password field with eye icon
        card.add(buildFlatField("images/exam/icons/lock.svg", "Mật khẩu", true, "images/exam/icons/eye.svg"), "growx, h 44!");

        // Captcha row: [mã bảo mật field] [captcha display] [refresh btn]
        card.add(buildCaptchaRow(), "growx, h 44!");

        // Login button
        JButton btnLogin = new JButton("  Đăng nhập");
        btnLogin.setIcon(loadSVG("images/exam/icons/lock.svg", 16, Color.WHITE));
        btnLogin.setIconTextGap(6);
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnLogin.setBackground(GREEN_BTN);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setOpaque(true);
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.addActionListener(e -> { if (onLogin != null) onLogin.run(); });
        // Rounded button via UI override – FlatLaf supports putClientProperty
        btnLogin.putClientProperty("JButton.buttonType", "roundRect");
        btnLogin.putClientProperty("JComponent.roundRect", true);
        card.add(btnLogin, "growx, h 48!, gaptop 4");

        wrapper.add(card);
        return wrapper;
    }

    /** FlatLaf-style single-row field with leading icon, optional trailing icon. */
    private JPanel buildFlatField(String iconPath, String placeholder, boolean isPassword, String trailingIconPath) {
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
        JTextField field = isPassword ? new JPasswordField() : new JTextField();
        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.putClientProperty("JTextField.placeholderText", placeholder);
        if (isPassword) ((JPasswordField) field).setEchoChar('●');
        inner.add(field, "grow, growy");

        // Trailing icon (optional)
        if (trailingIconPath != null) {
            JLabel trail = new JLabel(loadSVG(trailingIconPath, 16, FIELD_ICON_FG));
            inner.add(trail, "w 20!, growy, gapleft 4");
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
        JPanel captchaField = buildFlatField("images/exam/icons/shield.svg", "Mã bảo mật", false, null);
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
        row.add(btnRef, "h 44!, w 36!");

        return row;
    }

    // ── taskbar (shared style) ────────────────────────────────────
    static JPanel buildTaskbarStatic(Runnable onExit) {
        JPanel bar = new JPanel(new MigLayout("insets 5 18, fillx, aligny center", "push[right]"));
        bar.setBackground(new Color(228, 231, 235)); // 10% darker gray, fully opaque
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0, 0, 0, 15)));
        bar.setOpaque(true);

        JPanel right = new JPanel(new MigLayout("insets 0, gap 14, aligny center"));
        right.setOpaque(false);

        JLabel lblVie = new JLabel("VIE");
        lblVie.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblVie.setForeground(ICON_NAVY);
        right.add(lblVie);
        right.add(taskbarLabel(null, "images/exam/icons/wifi.svg"));
        right.add(taskbarLabel(null, "images/exam/icons/volume-2.svg"));

        // Battery: custom Graphics2D component (98%, charging)
        BatteryStatusIcon battery = new BatteryStatusIcon();
        battery.setBatteryPercent(98);
        battery.setCharging(true);
        battery.setToolTipText("98% – Đang sạc");
        right.add(battery, "w 28!, h 14!, aligny center");

        if (onExit != null) {
            JButton btnPower = new JButton(loadSVG("images/exam/icons/power.svg", 18, Color.decode("#C62828"))); // Dark red
            btnPower.setContentAreaFilled(false);
            btnPower.setBorderPainted(false);
            btnPower.setFocusPainted(false);
            btnPower.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnPower.addActionListener(e -> {
                JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(bar);
                ExitConfirmDialog dlg = new ExitConfirmDialog(parent, onExit);
                dlg.setVisible(true);
            });
            right.add(btnPower);
        }

        bar.add(right, "cell 0 0");
        return bar;
    }

    private JPanel buildTaskbar() {
        return buildTaskbarStatic(onExit);
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
