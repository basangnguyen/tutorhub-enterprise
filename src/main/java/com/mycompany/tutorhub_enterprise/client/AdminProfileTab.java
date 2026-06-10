package com.mycompany.tutorhub_enterprise.client;

import com.mycompany.tutorhub_enterprise.models.Packet;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;

public class AdminProfileTab extends JPanel {

    private final Color BG_MAIN = Color.decode("#F4F7F9");
    private final Color TEXT_MAIN = Color.decode("#111827");
    private final Color TEXT_MUTED = Color.decode("#6B7280");
    private final Color PRIMARY = Color.decode("#2563EB");
    private final Color CARD_BG = Color.WHITE;
    private final Color BORDER_COLOR = Color.decode("#E5E7EB");
    
    // UI Elements for Avatar
    private JLabel bigAvatarLabel;
    private JLabel miniAvatarLabel;
    
    private byte[] pendingAvatarBytes = null;
    private Image pendingRawImage = null;
    
    // Cờ chặn ảnh mặc định khi có ảnh Base64
    private boolean hasCustomAvatar = false; 

    private String adminName;

    // ==========================================================
    // KIẾN TRÚC THÔNG BÁO: ĐỊNH NGHĨA LISTENER ĐỂ BÁO CHO PARENT
    // ==========================================================
    public interface AvatarUpdateListener {
        void onAvatarUpdated(Image newAvatarImage); // Báo cho CenterDashboard biết ảnh đã đổi
    }
    private AvatarUpdateListener avatarListener;

    public void setAvatarUpdateListener(AvatarUpdateListener listener) {
        this.avatarListener = listener;
    }
    // ----------------------------------------------------------

    public AdminProfileTab(String adminName) {
        this.adminName = adminName;
        setLayout(new BorderLayout());
        setBackground(BG_MAIN);

        JPanel topArea = new JPanel(new BorderLayout());
        topArea.setOpaque(false);
        topArea.add(createHeader(), BorderLayout.NORTH);
        topArea.add(createTabs(), BorderLayout.SOUTH);
        add(topArea, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(24, 0));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(24, 40, 40, 40));

