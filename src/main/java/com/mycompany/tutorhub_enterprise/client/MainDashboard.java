package com.mycompany.tutorhub_enterprise.client;

import com.formdev.flatlaf.FlatLightLaf;
import com.mycompany.tutorhub_enterprise.models.ConversationInfo;
import com.mycompany.tutorhub_enterprise.models.ClassroomLessonModel;
import com.mycompany.tutorhub_enterprise.models.ClassroomMemberModel;
import com.mycompany.tutorhub_enterprise.models.Message;
import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.models.UserInfo; 
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class MainDashboard extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainCardPanel;
    private int currentUserId;
    private String userName;
    
    private ArrayList<SidebarMenuItem> menuItems = new ArrayList<>();
    private ArrayList<JLabel> groupLabels = new ArrayList<>();
    private boolean isSidebarExpanded = true;
    private JLabel lblLogoText;
    private JPanel bottomPanel;
    private JPanel sidebarPanel;
    private JSplitPane mainSplitPane;
    private static Map<String, ImageIcon> iconCache = new HashMap<>();
    
    private HomeTab homeTab;
    private UpgradeTab upgradeTab;
    private ProfileTab profileTab;
    private TodoListTab todoTab; 
    private ChatTab chatTab; 
    private HeaderPanel headerPanel;
    private ScheduleTab scheduleTab;
    private AcceptedClassTab acceptedTab;
    private BlackboardManagerTab blackboardManagerTab;
    private BlackboardFrame blackboardFrame;
    private ClassManagerTab classManagerTab;
    
    // Khai báo biến Chatbot
    private LavieChatWidget lavieWidget;
    private DriveTab driveTab;
    private ReelsTabPanel reelsTab;
    private com.mycompany.tutorhub_enterprise.client.exam.ExamTab examTab;
    // Locket Class fields
    public NativeReelPlayer activeLocketPlayer = null;
    public java.util.List<String> locketVideos = new java.util.ArrayList<>();


    // --- BẢNG MÀU LEARNING SHORT PLAYER CHO SIDEBAR ---
    private final Color SIDEBAR_BG = Color.decode("#FFFFFF");
    private final Color PRIMARY = Color.decode("#7C3AED");      
    private final Color PRIMARY_BG = Color.decode("#EDE7FF");   
    private final Color TEXT_MAIN = Color.decode("#17172F");    
    private final Color TEXT_MUTED = Color.decode("#73708A");   
    private final Color TEXT_HEADING = Color.decode("#94A3B8"); 
    private final Color HOVER_BG = Color.decode("#F0E8FF");     

    public String getUserName() { return this.userName; }
    public int getCurrentUserId() { return this.currentUserId; }
    public Image getAvatar() { return headerPanel != null ? headerPanel.getAvatar() : null; }

    public MainDashboard(int userId, String userName) {
        this.currentUserId = userId; 
        this.userName = userName;
        setupFrame();
        
        JPanel sidebar = createSidebar();
        JPanel mainArea = createMainArea();
        
        setupSplitPane(sidebar, mainArea);
        startNetworkListener();
        
        // ---------------------------------------------------------
        // NHÚNG CHAT UI (JAVA NATIVE) VÀ BÓNG MA ELECTRON (3D)
        // ---------------------------------------------------------
        lavieWidget = new LavieChatWidget(this);
        
        // Cửa sổ Chat bây giờ là JWindow nổi độc lập nên không cần gắn vào GlassPane nữa
        
        // Khởi động Electron ngầm
        startGhostElectron();
    }

    private Process electronProcess;

    private void startGhostElectron() {
        try {
            // Chạy ngầm npm start không bật cửa sổ console
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "npm", "start");
            pb.directory(new java.io.File("lavie_desktop"));
            pb.redirectErrorStream(true);
            
            // Tắt giao diện console của Windows (nếu có thể)
            electronProcess = pb.start();
            
            // Đọc log từ Electron (qua UDP) để bắt sự kiện click
            new Thread(() -> {
                try (java.net.DatagramSocket socket = new java.net.DatagramSocket(15001)) {
                    byte[] buffer = new byte[256];
                    while (true) {
                        java.net.DatagramPacket packet = new java.net.DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        String msg = new String(packet.getData(), 0, packet.getLength());
                        if (msg.contains("LAVIE_CLICKED")) {
                            SwingUtilities.invokeLater(() -> {
                                if (lavieWidget != null) lavieWidget.toggleVisibility();
                            });
                        } else if (msg.startsWith("POS:")) {
                            try {
                                String[] parts = msg.substring(4).split(",");
                                int lx = Integer.parseInt(parts[0].trim());
                                int ly = Integer.parseInt(parts[1].trim());
                                SwingUtilities.invokeLater(() -> {
                                    if (lavieWidget != null) lavieWidget.updatePositionFromLavie(lx, ly);
                                });
                            } catch (Exception e) {}
                        }
                    }
                } catch (Exception ex) {}
            }).start();
            
            // Khi tắt ứng dụng Java, tự động kill Electron
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (electronProcess != null && electronProcess.isAlive()) {
                    electronProcess.destroy();
                    // Đảm bảo kill triệt để Node.js trên Windows
                    try {
                        Runtime.getRuntime().exec("taskkill /F /IM electron.exe");
                    } catch (Exception ignored) {}
                }
            }));
            
            System.out.println("Ghost Electron Started!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    MainDashboard(int uid, String role, String avatar, String email) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    private void setupFrame() {
        setTitle("TutorHub Enterprise");
        setSize(1350, 800);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.decode("#FAF8FF"));
    }

    public void openNewBlackboard() {
        if (blackboardFrame != null) {
            blackboardFrame.dispose();
        }
        blackboardFrame = new BlackboardFrame(this, "CLASS_DEFAULT", "teacher");
        blackboardFrame.resetCanvas(); 
        blackboardFrame.setVisible(true);
    }

    public void openExistingBlackboard(BlackboardManagerTab.BoardMeta meta) {
        if (blackboardFrame != null) {
            blackboardFrame.dispose();
        }
        blackboardFrame = new BlackboardFrame(this, meta.boardId, "teacher");
        blackboardFrame.loadBoardData(meta.boardId, meta.title, meta.thumbnailUrl);
        blackboardFrame.setVisible(true);
    }
    
    public void joinLiveClass(String classId, String className) {
        openLiveClassroom(classId, className, "student");
    }

        public void openLiveClassroom(String classId, String className, String role) {
        if (blackboardFrame != null) {
            blackboardFrame.dispose();
        }
        blackboardFrame = new BlackboardFrame(this, classId, role);
        blackboardFrame.connectToLiveRoom("0", classId, className);
        blackboardFrame.setVisible(true);
    }

        public void openLiveLesson(String classroomId, String lessonId, String boardId, String className, String role) {
        if (blackboardFrame != null) {
            blackboardFrame.dispose();
        }
        blackboardFrame = new BlackboardFrame(this, boardId, role);
        blackboardFrame.connectToLiveRoom(lessonId, boardId, className);
        blackboardFrame.setVisible(true);
    }

    // =========================================================
    // MENU BÊN TRÁI (ĐÃ NÂNG CẤP LÊN GIAO DIỆN ENTERPRISE)
    // =========================================================
    private JPanel createSidebar() {
        sidebarPanel = new JPanel(new BorderLayout());
        sidebarPanel.setBackground(SIDEBAR_BG);
        sidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.decode("#E2E8F0"))); 

        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.setOpaque(false);

        // --- LOGO ---
        JPanel logoPanel = new JPanel();
        logoPanel.setLayout(new BoxLayout(logoPanel, BoxLayout.X_AXIS));
        logoPanel.setOpaque(false);
        logoPanel.setBorder(new EmptyBorder(30, 16, 20, 0));
        
        JLabel lblLogoIcon = new JLabel();
        try {
            java.net.URL logoUrl = getClass().getResource("/images/logomoi.png");
            if (logoUrl != null) {
                java.awt.Image img = javax.imageio.ImageIO.read(logoUrl);
                if (img != null) {
                    lblLogoIcon.setIcon(new javax.swing.ImageIcon(img.getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH)));
                    this.setIconImage(img);
                }
            }
        } catch (Exception ex) {
            com.formdev.flatlaf.extras.FlatSVGIcon fb = new com.formdev.flatlaf.extras.FlatSVGIcon("images/icon/home.svg", 32, 32);
            fb.setColorFilter(new com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter(c -> PRIMARY));
            lblLogoIcon.setIcon(fb);
        }

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        lblLogoText = new JLabel("TutorHub");
        lblLogoText.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblLogoText.setForeground(PRIMARY);
        lblLogoText.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel lblEnterprise = new JLabel("Enterprise");
        lblEnterprise.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblEnterprise.setForeground(TEXT_MUTED);
        lblEnterprise.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(lblLogoText);
        textPanel.add(lblEnterprise);
        
        logoPanel.add(lblLogoIcon);
        logoPanel.add(Box.createRigidArea(new Dimension(8, 0)));
        logoPanel.add(textPanel);
        logoPanel.add(Box.createHorizontalGlue());
        
        topWrapper.add(logoPanel, BorderLayout.CENTER);

        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 35));
        togglePanel.setOpaque(false);
        
        JPanel btnToggle = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillOval(0,0,getWidth(),getHeight());
                g2.setColor(Color.decode("#E2E8F0"));
                g2.drawOval(0,0,getWidth()-1,getHeight()-1);
                g2.dispose();
            }
        };
        btnToggle.setOpaque(false);
        btnToggle.setPreferredSize(new Dimension(28, 28));
        btnToggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JLabel lblToggleIcon = new JLabel("«", SwingConstants.CENTER);
        lblToggleIcon.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblToggleIcon.setForeground(TEXT_MUTED);
        btnToggle.add(lblToggleIcon, BorderLayout.CENTER);

        btnToggle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                isSidebarExpanded = !isSidebarExpanded;
                if(isSidebarExpanded) {
                    sidebarPanel.setMinimumSize(new Dimension(180, 0));
                    sidebarPanel.setPreferredSize(new Dimension(180, 0));
                    textPanel.setVisible(true);
                    lblLogoIcon.setVisible(true);
                    bottomPanel.setVisible(true);
                    for(SidebarMenuItem item : menuItems) {
                        item.setExpanded(true);
                    }
                    lblToggleIcon.setText("«");
                } else {
                    sidebarPanel.setMinimumSize(new Dimension(56, 0));
                    sidebarPanel.setPreferredSize(new Dimension(56, 0));
                    textPanel.setVisible(false);
                    lblLogoIcon.setVisible(true); // logo icon still shown
                    bottomPanel.setVisible(false);
                    for(SidebarMenuItem item : menuItems) {
                        item.setExpanded(false);
                    }
                    lblToggleIcon.setText("»");
                }
                sidebarPanel.revalidate();
                sidebarPanel.repaint();
                if(mainSplitPane != null) {
                    mainSplitPane.resetToPreferredSizes();
                }
                MainDashboard.this.revalidate();
            }
        });

        togglePanel.add(btnToggle);
        topWrapper.add(togglePanel, BorderLayout.EAST);

        sidebarPanel.add(topWrapper, BorderLayout.NORTH);

        // --- CÁC TAB MENU ---
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setOpaque(false);
        menuPanel.setBorder(new EmptyBorder(16, 12, 0, 12));

        // Theo thứ tự menu hình mẫu
        menuPanel.add(createMenuItem("Bảng tin lớp", "home", "Home", 0)); 
        menuPanel.add(createMenuItem("Reels", "reel", "Reels", 0));
        menuPanel.add(createMenuItem("Tin nhắn", "message", "Chat", 2)); 
        menuPanel.add(createMenuItem("Lớp học của tôi", "saved", "Saved", 0)); 
        menuPanel.add(createMenuItem("Lớp đã nhận", "myclass", "Taken", 0)); 
        menuPanel.add(createMenuItem("Lịch dạy", "calendar", "Schedule", 0)); 
        menuPanel.add(createMenuItem("Thi (Exam)", "task", "Exam", 0)); 
        menuPanel.add(createMenuItem("Nhiệm vụ", "task", "Todo", 0));
        menuPanel.add(createMenuItem("Tài liệu", "text", "Docs", 0));
        menuPanel.add(createMenuItem("Bảng vẽ", "draw", "Blackboard", 0)); 
        menuPanel.add(createMenuItem("Hồ sơ (eKYC)", "user", "Profile", 0));

        JScrollPane scrollPane = new JScrollPane(menuPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(4, 0));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        sidebarPanel.add(scrollPane, BorderLayout.CENTER);

        // --- THẺ PREMIUM ---
        bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(16, 16, 24, 16));
        bottomPanel.add(new PremiumCard(), BorderLayout.CENTER);
        
        sidebarPanel.add(bottomPanel, BorderLayout.SOUTH);

        return sidebarPanel;
    }

    private JLabel createGroupLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(TEXT_HEADING);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(new EmptyBorder(0, 12, 10, 0));
        groupLabels.add(lbl);
        return lbl;
    }

    class SidebarMenuItem extends JPanel {
        private String cardName;
        private boolean isActive = false;
        private boolean isHovered = false;
        private JLabel lblIcon, lblText;
        private String iconName;
        private JPanel badgeWrapper;

        public SidebarMenuItem(String title, String iconName, String cardName, int badgeCount) {
            this.cardName = cardName; 
            this.iconName = iconName;
            
            setLayout(new BorderLayout(12, 0)); 
            setOpaque(false); 
            setBorder(new EmptyBorder(0, 12, 0, 12)); 
            setMaximumSize(new Dimension(999, 42)); 
            setPreferredSize(new Dimension(228, 42)); 
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            
            lblIcon = new JLabel(); 
            try {
                com.formdev.flatlaf.extras.FlatSVGIcon svgIc = new com.formdev.flatlaf.extras.FlatSVGIcon("images/icon/" + iconName + ".svg", 20, 20);
                svgIc.setColorFilter(new com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter(c -> Color.BLACK));
                lblIcon.setIcon(svgIc);
            } catch(Exception ex) {
                try {
                    com.formdev.flatlaf.extras.FlatSVGIcon svgIc2 = new com.formdev.flatlaf.extras.FlatSVGIcon("images/icon_svg/" + iconName + ".svg", 20, 20);
                    svgIc2.setColorFilter(new com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter(c -> Color.BLACK));
                    lblIcon.setIcon(svgIc2);
                } catch(Exception ex2) {
                    setNetworkIcon(lblIcon, "https://img.icons8.com/fluency-systems-regular/48/73708A/" + iconName + ".png", 20, 20);
                }
            }
            
            lblText = new JLabel(title); 
            lblText.setFont(new Font("Segoe UI", Font.PLAIN, 13)); 
            lblText.setForeground(TEXT_MUTED);
            
            add(lblIcon, BorderLayout.WEST); 
            add(lblText, BorderLayout.CENTER);

            if (badgeCount > 0) {
                JPanel badgePanel = new JPanel(new BorderLayout()) { 
                    @Override protected void paintComponent(Graphics g) { 
                        Graphics2D g2 = (Graphics2D) g.create(); 
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
                        g2.setColor(PRIMARY); 
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getWidth(), getHeight()); 
                        g2.dispose(); 
                    } 
                };
                badgePanel.setOpaque(false); 
                badgePanel.setBorder(new EmptyBorder(2, 6, 2, 6));
                JLabel lblBadge = new JLabel(badgeCount > 99 ? "99+" : String.valueOf(badgeCount)); 
                lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 10)); 
                lblBadge.setForeground(Color.WHITE); 
                badgePanel.add(lblBadge, BorderLayout.CENTER);
                
                badgeWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10)); 
                badgeWrapper.setOpaque(false); 
                badgeWrapper.add(badgePanel); 
                add(badgeWrapper, BorderLayout.EAST);
            }

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { if(!isActive) { isHovered = true; repaint(); } }
                @Override public void mouseExited(MouseEvent e) { if(!isActive) { isHovered = false; repaint(); } }
                @Override public void mouseClicked(MouseEvent e) { switchTab(SidebarMenuItem.this, cardName); }
            });
        }

                public void setExpanded(boolean expanded) {
            lblText.setVisible(expanded);
            if(badgeWrapper != null) badgeWrapper.setVisible(expanded);
            if (expanded) {
                setPreferredSize(new Dimension(228, 42));
                setMaximumSize(new Dimension(999, 42));
            } else {
                setPreferredSize(new Dimension(42, 42));
                setMaximumSize(new Dimension(42, 42));
            }
            revalidate();
            repaint();
        }

        public void setActive(boolean active) {
            this.isActive = active;
            this.isHovered = false;
            Color iconColor = active ? PRIMARY : Color.BLACK;
            if (active) {
                lblText.setForeground(PRIMARY);
                lblText.setFont(new Font("Segoe UI", Font.BOLD, 13));
            } else {
                lblText.setForeground(TEXT_MUTED);
                lblText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            }
            try {
                com.formdev.flatlaf.extras.FlatSVGIcon svgIc = new com.formdev.flatlaf.extras.FlatSVGIcon("images/icon/" + iconName + ".svg", 20, 20);
                svgIc.setColorFilter(new com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter(c -> iconColor));
                lblIcon.setIcon(svgIc);
            } catch(Exception ex) {
                try {
                    com.formdev.flatlaf.extras.FlatSVGIcon svgIc2 = new com.formdev.flatlaf.extras.FlatSVGIcon("images/icon_svg/" + iconName + ".svg", 20, 20);
                    svgIc2.setColorFilter(new com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter(c -> iconColor));
                    lblIcon.setIcon(svgIc2);
                } catch(Exception ex2) {
                    String colorHex = active ? "7C3AED" : "000000";
                    setNetworkIcon(lblIcon, "https://img.icons8.com/fluency-systems-" + (active ? "filled" : "regular") + "/48/" + colorHex + "/" + iconName + ".png", 20, 20);
                }
            }
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create(); 
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isActive) {
                g2.setColor(PRIMARY_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            } else if (isHovered) {
                g2.setColor(HOVER_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }
            g2.dispose();
        }
    }

    private SidebarMenuItem createMenuItem(String title, String iconName, String cardName, int badgeCount) {
        SidebarMenuItem item = new SidebarMenuItem(title, iconName, cardName, badgeCount); 
        menuItems.add(item); 
        return item;
    }

    private void switchTab(SidebarMenuItem activeItem, String cardName) {
        for (SidebarMenuItem item : menuItems) { item.setActive(false); }
        activeItem.setActive(true); 
        if ("Reels".equals(cardName) && reelsTab != null) {
            reelsTab.setActive(true);
        } else if (reelsTab != null) {
            reelsTab.setActive(false);
        }
        cardLayout.show(mainCardPanel, cardName);
    }

    public void switchToCard(String cardName) {
        if ("Upgrade".equals(cardName)) {
            for (SidebarMenuItem item : menuItems) { item.setActive(false); }
        } else {
            for (SidebarMenuItem item : menuItems) {
                if(item.cardName.equals(cardName)) {
                    item.setActive(true);
                } else {
                    item.setActive(false);
                }
            }
        }
        if ("Reels".equals(cardName) && reelsTab != null) {
            reelsTab.setActive(true);
        } else if (reelsTab != null) {
            reelsTab.setActive(false);
        }
        cardLayout.show(mainCardPanel, cardName);
    }



    class PremiumCard extends JPanel {
        public PremiumCard() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); 
            setOpaque(false); 
            setBorder(new EmptyBorder(16, 16, 16, 16)); 
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            JLabel lblTitle = new JLabel("Nâng cấp tài khoản"); 
            lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14)); 
            lblTitle.setForeground(Color.WHITE);
            lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lblDesc = new JLabel("<html><div style='text-align:center; color: #EDE7FF; font-size: 11px; line-height: 1.4;'>Trải nghiệm nhiều tính năng hơn với gói Premium.</div></html>"); 
            lblDesc.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            JPanel btnCTA = new JPanel(new BorderLayout()) { 
                @Override protected void paintComponent(Graphics g) { 
                    Graphics2D g2 = (Graphics2D) g.create(); 
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
                    g2.setColor(new Color(255, 255, 255, 50)); 
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12); 
                    g2.dispose(); 
                } 
            };
            btnCTA.setOpaque(false); 
            btnCTA.setMaximumSize(new Dimension(999, 32)); 
            btnCTA.setBorder(new EmptyBorder(6, 0, 6, 0));
            
            JLabel lblBtnText = new JLabel("Nâng cấp ngay  →", SwingConstants.CENTER); 
            lblBtnText.setFont(new Font("Segoe UI", Font.BOLD, 12)); 
            lblBtnText.setForeground(Color.WHITE);
            btnCTA.add(lblBtnText, BorderLayout.CENTER); 
            btnCTA.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            add(lblTitle); add(Box.createVerticalStrut(6)); add(lblDesc); add(Box.createVerticalStrut(14)); add(btnCTA);
            
            addMouseListener(new MouseAdapter() { 
                public void mouseClicked(MouseEvent e) { 
                    switchToCard("Upgrade"); 
                } 
            });
        }
        @Override protected void paintComponent(Graphics g) { 
            Graphics2D g2 = (Graphics2D) g.create(); 
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
            GradientPaint gp = new GradientPaint(0, 0, Color.decode("#A78BFA"), getWidth(), getHeight(), Color.decode("#7C3AED")); 
            g2.setPaint(gp); 
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16); 
            g2.dispose(); 
            super.paintComponent(g); 
        }
    }

    private JPanel createMainArea() {
        JPanel mainArea = new JPanel(new BorderLayout());
        
        headerPanel = new HeaderPanel(this, this.userName);
        mainArea.add(headerPanel, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        mainCardPanel = new JPanel(cardLayout);
        
        homeTab = new HomeTab(); 
        profileTab = new ProfileTab();
        profileTab.setAvatarUpdateListener(newAvatar -> {
            if (headerPanel != null) headerPanel.updateAvatar(newAvatar);
        });
        
        todoTab = new TodoListTab();
        todoTab.setOnBackListener(() -> { if (!menuItems.isEmpty()) { switchTab(menuItems.get(0), "Home"); } });
        
        upgradeTab = new UpgradeTab();
        upgradeTab.setOnBackListener(() -> { if (!menuItems.isEmpty()) { switchTab(menuItems.get(0), "Home"); } });
        
        chatTab = new ChatTab(this.currentUserId);
        chatTab.bindGlobalSearchBar(headerPanel.getGlobalSearchInput(), headerPanel.getGlobalSearchContainer());
        chatTab.setOnSwitchToChatCallback(() -> switchToCard("Chat"));
        
        chatTab.setOnUnreadCountChanged(totalUnread -> {
            if (headerPanel != null) {
                headerPanel.updateChatBadge(totalUnread);
            }
        });
        
        headerPanel.bindChatTab(chatTab);

        scheduleTab = new ScheduleTab();
        acceptedTab = new AcceptedClassTab(this);
        blackboardManagerTab = new BlackboardManagerTab(this);

        
        // 👇 BƯỚC 2: Khởi tạo DriveTab 👇
        driveTab = new DriveTab(this.currentUserId);

        mainCardPanel.add(homeTab, "Home");
        classManagerTab = new ClassManagerTab(this);
        mainCardPanel.add(classManagerTab, "Saved"); 
        mainCardPanel.add(chatTab, "Chat"); 
        mainCardPanel.add(acceptedTab, "Taken"); 
        mainCardPanel.add(scheduleTab, "Schedule"); 
        mainCardPanel.add(blackboardManagerTab, "Blackboard"); 

        mainCardPanel.add(profileTab, "Profile");
        mainCardPanel.add(todoTab, "Todo"); 
        mainCardPanel.add(upgradeTab, "Upgrade"); 
        
        // 👇 BƯỚC 3: Thay JPanel rỗng bằng driveTab 👇
        mainCardPanel.add(driveTab, "Docs"); 
        
        reelsTab = new ReelsTabPanel();
        mainCardPanel.add(reelsTab, "Reels");

        examTab = new com.mycompany.tutorhub_enterprise.client.exam.ExamTab(this.currentUserId, "TUTOR", NetworkManager.getInstance());
        mainCardPanel.add(examTab, "Exam");

        JPanel mainWrapper = new JPanel(new BorderLayout());
        mainWrapper.setOpaque(true);
        mainWrapper.setBackground(Color.decode("#F0F2F5"));
        mainWrapper.setBorder(new EmptyBorder(16, 16, 16, 16));
        
        // Tạo bo góc và shadow cho content
        JPanel cardWrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Soft shadow
                g2.setColor(new Color(0, 0, 0, 18));
                g2.fillRoundRect(3, 5, getWidth() - 6, getHeight() - 4, 20, 20);
                // White card
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight() - 2, 20, 20);
                g2.dispose();
            }
        };
        cardWrapper.setOpaque(false);
        cardWrapper.setBorder(new EmptyBorder(0, 0, 0, 0));
        
        mainCardPanel.setOpaque(false);
        mainCardPanel.setBackground(Color.WHITE);
        mainCardPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        
        cardWrapper.add(mainCardPanel, BorderLayout.CENTER);
        mainWrapper.add(cardWrapper, BorderLayout.CENTER);
        mainArea.add(mainWrapper, BorderLayout.CENTER);
        
        if (!menuItems.isEmpty()) { switchTab(menuItems.get(0), "Home"); }
        return mainArea;
    }

    private void setupSplitPane(JPanel sidebar, JPanel mainArea) {
        sidebar.setMinimumSize(new Dimension(180, 0)); 
        sidebar.setPreferredSize(new Dimension(180, 0));
        mainArea.setMinimumSize(new Dimension(600, 0));

        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, mainArea);
        mainSplitPane.setContinuousLayout(true);
        mainSplitPane.setResizeWeight(0.0); 
        mainSplitPane.setBorder(null);
        mainSplitPane.setOpaque(false);
        mainSplitPane.setDividerSize(0); 
        mainSplitPane.setBackground(Color.decode("#F0F2F5"));
        
        add(mainSplitPane, BorderLayout.CENTER);
    }

    private void setNetworkIcon(JLabel label, String urlStr, int width, int height) {
        String key = urlStr + "_" + width + "x" + height;
        if (iconCache.containsKey(key)) { label.setIcon(iconCache.get(key)); return; }
        new Thread(() -> { try { ImageIcon raw = new ImageIcon(new URL(urlStr)); Image img = raw.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH); ImageIcon scaled = new ImageIcon(img); iconCache.put(key, scaled); SwingUtilities.invokeLater(() -> label.setIcon(scaled)); } catch (Exception e) {} }).start();
    }

    private void startNetworkListener() {
        new Thread(() -> {
            try {
                NetworkManager net = NetworkManager.getInstance();
                
                if (!net.isConnected()) {
                    net.connect("localhost", 8888);
                }
                net.sendPacket(new Packet("SYNC_SESSION", String.valueOf(this.currentUserId)));
                String myId = String.valueOf(this.currentUserId);
                
                net.sendPacket(new Packet("GET_ALL_CLASSES", ""));
                net.sendPacket(new Packet("GET_TASKS", "")); 
                net.sendPacket(new Packet("GET_PROFILE", "")); 
                net.sendPacket(new Packet("GET_USER_BOARDS", myId));
                net.sendPacket(new Packet("GET_CLASSROOM_LESSONS", ""));
                
                net.sendPacket(new Packet("GET_FULL_PROFILE", myId));
                net.sendPacket(new Packet("GET_DEGREES", myId));
                net.sendPacket(new Packet("GET_EXPERIENCES", myId));
                net.sendPacket(new Packet("GET_CERTIFICATES", myId));
                net.sendPacket(new Packet("GET_REELS", ""));
                net.sendPacket(new Packet("GET_LOCKET_VIDEOS", ""));
                
                while (net.isConnected()) {
                    Packet response = net.receivePacket(); 
                    handleServerMessage(response);
                }
            } catch (Exception e) { 
                if (headerPanel != null) {
                    headerPanel.addNotification("https://img.icons8.com/fluency/48/box-important.png", "Lỗi mạng", e.getMessage());
                }
            }
        }).start(); 
    }

    private void requestClassroomLessonsRefresh() {
        new Thread(() -> {
            try {
                NetworkManager.getInstance().sendPacket(new Packet("GET_CLASSROOM_LESSONS", ""));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    if (headerPanel != null) {
                        headerPanel.addNotification(
                                "https://img.icons8.com/fluency/48/box-important.png",
                                "Lessons",
                                "Cannot refresh lessons: " + ex.getMessage()
                        );
                    }
                });
            }
        }, "classroom-lessons-refresh").start();
    }

    @SuppressWarnings("unchecked")
    private void handleServerMessage(Packet packet) {
        SwingUtilities.invokeLater(() -> {
            try {
                if (packet == null || packet.action == null) return;
                
                if ("BROADCAST_CLASS".equals(packet.action)) {
                    if (homeTab != null) {
                        String[] data = packet.payload.split("\\|");
                        if(data.length >= 8) homeTab.displayNewClass(data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7]);
                    }
                } else if ("BROADCAST_LIVE_CLASS".equals(packet.action)) {
                    String[] data = packet.payload.split("\\|");
                    if (data.length >= 2) {
                        showLiveClassNotification(data[0], data[1]);
                    }
                } else if ("CLASS_TAKEN".equals(packet.action)) {
                    if (homeTab != null) homeTab.markClassAsTaken(packet.payload);
                    
                } else if ("FULL_PROFILE_RESULT".equals(packet.action)) {
                    if (profileTab != null && packet.payload != null && !packet.payload.isEmpty()) {
                        String[] profileDataArr = packet.payload.split(";;", -1);
                        profileTab.loadProfileData(profileDataArr);
                    }
                } else if ("DEGREES_RESULT".equals(packet.action)) {
                    if (profileTab != null && packet.data != null) {
                        profileTab.loadDegreesList((java.util.List<String>) packet.data);
                    }
                } else if ("EXPERIENCES_RESULT".equals(packet.action)) {
                    if (profileTab != null && packet.data != null) {
                        profileTab.loadExperiencesList((java.util.List<String>) packet.data);
                    }
                } else if ("CERTIFICATES_RESULT".equals(packet.action)) {
                    if (profileTab != null && packet.data != null) {
                        profileTab.loadCertificatesList((java.util.List<String>) packet.data);
                    }
                } else if ("GET_REELS_RESPONSE".equals(packet.action)) {
                    if (reelsTab != null && packet.data != null) {
                        reelsTab.loadReels((java.util.List<String>) packet.data);
                    }
                } else if ("GET_REEL_COMMENTS_RESPONSE".equals(packet.action)) {
                    if (reelsTab != null && packet.data != null) {
                        reelsTab.loadComments((java.util.List<String>) packet.data);
                    }
                } else if ("GET_LOCKET_VIDEOS_RESPONSE".equals(packet.action)) {
                    if (packet.data != null) {
                        locketVideos = (java.util.List<String>) packet.data;
                        if (homeTab != null) homeTab.loadReelsToVideoSection(locketVideos);
                        if (activeLocketPlayer != null && activeLocketPlayer.isDisplayable()) {
                            activeLocketPlayer.reloadVideos(locketVideos);
                        }
                    }
                } 
                
                else if ("ACCEPT_SUCCESS".equals(packet.action)) {
                    if (headerPanel != null) {
                        headerPanel.addNotification("https://img.icons8.com/fluency/48/ok.png", "Thành công", "Bạn đã nhận lớp " + packet.payload + "!");
                    }
                    if (homeTab != null) {
                        homeTab.markClassAsTaken(packet.payload); 
                        if (acceptedTab != null) {
                            HomeTab.ClassModel acceptedModel = homeTab.getClassModelById(packet.payload);
                            if (acceptedModel != null) { acceptedTab.addAcceptedClass(acceptedModel); }
                        }
                    }
                    try { NetworkManager.getInstance().sendPacket(new Packet("GET_TASKS", "")); } catch (Exception e) {}
                } else if ("ACCEPT_FAIL".equals(packet.action)) {
                    if (headerPanel != null) {
                        headerPanel.addNotification("https://img.icons8.com/fluency/48/cancel.png", "Thất bại", "Lớp " + packet.payload + " đã bị nhận trước!");
                    }
                    if (homeTab != null) {
                        homeTab.resetClassButton(packet.payload); 
                    }
                } else if ("RESPONSE".equals(packet.action)) {
                    if (headerPanel != null) {
                        headerPanel.addNotification(packet.success ? "https://img.icons8.com/fluency/48/ok.png" : "https://img.icons8.com/fluency/48/cancel.png", "Hệ thống", packet.message);
                    }
                } else if ("SYNC_TASKS".equals(packet.action)) {
                    if (todoTab != null) todoTab.updateTasksFromServer(packet.payload);
                    if (homeTab != null) homeTab.updateTodoWidget(packet.payload);
                } else if ("LOAD_AVATAR".equals(packet.action) || "UPDATE_AVATAR_SUCCESS".equals(packet.action)) {
                    try {
                        byte[] imageBytes = java.util.Base64.getDecoder().decode(packet.payload);
                        Image rawImg = new ImageIcon(imageBytes).getImage();
                        if (headerPanel != null) headerPanel.updateAvatar(rawImg);
                        if (profileTab != null) profileTab.updateAvatarFromBase64(packet.payload);
                    } catch (Exception e) {}
                } 
                else if ("SEARCH_USER_RESULT".equals(packet.action)) {
                    if (packet.data != null && chatTab != null) {
                        chatTab.updateSearchResults((List<UserInfo>) packet.data);
                    }
                } else if ("FRIEND_REQUEST_SENT".equals(packet.action) || "FRIEND_ACCEPTED".equals(packet.action)) {
                    if (headerPanel != null) {
                        headerPanel.addNotification("https://img.icons8.com/fluency/48/ok.png", "Kết bạn", packet.message);
                    }
                } else if ("GET_CONVO_LIST".equals(packet.action)) {
                    if (chatTab != null && packet.data != null) {
                        chatTab.updateConversationList((List<ConversationInfo>) packet.data);
                    }
                } else if ("GET_MESSAGES".equals(packet.action)) {
                    if (chatTab != null && packet.data != null) {
                        chatTab.updateMessages((List<Message>) packet.data);
                    }
                } else if ("RECEIVE_CHAT".equals(packet.action)) {
                    if (chatTab != null) {
                        String[] data = packet.payload.split("\\|", 3);
                        if(data.length >= 3) chatTab.appendIncomingMessage(data[0], data[1], data[2]);
                    }
                } else if ("SEND_CHAT_ACK".equals(packet.action)) {
                    if (chatTab != null) {
                        chatTab.handleSendAck(packet.payload);
                    }
                } else if ("MESSAGE_DELIVERED_ACK".equals(packet.action)) {
                    if (chatTab != null) {
                        chatTab.handleDeliveredAck(packet.payload);
                    }
                } else if ("TYPING".equals(packet.action)) {
                    if (chatTab != null) {
                        String[] data = packet.payload.split("\\|", 2);
                        if(data.length >= 2) chatTab.showTypingIndicator(data[0], data[1]);
                    }
                } else if ("CALL_INIT".equals(packet.action)) {
                    String[] data = packet.payload.split("\\|", 3);
                    if (data.length >= 3) {
                        com.mycompany.tutorhub_enterprise.client.managers.CallManager.getInstance().handleIncomingCall(data[0], data[1], data[2]);
                    }
                } else if ("CALL_ACCEPT".equals(packet.action)) {
                    String[] data = packet.payload.split("\\|", 2);
                    com.mycompany.tutorhub_enterprise.client.managers.CallManager.getInstance().handleCallAccept(data[0]);
                } else if ("CALL_REJECT".equals(packet.action)) {
                    String[] data = packet.payload.split("\\|", 2);
                    com.mycompany.tutorhub_enterprise.client.managers.CallManager.getInstance().handleCallReject(data[0]);
                } else if ("CALL_CANCEL".equals(packet.action)) {
                    String[] data = packet.payload.split("\\|", 2);
                    com.mycompany.tutorhub_enterprise.client.managers.CallManager.getInstance().handleCallCancel(data[0]);
                }
                else if ("READ_ACK".equals(packet.action)) {
                    if (chatTab != null) {
                        chatTab.markConversationAsRead(packet.payload);
                    }
                }
                else if ("USER_BOARDS_RESULT".equals(packet.action)) {
                    if (blackboardManagerTab != null) {
                        blackboardManagerTab.syncBoardsFromServer(packet.payload);
                    }
                }
               else if ("SAVE_BOARD_SUCCESS".equals(packet.action)) {
                    if (blackboardManagerTab != null) {
                        blackboardManagerTab.showSaveSuccessBanner();
                        try { NetworkManager.getInstance().sendPacket(new Packet("GET_USER_BOARDS", "")); } catch(Exception ex){}
                    }
                    switchToCard("Blackboard");
                } 
                else if ("GET_CLASSROOMS_RESPONSE".equals(packet.action)) {
                    if (classManagerTab != null && packet.data != null) {
                        classManagerTab.loadClassrooms((List<com.mycompany.tutorhub_enterprise.models.ClassroomGroupModel>) packet.data);
                    }
                }
                else                 if ("GET_EXAMS_RESPONSE".equals(packet.action) || "EXAM_LIST".equals(packet.action)) {
                    if (examTab != null && packet.data != null) {
                        examTab.updateExamList((java.util.List<com.mycompany.tutorhub_enterprise.models.exam.Exam>) packet.data);
                    }
                }

                if ("GET_CLASSROOM_LESSONS_RESPONSE".equals(packet.action)) {
                    if (classManagerTab != null && packet.data != null) {
                        classManagerTab.syncLessonsFromServer((List<ClassroomLessonModel>) packet.data);
                    }
                }
                else if ("CREATE_CLASSROOM_AND_ENTER_SUCCESS".equals(packet.action)) {
                    String[] data = packet.payload != null ? packet.payload.split("\\|", 4) : new String[0];
                    if (data.length >= 4) {
                        if (headerPanel != null) {
                            headerPanel.addNotification("https://img.icons8.com/fluency/48/ok.png", "Thanh cong", packet.message);
                        }
                        requestClassroomLessonsRefresh();
                        openLiveLesson(data[0], data[1], data[2], data[3], "teacher");
                    }
                }
                else if ("CREATE_CLASSROOM_AND_ENTER_FAIL".equals(packet.action)) {
                    if (headerPanel != null) {
                        headerPanel.addNotification("https://img.icons8.com/fluency/48/cancel.png", "That bai", packet.message);
                    }
                }
                else if ("CREATE_PUBLIC_LESSON_SUCCESS".equals(packet.action)) {
                    if (headerPanel != null) {
                        headerPanel.addNotification("https://img.icons8.com/fluency/48/ok.png", "Public Lesson", packet.message);
                    }
                    requestClassroomLessonsRefresh();
                    showPublicLessonInvite(packet.payload);
                }
                else if ("CREATE_PUBLIC_LESSON_FAIL".equals(packet.action)) {
                    if (headerPanel != null) {
                        headerPanel.addNotification("https://img.icons8.com/fluency/48/cancel.png", "Public Lesson", packet.message);
                    }
                }
                else if ("JOIN_PUBLIC_LESSON_SUCCESS".equals(packet.action)) {
                    if (packet.data instanceof ClassroomLessonModel) {
                        ClassroomLessonModel lesson = (ClassroomLessonModel) packet.data;
                        if (headerPanel != null) {
                            headerPanel.addNotification("https://img.icons8.com/fluency/48/ok.png", "Public Lesson", packet.message);
                        }
                        openStudentPublicLesson(lesson);
                    }
                }
                else if ("JOIN_PUBLIC_LESSON_WAITING".equals(packet.action)) {
                    if (headerPanel != null) {
                        headerPanel.addNotification("https://img.icons8.com/fluency/48/hourglass.png", "Waiting Room", packet.message);
                    }
                    JOptionPane.showMessageDialog(
                            MainDashboard.this,
                            "Your request was sent to the teacher. Please wait for approval.",
                            "Waiting Room",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
                else if ("JOIN_PUBLIC_LESSON_FAIL".equals(packet.action)) {
                    if (headerPanel != null) {
                        headerPanel.addNotification("https://img.icons8.com/fluency/48/cancel.png", "Public Lesson", packet.message);
                    }
                }
                else if ("GET_PUBLIC_LESSON_WAITING_ROOM_RESPONSE".equals(packet.action)) {
                    if (classManagerTab != null && packet.data != null) {
                        classManagerTab.showWaitingRoom((List<ClassroomMemberModel>) packet.data);
                    }
                }
                else if ("PUBLIC_LESSON_WAITING_ROOM_UPDATED".equals(packet.action)) {
                    if (headerPanel != null) {
                        headerPanel.addNotification("https://img.icons8.com/fluency/48/queue.png", "Waiting Room", packet.message);
                    }
                }
                else if ("PUBLIC_LESSON_APPROVED".equals(packet.action)) {
                    if (packet.data instanceof ClassroomLessonModel) {
                        ClassroomLessonModel lesson = (ClassroomLessonModel) packet.data;
                        if (headerPanel != null) {
                            headerPanel.addNotification("https://img.icons8.com/fluency/48/ok.png", "Waiting Room", packet.message);
                        }
                        openStudentPublicLesson(lesson);
                    }
                }
                else if ("APPROVE_PUBLIC_LESSON_STUDENT_FAIL".equals(packet.action)) {
                    if (headerPanel != null) {
                        headerPanel.addNotification("https://img.icons8.com/fluency/48/cancel.png", "Waiting Room", packet.message);
                    }
                }
                else if ("CREATE_CLASSROOM_SUCCESS".equals(packet.action)) {
                    if (headerPanel != null) {
                        headerPanel.addNotification("https://img.icons8.com/fluency/48/ok.png", "Thành công", packet.message);
                    }
                }
                else if ("CREATE_CLASSROOM_FAIL".equals(packet.action)) {
                    if (headerPanel != null) {
                        headerPanel.addNotification("https://img.icons8.com/fluency/48/cancel.png", "Thất bại", packet.message);
                    }
                }
               else if ("DOWNLOAD_FILE_RESPONSE".equals(packet.action)) {
                    String base64Data = packet.payload;
                    if (base64Data != null && !base64Data.equals("ERROR")) {
                        try {
                            byte[] fileBytes = java.util.Base64.getDecoder().decode(base64Data);
                            
                            // NẾU ĐANG BẬT CỜ XEM TRƯỚC (PREVIEW)
                            if (com.mycompany.tutorhub_enterprise.client.ProfileTab.isPreviewingFile) {
                                java.io.File tempFile = java.io.File.createTempFile("cv_preview_", ".pdf");
                                tempFile.deleteOnExit(); 
                                java.nio.file.Files.write(tempFile.toPath(), fileBytes);
                                
                                if (Desktop.isDesktopSupported()) {
                                    Desktop.getDesktop().open(tempFile);
                                }
                                com.mycompany.tutorhub_enterprise.client.ProfileTab.isPreviewingFile = false; // Tắt cờ
                            } 
                            // NẾU LÀ TẢI XUỐNG BÌNH THƯỜNG
                            else {
                                java.io.File saveTarget = com.mycompany.tutorhub_enterprise.client.ProfileTab.pendingDownloadFile;
                                if (saveTarget != null) {
                                    java.nio.file.Files.write(saveTarget.toPath(), fileBytes);
                                    JOptionPane.showMessageDialog(MainDashboard.this, 
                                        "Tải xuống thành công!\nĐã lưu tại: " + saveTarget.getAbsolutePath(), 
                                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                                    com.mycompany.tutorhub_enterprise.client.ProfileTab.pendingDownloadFile = null; 
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(MainDashboard.this, "Lỗi khi xử lý file!", "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        com.mycompany.tutorhub_enterprise.client.ProfileTab.isPreviewingFile = false; // Reset cờ nếu lỗi
                        JOptionPane.showMessageDialog(MainDashboard.this, "Lỗi: Không tìm thấy file trên server!", "Thất bại", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (Exception e) {
                System.err.println("[CLIENT LỖI] Lỗi khi xử lý gói tin: " + e.getMessage());
            }
        });
    }

    private void showLiveClassNotification(String classId, String className) {
        JDialog popup = new JDialog(this, false);
        popup.setUndecorated(true);
        popup.setBackground(new Color(0,0,0,0));
        popup.setAlwaysOnTop(true);
        
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setBackground(Color.decode("#1E293B"));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.decode("#3B82F6"), 2),
            new EmptyBorder(15, 20, 15, 20)
        ));
        
        JLabel icon = new JLabel();
        try {
            ImageIcon raw = new ImageIcon(new URL("https://img.icons8.com/fluency/48/classroom.png"));
            Image img = raw.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            icon.setIcon(new ImageIcon(img));
        } catch (Exception e) {}
        
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Giáo viên vừa mở lớp học trực tuyến!");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(Color.WHITE);
        JLabel lblName = new JLabel("Lớp: " + className + " (" + classId + ")");
        lblName.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblName.setForeground(Color.decode("#94A3B8"));
        centerPanel.add(lblTitle);
        centerPanel.add(Box.createVerticalStrut(4));
        centerPanel.add(lblName);
        
        JButton btnJoin = new JButton("Vào lớp ngay");
        btnJoin.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnJoin.setBackground(Color.decode("#10B981"));
        btnJoin.setForeground(Color.WHITE);
        btnJoin.setFocusPainted(false);
        btnJoin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnJoin.setBorder(new EmptyBorder(8, 15, 8, 15));
        
        btnJoin.addActionListener(e -> {
            popup.dispose();
            openLiveClassroom(classId, className, "student");
        });
        
        panel.add(icon, BorderLayout.WEST);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(btnJoin, BorderLayout.EAST);
        popup.add(panel);
        
        popup.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        popup.setLocation(screenSize.width - popup.getWidth() - 20, screenSize.height - popup.getHeight() - 50);
        
        // Slide up animation
        int finalY = popup.getLocation().y;
        popup.setLocation(popup.getLocation().x, screenSize.height);
        popup.setVisible(true);
        
        javax.swing.Timer slideTimer = new javax.swing.Timer(10, null);
        slideTimer.addActionListener(e -> {
            int currentY = popup.getLocation().y;
            if (currentY > finalY) {
                popup.setLocation(popup.getLocation().x, currentY - 5);
            } else {
                slideTimer.stop();
            }
        });
        slideTimer.start();
        
        // Auto hide after 15s
        new javax.swing.Timer(15000, e -> popup.dispose()).start();
    }

    private void showPublicLessonInvite(String payload) {
        String[] data = payload != null ? payload.split("\\|", 6) : new String[0];
        if (data.length < 6 || data[5] == null || data[5].trim().isEmpty()) {
            return;
        }

        String lessonTitle = data[3] == null || data[3].trim().isEmpty() ? "Public Lesson" : data[3].trim();
        InviteLinkDialog dialog = new InviteLinkDialog(this, lessonTitle, data[5]);
        dialog.setVisible(true);
    }

    private void openStudentPublicLesson(ClassroomLessonModel lesson) {
        openLiveLesson(
                String.valueOf(lesson.getClassroomId()),
                String.valueOf(lesson.getId()),
                lesson.getBoardId() != null ? lesson.getBoardId() : "LESSON_" + lesson.getId(),
                lesson.getTitle() != null ? lesson.getTitle() : "Public Lesson",
                "student"
        );
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf()); } catch (Exception ex) {}
        SwingUtilities.invokeLater(() -> new MainDashboard(1, "Bá Sáng").setVisible(true));
    }
}

