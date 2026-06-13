package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import java.awt.*;

/**
 * ExamFooterStatusBar – Thin bottom status bar for exam taking screen.
 *
 * Design:
 *  - Light translucent bar, ~32px tall
 *  - Push-right layout: VIE / wifi / volume / BatteryStatusIcon / power
 *  - BatteryStatusIcon drawn via Graphics2D (no static icon)
 *  - Power button calls onExit (dispose, never System.exit)
 *
 * The bar is pure Swing, sits outside JCEF.
 */
public class ExamFooterStatusBar extends JPanel {

    private static final Color BAR_BG     = new Color(228, 231, 235); // 10% darker gray, fully opaque
    private static final Color BORDER_TOP = new Color(0, 0, 0, 12);
    private static final Color ICON_COLOR = Color.decode("#1E3A5F");
    private static final Color POWER_RED  = Color.decode("#C62828");

    private final JButton btnLanguage;
    private final JButton btnPower;

    public ExamFooterStatusBar(Runnable onExit) {
        this("VIE", null, onExit);
    }

    public ExamFooterStatusBar(String languageLabel, Runnable onLanguageToggle, Runnable onExitRequest) {
        setLayout(new MigLayout("insets 4 16, fillx, aligny center", "push[right]", "[24!]"));
        setBackground(BAR_BG);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_TOP));

        JPanel icons = new JPanel(new MigLayout("insets 0, gap 14, aligny center"));
        icons.setOpaque(false);

        // Language label
        btnLanguage = dimTextButton(languageLabel);
        btnLanguage.addActionListener(e -> {
            if (onLanguageToggle != null) {
                onLanguageToggle.run();
            }
        });
        icons.add(btnLanguage);

        // Wifi icon
        icons.add(dimSvgLabel("images/exam/icons/wifi.svg"));

        // Volume icon
        icons.add(dimSvgLabel("images/exam/icons/volume-2.svg"));

        // Battery – custom Graphics2D component (98%, charging)
        BatteryStatusIcon battery = new BatteryStatusIcon();
        battery.setBatteryPercent(98);
        battery.setCharging(true);
        battery.setToolTipText("98% – Đang sạc");
        icons.add(battery, "w 28!, h 14!, aligny center");

        // Power button
        btnPower = new JButton(ExamLoginMockPanel.loadSVG("images/exam/icons/power.svg", 16, POWER_RED));
        btnPower.setContentAreaFilled(false);
        btnPower.setBorderPainted(false);
        btnPower.setFocusPainted(false);
        btnPower.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPower.addActionListener(e -> {
            if (onExitRequest == null) {
                return;
            }
            onExitRequest.run();
        });
        icons.add(btnPower);

        add(icons, "cell 0 0");
    }

    public void applyLanguage(TSELanguageManager languageManager) {
        setLanguageLabel(languageManager.getFooterLabel());
        btnLanguage.setToolTipText(languageManager.text("language.tooltip"));
        btnPower.setToolTipText(languageManager.text("power.tooltip"));
    }

    public void applyInputMode(TSEInputModeManager inputModeManager, TSELanguageManager languageManager) {
        setLanguageLabel(inputModeManager.getFooterLabel());
        btnLanguage.setToolTipText(languageManager.text("language.tooltip"));
        btnPower.setToolTipText(languageManager.text("power.tooltip"));
    }

    public void setLanguageLabel(String text) {
        btnLanguage.setText(text);
        btnLanguage.revalidate();
        btnLanguage.repaint();
    }

    private JButton dimTextButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(ICON_COLOR);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(0, 0, 0, 0));
        return button;
    }

    private JLabel dimSvgLabel(String svgPath) {
        JLabel lbl = new JLabel(ExamLoginMockPanel.loadSVG(svgPath, 16, ICON_COLOR));
        return lbl;
    }
}
