package com.mycompany.tutorhub_enterprise.client;

import com.mycompany.tutorhub_enterprise.models.Packet;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

public class AdminTutorProfileTab extends JPanel {

    private final Color BG_MAIN = Color.decode("#F4F7FA");
    private final Color TEXT_MAIN = Color.decode("#0F172A");
    private final Color TEXT_MUTED = Color.decode("#64748B");
    private final Color PRIMARY = Color.decode("#6366F1"); 
    private final Color CARD_BG = Color.WHITE;
    private final Color BORDER_COLOR = Color.decode("#E2E8F0");

    private int targetTutorId; // ID của gia sư đang được Admin xem

    private JLabel bigAvatarLabel, miniAvatarLabel;
    private JPanel timelineListPanel;
    private DefaultTableModel degTableModel, certTableModel; 
    
    private JTextField txtName, txtDob, txtPhone, txtEmail, txtAddress, txtSubject;
    private JComboBox<String> cbGender, cbLocation;
    private JTextArea txtBio;
    
    private String cvFileNameStr = "";
    private JLabel lblCvPreview;
    private JLabel lblCvNameDetail, lblCvSizeDetail, lblCvDateDetail, lblBadgeTextCV;
    
    private byte[] ekycFrontBytes = null, ekycBackBytes = null;
    private JLabel lblEkycFrontPreview, lblEkycBackPreview;

    private JLabel lblLeftName = new JLabel("");
    private JLabel lblLeftRole = new JLabel("");
    private JLabel lblLeftId = new JLabel("");
    private JLabel lblLeftLocation = new JLabel("");
    private JLabel lblJoinDate = new JLabel("-");
    private JLabel lblLeftStatus = new JLabel("Đang tải...");

    private CardLayout centerCardLayout;
    private JPanel centerCardPanel;
    private JPanel[] tabButtons;
    
    public static java.io.File pendingDownloadFile = null;
    public static boolean isPreviewingFile = false; 

    public AdminTutorProfileTab(int targetTutorId) {
        this.targetTutorId = targetTutorId;
        setLayout(new BorderLayout()); 
        setBackground(BG_MAIN);
        
        JPanel topArea = new JPanel(new BorderLayout()); 
        topArea.setOpaque(false);
        topArea.add(createHeader(), BorderLayout.NORTH); 
        topArea.add(createTabs(), BorderLayout.SOUTH);
        add(topArea, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(20, 0)); 
        contentPanel.setOpaque(false); 
        contentPanel.setBorder(new EmptyBorder(10, 20, 15, 20)); 
        contentPanel.add(createLeftColumn(), BorderLayout.WEST);
        
        centerCardLayout = new CardLayout(); 
        centerCardPanel = new JPanel(centerCardLayout); 
        centerCardPanel.setOpaque(false);
        setupCenterTabs(); 
        contentPanel.add(centerCardPanel, BorderLayout.CENTER);
        
        add(contentPanel, BorderLayout.CENTER);
        switchTab(0);

        // Gọi Server lấy dữ liệu của Gia sư này
        fetchTutorData();
    }

