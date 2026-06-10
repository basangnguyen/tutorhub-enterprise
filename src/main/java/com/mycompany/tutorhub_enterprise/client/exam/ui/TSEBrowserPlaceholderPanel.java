package com.mycompany.tutorhub_enterprise.client.exam.ui;

import javax.swing.*;
import java.awt.*;

/**
 * TSEBrowserPlaceholderPanel – Center content area (JCEF placeholder).
 *
 * Design (ref: d1e147be…png):
 *  - Solid white background – CLEAN, no noisy placeholder text
 *  - Very faint ghost text centered to mark where JCEF will live
 *
 * When Phase 3 arrives, replace this panel with the real JCEF component.
 * NO logic, NO threads, NO network calls here.
 */
public class TSEBrowserPlaceholderPanel extends JPanel {

    public TSEBrowserPlaceholderPanel() {
        setLayout(new GridBagLayout());
        setBackground(Color.WHITE);

        // Ghost label – very faint, won't distract from real exam content
        JLabel ghost = new JLabel("[ Vùng hiển thị đề thi – JCEF Browser ]", SwingConstants.CENTER);
        ghost.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        ghost.setForeground(new Color(200, 200, 210)); // near-invisible
        add(ghost);
    }
}
