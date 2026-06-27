package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * A composite Windows 11-style quick settings tray cluster.
 * Unifies Wi-Fi, Volume, and Battery into a single button-like hoverable block.
 */
public class TSEQuickSettingsTrayCluster extends JPanel {

    private final JLabel lblWifi;
    private final JLabel lblVolume;
    private final BatteryStatusIcon battery;
    
    private boolean hovered = false;
    private boolean active = false;

    public TSEQuickSettingsTrayCluster(Consumer<JComponent> onClick) {
        setLayout(new MigLayout("insets 4 8, gap 6, aligny center"));
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        lblWifi = new JLabel();
        FlatSVGIcon wifiIcon = new FlatSVGIcon("images/exam/icons/wifi.svg", 16, 16);
        wifiIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> Color.decode("#1E3A5F")));
        lblWifi.setIcon(wifiIcon);

        lblVolume = new JLabel();
        FlatSVGIcon volIcon = new FlatSVGIcon("images/exam/icons/volume-2.svg", 16, 16);
        volIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> Color.decode("#1E3A5F")));
        lblVolume.setIcon(volIcon);

        battery = new BatteryStatusIcon(Color.decode("#1E3A5F"));
        
        add(lblWifi);
        add(lblVolume);
        add(battery);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            @Override
            public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            @Override
            public void mousePressed(MouseEvent e) { active = true; repaint(); }
            @Override
            public void mouseReleased(MouseEvent e) {
                active = false;
                repaint();
                // If the user releases mouse inside the bounds
                if (onClick != null && TSEQuickSettingsTrayCluster.this.contains(SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), TSEQuickSettingsTrayCluster.this))) {
                    System.out.println("[TSE_TRAY_CLUSTER] Quick Settings cluster clicked");
                    onClick.accept(TSEQuickSettingsTrayCluster.this);
                }
            }
        };

        addMouseListener(mouseAdapter);
        lblWifi.addMouseListener(mouseAdapter);
        lblVolume.addMouseListener(mouseAdapter);
        battery.addMouseListener(mouseAdapter);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (active) {
            g2.setColor(new Color(255, 255, 255, 30));
        } else if (hovered) {
            g2.setColor(new Color(255, 255, 255, 15));
        } else {
            g2.setColor(new Color(0, 0, 0, 0));
        }
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        g2.dispose();
        super.paintComponent(g);
    }

    public void updateWifi(boolean hasAdapter, boolean connected, String tooltip) {
        FlatSVGIcon icon;
        if (!hasAdapter || !connected) {
            icon = new FlatSVGIcon("images/exam/icons/wifi-off.svg", 16, 16);
        } else {
            icon = new FlatSVGIcon("images/exam/icons/wifi.svg", 16, 16);
        }
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> Color.decode("#1E3A5F")));
        lblWifi.setIcon(icon);
        lblWifi.setToolTipText(tooltip);
    }

    public void updateVolume(boolean supported, boolean muted, int percent, String tooltip) {
        FlatSVGIcon icon;
        if (!supported || muted || percent == 0) {
            icon = new FlatSVGIcon("images/exam/icons/volume-x.svg", 16, 16);
        } else {
            icon = new FlatSVGIcon("images/exam/icons/volume-2.svg", 16, 16);
        }
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> Color.decode("#1E3A5F")));
        lblVolume.setIcon(icon);
        lblVolume.setToolTipText(tooltip);
    }

    public void updateBattery(boolean hasBattery, boolean charging, int percent, String tooltip) {
        battery.setVisible(true);
        if (hasBattery) {
            battery.setBatteryPercent(percent);
            battery.setCharging(charging);
        } else {
            battery.setBatteryPercent(100);
            battery.setCharging(true);
        }
        battery.setToolTipText(tooltip);
    }
}
