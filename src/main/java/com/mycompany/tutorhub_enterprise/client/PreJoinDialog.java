package com.mycompany.tutorhub_enterprise.client;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PreJoinDialog extends JDialog {

    private final String title;
    private final String role;
    private final Runnable onJoin;
    
    private final Color background = TutorHubTheme.BACKGROUND;
    private final Color surface = TutorHubTheme.SURFACE;
    private final Color primaryBlue = TutorHubTheme.PRIMARY_BLUE;
    private final Color textDark = TutorHubTheme.TEXT_DARK;
    private final Color textMuted = TutorHubTheme.TEXT_MUTED;
    private final Color border = TutorHubTheme.BORDER;

    public PreJoinDialog(Window owner, String title, String role, Runnable onJoin) {
        super(owner, "Chuẩn bị vào lớp", ModalityType.APPLICATION_MODAL);
        this.title = title;
        this.role = role;
        this.onJoin = onJoin;
        
        setSize(480, 360);
        setLocationRelativeTo(owner);
        setResizable(false);
        getContentPane().setBackground(background);
        
        initUI();
    }
    
    private void initUI() {
        JPanel container = new JPanel(new MigLayout("insets 24, fillx, wrap 1", "[grow,fill]", "[]16[]24[]"));
        container.setBackground(background);
        
        // Header
        JLabel titleLabel = new JLabel("Chuẩn bị vào lớp");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(textDark);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel subtitleLabel = new JLabel("Bạn sắp tham gia vào phiên học trực tuyến");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(textMuted);
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JPanel headerPanel = new JPanel(new MigLayout("insets 0, fillx, wrap 1", "[grow,fill]", "[]4[]"));
        headerPanel.setOpaque(false);
        headerPanel.add(titleLabel);
        headerPanel.add(subtitleLabel);
        
        // Info Box
        JPanel infoBox = new ElevatedPanel(12, surface);
        infoBox.setLayout(new MigLayout("insets 16, fillx, wrap 1", "[grow,fill]", "[]8[]8[]"));
        infoBox.setBorder(BorderFactory.createLineBorder(border));
        
        String displayTitle = title != null && !title.trim().isEmpty() ? title : "Lop hoc chua dat ten";
        JLabel lblClassName = new JLabel(displayTitle);
        lblClassName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblClassName.setForeground(textDark);
        
        JLabel lblRole = new JLabel("Vai trò của bạn: " + ("teacher".equals(role) ? "Giáo viên" : "Học viên"));
        lblRole.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblRole.setForeground(textMuted);
        
        infoBox.add(lblClassName);
        infoBox.add(lblRole);
        
        // Notice
        JPanel noticePanel = new JPanel(new BorderLayout(8, 0));
        noticePanel.setOpaque(false);
        noticePanel.setBorder(new EmptyBorder(8, 0, 0, 0));
        JLabel lblNoticeIcon = new JLabel("⚠️"); // Can use an icon later
        JLabel lblNoticeText = new JLabel("<html>Vui lòng kiểm tra micro và camera của bạn. Trình duyệt sẽ yêu cầu quyền truy cập khi bạn vào lớp.</html>");
        lblNoticeText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblNoticeText.setForeground(textMuted);
        noticePanel.add(lblNoticeIcon, BorderLayout.WEST);
        noticePanel.add(lblNoticeText, BorderLayout.CENTER);
        infoBox.add(noticePanel);

        // Footer Actions
        JPanel actionPanel = new JPanel(new MigLayout("insets 0, align right", "[][]", "[]"));
        actionPanel.setOpaque(false);
        
        JButton btnCancel = createButton("Hủy", surface, textDark, border);
        btnCancel.addActionListener(e -> dispose());
        
        JButton btnJoin = createButton("Vào phòng", primaryBlue, Color.WHITE, primaryBlue);
        btnJoin.addActionListener(e -> {
            dispose();
            if (onJoin != null) onJoin.run();
        });
        
        actionPanel.add(btnCancel, "w 80!, h 36!");
        actionPanel.add(btnJoin, "w 120!, h 36!");
        
        container.add(headerPanel);
        container.add(infoBox);
        container.add(actionPanel);
        
        add(container);
    }
    
    private JButton createButton(String text, Color bg, Color fg, Color borderColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(bg.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(bg.brighter());
                } else {
                    g2.setColor(bg);
                }
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(borderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}

