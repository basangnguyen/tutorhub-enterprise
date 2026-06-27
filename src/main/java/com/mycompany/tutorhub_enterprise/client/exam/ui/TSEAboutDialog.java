package com.mycompany.tutorhub_enterprise.client.exam.ui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.time.Year;

/**
 * Compact About/Help dialog for Secure Exam parent screens.
 */
public final class TSEAboutDialog extends JDialog {

    private static final String PRODUCT_NAME = "TutorHub Secure Exam";
    private static final String PRODUCT_VERSION = "tutorhub_tse_v1";
    private static final String FOUNDER = "Nguyễn Bá Sáng";

    private static final Color CARD_BG = Color.WHITE;
    private static final Color SECTION_BG = Color.decode("#F8FAFC");
    private static final Color BORDER = Color.decode("#D7DCE8");
    private static final Color SOFT_BORDER = Color.decode("#E7ECF5");
    private static final Color TITLE = Color.decode("#102A56");
    private static final Color TEXT = Color.decode("#1E293B");
    private static final Color MUTED = Color.decode("#64748B");
    private static final Color BLUE = Color.decode("#2563EB");

    public static void showDialog(Component parent, String screenName) {
        Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        TSEAboutDialog dialog = new TSEAboutDialog(owner, screenName);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private TSEAboutDialog(Window owner, String screenName) {
        super(owner, PRODUCT_NAME, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setResizable(false);
        setAlwaysOnTop(true);
        setBackground(new Color(0, 0, 0, 0));

        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 23, 42, 30));
                g2.fillRoundRect(8, 10, getWidth() - 16, getHeight() - 16, 26, 26);
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 8, getHeight() - 8, 22, 22));
                g2.setColor(BORDER);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 9, getHeight() - 9, 22, 22));
                g2.dispose();
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };
        root.setBorder(BorderFactory.createEmptyBorder(20, 22, 28, 30));
        root.add(buildContent(screenName), BorderLayout.CENTER);
        bindEscapeToClose(root);

        setContentPane(root);
        setSize(560, 430);
        setMinimumSize(new Dimension(560, 430));
    }

    private JComponent buildContent(String screenName) {
        JPanel content = new JPanel(new MigLayout("wrap 1, insets 0, gapy 12", "[grow,fill]"));
        content.setOpaque(false);

        content.add(buildHeader(), "growx");
        content.add(buildMetaStrip(screenName), "growx");
        content.add(buildSummary(), "growx");
        content.add(buildFooter(), "growx, pushy, aligny bottom");

        return content;
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new MigLayout("insets 0, fillx, gapx 12", "[48!][grow][28!]"));
        header.setOpaque(false);

        header.add(new LogoIconLabel(46), "w 46!, h 46!");

        JPanel titleBlock = new JPanel(new MigLayout("wrap 1, insets 0, gapy 2", "[grow]"));
        titleBlock.setOpaque(false);

        JLabel title = new JLabel(PRODUCT_NAME);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TITLE);

        JLabel subtitle = new JLabel("Ứng dụng thi an toàn cho TutorHub");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(MUTED);

        titleBlock.add(title, "growx");
        titleBlock.add(subtitle, "growx");
        header.add(titleBlock, "growx, aligny center");

        JButton close = ExamIconFactory.closeButton();
        close.addActionListener(e -> dispose());
        header.add(close, "aligny top, w 26!, h 26!");
        return header;
    }

    private JComponent buildMetaStrip(String screenName) {
        RoundedPanel panel = new RoundedPanel(Color.decode("#F5F8FF"), Color.decode("#DDE7FF"), 16);
        panel.setLayout(new MigLayout("insets 10 12, fillx, gapx 12", "[grow][grow]"));

        panel.add(metaLine("Phiên bản", PRODUCT_VERSION), "growx");
        panel.add(metaLine("Nhà sáng lập", FOUNDER), "growx, wrap");
        panel.add(metaLine("Màn hình", safeValue(screenName)), "span 2, growx");
        return panel;
    }

    private JComponent buildSummary() {
        RoundedPanel panel = new RoundedPanel(SECTION_BG, SOFT_BORDER, 18);
        panel.setLayout(new MigLayout("wrap 1, insets 14 16, gapy 10", "[grow,fill]"));

        panel.add(sectionTitle("Giới thiệu"), "growx");
        panel.add(textBlock("TSE hỗ trợ đăng nhập, chọn bài, làm bài và nộp bài trong môi trường thi được kiểm soát."), "growx");

        panel.add(sectionTitle("Chức năng chính"), "growx, gaptop 2");
        panel.add(shortList(
                "Đăng nhập hệ thống và chọn bài thi",
                "Làm bài trong chế độ thi an toàn",
                "Tự lưu bài, nộp bài cuối và dùng quick settings"
        ), "growx");

        panel.add(sectionTitle("Cách sử dụng"), "growx, gaptop 2");
        panel.add(textBlock("1. Đăng nhập  2. Chọn bài thi  3. Làm bài  4. Kiểm tra và nộp bài"), "growx");
        return panel;
    }

    private JLabel sectionTitle(String value) {
        JLabel label = new JLabel(value);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(TITLE);
        return label;
    }

    private JLabel textBlock(String value) {
        JLabel label = new JLabel("<html><div style='width:455px; line-height:1.32;'>"
                + escapeHtml(value) + "</div></html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(Color.decode("#475569"));
        return label;
    }

    private JComponent shortList(String... items) {
        JPanel list = new JPanel(new MigLayout("wrap 1, insets 0, gapy 6", "[grow]"));
        list.setOpaque(false);
        for (String item : items) {
            JPanel row = new JPanel(new MigLayout("insets 0, gapx 8, fillx", "[16!][grow]"));
            row.setOpaque(false);
            row.add(new JLabel(ExamIconFactory.checkIcon(14)), "aligny top");

            JLabel text = new JLabel("<html><div style='width:430px;'>"
                    + escapeHtml(item) + "</div></html>");
            text.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            text.setForeground(TEXT);
            row.add(text, "growx");
            list.add(row, "growx");
        }
        return list;
    }

    private JComponent metaLine(String label, String value) {
        JPanel line = new JPanel(new MigLayout("insets 0, gapx 6", "[][grow]"));
        line.setOpaque(false);

        JLabel left = new JLabel(label + ":");
        left.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        left.setForeground(MUTED);

        JLabel right = new JLabel(value);
        right.setFont(new Font("Segoe UI", Font.BOLD, 12));
        right.setForeground(TEXT);

        line.add(left);
        line.add(right, "growx");
        return line;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new MigLayout("insets 0, fillx", "[grow][108!]"));
        footer.setOpaque(false);

        JLabel copyright = new JLabel("© " + Year.now().getValue() + " TutorHub Enterprise");
        copyright.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        copyright.setForeground(MUTED);

        JButton close = new JButton("Đóng");
        close.setFont(new Font("Segoe UI", Font.BOLD, 13));
        close.setBackground(BLUE);
        close.setForeground(Color.WHITE);
        close.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addActionListener(e -> dispose());

        footer.add(copyright, "growx");
        footer.add(close, "w 108!, h 36!");
        return footer;
    }

    private void bindEscapeToClose(JComponent root) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "close-about");
        root.getActionMap().put("close-about", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        });
    }

    private String safeValue(String value) {
        return value == null || value.trim().isEmpty() ? "Secure Exam" : value.trim();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static final class RoundedPanel extends JPanel {
        private final Color fill;
        private final Color border;
        private final int radius;

        private RoundedPanel(Color fill, Color border, int radius) {
            this.fill = fill;
            this.border = border;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            if (border != null) {
                g2.setColor(border);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
