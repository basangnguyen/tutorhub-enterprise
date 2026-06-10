package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * ExamIconFactory – Centralized icon provider for all exam UI components.
 *
 * Rules:
 *  - Never uses emoji, unicode glyphs, or text-as-icon.
 *  - SVG icons loaded via FlatSVGIcon (classpath, no hardcoded D:\ paths).
 *  - Fallback icons drawn via Graphics2D / Path2D when SVG not available.
 *  - All public methods are static for easy use across the package.
 */
public final class ExamIconFactory {

    // ── icon paths (classpath-relative, no leading '/') ──────────
    private static final String ICON_DIR = "images/exam/icons/";

    private ExamIconFactory() {} // utility class

    // ── SVG loader ────────────────────────────────────────────────

    /**
     * Load an SVG from resources/images/exam/icons/<name>.svg,
     * tinted to the given color, at the given pixel size.
     */
    public static FlatSVGIcon svg(String name, int size, Color color) {
        FlatSVGIcon icon = new FlatSVGIcon(ICON_DIR + name + ".svg", size, size);
        if (color != null) {
            icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
        }
        return icon;
    }

    /** Convenience: load SVG in navy-blue tone (icon default). */
    public static FlatSVGIcon svg(String name, int size) {
        return svg(name, size, Palette.ICON_NAVY);
    }

    // ── common icon shortcuts ─────────────────────────────────────
    public static FlatSVGIcon iconRefresh(int size)   { return svg("refresh-cw",     size); }
    public static FlatSVGIcon iconHelp(int size)      { return svg("circle-help",     size); }
    public static FlatSVGIcon iconSun(int size)       { return svg("sun",             size); }
    public static FlatSVGIcon iconUser(int size)      { return svg("user",            size); }
    public static FlatSVGIcon iconLock(int size)      { return svg("lock",            size); }
    public static FlatSVGIcon iconEye(int size)       { return svg("eye",             size); }
    public static FlatSVGIcon iconEyeOff(int size)    { return svg("eye-off",         size); }
    public static FlatSVGIcon iconShield(int size)    { return svg("shield",          size); }
    public static FlatSVGIcon iconKey(int size)       { return svg("key-round",       size); }
    public static FlatSVGIcon iconWifi(int size)      { return svg("wifi",            size); }
    public static FlatSVGIcon iconVolume(int size)    { return svg("volume-2",        size); }
    public static FlatSVGIcon iconPower(int size, Color c) { return svg("power",      size, c); }
    public static FlatSVGIcon iconGradCap(int size)   { return svg("graduation-cap",  size, Color.WHITE); }

    // ── Close button (replaces "✕" text) ─────────────────────────

    /**
     * Creates a flat close button that draws an X via Graphics2D.
     * Never relies on font glyphs → always renders correctly.
     */
    public static JButton closeButton() {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Hover background
                if (getModel().isRollover()) {
                    g2.setColor(new Color(0, 0, 0, 20));
                    g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 6, 6);
                }
                // Draw X
                int m = 6; // margin from edge
                int w = getWidth(), h = getHeight();
                g2.setColor(getModel().isRollover()
                        ? Color.decode("#111111")
                        : Color.decode("#666666"));
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(m, m, w - m, h - m);
                g2.drawLine(w - m, m, m, h - m);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(22, 22));
        btn.setMinimumSize(new Dimension(22, 22));
        btn.setMaximumSize(new Dimension(22, 22));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Warning icon (replaces "⚠" text) ─────────────────────────

    /**
     * Warning icon drawn via Path2D: yellow triangle with ! inside.
     */
    public static JLabel warningLabel(String text) {
        JLabel lbl = new JLabel(text) {
            private final Icon warnIcon = buildWarnIcon();
            { setIcon(warnIcon); setIconTextGap(6); }
        };
        return lbl;
    }

    private static Icon buildWarnIcon() {
        return new Icon() {
            private static final int W = 16, H = 15;
            @Override public int getIconWidth()  { return W; }
            @Override public int getIconHeight() { return H; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Triangle
                Path2D tri = new Path2D.Float();
                tri.moveTo(x + W / 2f,     y + 1);
                tri.lineTo(x + W - 1f,     y + H - 1f);
                tri.lineTo(x + 1f,         y + H - 1f);
                tri.closePath();
                g2.setColor(new Color(251, 191, 36)); // amber-400
                g2.fill(tri);
                g2.setColor(new Color(180, 120, 0));
                g2.setStroke(new BasicStroke(0.8f));
                g2.draw(tri);
                // Exclamation mark
                g2.setColor(new Color(92, 60, 0));
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = x + W / 2;
                g2.drawLine(cx, y + 5, cx, y + H - 6);      // stem
                g2.drawLine(cx, y + H - 4, cx, y + H - 3);  // dot
                g2.dispose();
            }
        };
    }

    // ── Check / Success icon (replaces "✔" text) ──────────────────

    /**
     * Success icon drawn via Path2D: green circle with checkmark.
     */
    public static Icon checkIcon(int size) {
        return new Icon() {
            @Override public int getIconWidth()  { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Circle
                g2.setColor(new Color(34, 197, 94)); // green-500
                g2.fillOval(x, y, size, size);
                // Checkmark
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(size * 0.12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int pad = size / 5;
                int mx  = x + pad;
                int my  = y + size / 2;
                int mdx = x + size / 3;
                int mdy = y + size - pad;
                int rx  = x + size - pad;
                int ry  = y + pad + 1;
                g2.drawLine(mx, my, mdx, mdy);
                g2.drawLine(mdx, mdy, rx, ry);
                g2.dispose();
            }
        };
    }

    // ── Diamond divider (replaces "◆" text) ───────────────────────

    /**
     * A tiny diamond icon drawn via Path2D for use in brand divider rows.
     */
    public static Icon diamondIcon(int size, Color color) {
        return new Icon() {
            @Override public int getIconWidth()  { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int h2 = size / 2;
                Path2D d = new Path2D.Float();
                d.moveTo(x + h2, y);            // top
                d.lineTo(x + size, y + h2);     // right
                d.lineTo(x + h2, y + size);     // bottom
                d.lineTo(x, y + h2);            // left
                d.closePath();
                g2.setColor(color);
                g2.fill(d);
                g2.dispose();
            }
        };
    }

    // ── Shared palette ────────────────────────────────────────────
    public static final class Palette {
        public static final Color ICON_NAVY  = Color.decode("#1E3A5F");
        public static final Color ICON_SLATE = Color.decode("#64748B");
        public static final Color ICON_WHITE = Color.WHITE;
        public static final Color ACCENT     = Color.decode("#2563EB");
        public static final Color GREEN      = Color.decode("#16A34A");
        public static final Color RED        = Color.decode("#EF4444");
        private Palette() {}
    }
}