   private void fetchTutorData() {
        String idStr = String.valueOf(this.targetTutorId);
        new Thread(() -> {
            try {
                NetworkManager.getInstance().sendPacket(new Packet("GET_FULL_PROFILE", idStr));
                NetworkManager.getInstance().sendPacket(new Packet("GET_DEGREES", idStr));
                NetworkManager.getInstance().sendPacket(new Packet("GET_CERTIFICATES", idStr));
                NetworkManager.getInstance().sendPacket(new Packet("GET_EXPERIENCES", idStr));
                
                // ĐÃ BỔ SUNG: Yêu cầu Server gửi Avatar của gia sư này về
                NetworkManager.getInstance().sendPacket(new Packet("GET_USER_AVATAR", idStr));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout()); 
        header.setOpaque(false); 
        header.setBorder(new EmptyBorder(10, 20, 10, 20)); 
        
        JPanel leftTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftTitle.setOpaque(false);
        
        JPanel iconWrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); 
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); g2.fillOval(0, 0, getWidth(), getHeight()); 
                g2.setColor(BORDER_COLOR); g2.drawOval(0, 0, getWidth()-1, getHeight()-1); 
                g2.dispose();
            }
        };
        iconWrapper.setOpaque(false); iconWrapper.setPreferredSize(new Dimension(40, 40));
        JLabel lblIcon = new JLabel(); setNetworkIcon(lblIcon, "https://img.icons8.com/fluency-systems-regular/48/64748B/user.png", 20, 20);
        iconWrapper.add(lblIcon, BorderLayout.CENTER);

        JPanel titlePanel = new JPanel(); titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS)); titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Chi tiết Hồ sơ Gia sư"); lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18)); lblTitle.setForeground(TEXT_MAIN);
        JLabel lblBreadcrumb = new JLabel("Trung tâm  >  Quản lý Gia sư  >  Hồ sơ (ID: " + this.targetTutorId + ")"); lblBreadcrumb.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblBreadcrumb.setForeground(TEXT_MUTED);
        titlePanel.add(lblTitle); titlePanel.add(Box.createVerticalStrut(2)); titlePanel.add(lblBreadcrumb);
        
        leftTitle.add(iconWrapper); leftTitle.add(titlePanel);
        header.add(leftTitle, BorderLayout.WEST);
        
        // CÓ THỂ THÊM NÚT "DUYỆT HỒ SƠ" Ở ĐÂY NẾU MUỐN (Tạm thời để trống theo UI View-Only)
        return header;
    }

    private JPanel createTabs() {
        JPanel tabsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 0)); tabsPanel.setOpaque(false);
        tabsPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(0, 20, 0, 20), BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR)));
        String[] tabNames = {"Thông tin cá nhân", "Bằng cấp", "Chứng chỉ", "CV", "Chuyên môn", "Xác minh"};
        tabButtons = new JPanel[tabNames.length];
        for (int i = 0; i < tabNames.length; i++) { tabButtons[i] = createTabButton(tabNames[i], i); tabsPanel.add(tabButtons[i]); }
        return tabsPanel;
    }

    private JPanel createTabButton(String title, int index) {
        JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false); p.setCursor(new Cursor(Cursor.HAND_CURSOR)); 
        p.setBorder(new EmptyBorder(10, 0, 10, 0));
        JLabel lbl = new JLabel(title); lbl.setFont(new Font("Segoe UI", Font.BOLD, 13)); lbl.setForeground(TEXT_MUTED);
        JPanel line = new JPanel(); line.setPreferredSize(new Dimension(0, 3)); line.setBackground(new Color(0,0,0,0));
        p.add(lbl, BorderLayout.CENTER); p.add(line, BorderLayout.SOUTH);
        p.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { switchTab(index); } });
        return p;
    }

    private void switchTab(int index) {
        for (int i = 0; i < tabButtons.length; i++) {
            JLabel lbl = (JLabel) tabButtons[i].getComponent(0); JPanel line = (JPanel) tabButtons[i].getComponent(1);
            if (i == index) { lbl.setForeground(PRIMARY); line.setBackground(PRIMARY); } 
            else { lbl.setForeground(TEXT_MUTED); line.setBackground(new Color(0,0,0,0)); }
        }
        centerCardLayout.show(centerCardPanel, "TAB_" + index);
    }

    private void setupCenterTabs() {
        JPanel pnlPersonalTab = new JPanel(new BorderLayout(20, 0)); 
        pnlPersonalTab.setOpaque(false);
        pnlPersonalTab.add(createPersonalInfoForm(), BorderLayout.CENTER); 
        pnlPersonalTab.add(createRightColumn(), BorderLayout.EAST);   
        
        centerCardPanel.add(pnlPersonalTab, "TAB_0"); 
        centerCardPanel.add(createDegreesForm(), "TAB_1");
        centerCardPanel.add(createCertificatesForm(), "TAB_2");
        centerCardPanel.add(createCVForm(), "TAB_3");
        centerCardPanel.add(createExpertiseForm(), "TAB_4");
        centerCardPanel.add(createVerificationForm(), "TAB_5");
    }

    // --- CỘT TRÁI (PROFILE META) - READ ONLY ---
    private JPanel createLeftColumn() {
        RoundedPanel p = new RoundedPanel(12); 
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); 
        p.setPreferredSize(new Dimension(270, 0)); 
        p.setBorder(new EmptyBorder(25, 20, 20, 20)); 

        JPanel avaPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)); avaPanel.setOpaque(false);
        bigAvatarLabel = new JLabel(); setAvatarNetworkIcon(bigAvatarLabel, "https://img.icons8.com/color/150/circled-user-male-skin-type-4--v1.png", 100); 
        avaPanel.add(bigAvatarLabel);

        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0)); nameRow.setOpaque(false);
        lblLeftName.setFont(new Font("Segoe UI", Font.BOLD, 17)); lblLeftName.setForeground(TEXT_MAIN);
        JLabel lblCheck = new JLabel(); setNetworkIcon(lblCheck, "https://img.icons8.com/color/48/verified-badge.png", 18, 18);
        nameRow.add(lblLeftName); nameRow.add(lblCheck);

        JPanel roleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)); roleRow.setOpaque(false);
        lblLeftRole.setFont(new Font("Segoe UI", Font.PLAIN, 13)); lblLeftRole.setForeground(TEXT_MUTED); 
        roleRow.add(lblLeftRole);

        p.add(avaPanel); p.add(Box.createVerticalStrut(15)); p.add(nameRow); p.add(Box.createVerticalStrut(4)); p.add(roleRow); p.add(Box.createVerticalStrut(20)); 
        
        JPanel separator = new JPanel() {
            @Override protected void paintComponent(Graphics g) { g.setColor(Color.decode("#F1F5F9")); g.drawLine(0, getHeight()/2, getWidth(), getHeight()/2); }
        };
        separator.setOpaque(false); separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); p.add(separator); p.add(Box.createVerticalStrut(20));

        JPanel infoList = new JPanel(); infoList.setLayout(new BoxLayout(infoList, BoxLayout.Y_AXIS)); infoList.setOpaque(false); 
        
        infoList.add(createReadOnlyRow("https://img.icons8.com/fluency-systems-regular/48/64748B/identity-theft.png", "Mã gia sư", lblLeftId));
        infoList.add(Box.createVerticalStrut(18)); 
        infoList.add(createReadOnlyRow("https://img.icons8.com/fluency-systems-regular/48/64748B/calendar--v1.png", "Ngày tham gia", lblJoinDate));
        infoList.add(Box.createVerticalStrut(18));
        infoList.add(createReadOnlyRow("https://img.icons8.com/fluency-systems-regular/48/64748B/marker.png", "Khu vực", lblLeftLocation));
        infoList.add(Box.createVerticalStrut(18));
        infoList.add(createReadOnlyRow("https://img.icons8.com/fluency-systems-regular/48/64748B/checked-user-male.png", "Trạng thái", lblLeftStatus));
        
        p.add(infoList); p.add(Box.createVerticalGlue()); 
        return p;
    }

    private JPanel createReadOnlyRow(String iconUrl, String labelTxt, JLabel displayLbl) {
        JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false); p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30)); 
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); left.setOpaque(false);
        JLabel icon = new JLabel(); setNetworkIcon(icon, iconUrl, 16, 16);
        JLabel lbl = new JLabel(labelTxt); lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13)); lbl.setForeground(TEXT_MUTED);
        left.add(icon); left.add(lbl);
        
        displayLbl.setFont(new Font("Segoe UI", Font.BOLD, 13)); displayLbl.setHorizontalAlignment(SwingConstants.RIGHT); displayLbl.setForeground(TEXT_MAIN);
        p.add(left, BorderLayout.WEST); p.add(displayLbl, BorderLayout.EAST);
        return p;
    }

    // --- TAB 1: THÔNG TIN CÁ NHÂN (READ ONLY) ---
    private JPanel createPersonalInfoForm() {
        RoundedPanel p = new RoundedPanel(12); p.setLayout(new BorderLayout(0, 5)); p.setBorder(new EmptyBorder(20, 25, 20, 25)); 
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        JLabel title = new JLabel("Thông tin cá nhân"); title.setFont(new Font("Segoe UI", Font.BOLD, 18)); title.setForeground(TEXT_MAIN);
        header.add(title, BorderLayout.WEST); p.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(); body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS)); body.setOpaque(false); body.setBorder(new EmptyBorder(15, 0, 0, 0));
        JPanel grid = new JPanel(new GridLayout(4, 2, 20, 15)); grid.setOpaque(false); grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtName = createReadOnlyTextField(); txtDob = createReadOnlyTextField(); txtPhone = createReadOnlyTextField(); txtEmail = createReadOnlyTextField(); 
        cbGender = new JComboBox<>(new String[]{"Nam", "Nữ", "Khác"}); cbGender.setEnabled(false);
        txtAddress = createReadOnlyTextField(); 
        cbLocation = new JComboBox<>(new String[]{"Hà Nội", "Hồ Chí Minh", "Đà Nẵng", "Toàn quốc"}); cbLocation.setEnabled(false);
        txtSubject = createReadOnlyTextField();

        grid.add(createIconInputGroup("Họ và tên", null, txtName)); grid.add(createIconInputGroup("Ngày sinh", "https://img.icons8.com/fluency-systems-regular/48/64748B/calendar--v1.png", txtDob));
        grid.add(createIconInputGroup("Số điện thoại", "https://img.icons8.com/fluency-systems-regular/48/64748B/phone.png", txtPhone)); grid.add(createIconInputGroup("Email", "https://img.icons8.com/fluency-systems-regular/48/64748B/mail.png", txtEmail));
        grid.add(createIconInputGroup("Giới tính", "https://img.icons8.com/fluency-systems-regular/48/64748B/user.png", cbGender)); grid.add(createIconInputGroup("Địa chỉ", "https://img.icons8.com/fluency-systems-regular/48/64748B/marker.png", txtAddress));
        grid.add(createIconInputGroup("Khu vực dạy", "https://img.icons8.com/fluency-systems-regular/48/64748B/map.png", cbLocation)); grid.add(createIconInputGroup("Môn dạy chính", "https://img.icons8.com/fluency-systems-regular/48/64748B/monitor.png", txtSubject));
        
        body.add(grid); body.add(Box.createVerticalStrut(15)); 

        JLabel lblBio = new JLabel("Giới thiệu bản thân"); lblBio.setFont(new Font("Segoe UI", Font.BOLD, 13)); lblBio.setForeground(TEXT_MAIN); lblBio.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel bioWrap = new JPanel(new BorderLayout()); bioWrap.setOpaque(false); bioWrap.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR), new EmptyBorder(8, 12, 8, 12))); bioWrap.setAlignmentX(Component.LEFT_ALIGNMENT); bioWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65)); 
        
        txtBio = new JTextArea(""); txtBio.setRows(2); txtBio.setFont(new Font("Segoe UI", Font.PLAIN, 13)); txtBio.setLineWrap(true); txtBio.setWrapStyleWord(true); txtBio.setEditable(false); txtBio.setOpaque(false);
        JScrollPane bioScroll = new JScrollPane(txtBio, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); bioScroll.setBorder(null); bioScroll.setOpaque(false); bioScroll.getViewport().setOpaque(false);
        bioWrap.add(bioScroll, BorderLayout.CENTER);
        
        body.add(lblBio); body.add(Box.createVerticalStrut(6)); body.add(bioWrap); body.add(Box.createVerticalGlue()); 
        p.add(body, BorderLayout.CENTER); 
        return p;
    }
    
    private JTextField createReadOnlyTextField() {
        JTextField txt = new JTextField(); txt.setEditable(false); txt.setBorder(null); txt.setOpaque(false); txt.setFont(new Font("Segoe UI", Font.PLAIN, 13)); txt.setForeground(TEXT_MAIN); return txt;
    }

    private JPanel createIconInputGroup(String labelStr, String iconUrl, JComponent inputComp) {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setOpaque(false);
        JLabel lbl = new JLabel(labelStr); lbl.setFont(new Font("Segoe UI", Font.BOLD, 13)); lbl.setForeground(TEXT_MAIN); lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel inputWrap = new JPanel(new BorderLayout(10, 0)); inputWrap.setOpaque(false); inputWrap.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR), new EmptyBorder(0, 12, 0, 12))); inputWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); inputWrap.setPreferredSize(new Dimension(0, 40)); inputWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (iconUrl != null) { JLabel iconLbl = new JLabel(); setNetworkIcon(iconLbl, iconUrl, 16, 16); inputWrap.add(iconLbl, BorderLayout.WEST); }
        inputWrap.add(inputComp, BorderLayout.CENTER);
        p.add(lbl); p.add(Box.createVerticalStrut(6)); p.add(inputWrap); return p;
    }

    // --- CỘT PHẢI (CHỈ XEM ẢNH, KHÔNG NÚT ĐỔI) ---
    private JPanel createRightColumn() {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setOpaque(false); p.setPreferredSize(new Dimension(240, 0)); 
        RoundedPanel box1 = new RoundedPanel(12); box1.setLayout(new BorderLayout(10, 0)); box1.setBorder(new EmptyBorder(16, 20, 20, 20)); 
        JLabel title1 = new JLabel("Ảnh đại diện"); title1.setFont(new Font("Segoe UI", Font.BOLD, 15)); box1.add(title1, BorderLayout.NORTH);
        JPanel avaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15)); avaPanel.setOpaque(false);
        miniAvatarLabel = new JLabel(); setAvatarNetworkIcon(miniAvatarLabel, "https://img.icons8.com/color/80/circled-user-male-skin-type-4--v1.png", 64); 
        avaPanel.add(miniAvatarLabel); box1.add(avaPanel, BorderLayout.CENTER); 
        
        p.add(box1); p.add(Box.createVerticalGlue()); 
        return p;
    }

    // --- TAB 2: BẰNG CẤP (BỎ NÚT THÊM, BỎ CỘT THAO TÁC) ---
    private JPanel createDegreesForm() {
        RoundedPanel p = new RoundedPanel(12); p.setLayout(new BorderLayout(0, 10)); p.setBorder(new EmptyBorder(20, 20, 15, 20));
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false);
        JPanel titleWrap = new JPanel(); titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS)); titleWrap.setOpaque(false);
        JLabel title = new JLabel("Bằng cấp giáo dục"); title.setFont(new Font("Segoe UI", Font.BOLD, 16)); title.setForeground(TEXT_MAIN);
        JLabel lblCount = new JLabel("Danh sách bằng cấp đã cung cấp"); lblCount.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblCount.setForeground(TEXT_MUTED);
        titleWrap.add(title); titleWrap.add(lblCount); top.add(titleWrap, BorderLayout.WEST); p.add(top, BorderLayout.NORTH);

        String[] cols = {"Tên bằng cấp, Chuyên ngành", "Trường đào tạo", "Năm TN", "Xếp loại", "Tệp đính kèm", "Trạng thái"};
        degTableModel = new DefaultTableModel(null, cols) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        JTable table = new JTable(degTableModel); table.setRowHeight(55); table.setShowGrid(false); table.setIntercellSpacing(new Dimension(0, 0)); table.setSelectionBackground(Color.decode("#F8FAFC"));
        
        JTableHeader header = table.getTableHeader(); header.setPreferredSize(new Dimension(0, 40)); header.setFont(new Font("Segoe UI", Font.BOLD, 12)); header.setBackground(Color.decode("#EEF2FF")); header.setForeground(Color.decode("#312E81")); header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.decode("#C7D2FE"))); header.setReorderingAllowed(false);
        
        table.getColumnModel().getColumn(4).setCellRenderer(createLinkRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(createStatusRenderer());

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint()); int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 4) { downloadAttachedFile((String) degTableModel.getValueAt(row, 4)); }
            }
        });
        
        JScrollPane scroll = new JScrollPane(table); scroll.setBorder(null); scroll.getViewport().setBackground(Color.WHITE); p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // --- TAB 3: CHỨNG CHỈ (BỎ NÚT THÊM, BỎ CỘT THAO TÁC) ---
    private JPanel createCertificatesForm() {
        RoundedPanel p = new RoundedPanel(12); p.setLayout(new BorderLayout(0, 10)); p.setBorder(new EmptyBorder(20, 20, 15, 20));
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false);
        JPanel titleWrap = new JPanel(); titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS)); titleWrap.setOpaque(false);
        JLabel title = new JLabel("Chứng chỉ chuyên môn"); title.setFont(new Font("Segoe UI", Font.BOLD, 16)); title.setForeground(TEXT_MAIN);
        JLabel lblCount = new JLabel("Danh sách chứng chỉ đã cung cấp"); lblCount.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblCount.setForeground(TEXT_MUTED);
        titleWrap.add(title); titleWrap.add(lblCount); top.add(titleWrap, BorderLayout.WEST); p.add(top, BorderLayout.NORTH);

        String[] cols = {"Tên chứng chỉ", "Đơn vị cấp", "Ngày cấp", "Hạn SD", "Tệp đính kèm", "Trạng thái"};
        certTableModel = new DefaultTableModel(null, cols) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        JTable table = new JTable(certTableModel); table.setRowHeight(55); table.setShowGrid(false); table.setIntercellSpacing(new Dimension(0, 0)); table.setSelectionBackground(Color.decode("#F8FAFC")); table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        JTableHeader header = table.getTableHeader(); header.setPreferredSize(new Dimension(0, 40)); header.setFont(new Font("Segoe UI", Font.BOLD, 12)); header.setBackground(Color.decode("#EEF2FF")); header.setForeground(Color.decode("#312E81")); header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.decode("#C7D2FE"))); header.setReorderingAllowed(false);
        
        table.getColumnModel().getColumn(4).setCellRenderer(createLinkRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(createStatusRenderer());

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint()); int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 4) { downloadAttachedFile((String) certTableModel.getValueAt(row, 4)); }
            }
        });
        
        JScrollPane scroll = new JScrollPane(table); scroll.setBorder(null); scroll.getViewport().setBackground(Color.WHITE); p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // --- TAB 4: CV (BỎ KHUNG UPLOAD) ---
    private JPanel createCVForm() {
        RoundedPanel p = new RoundedPanel(12); p.setLayout(new BorderLayout(15, 10)); p.setBorder(new EmptyBorder(10, 15, 10, 15)); 
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        JPanel titleWrap = new JPanel(); titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS)); titleWrap.setOpaque(false);
        JLabel lblTitle = new JLabel("Hồ sơ năng lực (CV)"); lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15)); lblTitle.setForeground(TEXT_MAIN);
        JLabel lblSub = new JLabel("CV được cung cấp bởi gia sư"); lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11)); lblSub.setForeground(TEXT_MUTED);
        titleWrap.add(lblTitle); titleWrap.add(lblSub); header.add(titleWrap, BorderLayout.WEST); p.add(header, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 15, 0)); centerPanel.setOpaque(false);
        JPanel leftCol = new JPanel(); leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS)); leftCol.setOpaque(false);

        JPanel infoPanel = new JPanel(); infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS)); infoPanel.setOpaque(false); infoPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        JLabel lblInfoTitle = new JLabel("Thông tin tệp"); lblInfoTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        JPanel fileRow = new JPanel(new BorderLayout()); fileRow.setOpaque(false); fileRow.setBorder(new EmptyBorder(5, 0, 5, 0));
        JPanel fileLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); fileLeft.setOpaque(false);
        JLabel fileIcon = new JLabel(); setNetworkIcon(fileIcon, "https://img.icons8.com/color/48/pdf.png", 28, 28);
        JPanel fileDetail = new JPanel(); fileDetail.setLayout(new BoxLayout(fileDetail, BoxLayout.Y_AXIS)); fileDetail.setOpaque(false);
        lblCvNameDetail = new JLabel("Chưa có tệp nào"); lblCvNameDetail.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCvDateDetail = new JLabel("-"); lblCvDateDetail.setFont(new Font("Segoe UI", Font.PLAIN, 10)); lblCvDateDetail.setForeground(TEXT_MUTED);
        fileDetail.add(lblCvNameDetail); fileDetail.add(Box.createVerticalStrut(2)); fileDetail.add(lblCvDateDetail);
        fileLeft.add(fileIcon); fileLeft.add(fileDetail);
        
        JPanel fileRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); fileRight.setOpaque(false);
        lblCvSizeDetail = new JLabel(""); lblCvSizeDetail.setFont(new Font("Segoe UI", Font.PLAIN, 11)); lblCvSizeDetail.setForeground(TEXT_MUTED);
        fileRight.add(lblCvSizeDetail); fileRow.add(fileLeft, BorderLayout.WEST); fileRow.add(fileRight, BorderLayout.EAST);

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); statusRow.setOpaque(false); statusRow.setBorder(new EmptyBorder(0, 0, 10, 0));
        JLabel lblStatus = new JLabel("Trạng thái"); lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblStatus.setForeground(TEXT_MUTED);
        ColorRoundedPanel badge = new ColorRoundedPanel(8, Color.decode("#DCFCE7")); badge.setBorder(new EmptyBorder(3, 8, 3, 8));
        lblBadgeTextCV = new JLabel("Đã cập nhật"); lblBadgeTextCV.setFont(new Font("Segoe UI", Font.BOLD, 11)); lblBadgeTextCV.setForeground(Color.decode("#15803D"));
        badge.add(lblBadgeTextCV); statusRow.add(lblStatus); statusRow.add(badge);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); actionRow.setOpaque(false);
        JButton btnViewCV = createButton("Xem CV", "https://img.icons8.com/fluency-systems-regular/48/2563EB/visible.png");
        JButton btnDownload = createButton("Tải xuống", "https://img.icons8.com/fluency-systems-regular/48/2563EB/download.png");
        actionRow.add(btnViewCV); actionRow.add(btnDownload);

        infoPanel.add(lblInfoTitle); infoPanel.add(fileRow); infoPanel.add(statusRow); infoPanel.add(actionRow);
        leftCol.add(infoPanel); leftCol.add(Box.createVerticalGlue()); 

        JPanel rightCol = new JPanel(new BorderLayout()); rightCol.setOpaque(false);
        JLabel lblPreviewTitle = new JLabel("Xem trước CV"); lblPreviewTitle.setFont(new Font("Segoe UI", Font.BOLD, 13)); lblPreviewTitle.setBorder(new EmptyBorder(0, 0, 5, 0));
        RoundedPanel previewCard = new RoundedPanel(12); previewCard.setLayout(new BorderLayout());
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5)); toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR)); toolbar.setBackground(Color.decode("#FAFAFA")); toolbar.add(new JLabel("1 / 1")); toolbar.add(new JLabel("100%"));
        JPanel previewBody = new JPanel(new GridBagLayout()); previewBody.setBackground(Color.decode("#F1F5F9")); previewBody.setBorder(new EmptyBorder(10, 10, 10, 10));
        lblCvPreview = new JLabel("<html><div style='text-align: center;'><img src='https://img.icons8.com/color/48/pdf.png'><br><br>Đang tải...</div></html>", SwingConstants.CENTER); lblCvPreview.setOpaque(true); lblCvPreview.setBackground(Color.WHITE); lblCvPreview.setBorder(BorderFactory.createLineBorder(Color.decode("#E2E8F0"))); lblCvPreview.setPreferredSize(new Dimension(240, 300)); 
        previewBody.add(lblCvPreview); previewCard.add(toolbar, BorderLayout.NORTH); previewCard.add(previewBody, BorderLayout.CENTER);
        rightCol.add(lblPreviewTitle, BorderLayout.NORTH); rightCol.add(previewCard, BorderLayout.CENTER);

        centerPanel.add(leftCol); centerPanel.add(rightCol); p.add(centerPanel, BorderLayout.CENTER);

        btnDownload.addActionListener(e -> { if (!cvFileNameStr.isEmpty()) downloadAttachedFile(cvFileNameStr); });
        btnViewCV.addActionListener(e -> { if (!cvFileNameStr.isEmpty()) { isPreviewingFile = true; try { NetworkManager.getInstance().sendPacket(new Packet("DOWNLOAD_FILE", cvFileNameStr)); } catch (Exception ex) {} } });

        return p;
    }

    // --- TAB 5: KINH NGHIỆM (BỎ NÚT THÊM, BỎ NÚT ACTION TRONG TỪNG CARD) ---
    private JPanel createExpertiseForm() {
        JPanel p = new JPanel(new BorderLayout(15, 0)); p.setOpaque(false); p.setBorder(new EmptyBorder(5, 0, 5, 0));
        RoundedPanel centerPanel = new RoundedPanel(12); centerPanel.setLayout(new BorderLayout()); centerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        JPanel titleWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0)); titleWrap.setOpaque(false);
        ColorRoundedPanel iconWrap = new ColorRoundedPanel(10, Color.decode("#EFF6FF")); iconWrap.setBorder(new EmptyBorder(6, 6, 6, 6));
        JLabel iconLbl = new JLabel(); setNetworkIcon(iconLbl, "https://img.icons8.com/briefcase.png", 20, 20); iconWrap.add(iconLbl);
        JPanel textWrap = new JPanel(); textWrap.setLayout(new BoxLayout(textWrap, BoxLayout.Y_AXIS)); textWrap.setOpaque(false);
        JLabel lblTitle = new JLabel("Kinh nghiệm giảng dạy"); lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16)); lblTitle.setForeground(TEXT_MAIN);
        JLabel lblSub = new JLabel("Quá trình công tác của gia sư"); lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblSub.setForeground(TEXT_MUTED);
        textWrap.add(lblTitle); textWrap.add(lblSub); titleWrap.add(iconWrap); titleWrap.add(textWrap); header.add(titleWrap, BorderLayout.WEST); centerPanel.add(header, BorderLayout.NORTH);
        
        timelineListPanel = new JPanel(); timelineListPanel.setLayout(new BoxLayout(timelineListPanel, BoxLayout.Y_AXIS)); timelineListPanel.setOpaque(false); timelineListPanel.setBorder(new EmptyBorder(25, 0, 0, 0)); 
        
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER)); footer.setOpaque(false); footer.setBorder(new EmptyBorder(15, 0, 0, 0));
        JButton btnMore = new JButton("Hiển thị thêm ▾"); btnMore.setFont(new Font("Segoe UI", Font.BOLD, 12)); btnMore.setForeground(PRIMARY); btnMore.setContentAreaFilled(false); btnMore.setBorderPainted(false); btnMore.setCursor(new Cursor(Cursor.HAND_CURSOR)); footer.add(btnMore);
        
        JPanel scrollContent = new JPanel(new BorderLayout()); scrollContent.setOpaque(false); scrollContent.add(timelineListPanel, BorderLayout.CENTER); scrollContent.add(footer, BorderLayout.SOUTH);
        JScrollPane scroll = new JScrollPane(scrollContent); scroll.setBorder(null); scroll.setOpaque(false); scroll.getViewport().setOpaque(false); scroll.getVerticalScrollBar().setUnitIncrement(16);
        
        centerPanel.add(scroll, BorderLayout.CENTER); p.add(centerPanel, BorderLayout.CENTER);
        return p;
    }

    private JPanel createTimelineCard(String time, String title, String status, String type, String location, String desc, String[] tags) {
        JPanel row = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g); Graphics2D g2 = (Graphics2D) g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode("#E2E8F0")); g2.fillRect(70, 15, 2, getHeight()); g2.setColor(PRIMARY); g2.fillOval(66, 11, 10, 10); 
            }
        };
        row.setOpaque(false);
        String timeHtml = "<html><div style='text-align:right; color:#2563EB; font-weight:bold; font-size:11px; width: 50px; line-height:1.3;'>" + time.replace(" - ", " -<br>") + "</div></html>";
        JLabel lblTime = new JLabel(timeHtml); lblTime.setVerticalAlignment(SwingConstants.TOP); lblTime.setBorder(new EmptyBorder(8, 0, 0, 0)); row.add(lblTime, BorderLayout.WEST);
        
        RoundedPanel card = new RoundedPanel(12); card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS)); card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR), new EmptyBorder(15, 20, 15, 20) ));
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false);
        JPanel titleWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0)); titleWrap.setOpaque(false);
        JLabel lblTitle = new JLabel(title); lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15)); lblTitle.setForeground(TEXT_MAIN);
        ColorRoundedPanel badge = new ColorRoundedPanel(8, status.equals("Đã duyệt") ? Color.decode("#DCFCE7") : Color.decode("#FEF3C7")); badge.setBorder(new EmptyBorder(2, 8, 2, 8));
        JLabel lblBadge = new JLabel(status); lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 10)); lblBadge.setForeground(status.equals("Đã duyệt") ? Color.decode("#15803D") : Color.decode("#B45309")); badge.add(lblBadge);
        titleWrap.add(lblTitle); titleWrap.add(badge); top.add(titleWrap, BorderLayout.WEST);
        
        JPanel subInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0)); subInfo.setOpaque(false); subInfo.setBorder(new EmptyBorder(8, 0, 8, 0));
        JLabel i1 = new JLabel(); setNetworkIcon(i1, "https://img.icons8.com/fluency-systems-regular/48/64748B/user.png", 14, 14); subInfo.add(i1); JLabel t1 = new JLabel(type); t1.setForeground(TEXT_MUTED); subInfo.add(t1);
        JLabel i2 = new JLabel(); setNetworkIcon(i2, "https://img.icons8.com/marker.png", 14, 14); subInfo.add(i2); JLabel t2 = new JLabel(location); t2.setForeground(TEXT_MUTED); subInfo.add(t2);
        
        JTextArea txtDesc = new JTextArea(desc); txtDesc.setWrapStyleWord(true); txtDesc.setLineWrap(true); txtDesc.setEditable(false); txtDesc.setOpaque(false); txtDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13)); txtDesc.setForeground(TEXT_MAIN); txtDesc.setBorder(new EmptyBorder(0, 5, 10, 5));
        
        JPanel tagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); tagsPanel.setOpaque(false);
        for (String tag : tags) { ColorRoundedPanel pill = new ColorRoundedPanel(8, Color.decode("#F1F5F9")); pill.setBorder(new EmptyBorder(3, 10, 3, 10)); JLabel l = new JLabel(tag); l.setFont(new Font("Segoe UI", Font.PLAIN, 11)); l.setForeground(PRIMARY); pill.add(l); tagsPanel.add(pill); }
        
        card.add(top); card.add(subInfo); card.add(txtDesc); card.add(tagsPanel);
        JPanel cardWrap = new JPanel(new BorderLayout()); cardWrap.setOpaque(false); cardWrap.setBorder(new EmptyBorder(0, 20, 0, 0)); cardWrap.add(card, BorderLayout.CENTER);
        row.add(cardWrap, BorderLayout.CENTER); return row;
    }

    // --- TAB 6: EKYC (READ ONLY, NO UPLOAD) ---
    private JPanel createVerificationForm() {
        RoundedPanel p = new RoundedPanel(12); p.setLayout(new BorderLayout(0, 20)); p.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        JPanel titleWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0)); titleWrap.setOpaque(false);
        ColorRoundedPanel iconWrap = new ColorRoundedPanel(12, Color.decode("#EFF6FF")); iconWrap.setBorder(new EmptyBorder(8, 8, 8, 8));
        JLabel iconLbl = new JLabel(); setNetworkIcon(iconLbl, "https://img.icons8.com/fluency-systems-regular/48/2563EB/security-checked.png", 24, 24); iconWrap.add(iconLbl);
        JPanel textWrap = new JPanel(); textWrap.setLayout(new BoxLayout(textWrap, BoxLayout.Y_AXIS)); textWrap.setOpaque(false);
        JLabel title = new JLabel("Xác minh danh tính (eKYC)"); title.setFont(new Font("Segoe UI", Font.BOLD, 16)); title.setForeground(TEXT_MAIN);
        JLabel subtitle = new JLabel("Tài liệu xác minh của gia sư"); subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12)); subtitle.setForeground(TEXT_MUTED);
        textWrap.add(title); textWrap.add(Box.createVerticalStrut(3)); textWrap.add(subtitle); titleWrap.add(iconWrap); titleWrap.add(textWrap);
        header.add(titleWrap, BorderLayout.WEST); p.add(header, BorderLayout.NORTH);
        
        JPanel uploadGrid = new JPanel(new GridLayout(1, 2, 20, 0)); uploadGrid.setOpaque(false);
        uploadGrid.add(createDocViewBox("CMND/CCCD Mặt trước", true));
        uploadGrid.add(createDocViewBox("CMND/CCCD Mặt sau", false));
        p.add(uploadGrid, BorderLayout.CENTER);
        
        return p;
    }

    private JPanel createDocViewBox(String titleStr, boolean isFront) {
        RoundedPanel outerBox = new RoundedPanel(12); outerBox.setLayout(new BorderLayout(0, 15)); outerBox.setBorder(new EmptyBorder(20, 20, 20, 20));
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        JLabel title = new JLabel(titleStr); title.setFont(new Font("Segoe UI", Font.BOLD, 14)); title.setForeground(TEXT_MAIN);
        ColorRoundedPanel badge = new ColorRoundedPanel(6, Color.decode("#DCFCE7")); badge.setBorder(new EmptyBorder(3, 10, 3, 10));
        JLabel lblBadge = new JLabel("Đã cập nhật"); lblBadge.setFont(new Font("Segoe UI", Font.PLAIN, 11)); lblBadge.setForeground(Color.decode("#15803D")); badge.add(lblBadge);
        header.add(title, BorderLayout.WEST); header.add(badge, BorderLayout.EAST);

        JPanel contentArea = new JPanel(new BorderLayout(0, 15)); contentArea.setBackground(Color.WHITE); contentArea.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.decode("#E2E8F0")), new EmptyBorder(15, 15, 15, 15)));
        JLabel lblPreview = new JLabel("Đang tải ảnh...", SwingConstants.CENTER); 
        
        JPanel bottomArea = new JPanel(); bottomArea.setLayout(new BoxLayout(bottomArea, BoxLayout.Y_AXIS)); bottomArea.setOpaque(false);
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0)); actionRow.setOpaque(false);
        
        JButton btnView = createButton("Xem ảnh lớn", "https://img.icons8.com/fluency-systems-regular/48/2563EB/search.png");
        JButton btnDownload = createButton("Tải xuống", "https://img.icons8.com/fluency-systems-regular/48/2563EB/download.png");
        actionRow.add(btnView); actionRow.add(btnDownload); bottomArea.add(actionRow);

        contentArea.add(lblPreview, BorderLayout.CENTER); contentArea.add(bottomArea, BorderLayout.SOUTH); 
        if (isFront) { lblEkycFrontPreview = lblPreview; } else { lblEkycBackPreview = lblPreview; }

        outerBox.add(header, BorderLayout.NORTH); outerBox.add(contentArea, BorderLayout.CENTER);

        btnView.addActionListener(e -> {
            byte[] data = isFront ? ekycFrontBytes : ekycBackBytes;
            if (data == null) { JOptionPane.showMessageDialog(this, "Chưa có ảnh để xem!"); return; }
            showImageDialog(data, titleStr);
        });

        btnDownload.addActionListener(e -> {
            byte[] data = isFront ? ekycFrontBytes : ekycBackBytes;
            if (data == null) { JOptionPane.showMessageDialog(this, "Chưa có ảnh để tải xuống!"); return; }
            JFileChooser fileChooser = new JFileChooser(); fileChooser.setSelectedFile(new File(isFront ? "mat_truoc.jpg" : "mat_sau.jpg"));
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try { Files.write(fileChooser.getSelectedFile().toPath(), data); JOptionPane.showMessageDialog(this, "Tải xuống thành công!"); } 
                catch (Exception ex) { JOptionPane.showMessageDialog(this, "Lỗi khi lưu file!"); }
            }
        });

        return outerBox;
    }

    // --- HELPER RENDERS & METHODS ---
    private DefaultTableCellRenderer createLinkRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 15)); cell.setOpaque(true); cell.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                if (value != null && !value.toString().isEmpty() && !value.toString().equals("NO_FILE")) {
                    String fn = new java.io.File(value.toString()).getName(); if (fn.length() > 18) fn = fn.substring(0, 15) + "...";
                    JLabel lblFile = new JLabel("<html><u>" + fn + "</u></html>"); lblFile.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblFile.setForeground(PRIMARY); lblFile.setCursor(new Cursor(Cursor.HAND_CURSOR)); cell.add(lblFile);
                } else { JLabel l = new JLabel("-"); l.setForeground(TEXT_MUTED); cell.add(l); }
                return cell;
            }
        };
    }

    private DefaultTableCellRenderer createStatusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                String status = (value != null) ? value.toString() : ""; JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 15)); cell.setOpaque(true); cell.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                JLabel badge = new JLabel(status); badge.setFont(new Font("Segoe UI", Font.BOLD, 11)); badge.setBorder(new EmptyBorder(4, 10, 4, 10)); badge.setOpaque(true);
                if (status.equals("Đã xác minh") || status.equals("Đã duyệt")) { badge.setBackground(Color.decode("#DCFCE7")); badge.setForeground(Color.decode("#15803D")); } else { badge.setBackground(Color.decode("#FEF3C7")); badge.setForeground(Color.decode("#B45309")); }
                cell.add(badge); return cell;
            }
        };
    }

    private JButton createButton(String text, String iconUrl) {
        JButton btn = new JButton(text); btn.setFont(new Font("Segoe UI", Font.BOLD, 12)); btn.setForeground(PRIMARY); btn.setBackground(Color.WHITE); btn.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PRIMARY), new EmptyBorder(6,12,6,12))); btn.setFocusPainted(false); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (iconUrl != null) setNetworkIcon(btn, iconUrl, 14, 14); return btn;
    }

    // --- NETWORK LOADERS ---
    public void loadProfileData(String[] data) {
        SwingUtilities.invokeLater(() -> {
            try {
                if(data.length > 0 && data[0] != null) lblLeftId.setText(data[0]);
                if(data.length > 1 && data[1] != null) txtEmail.setText(data[1]);
                if(data.length > 2 && data[2] != null) { txtName.setText(data[2]); lblLeftName.setText(data[2]); }
                if(data.length > 3 && data[3] != null) txtDob.setText(data[3]);
                if(data.length > 4 && data[4] != null) cbGender.setSelectedItem(data[4]);
                if(data.length > 5 && data[5] != null) txtPhone.setText(data[5]);
                if(data.length > 6 && data[6] != null) txtAddress.setText(data[6]);
                if(data.length > 7 && data[7] != null) { cbLocation.setSelectedItem(data[7]); lblLeftLocation.setText(data[7]); }
                if(data.length > 8 && data[8] != null) { txtSubject.setText(data[8]); lblLeftRole.setText("Gia sư " + data[8]); }
                if(data.length > 9 && data[9] != null) txtBio.setText(data[9].replace("\\n", "\n"));
                
                if(data.length > 10 && data[10] != null && !data[10].isEmpty() && !data[10].equals("null")) {
                    File f = new File(data[10]); cvFileNameStr = f.getName(); 
                    lblCvNameDetail.setText(cvFileNameStr);
                    lblCvPreview.setText("<html><div style='text-align: center;'><img src='https://img.icons8.com/color/48/pdf.png'><br><br><b style='color:#2563EB;'>Đã tải lên: " + cvFileNameStr + "</b></div></html>");
                }
                
                if(data.length > 11 && data[11] != null && !data[11].trim().isEmpty() && !data[11].equals("null")) {
                    try { ekycFrontBytes = Base64.getDecoder().decode(data[11]); Image img = new ImageIcon(ekycFrontBytes).getImage(); double scale = Math.min(280.0 / img.getWidth(null), 160.0 / img.getHeight(null)); Image scaledImg = img.getScaledInstance((int)(img.getWidth(null) * scale), (int)(img.getHeight(null) * scale), Image.SCALE_SMOOTH); if (lblEkycFrontPreview != null) { lblEkycFrontPreview.setText(""); lblEkycFrontPreview.setIcon(new ImageIcon(scaledImg)); } } catch (Exception ex) {}
                } else { if(lblEkycFrontPreview != null) lblEkycFrontPreview.setText("Chưa cung cấp ảnh mặt trước");}
                
                if(data.length > 12 && data[12] != null && !data[12].trim().isEmpty() && !data[12].equals("null")) {
                    try { ekycBackBytes = Base64.getDecoder().decode(data[12]); Image img = new ImageIcon(ekycBackBytes).getImage(); double scale = Math.min(280.0 / img.getWidth(null), 160.0 / img.getHeight(null)); Image scaledImg = img.getScaledInstance((int)(img.getWidth(null) * scale), (int)(img.getHeight(null) * scale), Image.SCALE_SMOOTH); if (lblEkycBackPreview != null) { lblEkycBackPreview.setText(""); lblEkycBackPreview.setIcon(new ImageIcon(scaledImg)); } } catch (Exception ex) {}
                } else { if(lblEkycBackPreview != null) lblEkycBackPreview.setText("Chưa cung cấp ảnh mặt sau");}
                
                lblLeftStatus.setText("Sẵn sàng"); lblLeftStatus.setForeground(Color.decode("#10B981")); setNetworkIcon(lblLeftStatus, "https://img.icons8.com/color/48/ok--v1.png", 14, 14);
            } catch (Exception e) { e.printStackTrace(); }
        });
    }            

    public void loadDegreesList(java.util.List<String> data) {
        SwingUtilities.invokeLater(() -> {
            if (degTableModel == null) return; degTableModel.setRowCount(0);
            for (String row : data) {
                String[] parts = row.split("\\|");
                if (parts.length >= 6) {
                    String firstCol = "<html><div style='margin-left:5px;'><b style='color:#1E293B;'>" + parts[0] + "</b><br><span style='color:#64748B; font-size:10px;'>" + parts[1] + "</span></div></html>";
                    degTableModel.addRow(new Object[]{firstCol, parts[2], parts[3], "Khá", parts[4], parts[5]});
                }
            }
        });
    }

    public void loadCertificatesList(java.util.List<String> data) {
        SwingUtilities.invokeLater(() -> {
            if (certTableModel == null) return; certTableModel.setRowCount(0);
            for (String row : data) {
                String[] parts = row.split("\\|");
                if (parts.length >= 6) { certTableModel.addRow(new Object[]{parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]}); }
            }
        });
    }
    
    public void loadExperiencesList(java.util.List<String> data) { 
        SwingUtilities.invokeLater(() -> { 
            if (timelineListPanel == null) return; timelineListPanel.removeAll(); 
            if (data == null || data.isEmpty()) { JLabel lblEmpty = new JLabel("Chưa có kinh nghiệm giảng dạy nào được cập nhật."); lblEmpty.setFont(new Font("Segoe UI", Font.ITALIC, 13)); lblEmpty.setForeground(TEXT_MUTED); timelineListPanel.add(lblEmpty); } 
            else {
                for (String row : data) { 
                    String[] parts = row.split("\\|"); 
                    String time = parts.length > 0 ? parts[0] : ""; String title = parts.length > 1 ? parts[1] : ""; String status = parts.length > 2 ? parts[2] : "Chờ duyệt"; String type = parts.length > 3 ? parts[3] : "Cá nhân"; String loc = parts.length > 4 ? parts[4] : "Không rõ"; String desc = parts.length > 5 ? parts[5].replace("\\n", "\n") : ""; String tagsStr = parts.length > 6 ? parts[6] : ""; String[] tags = tagsStr.trim().isEmpty() ? new String[0] : tagsStr.split(",");
                    timelineListPanel.add(createTimelineCard(time, title, status, type, loc, desc, tags)); timelineListPanel.add(Box.createVerticalStrut(15));
                } 
            }
            timelineListPanel.revalidate(); timelineListPanel.repaint();
        }); 
    }

    private void showImageDialog(byte[] imageData, String title) {
        try {
            Image img = new ImageIcon(imageData).getImage(); JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
            int imgW = img.getWidth(null); int imgH = img.getHeight(null);
            if (imgW > 800 || imgH > 600) { double scale = Math.min(800.0 / imgW, 600.0 / imgH); img = img.getScaledInstance((int)(imgW * scale), (int)(imgH * scale), Image.SCALE_SMOOTH); }
            JLabel lblImage = new JLabel(new ImageIcon(img)); lblImage.setHorizontalAlignment(SwingConstants.CENTER); lblImage.setBorder(new EmptyBorder(10, 10, 10, 10));
            JScrollPane scroll = new JScrollPane(lblImage); scroll.setBorder(null); scroll.getViewport().setBackground(Color.decode("#F8FAFC"));
            dialog.getContentPane().add(scroll, BorderLayout.CENTER); dialog.pack(); dialog.setLocationRelativeTo(this); dialog.setVisible(true);
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Không thể hiển thị ảnh!"); }
    }

    private void downloadAttachedFile(String rawFileName) {
        if (rawFileName == null || rawFileName.isEmpty() || rawFileName.equals("-") || rawFileName.equals("NO_FILE")) return;
        String cleanFileName = rawFileName.replaceAll("<[^>]*>", "");
        JFileChooser fileChooser = new JFileChooser(); fileChooser.setSelectedFile(new File(cleanFileName));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            pendingDownloadFile = fileChooser.getSelectedFile();
            new Thread(() -> { try { NetworkManager.getInstance().sendPacket(new Packet("DOWNLOAD_FILE", cleanFileName)); } catch (Exception ex) {} }).start();
        }
    }

    // --- UTILS (Giữ nguyên cấu trúc) ---
    class RoundedPanel extends JPanel {
        private int radius; public RoundedPanel(int radius) { this.radius = radius; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(new Color(0, 0, 0, 8)); g2.fillRoundRect(1, 2, getWidth() - 2, getHeight() - 2, radius, radius); g2.setColor(CARD_BG); g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 3, radius, radius); g2.setColor(BORDER_COLOR); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0, 0, getWidth() - 3, getHeight() - 4, radius, radius); g2.dispose(); super.paintComponent(g); }
    }
    class ColorRoundedPanel extends JPanel {
        private int radius; public Color bg; public ColorRoundedPanel(int radius, Color bg) { this.radius = radius; this.bg = bg; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(bg); g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius); g2.dispose(); super.paintComponent(g); }
    }
    private void setNetworkIcon(JComponent label, String urlStr, int width, int height) {
        new Thread(() -> { try { ImageIcon raw = new ImageIcon(new URL(urlStr)); Image img = raw.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH); SwingUtilities.invokeLater(() -> { if (label instanceof JLabel) ((JLabel)label).setIcon(new ImageIcon(img)); else if (label instanceof JButton) ((JButton)label).setIcon(new ImageIcon(img)); }); } catch (Exception ignored) {} }).start();
    }
    private void setAvatarNetworkIcon(JLabel label, String urlStr, int size) { 
        new Thread(() -> { try { ImageIcon raw = new ImageIcon(new URL(urlStr)); SwingUtilities.invokeLater(() -> { label.setIcon(getShadowedCircularImageIcon(raw.getImage(), size)); }); } catch (Exception ignored) {} }).start(); 
    }
    
    public void updateAvatarFromBase64(String base64Image) {
        SwingUtilities.invokeLater(() -> { 
            try { 
                byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image); 
                Image rawImg = new ImageIcon(imageBytes).getImage(); 
                if (bigAvatarLabel != null) bigAvatarLabel.setIcon(getShadowedCircularImageIcon(rawImg, 100)); 
                if (miniAvatarLabel != null) miniAvatarLabel.setIcon(getShadowedCircularImageIcon(rawImg, 64)); 
            } catch (Exception e) {} 
        });
    }
    private ImageIcon getShadowedCircularImageIcon(Image rawImage, int size) {
        int padding = 4; int totalSize = size + padding * 2; BufferedImage buffer = new BufferedImage(totalSize, totalSize, BufferedImage.TYPE_INT_ARGB); Graphics2D g2 = buffer.createGraphics(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < padding; i++) { g2.setColor(new Color(0, 0, 0, 8)); g2.fillOval(padding - i, padding - i + 2, size + i * 2, size + i * 2); }
        BufferedImage circleImg = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB); Graphics2D cg2 = circleImg.createGraphics(); cg2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); cg2.fillOval(0, 0, size, size); cg2.setComposite(AlphaComposite.SrcIn);
        int imgW = rawImage.getWidth(null); int imgH = rawImage.getHeight(null); if (imgW > 0 && imgH > 0) { double scale = Math.max((double) size / imgW, (double) size / imgH); int drawW = (int) (imgW * scale); int drawH = (int) (imgH * scale); cg2.drawImage(rawImage, (size - drawW) / 2, (size - drawH) / 2, drawW, drawH, null); } cg2.dispose();
        g2.drawImage(circleImg, padding, padding, null); g2.dispose(); return new ImageIcon(buffer);
    }
}