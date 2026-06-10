package com.mycompany.tutorhub_enterprise.client;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class SidebarPanel extends JPanel {

    private final Color BG_COLOR = Color.WHITE;
    private final Color PRIMARY = Color.decode("#7C3AED");      
    private final Color PRIMARY_BG = Color.decode("#F3E8FF");   
    private final Color TEXT_MUTED = Color.decode("#64748B");   
    private final Color HOVER_BG = Color.decode("#F8FAFC");     

    private final List<NavButton> navButtons = new ArrayList<>();

    public SidebarPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        setPreferredSize(new Dimension(80, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.decode("#E2E8F0")));

        JPanel menuContainer = new JPanel();
        menuContainer.setLayout(new BoxLayout(menuContainer, BoxLayout.Y_AXIS));
        menuContainer.setOpaque(false);
        menuContainer.setBorder(new EmptyBorder(24, 0, 24, 0));

        // 1. LOGO (Icon Only)
        JPanel logoPanel = new JPanel();
        logoPanel.setLayout(new BoxLayout(logoPanel, BoxLayout.Y_AXIS));
        logoPanel.setOpaque(false);
        logoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel lblLogo = new JLabel();
        FlatSVGIcon logoIcon = new FlatSVGIcon("images/icon_svg/book-open.svg", 32, 32);
        logoIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> PRIMARY));
        lblLogo.setIcon(logoIcon);
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoPanel.add(lblLogo);
        menuContainer.add(logoPanel);
        menuContainer.add(Box.createVerticalStrut(32));

        // 2. CÁC NÚT ĐIỀU HƯỚNG
        NavButton btnFeed = createNavButton("home", 0);
        btnFeed.setActive(true);
        menuContainer.add(btnFeed);
        menuContainer.add(createNavButton("bookmark", 0));
        menuContainer.add(createNavButton("briefcase", 0));
        menuContainer.add(createNavButton("calendar", 0));
        
        // Chat badge 2
        NavButton btnChat = createNavButton("message-circle", 2);
        menuContainer.add(btnChat);
        
        menuContainer.add(createNavButton("video", 0));
        menuContainer.add(createNavButton("file-text", 0));
        menuContainer.add(createNavButton("user", 0));
        menuContainer.add(createNavButton("check-circle-2", 0));

        JScrollPane scrollPane = new JScrollPane(menuContainer);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);
        
        // Settings/Diamond button at bottom
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(16, 0, 24, 0));
        bottomPanel.add(createNavButton("wallet", 0));
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private NavButton createNavButton(String svgName, int badgeCount) {
        NavButton btn = new NavButton(svgName, badgeCount);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        navButtons.add(btn);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (NavButton b : navButtons) {
                    b.setActive(false);
                }
                btn.setActive(true);
            }
        });
        return btn;
    }

    class NavButton extends JPanel {
        private final JLabel lblIcon;
        private final String svgName;
        private final int badgeCount;
        private boolean isActive = false;
        private boolean isHover = false;
        private FlatSVGIcon svgIcon;

        public NavButton(String svgName, int badgeCount) {
            this.svgName = svgName;
            this.badgeCount = badgeCount;

            setLayout(new GridBagLayout());
            setOpaque(false);
            setPreferredSize(new Dimension(56, 56));
            setMaximumSize(new Dimension(56, 56));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            lblIcon = new JLabel();
            svgIcon = new FlatSVGIcon("images/icon_svg/" + svgName + ".svg", 24, 24);
            svgIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> TEXT_MUTED));
            lblIcon.setIcon(svgIcon);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0; gbc.gridy = 0;
            add(lblIcon, gbc);

            if (badgeCount > 0) {
                JPanel badgePanel = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(PRIMARY); 
                        g2.fillOval(0, 0, getWidth(), getHeight());
                        g2.dispose();
                    }
                };
                badgePanel.setOpaque(false);
                badgePanel.setPreferredSize(new Dimension(18, 18));
                badgePanel.setLayout(new GridBagLayout());
                
                JLabel lblBadge = new JLabel(String.valueOf(badgeCount));
                lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
                lblBadge.setForeground(Color.WHITE);
                badgePanel.add(lblBadge);
                
                gbc.insets = new Insets(-16, 20, 0, 0);
                add(badgePanel, gbc);
            }

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { if (!isActive) { isHover = true; repaint(); } }
                @Override public void mouseExited(MouseEvent e) { if (!isActive) { isHover = false; repaint(); } }
            });
        }

        public void setActive(boolean active) {
            this.isActive = active;
            this.isHover = false;
            
            if (active) {
                svgIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> PRIMARY));
            } else {
                svgIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> TEXT_MUTED));
            }
            lblIcon.repaint();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = 48;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;

            if (isActive) {
                g2.setColor(PRIMARY_BG);
                g2.fillRoundRect(x, y, size, size, 16, 16);
            } else if (isHover) {
                g2.setColor(HOVER_BG);
                g2.fillRoundRect(x, y, size, size, 16, 16);
            }
            g2.dispose();
        }
    }
}
