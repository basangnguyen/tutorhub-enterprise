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

    public ExamFooterStatusBar(Runnable onExit) {
        setLayout(new MigLayout("insets 4 16, fillx, aligny center", "push[right]", "[24!]"));
        setBackground(BAR_BG);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_TOP));

        JPanel icons = new JPanel(new MigLayout("insets 0, gap 14, aligny center"));
        icons.setOpaque(false);

        // Language label
        icons.add(dimLabel("VIE"));

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
        JButton btnPower = new JButton(ExamLoginMockPanel.loadSVG("images/exam/icons/power.svg", 16, POWER_RED));
        btnPower.setContentAreaFilled(false);
        btnPower.setBorderPainted(false);
        btnPower.setFocusPainted(false);
        btnPower.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPower.addActionListener(e -> {
            if (onExit != null) {
                JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
                ExitConfirmDialog dlg = new ExitConfirmDialog(parent, onExit);
                dlg.setVisible(true);
            }
        });
        icons.add(btnPower);

        add(icons, "cell 0 0");
    }

    private JLabel dimLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(ICON_COLOR);
        return lbl;
    }

    private JLabel dimSvgLabel(String svgPath) {
        JLabel lbl = new JLabel(ExamLoginMockPanel.loadSVG(svgPath, 16, ICON_COLOR));
        return lbl;
    }
}
