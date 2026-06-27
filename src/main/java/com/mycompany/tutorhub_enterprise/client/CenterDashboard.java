package com.mycompany.tutorhub_enterprise.client;

import com.formdev.flatlaf.FlatLightLaf;
import com.mycompany.tutorhub_enterprise.models.ConversationInfo;
import com.mycompany.tutorhub_enterprise.models.Message;
import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.models.UserInfo;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.List;

public class CenterDashboard extends JFrame {

    // --- MÀU SẮC CHUẨN THEO MOCKUP ---
    private final Color BG_MAIN = Color.decode("#F4F7F9");
    private final Color BG_WHITE = Color.WHITE;
    private final Color PRIMARY_BLUE = Color.decode("#2563EB");
    private final Color SUCCESS_GREEN = Color.decode("#10B981");
    private final Color WARNING_ORANGE = Color.decode("#F59E0B");
    private final Color DANGER_RED = Color.decode("#EF4444");
    private final Color TEXT_MAIN = Color.decode("#1F2937");
    private final Color TEXT_MUTED = Color.decode("#6B7280");
    private final Color BORDER_COLOR = Color.decode("#E5E7EB");

    private int adminId;
    private String adminName;
    private JLabel lblAvatar; 
    private AdminProfileTab adminProfileTab;
    private boolean hasCustomAvatar = false;
    private ChatTab chatTab;
    private TutorManagementTab tutorManagementTab;
    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private JPanel rightInfoPanel;
    private DefaultTableModel overviewTutorTableModel;
    
    // ĐÃ SỬA: Đổi tên biến sang class mới AdminTutorProfileTab
    private AdminTutorProfileTab adminTutorProfileTab;
    private JPanel sidebar;
    private JPanel premiumBox;
    private JLabel lblLogoText;
    private JPanel logoWrap;
    private java.util.List<JLabel> menuLabels = new java.util.ArrayList<>();
    private boolean isSidebarExpanded = true;

    public CenterDashboard(int adminId, String adminName, String avatarBase64) {
        this.adminId = adminId;
        this.adminName = adminName;

        setTitle("TutorHub Enterprise - Hệ thống Quản trị Trung tâm");
        setSize(1366, 768);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        initMenuBar();
        sidebar = createSidebar();
        add(sidebar, BorderLayout.WEST);
        
        JPanel rightWrap = new JPanel(new BorderLayout());
        rightWrap.add(createTopToolbar(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(BG_MAIN);
        
        adminProfileTab = new AdminProfileTab(adminName);
        adminProfileTab.setAvatarUpdateListener(new AdminProfileTab.AvatarUpdateListener() {
            @Override
            public void onAvatarUpdated(Image newAvatarImage) {
                if (lblAvatar != null) {
                    lblAvatar.setIcon(getCircularImageIcon(newAvatarImage, 36));
                    lblAvatar.revalidate();
                    lblAvatar.repaint();
                }
            }
        });
        mainContentPanel.add(adminProfileTab, "ADMIN_PROFILE");
        chatTab = new ChatTab(adminId); 
        mainContentPanel.add(chatTab, "CHAT_TAB");
        mainContentPanel.add(createOverviewDashboard(), "DASHBOARD"); 
        
        rightWrap.add(mainContentPanel, BorderLayout.CENTER); 
        add(rightWrap, BorderLayout.CENTER);
        cardLayout.show(mainContentPanel, "DASHBOARD"); 

        // Khởi tạo tab Quản lý gia sư
        tutorManagementTab = new TutorManagementTab(this);
        mainContentPanel.add(tutorManagementTab, "TUTOR_MANAGEMENT");
        
        // ĐÃ SỬA: Khởi tạo tab Chi tiết hồ sơ gia sư mặc định (ẩn)
        // Chờ đến khi Admin click vào gia sư cụ thể thì mới nạp ID thực tế vào
        adminTutorProfileTab = new AdminTutorProfileTab(-1);
        mainContentPanel.add(adminTutorProfileTab, "ADMIN_TUTOR_PROFILE_VIEW");

        if (avatarBase64 != null && !avatarBase64.trim().isEmpty() && !avatarBase64.equals("NO_AVATAR")) {
           hasCustomAvatar = true; 
           adminProfileTab.updateAvatarFromBase64(avatarBase64);
            try {
                byte[] bytes = java.util.Base64.getDecoder().decode(avatarBase64);
                Image img = new ImageIcon(bytes).getImage();
                lblAvatar.setIcon(getCircularImageIcon(img, 36));
            } catch (Exception ignored) {}
        }

        listenToServer();
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(BG_WHITE);
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        String[] menus = {"Tệp", "Chỉnh sửa", "Xem", "Công cụ", "Trợ giúp"};
        for (String m : menus) {
            JMenu menu = new JMenu(m);
            menu.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            menu.setForeground(TEXT_MAIN);
            menu.setBorder(new EmptyBorder(4, 8, 4, 8));
            menuBar.add(menu);
        }
        setJMenuBar(menuBar);
    }

    private JPanel createTopToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(BG_WHITE);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(10, 20, 10, 20)
        ));

        JPanel leftNav = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftNav.setOpaque(false);
        leftNav.add(createIconButton("https://img.icons8.com/fluency-systems-regular/48/6B7280/back.png", "Quay lại"));
        leftNav.add(createIconButton("https://img.icons8.com/fluency-systems-regular/48/6B7280/forward.png", "Tiến tới"));
        leftNav.add(createIconButton("https://img.icons8.com/fluency-systems-regular/48/6B7280/refresh.png", "Làm mới"));
        
        leftNav.add(Box.createHorizontalStrut(10));
        JLabel separator = new JLabel("|"); separator.setForeground(BORDER_COLOR);
        leftNav.add(separator);
        leftNav.add(Box.createHorizontalStrut(10));
        
