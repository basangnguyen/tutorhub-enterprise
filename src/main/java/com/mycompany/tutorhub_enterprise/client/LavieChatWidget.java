package com.mycompany.tutorhub_enterprise.client;

import javazoom.jl.player.Player;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.UUID;

public class LavieChatWidget extends JDialog {
    private JPanel chatContainer;
    private JPanel chatMessagesPanel;
    private JTextField chatInput;
    private JButton btnSend;
    private JButton btnMic;
    private JButton btnImage;
    
    private boolean isChatVisible = false;
    private boolean isRecording = false;
    
    private TargetDataLine targetDataLine;
    private AudioFormat audioFormat;
    private ByteArrayOutputStream audioOutputStream;

    private JFrame mainFrame;
    private File stagedImageFile = null;
    private JLabel stagedImageLabel;
    
    private javax.swing.Timer idleFollowUpTimer;
    private boolean hasSentIdleFollowUp = false;
    private String lastContextStr = "generic";
    private static final java.util.concurrent.BlockingQueue<String> audioQueue = new java.util.concurrent.LinkedBlockingQueue<>();
    private static Thread audioPlayerThread = null;

    public LavieChatWidget(JFrame parentFrame) {
        super(parentFrame, false);
        setUndecorated(true);
        this.mainFrame = parentFrame;
        setAlwaysOnTop(true);
        setBackground(new java.awt.Color(0, 0, 0, 0));
        setFocusableWindowState(true);
        
        setSize(360, 520);
        
        chatContainer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int x = 12, y = 12;
                int w = getWidth() - 24;
                int h = getHeight() - 24;
                
                // Shadow
                for (int i = 0; i < 6; i++) {
                    g2.setColor(new java.awt.Color(0, 0, 0, 8 + i * 2));
                    g2.fillRoundRect(x - 6 + i, y - 6 + i, w + 12 - i*2, h + 12 - i*2, 28, 28);
                }
                
                // Dark Background tối và đục hơn
                g2.setColor(new java.awt.Color(60, 65, 80, 240));
                g2.fillRoundRect(x, y, w, h, 24, 24);
                
                // Thin Border
                g2.setColor(new java.awt.Color(255, 255, 255, 180));
                g2.drawRoundRect(x, y, w, h, 24, 24);
                
                g2.dispose();
            }
        };
        chatContainer.setLayout(new BorderLayout(0, 10));
        chatContainer.setOpaque(false);
        chatContainer.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        MouseAdapter ma = new MouseAdapter() {
            int pX, pY;
            public void mousePressed(MouseEvent e) { pX = e.getX(); pY = e.getY(); }
            public void mouseDragged(MouseEvent e) {
                setLocation(getLocation().x + e.getX() - pX, getLocation().y + e.getY() - pY);
            }
        };
        chatContainer.addMouseListener(ma);
        chatContainer.addMouseMotionListener(ma);
        
        // --- HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setOpaque(false);
        
        JPanel logoPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                try {
                    java.net.URL imgUrl = getClass().getResource("/images/logomoi.png");
                    if (imgUrl != null) {
                        Image img = new ImageIcon(imgUrl).getImage();
                        g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
                    } else {
                        g2.setColor(java.awt.Color.GRAY);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                g2.dispose();
            }
            @Override
            public Dimension getPreferredSize() { return new Dimension(40, 40); }
        };
        logoPanel.setOpaque(false);
        
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);
        
        final JLabel lblBrand = new JLabel(" Lavie AI");
        lblBrand.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblBrand.setForeground(java.awt.Color.WHITE);
        try {
            java.net.URL logoUrl = getClass().getResource("/images/logo/logo.png");
            if (logoUrl != null) {
                Image logoImg = new ImageIcon(logoUrl).getImage().getScaledInstance(26, 26, Image.SCALE_SMOOTH);
                lblBrand.setIcon(new ImageIcon(logoImg));
            }
        } catch (Exception e) {}
        
        final JButton btnToggleSidebar = new JButton();
        try {
            java.net.URL iconUrl = getClass().getResource("/images/icon/resize.png");
            if (iconUrl != null) {
                Image img = new ImageIcon(iconUrl).getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                btnToggleSidebar.setIcon(new ImageIcon(img));
            } else {
                btnToggleSidebar.setText("||");
            }
        } catch (Exception e) {}
        btnToggleSidebar.setContentAreaFilled(false);
        btnToggleSidebar.setBorderPainted(false);
        btnToggleSidebar.setFocusPainted(false);
        btnToggleSidebar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnToggleSidebar.setMargin(new Insets(0, 0, 0, 0));
        btnToggleSidebar.setVisible(false);
        
        titlePanel.add(lblBrand);
        
        JPanel headerLeft = new JPanel(new BorderLayout(10, 0));
        headerLeft.setOpaque(false);
        headerLeft.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerLeft.add(logoPanel, BorderLayout.WEST);
        headerLeft.add(titlePanel, BorderLayout.CENTER);
        headerLeft.add(btnToggleSidebar, BorderLayout.EAST);
        
        JPanel topRightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topRightButtons.setOpaque(false);
        JButton btnMin = createHeaderButton("-");
        JButton btnMax = createHeaderButton("⤢");
        btnMax.addActionListener(e -> {
            isExpanded = !isExpanded;
            btnMax.setText(isExpanded ? "⤡" : "⤢");
            if (isExpanded) {
                btnToggleSidebar.setVisible(true);
                normalBounds = getBounds();
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int targetW = 1100;
                int targetH = (int)(screenSize.height * 0.85);
                int targetX = (screenSize.width - targetW) / 2;
                int targetY = (screenSize.height - targetH) / 2;
                animateBounds(targetX, targetY, targetW, targetH);
                notifyLavieState("OPEN");
                if (sidebarPanel != null) {
                    sidebarPanel.setVisible(true);
                    headerLeft.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
                    headerLeft.setBorder(new EmptyBorder(15, 15, 10, 15));
                    sidebarPanel.add(headerLeft, 0);
                    sidebarPanel.revalidate();
                    sidebarPanel.repaint();
                }
            } else {
                btnToggleSidebar.setVisible(false);
                lblBrand.setVisible(true);
                logoPanel.setVisible(true);
                if (normalBounds != null) {
                    animateBounds(normalBounds.x, normalBounds.y, normalBounds.width, normalBounds.height);
                }
                notifyLavieState("CLOSED");
                if (sidebarPanel != null) {
                    sidebarPanel.setVisible(false);
                }
                headerLeft.setBorder(null);
                headerPanel.add(headerLeft, BorderLayout.WEST);
                headerPanel.revalidate();
                headerPanel.repaint();
            }
        });
        JButton btnClose = createHeaderButton("✕");
        btnClose.addActionListener(e -> setVisible(false));
        
        topRightButtons.add(btnMin);
        topRightButtons.add(btnMax);
        topRightButtons.add(btnClose);
        
        headerPanel.add(headerLeft, BorderLayout.WEST);
        headerPanel.add(topRightButtons, BorderLayout.EAST);
        
        // --- MESSAGES AREA ---
        chatMessagesPanel = new JPanel();
        chatMessagesPanel.setLayout(new BoxLayout(chatMessagesPanel, BoxLayout.Y_AXIS));
        chatMessagesPanel.setOpaque(false);
        JScrollPane scrollPane = new JScrollPane(chatMessagesPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        verticalBar.setPreferredSize(new Dimension(4, 0));
        verticalBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new java.awt.Color(255, 255, 255, 50);
                this.trackColor = new java.awt.Color(0, 0, 0, 0);
            }
            @Override
            protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() {
                JButton jb = new JButton();
                jb.setPreferredSize(new Dimension(0, 0));
                jb.setMinimumSize(new Dimension(0, 0));
                jb.setMaximumSize(new Dimension(0, 0));
                return jb;
            }
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(thumbColor);
                g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, 4, 4);
                g2.dispose();
            }
            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {}
        });
        
        // --- INPUT AREA ---
        JPanel inputWrapper = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new java.awt.Color(45, 50, 60, 200));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new java.awt.Color(100, 150, 255, 80));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        inputWrapper.setLayout(new BorderLayout(5, 5));
        inputWrapper.setOpaque(false);
        inputWrapper.setBorder(new EmptyBorder(10, 15, 10, 15));
        
        chatInput = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new java.awt.Color(153, 153, 153));
                    g2.setFont(getFont());
                    g2.drawString("Hỏi bất kỳ điều gì...", getInsets().left, g.getFontMetrics().getMaxAscent() + getInsets().top);
                    g2.dispose();
                }
            }
        };
        chatInput.setOpaque(false);
        chatInput.setBackground(new java.awt.Color(0,0,0,0));
        chatInput.setForeground(new java.awt.Color(230, 230, 230));
        chatInput.setBorder(new EmptyBorder(0, 5, 0, 5));
        chatInput.setCaretColor(java.awt.Color.WHITE);
        chatInput.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        JPanel inputToolsLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        inputToolsLeft.setOpaque(false);
        
        btnImage = createToolButton("/images/icon/gime.png");
        btnMic = createToolButton("/images/icon/microphone.png"); 
        
        inputToolsLeft.add(btnImage);
        inputToolsLeft.add(btnMic);
        
        btnSend = new JButton() {
            private Image iconImg = null;
            {
                try {
                    // Lấy icon telegram từ tài nguyên cục bộ
                    java.net.URL imgUrl = getClass().getResource("/images/telegram2.png");
                    if (imgUrl != null) {
                        iconImg = new ImageIcon(imgUrl).getImage();
                    }
                } catch(Exception e) {}
            }
            
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Nền tím nhạt
                g2.setColor(new java.awt.Color(200, 180, 255));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                
                if (iconImg != null) {
                    g2.drawImage(iconImg, 6, 6, getWidth()-12, getHeight()-12, null);
                } else {
                    g2.setColor(java.awt.Color.WHITE);
                    int[] xPoints = {8, 22, 8, 12};
                    int[] yPoints = {8, 15, 22, 15};
                    g2.fillPolygon(xPoints, yPoints, 4);
                }
                
                // Hiệu ứng viền sáng (glow) xung quanh nút gửi
                g2.setColor(new java.awt.Color(255, 255, 255, 60));
                g2.setStroke(new BasicStroke(3f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
                g2.setColor(new java.awt.Color(255, 255, 255, 200));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                
                g2.dispose();
            }
            @Override
            public Dimension getPreferredSize() { return new Dimension(32, 32); }
        };
        btnSend.setContentAreaFilled(false);
        btnSend.setBorderPainted(false);
        btnSend.setFocusPainted(false);
        btnSend.setForeground(java.awt.Color.WHITE);
        btnSend.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JPanel inputTopPanel = new JPanel(new BorderLayout());
        inputTopPanel.setOpaque(false);
        stagedImageLabel = new JLabel("");
        stagedImageLabel.setForeground(new java.awt.Color(200, 200, 255));
        stagedImageLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        stagedImageLabel.setVisible(false);
        inputTopPanel.add(stagedImageLabel, BorderLayout.NORTH);
        inputTopPanel.add(chatInput, BorderLayout.CENTER);
        
        inputWrapper.add(inputTopPanel, BorderLayout.NORTH);
        
        // Thêm đường kẻ ngang ngăn cách
        JPanel separator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new java.awt.Color(255, 255, 255, 30));
                g2.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
                g2.dispose();
            }
        };
        separator.setOpaque(false);
        separator.setPreferredSize(new Dimension(1, 10));
        
        inputWrapper.add(separator, BorderLayout.CENTER);
        
        JPanel bottomInputRow = new JPanel(new BorderLayout());
        bottomInputRow.setOpaque(false);
        bottomInputRow.add(inputToolsLeft, BorderLayout.WEST);
        bottomInputRow.add(btnSend, BorderLayout.EAST);
        
        inputWrapper.add(bottomInputRow, BorderLayout.SOUTH);
        
        JPanel inputBottomArea = new JPanel() {
            @Override
            public void doLayout() {
                int maxW = 850;
                int w = getWidth();
                int h = getHeight();
                int targetW = Math.min(w, maxW);
                int targetX = (w - targetW) / 2;
                if (getComponentCount() > 0) {
                    getComponent(0).setBounds(targetX, 0, targetW, h);
                }
            }
            @Override
            public Dimension getPreferredSize() {
                if (getComponentCount() > 0) return getComponent(0).getPreferredSize();
                return super.getPreferredSize();
            }
        };
        inputBottomArea.setOpaque(false);
        inputBottomArea.add(inputWrapper);

        JPanel mainChatArea = new JPanel(new BorderLayout(0, 10));
        mainChatArea.setOpaque(false);
        mainChatArea.add(headerPanel, BorderLayout.NORTH);
        mainChatArea.add(scrollPane, BorderLayout.CENTER);
        mainChatArea.add(inputBottomArea, BorderLayout.SOUTH);

        sidebarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new java.awt.Color(15, 23, 42, 240), getWidth(), getHeight(), new java.awt.Color(30, 41, 59, 230));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setOpaque(false);
        sidebarPanel.setPreferredSize(new Dimension(230, 0));
        sidebarPanel.setMinimumSize(new Dimension(80, 0));
        sidebarPanel.setVisible(false);
        sidebarPanel.setBorder(new javax.swing.border.MatteBorder(0, 0, 0, 1, new java.awt.Color(255, 255, 255, 20)));

        JPanel newChatPanel = new JPanel(new BorderLayout());
        newChatPanel.setOpaque(false);
        newChatPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        newChatPanel.setBorder(new EmptyBorder(15, 15, 0, 15));
        newChatPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton btnNewChat = new JButton("Đoạn chat mới");
        btnNewChat.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnNewChat.setForeground(java.awt.Color.WHITE);
        btnNewChat.setContentAreaFilled(false);
        btnNewChat.setBorderPainted(false);
        btnNewChat.setFocusPainted(false);
        btnNewChat.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNewChat.setMargin(new Insets(0, 0, 0, 0));
        btnNewChat.setIconTextGap(10);
        try {
            java.net.URL iconUrl = getClass().getResource("/images/icon/newcon.png");
            if (iconUrl != null) {
                Image img = new ImageIcon(iconUrl).getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                btnNewChat.setIcon(new ImageIcon(img));
            }
        } catch (Exception e) {}
        
        newChatPanel.add(btnNewChat, BorderLayout.WEST);


        JPanel searchChatPanel = new JPanel(new BorderLayout());
        searchChatPanel.setOpaque(false);
        searchChatPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        searchChatPanel.setBorder(new EmptyBorder(5, 15, 0, 15));
        searchChatPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        final JButton btnSearchChat = new JButton("Tìm kiếm đoạn chat");
        btnSearchChat.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnSearchChat.setForeground(java.awt.Color.WHITE);
        btnSearchChat.setContentAreaFilled(false);
        btnSearchChat.setBorderPainted(false);
        btnSearchChat.setFocusPainted(false);
        btnSearchChat.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSearchChat.setMargin(new Insets(0, 0, 0, 0));
        btnSearchChat.setIconTextGap(10);
        try {
            java.net.URL searchIconUrl = getClass().getResource("/images/icon/search.png");
            if (searchIconUrl != null) {
                Image img = new ImageIcon(searchIconUrl).getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                btnSearchChat.setIcon(new ImageIcon(img));
            } else {
                btnSearchChat.setText("Tìm kiếm đoạn chat");
            }
        } catch (Exception e) {}
        searchChatPanel.add(btnSearchChat, BorderLayout.WEST);

        sidebarPanel.add(newChatPanel);
        sidebarPanel.add(Box.createVerticalStrut(2));
        sidebarPanel.add(searchChatPanel);
        sidebarPanel.add(Box.createVerticalStrut(15));
        
        JPanel recentChatsHeader = new JPanel(new BorderLayout(10, 0));
        recentChatsHeader.setOpaque(false);
        recentChatsHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        recentChatsHeader.setBorder(new EmptyBorder(15, 15, 5, 20));
        recentChatsHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel lblRecent = new JLabel("Đoạn chat gần đây");
        lblRecent.setForeground(new java.awt.Color(150, 150, 160));
        lblRecent.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        JPanel separatorLine = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new java.awt.Color(255, 255, 255, 20));
                g.drawLine(5, getHeight() / 2, 60, getHeight() / 2);
            }
        };
        separatorLine.setOpaque(false);
        
        recentChatsHeader.add(lblRecent, BorderLayout.WEST);
        recentChatsHeader.add(separatorLine, BorderLayout.CENTER);
        
        sidebarPanel.add(recentChatsHeader);

        String[] mockHistory = {
            "Phân tích Code C++",
            "Luyện thi TOEIC",
            "Giải Tích 1 - Đạo hàm",
            "Cấu trúc dữ liệu",
            "Kế hoạch ôn tập",
            "Chỉnh màu nền"
        };
        for (String hist : mockHistory) {
            JPanel histPanel = new JPanel(new BorderLayout());
            histPanel.setOpaque(false);
            histPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            histPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            histPanel.setBorder(new EmptyBorder(5, 15, 5, 15));
            histPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            JLabel lblText = new JLabel(hist);
            lblText.setForeground(new java.awt.Color(220, 220, 230));
            lblText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lblText.setPreferredSize(new Dimension(170, 22));
            
            histPanel.add(lblText, BorderLayout.WEST);
            sidebarPanel.add(histPanel);
        }
        
        sidebarPanel.add(Box.createVerticalGlue());
        
        JPanel settingsSeparator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new java.awt.Color(255, 255, 255, 15));
                g.drawLine(15, getHeight() / 2, getWidth() - 15, getHeight() / 2);
            }
        };
        settingsSeparator.setOpaque(false);
        settingsSeparator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        settingsSeparator.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebarPanel.add(settingsSeparator);
        
        JPanel settingsPanel = new JPanel(new BorderLayout());
        settingsPanel.setOpaque(false);
        settingsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        settingsPanel.setBorder(new EmptyBorder(10, 15, 15, 15));
        settingsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel lblSettings = new JLabel("Cài đặt");
        lblSettings.setForeground(new java.awt.Color(220, 220, 230));
        lblSettings.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSettings.setCursor(new Cursor(Cursor.HAND_CURSOR));
        try {
            java.net.URL settingsIconUrl = getClass().getResource("/images/icon/settings.png");
            if (settingsIconUrl != null) {
                Image img = new ImageIcon(settingsIconUrl).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
                lblSettings.setIcon(new ImageIcon(img));
                lblSettings.setIconTextGap(10);
            } else {
                lblSettings.setText("⚙ Cài đặt");
            }
        } catch (Exception e) {}
        
        JLabel lblArrow = new JLabel(">");
        lblArrow.setForeground(new java.awt.Color(150, 150, 160));
        lblArrow.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        settingsPanel.add(lblSettings, BorderLayout.WEST);
        settingsPanel.add(lblArrow, BorderLayout.EAST);
        
        sidebarPanel.add(settingsPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebarPanel, mainChatArea);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerSize(4);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);
        splitPane.setBackground(new java.awt.Color(0,0,0,0));
        splitPane.setOneTouchExpandable(false);
        splitPane.setDividerLocation(230);

        splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, e -> {
            if (!isExpanded) return;
            int loc = splitPane.getDividerLocation();
            if (loc < 80 && loc > 0) {
                SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(80));
                loc = 80;
            }
            
            boolean isSmall = loc < 150;
            lblBrand.setVisible(!isSmall);
            logoPanel.setVisible(!isSmall);
            
            if (isSmall) {
                if (btnToggleSidebar.getParent() == headerLeft && ((BorderLayout)headerLeft.getLayout()).getLayoutComponent(BorderLayout.EAST) == btnToggleSidebar) {
                    headerLeft.remove(btnToggleSidebar);
                    headerLeft.add(btnToggleSidebar, BorderLayout.WEST);
                    headerLeft.revalidate();
                    headerLeft.repaint();
                }
            } else {
                if (btnToggleSidebar.getParent() == headerLeft && ((BorderLayout)headerLeft.getLayout()).getLayoutComponent(BorderLayout.WEST) == btnToggleSidebar) {
                    headerLeft.remove(btnToggleSidebar);
                    headerLeft.add(logoPanel, BorderLayout.WEST);
                    headerLeft.add(btnToggleSidebar, BorderLayout.EAST);
                    headerLeft.revalidate();
                    headerLeft.repaint();
                }
            }
            
            btnNewChat.setText(isSmall ? "" : "Đoạn chat mới");
            btnSearchChat.setText(isSmall ? "" : (btnSearchChat.getIcon() != null ? "Tìm kiếm đoạn chat" : "Tìm kiếm đoạn chat"));
            for (Component c : sidebarPanel.getComponents()) {
                if (c != newChatPanel && c != searchChatPanel && c != headerLeft && !(c instanceof Box.Filler)) {
                    c.setVisible(!isSmall);
                }
            }
            sidebarPanel.revalidate();
            sidebarPanel.repaint();
        });
        
        btnToggleSidebar.addActionListener(e -> {
            int currentLoc = splitPane.getDividerLocation();
            int target = currentLoc < 150 ? 230 : 80;
            new Thread(() -> {
                int step = currentLoc < target ? 15 : -15;
                int current = currentLoc;
                while ((step > 0 && current < target) || (step < 0 && current > target)) {
                    current += step;
                    final int c = current;
                    SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(c));
                    try { Thread.sleep(10); } catch (Exception ex) {}
                }
                SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(target));
            }).start();
        });

        splitPane.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                return new javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        g.setColor(new java.awt.Color(255, 255, 255, 10));
                        g.fillRect(0, 0, getSize().width, getSize().height);
                        super.paint(g);
                    }
                };
            }
        });
        chatContainer.add(splitPane, BorderLayout.CENTER);
        
        setContentPane(chatContainer);
        
        // Initial setup for the Follow-Up Timer
        idleFollowUpTimer = new javax.swing.Timer(50000, e -> triggerIdleFollowUp());
        idleFollowUpTimer.setRepeats(false);
        
        // --- EVENT LISTENERS ---
        btnSend.addActionListener(e -> {
            clearIdleFollowUpTimer();
            sendTextMessage();
        });
        
        chatInput.addActionListener(e -> {
            clearIdleFollowUpTimer();
            sendTextMessage();
        });
        
        btnMic.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { startRecording(); }
            @Override
            public void mouseReleased(MouseEvent e) { stopRecording(); }
        });
        btnImage.addActionListener(e -> chooseAndSendImage());
        
        audioFormat = new AudioFormat(16000.0f, 16, 1, true, false);
    }
    
    private int lastLavieX = -1;
    private int lastLavieY = -1;
    private boolean isExpanded = false;
    private JPanel sidebarPanel;
    private Rectangle normalBounds;
    private javax.swing.Timer expandTimer;

    private void notifyLavieState(String state) {
        try (java.net.DatagramSocket socket = new java.net.DatagramSocket()) {
            String msg = "STATE:" + state;
            byte[] buf = msg.getBytes("UTF-8");
            java.net.DatagramPacket packet = new java.net.DatagramPacket(buf, buf.length, java.net.InetAddress.getByName("127.0.0.1"), 15000);
            socket.send(packet);
        } catch (Exception e) {}
    }

    private void animateBounds(int targetX, int targetY, int targetW, int targetH) {
        if (expandTimer != null && expandTimer.isRunning()) {
            expandTimer.stop();
        }
        int startX = getX(), startY = getY(), startW = getWidth(), startH = getHeight();
        int frames = 15;
        int delay = 15;
        long startTime = System.currentTimeMillis();
        
        expandTimer = new javax.swing.Timer(delay, e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            float progress = (float)elapsed / (frames * delay);
            if (progress >= 1.0f) {
                progress = 1.0f;
                expandTimer.stop();
            }
            float easeOut = 1.0f - (float)Math.pow(1.0f - progress, 3);
            int newX = (int)(startX + (targetX - startX) * easeOut);
            int newY = (int)(startY + (targetY - startY) * easeOut);
            int newW = (int)(startW + (targetW - startW) * easeOut);
            int newH = (int)(startH + (targetH - startH) * easeOut);
            setBounds(newX, newY, newW, newH);
            revalidate();
            repaint();
        });
        expandTimer.start();
    }

    public void updatePositionFromLavie(int lavieX, int lavieY) {
        this.lastLavieX = lavieX;
        this.lastLavieY = lavieY;
        if (isExpanded) return;
        
        int chatW = getWidth();
        int chatH = getHeight();
        // Góc phải dưới của khung chat nằm sát đầu Lavie (khung chat lệch nhẹ sang trái)
        int newX = lavieX - chatW + 60;
        // Khoảng cách từ đỉnh đầu Lavie đến khung chat là 6px
        int newY = lavieY - chatH - 6;
        
        // Đảm bảo không bay ra ngoài màn hình
        if (newX < 0) newX = 0;
        if (newY < 0) newY = 0;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (newX + chatW > screenSize.width) newX = screenSize.width - chatW;
        if (newY + chatH > screenSize.height) newY = screenSize.height - chatH;
        
        setLocation(newX, newY);
    }

    public void toggleVisibility() {
        if (!isVisible()) {
            if (lastLavieX == -1) {
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                setLocation(screenSize.width - 320 - 250, screenSize.height - 450 - 50);
            }
            setVisible(true);
            toFront();
            SwingUtilities.invokeLater(() -> chatInput.requestFocusInWindow());
            notifyLavieState(isExpanded ? "OPEN" : "CLOSED");
        } else {
            setVisible(false);
            notifyLavieState("CLOSED");
        }
    }
    
    private JButton createHeaderButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new java.awt.Color(255, 255, 255, 30)); 
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new java.awt.Color(255, 255, 255, 50));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(24, 24));
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setForeground(new java.awt.Color(220, 220, 220));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(0,0,0,0));
        return btn;
    }

    private JButton createToolButton(String path) {
        JButton btn = new JButton() {
            private Image iconImg = null;
            {
                try {
                    java.net.URL imgUrl = getClass().getResource(path);
                    if (imgUrl != null) iconImg = new ImageIcon(imgUrl).getImage();
                } catch(Exception e) {}
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new java.awt.Color(255, 255, 255, 30));
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                if (iconImg != null) {
                    g2.drawImage(iconImg, 6, 6, getWidth()-12, getHeight()-12, null);
                }
                g2.dispose();
            }
            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new java.awt.Color(255, 255, 255, 100));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(28, 28));
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(0,0,0,0));
        return btn;
    }
    
    private void appendMessage(String sender, String text) {
        SwingUtilities.invokeLater(() -> {
            boolean isUser = sender.contains("Bạn");
            int chatW = 0;
            try { chatW = chatMessagesPanel.getParent().getParent().getWidth(); } catch (Exception e) {}
            if (chatW <= 0) chatW = getWidth() - (sidebarPanel != null && sidebarPanel.isVisible() ? 260 : 0);
            final int maxBubbleW = Math.max(180, chatW - 60);
            
            JPanel messageContainer = new JPanel();
            messageContainer.setLayout(new BoxLayout(messageContainer, BoxLayout.Y_AXIS));
            messageContainer.setOpaque(false);
            
            String[] parts = text.split("```");
            for (int i = 0; i < parts.length; i++) {
                if (i % 2 == 0) {
                    String normalText = parts[i].trim();
                    if (normalText.isEmpty() && parts.length > 1) continue;
                    
                    normalText = normalText.replace("<", "&lt;").replace(">", "&gt;");
                    normalText = normalText.replace("\n", "<br>");
                    normalText = normalText.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
                    normalText = normalText.replaceAll("\\*(.*?)\\*", "<i>$1</i>");
                    normalText = normalText.replaceAll("`(.*?)`", "<code style=\"color:#ffcc00;\">$1</code>");
                    
                    String htmlText = "<html><body style='font-family: Segoe UI, sans-serif; font-size: 12px; color: " + (isUser ? "#F0F0F0" : "white") + "; margin: 0; padding: 0;'>" + normalText + "</body></html>";
                    
                    JTextPane textPane = new JTextPane() {
                        @Override
                        public boolean getScrollableTracksViewportWidth() {
                            return true;
                        }
                        @Override
                        public Dimension getPreferredSize() {
                            int cw = 0;
                            try { cw = chatMessagesPanel.getParent().getParent().getWidth(); } catch (Exception e) {}
                            if (cw <= 0) cw = LavieChatWidget.this.getWidth() - (sidebarPanel != null && sidebarPanel.isVisible() ? 260 : 0);
                            int dynMaxW = Math.max(180, cw - 60);
                            
                            Dimension pref = super.getPreferredSize();
                            if (pref.width > dynMaxW) {
                                setSize(new Dimension(dynMaxW, Integer.MAX_VALUE));
                                return new Dimension(dynMaxW, super.getPreferredSize().height);
                            }
                            return pref;
                        }
                    };
                    textPane.setContentType("text/html");
                    textPane.setEditable(false);
                    textPane.setOpaque(false);
                    textPane.setText(htmlText);
                    textPane.setBorder(new EmptyBorder(8, 12, 8, 12));
                    textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
                    
                    JPanel bubble = new JPanel(new BorderLayout()) {
                        @Override
                        protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            if (isUser) g2.setColor(new java.awt.Color(175, 165, 255, 90));
                            else g2.setColor(new java.awt.Color(255, 255, 255, 40));
                            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                            g2.setColor(new java.awt.Color(255, 255, 255, 80));
                            g2.setStroke(new BasicStroke(1.5f));
                            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                            g2.dispose();
                        }
                    };
                    bubble.setOpaque(false);
                    bubble.add(textPane, BorderLayout.CENTER);
                    
                    JPanel bubbleWrapper = new JPanel(new FlowLayout(isUser ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 2));
                    bubbleWrapper.setOpaque(false);
                    bubbleWrapper.add(bubble);
                    messageContainer.add(bubbleWrapper);
                } else {
                    String codeBlock = parts[i];
                    int firstNewline = codeBlock.indexOf('\n');
                    String lang = "";
                    if (firstNewline != -1 && firstNewline < 20) {
                        lang = codeBlock.substring(0, firstNewline).trim();
                        codeBlock = codeBlock.substring(firstNewline + 1);
                    }
                    
                    JTextArea codeArea = new JTextArea(codeBlock);
                    codeArea.setFont(new Font("Consolas", Font.PLAIN, 12));
                    codeArea.setBackground(new java.awt.Color(40, 44, 52));
                    codeArea.setForeground(new java.awt.Color(171, 178, 191));
                    codeArea.setCaretColor(java.awt.Color.WHITE);
                    codeArea.setEditable(false);
                    codeArea.setMargin(new java.awt.Insets(10, 10, 10, 10));
                    
                    JScrollPane scrollPane = new JScrollPane(codeArea) {
                        @Override
                        public Dimension getPreferredSize() {
                            Dimension d = super.getPreferredSize();
                            d.width = Math.min(d.width, maxBubbleW);
                            return d;
                        }
                    };
                    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                    scrollPane.setBorder(null);
                    scrollPane.setOpaque(false);
                    scrollPane.getViewport().setOpaque(false);
                    codeArea.setOpaque(false);
                    
                    JPanel codeHeader = new JPanel(new BorderLayout()) {
                        @Override
                        protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(new java.awt.Color(50, 50, 50));
                            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                            g2.fillRect(0, getHeight() - 10, getWidth(), 10);
                            g2.dispose();
                        }
                    };
                    codeHeader.setOpaque(false);
                    codeHeader.setBorder(new EmptyBorder(5, 10, 5, 10));
                    JLabel langLbl = new JLabel(lang);
                    langLbl.setForeground(java.awt.Color.LIGHT_GRAY);
                    langLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    
                    JButton btnCopyCode = new JButton();
                    try {
                        java.net.URL imgUrl = getClass().getResource("/images/icon/coppy.png");
                        if (imgUrl != null) {
                            Image iconImg = new ImageIcon(imgUrl).getImage().getScaledInstance(14, 14, Image.SCALE_SMOOTH);
                            btnCopyCode.setIcon(new ImageIcon(iconImg));
                        } else btnCopyCode.setText("Copy");
                    } catch (Exception e) {}
                    btnCopyCode.setPreferredSize(new Dimension(20, 20));
                    btnCopyCode.setToolTipText("Copy Code");
                    btnCopyCode.setContentAreaFilled(false);
                    btnCopyCode.setBorderPainted(false);
                    btnCopyCode.setFocusPainted(false);
                    btnCopyCode.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    final String codeToCopy = codeBlock;
                    btnCopyCode.addActionListener(e -> {
                        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(codeToCopy), null);
                        btnCopyCode.setToolTipText("Copied!");
                        new javax.swing.Timer(2000, ev -> btnCopyCode.setToolTipText("Copy Code")).start();
                    });
                    
                    codeHeader.add(langLbl, BorderLayout.WEST);
                    codeHeader.add(btnCopyCode, BorderLayout.EAST);
                    
                    JPanel codePanel = new JPanel(new BorderLayout()) {
                        @Override
                        protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(new java.awt.Color(40, 44, 52));
                            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                            g2.setColor(new java.awt.Color(100, 100, 100, 50));
                            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                            g2.dispose();
                        }
                    };
                    codePanel.setOpaque(false);
                    codePanel.add(codeHeader, BorderLayout.NORTH);
                    codePanel.add(scrollPane, BorderLayout.CENTER);
                    
                    JPanel codeWrapper = new JPanel(new FlowLayout(isUser ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 5));
                    codeWrapper.setOpaque(false);
                    codeWrapper.add(codePanel);
                    messageContainer.add(codeWrapper);
                }
            }
            
            JPanel toolsPanel = new JPanel(new FlowLayout(isUser ? FlowLayout.RIGHT : FlowLayout.LEFT, 5, 0));
            toolsPanel.setOpaque(false);
            JButton btnCopy = new JButton();
            try {
                java.net.URL imgUrl = getClass().getResource("/images/icon/coppy.png");
                if (imgUrl != null) {
                    Image iconImg = new ImageIcon(imgUrl).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
                    btnCopy.setIcon(new ImageIcon(iconImg));
                } else {
                    btnCopy.setText("Copy");
                }
            } catch (Exception e) {}
            btnCopy.setPreferredSize(new Dimension(24, 24));
            btnCopy.setToolTipText("Copy");
            btnCopy.setContentAreaFilled(false);
            btnCopy.setBorderPainted(false);
            btnCopy.setFocusPainted(false);
            btnCopy.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnCopy.addActionListener(e -> {
                String clean = text.replaceAll("\\<.*?\\>", "").replace("&lt;", "<").replace("&gt;", ">");
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(clean), null);
                btnCopy.setToolTipText("Copied!");
                new javax.swing.Timer(2000, ev -> btnCopy.setToolTipText("Copy")).start();
            });
            toolsPanel.add(btnCopy);
            
            JPanel finalWrapper = new JPanel(new BorderLayout());
            finalWrapper.setOpaque(false);
            finalWrapper.add(messageContainer, BorderLayout.CENTER);
            finalWrapper.add(toolsPanel, BorderLayout.SOUTH);
            
            JPanel chatRow = new JPanel(new FlowLayout(isUser ? FlowLayout.RIGHT : FlowLayout.LEFT, 5, 2));
            chatRow.setOpaque(false);
            chatRow.setBorder(new EmptyBorder(0, 5, 2, 5));
            chatRow.add(finalWrapper);
            
            chatMessagesPanel.add(chatRow);
            chatMessagesPanel.revalidate();
            chatMessagesPanel.repaint();
            
            try {
                JScrollBar vertical = ((JScrollPane) chatMessagesPanel.getParent().getParent()).getVerticalScrollBar();
                SwingUtilities.invokeLater(() -> vertical.setValue(vertical.getMaximum()));
            } catch(Exception e) {}
        });
    }

    private JLabel appendStreamingMessage(String sender) {
        JLabel[] lblRef = {null};
        try {
            SwingUtilities.invokeAndWait(() -> {
                boolean isSystem = true;
                String htmlText = "<html><div style='font-family: Segoe UI, sans-serif; font-size: 11px; color: white;'></div></html>";
                JLabel lbl = new JLabel(htmlText);
                lbl.setBorder(new EmptyBorder(8, 12, 8, 12));
                lblRef[0] = lbl;
                
                JPanel bubble = new JPanel(new BorderLayout()) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new java.awt.Color(255, 255, 255, 40));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                        // Hiệu ứng viền sáng xung quanh tin nhắn
                        g2.setColor(new java.awt.Color(255, 255, 255, 80));
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                        g2.dispose();
                    }
                };
                bubble.setOpaque(false);
                bubble.add(lbl, BorderLayout.CENTER);
                
                JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
                wrapper.setOpaque(false);
                wrapper.setBorder(new EmptyBorder(0, 5, 5, 5)); 
                wrapper.add(bubble);
                
                chatMessagesPanel.add(wrapper);
                chatMessagesPanel.revalidate();
                chatMessagesPanel.repaint();
                
                try {
                    JScrollBar vertical = ((JScrollPane) chatMessagesPanel.getParent().getParent()).getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                } catch(Exception e) {}
            });
        } catch(Exception e) {}
        return lblRef[0];
    }

    private void sendTextMessage() {
        String text = chatInput.getText().trim();
        if (text.isEmpty() && stagedImageFile == null) return;
        chatInput.setText("");
        
        if (stagedImageFile != null) {
            File file = stagedImageFile;
            String message = text;
            stagedImageFile = null;
            stagedImageLabel.setVisible(false);
            
            String fileName = file.getName().toLowerCase();
            boolean isDocument = fileName.endsWith(".pdf") || fileName.endsWith(".docx") || fileName.endsWith(".txt");
            
            if (isDocument) {
                String pfx = fileName.endsWith(".pdf") ? "📄" : "📝";
                appendMessage("Bạn", pfx + " [Tài liệu] " + file.getName() + (message.isEmpty() ? "" : "<br>" + message));
                new Thread(() -> {
                    sendMultipartStream("https://hocba299-3-tutorhub-ai.hf.space/api/chat/document", file, "image", "application/octet-stream", message);
                }).start();
                return;
            }
            
            try {
                java.awt.Image img = javax.imageio.ImageIO.read(file);
                if (img != null) {
                    int w = img.getWidth(null);
                    int h = img.getHeight(null);
                    int maxW = 140;
                    if (w > maxW) {
                        h = (h * maxW) / w;
                        w = maxW;
                    }
                    java.awt.Image scaled = img.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH);
                    java.awt.image.BufferedImage bimg = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = bimg.createGraphics();
                    g2d.drawImage(scaled, 0, 0, null);
                    g2d.dispose();
                    
                    File tmp = File.createTempFile("thumb", ".png");
                    javax.imageio.ImageIO.write(bimg, "png", tmp);
                    String imgUrl = tmp.toURI().toURL().toString();
                    String textToAppend = "<img src='" + imgUrl + "' width='" + w + "' height='" + h + "'><br>" + message;
                    appendMessage("Bạn", textToAppend);
                } else {
                    appendMessage("Bạn", "🖼️ [Ảnh] " + message);
                }
            } catch (Exception ex) {
                appendMessage("Bạn", "🖼️ [Ảnh] " + message);
            }
            
            new Thread(() -> {
                sendMultipart("https://hocba299-3-tutorhub-ai.hf.space/api/chat/vision", file, "image", "image/png", message);
            }).start();
            return;
        }

        lastContextStr = text;
        appendMessage("Bạn", text);
        
        new Thread(() -> {
            try {
                URL url = new URL("https://hocba299-3-tutorhub-ai.hf.space/api/chat/stream");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                
                String jsonInputString = "{\"message\": \"" + text.replace("\"", "\\\"") + "\", \"user_id\": \"java_user\", \"voice\": true}";
                try(OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                String line;
                JLabel streamLbl = null;
                StringBuilder currentContent = new StringBuilder();
                
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if (data.equals("[DONE]")) break;
                        if (data.isEmpty()) continue;
                        
                        String content = extractJsonValue(data, "content");
                        if (content != null) {
                            content = content.replace("\\n", "\n");
                            content = content.replace("\\\"", "\"");
                            currentContent.append(content);
                            
                            boolean needsWrap = false;
                            int maxBubbleW = Math.max(180, getWidth() - 150);
                            FontMetrics fm = chatInput.getFontMetrics(new Font("Segoe UI", Font.PLAIN, 11));
                            String[] lines = currentContent.toString().split("\n");
                            for (String l : lines) {
                                if (fm.stringWidth(l) > maxBubbleW) {
                                    needsWrap = true;
                                    break;
                                }
                            }
                            String widthStyle = needsWrap ? "width: " + maxBubbleW + "px;" : "";
                            
                            String parsed = parseMarkdownToHtml(currentContent.toString());
                            String finalHtml = "<html><div style='" + widthStyle + " font-family: Segoe UI, sans-serif; font-size: 11px; color: white;'>" + parsed + "</div></html>";
                            
                            if (streamLbl == null) {
                                streamLbl = appendStreamingMessage("Lavie");
                            }
                            final JLabel lblToUpdate = streamLbl;
                            SwingUtilities.invokeLater(() -> {
                                lblToUpdate.setText(finalHtml);
                                try {
                                    JScrollBar vertical = ((JScrollPane) chatMessagesPanel.getParent().getParent()).getVerticalScrollBar();
                                    vertical.setValue(vertical.getMaximum());
                                } catch(Exception e) {}
                            });
                        }
                        
                        String audioUrl = extractJsonValue(data, "audio_url");
                        if (audioUrl != null) {
                            playAudioWithLipSync("https://hocba299-3-tutorhub-ai.hf.space" + audioUrl.replace("\\/", "/"));
                        }
                    }
                }
                br.close();
                if (streamLbl != null) {
                    final JLabel lblToUpdate = streamLbl;
                    SwingUtilities.invokeLater(() -> {
                        chatMessagesPanel.remove(lblToUpdate.getParent().getParent());
                        appendMessage("Lavie", currentContent.toString());
                    });
                }
            } catch (Exception ex) {
                appendMessage("Lavie", "Lỗi kết nối máy chủ.");
            }
        }).start();
    }
    
    private void startRecording() {
        if (isRecording) return;
        isRecording = true;
        btnMic.setBackground(java.awt.Color.RED);
        
        new Thread(() -> {
            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
                if (!AudioSystem.isLineSupported(info)) {
                    appendMessage("Lavie", "Microphone không được hỗ trợ.");
                    return;
                }
                
                targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
                targetDataLine.open(audioFormat);
                targetDataLine.start();
                
                audioOutputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                while (isRecording) {
                    int count = targetDataLine.read(buffer, 0, buffer.length);
                    if (count > 0) {
                        audioOutputStream.write(buffer, 0, count);
                    }
                }
                targetDataLine.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }
    
    private void stopRecording() {
        if (!isRecording) return;
        isRecording = false;
        btnMic.setBackground(new java.awt.Color(79, 70, 229));
        
        new Thread(() -> {
            try {
                // Save to temporary WAV file
                File wavFile = File.createTempFile("java_voice", ".wav");
                byte[] audioData = audioOutputStream.toByteArray();
                ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
                AudioInputStream ais = new AudioInputStream(bais, audioFormat, audioData.length / audioFormat.getFrameSize());
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavFile);
                
                appendMessage("Bạn", "🎤 [Ghi âm]");
                
                // Send multipart form data
                sendMultipart("https://hocba299-3-tutorhub-ai.hf.space/api/chat/voice", wavFile, "audio", "audio/wav", null);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }
    
    private void chooseAndSendImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images & Documents", "jpg", "jpeg", "png", "gif", "pdf", "docx", "txt"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            stagedImageFile = fileChooser.getSelectedFile();
            String name = stagedImageFile.getName().toLowerCase();
            String pfx = "🖼️";
            if (name.endsWith(".pdf")) pfx = "📄";
            else if (name.endsWith(".docx") || name.endsWith(".txt")) pfx = "📝";
            
            stagedImageLabel.setText(pfx + " Đã đính kèm: " + stagedImageFile.getName());
            stagedImageLabel.setVisible(true);
            chatInput.requestFocusInWindow();
        }
    }
    
    private void sendMultipart(String requestURL, File uploadFile, String fileField, String fileType, String extraMessage) {
        try {
            String boundary = "===" + System.currentTimeMillis() + "===";
            HttpURLConnection conn = (HttpURLConnection) new URL(requestURL).openConnection();
            conn.setUseCaches(false);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            
            try (OutputStream outputStream = conn.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true)) {
                
                writer.append("--" + boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"" + fileField + "\"; filename=\"" + uploadFile.getName() + "\"").append("\r\n");
                writer.append("Content-Type: " + fileType).append("\r\n");
                writer.append("\r\n").flush();
                
                Files.copy(uploadFile.toPath(), outputStream);
                outputStream.flush();
                writer.append("\r\n").flush();
                
                writer.append("--" + boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"user_id\"").append("\r\n\r\n");
                writer.append("java_user").append("\r\n").flush();
                
                if (extraMessage != null && !extraMessage.isEmpty()) {
                    writer.append("--" + boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"message\"").append("\r\n\r\n");
                    writer.append(extraMessage).append("\r\n").flush();
                }
                
                writer.append("--" + boundary + "--").append("\r\n").flush();
            }
            lastContextStr = uploadFile.getName() + " " + extraMessage;
            handleApiResponse(conn);
        } catch (Exception ex) {
            ex.printStackTrace();
            appendMessage("Lavie", "Lỗi upload.");
        }
    }
    
    private void sendMultipartStream(String requestURL, File uploadFile, String fileField, String fileType, String extraMessage) {
        try {
            String boundary = "===" + System.currentTimeMillis() + "===";
            HttpURLConnection conn = (HttpURLConnection) new URL(requestURL).openConnection();
            conn.setUseCaches(false);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            
            try (OutputStream outputStream = conn.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true)) {
                
                writer.append("--" + boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"" + fileField + "\"; filename=\"" + uploadFile.getName() + "\"").append("\r\n");
                writer.append("Content-Type: " + fileType).append("\r\n");
                writer.append("\r\n").flush();
                
                Files.copy(uploadFile.toPath(), outputStream);
                outputStream.flush();
                writer.append("\r\n").flush();
                
                writer.append("--" + boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"user_id\"").append("\r\n\r\n");
                writer.append("java_user").append("\r\n").flush();
                
                if (extraMessage != null && !extraMessage.isEmpty()) {
                    writer.append("--" + boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"message\"").append("\r\n\r\n");
                    writer.append(extraMessage).append("\r\n").flush();
                }
                
                writer.append("--" + boundary + "--").append("\r\n").flush();
            }
            lastContextStr = uploadFile.getName() + " " + extraMessage;
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            String line;
            JLabel streamLbl = null;
            StringBuilder currentContent = new StringBuilder();
            
            while ((line = br.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if (data.equals("[DONE]")) break;
                    if (data.isEmpty()) continue;
                    
                    String chunkText = extractJsonValue(data, "chunk");
                    if (chunkText == null) chunkText = extractJsonValue(data, "content"); // Fallback for agent
                    
                    String audioUrl = extractJsonValue(data, "audio_url");
                    if (audioUrl != null && !audioUrl.isEmpty()) {
                        playAudioWithLipSync("https://hocba299-3-tutorhub-ai.hf.space" + audioUrl.replace("\\/", "/"));
                    }
                    
                    if (chunkText != null) {
                        chunkText = chunkText.replace("\\n", "<br>");
                        chunkText = chunkText.replace("\\\"", "\"");
                        currentContent.append(chunkText);
                        
                        boolean needsWrap = false;
                        int maxBubbleW = Math.max(180, getWidth() - 150);
                        FontMetrics fm = chatInput.getFontMetrics(new Font("Segoe UI", Font.PLAIN, 11));
                        String[] lines = currentContent.toString().replace("<br>", "\n").split("\n");
                        for (String l : lines) {
                            if (fm.stringWidth(l) > maxBubbleW) {
                                needsWrap = true;
                                break;
                            }
                        }
                        String widthStyle = needsWrap ? "width: " + maxBubbleW + "px;" : "";
                        String finalHtml = "<html><div style='" + widthStyle + " font-family: Segoe UI, sans-serif; font-size: 11px; color: white;'>" + currentContent.toString() + "</div></html>";
                        
                        if (streamLbl == null) {
                            streamLbl = appendStreamingMessage("Lavie");
                        }
                        final JLabel lblToUpdate = streamLbl;
                        SwingUtilities.invokeLater(() -> {
                            lblToUpdate.setText(finalHtml);
                            chatMessagesPanel.revalidate();
                            chatMessagesPanel.repaint();
                            javax.swing.JScrollBar vertical = ((javax.swing.JScrollPane) chatMessagesPanel.getParent().getParent()).getVerticalScrollBar();
                            vertical.setValue(vertical.getMaximum());
                        });
                    }
                }
            }
            br.close();
            if (streamLbl != null) {
                final JLabel lblToUpdate = streamLbl;
                SwingUtilities.invokeLater(() -> {
                    chatMessagesPanel.remove(lblToUpdate.getParent().getParent());
                    appendMessage("Lavie", currentContent.toString());
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            appendMessage("Lavie", "Lỗi gửi tài liệu.");
        }
    }
    
    private void handleApiResponse(HttpURLConnection conn) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
        StringBuilder response = new StringBuilder();
        String responseLine;
        while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
        }
        
        String res = response.toString();
        // Giả lập bóc tách JSON (do không muốn add thư viện org.json nếu dự án chưa có)
        String answer = extractJsonValue(res, "answer");
        String audioUrl = extractJsonValue(res, "audio_url");
        String userText = extractJsonValue(res, "user_text");
        
        if (userText != null && !userText.isEmpty()) {
            appendMessage("Bạn (Voice)", userText.replace("\\n", "<br>"));
        }
        
        if (answer != null) {
            appendMessage("Lavie", answer.replace("\\n", "<br>"));
        }
        
        if (audioUrl != null && !audioUrl.isEmpty()) {
            playAudioWithLipSync("https://hocba299-3-tutorhub-ai.hf.space" + audioUrl.replace("\\/", "/"));
        }
    }
    
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int idx = json.indexOf(searchKey);
        if (idx == -1) return null;
        int start = json.indexOf("\"", idx + searchKey.length());
        if (start == -1) return null;
        int end = json.indexOf("\"", start + 1);
        // Bỏ qua escape quotes
        while (end != -1 && json.charAt(end - 1) == '\\') {
            end = json.indexOf("\"", end + 1);
        }
        if (end == -1) return null;
        return json.substring(start + 1, end).replace("\\\"", "\"");
    }
    
    private void playAudioWithLipSync(String urlStr) {
        audioQueue.offer(urlStr);
        if (audioPlayerThread == null || !audioPlayerThread.isAlive()) {
            audioPlayerThread = new Thread(() -> {
                while (!audioQueue.isEmpty()) {
                    try {
                        String currentUrl = audioQueue.poll();
                        if (currentUrl == null) break;
                        URL url = new URL(currentUrl);
                        try (InputStream in = url.openStream()) {
                            Player player = new Player(in);
                            
                            Thread lipSyncThread = new Thread(() -> {
                                while (!player.isComplete()) {
                                    try {
                                        double vol = Math.random() * 0.8 + 0.2;
                                        try (DatagramSocket socket = new DatagramSocket()) {
                                            String msg = String.valueOf(vol);
                                            byte[] buf = msg.getBytes();
                                            InetAddress address = InetAddress.getByName("127.0.0.1");
                                            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 15000);
                                            socket.send(packet);
                                        } catch (Exception ignored) {}
                                        
                                        Thread.sleep(100);
                                    } catch (Exception e) {}
                                }
                                
                                try (DatagramSocket socket = new DatagramSocket()) {
                                    String msg = "0.0";
                                    byte[] buf = msg.getBytes();
                                    InetAddress address = InetAddress.getByName("127.0.0.1");
                                    DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 15000);
                                    socket.send(packet);
                                } catch (Exception ignored) {}
                            });
                            
                            lipSyncThread.start();
                            player.play();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                javax.swing.SwingUtilities.invokeLater(this::startIdleFollowUpTimer);
            });
            audioPlayerThread.start();
        }
    }
    
    private String parseMarkdownToHtml(String text) {
        if (text == null) return "";
        String[] parts = text.split("```");
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < parts.length; i++) {
            if (i % 2 == 0) {
                String normalText = parts[i];
                normalText = normalText.replace("<", "&lt;").replace(">", "&gt;");
                normalText = normalText.replace("\n", "<br>");
                normalText = normalText.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
                normalText = normalText.replaceAll("\\*(.*?)\\*", "<i>$1</i>");
                normalText = normalText.replaceAll("`(.*?)`", "<code style=\"color:#ffcc00;\">$1</code>");
                sb.append(normalText);
            } else {
                String codeBlock = parts[i];
                int firstNewline = codeBlock.indexOf('\n');
                String lang = "";
                if (firstNewline != -1 && firstNewline < 20) {
                    lang = codeBlock.substring(0, firstNewline).trim();
                    codeBlock = codeBlock.substring(firstNewline + 1);
                }
                codeBlock = codeBlock.replace("<", "&lt;").replace(">", "&gt;");
                codeBlock = highlightSyntax(codeBlock);
                
                sb.append("<br><table bgcolor=\"#2b2b2b\" width=\"100%\" cellpadding=\"8\"><tr><td>");
                if (!lang.isEmpty()) {
                    sb.append("<div style=\"color:#888888; font-size:9px;\"><b>").append(lang).append("</b></div>");
                }
                sb.append("<pre style=\"font-family: Consolas, monospace; font-size: 10px; color: #a9b7c6; margin: 0;\">");
                sb.append(codeBlock);
                sb.append("</pre></td></tr></table><br>");
            }
        }
        return sb.toString();
    }
    
    private String highlightSyntax(String code) {
        code = code.replaceAll("(?m)(#.*)$", "<font color=\"#808080\">$1</font>");
        code = code.replaceAll("\"([^\"]*)\"", "<font color=\"#6a8759\">\"$1\"</font>");
        code = code.replaceAll("'([^']*)'", "<font color=\"#6a8759\">'$1'</font>");
        
        String[] keywords = {"def", "class", "import", "from", "return", "if", "else", "elif", "try", "except", "while", "for", "in", "and", "or", "not", "True", "False", "None", "public", "private", "void", "static"};
        for (String kw : keywords) {
            code = code.replaceAll("\\b" + kw + "\\b(?![^<]*>)", "<font color=\"#cc7832\">" + kw + "</font>");
        }
        return code;
    }

    public void startIdleFollowUpTimer() {
        if (hasSentIdleFollowUp) return;
        idleFollowUpTimer.restart();
    }

    public void clearIdleFollowUpTimer() {
        if (idleFollowUpTimer != null) idleFollowUpTimer.stop();
        hasSentIdleFollowUp = false;
    }

    private void triggerIdleFollowUp() {
        if (hasSentIdleFollowUp) return;
        hasSentIdleFollowUp = true;
        
        String prompt = "Anh còn muốn Lavie hỗ trợ thêm gì không ạ?";
        String lc = lastContextStr.toLowerCase();
        if (lc.contains("bài") || lc.contains("học") || lc.contains("ôn tập")) {
            prompt = "Anh muốn Lavie tóm tắt lại ý chính hoặc tạo câu hỏi ôn tập không ạ?";
        } else if (lc.contains("code") || lc.contains("lập trình") || lc.contains("python")) {
            prompt = "Anh muốn Lavie kiểm tra tiếp phần code liên quan không ạ?";
        } else if (lc.contains("tài liệu") || lc.contains("file") || lc.contains("docx") || lc.contains("pdf")) {
            prompt = "Anh muốn Lavie tìm thêm tài liệu tương tự không ạ?";
        } else if (lc.contains("lịch") || lc.contains("giờ")) {
            prompt = "Anh muốn Lavie gợi ý thêm khung giờ phù hợp không ạ?";
        }
        
        appendMessage("Lavie", prompt);
        
        final String textToSpeak = prompt;
        new Thread(() -> {
            try {
                URL url = new URL("https://hocba299-3-tutorhub-ai.hf.space/api/tts");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                
                String jsonInputString = "{\"text\": \"" + textToSpeak + "\"}";
                try(java.io.OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                String res = response.toString();
                String audioUrl = extractJsonValue(res, "audio_url");
                if (audioUrl != null && !audioUrl.isEmpty()) {
                    playAudioWithLipSync("https://hocba299-3-tutorhub-ai.hf.space" + audioUrl.replace("\\/", "/"));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }
}