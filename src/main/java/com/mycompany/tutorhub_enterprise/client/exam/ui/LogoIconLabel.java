package com.mycompany.tutorhub_enterprise.client.exam.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * LogoIconLabel – Draws the TutorHub logo.
 *
 * Tries to load /images/icon/logo.svg as PNG first (SVG requires extra lib).
 * Falls back to a painted gradient rounded-square with mortar-board glyph.
 *
 * Size: configurable via constructor.
 */
public class LogoIconLabel extends JLabel {

    private final int size;
    private FlatSVGIcon logoIcon;
    private FlatSVGIcon fallbackIcon;

    // Gradient: purple-blue matching the screenshot icon
    private static final Color GRAD_TOP    = Color.decode("#5B4FD4");
    private static final Color GRAD_BOTTOM = Color.decode("#3B8FE8");

    public LogoIconLabel(int size) {
        this.size = size;
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));

        // Load the actual app logo
        try {
            FlatSVGIcon rawLogo = new FlatSVGIcon("images/icon/logo.svg", size, size);
            if (rawLogo.hasFound()) {
                logoIcon = rawLogo;
            }
        } catch (Exception ignored) {}
        
        fallbackIcon = ExamLoginMockPanel.loadSVG("images/exam/icons/graduation-cap.svg", size / 2, Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int s = Math.min(getWidth(), getHeight());
        int ox = (getWidth() - s) / 2;
        int oy = (getHeight() - s) / 2;
        int arc = s / 4;

        if (logoIcon != null) {
            // Draw real logo
            logoIcon.paintIcon(this, g2, ox, oy);
        } else {
            // Painted gradient icon fallback
            GradientPaint gp = new GradientPaint(ox, oy, GRAD_TOP, ox, oy + s, GRAD_BOTTOM);
            g2.setPaint(gp);
            g2.fillRoundRect(ox, oy, s, s, arc, arc);

            // Mortar-board symbol (SVG)
            if (fallbackIcon != null) {
                int iw = fallbackIcon.getIconWidth();
                int ih = fallbackIcon.getIconHeight();
                fallbackIcon.paintIcon(this, g2, ox + (s - iw) / 2, oy + (s - ih) / 2);
            }
        }
        g2.dispose();
    }
}
