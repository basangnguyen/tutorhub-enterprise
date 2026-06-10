package com.mycompany.tutorhub_enterprise.client;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;

import javax.swing.JPanel;

public class RoundedImagePanel extends JPanel {
    public enum Fit {
        COVER,
        CONTAIN
    }

    private Image image;
    private Fit fit;
    private int radius;
    private Color backgroundColor = Color.decode("#F2F4FA");
    private Color overlayColor;
    private Color borderColor;
    private String fallbackText;
    private Color fallbackColor = TutorHubTheme.PRIMARY;
    private Font fallbackFont = TutorHubTheme.font(Font.BOLD, 30);

    public RoundedImagePanel(int width, int height, int radius, Fit fit) {
        this.radius = radius;
        this.fit = fit;
        setLayout(new java.awt.BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(width, height));
    }

    public void setImage(Image image) {
        this.image = image;
        repaint();
    }

    public void setFit(Fit fit) {
        this.fit = fit;
        repaint();
    }

    public void setRadius(int radius) {
        this.radius = radius;
        repaint();
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
        repaint();
    }

    public void setOverlayColor(Color overlayColor) {
        this.overlayColor = overlayColor;
        repaint();
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }

    public void setFallbackText(String fallbackText, Color fallbackColor) {
        this.fallbackText = fallbackText;
        this.fallbackColor = fallbackColor;
        repaint();
    }

    public void setFallbackFont(Font fallbackFont) {
        this.fallbackFont = fallbackFont;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        Shape oldClip = g2.getClip();
        Shape clip = new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius);
        g2.setClip(clip);
        g2.setColor(backgroundColor);
        g2.fillRect(0, 0, getWidth(), getHeight());

        if (image != null && image.getWidth(null) > 0 && image.getHeight(null) > 0) {
            drawImage(g2);
            if (overlayColor != null) {
                g2.setColor(overlayColor);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        } else {
            drawFallback(g2);
        }

        g2.setClip(oldClip);
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        }
        g2.dispose();
    }

    private void drawImage(Graphics2D g2) {
        double sx = (double) getWidth() / image.getWidth(null);
        double sy = (double) getHeight() / image.getHeight(null);
        double scale = fit == Fit.CONTAIN ? Math.min(sx, sy) : Math.max(sx, sy);
        int drawW = (int) Math.ceil(image.getWidth(null) * scale);
        int drawH = (int) Math.ceil(image.getHeight(null) * scale);
        int x = (getWidth() - drawW) / 2;
        int y = (getHeight() - drawH) / 2;
        g2.drawImage(image, x, y, drawW, drawH, null);
    }

    private void drawFallback(Graphics2D g2) {
        g2.setColor(Color.decode("#E2E5F0"));
        for (int y = 20; y < getHeight(); y += 24) {
            g2.drawLine(0, y, getWidth(), y);
        }
        for (int x = 24; x < getWidth(); x += 32) {
            g2.drawLine(x, 0, x, getHeight());
        }
        if (fallbackText != null && !fallbackText.isBlank()) {
            g2.setFont(fallbackFont);
            g2.setColor(fallbackColor);
            int textW = g2.getFontMetrics().stringWidth(fallbackText);
            int textY = (getHeight() + g2.getFontMetrics().getAscent()) / 2 - 4;
            g2.drawString(fallbackText, (getWidth() - textW) / 2, textY);
        }
    }
}
