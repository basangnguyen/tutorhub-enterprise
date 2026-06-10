package com.mycompany.tutorhub_enterprise.client;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

public class ElevatedPanel extends JPanel {
    private final int radius;
    private final Color fill;

    public ElevatedPanel(int radius, Color fill) {
        this.radius = radius;
        this.fill = fill;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(31, 41, 55, 14));
        g2.fillRoundRect(1, 3, Math.max(0, getWidth() - 2), Math.max(0, getHeight() - 4), radius, radius);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, Math.max(0, getWidth() - 1), Math.max(0, getHeight() - 2), radius, radius);
        g2.setColor(TutorHubTheme.BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(0, 0, Math.max(0, getWidth() - 2), Math.max(0, getHeight() - 3), radius, radius);
        g2.dispose();
        super.paintComponent(g);
    }
}
