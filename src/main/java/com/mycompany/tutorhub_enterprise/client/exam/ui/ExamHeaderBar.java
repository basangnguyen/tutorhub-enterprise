package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import java.awt.*;

/**
 * ExamHeaderBar – Top header for the exam taking screen.
 *
 * Design (ref: d1e147be…png):
 *  - White background, subtle bottom shadow/border
 *  - Left:   small refresh icon ↻ (blue) + exam title text
 *  - Center: "Thời gian còn lại: 45:00" red bold
 *  - Right:  navy rounded "Nộp Bài" button
 *
 * Height: ~56px. Lives in Swing – not inside JCEF.
 */
public class ExamHeaderBar extends JPanel {

    private static final Color ACCENT_BLUE = Color.decode("#2563EB");
    private static final Color NAVY        = Color.decode("#1E3A7A");
    private static final Color TIMER_RED   = Color.decode("#DC2626");
    private static final Color TEXT_DARK   = Color.decode("#1A1A2E");

    public ExamHeaderBar(String examName, Runnable onSubmit) {
        setLayout(new MigLayout(
            "insets 8 16, fillx, aligny center",
            "[left][center, grow][right]",
            "[40!]"
        ));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Color.decode("#E5E7EB")),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // ── Left: refresh icon + exam title ──────────────────
        JPanel left = new JPanel(new MigLayout("insets 0, gap 10, aligny center"));
        left.setOpaque(false);

        JButton btnRefresh = new JButton(ExamLoginMockPanel.loadSVG("images/exam/icons/refresh-cw.svg", 20, ACCENT_BLUE));
        btnRefresh.setContentAreaFilled(false);
        btnRefresh.setBorderPainted(false);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel lblName = new JLabel(examName);
        lblName.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblName.setForeground(TEXT_DARK);

        left.add(btnRefresh);
        left.add(lblName);
        add(left, "cell 0 0");

        // ── Center: timer ─────────────────────────────────────
        JLabel lblTimer = new JLabel("Thời gian còn lại: 45:00");
        lblTimer.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTimer.setForeground(TIMER_RED);
        add(lblTimer, "cell 1 0");

        // ── Right: submit button ──────────────────────────────
        JButton btnSubmit = new JButton("Nộp Bài") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(NAVY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                // draw text on top
                FontMetrics fm = g.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g.setColor(Color.WHITE);
                g.setFont(getFont());
                g.drawString(getText(), tx, ty);
            }
        };
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setContentAreaFilled(false);
        btnSubmit.setBorderPainted(false);
        btnSubmit.setFocusPainted(false);
        btnSubmit.setOpaque(false);
        btnSubmit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSubmit.addActionListener(e -> { if (onSubmit != null) onSubmit.run(); });
        add(btnSubmit, "cell 2 0, w 110!, h 38!");
    }
}
