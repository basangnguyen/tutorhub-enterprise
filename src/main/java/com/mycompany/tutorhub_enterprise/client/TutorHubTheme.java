package com.mycompany.tutorhub_enterprise.client;

import java.awt.Color;
import java.awt.Font;

public final class TutorHubTheme {
    public static final String FONT_FAMILY = "Segoe UI";

    public static final Color BACKGROUND = Color.decode("#F5F6FA");
    public static final Color SURFACE = Color.WHITE;
    public static final Color PRIMARY = Color.decode("#6D5DF6");
    public static final Color PRIMARY_BLUE = Color.decode("#2563EB");
    public static final Color SUCCESS = Color.decode("#12A978");
    public static final Color TEXT_DARK = Color.decode("#101114");
    public static final Color TEXT_MUTED = Color.decode("#6F7684");
    public static final Color BORDER = Color.decode("#E7E9F0");
    public static final Color HOVER_BORDER = Color.decode("#C9C3FF");

    public static final int RADIUS_CARD = 16;
    public static final int RADIUS_ACTION_CARD = 18;
    public static final int RADIUS_CONTROL = 12;
    public static final int ACTION_CARD_HEIGHT = 84;
    public static final int ACTION_ICON_BOX = 42;
    public static final int ACTION_ICON_SIZE = 34;
    public static final int ACTION_VISUAL_WIDTH = 92;
    public static final int ACTION_VISUAL_HEIGHT = 58;
    public static final int SHADOW_ALPHA = 8;
    public static final int SHADOW_ALPHA_HOVER = 14;

    private TutorHubTheme() {
    }

    public static Font font(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    public static Color alpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
