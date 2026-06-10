package com.mycompany.tutorhub_enterprise.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class AcceptedClassTab extends JPanel {

    // --- COLOR PALETTE TỪ MOCKUP ---
    private final Color BG_MAIN = Color.decode("#FFFFFF");
    private final Color BG_HOVER = Color.decode("#F9FAFB");
    private final Color BORDER = Color.decode("#E5E7EB");
    private final Color PRIMARY = Color.decode("#2563EB");
    private final Color TEXT_DARK = Color.decode("#111827");
    private final Color TEXT_MUTED = Color.decode("#6B7280");

    // --- DATA MODEL ---
    public static class AcceptedClass {
        String id, title, subject, location, fee, schedule, studentName, parentName, startDate, endDate, status;
        int completedSessions, totalSessions;
        String thumbnailPath, note;
        boolean isOnline, hasUnreadMessage;
        double rating;

        public AcceptedClass(String id, String title, String subject, String location, String fee, String schedule, String studentName, String startDate, String status, int completedSessions, int totalSessions) {
            this.id = id; this.title = title; this.subject = subject; this.location = location; this.fee = fee;
            this.schedule = schedule; this.studentName = studentName; this.startDate = startDate; this.status = status;
            this.completedSessions = completedSessions; this.totalSessions = totalSessions;
        }
    }

    // --- STATE VARIABLES ---
    private final List<AcceptedClass> allClasses = new ArrayList<>();
    private String currentStatusFilter = "ALL";
    private String currentSortType = "Mới nhất";
    private String searchQuery = "";
    
    // --- UI COMPONENTS ---
    private JPanel classListPanel;
    private JScrollPane scrollPane;
    private JLabel lblTotal, lblTeaching, lblUpcoming, lblCompleted;
    private JPanel tabsPanel;
    private MainDashboard dashboard; 

    // --- CONSTRUCTORS ---
    public AcceptedClassTab() {
        this(null);
    }

    public AcceptedClassTab(MainDashboard dashboard) {
        this.dashboard = dashboard;
        setLayout(new BorderLayout());
        setBackground(BG_MAIN);
        
        initUI();
        loadAcceptedClasses();
    }

    private void initUI() {
        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);
        topSection.setBorder(new EmptyBorder(25, 30, 0, 30));

        // 1. Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JPanel titleBox = new JPanel(); titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS)); titleBox.setOpaque(false);
        JLabel lblTitle = new JLabel("Lớp đã nhận"); lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28)); lblTitle.setForeground(TEXT_DARK);
        JLabel lblSub = new JLabel("Quản lý các lớp bạn đã nhận và theo dõi tiến độ giảng dạy"); lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 14)); lblSub.setForeground(TEXT_MUTED);
        titleBox.add(lblTitle); titleBox.add(Box.createVerticalStrut(5)); titleBox.add(lblSub);
        
        // Search bar (ĐÃ HOÀN THIỆN LOGIC TÌM KIẾM)
        JPanel searchBox = new JPanel(new BorderLayout()); searchBox.setOpaque(false);
        JTextField txtSearch = new JTextField(20);
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm lớp theo tên, môn học...");
        txtSearch.setPreferredSize(new Dimension(250, 36));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applySearch(txtSearch.getText()); }
            public void removeUpdate(DocumentEvent e) { applySearch(txtSearch.getText()); }
            public void changedUpdate(DocumentEvent e) { applySearch(txtSearch.getText()); }
        });
        searchBox.add(txtSearch, BorderLayout.CENTER);

        headerPanel.add(titleBox, BorderLayout.WEST);
        headerPanel.add(searchBox, BorderLayout.EAST);
        topSection.add(headerPanel);
        topSection.add(Box.createVerticalStrut(25));

        // 2. KPI Cards
        JPanel kpiPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        kpiPanel.setOpaque(false);
        kpiPanel.add(createKpiCard("Tổng lớp đã nhận", "Tất cả các lớp", PRIMARY, "0", 1));
        kpiPanel.add(createKpiCard("Đang dạy", "Lớp đang diễn ra", Color.decode("#22C55E"), "0", 2));
        kpiPanel.add(createKpiCard("Sắp diễn ra", "Sắp bắt đầu", Color.decode("#F59E0B"), "0", 3));
        kpiPanel.add(createKpiCard("Đã hoàn thành", "Đã kết thúc", Color.decode("#8B5CF6"), "0", 4));
        topSection.add(kpiPanel);
        topSection.add(Box.createVerticalStrut(25));

        // 3. Filter Row
        topSection.add(createFilterBar());
        add(topSection, BorderLayout.NORTH);

        // 4. Class List Area
        classListPanel = new JPanel();
        classListPanel.setLayout(new BoxLayout(classListPanel, BoxLayout.Y_AXIS));
        classListPanel.setOpaque(false);
        classListPanel.setBorder(new EmptyBorder(10, 30, 30, 30));

        scrollPane = new JScrollPane(classListPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        
        add(scrollPane, BorderLayout.CENTER);
    }

    // ==========================================
    // HÀM NHẬN REAL-TIME TỪ HOMETAB
    // ==========================================
    public void addAcceptedClass(HomeTab.ClassModel m) {
        SwingUtilities.invokeLater(() -> {
            // Đẩy dữ liệu lớp học mới nhận vào danh sách
            AcceptedClass newClass = new AcceptedClass(
                m.id, m.subj, "Môn học", m.addr, m.sal, m.time, 
                "Chưa cập nhật", "Hôm nay", "TEACHING", 0, 10
            );
            allClasses.add(0, newClass); // Thêm lên đầu danh sách
            updateKpiCards();
            applyFiltersAndSort();
            
            // Cuộn lên trên cùng để người dùng thấy lớp mới
            if (scrollPane.getViewport() != null) scrollPane.getViewport().setViewPosition(new Point(0, 0));
        });
    }

    // ==========================================
    // DATA, FILTER & SORT LOGIC
    // ==========================================
    private void loadAcceptedClasses() {
        showLoadingState();
        
        // Giả lập gọi API (Sẽ thay bằng logic NetworkManager sau này)
        new Timer(600, e -> {
            ((Timer)e.getSource()).stop();
            allClasses.clear();
            allClasses.add(new AcceptedClass("C1", "Toán Cao Cấp KMA", "Toán", "Hà Đông, Hà Nội", "200.000đ", "Thứ 2,4,6 - 19:00", "Lê Minh Anh", "15/04/2024", "TEACHING", 13, 20));
            allClasses.add(new AcceptedClass("C2", "Tiếng Anh Giao Tiếp", "Tiếng Anh", "Thanh Xuân, Hà Nội", "250.000đ", "Thứ 3,5 - 18:00", "Nguyễn Hoàng Linh", "27/05/2024", "UPCOMING", 0, 15));
            allClasses.add(new AcceptedClass("C3", "Vật Lý Đại Cương", "Vật Lý", "Cầu Giấy, Hà Nội", "180.000đ", "Thứ 2,5 - 19:00", "Trần Bảo Long", "01/04/2024", "TEACHING", 8, 20));
            allClasses.add(new AcceptedClass("C4", "Luyện Thi IELTS", "IELTS", "Đống Đa, Hà Nội", "220.000đ", "Thứ 2, CN - 09:00", "Phạm Quỳnh Chi", "05/05/2024", "COMPLETED", 20, 20));
            allClasses.add(new AcceptedClass("C5", "Toán 9 Cơ Bản", "Toán", "Long Biên, Hà Nội", "150.000đ", "Thứ 3,7 - 17:30", "Vũ Hoàng Nam", "10/06/2024", "PAUSED", 5, 10));
            
            updateKpiCards();
            applyFiltersAndSort();
        }).start();
    }

    private void updateKpiCards() {
        int total = allClasses.size();
        int teaching = (int) allClasses.stream().filter(c -> c.status.equals("TEACHING")).count();
        int upcoming = (int) allClasses.stream().filter(c -> c.status.equals("UPCOMING")).count();
        int completed = (int) allClasses.stream().filter(c -> c.status.equals("COMPLETED")).count();

        lblTotal.setText(String.valueOf(total));
        lblTeaching.setText(String.valueOf(teaching));
        lblUpcoming.setText(String.valueOf(upcoming));
        lblCompleted.setText(String.valueOf(completed));
    }

    private void setStatusFilter(String status) {
        currentStatusFilter = status;
        applyFiltersAndSort();
    }

    private void applySearch(String keyword) {
        this.searchQuery = keyword.toLowerCase().trim();
        applyFiltersAndSort();
    }

    private void applyFiltersAndSort() {
        // 1. LỌC (TÌM KIẾM + TRẠNG THÁI)
        List<AcceptedClass> filtered = allClasses.stream()
            .filter(c -> currentStatusFilter.equals("ALL") || c.status.equals(currentStatusFilter))
            .filter(c -> searchQuery.isEmpty() || 
                         c.title.toLowerCase().contains(searchQuery) || 
                         c.subject.toLowerCase().contains(searchQuery) || 
                         c.studentName.toLowerCase().contains(searchQuery))
            .collect(Collectors.toList());

        // 2. SẮP XẾP
        sortClassList(filtered);
        
        // 3. RENDER LẠI GIAO DIỆN
        renderClassList(filtered);
    }

    private void sortClassList(List<AcceptedClass> list) {
        if (currentSortType.equals("Học phí cao đến thấp")) {
            list.sort((c1, c2) -> Double.compare(
                Double.parseDouble(c2.fee.replaceAll("[^0-9]", "")), 
                Double.parseDouble(c1.fee.replaceAll("[^0-9]", ""))
            ));
        } else if (currentSortType.equals("Học phí thấp đến cao")) {
            list.sort((c1, c2) -> Double.compare(
                Double.parseDouble(c1.fee.replaceAll("[^0-9]", "")), 
                Double.parseDouble(c2.fee.replaceAll("[^0-9]", ""))
            ));
        }
        // Nếu là "Mới nhất", giữ nguyên thứ tự (vì lúc thêm đã đẩy lên đầu list)
    }

    // ==========================================
    // RENDER UI
    // ==========================================
   private void renderClassList(List<AcceptedClass> list) {
        classListPanel.removeAll();
        if (list.isEmpty()) {
            showEmptyState("Không tìm thấy lớp phù hợp");
        } else {
            // SỬA LỖI Ở ĐÂY: Ép Swing trả lại Layout dạng danh sách dọc (BoxLayout)
            classListPanel.setLayout(new BoxLayout(classListPanel, BoxLayout.Y_AXIS));
            
            for (AcceptedClass cls : list) {
                classListPanel.add(createAcceptedClassRow(cls));
                classListPanel.add(Box.createVerticalStrut(15));
            }
        }
        classListPanel.revalidate();
        classListPanel.repaint();
    }

    private JPanel createFilterBar() {
        JPanel bar = new JPanel(new BorderLayout()); bar.setOpaque(false);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));

        tabsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0)); tabsPanel.setOpaque(false);
        String[] filters = {"Tất cả", "Đang dạy", "Sắp diễn ra", "Đã hoàn thành", "Đã hủy"};
        String[] filterKeys = {"ALL", "TEACHING", "UPCOMING", "COMPLETED", "CANCELLED"};
        
        for (int i=0; i<filters.length; i++) {
            final String key = filterKeys[i];
            JLabel tab = new JLabel(filters[i]); 
            tab.setFont(new Font("Segoe UI", Font.BOLD, 14));
            tab.setForeground(i == 0 ? PRIMARY : TEXT_MUTED);
            tab.setBorder(new EmptyBorder(0, 0, 10, 0));
            tab.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            tab.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    for (Component c : tabsPanel.getComponents()) {
                        c.setForeground(TEXT_MUTED);
                        ((JComponent)c).setBorder(new EmptyBorder(0,0,10,0));
                    }
                    tab.setForeground(PRIMARY);
                    tab.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(0,0,8,0), BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY)));
                    setStatusFilter(key);
                }
            });
            tabsPanel.add(tab);
        }
        if(tabsPanel.getComponentCount() > 0) ((JComponent)tabsPanel.getComponent(0)).setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(0,0,8,0), BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY)));

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); rightControls.setOpaque(false);
        
        rightControls.add(createControlBtn("Bộ lọc", "https://img.icons8.com/fluency-systems-regular/48/6B7280/filter.png", this::openAdvancedFilterModal));
        
        // Nút Dropdown Sắp xếp
        JPanel btnSort = createControlBtn("Sắp xếp ▼", "https://img.icons8.com/fluency-systems-regular/48/6B7280/sort.png", () -> {});
        setupDropdownMenu(btnSort, new String[]{"Mới nhất", "Học phí cao đến thấp", "Học phí thấp đến cao"});
        rightControls.add(btnSort);
        
        rightControls.add(createControlBtn("Xuất danh sách", "https://img.icons8.com/fluency-systems-regular/48/6B7280/export.png", this::exportClassList));

        bar.add(tabsPanel, BorderLayout.WEST);
        bar.add(rightControls, BorderLayout.EAST);
        return bar;
    }

    private JPanel createControlBtn(String text, String icon, Runnable action) {
        JPanel btn = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g2.setColor(BORDER); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,8,8); g2.dispose();
            }
        };
        btn.setOpaque(false); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(4, 10, 4, 10));
        JLabel lblIco = new JLabel(); setNetworkIcon(lblIco, icon, 14, 14);
        JLabel lblTxt = new JLabel(text); lblTxt.setFont(new Font("Segoe UI", Font.BOLD, 12)); lblTxt.setForeground(TEXT_MUTED);
        btn.add(lblIco); btn.add(lblTxt);
        btn.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { action.run(); }});
        return btn;
    }
    
    private void setupDropdownMenu(JPanel badge, String[] options) {
        JPopupMenu popupMenu = new JPopupMenu(); popupMenu.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        for (String option : options) {
            JMenuItem item = new JMenuItem("  " + option + "  "); 
            item.setFont(new Font("Segoe UI", Font.PLAIN, 14)); 
            item.setBackground(Color.WHITE); 
            item.setPreferredSize(new Dimension(190, 38)); 
            item.setCursor(new Cursor(Cursor.HAND_CURSOR));
            item.addActionListener(e -> { 
                currentSortType = option; 
                applyFiltersAndSort(); 
            }); 
            popupMenu.add(item);
        }
        badge.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { popupMenu.show(badge, 0, badge.getHeight() + 5); }
        });
    }

    // ==========================================
    // CLASS ROW (DÒNG LỚP HỌC)
    // ==========================================
    private JPanel createAcceptedClassRow(AcceptedClass cls) {
        JPanel row = new JPanel(new BorderLayout(15, 0)) {
            boolean isHover = false;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isHover ? BG_HOVER : Color.WHITE); g2.fillRoundRect(0,0,getWidth(),getHeight(),16,16);
                g2.setColor(isHover ? Color.decode("#93C5FD") : BORDER); g2.setStroke(new BasicStroke(isHover ? 1.5f : 1f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,16,16); g2.dispose();
            }
            public void setHover(boolean h) { this.isHover = h; repaint(); }
        };
        row.setOpaque(false); row.setBorder(new EmptyBorder(15, 15, 15, 15)); 
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        
        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { try{ row.getClass().getMethod("setHover", boolean.class).invoke(row, true); }catch(Exception ex){} }
            @Override public void mouseExited(MouseEvent e) { try{ row.getClass().getMethod("setHover", boolean.class).invoke(row, false); }catch(Exception ex){} }
            @Override public void mouseClicked(MouseEvent e) { openClassDetailModal(cls); }
        });

        // 1. Thumbnail + Badge
        JPanel thumbWrap = new JPanel(new BorderLayout()); thumbWrap.setOpaque(false); thumbWrap.setPreferredSize(new Dimension(160, 0));
        ImageHeaderPanel thumb = new ImageHeaderPanel(getSubjectImagePath(cls.subject), Color.decode("#1F2937"), Color.decode("#475569"));
        thumb.putClientProperty("JComponent.arc", 12);
        
        thumb.add(createStatusBadge(cls.status), BorderLayout.NORTH);
        thumbWrap.add(thumb, BorderLayout.CENTER);
        row.add(thumbWrap, BorderLayout.WEST);

        // 2. Center Info (Dùng BoxLayout ngang)
        JPanel centerWrap = new JPanel();
        centerWrap.setLayout(new BoxLayout(centerWrap, BoxLayout.X_AXIS));
        centerWrap.setOpaque(false);

        // Info Cột 1
        JPanel infoP = new JPanel(); infoP.setLayout(new BoxLayout(infoP, BoxLayout.Y_AXIS)); infoP.setOpaque(false); 
        infoP.setMaximumSize(new Dimension(280, 999));
        JLabel lblTitle = new JLabel(cls.title); lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16)); lblTitle.setForeground(TEXT_DARK);
        infoP.add(lblTitle); infoP.add(Box.createVerticalStrut(8));
        infoP.add(createIconTextRow("https://img.icons8.com/fluency-systems-regular/48/6B7280/marker.png", cls.location)); infoP.add(Box.createVerticalStrut(4));
        JLabel lblFee = new JLabel(cls.fee + "/buổi"); lblFee.setFont(new Font("Segoe UI", Font.BOLD, 14)); lblFee.setForeground(Color.decode("#10B981"));
        infoP.add(lblFee); infoP.add(Box.createVerticalStrut(4));
        infoP.add(createIconTextRow("https://img.icons8.com/fluency-systems-regular/48/6B7280/clock--v1.png", cls.schedule));

        // Info Cột 2
        JPanel stuP = new JPanel(); stuP.setLayout(new BoxLayout(stuP, BoxLayout.Y_AXIS)); stuP.setOpaque(false); 
        stuP.setMaximumSize(new Dimension(200, 999));
        stuP.add(Box.createVerticalStrut(25));
        stuP.add(createIconTextRow("https://img.icons8.com/fluency-systems-regular/48/6B7280/person-male.png", "Học viên: " + cls.studentName)); stuP.add(Box.createVerticalStrut(15));
        stuP.add(createIconTextRow("https://img.icons8.com/fluency-systems-regular/48/6B7280/calendar.png", "Bắt đầu: " + cls.startDate));

        // Info Cột 3 (Tiến độ)
        JPanel progP = createProgressSection(cls);
        progP.setMaximumSize(new Dimension(220, 999));

        centerWrap.add(infoP); 
        centerWrap.add(Box.createHorizontalStrut(10));
        centerWrap.add(new JSeparator(JSeparator.VERTICAL)); 
        centerWrap.add(Box.createHorizontalStrut(20));
        centerWrap.add(stuP); 
        centerWrap.add(Box.createHorizontalStrut(10));
        centerWrap.add(new JSeparator(JSeparator.VERTICAL)); 
        centerWrap.add(Box.createHorizontalStrut(20));
        centerWrap.add(progP);
        centerWrap.add(Box.createHorizontalGlue()); 
        
        row.add(centerWrap, BorderLayout.CENTER);

        // 3. Action Buttons
        JPanel actionP = new JPanel(); actionP.setLayout(new BoxLayout(actionP, BoxLayout.Y_AXIS)); actionP.setOpaque(false); 
        actionP.setPreferredSize(new Dimension(140, 0));
        actionP.add(Box.createVerticalGlue());
        
        JButton btnMain = new JButton(); btnMain.setFont(new Font("Segoe UI", Font.BOLD, 13)); btnMain.setFocusPainted(false); btnMain.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnMain.setMaximumSize(new Dimension(140, 36)); btnMain.setPreferredSize(new Dimension(140, 36));
        
        if (cls.status.equals("TEACHING")) {
            btnMain.setText("Vào lớp"); btnMain.setBackground(PRIMARY); btnMain.setForeground(Color.WHITE);
            btnMain.addActionListener(e -> handleJoinClass(cls));
        } else if (cls.status.equals("UPCOMING")) {
            btnMain.setText("Nhắc lịch"); btnMain.setBackground(PRIMARY); btnMain.setForeground(Color.WHITE);
        } else if (cls.status.equals("COMPLETED")) {
            btnMain.setText("Xem đánh giá"); btnMain.setBackground(PRIMARY); btnMain.setForeground(Color.WHITE);
            btnMain.addActionListener(e -> openReviewModal(cls)); 
        } else if (cls.status.equals("PAUSED")) {
            btnMain.setText("Tiếp tục lớp"); btnMain.setBackground(PRIMARY); btnMain.setForeground(Color.WHITE);
            btnMain.addActionListener(e -> handleResumeClass(cls)); 
        } else {
            btnMain.setText("Đã hủy"); btnMain.setBackground(Color.decode("#F3F4F6")); btnMain.setForeground(TEXT_MUTED); btnMain.setEnabled(false);
        }

        JButton btnChat = new JButton("Nhắn tin"); btnChat.setFont(new Font("Segoe UI", Font.BOLD, 13)); btnChat.setBackground(Color.WHITE); btnChat.setForeground(TEXT_DARK);
        btnChat.setMaximumSize(new Dimension(140, 36)); btnChat.setFocusPainted(false); btnChat.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnChat.addActionListener(e -> handleOpenChat(cls)); 

        JLabel lblDetail = new JLabel("Xem chi tiết >", SwingConstants.CENTER); lblDetail.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblDetail.setForeground(PRIMARY); lblDetail.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblDetail.setAlignmentX(Component.CENTER_ALIGNMENT);

        actionP.add(btnMain); actionP.add(Box.createVerticalStrut(8)); actionP.add(btnChat); actionP.add(Box.createVerticalStrut(8)); actionP.add(lblDetail);
        actionP.add(Box.createVerticalGlue());

        row.add(actionP, BorderLayout.EAST);
        return row;
    }

    private JPanel createStatusBadge(String status) {
        Color bg, fg; String text;
        switch (status) {
            case "TEACHING": bg = Color.decode("#DCFCE7"); fg = Color.decode("#15803D"); text = "Đang dạy"; break;
            case "UPCOMING": bg = Color.decode("#FEF3C7"); fg = Color.decode("#B45309"); text = "Sắp diễn ra"; break;
            case "COMPLETED": bg = Color.decode("#F3E8FF"); fg = Color.decode("#6D28D9"); text = "Đã hoàn thành"; break;
            case "PAUSED": bg = Color.decode("#F3F4F6"); fg = Color.decode("#4B5563"); text = "Tạm hoãn"; break;
            default: bg = Color.decode("#FEE2E2"); fg = Color.decode("#B91C1C"); text = "Đã hủy"; break;
        }

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); p.setOpaque(false); p.setBorder(new EmptyBorder(8, 8, 0, 0));
        JPanel badge = new JPanel(new BorderLayout()) { @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(bg); g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8); g2.dispose(); }};
        badge.setOpaque(false); badge.setBorder(new EmptyBorder(2, 8, 2, 8));
        JLabel lbl = new JLabel(text); lbl.setFont(new Font("Segoe UI", Font.BOLD, 11)); lbl.setForeground(fg);
        badge.add(lbl); p.add(badge); return p;
    }

    private JPanel createProgressSection(AcceptedClass cls) {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setOpaque(false);
        p.add(Box.createVerticalStrut(15));
        
        int prog = cls.totalSessions == 0 ? 0 : (int)((cls.completedSessions / (float)cls.totalSessions) * 100);
        
        JPanel topRow = new JPanel(new BorderLayout()); topRow.setOpaque(false); topRow.setMaximumSize(new Dimension(200, 20));
        JLabel lblT = new JLabel("Tiến độ"); lblT.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblT.setForeground(TEXT_MUTED);
        JLabel lblP = new JLabel(prog + "%"); lblP.setFont(new Font("Segoe UI", Font.BOLD, 13)); lblP.setForeground(TEXT_DARK);
        topRow.add(lblT, BorderLayout.WEST); topRow.add(lblP, BorderLayout.EAST);
        p.add(topRow); p.add(Box.createVerticalStrut(5));

        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode("#E5E7EB")); g2.fillRoundRect(0,0,getWidth(),getHeight(),6,6);
                int w = (int)(getWidth() * (prog / 100f));
                g2.setColor(cls.status.equals("COMPLETED") ? Color.decode("#8B5CF6") : (cls.status.equals("TEACHING") ? Color.decode("#22C55E") : PRIMARY)); 
                g2.fillRoundRect(0,0,w,getHeight(),6,6); g2.dispose();
            }
        };
        bar.setOpaque(false); bar.setMaximumSize(new Dimension(200, 6)); bar.setPreferredSize(new Dimension(200, 6));
        p.add(bar); p.add(Box.createVerticalStrut(5));

        JLabel lblSub = new JLabel(cls.status.equals("COMPLETED") ? "Hoàn thành khóa học" : (cls.completedSessions + "/" + cls.totalSessions + " buổi hoàn thành"));
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblSub.setForeground(TEXT_MUTED);
        p.add(lblSub); p.add(Box.createVerticalGlue());
        return p;
    }

    // ==========================================
    // KHU VỰC CÁC HÀM XỬ LÝ (ACTIONS) BỊ THIẾU
    // ==========================================
    private void handleJoinClass(AcceptedClass cls) {
        showToast("Đang kết nối vào lớp " + cls.title + "...", "INFO");
        if (dashboard != null) {
            // Gọi hàm cầu nối bên MainDashboard
            dashboard.joinLiveClass(cls.id, cls.title);
        }
    }

    private void openReviewModal(AcceptedClass cls) {
        showToast("Đang tải đánh giá của " + cls.studentName, "INFO");
    }

    private void handleResumeClass(AcceptedClass cls) {
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có muốn tiếp tục dạy lớp " + cls.title + "?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            cls.status = "TEACHING";
            updateKpiCards();
            applyFiltersAndSort();
            showToast("Đã khôi phục trạng thái lớp", "SUCCESS");
        }
    }

    private void handleOpenChat(AcceptedClass cls) {
        if(dashboard != null) dashboard.switchToCard("Chat");
    }

    private void openClassDetailModal(AcceptedClass cls) {
        showToast("Mở chi tiết lớp: " + cls.title, "INFO");
    }

    private void exportClassList() {
        showToast("Đang xuất danh sách ra file Excel...", "SUCCESS");
    }

    private void openAdvancedFilterModal() {
        showToast("Tính năng lọc nâng cao đang phát triển", "WARNING");
    }

    // ==========================================
    // HELPERS (UI, ICON & ẢNH BÌA)
    // ==========================================
    private JPanel createKpiCard(String title, String sub, Color iconBg, String startVal, int type) {
        JPanel card = new JPanel(new BorderLayout(15, 0)) {
            @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(Color.WHITE); g2.fillRoundRect(0,0,getWidth(),getHeight(),16,16); g2.setColor(BORDER); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,16,16); g2.dispose(); }
        };
        card.setOpaque(false); card.setBorder(new EmptyBorder(20, 20, 20, 20)); card.setPreferredSize(new Dimension(0, 100));

        JPanel iconP = new JPanel(new BorderLayout()) { @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(new Color(iconBg.getRed(), iconBg.getGreen(), iconBg.getBlue(), 20)); g2.fillOval(0,0,getWidth(),getHeight()); g2.dispose(); }};
        iconP.setOpaque(false); iconP.setPreferredSize(new Dimension(48, 48));
        String icoStr = type == 1 ? "https://img.icons8.com/fluency-systems-filled/48/2563EB/book.png" : (type == 2 ? "https://img.icons8.com/fluency-systems-filled/48/22C55E/play.png" : (type == 3 ? "https://img.icons8.com/fluency-systems-filled/48/F59E0B/clock.png" : "https://img.icons8.com/fluency-systems-filled/48/8B5CF6/checkmark.png"));
        JLabel lblIcon = new JLabel(); setNetworkIcon(lblIcon, icoStr, 24, 24); iconP.add(lblIcon, BorderLayout.CENTER);

        JPanel textP = new JPanel(); textP.setLayout(new BoxLayout(textP, BoxLayout.Y_AXIS)); textP.setOpaque(false);
        JLabel lblTitle = new JLabel(title); lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15)); lblTitle.setForeground(PRIMARY);
        JLabel lblVal = new JLabel(startVal); lblVal.setFont(new Font("Segoe UI", Font.BOLD, 24)); lblVal.setForeground(TEXT_DARK);
        JLabel lblSub = new JLabel(sub); lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblSub.setForeground(TEXT_MUTED);
        textP.add(lblTitle); textP.add(Box.createVerticalStrut(4)); textP.add(lblVal); textP.add(lblSub);

        if(type==1) lblTotal = lblVal; else if(type==2) lblTeaching = lblVal; else if(type==3) lblUpcoming = lblVal; else lblCompleted = lblVal;

        card.add(iconP, BorderLayout.WEST); card.add(textP, BorderLayout.CENTER); return card;
    }

    private void showEmptyState(String msg) {
        classListPanel.removeAll();
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setOpaque(false);
        JLabel ico = new JLabel(); setNetworkIcon(ico, "https://img.icons8.com/fluency/96/empty-box.png", 80, 80); ico.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel txt = new JLabel(msg); txt.setFont(new Font("Segoe UI", Font.BOLD, 16)); txt.setForeground(TEXT_DARK); txt.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(Box.createVerticalStrut(80)); p.add(ico); p.add(Box.createVerticalStrut(15)); p.add(txt);
        classListPanel.setLayout(new BorderLayout()); classListPanel.add(p, BorderLayout.CENTER);
    }

    private void showLoadingState() {
        classListPanel.removeAll(); classListPanel.setLayout(new BorderLayout());
        JLabel lbl = new JLabel("Đang tải dữ liệu...", SwingConstants.CENTER); lbl.setFont(new Font("Segoe UI", Font.ITALIC, 14)); lbl.setForeground(TEXT_MUTED);
        classListPanel.add(lbl, BorderLayout.CENTER); classListPanel.revalidate(); classListPanel.repaint();
    }

    private void showToast(String message, String type) {
        JOptionPane.showMessageDialog(this, message, type, JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel createIconTextRow(String iconUrl, String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); p.setOpaque(false); p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel iconLbl = new JLabel(); setNetworkIcon(iconLbl, iconUrl, 16, 16);
        JLabel txtLbl = new JLabel("<html><div style='width: 170px;'>" + text + "</div></html>"); txtLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13)); txtLbl.setForeground(TEXT_MUTED);
        p.add(iconLbl); p.add(txtLbl); return p;
    }

    private String getSubjectImagePath(String subj) {
        if (subj == null || subj.trim().isEmpty()) return "/images/general/general1.png";
        String s = subj.toLowerCase(); java.util.Random rand = new java.util.Random(); int index = rand.nextInt(6) + 1;
        if (s.contains("ielts")) return "/images/IELTS/IELTS" + index + ".jpg";
        if (s.contains("anh") || s.contains("toeic") || s.contains("toefl")) return "/images/english/english" + index + ".jpg";
        if (s.contains("toán") || s.contains("đại số") || s.contains("hình học") || s.contains("giải tích")) return "/images/math/math" + index + ".jpg";
        if (s.contains("lý") || s.contains("vật lý") || s.contains("cơ học")) return "/images/physics/physics" + index + ".jpg";
        if (s.contains("hóa")) return "/images/chemistry/chemistry" + index + ".jpg";
        if (s.contains("văn") || s.contains("ngữ văn") || s.contains("tiếng việt")) return "/images/literature/literature" + index + ".jpg";
        if (s.contains("tin") || s.contains("lập trình") || s.contains("java") || s.contains("python") || s.contains("it")) return "/images/it/it" + index + ".jpg";
        return "/images/general/general1.png";
    }

    private void setNetworkIcon(JLabel label, String urlStr, int width, int height) {
        new Thread(() -> { try { ImageIcon raw = new ImageIcon(new URL(urlStr)); Image img = raw.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH); SwingUtilities.invokeLater(() -> label.setIcon(new ImageIcon(img))); } catch (Exception e) {} }).start();
    }

    // --- CLASS VẼ ẢNH BÌA BO GÓC (ĐÃ ĐƯỢC KHÔI PHỤC) ---
    class ImageHeaderPanel extends JPanel {
        private Image image; private final Color fallbackA; private final Color fallbackB;
        public ImageHeaderPanel(String resourcePath, Color fallbackA, Color fallbackB) { this.fallbackA = fallbackA; this.fallbackB = fallbackB; setOpaque(false); setLayout(new BorderLayout()); try { URL url = getClass().getResource(resourcePath); if (url != null) image = new ImageIcon(url).getImage(); } catch (Exception ignored) {} }
        @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
        Object arcObj = getClientProperty("JComponent.arc"); int arc = arcObj instanceof Integer ? (Integer) arcObj : 0;
        Shape oldClip = g2.getClip(); if (arc > 0) { g2.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arc, arc)); }
        if (image != null) { int w = getWidth(); int h = getHeight(); int imgW = image.getWidth(null); int imgH = image.getHeight(null); if (imgW > 0 && imgH > 0) { double scale = Math.max((double) w / imgW, (double) h / imgH); int drawW = (int) (imgW * scale); int drawH = (int) (imgH * scale); int x = (w - drawW) / 2; int y = (h - drawH) / 2; g2.drawImage(image, x, y, drawW, drawH, null); g2.setColor(new Color(17, 24, 39, 42)); g2.fillRect(0, 0, w, h); } } else { GradientPaint gp = new GradientPaint(0, 0, fallbackA, getWidth(), getHeight(), fallbackB); g2.setPaint(gp); g2.fillRect(0, 0, getWidth(), getHeight()); } g2.setClip(oldClip); g2.dispose(); super.paintComponent(g); }
    }
}