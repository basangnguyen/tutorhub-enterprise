package com.mycompany.tutorhub_enterprise.client;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;

class ActionCardVisualPanel extends JPanel {
    private final FlatSVGIcon visualIcon;
    private final Color accent;

    ActionCardVisualPanel(String mark, Color accent) {
        this.visualIcon = new FlatSVGIcon(resourceFor(mark), TutorHubTheme.ACTION_VISUAL_WIDTH, TutorHubTheme.ACTION_VISUAL_HEIGHT);
        this.accent = accent;
        setOpaque(false);
        Dimension size = new Dimension(TutorHubTheme.ACTION_VISUAL_WIDTH, TutorHubTheme.ACTION_VISUAL_HEIGHT);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Shape oldClip = g2.getClip();
        g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));

        boolean hover = Boolean.TRUE.equals(getClientProperty("hover"));
        g2.setColor(TutorHubTheme.alpha(accent, hover ? 16 : 10));
        g2.fillRoundRect(3, 7, getWidth() - 6, getHeight() - 11, 18, 18);

        int x = Math.max(0, (getWidth() - visualIcon.getIconWidth()) / 2);
        int y = Math.max(0, (getHeight() - visualIcon.getIconHeight()) / 2 + (hover ? -1 : 0));
        visualIcon.paintIcon(this, g2, x, y);

        g2.setClip(oldClip);
        g2.dispose();
        super.paintComponent(g);
    }

    private static String resourceFor(String mark) {
        if ("+".equals(mark)) {
            return "images/action/create-visual.svg";
        }
        if ("PL".equals(mark)) {
            return "images/action/public-visual.svg";
        }
        return "images/action/join-visual.svg";
    }
}