        contentPanel.add(createLeftColumn(), BorderLayout.WEST);
        contentPanel.add(createCenterColumn(), BorderLayout.CENTER);
        contentPanel.add(createRightColumn(), BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(contentPanel);
        scroll.setBorder(null); scroll.getViewport().setOpaque(false); scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16); scroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0)); header.setOpaque(false); header.setBorder(new EmptyBorder(30, 40, 10, 40));
        JPanel iconWrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(BORDER_COLOR); g2.drawOval(0, 0, getWidth()-1, getHeight()-1); g2.dispose();
            }
        };
        iconWrapper.setOpaque(false); iconWrapper.setPreferredSize(new Dimension(48, 48));
        JLabel lblIcon = new JLabel(); setNetworkIcon(lblIcon, "https://img.icons8.com/fluency-systems-regular/48/2563EB/settings.png", 24, 24);
        iconWrapper.add(lblIcon, BorderLayout.CENTER);

        JPanel titlePanel = new JPanel(); titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS)); titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Cài đặt hệ thống & Tài khoản"); lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22)); lblTitle.setForeground(TEXT_MAIN);
        JLabel lblBreadcrumb = new JLabel("Trang chủ  >  Cài đặt"); lblBreadcrumb.setFont(new Font("Segoe UI", Font.PLAIN, 13)); lblBreadcrumb.setForeground(TEXT_MUTED);
        titlePanel.add(lblTitle); titlePanel.add(Box.createVerticalStrut(4)); titlePanel.add(lblBreadcrumb);

        header.add(iconWrapper); header.add(titlePanel); return header;
    }

    private JPanel createTabs() {
        JPanel tabsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 0)); tabsPanel.setOpaque(false);
        tabsPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(0, 40, 0, 40), BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR)));
        tabsPanel.add(createTabItem("Thông tin Quản trị viên", true)); 
        tabsPanel.add(createTabItem("Phân quyền nội bộ", false));
        tabsPanel.add(createTabItem("Cấu hình Trung tâm", false)); 
        tabsPanel.add(createTabItem("Bảo mật", false));
        return tabsPanel;
    }

    private JPanel createTabItem(String text, boolean isActive) {
        JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false); p.setBorder(new EmptyBorder(10, 0, 10, 0)); p.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JLabel lbl = new JLabel(text); lbl.setFont(new Font("Segoe UI", isActive ? Font.BOLD : Font.PLAIN, 14)); lbl.setForeground(isActive ? PRIMARY : TEXT_MUTED); p.add(lbl, BorderLayout.CENTER);
        if (isActive) { JPanel line = new JPanel(); line.setBackground(PRIMARY); line.setPreferredSize(new Dimension(0, 2)); p.add(line, BorderLayout.SOUTH); }
        return p;
    }

    private JPanel createLeftColumn() {
        RoundedPanel p = new RoundedPanel(20); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setPreferredSize(new Dimension(300, 0));
        
        bigAvatarLabel = new JLabel(); 
        // Đặt ảnh mặc định ban đầu
        setAvatarNetworkIcon(bigAvatarLabel, "https://img.icons8.com/color/144/circled-user-male-skin-type-4--v1.png", 130); 
        bigAvatarLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0)); nameRow.setOpaque(false);
        JLabel lblName = new JLabel(adminName); lblName.setFont(new Font("Segoe UI", Font.BOLD, 20)); lblName.setForeground(TEXT_MAIN);
        JLabel lblCheck = new JLabel(); setNetworkIcon(lblCheck, "https://img.icons8.com/fluency/48/verified-badge.png", 20, 20);
        nameRow.add(lblName); nameRow.add(lblCheck);
        JLabel lblRole = new JLabel("Giám đốc Vận hành (COO)"); lblRole.setFont(new Font("Segoe UI", Font.PLAIN, 14)); lblRole.setForeground(TEXT_MUTED); lblRole.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(Box.createVerticalStrut(30)); p.add(bigAvatarLabel); p.add(Box.createVerticalStrut(15));
        p.add(nameRow); p.add(Box.createVerticalStrut(5)); p.add(lblRole); p.add(Box.createVerticalStrut(25));

        JPanel infoList = new JPanel(); infoList.setLayout(new BoxLayout(infoList, BoxLayout.Y_AXIS)); infoList.setOpaque(false); infoList.setBorder(new EmptyBorder(0, 20, 0, 20));
        infoList.add(createInfoRow("https://img.icons8.com/fluency-systems-regular/48/6B7280/identity-theft.png", "Mã nhân sự:", "AD_001")); infoList.add(Box.createVerticalStrut(12));
        infoList.add(createInfoRow("https://img.icons8.com/fluency-systems-regular/48/6B7280/calendar--v1.png", "Ngày gia nhập:", "01/01/2022")); infoList.add(Box.createVerticalStrut(12));
        infoList.add(createInfoRow("https://img.icons8.com/fluency-systems-regular/48/6B7280/lock.png", "Quyền hạn:", "Toàn quyền")); infoList.add(Box.createVerticalStrut(12));
        JPanel statusRow = createInfoRow("https://img.icons8.com/fluency-systems-regular/48/6B7280/ok.png", "Trạng thái:", "Đang Online");
        ((JLabel) statusRow.getComponent(2)).setForeground(Color.decode("#10B981")); infoList.add(statusRow);
        p.add(infoList); p.add(Box.createVerticalStrut(30));

        return p;
    }

    private JPanel createInfoRow(String icon, String label, String val) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); p.setOpaque(false); p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblIcon = new JLabel(); setNetworkIcon(lblIcon, icon, 18, 18);
        JLabel lblLab = new JLabel(label); lblLab.setFont(new Font("Segoe UI", Font.PLAIN, 13)); lblLab.setForeground(TEXT_MUTED);
        JLabel lblVal = new JLabel(val); lblVal.setFont(new Font("Segoe UI", Font.BOLD, 13)); lblVal.setForeground(TEXT_MAIN);
        p.add(lblIcon); p.add(lblLab); p.add(lblVal); return p;
    }

    private JPanel createCenterColumn() {
        RoundedPanel p = new RoundedPanel(20); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setBorder(new EmptyBorder(30, 30, 30, 30));
        JLabel title = new JLabel("Thông tin Quản trị viên"); title.setFont(new Font("Segoe UI", Font.BOLD, 18)); title.setForeground(TEXT_MAIN); title.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(title); p.add(Box.createVerticalStrut(25));

        JPanel grid = new JPanel(new GridLayout(4, 2, 20, 20)); grid.setOpaque(false); grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.add(createInputGroup("Họ và tên", adminName)); grid.add(createInputGroup("Ngày sinh", "15/08/1990"));
        grid.add(createInputGroup("Số điện thoại", "0912 345 678")); grid.add(createInputGroup("Email quản trị", "admin@tutorhub.vn"));
        grid.add(createComboGroup("Phòng ban", new String[]{"Ban Giám Đốc", "Phòng Vận Hành", "Phòng Kế Toán"})); grid.add(createInputGroup("Chi nhánh", "Trụ sở chính (Hà Nội)"));
        grid.add(createComboGroup("Báo cáo báo động", new String[]{"Qua Email & SMS", "Chỉ qua Email", "Tắt thông báo"})); grid.add(createInputGroup("MST Cá nhân", "0123456789"));
        p.add(grid); p.add(Box.createVerticalStrut(20));

        JLabel lblBio = new JLabel("Ghi chú nội bộ (Chỉ hiển thị với Admin khác)"); lblBio.setFont(new Font("Segoe UI", Font.BOLD, 13)); lblBio.setForeground(TEXT_MAIN); lblBio.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextArea txtBio = new JTextArea("Phụ trách phê duyệt tài khoản gia sư cấp cao và xử lý các vấn đề thanh toán, khiếu nại từ phụ huynh.");
        txtBio.setFont(new Font("Segoe UI", Font.PLAIN, 14)); txtBio.setLineWrap(true); txtBio.setWrapStyleWord(true); txtBio.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR), new EmptyBorder(10, 10, 10, 10)));
        JScrollPane scrollBio = new JScrollPane(txtBio); scrollBio.setPreferredSize(new Dimension(0, 100)); scrollBio.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lblBio); p.add(Box.createVerticalStrut(8)); p.add(scrollBio); p.add(Box.createVerticalStrut(25));

        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); btnWrap.setOpaque(false); btnWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton btnSave = new JButton("Lưu thay đổi") { @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(PRIMARY); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12); super.paintComponent(g); g2.dispose(); } };
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 14)); btnSave.setForeground(Color.WHITE); btnSave.setContentAreaFilled(false); btnSave.setBorderPainted(false); btnSave.setPreferredSize(new Dimension(140, 40)); btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // --- SỰ KIỆN LƯU THÔNG TIN (GỬI ẢNH LÊN SERVER TẠI ĐÂY) ---
        btnSave.addActionListener(e -> {
            // Chỉ gửi lên Server nếu người dùng đã đổi ảnh (pendingAvatarBytes != null)
            if (pendingAvatarBytes != null && pendingRawImage != null) {
                new Thread(() -> {
                    try {
                        // 1. Chuyển ảnh sang Base64 string
                        String base64Image = Base64.getEncoder().encodeToString(pendingAvatarBytes);
                        
                        // 2. Gửi lệnh UPDATE_AVATAR lên Server
                        Packet packet = new Packet("UPDATE_AVATAR", base64Image);
                        NetworkManager.getInstance().sendPacket(packet);
                        // Server sẽ phản hồi (xử lý case này ở CenterDashboard nếu cần)

                        // ==========================================================
                        // BƯỚC THÔNG BÁO QUAN TRỌNG: GỌI LISTENER ĐỂ BÁO CHO PARENT
                        // ==========================================================
                        if (avatarListener != null) {
                            avatarListener.onAvatarUpdated(pendingRawImage); // Truyền tấm ảnh thật đi
                        }
                        // ----------------------------------------------------------
                        
                        pendingAvatarBytes = null; // Xóa cache
                    } catch (Exception ex) {
                        System.err.println("Lỗi tải ảnh lên Server: " + ex.getMessage());
                    }
                }).start();
            }
            JOptionPane.showMessageDialog(this, "Đã lưu thông tin hồ sơ và ảnh đại diện thành công!");
        });
        
        btnWrap.add(btnSave); p.add(btnWrap); return p;
    }

    private JPanel createInputGroup(String label, String value) {
        JPanel p = new JPanel(new BorderLayout(0, 8)); p.setOpaque(false);
        JLabel lbl = new JLabel(label); lbl.setFont(new Font("Segoe UI", Font.BOLD, 13)); lbl.setForeground(TEXT_MAIN);
        JTextField txt = new JTextField(value); txt.setFont(new Font("Segoe UI", Font.PLAIN, 14)); txt.setPreferredSize(new Dimension(0, 40)); txt.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR), new EmptyBorder(0, 12, 0, 12)));
        p.add(lbl, BorderLayout.NORTH); p.add(txt, BorderLayout.CENTER); return p;
    }
    
    private JPanel createComboGroup(String label, String[] items) {
        JPanel p = new JPanel(new BorderLayout(0, 8)); p.setOpaque(false);
        JLabel lbl = new JLabel(label); lbl.setFont(new Font("Segoe UI", Font.BOLD, 13)); lbl.setForeground(TEXT_MAIN);
        JComboBox<String> cb = new JComboBox<>(items); cb.setFont(new Font("Segoe UI", Font.PLAIN, 14)); cb.setBackground(Color.WHITE); cb.setPreferredSize(new Dimension(0, 40));
        p.add(lbl, BorderLayout.NORTH); p.add(cb, BorderLayout.CENTER); return p;
    }

    private JPanel createRightColumn() {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setOpaque(false); p.setPreferredSize(new Dimension(320, 0));

        RoundedPanel box1 = new RoundedPanel(20); box1.setLayout(new BorderLayout(15, 0)); box1.setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel title1 = new JLabel("Ảnh hồ sơ"); title1.setFont(new Font("Segoe UI", Font.BOLD, 16)); box1.add(title1, BorderLayout.NORTH);
        
        JPanel avaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10)); avaPanel.setOpaque(false);
        miniAvatarLabel = new JLabel(); 
        setAvatarNetworkIcon(miniAvatarLabel, "https://img.icons8.com/color/144/circled-user-male-skin-type-4--v1.png", 64);
        
        JPanel avaText = new JPanel(); avaText.setLayout(new BoxLayout(avaText, BoxLayout.Y_AXIS)); avaText.setOpaque(false);
        JLabel lblAvaReq = new JLabel("JPG, PNG. Max 2MB."); lblAvaReq.setFont(new Font("Segoe UI", Font.PLAIN, 11)); lblAvaReq.setForeground(TEXT_MUTED);
        
        // --- NÚT ĐỔI ẢNH (CHỈ XEM TRƯỚC LÀM CỤC BỘ) ---
        JButton btnChangeAva = new JButton("Đổi ảnh"); 
        btnChangeAva.setBackground(Color.WHITE); btnChangeAva.setForeground(PRIMARY); btnChangeAva.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnChangeAva.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Hình ảnh (JPG, PNG)", "jpg", "png", "jpeg"));
            
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (file.length() > 2 * 1024 * 1024) { // 2MB
                    JOptionPane.showMessageDialog(this, "Ảnh quá lớn! Vui lòng chọn ảnh dưới 2MB.");
                    return;
                }
                
                try {
                    // 1. Đọc ảnh vào cache
                    pendingAvatarBytes = Files.readAllBytes(file.toPath());
                    pendingRawImage = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(pendingAvatarBytes));
                    
                    // 2. Cập nhật xem trước CỤC BỘ trong trang cài đặt
                    hasCustomAvatar = true; // Chặn load ảnh mặc định
                    bigAvatarLabel.setIcon(getCircularImageIcon(pendingRawImage, 120)); // Cắt bo tròn to
                    miniAvatarLabel.setIcon(getCircularImageIcon(pendingRawImage, 64)); // Cắt bo tròn nhỏ
                    
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi đọc file ảnh: " + ex.getMessage());
                }
            }
        });
        // ----------------------------------------------
        
        avaText.add(lblAvaReq); avaText.add(Box.createVerticalStrut(8)); avaText.add(btnChangeAva);
        avaPanel.add(miniAvatarLabel); avaPanel.add(avaText); box1.add(avaPanel, BorderLayout.CENTER);
        
        RoundedPanel box2 = new RoundedPanel(20); box2.setLayout(new BoxLayout(box2, BoxLayout.Y_AXIS)); box2.setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel title2 = new JLabel("Hoạt động quản trị"); title2.setFont(new Font("Segoe UI", Font.BOLD, 16)); title2.setAlignmentX(Component.LEFT_ALIGNMENT); box2.add(title2); box2.add(Box.createVerticalStrut(15));
        box2.add(createQuickStatRow("https://img.icons8.com/fluency-systems-regular/48/6B7280/conference-call.png", "Gia sư phụ trách", "128 người")); box2.add(Box.createVerticalStrut(10));
        box2.add(createQuickStatRow("https://img.icons8.com/fluency-systems-regular/48/6B7280/pass.png", "Hồ sơ đã duyệt", "450 hồ sơ")); box2.add(Box.createVerticalStrut(10));
        box2.add(createQuickStatRow("https://img.icons8.com/fluency-systems-regular/48/6B7280/time.png", "Cập nhật lần cuối", "Hôm nay 08:30"));

        RoundedPanel box3 = new RoundedPanel(20); box3.setLayout(new BoxLayout(box3, BoxLayout.Y_AXIS)); box3.setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel title3 = new JLabel("Nhật ký hệ thống"); title3.setFont(new Font("Segoe UI", Font.BOLD, 16)); title3.setAlignmentX(Component.LEFT_ALIGNMENT); box3.add(title3); box3.add(Box.createVerticalStrut(15));
        box3.add(createLogRow("Bạn đã thêm gia sư mới", "10 phút trước")); box3.add(Box.createVerticalStrut(10));
        box3.add(createLogRow("Bạn đã duyệt lớp L-1002", "1 giờ trước")); box3.add(Box.createVerticalStrut(10));
        box3.add(createLogRow("Đăng nhập thành công", "Sáng nay 08:00"));

        p.add(box1); p.add(Box.createVerticalStrut(20)); p.add(box2); p.add(Box.createVerticalStrut(20)); p.add(box3); return p;
    }

    private JPanel createQuickStatRow(String icon, String label, String val) {
        JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false); p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); left.setOpaque(false);
        JLabel lblIcon = new JLabel(); setNetworkIcon(lblIcon, icon, 18, 18);
        JLabel lblLab = new JLabel(label); lblLab.setFont(new Font("Segoe UI", Font.PLAIN, 13)); lblLab.setForeground(TEXT_MUTED);
        left.add(lblIcon); left.add(lblLab);
        JLabel lblVal = new JLabel(val); lblVal.setFont(new Font("Segoe UI", Font.BOLD, 13)); lblVal.setForeground(TEXT_MAIN);
        p.add(left, BorderLayout.WEST); p.add(lblVal, BorderLayout.EAST); return p;
    }
    
    private JPanel createLogRow(String action, String time) {
        JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false); p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblAction = new JLabel("• " + action); lblAction.setFont(new Font("Segoe UI", Font.PLAIN, 13)); lblAction.setForeground(TEXT_MAIN);
        JLabel lblTime = new JLabel(time); lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblTime.setForeground(TEXT_MUTED);
        p.add(lblAction, BorderLayout.WEST); p.add(lblTime, BorderLayout.EAST); return p;
    }

    class RoundedPanel extends JPanel {
        private int radius; public RoundedPanel(int radius) { this.radius = radius; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CARD_BG); g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.setColor(BORDER_COLOR); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, radius, radius);
            g2.dispose(); super.paintComponent(g);
        }
    }

    private void setNetworkIcon(JLabel label, String urlStr, int width, int height) {
        new Thread(() -> { try { ImageIcon raw = new ImageIcon(new URL(urlStr)); Image img = raw.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH); SwingUtilities.invokeLater(() -> label.setIcon(new ImageIcon(img))); } catch (Exception ignored) {} }).start();
    }
    
    // ==========================================================
    // TIỆN ÍCH CHUYỂN ẢNH THÀNH HÌNH TRÒN BO GÓC CHUẨN
    // ==========================================================
    private ImageIcon getCircularImageIcon(Image rawImage, int size) {
        if (rawImage == null) return null;
        BufferedImage circleBuffer = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = circleBuffer.createGraphics();
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        g2.fillOval(0, 0, size, size); // Vẽ hình tròn làm mặt nạ
        g2.setComposite(AlphaComposite.SrcIn); // Chỉ vẽ ảnh bên trong hình tròn
        
        int imgW = rawImage.getWidth(null); int imgH = rawImage.getHeight(null);
        if (imgW > 0 && imgH > 0) {
            // Scale ảnh phủ kín (object-fit: cover)
            double scale = Math.max((double) size / imgW, (double) size / imgH);
            int drawW = (int) (imgW * scale); int drawH = (int) (imgH * scale);
            g2.drawImage(rawImage, (size - drawW) / 2, (size - drawH) / 2, drawW, drawH, null);
        }
        g2.dispose(); return new ImageIcon(circleBuffer);
    }
    
    private void setAvatarNetworkIcon(JLabel label, String urlStr, int size) {
        new Thread(() -> { 
            try { 
                ImageIcon raw = new ImageIcon(new URL(urlStr)); 
                SwingUtilities.invokeLater(() -> {
                    // Chỉ nạp ảnh mạng mặc định nếu chưa có ảnh từ Server
                    if (!hasCustomAvatar) label.setIcon(getCircularImageIcon(raw.getImage(), size));
                }); 
            } catch (Exception ignored) {} 
        }).start();
    }
    
    // ==========================================================
    // HÀM PUBLIC: HỨNG ẢNH BASE64 TỪ SERVER VÀ NẠP VÀO UI
    // ==========================================================
    public void updateAvatarFromBase64(String base64Image) {
        this.hasCustomAvatar = true; // Chặn load ảnh mặc định
        SwingUtilities.invokeLater(() -> {
            try {
                // Giải mã Base64 thành mảng byte
                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                Image rawImg = new ImageIcon(imageBytes).getImage();
                
                // Cắt tròn và nạp vào 2 label bên trong tab
                if (bigAvatarLabel != null) bigAvatarLabel.setIcon(getCircularImageIcon(rawImg, 130));
                if (miniAvatarLabel != null) miniAvatarLabel.setIcon(getCircularImageIcon(rawImg, 64));
            } catch (Exception e) {
                System.err.println("Lỗi giải mã ảnh Base64: " + e.getMessage());
            }
        });
    }
}