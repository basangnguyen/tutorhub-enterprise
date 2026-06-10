package com.mycompany.tutorhub_enterprise.client.exam.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * KioskBgPanel – JPanel that paints background.jpg scaled to fill.
 * Used by Login and Config screens.
 * Loads from classpath: /images/exam/background.jpg
 * Falls back to gradient if image not found.
 */
public class KioskBgPanel extends JPanel {

    private BufferedImage bgImage;

    // Gradient fallback colours (match the lavender-pink in screenshots)
    private static final Color GRAD_TOP    = Color.decode("#D8D8EE");
    private static final Color GRAD_BOTTOM = Color.decode("#F0D8E8");

    public KioskBgPanel(LayoutManager layout) {
        super(layout);
        setOpaque(true);
        try (InputStream is = getClass().getResourceAsStream("/images/exam/background.jpg")) {
            if (is != null) {
                bgImage = ImageIO.read(is);
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();
        if (bgImage != null) {
            // Scale to fill, centre-crop
            double scaleX = (double) w / bgImage.getWidth();
            double scaleY = (double) h / bgImage.getHeight();
            double scale  = Math.max(scaleX, scaleY);
            int iw = (int) (bgImage.getWidth()  * scale);
            int ih = (int) (bgImage.getHeight() * scale);
            int ox = (w - iw) / 2;
            int oy = (h - ih) / 2;
            g2.drawImage(bgImage, ox, oy, iw, ih, null);
        } else {
            // Gradient fallback
            GradientPaint gp = new GradientPaint(0, 0, GRAD_TOP, 0, h, GRAD_BOTTOM);
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);
        }
        g2.dispose();
        // Let children paint on top
        super.paintComponent(g);
    }

    @Override
    public boolean isOpaque() { return false; }
}