        JPanel btnAddTutor = createTextIconButton("https://img.icons8.com/fluency-systems-regular/48/2563EB/add-user-male.png", "Thêm gia sư", PRIMARY_BLUE);
        btnAddTutor.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CreateTutorDialog dialog = new CreateTutorDialog(CenterDashboard.this, () -> {
                    System.out.println("Thêm gia sư thành công! Làm mới bảng gia sư...");
                });
                dialog.setVisible(true);
            }
        });
        leftNav.add(btnAddTutor);
        
        JPanel btnCreateClass = createTextIconButton("https://img.icons8.com/fluency-systems-regular/48/10B981/classroom.png", "Tạo lớp học", SUCCESS_GREEN);
        btnCreateClass.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CreateClassDialog dialog = new CreateClassDialog(CenterDashboard.this, () -> {
                    System.out.println("Tạo lớp thành công! Cập nhật lại UI...");
                });
                dialog.setVisible(true);
            }
        });
        leftNav.add(btnCreateClass);

        leftNav.add(createTextIconButton("https://img.icons8.com/fluency-systems-regular/48/10B981/money.png", "Doanh thu", SUCCESS_GREEN));
        leftNav.add(createTextIconButton("https://img.icons8.com/fluency-systems-regular/48/2563EB/combo-chart.png", "Báo cáo", PRIMARY_BLUE));

        toolbar.add(leftNav, BorderLayout.WEST);

        JPanel rightNav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        rightNav.setOpaque(false);

        JPanel searchBox = new JPanel(new BorderLayout());
        searchBox.setBackground(BG_MAIN);
        searchBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(5, 15, 5, 15)
        ));
        searchBox.setPreferredSize(new Dimension(250, 36));
        JTextField txtSearch = new JTextField();
        txtSearch.setBorder(null);
        txtSearch.setBackground(BG_MAIN);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm kiếm (Ctrl+K)");
        searchBox.add(new JLabel(new ImageIcon(getScaledImage("https://img.icons8.com/fluency-systems-regular/48/9CA3AF/search.png", 18, 18))), BorderLayout.WEST);
        searchBox.add(txtSearch, BorderLayout.CENTER);
        rightNav.add(searchBox);

        rightNav.add(createIconButton("https://img.icons8.com/fluency-systems-regular/48/EF4444/bell.png", "Thông báo"));

        JPanel adminInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        adminInfo.setOpaque(false);
        adminInfo.setCursor(new Cursor(Cursor.HAND_CURSOR)); 

        lblAvatar = new JLabel(); 
        setAvatarNetworkIcon(lblAvatar, "https://img.icons8.com/color/144/circled-user-male-skin-type-4--v1.png", 36);
        
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.setOpaque(false);
        JLabel lblName = new JLabel(adminName);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblName.setForeground(TEXT_MAIN);
        JLabel lblRole = new JLabel("Quản trị viên");
        lblRole.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblRole.setForeground(TEXT_MUTED);
        namePanel.add(lblName);
        namePanel.add(lblRole);

        adminInfo.add(lblAvatar);
        adminInfo.add(namePanel);
        
        JPopupMenu profileMenu = new JPopupMenu();
        profileMenu.setBackground(Color.WHITE);
        profileMenu.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        JMenuItem mnuProfile = createProfileMenuItem("Hồ sơ quản trị", "user", null);
        JMenuItem mnuSettings = createProfileMenuItem("Cài đặt hệ thống", "settings", null);
        JMenuItem mnuLogout = createProfileMenuItem("Đăng xuất", "exit", DANGER_RED); 

        mnuProfile.addActionListener(e -> cardLayout.show(mainContentPanel, "ADMIN_PROFILE"));
        mnuSettings.addActionListener(e -> cardLayout.show(mainContentPanel, "ADMIN_PROFILE"));
        mnuLogout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn đăng xuất?", "Đăng xuất", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                dispose(); 
                new LoginFrame().setVisible(true); 
            }
        });

        profileMenu.add(mnuProfile);
        profileMenu.add(mnuSettings);
        profileMenu.addSeparator();
        profileMenu.add(mnuLogout);

        adminInfo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                profileMenu.show(adminInfo, 0, adminInfo.getHeight() + 5);
            }
        });

        rightNav.add(adminInfo);
        toolbar.add(rightNav, BorderLayout.EAST);
        return toolbar;
    }

    private JPanel createSidebar() {
        JPanel sidebarPanel = new JPanel(new BorderLayout());
        sidebarPanel.setBackground(BG_WHITE);
        sidebarPanel.setPreferredSize(new Dimension(240, 0)); // Expanded width
        sidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR));

        // Logo & Toggle Button Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BG_WHITE);
        headerPanel.setBorder(new EmptyBorder(15, 15, 10, 15));

        logoWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logoWrap.setOpaque(false);
        JLabel lblLogoIcon = new JLabel();
        setNetworkIcon(lblLogoIcon, "https://img.icons8.com/color/48/book.png", 32, 32);
        
        lblLogoText = new JLabel("TutorHub");
        lblLogoText.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblLogoText.setForeground(Color.decode("#6366F1")); 
        
        logoWrap.add(lblLogoIcon);
        logoWrap.add(lblLogoText);
        
        headerPanel.add(logoWrap, BorderLayout.WEST);

        // Toggle Button
        JPanel btnToggle = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_WHITE);
                g2.fillOval(0,0,getWidth(),getHeight());
                g2.setColor(BORDER_COLOR);
                g2.drawOval(0,0,getWidth()-1,getHeight()-1);
                g2.dispose();
            }
        };
        btnToggle.setOpaque(false);
        btnToggle.setPreferredSize(new Dimension(32, 32));
        btnToggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JLabel lblToggleIcon = new JLabel("«", SwingConstants.CENTER);
        lblToggleIcon.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblToggleIcon.setForeground(TEXT_MUTED);
        btnToggle.add(lblToggleIcon, BorderLayout.CENTER);

        btnToggle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                isSidebarExpanded = !isSidebarExpanded;
                if(isSidebarExpanded) {
                    sidebarPanel.setPreferredSize(new Dimension(240, 0));
                    lblLogoText.setVisible(true);
                    premiumBox.setVisible(true);
                    for(JLabel l : menuLabels) l.setVisible(true);
                    lblToggleIcon.setText("«");
                } else {
                    sidebarPanel.setPreferredSize(new Dimension(70, 0));
                    lblLogoText.setVisible(false);
                    premiumBox.setVisible(false);
                    for(JLabel l : menuLabels) l.setVisible(false);
                    lblToggleIcon.setText("»");
                }
                sidebarPanel.revalidate();
                sidebarPanel.repaint();
            }
        });
        
        headerPanel.add(btnToggle, BorderLayout.EAST);
        sidebarPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel menuList = new JPanel();
        menuList.setLayout(new BoxLayout(menuList, BoxLayout.Y_AXIS));
        menuList.setBackground(BG_WHITE);
        menuList.setBorder(new EmptyBorder(10, 10, 10, 10));

        menuList.add(createSidebarMenu("Bảng tin", "https://img.icons8.com/fluency-systems-regular/48/6B7280/home.png", false));
        menuList.add(Box.createVerticalStrut(5));
        menuList.add(createSidebarMenu("Lớp đã lưu", "https://img.icons8.com/fluency-systems-regular/48/6B7280/bookmark-ribbon.png", false));
        menuList.add(Box.createVerticalStrut(20));
        
        JLabel lblQuanLy = new JLabel("QUẢN LÝ"); lblQuanLy.setFont(new Font("Segoe UI", Font.BOLD, 10)); lblQuanLy.setForeground(TEXT_MUTED);
        menuLabels.add(lblQuanLy);
        JPanel pQuanLy = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0)); pQuanLy.setOpaque(false); pQuanLy.add(lblQuanLy);
        menuList.add(pQuanLy);
        menuList.add(Box.createVerticalStrut(10));
        
        menuList.add(createSidebarMenu("Lớp học", "https://img.icons8.com/fluency-systems-regular/48/6B7280/briefcase.png", false));
        menuList.add(Box.createVerticalStrut(5));
        menuList.add(createSidebarMenu("Lịch", "https://img.icons8.com/fluency-systems-regular/48/6B7280/calendar.png", false));
        menuList.add(Box.createVerticalStrut(20));
        
        JLabel lblCongCu = new JLabel("CÔNG CỤ"); lblCongCu.setFont(new Font("Segoe UI", Font.BOLD, 10)); lblCongCu.setForeground(TEXT_MUTED);
        menuLabels.add(lblCongCu);
        JPanel pCongCu = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0)); pCongCu.setOpaque(false); pCongCu.add(lblCongCu);
        menuList.add(pCongCu);
        menuList.add(Box.createVerticalStrut(10));
        
        menuList.add(createSidebarMenu("Tin nhắn", "https://img.icons8.com/fluency-systems-regular/48/6B7280/chat.png", false));
        menuList.add(Box.createVerticalStrut(5));
        menuList.add(createSidebarMenu("Bảng vẽ", "https://img.icons8.com/fluency-systems-regular/48/6B7280/paint-palette.png", false));
        menuList.add(Box.createVerticalStrut(5));
        menuList.add(createSidebarMenu("Tài liệu", "https://img.icons8.com/fluency-systems-regular/48/6B7280/document.png", false));
        menuList.add(Box.createVerticalStrut(20));

        JLabel lblCaNhan = new JLabel("CÁ NHÂN"); lblCaNhan.setFont(new Font("Segoe UI", Font.BOLD, 10)); lblCaNhan.setForeground(TEXT_MUTED);
        menuLabels.add(lblCaNhan);
        JPanel pCaNhan = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0)); pCaNhan.setOpaque(false); pCaNhan.add(lblCaNhan);
        menuList.add(pCaNhan);
        menuList.add(Box.createVerticalStrut(10));

        menuList.add(createSidebarMenu("Hồ sơ (eKYC)", "https://img.icons8.com/fluency-systems-regular/48/6B7280/user.png", false));
        menuList.add(Box.createVerticalStrut(5));
        menuList.add(createSidebarMenu("Nhiệm vụ", "https://img.icons8.com/fluency-systems-regular/48/6B7280/checked-checkbox.png", false));
        menuList.add(Box.createVerticalStrut(5));
        menuList.add(createSidebarMenu("Reels", "https://img.icons8.com/fluency-systems-regular/48/6366F1/video-playlist.png", true));

        sidebarPanel.add(menuList, BorderLayout.CENTER);

        premiumBox = new JPanel();
        premiumBox.setLayout(new BoxLayout(premiumBox, BoxLayout.Y_AXIS));
        premiumBox.setBackground(Color.decode("#8B5CF6")); 
        premiumBox.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel lblTitle = new JLabel("Nâng cấp tài khoản");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(BG_WHITE);
        JLabel lblPack = new JLabel("<html>Trải nghiệm nhiều tính năng<br>hơn với gói Premium.</html>");
        lblPack.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblPack.setForeground(new Color(255, 255, 255, 200));
        
        JButton btnUpgrade = new JButton("Nâng cấp ngay →");
        btnUpgrade.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnUpgrade.setForeground(BG_WHITE);
        btnUpgrade.setBackground(new Color(255, 255, 255, 50));
        btnUpgrade.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btnUpgrade.setFocusPainted(false);
        btnUpgrade.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnUpgrade.setAlignmentX(Component.LEFT_ALIGNMENT);

        premiumBox.add(lblTitle);
        premiumBox.add(Box.createVerticalStrut(5));
        premiumBox.add(lblPack);
        premiumBox.add(Box.createVerticalStrut(10));
        premiumBox.add(btnUpgrade);
        
        JPanel bottomWrap = new JPanel(new BorderLayout());
        bottomWrap.setOpaque(false);
        bottomWrap.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JPanel roundedPremium = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode("#8B5CF6"));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        roundedPremium.setOpaque(false);
        roundedPremium.add(premiumBox, BorderLayout.CENTER);
        bottomWrap.add(roundedPremium, BorderLayout.CENTER);

        sidebarPanel.add(bottomWrap, BorderLayout.SOUTH);
        return sidebarPanel;
    }

    private JPanel createSidebarMenu(String text, String iconUrl, boolean isActive) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                if(isActive) {
                    Graphics2D g2 = (Graphics2D)g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.decode("#F3E8FF"));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                    g2.dispose();
                }
            }
        };
        p.setOpaque(false);
        p.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel icon = new JLabel();
        setNetworkIcon(icon, iconUrl, 20, 20);
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", isActive ? Font.BOLD : Font.PLAIN, 14));
        lbl.setForeground(isActive ? Color.decode("#6366F1") : TEXT_MAIN);
        
        menuLabels.add(lbl);

        p.add(icon);
        p.add(lbl);

        wrapper.add(p, BorderLayout.CENTER);

        wrapper.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (text.equals("Tổng quan trung tâm") || text.equals("Bảng tin")) {
                    cardLayout.show(mainContentPanel, "DASHBOARD");
                } else if (text.equals("Cài đặt")) {
                    cardLayout.show(mainContentPanel, "ADMIN_PROFILE");
                } else if (text.equals("Tin nhắn")) {
                    cardLayout.show(mainContentPanel, "CHAT_TAB");
                } 
                else if (text.equals("Quản lý gia sư") || text.equals("Hồ sơ gia sư")) {
                    cardLayout.show(mainContentPanel, "TUTOR_MANAGEMENT");
                } 
                else {
                    JOptionPane.showMessageDialog(wrapper, "Module '" + text + "' đang được phát triển!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        return wrapper;
    }

    private JPanel createOverviewDashboard() {
        JPanel dashboard = new JPanel(new BorderLayout(20, 20));
        dashboard.setOpaque(false);
        dashboard.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titleWrap = new JPanel();
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.setOpaque(false);
        JLabel title = new JLabel("Bảng điều hành trung tâm gia sư");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_MAIN);
        JLabel subTitle = new JLabel("Theo dõi hoạt động gia sư, lớp học, doanh thu và hiệu suất vận hành.");
        subTitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subTitle.setForeground(TEXT_MUTED);
        titleWrap.add(title);
        titleWrap.add(subTitle);
        header.add(titleWrap, BorderLayout.WEST);

        JPanel monthFilter = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        monthFilter.setOpaque(false);
        JComboBox<String> cbMonth = new JComboBox<>(new String[]{"Tháng 5, 2025", "Tháng 4, 2025", "Tháng 3, 2025"});
        cbMonth.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbMonth.setBackground(BG_WHITE);
        monthFilter.add(cbMonth);
        header.add(monthFilter, BorderLayout.EAST);
        
        dashboard.add(header, BorderLayout.NORTH);

        JPanel centerContent = new JPanel(new BorderLayout(20, 20));
        centerContent.setOpaque(false);

        JPanel kpiRow = new JPanel(new GridLayout(1, 5, 20, 0));
        kpiRow.setOpaque(false);
        kpiRow.add(createKPICard("128", "Gia sư", "Đang hoạt động", "https://img.icons8.com/fluency-systems-regular/48/2563EB/user.png", PRIMARY_BLUE));
        kpiRow.add(createKPICard("342", "Lớp học", "Đang quản lý", "https://img.icons8.com/fluency-systems-regular/48/10B981/graduation-cap.png", SUCCESS_GREEN));
        kpiRow.add(createKPICard("286.5M", "Doanh thu tháng", "↑ 12.5% so với tháng trước", "https://img.icons8.com/fluency-systems-regular/48/F59E0B/money.png", WARNING_ORANGE));
        kpiRow.add(createKPICard("92%", "Tỷ lệ lấp lớp", "↑ 5% so với tháng trước", "https://img.icons8.com/fluency-systems-regular/48/8B5CF6/pie-chart.png", Color.decode("#8B5CF6")));
        kpiRow.add(createKPICard("24", "Hồ sơ chờ", "Chờ duyệt", "https://img.icons8.com/fluency-systems-regular/48/EF4444/pass.png", DANGER_RED));
        
        centerContent.add(kpiRow, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);
        splitPane.setDividerSize(0);

        JPanel leftDataPanel = new JPanel();
        leftDataPanel.setLayout(new BoxLayout(leftDataPanel, BoxLayout.Y_AXIS));
        leftDataPanel.setOpaque(false);

        leftDataPanel.add(createSectionPanel("Gia sư nổi bật / Danh sách gia sư", createTutorTable(), "Xem tất cả gia sư"));
        leftDataPanel.add(Box.createVerticalStrut(20));

        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 20, 0));
        bottomRow.setOpaque(false);
        bottomRow.add(createSectionPanel("Lớp học gần đây", createClassTable(), "Xem tất cả lớp học"));
        bottomRow.add(createSectionPanel("Doanh thu theo tháng", createRevenueChartPlaceholder(), "12 tháng qua ▾"));
        
        leftDataPanel.add(bottomRow);

        splitPane.setLeftComponent(leftDataPanel);

        rightInfoPanel = createTutorDetailPanel();
        splitPane.setRightComponent(rightInfoPanel);
        splitPane.setResizeWeight(0.72);

        centerContent.add(splitPane, BorderLayout.CENTER);
        dashboard.add(centerContent, BorderLayout.CENTER);

        return dashboard;
    }

    private JPanel createKPICard(String value, String title, String subText, String iconUrl, Color iconColor) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(BG_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel lblIcon = new JLabel();
        setNetworkIcon(lblIcon, iconUrl, 42, 42);
        card.add(lblIcon, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblValue.setForeground(TEXT_MAIN);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblTitle.setForeground(TEXT_MAIN);

        JLabel lblSub = new JLabel(subText);
        lblSub.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblSub.setForeground(iconColor);

        textPanel.add(lblValue);
        textPanel.add(lblTitle);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(lblSub);

        card.add(textPanel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createSectionPanel(String title, Component content, String rightText) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_WHITE);
        p.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(15, 20, 15, 20));
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(TEXT_MAIN);
        header.add(lblTitle, BorderLayout.WEST);
        
        if (rightText != null) {
            JLabel lblRight = new JLabel(rightText);
            lblRight.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lblRight.setForeground(PRIMARY_BLUE);
            lblRight.setCursor(new Cursor(Cursor.HAND_CURSOR));
            header.add(lblRight, BorderLayout.EAST);
        }
        
        p.add(header, BorderLayout.NORTH);
        p.add(content, BorderLayout.CENTER);
        return p;
    }

   private JScrollPane createTutorTable() {
        String[] columns = {"Gia sư", "Chuyên môn", "Số lớp phụ trách", "Đánh giá", "Thu nhập tháng", "Trạng thái"};
        
        overviewTutorTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        JTable table = new JTable(overviewTutorTableModel);
        styleTable(table);
        
        table.getColumnModel().getColumn(5).setCellRenderer(new BadgeRenderer());
        
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setText("⭐⭐⭐⭐⭐ (" + value + ")");
                label.setForeground(WARNING_ORANGE);
                return label;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_WHITE);
        return scroll;
    }

    private JScrollPane createClassTable() {
        String[] columns = {"Lớp học", "Môn học", "Gia sư", "Học viên", "Lịch học gần nhất", "Trạng thái"};
        Object[][] data = {
            {"Toán 9 - Cơ bản", "Toán", "Nguyễn Thu Hà", "5", "24/05/2025 - 19:00", "Đang học"},
            {"IELTS Inter", "Tiếng Anh", "Trần Minh Đức", "8", "25/05/2025 - 10:00", "Đang học"},
            {"Lý 11 - Nâng cao", "Vật lý", "Lê Phương Linh", "6", "24/05/2025 - 17:30", "Sắp diễn ra"},
            {"Hóa 10 - Cơ bản", "Hóa học", "Phạm Quang Huy", "7", "25/05/2025 - 15:00", "Đang học"},
            {"Lập trình Python", "Tin học", "Vũ Hoàng Nam", "4", "26/05/2025 - 18:30", "Sắp diễn ra"}
        };
        JTable table = new JTable(new DefaultTableModel(data, columns)) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        styleTable(table);
        
        table.getColumnModel().getColumn(5).setCellRenderer(new BadgeRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_WHITE);
        return scroll;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(45);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setShowVerticalLines(false);
        table.setGridColor(Color.decode("#F3F4F6"));
        table.setSelectionBackground(Color.decode("#EBF5FF"));
        
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(BG_WHITE);
        header.setForeground(TEXT_MUTED);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
    }

    private JPanel createRevenueChartPlaceholder() {
        JPanel chartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth(), h = getHeight();
                int[] heights = {60, 110, 120, 140, 130, 180, 200, 170, 180, 240, 250, 290}; 
                String[] labels = {"06/24", "07/24", "08/24", "09/24", "10/24", "11/24", "12/24", "01/25", "02/25", "03/25", "04/25", "05/25"};
                
                int barWidth = (w - 100) / 12 - 10;
                int startX = 60;
                
                g2.setColor(TEXT_MUTED);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.drawString("400M", 10, h - 300);
                g2.drawString("300M", 10, h - 225);
                g2.drawString("200M", 10, h - 150);
                g2.drawString("100M", 10, h - 75);
                g2.drawString("0", 30, h - 30);

                for (int i = 0; i < heights.length; i++) {
                    g2.setColor(PRIMARY_BLUE);
                    g2.fillRoundRect(startX, h - heights[i] - 30, barWidth, heights[i], 4, 4);
                    g2.setColor(TEXT_MUTED);
                    g2.drawString(labels[i], startX - 2, h - 10);
                    
                    startX += barWidth + 10;
                }
                
                g2.setColor(BORDER_COLOR);
                g2.drawLine(50, h - 30, w - 20, h - 30);
                g2.dispose();
            }
        };
        chartPanel.setBackground(BG_WHITE);
        return chartPanel;
    }

    private JPanel createTutorDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(0, 20, 0, 0),
                BorderFactory.createLineBorder(BORDER_COLOR, 1)
        ));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(20, 20, 10, 20));
        JLabel title = new JLabel("Thông tin gia sư");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(TEXT_MAIN);
        header.add(title, BorderLayout.NORTH);
        
        JPanel profile = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        profile.setOpaque(false);
        profile.setBorder(new EmptyBorder(15, 0, 15, 0));
        
        JLabel ava = new JLabel();
        setNetworkIcon(ava, "https://img.icons8.com/color/96/circled-user-female-skin-type-4--v1.png", 60, 60);
        
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        
        JPanel nameWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        nameWrap.setOpaque(false);
        JLabel name = new JLabel("Nguyễn Thu Hà");
        name.setFont(new Font("Segoe UI", Font.BOLD, 18));
        
        RoundedPanel badge = new RoundedPanel(12, Color.decode("#DCFCE7"));
        badge.setBorder(new EmptyBorder(2, 8, 2, 8));
        JLabel lblBadge = new JLabel("Đang dạy");
        lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblBadge.setForeground(Color.decode("#15803D"));
        badge.add(lblBadge);

        nameWrap.add(name);
        nameWrap.add(badge);
        
        JLabel subj = new JLabel("Toán học");
        subj.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subj.setForeground(TEXT_MUTED);
        JLabel rating = new JLabel("⭐⭐⭐⭐⭐ (4.9/5)");
        rating.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rating.setForeground(WARNING_ORANGE);
        
        info.add(nameWrap); info.add(Box.createVerticalStrut(3)); info.add(subj); info.add(Box.createVerticalStrut(3)); info.add(rating);
        profile.add(ava); profile.add(info);
        header.add(profile, BorderLayout.CENTER);

        panel.add(header, BorderLayout.NORTH);

        JPanel centerWrapper = new JPanel();
        centerWrapper.setLayout(new BoxLayout(centerWrapper, BoxLayout.Y_AXIS));
        centerWrapper.setOpaque(false);

        JPanel details = new JPanel(new GridLayout(7, 2, 10, 15));
        details.setOpaque(false);
        details.setBorder(new EmptyBorder(0, 20, 20, 20));
        addDetailRow(details, "Mã gia sư", "GS1001");
        addDetailRow(details, "Số lớp đang phụ trách", "18 lớp");
        addDetailRow(details, "Số học viên", "42 học viên");
        addDetailRow(details, "Thu nhập tháng", "12.800.000đ");
        addDetailRow(details, "Tỷ lệ lấp lớp", "100%");
        addDetailRow(details, "Ngày tham gia", "15/03/2023");
        addDetailRow(details, "Khu vực giảng dạy", "Hà Nội");
        
        centerWrapper.add(details);
        centerWrapper.add(new JSeparator());

        JPanel schedulePanel = new JPanel();
        schedulePanel.setLayout(new BoxLayout(schedulePanel, BoxLayout.Y_AXIS));
        schedulePanel.setOpaque(false);
        schedulePanel.setBorder(new EmptyBorder(15, 20, 20, 20));
        
        JLabel schTitle = new JLabel("Lịch dạy gần nhất");
        schTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        schTitle.setForeground(TEXT_MAIN);
        schedulePanel.add(schTitle);
        schedulePanel.add(Box.createVerticalStrut(15));

        schedulePanel.add(createScheduleItem("24/05/2025 - 19:00", "Toán 9 - Cơ bản", "Đã hoàn thành", Color.decode("#DCFCE7"), Color.decode("#15803D")));
        schedulePanel.add(Box.createVerticalStrut(10));
        schedulePanel.add(createScheduleItem("25/05/2025 - 10:00", "Toán 8 - Nâng cao", "Sắp diễn ra", Color.decode("#DBEAFE"), Color.decode("#1D4ED8")));
        schedulePanel.add(Box.createVerticalStrut(10));
        schedulePanel.add(createScheduleItem("25/05/2025 - 14:00", "Toán 10 - Ôn thi", "Sắp diễn ra", Color.decode("#DBEAFE"), Color.decode("#1D4ED8")));

        centerWrapper.add(schedulePanel);
        panel.add(centerWrapper, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0));
        actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JButton btnView = new JButton("Xem hồ sơ");
        btnView.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnView.setBackground(BG_WHITE);
        btnView.setForeground(TEXT_MAIN);
        btnView.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        btnView.setFocusPainted(false);
        
        JButton btnAssign = new JButton("Phân công lớp ▾");
        btnAssign.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAssign.setBackground(PRIMARY_BLUE);
        btnAssign.setForeground(BG_WHITE);
        btnAssign.setFocusPainted(false);
        
        actions.add(btnView);
        actions.add(btnAssign);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private void addDetailRow(JPanel p, String label, String value) {
        JLabel l = new JLabel(label);
        l.setForeground(TEXT_MUTED);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JLabel v = new JLabel(value);
        v.setForeground(TEXT_MAIN);
        v.setFont(new Font("Segoe UI", Font.PLAIN, 13)); 
        p.add(l); p.add(v);
    }

    private JPanel createScheduleItem(String time, String course, String status, Color badgeBg, Color badgeFg) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JLabel icon = new JLabel();
        setNetworkIcon(icon, "https://img.icons8.com/fluency-systems-regular/48/2563EB/calendar.png", 20, 20);
        
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        JLabel lblTime = new JLabel(time);
        lblTime.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTime.setForeground(TEXT_MAIN);
        JLabel lblCourse = new JLabel(course);
        lblCourse.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCourse.setForeground(TEXT_MUTED);
        info.add(lblTime); info.add(lblCourse);
        
        left.add(icon); left.add(info);
        p.add(left, BorderLayout.WEST);

        RoundedPanel badge = new RoundedPanel(12, badgeBg);
        badge.setBorder(new EmptyBorder(4, 10, 4, 10));
        JLabel lblBadge = new JLabel(status);
        lblBadge.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblBadge.setForeground(badgeFg);
        badge.add(lblBadge);
        
        JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 5));
        rightWrap.setOpaque(false);
        rightWrap.add(badge);
        p.add(rightWrap, BorderLayout.EAST);
        
        return p;
    }

    private JPanel createIconButton(String iconUrl, String tooltip) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(32, 32));
        p.setCursor(new Cursor(Cursor.HAND_CURSOR));
        p.setToolTipText(tooltip);
        JLabel lbl = new JLabel("", SwingConstants.CENTER);
        setNetworkIcon(lbl, iconUrl, 20, 20);
        p.add(lbl, BorderLayout.CENTER);
        return p;
    }

    private JPanel createTextIconButton(String iconUrl, String text, Color fgColor) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        p.setOpaque(false);
        p.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JLabel lblIcon = new JLabel();
        setNetworkIcon(lblIcon, iconUrl, 18, 18);
        JLabel lblText = new JLabel(text);
        lblText.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblText.setForeground(fgColor);
        p.add(lblIcon);
        p.add(lblText);
        return p;
    }

    private void setNetworkIcon(JLabel label, String urlStr, int width, int height) {
        new Thread(() -> {
            try {
                Image img = new ImageIcon(new URL(urlStr)).getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                SwingUtilities.invokeLater(() -> label.setIcon(new ImageIcon(img)));
            } catch (Exception e) {}
        }).start();
    }

    private JMenuItem createProfileMenuItem(String text, String iconName, Color textColor) {
        JMenuItem item = new JMenuItem("  " + text);
        item.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        item.setBackground(Color.WHITE);
        item.setForeground(textColor != null ? textColor : TEXT_MAIN);
        item.setPreferredSize(new Dimension(200, 40));
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        new Thread(() -> {
            try {
                String colorCode = (textColor == DANGER_RED) ? "EF4444" : "4B5563";
                String urlStr = "https://img.icons8.com/fluency-systems-regular/48/" + colorCode + "/" + iconName + ".png";
                ImageIcon raw = new ImageIcon(new URL(urlStr));
                Image img = raw.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
                SwingUtilities.invokeLater(() -> item.setIcon(new ImageIcon(img)));
            } catch (Exception ignored) {}
        }).start();
        
        return item;
    }
    
    private Image getScaledImage(String urlStr, int w, int h) {
        try {
            return new ImageIcon(new URL(urlStr)).getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        } catch (Exception e) { return null; }
    }

    class BadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10)); 
            p.setOpaque(true);
            p.setBackground(isSelected ? table.getSelectionBackground() : BG_WHITE);

            String status = value != null ? value.toString() : "";
            Color bg = Color.decode("#E5E7EB");
            Color fg = TEXT_MUTED;
            
            if(status.equals("Đang dạy") || status.equals("Đang học")) { 
                bg = Color.decode("#DCFCE7"); fg = Color.decode("#15803D"); 
            } else if(status.equals("Tạm nghỉ")) { 
                bg = Color.decode("#FEF3C7"); fg = Color.decode("#B45309"); 
            } else if(status.equals("Sắp diễn ra")) {
                bg = Color.decode("#DBEAFE"); fg = Color.decode("#1D4ED8");
            }

            JLabel lbl = new JLabel(status);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lbl.setForeground(fg);

            RoundedPanel badge = new RoundedPanel(12, bg);
            badge.setBorder(new EmptyBorder(3, 10, 3, 10));
            badge.add(lbl);

            p.add(badge);
            return p;
        }
    }

    class RoundedPanel extends JPanel { 
        private int radius; private Color bgColor; 
        public RoundedPanel(int radius, Color bgColor) { this.radius = radius; this.bgColor = bgColor; setOpaque(false); } 
        @Override protected void paintComponent(Graphics g) { 
            Graphics2D g2 = (Graphics2D) g.create(); 
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
            g2.setColor(bgColor); g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius); 
            g2.dispose(); 
        } 
    }

    private ImageIcon getCircularImageIcon(Image rawImage, int size) {
        if (rawImage == null) return null;
        java.awt.image.BufferedImage circleBuffer = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = circleBuffer.createGraphics();
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        g2.fillOval(0, 0, size, size); 
        g2.setComposite(AlphaComposite.SrcIn); 
        
        int imgW = rawImage.getWidth(null); int imgH = rawImage.getHeight(null);
        if (imgW > 0 && imgH > 0) {
            double scale = Math.max((double) size / imgW, (double) size / imgH);
            int drawW = (int) (imgW * scale); int drawH = (int) (imgH * scale);
            g2.drawImage(rawImage, (size - drawW) / 2, (size - drawH) / 2, drawW, drawH, null);
        }
        g2.dispose(); 
        return new ImageIcon(circleBuffer);
    }
    
    private void setAvatarNetworkIcon(JLabel label, String urlStr, int size) {
        new Thread(() -> { 
            try { 
                ImageIcon raw = new ImageIcon(new URL(urlStr)); 
                SwingUtilities.invokeLater(() -> {
                  if (!hasCustomAvatar) {
                        label.setIcon(getCircularImageIcon(raw.getImage(), size));
                    }
                }); 
            } catch (Exception ignored) {} 
        }).start();
    }

    @SuppressWarnings("unchecked")
    private void listenToServer() {
        try {
            NetworkManager net = NetworkManager.getInstance();
            net.sendPacket(new Packet("GET_CONVO_LIST", String.valueOf(this.adminId)));
            net.sendPacket(new Packet("GET_ALL_TUTORS", "")); 
        } catch (Exception e) {}

        new Thread(() -> {
            try {
                NetworkManager net = NetworkManager.getInstance();
                while (true) {
                    Packet res = net.receivePacket();
                    if (res == null) break; 

                    SwingUtilities.invokeLater(() -> {
                        switch (res.action) {
                            case "USER_ONLINE":
                            case "USER_OFFLINE":
                                System.out.println("Có sự thay đổi trạng thái user: " + res.payload);
                                break;
                                
                            case "SEARCH_USER_RESULT":
                                if (res.data != null && chatTab != null) {
                                    chatTab.updateSearchResults((List<UserInfo>) res.data);
                                }
                                break;

                            case "GET_CONVO_LIST":
                                if (chatTab != null && res.data != null) {
                                    chatTab.updateConversationList((List<ConversationInfo>) res.data);
                                }
                                break;

                            case "GET_MESSAGES":
                                if (chatTab != null && res.data != null) {
                                    chatTab.updateMessages((List<Message>) res.data);
                                }
                                break;
                                
                            case "ALL_TUTORS_RESULT":
                                java.util.List<String> tutors = (java.util.List<String>) res.data;
                                
                                if (tutorManagementTab != null) {
                                    tutorManagementTab.updateTutorList(tutors);
                                }
                                
                                if (overviewTutorTableModel != null) {
                                    overviewTutorTableModel.setRowCount(0); 
                                    for (String row : tutors) {
                                        String[] parts = row.split(";;");
                                        if (parts.length >= 6) {
                                            String name = parts[0].split("\\|")[1];
                                            String subject = parts[2];
                                            String classesTaught = parts[4].split("\\|")[1];
                                            String rating = parts[4].split("\\|")[2];
                                            String status = parts[5];
                                            
                                            overviewTutorTableModel.addRow(new Object[]{
                                                name, subject, classesTaught, rating, "Đang tính...", status
                                            });
                                        }
                                    }
                                }
                                break;
                                
                            case "RECEIVE_CHAT":
                                if (chatTab != null) {
                                    String[] data = res.payload.split("\\|", 3);
                                    if(data.length >= 3) chatTab.appendIncomingMessage(data[0], data[1], data[2]);
                                }
                                break;

                            case "MESSAGE_DELIVERED_ACK":
                                if (chatTab != null) {
                                    chatTab.handleDeliveredAck(res.payload);
                                }
                                break;

                            case "READ_ACK":
                                if (chatTab != null) {
                                    chatTab.markConversationAsRead(res.payload);
                                }
                                break;
                            case "LOAD_TUTOR_AVATAR":
                                if (adminTutorProfileTab != null && res.payload != null) {
                                    adminTutorProfileTab.updateAvatarFromBase64(res.payload);
                                }
                                break;
                            // =======================================================
                            // CÁC CASE HỨNG DỮ LIỆU ĐỔ VÀO TAB CHI TIẾT GIA SƯ
                            // =======================================================
                            case "FULL_PROFILE_RESULT":
                                if (res.payload != null && !res.payload.isEmpty()) {
                                    String[] profileDataArr = res.payload.split(";;", -1);
                                    if (adminTutorProfileTab != null) {
                                        adminTutorProfileTab.loadProfileData(profileDataArr);
                                    }
                                }
                                break;
                                
                            case "DEGREES_RESULT":
                                if (res.data != null && adminTutorProfileTab != null) {
                                    adminTutorProfileTab.loadDegreesList((java.util.List<String>) res.data);
                                }
                                break;
                                
                            case "CERTIFICATES_RESULT":
                                if (res.data != null && adminTutorProfileTab != null) {
                                    adminTutorProfileTab.loadCertificatesList((java.util.List<String>) res.data);
                                }
                                break;
                                
                            case "EXPERIENCES_RESULT":
                                if (res.data != null && adminTutorProfileTab != null) {
                                    adminTutorProfileTab.loadExperiencesList((java.util.List<String>) res.data);
                                }
                                break;
                            
                            // Tích hợp case Download File để xử lý tải và xem CV bên Admin
                            case "DOWNLOAD_FILE_RESPONSE":
                                String base64Data = res.payload;
                                if (base64Data != null && !base64Data.equals("ERROR")) {
                                    try {
                                        byte[] fileBytes = java.util.Base64.getDecoder().decode(base64Data);
                                        
                                        if (AdminTutorProfileTab.isPreviewingFile) {
                                            java.io.File tempFile = java.io.File.createTempFile("cv_preview_", ".pdf");
                                            tempFile.deleteOnExit();
                                            java.nio.file.Files.write(tempFile.toPath(), fileBytes);
                                            
                                            if (Desktop.isDesktopSupported()) {
                                                Desktop.getDesktop().open(tempFile);
                                            }
                                            AdminTutorProfileTab.isPreviewingFile = false;
                                        } 
                                        else {
                                            java.io.File saveTarget = AdminTutorProfileTab.pendingDownloadFile;
                                            if (saveTarget != null) {
                                                java.nio.file.Files.write(saveTarget.toPath(), fileBytes);
                                                JOptionPane.showMessageDialog(CenterDashboard.this, 
                                                    "Tải xuống thành công!\nĐã lưu tại: " + saveTarget.getAbsolutePath(), 
                                                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
                                                AdminTutorProfileTab.pendingDownloadFile = null; 
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        JOptionPane.showMessageDialog(CenterDashboard.this, "Lỗi khi xử lý file!", "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
                                    }
                                } else {
                                    AdminTutorProfileTab.isPreviewingFile = false;
                                    JOptionPane.showMessageDialog(CenterDashboard.this, "Lỗi: Không tìm thấy file trên server!", "Thất bại", JOptionPane.ERROR_MESSAGE);
                                }
                                break;
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("Mất kết nối với Server từ CenterDashboard.");
            }
        }).start();
    }
    
    // --- CẬP NHẬT HÀM MỞ TRANG CHI TIẾT TỪ DANH SÁCH GIA SƯ ---
   // --- CẬP NHẬT HÀM MỞ TRANG CHI TIẾT TỪ DANH SÁCH GIA SƯ ---
    public void showTutorProfileDetail(String rawTutorId) {
        try {
            int tId = Integer.parseInt(rawTutorId.replace("GS", ""));
            
            if (adminTutorProfileTab != null) {
                mainContentPanel.remove(adminTutorProfileTab.getParent());
            }
            
            adminTutorProfileTab = new AdminTutorProfileTab(tId);
            
            JButton btnBack = new JButton("← Quay lại danh sách");
            btnBack.setFont(new Font("Segoe UI", Font.BOLD, 14));
            btnBack.setForeground(Color.decode("#2563EB"));
            btnBack.setContentAreaFilled(false);
            btnBack.setBorderPainted(false);
            btnBack.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnBack.addActionListener(e -> {
                cardLayout.show(mainContentPanel, "TUTOR_MANAGEMENT"); 
            });

            JPanel wrapperPanel = new JPanel(new BorderLayout());
            wrapperPanel.setBackground(Color.decode("#F8FAFC"));
            
            JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
            topBar.setOpaque(false);
            topBar.add(btnBack);
            
            wrapperPanel.add(topBar, BorderLayout.NORTH);
            wrapperPanel.add(adminTutorProfileTab, BorderLayout.CENTER);

            mainContentPanel.add(wrapperPanel, "ADMIN_TUTOR_PROFILE_VIEW");
            cardLayout.show(mainContentPanel, "ADMIN_TUTOR_PROFILE_VIEW");
            
            // ĐÃ BỔ SUNG: Gửi lệnh yêu cầu Server trả về ảnh Avatar của Gia sư
            new Thread(() -> {
                try {
                    NetworkManager.getInstance().sendPacket(new Packet("GET_USER_AVATAR", String.valueOf(tId)));
                } catch (Exception ex) {
                    System.err.println("Không thể yêu cầu load avatar: " + ex.getMessage());
                }
            }).start();
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Không thể tải thông tin Gia sư này!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showTutorManagement() {
        cardLayout.show(mainContentPanel, "TUTOR_MANAGEMENT");
    }
    
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(new FlatLightLaf()); } catch (Exception ex) {}
        SwingUtilities.invokeLater(() -> {
         new CenterDashboard(1, "Nguyễn Văn An", "NO_AVATAR").setVisible(true);
        });
    }
}
