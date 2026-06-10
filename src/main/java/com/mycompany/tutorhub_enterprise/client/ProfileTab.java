package com.mycompany.tutorhub_enterprise.client;

import com.mycompany.tutorhub_enterprise.models.Packet;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

public class ProfileTab extends JPanel {

    private final Color BG_MAIN = Color.decode("#F4F7FA"); // Nền xám xanh cực nhạt
    private final Color TEXT_MAIN = Color.decode("#0F172A");
    private final Color TEXT_MUTED = Color.decode("#64748B");
   // --- Cập nhật mã màu sang tông Indigo sáng (nhạt hơn) ---
    private final Color PRIMARY = Color.decode("#6366F1"); // Màu Indigo sáng (cho chữ nút và viền)
    private final Color BG_PRIMARY_LIGHT = Color.decode("#EEF2FF"); // Màu Indigo cực nhạt (nền nút, header bảng)
    private final Color CARD_BG = Color.WHITE;
    private final Color BORDER_COLOR = Color.decode("#E2E8F0");
    private final Color DISABLED_BG = Color.decode("#F8FAFC"); 
    // Thêm biến này lên đầu class để lưu tạm đường dẫn tải file
    public static java.io.File pendingDownloadFile = null;
    private JLabel bigAvatarLabel, miniAvatarLabel;
    private byte[] pendingAvatarBytes = null;
    private Image pendingRawImage = null;
    private boolean hasCustomAvatar = false; 
    private JPanel timelineListPanel;
    private DefaultTableModel degTableModel, certTableModel, expTableModel; 
    
    private JTextField txtName, txtDob, txtPhone, txtEmail, txtAddress, txtSubject;
    private JComboBox<String> cbGender, cbLocation;
    private JTextArea txtBio;
    
    private JButton btnEditProfile, btnSaveProfile, btnVerifyPhone;
    private boolean isEditingProfile = false;
    private boolean isPhoneVerified = false;
    private String pendingPhoneVerification = "";
    private JLabel lblPhoneVerificationStatus;
    private JLabel lblPhoneVerificationHelp;
    private PhoneOtpDialog phoneOtpDialog;

    private byte[] cvFileBytes = null;
    private String cvFileNameStr = "";
    private JLabel lblCvPreview;
    
    private byte[] ekycFrontBytes = null, ekycBackBytes = null;
    private JLabel lblFrontName, lblBackName, lblEkycFrontPreview, lblEkycBackPreview;

    private JLabel lblLeftName = new JLabel("");
    private JLabel lblLeftRole = new JLabel("");
    private JLabel lblLeftId = new JLabel("");
    private JLabel lblLeftLocation = new JLabel("");

    private CardLayout centerCardLayout;
    private JPanel centerCardPanel;
    private JPanel[] tabButtons;
    
   
    public static boolean isPreviewingFile = false; // Thêm cờ đánh dấu đang muốn xem file
    private String currentServerCvFileName = ""; // Lưu tên CV đang có trên server

    public interface AvatarUpdateListener { void onAvatarUpdated(Image newAvatar); }
    private AvatarUpdateListener avatarListener;
    public void setAvatarUpdateListener(AvatarUpdateListener listener) { this.avatarListener = listener; }

   public ProfileTab() {
        setLayout(new BorderLayout()); 
        setBackground(BG_MAIN);
        
        JPanel topArea = new JPanel(new BorderLayout()); 
        topArea.setOpaque(false);
        topArea.add(createHeader(), BorderLayout.NORTH); 
        topArea.add(createTabs(), BorderLayout.SOUTH);
        add(topArea, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(20, 0)); 
        contentPanel.setOpaque(false); 
        // Ép nhỏ padding tổng thể lại để vừa màn hình
        contentPanel.setBorder(new EmptyBorder(10, 20, 15, 20)); 
        contentPanel.add(createLeftColumn(), BorderLayout.WEST);
        
        centerCardLayout = new CardLayout(); 
        centerCardPanel = new JPanel(centerCardLayout); 
        centerCardPanel.setOpaque(false);
        setupCenterTabs(); 
        contentPanel.add(centerCardPanel, BorderLayout.CENTER);
        
        // ADD TRỰC TIẾP CONTENT PANEL (BỎ JSCROLLPANE)
        add(contentPanel, BorderLayout.CENTER);
        
        switchTab(0);
        setEditMode(false);
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
                g2.setColor(Color.WHITE); 
                g2.fillOval(0, 0, getWidth(), getHeight()); 
                g2.setColor(BORDER_COLOR); 
                g2.drawOval(0, 0, getWidth()-1, getHeight()-1); 
                g2.dispose();
            }
        };
        iconWrapper.setOpaque(false); 
        iconWrapper.setPreferredSize(new Dimension(40, 40));
        JLabel lblIcon = new JLabel(); 
        setNetworkIcon(lblIcon, "https://img.icons8.com/fluency-systems-regular/48/64748B/user.png", 20, 20);
        iconWrapper.add(lblIcon, BorderLayout.CENTER);

        JPanel titlePanel = new JPanel(); 
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS)); 
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Hồ sơ cá nhân"); 
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18)); 
        lblTitle.setForeground(TEXT_MAIN);
        JLabel lblBreadcrumb = new JLabel("Trang chủ  >  Hồ sơ cá nhân"); 
        lblBreadcrumb.setFont(new Font("Segoe UI", Font.PLAIN, 12)); 
        lblBreadcrumb.setForeground(TEXT_MUTED);
        titlePanel.add(lblTitle); 
        titlePanel.add(Box.createVerticalStrut(2)); 
        titlePanel.add(lblBreadcrumb);
        
        leftTitle.add(iconWrapper); 
        leftTitle.add(titlePanel);
        
        JButton btnUpdateGlobal = new JButton("Cập nhật hồ sơ") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12); 
                super.paintComponent(g); 
                g2.dispose();
            }
        };
        btnUpdateGlobal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnUpdateGlobal.setBackground(BG_PRIMARY_LIGHT); 
        btnUpdateGlobal.setForeground(PRIMARY);
        btnUpdateGlobal.setContentAreaFilled(false); 
        btnUpdateGlobal.setBorder(new EmptyBorder(8, 20, 8, 20));
        btnUpdateGlobal.setFocusPainted(false);
        btnUpdateGlobal.setCursor(new Cursor(Cursor.HAND_CURSOR));
        setNetworkIcon(btnUpdateGlobal, "https://img.icons8.com/fluency-systems-regular/48/6366F1/save.png", 16, 16);
        btnUpdateGlobal.setIconTextGap(8);
        
        btnUpdateGlobal.addActionListener(e -> {
            btnUpdateGlobal.setText("Đang lưu...");
            btnUpdateGlobal.setEnabled(false);
            new Thread(() -> {
                try {
                    // 1. LƯU THÔNG TIN CÁ NHÂN (TAB 1)
                    String bioSafe = txtBio.getText().replace("\n", "\\n");
                    String profilePayload = txtName.getText() + ";;" + txtDob.getText() + ";;" + cbGender.getSelectedItem() + ";;" 
                                          + txtPhone.getText() + ";;" + txtAddress.getText() + ";;" + cbLocation.getSelectedItem() + ";;" 
                                          + txtSubject.getText() + ";;" + bioSafe;
                    NetworkManager.getInstance().sendPacket(new Packet("UPDATE_PROFILE", profilePayload));

                    // 2. LƯU CV (TAB 4) - Chỉ lưu nếu người dùng có chọn file CV trên giao diện
                    if (cvFileBytes != null && !cvFileNameStr.isEmpty()) {
                        String b64CV = Base64.getEncoder().encodeToString(cvFileBytes);
                        NetworkManager.getInstance().sendPacket(new Packet("UPDATE_CV", cvFileNameStr + "|" + b64CV));
                    }

                    // 3. LƯU eKYC (TAB 6) - Cập nhật lại ảnh nếu có thay đổi
                    if (ekycFrontBytes != null && ekycBackBytes != null) {
                        String fB64 = Base64.getEncoder().encodeToString(ekycFrontBytes);
                        String bB64 = Base64.getEncoder().encodeToString(ekycBackBytes);
                        NetworkManager.getInstance().sendPacket(new Packet("UPDATE_EKYC", fB64 + "|||" + bB64));
                    }

                    // Chờ Server xử lý DB và Ghi file ra ổ cứng
                    Thread.sleep(1000); 
                    
                    // 4. YÊU CẦU ĐỒNG BỘ LẠI ĐỂ CHỐT DỮ LIỆU
                    NetworkManager.getInstance().sendPacket(new Packet("GET_FULL_PROFILE", ""));

                    SwingUtilities.invokeLater(() -> {
                        btnUpdateGlobal.setText("Cập nhật hồ sơ");
                        btnUpdateGlobal.setEnabled(true);
                        setEditMode(false);
                        
                        // Cập nhật giao diện thanh Sidebar trái
                        lblLeftName.setText(txtName.getText()); 
                        lblLeftRole.setText("Gia sư " + txtSubject.getText()); 
                        lblLeftLocation.setText(cbLocation.getSelectedItem().toString());
                        
                        JOptionPane.showMessageDialog(this, "Đã lưu toàn bộ thông tin Hồ sơ, CV và eKYC thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    });
                } catch(Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        btnUpdateGlobal.setText("Cập nhật hồ sơ");
                        btnUpdateGlobal.setEnabled(true);
                        JOptionPane.showMessageDialog(this, "Lỗi kết nối khi lưu: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        });
        
        header.add(leftTitle, BorderLayout.WEST);
        header.add(btnUpdateGlobal, BorderLayout.EAST);
        
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

    private void setEditMode(boolean edit) {
        this.isEditingProfile = edit;
        Color bgColor = edit ? Color.WHITE : DISABLED_BG;
        Color fgColor = edit ? TEXT_MAIN : TEXT_MUTED;

        JComponent[] inputs = {txtName, txtDob, txtPhone, txtAddress, txtSubject};
        for (JComponent comp : inputs) {
            JTextField txt = (JTextField) comp;
            txt.setEditable(edit);
            txt.setForeground(fgColor);
            txt.setBackground(bgColor);
            Container parent = txt.getParent();
            if (parent != null) parent.setBackground(bgColor);
        }

        cbGender.setEnabled(edit); 
        if(cbGender.getParent() != null) cbGender.getParent().setBackground(bgColor);
        
        cbLocation.setEnabled(edit); 
        if(cbLocation.getParent() != null) cbLocation.getParent().setBackground(bgColor);

        txtBio.setEditable(edit);
        txtBio.setBackground(bgColor);
        txtBio.setForeground(fgColor);
        if(txtBio.getParent() != null && txtBio.getParent().getParent() != null) {
            txtBio.getParent().setBackground(bgColor);
            txtBio.getParent().getParent().setBackground(bgColor);
        }

        btnSaveProfile.setEnabled(edit);
        btnSaveProfile.setBackground(edit ? PRIMARY : Color.decode("#93C5FD")); 
        if (btnVerifyPhone != null) {
            btnVerifyPhone.setEnabled(!edit && !isPhoneVerified);
            btnVerifyPhone.setText(edit ? "Lưu trước" : (isPhoneVerified ? "Đã xác minh" : "Xác minh"));
        }

        if (edit) {
            btnEditProfile.setText("Hủy chỉnh sửa");
            btnEditProfile.setForeground(Color.decode("#DC2626")); // Màu đỏ khi đang edit
            setNetworkIcon(btnEditProfile, "https://img.icons8.com/fluency-systems-regular/48/DC2626/cancel.png", 14, 14);
        } else {
            btnEditProfile.setText("Chỉnh sửa");
            btnEditProfile.setForeground(PRIMARY);
            setNetworkIcon(btnEditProfile, "https://img.icons8.com/fluency-systems-regular/48/2563EB/edit.png", 14, 14);
        }
        repaint();
    }

    // --- CỘT TRÁI (PROFILE META) ---
   // --- CỘT TRÁI (PROFILE META) ---
    // --- CỘT TRÁI (PROFILE META) ---
   // --- CỘT TRÁI (PROFILE META) ---
    // --- CỘT TRÁI (PROFILE META) ---
    // --- CỘT TRÁI (PROFILE META) ---
   // --- CỘT TRÁI (PROFILE META) ---
    // --- CỘT TRÁI (PROFILE META) ---
    private JPanel createLeftColumn() {
        RoundedPanel p = new RoundedPanel(12); 
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); 
        p.setPreferredSize(new Dimension(270, 0)); 
        p.setBorder(new EmptyBorder(25, 20, 20, 20)); 

        JPanel avaPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        avaPanel.setOpaque(false);
        bigAvatarLabel = new JLabel(); 
        setAvatarNetworkIcon(bigAvatarLabel, "https://img.icons8.com/color/150/circled-user-male-skin-type-4--v1.png", 100); 
        avaPanel.add(bigAvatarLabel);

        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0)); 
        nameRow.setOpaque(false);
        lblLeftName.setFont(new Font("Segoe UI", Font.BOLD, 17)); 
        lblLeftName.setForeground(TEXT_MAIN);
        JLabel lblCheck = new JLabel(); 
        setNetworkIcon(lblCheck, "https://img.icons8.com/color/48/verified-badge.png", 18, 18);
        nameRow.add(lblLeftName); nameRow.add(lblCheck);

        JPanel roleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        roleRow.setOpaque(false);
        lblLeftRole.setFont(new Font("Segoe UI", Font.PLAIN, 13)); 
        lblLeftRole.setForeground(TEXT_MUTED); 
        roleRow.add(lblLeftRole);

        p.add(avaPanel); 
        p.add(Box.createVerticalStrut(15)); 
        p.add(nameRow); 
        p.add(Box.createVerticalStrut(4)); 
        p.add(roleRow); 
        p.add(Box.createVerticalStrut(20)); 
        
        JPanel separator = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Color.decode("#F1F5F9")); 
                g.drawLine(0, getHeight()/2, getWidth(), getHeight()/2);
            }
        };
        separator.setOpaque(false); separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        p.add(separator);
        p.add(Box.createVerticalStrut(20));

        // DANH SÁCH THÔNG TIN
        JPanel infoList = new JPanel(); 
        infoList.setLayout(new BoxLayout(infoList, BoxLayout.Y_AXIS)); 
        infoList.setOpaque(false); 
        
        CardLayout clId = new CardLayout(); JPanel cardId = new JPanel(); JTextField txtId = new JTextField(); 
        CardLayout clDate = new CardLayout(); JPanel cardDate = new JPanel(); JLabel lblJoinDate = new JLabel("Gần đây"); JTextField txtDate = new JTextField(); 
        CardLayout clLoc = new CardLayout(); JPanel cardLoc = new JPanel(); JComboBox<String> cbLoc = new JComboBox<>(new String[]{"Đà Nẵng", "Hà Nội", "Hồ Chí Minh", "Toàn quốc"});
        CardLayout clStatus = new CardLayout(); JPanel cardStatus = new JPanel(); 
        JLabel lblLeftStatus = new JLabel("Đang hoạt động"); lblLeftStatus.setForeground(Color.decode("#10B981"));
        setNetworkIcon(lblLeftStatus, "https://img.icons8.com/color/48/ok--v1.png", 14, 14);
        JComboBox<String> cbStatus = new JComboBox<>(new String[]{"Đang hoạt động", "Tạm nghỉ", "Khóa"});

        infoList.add(createEditableRow("https://img.icons8.com/fluency-systems-regular/48/64748B/identity-theft.png", "Mã gia sư", lblLeftId, txtId, clId, cardId));
        infoList.add(Box.createVerticalStrut(18)); 
        infoList.add(createEditableRow("https://img.icons8.com/fluency-systems-regular/48/64748B/calendar--v1.png", "Ngày tham gia", lblJoinDate, txtDate, clDate, cardDate));
        infoList.add(Box.createVerticalStrut(18));
        infoList.add(createEditableRow("https://img.icons8.com/fluency-systems-regular/48/64748B/marker.png", "Khu vực", lblLeftLocation, cbLoc, clLoc, cardLoc));
        infoList.add(Box.createVerticalStrut(18));
        infoList.add(createEditableRow("https://img.icons8.com/fluency-systems-regular/48/64748B/checked-user-male.png", "Trạng thái", lblLeftStatus, cbStatus, clStatus, cardStatus));
        
        p.add(infoList); 
        p.add(Box.createVerticalStrut(25)); 
        
        final boolean[] isEditingLeft = {false};
        
        // --- NÚT CHỈNH SỬA THU NHỎ LẠI ---
       // --- NÚT CHỈNH SỬA THÔNG TIN (Đã sửa lỗi khuyết nét) ---
        JButton btnEditLeft = new JButton("Chỉnh sửa thông tin") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Thu vào 1px để không bị lẹm viền
                g2.setColor(getBackground());
                g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8); 
                
                // Vẽ viền
                g2.setColor(isEditingLeft[0] ? Color.decode("#10B981") : PRIMARY);
                g2.setStroke(new BasicStroke(1.2f)); // Cho viền rõ nét hơn một chút
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
                
                super.paintComponent(g); // Gọi super sau khi vẽ nền để chữ nổi lên trên
                g2.dispose();
            }
        };
        btnEditLeft.setContentAreaFilled(false);
        btnEditLeft.setBorderPainted(false); // Bắt buộc tắt viền mặc định
        btnEditLeft.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnEditLeft.setForeground(PRIMARY);
        btnEditLeft.setBackground(BG_PRIMARY_LIGHT); // Màu nền nhạt
        btnEditLeft.setBorder(new EmptyBorder(4, 12, 4, 12)); 
        setNetworkIcon(btnEditLeft, "https://img.icons8.com/fluency-systems-regular/48/6366F1/edit.png", 14, 14); 
        btnEditLeft.setIconTextGap(6);
        btnEditLeft.setFocusPainted(false);
        btnEditLeft.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnEditLeft.setMaximumSize(new Dimension(170, 32)); 
        btnEditLeft.setPreferredSize(new Dimension(170, 32));
        btnEditLeft.setAlignmentX(Component.CENTER_ALIGNMENT);
       
        
        btnEditLeft.addActionListener(e -> {
            isEditingLeft[0] = !isEditingLeft[0];
            if (isEditingLeft[0]) {
                btnEditLeft.setText("Lưu thông tin");
                btnEditLeft.setForeground(Color.decode("#10B981")); 
                setNetworkIcon(btnEditLeft, "https://img.icons8.com/fluency-systems-regular/48/10B981/save.png", 14, 14);
                
                txtId.setText(lblLeftId.getText());
                txtDate.setText(lblJoinDate.getText());
                cbLoc.setSelectedItem(lblLeftLocation.getText());
                cbStatus.setSelectedItem(lblLeftStatus.getText());
                
                clId.show(cardId, "EDIT"); clDate.show(cardDate, "EDIT");
                clLoc.show(cardLoc, "EDIT"); clStatus.show(cardStatus, "EDIT");
            } else {
                btnEditLeft.setText("Chỉnh sửa thông tin");
                btnEditLeft.setForeground(PRIMARY); 
                setNetworkIcon(btnEditLeft, "https://img.icons8.com/fluency-systems-regular/48/6366F1/edit.png", 14, 14);
                
                lblLeftId.setText(txtId.getText());
                lblJoinDate.setText(txtDate.getText());
                lblLeftLocation.setText(cbLoc.getSelectedItem().toString());
                
                String newStatus = cbStatus.getSelectedItem().toString();
                lblLeftStatus.setText(newStatus);
                lblLeftStatus.setForeground(newStatus.equals("Đang hoạt động") ? Color.decode("#10B981") : Color.decode("#DC2626"));
                setNetworkIcon(lblLeftStatus, newStatus.equals("Đang hoạt động") ? "https://img.icons8.com/color/48/ok--v1.png" : "https://img.icons8.com/color/48/cancel--v1.png", 14, 14);
                
                clId.show(cardId, "VIEW"); clDate.show(cardDate, "VIEW");
                clLoc.show(cardLoc, "VIEW"); clStatus.show(cardStatus, "VIEW");
            }
            p.repaint(); 
        });
        
        p.add(btnEditLeft);
        p.add(Box.createVerticalGlue()); 
        return p;
    }
    // --- HÀM HỖ TRỢ TẠO DÒNG KHÔNG BỊ RỚT CHỮ (ÉP NẰM NGANG) ---
   // --- HÀM HỖ TRỢ TẠO DÒNG CĂN ĐỀU 2 BÊN TẬN CÙNG ---
    private JPanel createEditableRow(String iconUrl, String labelTxt, JLabel displayLbl, JComponent editor, CardLayout cl, JPanel card) {
        // Dùng BorderLayout để ép 2 thành phần sát lề trái và phải
        JPanel p = new JPanel(new BorderLayout()); 
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30)); // Giới hạn chiều cao để không bị giãn
        
        // Bên trái (Icon + Text mờ)
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JLabel icon = new JLabel(); setNetworkIcon(icon, iconUrl, 16, 16);
        JLabel lbl = new JLabel(labelTxt); 
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13)); 
        lbl.setForeground(TEXT_MUTED);
        left.add(icon); left.add(lbl);
        
        // Bên phải (Label in đậm HOẶC Input)
        displayLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        displayLbl.setHorizontalAlignment(SwingConstants.RIGHT); // Ép text căn phải
        if (displayLbl.getForeground().equals(UIManager.getColor("Label.foreground"))) {
             displayLbl.setForeground(TEXT_MAIN);
        }
        
        editor.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        if(editor instanceof JTextField) {
            ((JTextField)editor).setHorizontalAlignment(JTextField.RIGHT);
            ((JTextField)editor).setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, PRIMARY));
            editor.setPreferredSize(new Dimension(100, 24));
        } else if (editor instanceof JComboBox) {
            editor.setPreferredSize(new Dimension(120, 24));
        }

        // Add 2 trạng thái vào thẻ Card
        card.setLayout(cl);
        card.setOpaque(false);
        card.add(displayLbl, "VIEW");
        card.add(editor, "EDIT");
        cl.show(card, "VIEW"); 
        
        // Add vào 2 bên
        p.add(left, BorderLayout.WEST);
        p.add(card, BorderLayout.EAST);
        
        return p;
    }
    private JPanel createDivider() {
        JPanel sep = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(BORDER_COLOR); 
                g.drawLine(0, 0, getWidth(), 0); 
            }
        };
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setOpaque(false);
        return sep;
    }

    private JPanel createProfileMetaRow(String iconUrl, String label, JLabel valLabel) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.setOpaque(false);
        JLabel lblIcon = new JLabel();
        setNetworkIcon(lblIcon, iconUrl, 16, 16); 
        JLabel lblLab = new JLabel(label);
        lblLab.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblLab.setForeground(TEXT_MUTED);
        leftPanel.add(lblIcon); leftPanel.add(lblLab);
        
        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 13)); 
        if (valLabel.getForeground().equals(UIManager.getColor("Label.foreground"))) {
             valLabel.setForeground(TEXT_MAIN);
        }
        
        p.add(leftPanel, BorderLayout.WEST);
        p.add(valLabel, BorderLayout.EAST); 
        return p;
    }

    // --- CENTER COLUMN (FORM) ---
    private JPanel createPersonalInfoForm() {
        RoundedPanel p = new RoundedPanel(12); 
        p.setLayout(new BorderLayout(0, 5)); 
        p.setBorder(new EmptyBorder(20, 25, 20, 25)); 

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Thông tin cá nhân"); 
        title.setFont(new Font("Segoe UI", Font.BOLD, 18)); 
        title.setForeground(TEXT_MAIN);
        
        btnEditProfile = new JButton("Chỉnh sửa");
        btnEditProfile.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnEditProfile.setForeground(PRIMARY);
        btnEditProfile.setBackground(Color.WHITE);
        btnEditProfile.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            new EmptyBorder(6, 12, 6, 12) 
        ));
        btnEditProfile.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEditProfile.setFocusPainted(false);
        btnEditProfile.addActionListener(e -> setEditMode(!isEditingProfile));
        
        header.add(title, BorderLayout.WEST);
        header.add(btnEditProfile, BorderLayout.EAST);
        p.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(15, 0, 0, 0));

        JPanel grid = new JPanel(new GridLayout(4, 2, 20, 15)); 
        grid.setOpaque(false); 
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtName = new JTextField(""); txtDob = new JTextField("");
        txtPhone = new JTextField(""); InputFilters.installPhoneFilter(txtPhone);
        txtEmail = new JTextField(""); txtEmail.setEditable(false); 
        cbGender = new JComboBox<>(new String[]{"Nam", "Nữ", "Khác"});
        txtAddress = new JTextField("");
        cbLocation = new JComboBox<>(new String[]{"Hà Nội", "Hồ Chí Minh", "Đà Nẵng", "Toàn quốc"});
        txtSubject = new JTextField("");

        grid.add(createIconInputGroup("Họ và tên", null, txtName)); 
        grid.add(createIconInputGroup("Ngày sinh", "https://img.icons8.com/fluency-systems-regular/48/64748B/calendar--v1.png", txtDob));
        grid.add(createIconInputGroup("Số điện thoại", "https://img.icons8.com/fluency-systems-regular/48/64748B/phone.png", txtPhone)); 
        grid.add(createIconInputGroup("Email", "https://img.icons8.com/fluency-systems-regular/48/64748B/mail.png", txtEmail));
        grid.add(createIconInputGroup("Giới tính", "https://img.icons8.com/fluency-systems-regular/48/64748B/user.png", cbGender)); 
        grid.add(createIconInputGroup("Địa chỉ", "https://img.icons8.com/fluency-systems-regular/48/64748B/marker.png", txtAddress));
        grid.add(createIconInputGroup("Khu vực dạy", "https://img.icons8.com/fluency-systems-regular/48/64748B/map.png", cbLocation)); 
        grid.add(createIconInputGroup("Môn dạy chính", "https://img.icons8.com/fluency-systems-regular/48/64748B/monitor.png", txtSubject));
        
        body.add(grid);
        body.add(Box.createVerticalStrut(15));
        body.add(createPhoneVerificationPanel());
        body.add(Box.createVerticalStrut(15));

        JLabel lblBio = new JLabel("Giới thiệu bản thân"); 
        lblBio.setFont(new Font("Segoe UI", Font.BOLD, 13)); 
        lblBio.setForeground(TEXT_MAIN); 
        lblBio.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel bioWrap = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                if (isEditingProfile) g2.setColor(PRIMARY); else g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
            }
        };
        bioWrap.setOpaque(false);
        bioWrap.setBorder(new EmptyBorder(8, 12, 8, 12));
        bioWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        bioWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65)); 

        txtBio = new JTextArea(""); 
        txtBio.setRows(2); 
        txtBio.setFont(new Font("Segoe UI", Font.PLAIN, 13)); 
        txtBio.setLineWrap(true); txtBio.setWrapStyleWord(true); 
        txtBio.setBorder(null);
        txtBio.setOpaque(false);
        
        JLabel lblCharCount = new JLabel("15/500");
        lblCharCount.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblCharCount.setForeground(TEXT_MUTED);
        lblCharCount.setHorizontalAlignment(SwingConstants.RIGHT);
        
        JScrollPane bioScroll = new JScrollPane(txtBio, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        bioScroll.setBorder(null); bioScroll.setOpaque(false); bioScroll.getViewport().setOpaque(false);
        
        bioWrap.add(bioScroll, BorderLayout.CENTER);
        bioWrap.add(lblCharCount, BorderLayout.SOUTH);

        body.add(lblBio); 
        body.add(Box.createVerticalStrut(6)); 
        body.add(bioWrap); 
        body.add(Box.createVerticalStrut(20)); 

        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); 
        btnWrap.setOpaque(false); 
        btnWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        btnSaveProfile = new JButton("Lưu thay đổi") { 
            @Override protected void paintComponent(Graphics g) { 
                Graphics2D g2 = (Graphics2D) g.create(); 
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
                g2.setColor(getBackground()); 
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8); 
                super.paintComponent(g); 
                g2.dispose(); 
            } 
        };
        btnSaveProfile.setFont(new Font("Segoe UI", Font.BOLD, 13)); 
        btnSaveProfile.setForeground(Color.WHITE); btnSaveProfile.setContentAreaFilled(false); 
        btnSaveProfile.setBorderPainted(false); 
        btnSaveProfile.setPreferredSize(new Dimension(150, 40)); 
        btnSaveProfile.setCursor(new Cursor(Cursor.HAND_CURSOR));
        setNetworkIcon(btnSaveProfile, "https://img.icons8.com/fluency-systems-regular/48/FFFFFF/checkmark.png", 16, 16);
        btnSaveProfile.setIconTextGap(8);
        
        btnSaveProfile.addActionListener(e -> {
            if(!isEditingProfile) return;
            btnSaveProfile.setText("Đang lưu...");
            new Thread(() -> {
                try {
                    String bioSafe = txtBio.getText().replace("\n", "\\n");
                    String payload = txtName.getText() + ";;" + txtDob.getText() + ";;" + cbGender.getSelectedItem() + ";;" 
                                   + txtPhone.getText() + ";;" + txtAddress.getText() + ";;" + cbLocation.getSelectedItem() + ";;" 
                                   + txtSubject.getText() + ";;" + bioSafe;
                    NetworkManager.getInstance().sendPacket(new Packet("UPDATE_PROFILE", payload));
                    NetworkManager.getInstance().sendPacket(new Packet("GET_FULL_PROFILE", ""));
                    SwingUtilities.invokeLater(() -> {
                        btnSaveProfile.setText("Lưu thay đổi");
                        lblLeftName.setText(txtName.getText()); lblLeftRole.setText("Gia sư " + txtSubject.getText()); lblLeftLocation.setText(cbLocation.getSelectedItem().toString());
                        JOptionPane.showMessageDialog(this, "Đã lưu thông hồ sơ thành công!");
                        setEditMode(false);
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> { btnSaveProfile.setText("Lưu thay đổi"); JOptionPane.showMessageDialog(this, "Lỗi kết nối!", "Lỗi", JOptionPane.ERROR_MESSAGE); });
                }
            }).start();
        });
        
        btnWrap.add(btnSaveProfile); body.add(btnWrap); 
        p.add(body, BorderLayout.CENTER); 
        return p;
    }

    private JPanel createPhoneVerificationPanel() {
        JPanel p = new JPanel(new BorderLayout(12, 4));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));

        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);

        JPanel statusWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        statusWrap.setOpaque(false);

        JLabel lbl = new JLabel("Xác minh số điện thoại");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(TEXT_MAIN);

        lblPhoneVerificationStatus = new JLabel("Chưa xác minh");
        lblPhoneVerificationStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPhoneVerificationStatus.setOpaque(true);
        lblPhoneVerificationStatus.setBorder(new EmptyBorder(6, 12, 6, 12));

        statusWrap.add(lbl);
        statusWrap.add(lblPhoneVerificationStatus);

        btnVerifyPhone = new JButton("Xác minh");
        btnVerifyPhone.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnVerifyPhone.setForeground(PRIMARY);
        btnVerifyPhone.setBackground(BG_PRIMARY_LIGHT);
        btnVerifyPhone.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(7, 14, 7, 14)
        ));
        btnVerifyPhone.setFocusPainted(false);
        btnVerifyPhone.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVerifyPhone.addActionListener(e -> requestPhoneVerification());

        top.add(statusWrap, BorderLayout.WEST);
        top.add(btnVerifyPhone, BorderLayout.EAST);

        lblPhoneVerificationHelp = new JLabel("Lưu số điện thoại trước, sau đó xác minh để dùng đăng nhập SMS.");
        lblPhoneVerificationHelp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPhoneVerificationHelp.setForeground(TEXT_MUTED);

        p.add(top, BorderLayout.NORTH);
        p.add(lblPhoneVerificationHelp, BorderLayout.SOUTH);
        updatePhoneVerificationState(false);
        return p;
    }
   
    private JPanel createIconInputGroup(String labelStr, String iconUrl, JComponent inputComp) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel lbl = new JLabel(labelStr);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13)); 
        lbl.setForeground(TEXT_MAIN);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel inputWrap = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                
                if (isEditingProfile && (inputComp.hasFocus() || (inputComp instanceof JComboBox && ((JComboBox)inputComp).isPopupVisible()))) {
                    g2.setColor(PRIMARY); 
                } else {
                    g2.setColor(BORDER_COLOR);
                }
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
            }
        };
        inputWrap.setOpaque(false);
        inputWrap.setBorder(new EmptyBorder(0, 12, 0, 12));
        inputWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); 
        inputWrap.setPreferredSize(new Dimension(0, 40));
        inputWrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (iconUrl != null && !iconUrl.isEmpty()) {
            JLabel iconLbl = new JLabel();
            setNetworkIcon(iconLbl, iconUrl, 16, 16);
            inputWrap.add(iconLbl, BorderLayout.WEST);
        }

        if (inputComp instanceof JTextField) {
            JTextField txt = (JTextField) inputComp;
            txt.setBorder(null);
            txt.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            txt.setOpaque(false);
            txt.addFocusListener(new java.awt.event.FocusAdapter() {
                public void focusGained(java.awt.event.FocusEvent evt) { inputWrap.repaint(); }
                public void focusLost(java.awt.event.FocusEvent evt) { inputWrap.repaint(); }
            });
        } else if (inputComp instanceof JComboBox) {
            JComboBox cb = (JComboBox) inputComp;
            cb.setBorder(null);
            cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            cb.setOpaque(false);
            cb.setFocusable(false);
        }
        
        inputWrap.add(inputComp, BorderLayout.CENTER);

        p.add(lbl);
        p.add(Box.createVerticalStrut(6)); 
        p.add(inputWrap);

        return p;
    }

    // --- CỘT PHẢI ---
    private JPanel createRightColumn() {
        JPanel p = new JPanel(); 
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); 
        p.setOpaque(false); 
        p.setPreferredSize(new Dimension(240, 0)); 

        RoundedPanel box1 = new RoundedPanel(12); 
        box1.setLayout(new BorderLayout(10, 0)); 
        box1.setBorder(new EmptyBorder(16, 20, 20, 20)); 
        JLabel title1 = new JLabel("Ảnh đại diện"); 
        title1.setFont(new Font("Segoe UI", Font.BOLD, 15)); 
        box1.add(title1, BorderLayout.NORTH);

        JPanel avaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15)); 
        avaPanel.setOpaque(false);
        miniAvatarLabel = new JLabel(); 
        setAvatarNetworkIcon(miniAvatarLabel, "https://img.icons8.com/color/80/circled-user-male-skin-type-4--v1.png", 64); 
        
        JPanel avaText = new JPanel(); 
        avaText.setLayout(new BoxLayout(avaText, BoxLayout.Y_AXIS)); 
        avaText.setOpaque(false);
        JLabel lblAvaReq = new JLabel("JPG, PNG < 2MB."); 
        lblAvaReq.setFont(new Font("Segoe UI", Font.PLAIN, 10)); 
        lblAvaReq.setForeground(TEXT_MUTED);
        
        JButton btnChangeAva = new JButton("Đổi ảnh"); 
        btnChangeAva.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnChangeAva.setBackground(Color.WHITE); 
        btnChangeAva.setForeground(PRIMARY); 
        btnChangeAva.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            new EmptyBorder(5, 12, 5, 12)
        ));
        setNetworkIcon(btnChangeAva, "https://img.icons8.com/fluency-systems-regular/48/2563EB/upload.png", 14, 14);
        btnChangeAva.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnChangeAva.setFocusPainted(false);
        
        btnChangeAva.addActionListener(e -> { 
            JFileChooser fileChooser = new JFileChooser(); 
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Hình ảnh", "jpg", "png", "jpeg")); 
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { 
                try { 
                    pendingAvatarBytes = java.nio.file.Files.readAllBytes(fileChooser.getSelectedFile().toPath()); 
                    pendingRawImage = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(pendingAvatarBytes)); 
                    hasCustomAvatar = true; 
                    bigAvatarLabel.setIcon(getShadowedCircularImageIcon(pendingRawImage, 115)); 
                    miniAvatarLabel.setIcon(getShadowedCircularImageIcon(pendingRawImage, 64)); 
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Lỗi đọc file ảnh!"); } 
            } 
        });
        
        avaText.add(lblAvaReq); avaText.add(Box.createVerticalStrut(6)); avaText.add(btnChangeAva); 
        avaPanel.add(miniAvatarLabel); avaPanel.add(avaText); 
        box1.add(avaPanel, BorderLayout.CENTER); 
        
        // Card Mẹo hồ sơ & hình bé Bot ở góc
        JPanel box2 = new JPanel(new BorderLayout(8, 6)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); 
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.dispose();
            }
        };
        box2.setOpaque(false);
        box2.setBorder(new EmptyBorder(15, 15, 15, 15)); 
        
        JPanel tipHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tipHeader.setOpaque(false);
        JLabel iconBulb = new JLabel();
        setNetworkIcon(iconBulb, "https://img.icons8.com/color/48/idea.png", 20, 20);
        JLabel lblTipTitle = new JLabel("Mẹo hồ sơ");
        lblTipTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTipTitle.setForeground(PRIMARY);
        tipHeader.add(iconBulb); tipHeader.add(lblTipTitle);
        
        JTextArea txtTip = new JTextArea("Hồ sơ đầy đủ và chính xác sẽ giúp học viên tin tưởng và dễ dàng lựa chọn bạn hơn.");
        txtTip.setWrapStyleWord(true); txtTip.setLineWrap(true);
        txtTip.setOpaque(false); txtTip.setEditable(false);
        txtTip.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtTip.setForeground(TEXT_MAIN);
        txtTip.setBorder(new EmptyBorder(5, 5, 0, 5));
        
        // Robot cute ở góc
        JLabel lblRobot = new JLabel();
        setNetworkIcon(lblRobot, "https://img.icons8.com/color/96/bot.png", 60, 60);
        JPanel robotPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        robotPanel.setOpaque(false);
        robotPanel.add(lblRobot);
        
        JPanel tipBody = new JPanel(new BorderLayout());
        tipBody.setOpaque(false);
        tipBody.add(txtTip, BorderLayout.CENTER);
        tipBody.add(robotPanel, BorderLayout.SOUTH);

        box2.add(tipHeader, BorderLayout.NORTH);
        box2.add(tipBody, BorderLayout.CENTER);

        p.add(box1); 
        p.add(Box.createVerticalStrut(15));
        p.add(box2);
        
        p.add(Box.createVerticalGlue()); 
        
        return p;
    }

    class RoundedPanel extends JPanel {
        private int radius; 
        public RoundedPanel(int radius) { this.radius = radius; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) { 
            Graphics2D g2 = (Graphics2D) g.create(); 
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
            
            // Soft Shadow
            int shadowGap = 2;
            int shadowOffset = 1;
            for (int i = 0; i < shadowGap; i++) {
                g2.setColor(new Color(0, 0, 0, 8));
                g2.fillRoundRect(i, i + shadowOffset, getWidth() - i * 2, getHeight() - i * 2, radius, radius);
            }
            
            g2.setColor(CARD_BG); 
            g2.fillRoundRect(0, 0, getWidth() - shadowGap, getHeight() - shadowGap - shadowOffset, radius, radius); 
            g2.setColor(BORDER_COLOR); 
            g2.setStroke(new BasicStroke(1f)); 
            g2.drawRoundRect(0, 0, getWidth() - shadowGap - 1, getHeight() - shadowGap - shadowOffset - 1, radius, radius); 
            g2.dispose(); 
            super.paintComponent(g); 
        }
    }

    private void setNetworkIcon(JComponent label, String urlStr, int width, int height) {
        new Thread(() -> { 
            try { 
                ImageIcon raw = new ImageIcon(new URL(urlStr)); 
                Image img = raw.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH); 
                SwingUtilities.invokeLater(() -> { 
                    if (label instanceof JLabel) ((JLabel)label).setIcon(new ImageIcon(img)); 
                    else if (label instanceof JButton) ((JButton)label).setIcon(new ImageIcon(img)); 
                }); 
            } catch (Exception ignored) {} 
        }).start(); 
    }
    
    private ImageIcon getShadowedCircularImageIcon(Image rawImage, int size) {
        int padding = 4;
        int totalSize = size + padding * 2;
        BufferedImage buffer = new BufferedImage(totalSize, totalSize, BufferedImage.TYPE_INT_ARGB); 
        Graphics2D g2 = buffer.createGraphics(); 
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
        
        for (int i = 0; i < padding; i++) {
            g2.setColor(new Color(0, 0, 0, 8)); 
            g2.fillOval(padding - i, padding - i + 2, size + i * 2, size + i * 2);
        }
        
        BufferedImage circleImg = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D cg2 = circleImg.createGraphics();
        cg2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        cg2.fillOval(0, 0, size, size);
        cg2.setComposite(AlphaComposite.SrcIn);
        int imgW = rawImage.getWidth(null); int imgH = rawImage.getHeight(null); 
        if (imgW > 0 && imgH > 0) { 
            double scale = Math.max((double) size / imgW, (double) size / imgH); 
            int drawW = (int) (imgW * scale); int drawH = (int) (imgH * scale); 
            cg2.drawImage(rawImage, (size - drawW) / 2, (size - drawH) / 2, drawW, drawH, null); 
        }
        cg2.dispose();
        
        g2.drawImage(circleImg, padding, padding, null);
        g2.dispose(); 
        return new ImageIcon(buffer);
    }
    
    private void setAvatarNetworkIcon(JLabel label, String urlStr, int size) { 
        new Thread(() -> { 
            try { 
                ImageIcon raw = new ImageIcon(new URL(urlStr)); 
                SwingUtilities.invokeLater(() -> { 
                    if (!hasCustomAvatar) label.setIcon(getShadowedCircularImageIcon(raw.getImage(), size)); 
                }); 
            } catch (Exception ignored) {} 
        }).start(); 
    }
    
    public void updateAvatarFromBase64(String base64Image) {
        this.hasCustomAvatar = true; SwingUtilities.invokeLater(() -> { 
            try { 
                byte[] imageBytes = Base64.getDecoder().decode(base64Image); 
                Image rawImg = new ImageIcon(imageBytes).getImage(); 
                if (bigAvatarLabel != null) bigAvatarLabel.setIcon(getShadowedCircularImageIcon(rawImg, 115)); 
                if (miniAvatarLabel != null) miniAvatarLabel.setIcon(getShadowedCircularImageIcon(rawImg, 64)); 
            } catch (Exception e) {} 
        });
    }

    private void updatePhoneVerificationState(boolean verified) {
        isPhoneVerified = verified;
        if (lblPhoneVerificationStatus != null) {
            lblPhoneVerificationStatus.setText(verified ? "Đã xác minh" : "Chưa xác minh");
            lblPhoneVerificationStatus.setForeground(verified ? Color.decode("#047857") : Color.decode("#B45309"));
            lblPhoneVerificationStatus.setBackground(verified ? Color.decode("#D1FAE5") : Color.decode("#FEF3C7"));
        }
        if (lblPhoneVerificationHelp != null) {
            lblPhoneVerificationHelp.setText(verified
                    ? "Số này đã có thể dùng để đăng nhập bằng SMS."
                    : "Lưu số điện thoại trước, sau đó xác minh để dùng đăng nhập SMS.");
            lblPhoneVerificationHelp.setForeground(verified ? Color.decode("#047857") : TEXT_MUTED);
        }
        if (btnVerifyPhone != null) {
            btnVerifyPhone.setEnabled(!verified && !isEditingProfile);
            btnVerifyPhone.setText(isEditingProfile ? "Lưu trước" : (verified ? "Đã xác minh" : "Xác minh"));
        }
    }

    private void requestPhoneVerification() {
        String phone = txtPhone != null ? txtPhone.getText().trim() : "";
        if (!InputFilters.isValidPhone(phone)) {
            showPhoneVerificationHelp("Nhập số điện thoại hợp lệ trước khi xác minh.", Color.decode("#DC2626"));
            txtPhone.requestFocus();
            return;
        }
        if (isEditingProfile) {
            showPhoneVerificationHelp("Lưu hồ sơ trước khi xác minh số điện thoại.", Color.decode("#B45309"));
            return;
        }

        pendingPhoneVerification = phone;
        sendPhoneVerificationOtp();
    }

    private void sendPhoneVerificationOtp() {
        btnVerifyPhone.setEnabled(false);
        btnVerifyPhone.setText("Đang gửi...");
        showPhoneVerificationHelp("Đang gửi mã OTP tới số điện thoại của bạn...", TEXT_MUTED);
        new Thread(() -> {
            try {
                NetworkManager.getInstance().sendPacket(new Packet("REQUEST_VERIFY_PHONE_OTP", pendingPhoneVerification));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    btnVerifyPhone.setEnabled(true);
                    btnVerifyPhone.setText("Xác minh");
                    showPhoneVerificationHelp("Không kết nối được server. Vui lòng thử lại.", Color.decode("#DC2626"));
                    if (phoneOtpDialog != null) {
                        phoneOtpDialog.markSendFailed("Không kết nối được server. Vui lòng thử lại.");
                    }
                });
            }
        }).start();
    }

    public void handlePhoneOtpSent(boolean success, String message) {
        SwingUtilities.invokeLater(() -> {
            if (!success) {
                updatePhoneVerificationState(false);
                showPhoneVerificationHelp(message, Color.decode("#DC2626"));
                if (phoneOtpDialog != null) {
                    phoneOtpDialog.markSendFailed(message);
                }
                return;
            }

            showPhoneVerificationHelp("Mã OTP đã được gửi. Nhập mã để hoàn tất xác minh.", Color.decode("#047857"));
            if (phoneOtpDialog == null || !phoneOtpDialog.isDisplayable()) {
                phoneOtpDialog = new PhoneOtpDialog(
                        SwingUtilities.getWindowAncestor(this),
                        pendingPhoneVerification,
                        this::verifyPhoneOtp,
                        this::sendPhoneVerificationOtp
                );
            }
            phoneOtpDialog.markSent(message);
        });
    }

    public void handlePhoneVerificationResult(boolean success, String message) {
        SwingUtilities.invokeLater(() -> {
            updatePhoneVerificationState(success);
            showPhoneVerificationHelp(message, success ? Color.decode("#047857") : Color.decode("#DC2626"));
            if (phoneOtpDialog != null) {
                phoneOtpDialog.handleResult(success, message);
            }
        });
    }

    private void verifyPhoneOtp(String otp) {
        if (phoneOtpDialog != null) {
            phoneOtpDialog.markVerifying();
        }
        new Thread(() -> {
            try {
                NetworkManager.getInstance().sendPacket(new Packet("VERIFY_PHONE_OTP", pendingPhoneVerification + "|" + otp));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    updatePhoneVerificationState(false);
                    showPhoneVerificationHelp("Không kết nối được server. Vui lòng thử lại.", Color.decode("#DC2626"));
                    if (phoneOtpDialog != null) {
                        phoneOtpDialog.markSendFailed("Không kết nối được server. Vui lòng thử lại.");
                    }
                });
            }
        }).start();
    }

    private void showPhoneVerificationHelp(String message, Color color) {
        if (lblPhoneVerificationHelp != null) {
            lblPhoneVerificationHelp.setText(message == null || message.trim().isEmpty() ? " " : message);
            lblPhoneVerificationHelp.setForeground(color);
        }
    }

   
    public void loadProfileData(String[] data) {
        SwingUtilities.invokeLater(() -> {
            try {
                if(data.length > 0 && data[0] != null && !data[0].isEmpty()) lblLeftId.setText(data[0]);
                if(data.length > 1 && data[1] != null) txtEmail.setText(data[1]);
                if(data.length > 2 && data[2] != null && !data[2].isEmpty()) { txtName.setText(data[2]); lblLeftName.setText(data[2]); }
                if(data.length > 3 && data[3] != null) txtDob.setText(data[3]);
                if(data.length > 4 && data[4] != null && !data[4].isEmpty()) cbGender.setSelectedItem(data[4]);
                if(data.length > 5 && data[5] != null) txtPhone.setText(data[5]);
                if(data.length > 6 && data[6] != null) txtAddress.setText(data[6]);
                if(data.length > 7 && data[7] != null && !data[7].isEmpty()) { cbLocation.setSelectedItem(data[7]); lblLeftLocation.setText(data[7]); }
                if(data.length > 8 && data[8] != null && !data[8].isEmpty()) { txtSubject.setText(data[8]); lblLeftRole.setText("Gia sư " + data[8]); }
                if(data.length > 9 && data[9] != null) txtBio.setText(data[9].replace("\\n", "\n"));
                updatePhoneVerificationState(data.length > 13 && Boolean.parseBoolean(data[13]));
                
                if(data.length > 10 && data[10] != null && !data[10].isEmpty() && !data[10].equals("null")) {
                    File f = new File(data[10]);
                    currentServerCvFileName = f.getName(); // Lưu lại tên file để tải/xem
                    lblCvPreview.setText("<html><div style='text-align: center;'><img src='https://img.icons8.com/color/48/pdf.png'><br><br><b style='color:#2563EB;'>Đã tải lên: " + currentServerCvFileName + "</b></div></html>");
                }
                
                // QUAN TRỌNG: Load ảnh Mặt trước eKYC
                if(data.length > 11 && data[11] != null && !data[11].trim().isEmpty() && !data[11].equals("null")) {
                    try {
                        ekycFrontBytes = Base64.getDecoder().decode(data[11]); 
                        Image img = new ImageIcon(ekycFrontBytes).getImage();
                        
                        double scale = Math.min(280.0 / img.getWidth(null), 160.0 / img.getHeight(null));
                        Image scaledImg = img.getScaledInstance((int)(img.getWidth(null) * scale), (int)(img.getHeight(null) * scale), Image.SCALE_SMOOTH); 
                        
                        if (lblEkycFrontPreview != null) {
                            lblEkycFrontPreview.setText(""); // Xóa chữ "Nhấn để chọn ảnh"
                            lblEkycFrontPreview.setIcon(new ImageIcon(scaledImg));
                        }
                    } catch (Exception ex) { System.err.println("Lỗi render eKYC Front"); }
                }
                
                // QUAN TRỌNG: Load ảnh Mặt sau eKYC
                if(data.length > 12 && data[12] != null && !data[12].trim().isEmpty() && !data[12].equals("null")) {
                    try {
                        ekycBackBytes = Base64.getDecoder().decode(data[12]);
                        Image img = new ImageIcon(ekycBackBytes).getImage();
                        
                        double scale = Math.min(280.0 / img.getWidth(null), 160.0 / img.getHeight(null));
                        Image scaledImg = img.getScaledInstance((int)(img.getWidth(null) * scale), (int)(img.getHeight(null) * scale), Image.SCALE_SMOOTH); 
                        
                        if (lblEkycBackPreview != null) {
                            lblEkycBackPreview.setText("");
                            lblEkycBackPreview.setIcon(new ImageIcon(scaledImg));
                        }
                    } catch (Exception ex) { System.err.println("Lỗi render eKYC Back"); }
                }
                
                setEditMode(false);
            } catch (Exception e) { 
                e.printStackTrace(); 
            }
        });
    }            

   private JPanel createDegreesForm() {
        RoundedPanel p = new RoundedPanel(12);
        p.setLayout(new BorderLayout(0, 10));
        p.setBorder(new EmptyBorder(20, 20, 15, 20));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel titleWrap = new JPanel();
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.setOpaque(false);
        JLabel title = new JLabel("Bằng cấp giáo dục");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(TEXT_MAIN);
        JLabel lblCount = new JLabel("Quản lý danh sách bằng cấp");
        lblCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCount.setForeground(TEXT_MUTED);
        titleWrap.add(title);
        titleWrap.add(lblCount);

        JButton btnAdd = new JButton("+ Thêm bằng cấp") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); 
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(PRIMARY); 
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btnAdd.setBackground(Color.WHITE);
        btnAdd.setForeground(PRIMARY); 
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAdd.setContentAreaFilled(false);
        btnAdd.setBorder(new EmptyBorder(6, 15, 6, 15));
        btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdd.addActionListener(e -> showAddDegreeDialog());

        top.add(titleWrap, BorderLayout.WEST);
        top.add(btnAdd, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);

        // --- 1. THÊM CỘT TỆP ĐÍNH KÈM VÀO MODEL (Tổng 7 cột) ---
        String[] cols = {"Tên bằng cấp, Chuyên ngành", "Trường đào tạo", "Năm TN", "Xếp loại", "Tệp đính kèm", "Trạng thái", "Thao tác"};
        degTableModel = new DefaultTableModel(null, cols) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        JTable table = new JTable(degTableModel);
        table.setRowHeight(55);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(Color.decode("#F8FAFC"));

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 40));
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(Color.decode("#EEF2FF")); 
        header.setForeground(Color.decode("#312E81")); 
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.decode("#C7D2FE"))); 
        header.setReorderingAllowed(false);

        // --- 2. RENDER CỘT 4: TỆP ĐÍNH KÈM (ĐÃ SỬA CÁCH LẤY TÊN FILE) ---
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 15));
                cell.setOpaque(true);
                cell.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                
                if (value != null && !value.toString().isEmpty() && !value.toString().equals("NO_FILE")) {
                    String fullPath = value.toString();
                    // Lấy ra tên file cuối cùng để hiển thị (loại bỏ phần thư mục server_uploads...)
                    String displayFileName = new java.io.File(fullPath).getName();
                    
                    // Rút gọn nếu tên quá dài
                    if (displayFileName.length() > 18) {
                        displayFileName = displayFileName.substring(0, 15) + "...";
                    }
                    
                    // Dùng thẻ HTML <u> để gạch chân giống link
                    JLabel lblFile = new JLabel("<html><u>" + displayFileName + "</u></html>");
                    lblFile.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    lblFile.setForeground(PRIMARY);
                    lblFile.setCursor(new Cursor(Cursor.HAND_CURSOR)); // Hiện bàn tay khi trỏ vào
                    cell.add(lblFile);
                } else {
                    JLabel lblEmpty = new JLabel("-");
                    lblEmpty.setForeground(TEXT_MUTED);
                    cell.add(lblEmpty);
                }
                return cell;
            }
        });

        // --- 3. RENDER CỘT 5: TRẠNG THÁI ---
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                String status = (value != null) ? value.toString() : "";
                JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 15));
                cell.setOpaque(true);
                cell.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);

                JLabel badge = new JLabel(status);
                badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
                badge.setBorder(new EmptyBorder(4, 10, 4, 10));
                badge.setOpaque(true);

                if (status.equals("Đã xác minh") || status.equals("Đã duyệt")) {
                    badge.setBackground(Color.decode("#DCFCE7"));
                    badge.setForeground(Color.decode("#15803D"));
                } else {
                    badge.setBackground(Color.decode("#FEF3C7"));
                    badge.setForeground(Color.decode("#B45309"));
                }
                cell.add(badge);
                return cell;
            }
        });

        // --- 4. RENDER CỘT 6: THAO TÁC ---
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JPanel cell = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
                cell.setOpaque(true);
                cell.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);

                JLabel lblEdit = new JLabel("<html><span style='color:#6366F1;font-size:18px;'>✎</span></html>");
                lblEdit.setCursor(new Cursor(Cursor.HAND_CURSOR));
                JLabel lblDel = new JLabel("<html><span style='color:#EF4444;font-size:18px;'>🗑</span></html>");
                lblDel.setCursor(new Cursor(Cursor.HAND_CURSOR));

                cell.add(lblEdit);
                cell.add(lblDel);
                return cell;
            }
        });
        table.getColumnModel().getColumn(6).setMaxWidth(100);

        // --- 5. XỬ LÝ SỰ KIỆN CLICK (Tải xuống & Thao tác) ---
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0) {
                    // Cột 4: Tải file đính kèm
                    if (col == 4) {
                        String fileName = (String) degTableModel.getValueAt(row, 4);
                        downloadAttachedFile(fileName);
                    }
                    // Cột 6: Sửa / Xóa
                    else if (col == 6) {
                        Rectangle cellRect = table.getCellRect(row, col, false);
                        int clickX = e.getPoint().x - cellRect.x;
                        String htmlName = (String) degTableModel.getValueAt(row, 0);
                        String uni = (String) degTableModel.getValueAt(row, 1);

                        if (clickX < cellRect.width / 2) {
                            String certName = null;
                            showEditCertificateDialog(row, certName);
                        } else {
                            int confirm = JOptionPane.showConfirmDialog(ProfileTab.this, "Bạn có chắc chắn muốn xóa chứng chỉ này?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
                            if (confirm == JOptionPane.YES_OPTION) {
                                // Lấy chính xác tên chứng chỉ gốc chưa bị cắt ngắn từ TableModel
                                String fullCertName = (String) certTableModel.getValueAt(row, 0);
                                certTableModel.removeRow(row);
                                new Thread(() -> {
                                    try { 
                                        // GỬI CHUẨN TÊN CHỨNG CHỈ SANG SERVER
                                        NetworkManager.getInstance().sendPacket(new Packet("DELETE_CERTIFICATE", fullCertName)); 
                                    } catch (Exception ex) {}
                                }).start();
                            }
                        }
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null); 
        scroll.getViewport().setBackground(Color.WHITE);
        p.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(10, 0, 0, 0));
        JLabel lblPageInfo = new JLabel("Danh sách bằng cấp đã tải lên");
        lblPageInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPageInfo.setForeground(TEXT_MUTED);
        JPanel pnlPagination = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlPagination.setOpaque(false);
        JComboBox<String> cbRows = new JComboBox<>(new String[]{"10 / trang", "20 / trang"});
        cbRows.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        pnlPagination.add(cbRows);

        footer.add(lblPageInfo, BorderLayout.WEST);
        footer.add(pnlPagination, BorderLayout.EAST);
        p.add(footer, BorderLayout.SOUTH);

        return p;
    }
    private JPanel createCertificatesForm() {
        RoundedPanel p = new RoundedPanel(12);
        p.setLayout(new BorderLayout(0, 10));
        p.setBorder(new EmptyBorder(20, 20, 15, 20));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        
        JPanel titleWrap = new JPanel();
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.setOpaque(false);
        JLabel title = new JLabel("Chứng chỉ chuyên môn");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(TEXT_MAIN);
        JLabel lblCount = new JLabel("Quản lý danh sách chứng chỉ");
        lblCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCount.setForeground(TEXT_MUTED);
        titleWrap.add(title); titleWrap.add(lblCount);

        // -- NÚT NỀN TRẮNG, CHỮ TÍM, VIỀN TÍM --
        // -- NÚT THÊM CHỨNG CHỈ (Đã sửa lỗi khuyết nét) --
        JButton btnAdd = new JButton("+ Thêm chứng chỉ") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Thụt vào 1px
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
                
                // Vẽ viền
                g2.setColor(PRIMARY); 
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
                
                super.paintComponent(g); 
                g2.dispose();
            }
        };
        btnAdd.setBackground(Color.WHITE); 
        btnAdd.setForeground(PRIMARY);
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAdd.setContentAreaFilled(false); 
        btnAdd.setBorderPainted(false); // Bắt buộc tắt viền mặc định
        btnAdd.setBorder(new EmptyBorder(6, 15, 6, 15));
        btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdd.addActionListener(e -> showAddCertificateDialog());
        
        top.add(titleWrap, BorderLayout.WEST);
        top.add(btnAdd, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);

        String[] cols = {"Tên chứng chỉ", "Đơn vị cấp", "Ngày cấp", "Hạn SD", "Tệp đính kèm", "Trạng thái", "Thao tác"};
        certTableModel = new DefaultTableModel(null, cols) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        
        JTable table = new JTable(certTableModel);
        table.setRowHeight(55); 
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(Color.decode("#F8FAFC"));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // -- HEADER TÍM NHẠT, CHỮ TÍM ĐẬM --
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 40));
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(Color.decode("#EEF2FF")); 
        header.setForeground(Color.decode("#312E81"));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.decode("#C7D2FE")));
        header.setReorderingAllowed(false);

        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 15));
                cell.setOpaque(true); cell.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                if (value != null && !value.toString().isEmpty()) {
                    JLabel lblFile = new JLabel(value.toString());
                    lblFile.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblFile.setForeground(PRIMARY);
                    cell.add(lblFile);
                }
                return cell;
            }
        });

        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                String status = (value != null) ? value.toString() : "";
                JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 15));
                cell.setOpaque(true); cell.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                JLabel badge = new JLabel(status); badge.setFont(new Font("Segoe UI", Font.BOLD, 11)); badge.setBorder(new EmptyBorder(4, 10, 4, 10)); badge.setOpaque(true);
                
                if (status.equals("Hợp lệ") || status.equals("Đã duyệt") || status.equals("Đã xác minh")) {
                    badge.setBackground(Color.decode("#DCFCE7")); badge.setForeground(Color.decode("#15803D"));
                } else { 
                    badge.setBackground(Color.decode("#FEF3C7")); badge.setForeground(Color.decode("#B45309"));
                }
                cell.add(badge); return cell;
            }
        });

        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JPanel cell = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
                cell.setOpaque(true); cell.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                
                JLabel lblEdit = new JLabel("<html><span style='color:#6366F1;font-size:18px;'>✎</span></html>");
                lblEdit.setCursor(new Cursor(Cursor.HAND_CURSOR));
                JLabel lblDel = new JLabel("<html><span style='color:#EF4444;font-size:18px;'>🗑</span></html>");
                lblDel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                cell.add(lblEdit); cell.add(lblDel); return cell;
            }
        });
        table.getColumnModel().getColumn(6).setMaxWidth(100);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0) {
                    // Cột 4: Tải file đính kèm
                    if (col == 4) {
                        String fileName = (String) certTableModel.getValueAt(row, 4);
                        downloadAttachedFile(fileName);
                    }
                    // Cột 6: Sửa / Xóa
                    else if (col == 6) {
                        Rectangle cellRect = table.getCellRect(row, col, false);
                        int clickX = e.getPoint().x - cellRect.x;
                        String certName = (String) certTableModel.getValueAt(row, 0);

                        if (clickX < cellRect.width / 2) {
                            showEditCertificateDialog(row, certName);
                        } else {
                            int confirm = JOptionPane.showConfirmDialog(ProfileTab.this, "Bạn có chắc chắn muốn xóa chứng chỉ này?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
                            if (confirm == JOptionPane.YES_OPTION) {
                                certTableModel.removeRow(row);
                                new Thread(() -> {
                                    try { NetworkManager.getInstance().sendPacket(new Packet("DELETE_CERTIFICATE", certName)); } catch (Exception ex) {}
                                }).start();
                            }
                        }
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        p.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout()); footer.setOpaque(false); footer.setBorder(new EmptyBorder(10, 0, 0, 0));
        JLabel lblPageInfo = new JLabel("Danh sách chứng chỉ đã tải lên"); lblPageInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblPageInfo.setForeground(TEXT_MUTED);
        JPanel pnlPagination = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); pnlPagination.setOpaque(false);
        JComboBox<String> cbRows = new JComboBox<>(new String[]{"10 / trang", "20 / trang"}); cbRows.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        pnlPagination.add(cbRows);

        footer.add(lblPageInfo, BorderLayout.WEST); footer.add(pnlPagination, BorderLayout.EAST);
        p.add(footer, BorderLayout.SOUTH);

        return p;
    }

  private JPanel createCVForm() {
        RoundedPanel p = new RoundedPanel(12); 
        p.setLayout(new BorderLayout(15, 10)); 
        p.setBorder(new EmptyBorder(10, 15, 10, 15)); 

        // --- HEADER ---
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titleWrap = new JPanel();
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.setOpaque(false);
        JLabel lblTitle = new JLabel("Hồ sơ năng lực (CV)"); 
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15)); 
        lblTitle.setForeground(TEXT_MAIN);
        JLabel lblSub = new JLabel("Tải lên CV để tăng độ tin cậy với học viên");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(TEXT_MUTED);
        titleWrap.add(lblTitle); titleWrap.add(lblSub);
        
        header.add(titleWrap, BorderLayout.WEST);
        // ĐÃ XÓA NÚT "+ Tải CV mới" Ở ĐÂY
        p.add(header, BorderLayout.NORTH);

        // --- CENTER AREA ---
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        centerPanel.setOpaque(false);

        // --- LEFT COLUMN (UPLOAD & INFO) ---
        JPanel leftCol = new JPanel();
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.setOpaque(false);

        // 1. Upload Box
        JPanel uploadWrap = new JPanel(new BorderLayout());
        uploadWrap.setOpaque(false);
        uploadWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150)); 
        
        RoundedPanel uploadBox = new RoundedPanel(12);
        uploadBox.setLayout(new BoxLayout(uploadBox, BoxLayout.Y_AXIS));
        uploadBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createDashedBorder(Color.decode("#CBD5E1"), 2, 5, 5, true),
            new EmptyBorder(10, 10, 10, 10) 
        ));
        
        JPanel iconWrap = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode("#EFF6FF")); g2.fillOval(0,0,getWidth(),getHeight()); g2.dispose();
            }
        };
        iconWrap.setOpaque(false); iconWrap.setPreferredSize(new Dimension(40,40)); iconWrap.setMaximumSize(new Dimension(40,40));
        iconWrap.setLayout(new GridBagLayout());
        JLabel lblUploadIcon = new JLabel(); setNetworkIcon(lblUploadIcon, "https://img.icons8.com/fluency-systems-filled/48/2563EB/upload-to-cloud.png", 20, 20);
        iconWrap.add(lblUploadIcon);
        iconWrap.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblDrag = new JLabel("Kéo & thả CV vào đây"); lblDrag.setFont(new Font("Segoe UI", Font.BOLD, 12)); lblDrag.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel lblOr = new JLabel("hoặc chọn tệp từ máy tính"); lblOr.setFont(new Font("Segoe UI", Font.PLAIN, 11)); lblOr.setForeground(TEXT_MUTED); lblOr.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // ĐÃ SỬA: Nút "Chọn tệp" với màu nền ĐỎ
        JButton btnChoose = new JButton("Chọn tệp");
        btnChoose.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnChoose.setForeground(Color.WHITE);
        btnChoose.setBackground(Color.decode("#EF4444")); // Mã màu đỏ của thẻ
        btnChoose.setBorder(new EmptyBorder(6, 16, 6, 16));
        btnChoose.setFocusPainted(false);
        btnChoose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnChoose.setAlignmentX(Component.CENTER_ALIGNMENT);

        uploadBox.add(iconWrap); uploadBox.add(Box.createVerticalStrut(5));
        uploadBox.add(lblDrag); uploadBox.add(lblOr); uploadBox.add(Box.createVerticalStrut(8));
        uploadBox.add(btnChoose);
        uploadWrap.add(uploadBox, BorderLayout.CENTER);

        // 2. File Info Area
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        JLabel lblInfoTitle = new JLabel("Thông tin tệp"); lblInfoTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        JPanel fileRow = new JPanel(new BorderLayout()); 
        fileRow.setOpaque(false); fileRow.setBorder(new EmptyBorder(5, 0, 5, 0));
        
        JPanel fileLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); fileLeft.setOpaque(false);
        JLabel fileIcon = new JLabel(); setNetworkIcon(fileIcon, "https://img.icons8.com/color/48/pdf.png", 28, 28);
        
        JPanel fileDetail = new JPanel(); fileDetail.setLayout(new BoxLayout(fileDetail, BoxLayout.Y_AXIS)); fileDetail.setOpaque(false);
        JLabel lblCvNameDetail = new JLabel("Chưa có tệp nào"); lblCvNameDetail.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JLabel lblCvDateDetail = new JLabel("Cập nhật: -"); lblCvDateDetail.setFont(new Font("Segoe UI", Font.PLAIN, 10)); lblCvDateDetail.setForeground(TEXT_MUTED);
        fileDetail.add(lblCvNameDetail); fileDetail.add(Box.createVerticalStrut(2)); fileDetail.add(lblCvDateDetail);
        
        fileLeft.add(fileIcon); fileLeft.add(fileDetail);
        
        JPanel fileRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); fileRight.setOpaque(false);
        JLabel lblCvSizeDetail = new JLabel("-"); lblCvSizeDetail.setFont(new Font("Segoe UI", Font.PLAIN, 11)); lblCvSizeDetail.setForeground(TEXT_MUTED);
        fileRight.add(lblCvSizeDetail);
        
        fileRow.add(fileLeft, BorderLayout.WEST); fileRow.add(fileRight, BorderLayout.EAST);

        // 3. Status Row
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); statusRow.setOpaque(false);
        statusRow.setBorder(new EmptyBorder(0, 0, 10, 0));
        JLabel lblStatus = new JLabel("Trạng thái"); lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblStatus.setForeground(TEXT_MUTED);
        
        ColorRoundedPanel badge = new ColorRoundedPanel(8, Color.WHITE); 
        badge.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR), new EmptyBorder(3, 8, 3, 8)));
        JLabel lblBadgeText = new JLabel("Chờ tải lên"); lblBadgeText.setFont(new Font("Segoe UI", Font.BOLD, 11)); lblBadgeText.setForeground(Color.decode("#B45309"));
        badge.add(lblBadgeText);
        statusRow.add(lblStatus); statusRow.add(badge);

        // 4. Action Buttons
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); actionRow.setOpaque(false);
        
        // ĐÃ THÊM: Nút "Xem CV"
        JButton btnViewCV = createUploadButton("Xem CV");
        setNetworkIcon(btnViewCV, "https://img.icons8.com/fluency-systems-regular/48/2563EB/visible.png", 14, 14);

        JButton btnDownload = createUploadButton("Tải xuống"); 
        setNetworkIcon(btnDownload, "https://img.icons8.com/fluency-systems-regular/48/2563EB/download.png", 14, 14);
        
        // Sắp xếp lại thứ tự các nút cho hợp lý
        actionRow.add(btnViewCV); actionRow.add(btnDownload);

        infoPanel.add(lblInfoTitle); 
        infoPanel.add(fileRow); 
        infoPanel.add(statusRow); 
        infoPanel.add(actionRow);

        leftCol.add(uploadWrap);
        leftCol.add(Box.createVerticalGlue()); 
        leftCol.add(infoPanel);

        // --- RIGHT COLUMN (PREVIEW) ---
        JPanel rightCol = new JPanel(new BorderLayout());
        rightCol.setOpaque(false);
        
        JLabel lblPreviewTitle = new JLabel("Xem trước CV");
        lblPreviewTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPreviewTitle.setBorder(new EmptyBorder(0, 0, 5, 0));
        
        RoundedPanel previewCard = new RoundedPanel(12);
        previewCard.setLayout(new BorderLayout());
        
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        toolbar.setBackground(Color.decode("#FAFAFA"));
        toolbar.add(new JLabel("1 / 1"));
        toolbar.add(new JLabel("100%"));
        
        JPanel previewBody = new JPanel(new GridBagLayout()); 
        previewBody.setBackground(Color.decode("#F1F5F9")); 
        previewBody.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        lblCvPreview = new JLabel("<html><div style='text-align: center;'><img src='https://img.icons8.com/color/48/pdf.png'><br><br>Chưa có bản xem trước</div></html>", SwingConstants.CENTER);
        lblCvPreview.setOpaque(true);
        lblCvPreview.setBackground(Color.WHITE);
        lblCvPreview.setBorder(BorderFactory.createLineBorder(Color.decode("#E2E8F0")));
        lblCvPreview.setPreferredSize(new Dimension(240, 300)); 
        
        previewBody.add(lblCvPreview);
        
        previewCard.add(toolbar, BorderLayout.NORTH);
        previewCard.add(previewBody, BorderLayout.CENTER);
        
        rightCol.add(lblPreviewTitle, BorderLayout.NORTH);
        rightCol.add(previewCard, BorderLayout.CENTER);

        centerPanel.add(leftCol);
        centerPanel.add(rightCol);
        p.add(centerPanel, BorderLayout.CENTER);

        // --- ACTIONS ---
        btnChoose.addActionListener(e -> {
            JFileChooser jfc = new JFileChooser();
            jfc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Documents", "pdf"));
            if (jfc.showOpenDialog(ProfileTab.this) == JFileChooser.APPROVE_OPTION) {
                try {
                    java.io.File f = jfc.getSelectedFile();
                    if(f.length() > 5 * 1024 * 1024) { JOptionPane.showMessageDialog(ProfileTab.this, "File tối đa 5MB"); return; }
                    cvFileBytes = java.nio.file.Files.readAllBytes(f.toPath());
                    cvFileNameStr = f.getName();
                    
                    lblCvNameDetail.setText(cvFileNameStr.length() > 20 ? cvFileNameStr.substring(0, 20) + "..." : cvFileNameStr);
                    double sizeMB = f.length() / (1024.0 * 1024.0);
                    lblCvSizeDetail.setText(String.format("%.1f MB", sizeMB));
                    
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
                    lblCvDateDetail.setText("Cập nhật: " + sdf.format(new java.util.Date()));
                    
                    lblBadgeText.setText("Sẵn sàng lưu");
                    badge.bg = Color.decode("#FEF3C7"); 
                    
                    lblCvPreview.setText("<html><div style='text-align: center; max-width: 180px;'><img src='https://img.icons8.com/color/48/pdf.png'><br><br><b style='color:#2563EB;'>" + cvFileNameStr + "</b></div></html>");
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });

        // Xử lý Tải xuống CV từ DB
        btnDownload.addActionListener(e -> {
            if (currentServerCvFileName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Chưa có CV nào trên hệ thống để tải xuống!");
                return;
            }
            downloadAttachedFile(currentServerCvFileName);
        });

        // Xử lý Xem CV
        btnViewCV.addActionListener(e -> {
            if (currentServerCvFileName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Chưa có CV nào trên hệ thống để xem!");
                return;
            }
            // Bật cờ Preview và gọi hàm tải file
            isPreviewingFile = true;
            try {
                NetworkManager.getInstance().sendPacket(new Packet("DOWNLOAD_FILE", currentServerCvFileName));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        return p;
    }
   private JPanel createExpertiseForm() {
        JPanel p = new JPanel(new BorderLayout(15, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(5, 0, 5, 0));

        RoundedPanel centerPanel = new RoundedPanel(12);
        centerPanel.setLayout(new BorderLayout());
        centerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        
        JPanel titleWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titleWrap.setOpaque(false);
        
        ColorRoundedPanel iconWrap = new ColorRoundedPanel(10, Color.decode("#EFF6FF"));
        iconWrap.setBorder(new EmptyBorder(6, 6, 6, 6));
        JLabel iconLbl = new JLabel();
        setNetworkIcon(iconLbl, "https://img.icons8.com/briefcase.png", 20, 20);
        iconWrap.add(iconLbl);
        
        JPanel textWrap = new JPanel();
        textWrap.setLayout(new BoxLayout(textWrap, BoxLayout.Y_AXIS));
        textWrap.setOpaque(false);
        JLabel lblTitle = new JLabel("Kinh nghiệm giảng dạy");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(TEXT_MAIN);
        JLabel lblSub = new JLabel("Thông tin kinh nghiệm giúp tăng độ uy tín với học viên");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(TEXT_MUTED);
        textWrap.add(lblTitle); textWrap.add(lblSub);
        
        titleWrap.add(iconWrap); titleWrap.add(textWrap);
        
        JButton btnAdd = new JButton("+ Thêm kinh nghiệm");
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setBackground(PRIMARY);
        btnAdd.setBorder(new EmptyBorder(6, 12, 6, 12));
        btnAdd.setFocusPainted(false);
        btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdd.addActionListener(e -> showAddExperienceDialog());
        
        header.add(titleWrap, BorderLayout.WEST);
        header.add(btnAdd, BorderLayout.EAST);
        
        centerPanel.add(header, BorderLayout.NORTH);
        
        // --- KHỞI TẠO VÀ LƯU BIẾN PANEL DANH SÁCH ---
        timelineListPanel = new JPanel();
        timelineListPanel.setLayout(new BoxLayout(timelineListPanel, BoxLayout.Y_AXIS));
        timelineListPanel.setOpaque(false);
        timelineListPanel.setBorder(new EmptyBorder(25, 0, 0, 0)); 
        
        // (Đã xóa các dòng add hardcode tạo dữ liệu mẫu ở đây)
            
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(15, 0, 0, 0));
        JButton btnMore = new JButton("Hiển thị thêm ▾");
        btnMore.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnMore.setForeground(PRIMARY);
        btnMore.setContentAreaFilled(false);
        btnMore.setBorderPainted(false);
        btnMore.setCursor(new Cursor(Cursor.HAND_CURSOR));
        footer.add(btnMore);
        
        JPanel scrollContent = new JPanel(new BorderLayout());
        scrollContent.setOpaque(false);
        scrollContent.add(timelineListPanel, BorderLayout.CENTER); // Dùng timelineListPanel
        scrollContent.add(footer, BorderLayout.SOUTH);
        
        JScrollPane scroll = new JScrollPane(scrollContent);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        
        centerPanel.add(scroll, BorderLayout.CENTER);
        p.add(centerPanel, BorderLayout.CENTER);
        
        return p;
    }

   private JPanel createTimelineCard(String time, String title, String status, String type, String location, String desc, String[] tags) {
        JPanel row = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode("#E2E8F0")); 
                g2.fillRect(70, 15, 2, getHeight()); 
                g2.setColor(PRIMARY);
                g2.fillOval(66, 11, 10, 10); 
            }
        };
        row.setOpaque(false);
        
        String timeHtml = "<html><div style='text-align:right; color:#2563EB; font-weight:bold; font-size:11px; width: 50px; line-height:1.3;'>" + time.replace(" - ", " -<br>") + "</div></html>";
        JLabel lblTime = new JLabel(timeHtml);
        lblTime.setVerticalAlignment(SwingConstants.TOP);
        lblTime.setBorder(new EmptyBorder(8, 0, 0, 0));
        row.add(lblTime, BorderLayout.WEST);
        
        RoundedPanel card = new RoundedPanel(12);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            new EmptyBorder(15, 20, 15, 20) 
        ));
        
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JPanel titleWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titleWrap.setOpaque(false);
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(TEXT_MAIN);
        
        ColorRoundedPanel badge = new ColorRoundedPanel(8, status.equals("Đã duyệt") ? Color.decode("#DCFCE7") : Color.decode("#FEF3C7"));
        badge.setBorder(new EmptyBorder(2, 8, 2, 8));
        JLabel lblBadge = new JLabel(status);
        lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblBadge.setForeground(status.equals("Đã duyệt") ? Color.decode("#15803D") : Color.decode("#B45309"));
        badge.add(lblBadge);
        
        titleWrap.add(lblTitle); titleWrap.add(badge);
        
        // --- CHỈNH SỬA Ở ĐÂY: KHU VỰC NÚT SỬA VÀ XÓA ---
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        actionPanel.setOpaque(false);

        JButton btnEdit = new JButton();
        btnEdit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEdit.setContentAreaFilled(false); btnEdit.setBorderPainted(false); btnEdit.setPreferredSize(new Dimension(30, 30));
        setNetworkIcon(btnEdit, "https://img.icons8.com/fluency-systems-regular/48/2563EB/edit.png", 16, 16);

        JButton btnDelete = new JButton();
        btnDelete.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDelete.setContentAreaFilled(false); btnDelete.setBorderPainted(false); btnDelete.setPreferredSize(new Dimension(30, 30));
        setNetworkIcon(btnDelete, "https://img.icons8.com/fluency-systems-regular/48/DC2626/trash.png", 16, 16);

        // Logic Xóa
       // Logic Xóa Kinh nghiệm giảng dạy
       // Logic Xóa
        btnDelete.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn xóa kinh nghiệm này?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                // Xóa UI cục bộ
                Container parentRow = row.getParent();
                if (parentRow != null) {
                    parentRow.remove(row);
                    parentRow.revalidate();
                    parentRow.repaint();
                }
                // Gửi packet báo server xóa
                new Thread(() -> {
                    try {
                        // ĐÃ SỬA: Dùng 'title' thay vì 'location' (vì title chứa giá trị 'Gia sư tự do' trùng khớp với DB)
                        String payload = time.trim() + "|" + title.trim(); 
                        NetworkManager.getInstance().sendPacket(new Packet("DELETE_EXPERIENCE", payload));
                    } catch (Exception ex) { ex.printStackTrace(); }
                }).start();
            }
        });

        // Logic Sửa
        btnEdit.addActionListener(e -> {
            // Mở Dialog chỉnh sửa với dữ liệu cũ
            showEditExperienceDialog(time, title, desc);
        });

        actionPanel.add(btnEdit);
        actionPanel.add(btnDelete);
        
        top.add(titleWrap, BorderLayout.WEST);
        top.add(actionPanel, BorderLayout.EAST);
        // ----------------------------------------------
        
        JPanel subInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        subInfo.setOpaque(false);
        subInfo.setBorder(new EmptyBorder(8, 0, 8, 0));
        subInfo.add(createIconText("https://img.icons8.com/fluency-systems-regular/48/64748B/user.png", type));
        subInfo.add(createIconText("https://img.icons8.com/marker.png", location));
        
        JTextArea txtDesc = new JTextArea(desc);
        txtDesc.setWrapStyleWord(true);
        txtDesc.setLineWrap(true);
        txtDesc.setEditable(false);
        txtDesc.setOpaque(false);
        txtDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtDesc.setForeground(TEXT_MAIN);
        txtDesc.setBorder(new EmptyBorder(0, 5, 10, 5));
        
        JPanel tagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tagsPanel.setOpaque(false);
        for (String tag : tags) {
            tagsPanel.add(createTagPill(tag));
        }
        
        card.add(top);
        card.add(subInfo);
        card.add(txtDesc);
        card.add(tagsPanel);
        
        JPanel cardWrap = new JPanel(new BorderLayout());
        cardWrap.setOpaque(false);
        cardWrap.setBorder(new EmptyBorder(0, 20, 0, 0));
        cardWrap.add(card, BorderLayout.CENTER);
        
        row.add(cardWrap, BorderLayout.CENTER);
        return row;
    }

    private JPanel createTagPill(String text) {
        ColorRoundedPanel pill = new ColorRoundedPanel(8, Color.decode("#F1F5F9"));
        pill.setBorder(new EmptyBorder(3, 10, 3, 10));
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(PRIMARY);
        pill.add(lbl);
        return pill;
    }

    class ColorRoundedPanel extends JPanel {
        private int radius;
        public Color bg;
        public ColorRoundedPanel(int radius, Color bg) {
            this.radius = radius; this.bg = bg; setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private JPanel createVerificationForm() {
        RoundedPanel p = new RoundedPanel(12); 
        p.setLayout(new BorderLayout(0, 20)); 
        p.setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- HEADER ---
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        
        JPanel titleWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        titleWrap.setOpaque(false);
        
        ColorRoundedPanel iconWrap = new ColorRoundedPanel(12, Color.decode("#EFF6FF"));
        iconWrap.setBorder(new EmptyBorder(8, 8, 8, 8));
        JLabel iconLbl = new JLabel();
        setNetworkIcon(iconLbl, "https://img.icons8.com/fluency-systems-regular/48/2563EB/security-checked.png", 24, 24);
        iconWrap.add(iconLbl);
        
        JPanel textWrap = new JPanel();
        textWrap.setLayout(new BoxLayout(textWrap, BoxLayout.Y_AXIS));
        textWrap.setOpaque(false);
        JLabel title = new JLabel("Xác minh danh tính (eKYC)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(TEXT_MAIN);
        JLabel subtitle = new JLabel("Xác minh danh tính giúp tăng độ tin cậy và bảo vệ tài khoản của bạn");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(TEXT_MUTED);
        textWrap.add(title); textWrap.add(Box.createVerticalStrut(3)); textWrap.add(subtitle);
        
        titleWrap.add(iconWrap); titleWrap.add(textWrap);
        
        header.add(titleWrap, BorderLayout.WEST);
        p.add(header, BorderLayout.NORTH);
        
        // --- BODY (2 BOXES) ---
        JPanel uploadGrid = new JPanel(new GridLayout(1, 2, 20, 0)); 
        uploadGrid.setOpaque(false);
        uploadGrid.add(createDocUploadBox("CMND/CCCD Mặt trước", true));
        uploadGrid.add(createDocUploadBox("CMND/CCCD Mặt sau", false));
        p.add(uploadGrid, BorderLayout.CENTER);
        
        // --- FOOTER INFO (Đổi nền xanh nhạt theo mẫu) ---
        ColorRoundedPanel footerInfo = new ColorRoundedPanel(8, Color.decode("#EFF6FF")); 
        footerInfo.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 8));
        JLabel infoIcon = new JLabel(); 
        setNetworkIcon(infoIcon, "https://img.icons8.com/fluency-systems-filled/48/2563EB/info.png", 16, 16);
        JLabel infoText = new JLabel("Vui lòng đảm bảo hình ảnh rõ nét, đầy đủ 4 góc, không bị lóa sáng.");
        infoText.setFont(new Font("Segoe UI", Font.BOLD, 12)); 
        infoText.setForeground(Color.decode("#1E3A8A")); // Chữ màu xanh đậm
        footerInfo.add(infoIcon); footerInfo.add(infoText);
        
        p.add(footerInfo, BorderLayout.SOUTH); 
        
        return p;
    }
    private JPanel createDocUploadBox(String titleStr, boolean isFront) {
        RoundedPanel outerBox = new RoundedPanel(12);
        outerBox.setLayout(new BorderLayout(0, 15));
        outerBox.setBorder(new EmptyBorder(20, 20, 20, 20));

        // -- Header --
        JPanel header = new JPanel(new BorderLayout()); 
        header.setOpaque(false);
        JLabel title = new JLabel(titleStr); 
        title.setFont(new Font("Segoe UI", Font.BOLD, 14)); 
        title.setForeground(TEXT_MAIN);

        ColorRoundedPanel badge = new ColorRoundedPanel(6, Color.decode("#F1F5F9"));
        badge.setBorder(new EmptyBorder(3, 10, 3, 10));
        JLabel lblBadge = new JLabel("Chưa tải lên"); 
        lblBadge.setFont(new Font("Segoe UI", Font.PLAIN, 11)); 
        lblBadge.setForeground(TEXT_MUTED);
        badge.add(lblBadge);

        header.add(title, BorderLayout.WEST);
        header.add(badge, BorderLayout.EAST);

        // -- Vùng Content (Khung xám nhạt) --
        // Đổi sang BorderLayout để đẩy vùng nút bấm xuống đáy (SOUTH), nhường CENTER cho ảnh
        JPanel contentArea = new JPanel(new BorderLayout(0, 15));
        contentArea.setBackground(Color.WHITE);
        contentArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.decode("#E2E8F0")), // Viền nét liền mỏng
            new EmptyBorder(15, 15, 15, 15)
        ));

        // Hình ảnh xem trước (Căn giữa, bung toàn bộ không gian CENTER)
        JLabel lblPreview = new JLabel("<html><div style='text-align: center;'><img src='https://img.icons8.com/color/48/camera.png'><br><br>Nhấn để chọn ảnh</div></html>", SwingConstants.CENTER);
        lblPreview.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblPreview.setToolTipText("Nhấn vào đây để tải ảnh lên");

        // --- Khu vực dưới cùng (SOUTH) chứa Trạng thái và Nút bấm ---
        JPanel bottomArea = new JPanel();
        bottomArea.setLayout(new BoxLayout(bottomArea, BoxLayout.Y_AXIS));
        bottomArea.setOpaque(false);

        // 1. Trạng thái (Check xanh + Cập nhật: Ngày giờ)
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0)); 
        statusRow.setOpaque(false);
        JLabel checkIcon = new JLabel(); 
        JLabel lblDate = new JLabel("Chưa có dữ liệu"); 
        lblDate.setFont(new Font("Segoe UI", Font.PLAIN, 12)); 
        lblDate.setForeground(TEXT_MUTED);
        statusRow.add(checkIcon); statusRow.add(lblDate);

        // 2. Row chứa 2 Nút bấm
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0)); 
        actionRow.setOpaque(false);
        
        // Nút Xem ảnh lớn (Tô màu tím PRIMARY, chữ trắng, bo góc 8px)
        JButton btnView = new JButton("Xem ảnh lớn") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PRIMARY); 
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8); 
                super.paintComponent(g); g2.dispose();
            }
        };
        btnView.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnView.setForeground(Color.WHITE);
        btnView.setContentAreaFilled(false); btnView.setBorderPainted(false);
        btnView.setBorder(new EmptyBorder(8, 16, 8, 16));
        setNetworkIcon(btnView, "https://img.icons8.com/fluency-systems-regular/48/FFFFFF/search.png", 14, 14);
        btnView.setFocusPainted(false); btnView.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Nút Tải xuống (Tô màu tím PRIMARY, chữ trắng, bo góc 8px)
        JButton btnDownload = new JButton("Tải xuống") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PRIMARY); 
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8); 
                super.paintComponent(g); g2.dispose();
            }
        };
        btnDownload.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDownload.setForeground(Color.WHITE);
        btnDownload.setContentAreaFilled(false); btnDownload.setBorderPainted(false);
        btnDownload.setBorder(new EmptyBorder(8, 16, 8, 16));
        setNetworkIcon(btnDownload, "https://img.icons8.com/fluency-systems-regular/48/FFFFFF/download.png", 14, 14);
        btnDownload.setFocusPainted(false); btnDownload.setCursor(new Cursor(Cursor.HAND_CURSOR));

        actionRow.add(btnView);
        actionRow.add(btnDownload);

        // Gộp Trạng thái và Nút vào vùng dưới cùng
        bottomArea.add(statusRow);
        bottomArea.add(Box.createVerticalStrut(12));
        bottomArea.add(actionRow);

        // Add vào layout chính của contentArea
        contentArea.add(lblPreview, BorderLayout.CENTER); // Ảnh chiếm không gian lớn nhất ở giữa
        contentArea.add(bottomArea, BorderLayout.SOUTH);  // Nút bị ép xuống đáy

        if (isFront) {
            lblEkycFrontPreview = lblPreview;
        } else {
            lblEkycBackPreview = lblPreview;
        }

        outerBox.add(header, BorderLayout.NORTH);
        outerBox.add(contentArea, BorderLayout.CENTER);

        // -- Xử lý Click Chọn File --
        lblPreview.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                JFileChooser jfc = new JFileChooser();
                jfc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Hình ảnh", "jpg", "png", "jpeg"));
                if (jfc.showOpenDialog(ProfileTab.this) == JFileChooser.APPROVE_OPTION) {
                    try {
                        File f = jfc.getSelectedFile();
                        if(f.length() > 5 * 1024 * 1024) { JOptionPane.showMessageDialog(ProfileTab.this, "File tối đa 5MB"); return; }
                        byte[] bytes = Files.readAllBytes(f.toPath());
                        Image img = javax.imageio.ImageIO.read(f);

                        // Tính toán kích thước scale
                        int maxW = contentArea.getWidth() - 30; 
                        int maxH = contentArea.getHeight() - bottomArea.getHeight() - 40; 
                        double scale = Math.min((double) maxW / img.getWidth(null), (double) maxH / img.getHeight(null));
                        Image scaledImg = img.getScaledInstance((int)(img.getWidth(null) * scale), (int)(img.getHeight(null) * scale), Image.SCALE_SMOOTH);

                        lblPreview.setText("");
                        lblPreview.setIcon(new ImageIcon(scaledImg));

                        // Đổi màu Badge góc trên
                        lblBadge.setText("Đã lưu");
                        lblBadge.setForeground(Color.decode("#15803D")); 
                        badge.bg = Color.decode("#DCFCE7");
                        badge.repaint();

                        // Cập nhật trạng thái ngày giờ xanh lá
                        lblDate.setText("Cập nhật: " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()));
                        lblDate.setForeground(Color.decode("#15803D"));
                        setNetworkIcon(checkIcon, "https://img.icons8.com/color/48/ok--v1.png", 16, 16);

                        if (isFront) { ekycFrontBytes = bytes; } 
                        else { ekycBackBytes = bytes; }

                    } catch (Exception ex) { ex.printStackTrace(); }
                }
            }
        });

        // -- Xử lý Xem ảnh lớn --
        btnView.addActionListener(e -> {
            byte[] data = isFront ? ekycFrontBytes : ekycBackBytes;
            if (data == null) {
                JOptionPane.showMessageDialog(ProfileTab.this, "Chưa có ảnh để xem!");
                return;
            }
            showImageDialog(data, titleStr);
        });

        // -- Xử lý Tải xuống --
        btnDownload.addActionListener(e -> {
            byte[] data = isFront ? ekycFrontBytes : ekycBackBytes;
            if (data == null) {
                JOptionPane.showMessageDialog(ProfileTab.this, "Chưa có ảnh để tải xuống!");
                return;
            }
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Lưu ảnh");
            fileChooser.setSelectedFile(new File(isFront ? "mat_truoc.jpg" : "mat_sau.jpg"));
            if (fileChooser.showSaveDialog(ProfileTab.this) == JFileChooser.APPROVE_OPTION) {
                try {
                    Files.write(fileChooser.getSelectedFile().toPath(), data);
                    JOptionPane.showMessageDialog(ProfileTab.this, "Tải xuống thành công!");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ProfileTab.this, "Lỗi khi lưu file!");
                }
            }
        });

        return outerBox;
    }

    private JButton createUploadButton(String text) {
        JButton btn = new JButton(text); btn.setFont(new Font("Segoe UI", Font.BOLD, 12)); btn.setForeground(PRIMARY); btn.setBackground(Color.WHITE); btn.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PRIMARY), new EmptyBorder(6,12,6,12))); btn.setFocusPainted(false); btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); return btn;
    }

    private void showAddDegreeDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Thêm Bằng Cấp", true); dialog.setSize(400, 450); dialog.setLocationRelativeTo(this); dialog.setLayout(new BorderLayout(10, 10));
        JPanel form = new JPanel(new GridLayout(5, 2, 10, 15)); form.setBorder(new EmptyBorder(20, 20, 10, 20));
        JTextField txtDName = new JTextField(); JTextField txtMajor = new JTextField(); JTextField txtUni = new JTextField(); JTextField txtYear = new JTextField();
        form.add(new JLabel("Tên bằng (VD: Cử nhân):")); form.add(txtDName); form.add(new JLabel("Chuyên ngành:")); form.add(txtMajor);
        form.add(new JLabel("Trường đào tạo:")); form.add(txtUni); form.add(new JLabel("Năm tốt nghiệp:")); form.add(txtYear);
        JButton btnSelectFile = new JButton("Chọn File (PDF/Ảnh)"); JLabel lblFileName = new JLabel("Chưa chọn file");
        final byte[][] fileData = {null}; final String[] fileName = {""};
        btnSelectFile.addActionListener(e -> { JFileChooser jfc = new JFileChooser(); if (jfc.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) { try { File f = jfc.getSelectedFile(); if(f.length() > 5 * 1024 * 1024) { JOptionPane.showMessageDialog(dialog, "File tối đa 5MB"); return; } fileData[0] = Files.readAllBytes(f.toPath()); fileName[0] = f.getName(); lblFileName.setText(f.getName()); } catch (Exception ex) { ex.printStackTrace(); } } });
        form.add(btnSelectFile); form.add(lblFileName);
        JButton btnSave = new JButton("Lưu & Gửi duyệt"); btnSave.setBackground(PRIMARY); btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            if (fileData[0] == null || txtDName.getText().isEmpty()) { JOptionPane.showMessageDialog(dialog, "Vui lòng nhập tên bằng và chọn file!"); return; }
            btnSave.setText("Đang tải lên..."); btnSave.setEnabled(false);
            new Thread(() -> { try { String b64 = Base64.getEncoder().encodeToString(fileData[0]); String payload = txtDName.getText() + "|" + txtMajor.getText() + "|" + txtUni.getText() + "|" + txtYear.getText() + "|" + fileName[0] + "|" + b64; NetworkManager.getInstance().sendPacket(new Packet("ADD_DEGREE", payload)); SwingUtilities.invokeLater(() -> dialog.dispose()); } catch (Exception ex) { ex.printStackTrace(); } }).start();
        });
        JPanel bottom = new JPanel(); bottom.add(btnSave); bottom.setBorder(new EmptyBorder(0,0,20,0)); dialog.add(form, BorderLayout.CENTER); dialog.add(bottom, BorderLayout.SOUTH); dialog.setVisible(true);
    }
    private void showEditDegreeDialog(int row, String rawHtml, String oldUni) {
        // Tách chuỗi HTML để lấy lại tên và chuyên ngành cũ
        String oldName = rawHtml.replaceAll("(?s).*<b[^>]*>(.*?)</b>.*", "$1");
        String oldMajor = rawHtml.replaceAll("(?s).*<span[^>]*>(.*?)</span>.*", "$1");
        String oldYear = (String) degTableModel.getValueAt(row, 2);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chỉnh Sửa Bằng Cấp", true); 
        dialog.setSize(400, 450); dialog.setLocationRelativeTo(this); dialog.setLayout(new BorderLayout(10, 10));
        JPanel form = new JPanel(new GridLayout(5, 2, 10, 15)); form.setBorder(new EmptyBorder(20, 20, 10, 20));
        
        JTextField txtDName = new JTextField(oldName); 
        JTextField txtMajor = new JTextField(oldMajor); 
        JTextField txtUni = new JTextField(oldUni); 
        JTextField txtYear = new JTextField(oldYear);
        
        form.add(new JLabel("Tên bằng (VD: Cử nhân):")); form.add(txtDName); 
        form.add(new JLabel("Chuyên ngành:")); form.add(txtMajor);
        form.add(new JLabel("Trường đào tạo:")); form.add(txtUni); 
        form.add(new JLabel("Năm tốt nghiệp:")); form.add(txtYear);
        
        JButton btnSelectFile = new JButton("Chọn File Mới"); 
        JLabel lblFileName = new JLabel("Giữ nguyên file cũ");
        final byte[][] fileData = {null}; final String[] fileName = {""};
        
        btnSelectFile.addActionListener(e -> { 
            JFileChooser jfc = new JFileChooser(); 
            if (jfc.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) { 
                try { 
                    File f = jfc.getSelectedFile(); 
                    if(f.length() > 5 * 1024 * 1024) { JOptionPane.showMessageDialog(dialog, "File tối đa 5MB"); return; } 
                    fileData[0] = Files.readAllBytes(f.toPath()); 
                    fileName[0] = f.getName(); 
                    lblFileName.setText(f.getName()); 
                } catch (Exception ex) { ex.printStackTrace(); } 
            } 
        });
        
        form.add(btnSelectFile); form.add(lblFileName);
        
        JButton btnSave = new JButton("Lưu Cập Nhật"); btnSave.setBackground(PRIMARY); btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            if (txtDName.getText().isEmpty()) { JOptionPane.showMessageDialog(dialog, "Vui lòng nhập tên bằng!"); return; }
            btnSave.setText("Đang lưu..."); btnSave.setEnabled(false);
            new Thread(() -> { 
                try { 
                    String b64 = fileData[0] != null ? Base64.getEncoder().encodeToString(fileData[0]) : "NO_FILE"; 
                    // Gửi lên Server: oldUni để tìm bản ghi cũ ||| dữ liệu mới
                    String payload = oldUni + "|||" + txtDName.getText() + "|" + txtMajor.getText() + "|" + txtUni.getText() + "|" + txtYear.getText() + "|" + fileName[0] + "|" + b64; 
                    NetworkManager.getInstance().sendPacket(new Packet("UPDATE_DEGREE", payload)); 
                    SwingUtilities.invokeLater(() -> {
                        dialog.dispose();
                        JOptionPane.showMessageDialog(this, "Đã gửi yêu cầu cập nhật!");
                    }); 
                } catch (Exception ex) { ex.printStackTrace(); } 
            }).start();
        });
        
        JPanel bottom = new JPanel(); bottom.add(btnSave); bottom.setBorder(new EmptyBorder(0,0,20,0)); 
        dialog.add(form, BorderLayout.CENTER); dialog.add(bottom, BorderLayout.SOUTH); dialog.setVisible(true);
    }
    private void showAddCertificateDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Thêm Chứng Chỉ", true); dialog.setSize(400, 450); dialog.setLocationRelativeTo(this); dialog.setLayout(new BorderLayout(10, 10));
        JPanel form = new JPanel(new GridLayout(5, 2, 10, 15)); form.setBorder(new EmptyBorder(20, 20, 10, 20));
        JTextField txtCName = new JTextField(); JTextField txtProv = new JTextField(); JTextField txtIssue = new JTextField("DD/MM/YYYY"); JTextField txtExp = new JTextField("Vĩnh viễn");
        form.add(new JLabel("Tên chứng chỉ:")); form.add(txtCName); form.add(new JLabel("Đơn vị cấp:")); form.add(txtProv);
        form.add(new JLabel("Ngày cấp:")); form.add(txtIssue); form.add(new JLabel("Hạn sử dụng:")); form.add(txtExp);
        JButton btnSelectFile = new JButton("Chọn File (PDF/Ảnh)"); JLabel lblFileName = new JLabel("Chưa chọn file");
        final byte[][] fileData = {null}; final String[] fileName = {""};
        btnSelectFile.addActionListener(e -> { JFileChooser jfc = new JFileChooser(); if (jfc.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) { try { File f = jfc.getSelectedFile(); if(f.length() > 5 * 1024 * 1024) { JOptionPane.showMessageDialog(dialog, "File tối đa 5MB"); return; } fileData[0] = Files.readAllBytes(f.toPath()); fileName[0] = f.getName(); lblFileName.setText(f.getName()); } catch (Exception ex) { ex.printStackTrace(); } } });
        form.add(btnSelectFile); form.add(lblFileName);
        JButton btnSave = new JButton("Lưu & Gửi duyệt"); btnSave.setBackground(PRIMARY); btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            if (fileData[0] == null || txtCName.getText().isEmpty()) { JOptionPane.showMessageDialog(dialog, "Vui lòng nhập tên chứng chỉ và chọn file!"); return; }
            btnSave.setText("Đang tải lên..."); btnSave.setEnabled(false);
            new Thread(() -> { try { String b64 = Base64.getEncoder().encodeToString(fileData[0]); String payload = txtCName.getText() + "|" + txtProv.getText() + "|" + txtIssue.getText() + "|" + txtExp.getText() + "|" + fileName[0] + "|" + b64; NetworkManager.getInstance().sendPacket(new Packet("ADD_CERTIFICATE", payload)); SwingUtilities.invokeLater(() -> dialog.dispose()); } catch (Exception ex) { ex.printStackTrace(); } }).start();
        });
        JPanel bottom = new JPanel(); bottom.add(btnSave); bottom.setBorder(new EmptyBorder(0,0,20,0)); dialog.add(form, BorderLayout.CENTER); dialog.add(bottom, BorderLayout.SOUTH); dialog.setVisible(true);
    }
    
    private void showEditCertificateDialog(int row, String oldName) {
        String oldProv = (String) certTableModel.getValueAt(row, 1);
        String oldIssue = (String) certTableModel.getValueAt(row, 2);
        String oldExp = (String) certTableModel.getValueAt(row, 3);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chỉnh Sửa Chứng Chỉ", true); 
        dialog.setSize(400, 450); dialog.setLocationRelativeTo(this); dialog.setLayout(new BorderLayout(10, 10));
        JPanel form = new JPanel(new GridLayout(5, 2, 10, 15)); form.setBorder(new EmptyBorder(20, 20, 10, 20));
        
        JTextField txtCName = new JTextField(oldName); 
        JTextField txtProv = new JTextField(oldProv); 
        JTextField txtIssue = new JTextField(oldIssue); 
        JTextField txtExp = new JTextField(oldExp);
        
        form.add(new JLabel("Tên chứng chỉ:")); form.add(txtCName); 
        form.add(new JLabel("Đơn vị cấp:")); form.add(txtProv);
        form.add(new JLabel("Ngày cấp:")); form.add(txtIssue); 
        form.add(new JLabel("Hạn sử dụng:")); form.add(txtExp);
        
        JButton btnSelectFile = new JButton("Chọn File Mới"); 
        JLabel lblFileName = new JLabel("Giữ nguyên file cũ");
        final byte[][] fileData = {null}; final String[] fileName = {""};
        
        btnSelectFile.addActionListener(e -> { 
            JFileChooser jfc = new JFileChooser(); 
            if (jfc.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) { 
                try { 
                    File f = jfc.getSelectedFile(); 
                    if(f.length() > 5 * 1024 * 1024) { JOptionPane.showMessageDialog(dialog, "File tối đa 5MB"); return; } 
                    fileData[0] = Files.readAllBytes(f.toPath()); 
                    fileName[0] = f.getName(); 
                    lblFileName.setText(f.getName()); 
                } catch (Exception ex) { ex.printStackTrace(); } 
            } 
        });
        
        form.add(btnSelectFile); form.add(lblFileName);
        
        JButton btnSave = new JButton("Lưu Cập Nhật"); btnSave.setBackground(PRIMARY); btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            if (txtCName.getText().isEmpty()) { JOptionPane.showMessageDialog(dialog, "Vui lòng nhập tên chứng chỉ!"); return; }
            btnSave.setText("Đang lưu..."); btnSave.setEnabled(false);
            new Thread(() -> { 
                try { 
                    String b64 = fileData[0] != null ? Base64.getEncoder().encodeToString(fileData[0]) : "NO_FILE"; 
                    // Gửi lên Server: oldName để tìm bản ghi cũ ||| dữ liệu mới
                    String payload = oldName + "|||" + txtCName.getText() + "|" + txtProv.getText() + "|" + txtIssue.getText() + "|" + txtExp.getText() + "|" + fileName[0] + "|" + b64; 
                    NetworkManager.getInstance().sendPacket(new Packet("UPDATE_CERTIFICATE", payload)); 
                    SwingUtilities.invokeLater(() -> {
                        dialog.dispose();
                        JOptionPane.showMessageDialog(this, "Đã gửi yêu cầu cập nhật!");
                    }); 
                } catch (Exception ex) { ex.printStackTrace(); } 
            }).start();
        });
        
        JPanel bottom = new JPanel(); bottom.add(btnSave); bottom.setBorder(new EmptyBorder(0,0,20,0)); 
        dialog.add(form, BorderLayout.CENTER); dialog.add(bottom, BorderLayout.SOUTH); dialog.setVisible(true);
    }
    
    private void showAddExperienceDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Thêm Kinh Nghiệm", true);
        dialog.setSize(400, 350); dialog.setLocationRelativeTo(this); dialog.setLayout(new BorderLayout(10, 10));
        JPanel form = new JPanel(new GridLayout(3, 2, 10, 15)); form.setBorder(new EmptyBorder(20, 20, 10, 20));
        JTextField txtTime = new JTextField("VD: 2020 - 2022"); JTextField txtLoc = new JTextField("VD: Gia sư tự do");
        JTextArea txtDesc = new JTextArea("Mô tả công việc..."); txtDesc.setLineWrap(true); txtDesc.setWrapStyleWord(true);
        form.add(new JLabel("Thời gian:")); form.add(txtTime); form.add(new JLabel("Vị trí / Nơi dạy:")); form.add(txtLoc); form.add(new JLabel("Mô tả:")); form.add(new JScrollPane(txtDesc));
        JButton btnSave = new JButton("Lưu Kinh Nghiệm"); btnSave.setBackground(PRIMARY); btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            if (txtTime.getText().isEmpty() || txtLoc.getText().isEmpty()) { JOptionPane.showMessageDialog(dialog, "Vui lòng nhập đủ thông tin!"); return; }
            btnSave.setText("Đang lưu..."); btnSave.setEnabled(false);
            new Thread(() -> { try { String payload = txtTime.getText() + "|" + txtLoc.getText() + "|" + txtDesc.getText().replace("\n", "\\n"); NetworkManager.getInstance().sendPacket(new Packet("ADD_EXPERIENCE", payload)); SwingUtilities.invokeLater(() -> dialog.dispose()); } catch (Exception ex) { ex.printStackTrace(); } }).start();
        });
        JPanel bottom = new JPanel(); bottom.add(btnSave); bottom.setBorder(new EmptyBorder(0,0,20,0));
        dialog.add(form, BorderLayout.CENTER); dialog.add(bottom, BorderLayout.SOUTH); dialog.setVisible(true);
    }
    
    private void showEditExperienceDialog(String oldTime, String oldTitle, String oldDesc) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chỉnh Sửa Kinh Nghiệm", true);
        dialog.setSize(400, 350); 
        dialog.setLocationRelativeTo(this); 
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel form = new JPanel(new GridLayout(3, 2, 10, 15)); 
        form.setBorder(new EmptyBorder(20, 20, 10, 20));
        
        // Đổ dữ liệu cũ vào form
        JTextField txtTime = new JTextField(oldTime); 
        JTextField txtLoc = new JTextField(oldTitle); // Trong logic Add cũ, text thứ 2 là Loc/Title
        JTextArea txtDesc = new JTextArea(oldDesc); 
        txtDesc.setLineWrap(true); 
        txtDesc.setWrapStyleWord(true);
        
        form.add(new JLabel("Thời gian:")); form.add(txtTime); 
        form.add(new JLabel("Vị trí / Nơi dạy:")); form.add(txtLoc); 
        form.add(new JLabel("Mô tả:")); form.add(new JScrollPane(txtDesc));
        
        // NÚT LƯU SAU KHI SỬA
        JButton btnSave = new JButton("Lưu Cập Nhật"); 
        btnSave.setBackground(PRIMARY); 
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            if (txtTime.getText().isEmpty() || txtLoc.getText().isEmpty()) { 
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập đủ thông tin!"); 
                return; 
            }
            btnSave.setText("Đang lưu..."); 
            btnSave.setEnabled(false);
            
            new Thread(() -> { 
                try { 
                    // Gửi lên server: oldTime|oldTitle làm key để tìm, sau đó ghép data mới
                    String payload = oldTime + "|" + oldTitle + "|||" + txtTime.getText() + "|" + txtLoc.getText() + "|" + txtDesc.getText().replace("\n", "\\n"); 
                    NetworkManager.getInstance().sendPacket(new Packet("UPDATE_EXPERIENCE", payload)); 
                    
                    SwingUtilities.invokeLater(() -> {
                        dialog.dispose();
                        JOptionPane.showMessageDialog(this, "Đã gửi yêu cầu cập nhật. Vui lòng tải lại trang (hoặc chờ server trả data về) để xem thay đổi!");
                    }); 
                } catch (Exception ex) { 
                    ex.printStackTrace(); 
                    SwingUtilities.invokeLater(() -> { btnSave.setText("Lưu Cập Nhật"); btnSave.setEnabled(true); });
                } 
            }).start();
        });
        
        JPanel bottom = new JPanel(); 
        bottom.add(btnSave); 
        bottom.setBorder(new EmptyBorder(0,0,20,0));
        
        dialog.add(form, BorderLayout.CENTER); 
        dialog.add(bottom, BorderLayout.SOUTH); 
        dialog.setVisible(true);
    }
    
    // --- HÀM TẠO ICON TEXT BỊ THIẾU ---
    private JPanel createIconText(String iconUrl, String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);
        JLabel icon = new JLabel();
        setNetworkIcon(icon, iconUrl, 14, 14);
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT_MUTED);
        p.add(icon);
        p.add(lbl);
        return p;
    }

  public void loadDegreesList(java.util.List<String> data) {
        SwingUtilities.invokeLater(() -> {
            if (degTableModel == null) return;
            degTableModel.setRowCount(0);
            for (String row : data) {
                String[] parts = row.split("\\|");
                // Giả định server gửi: Tên bằng | Chuyên ngành | Trường | Năm | Tên_File | Trạng thái
                if (parts.length >= 6) {
                    String firstCol = "<html><div style='margin-left:5px;'><b style='color:#1E293B;'>" + parts[0] + "</b><br>"
                                    + "<span style='color:#64748B; font-size:10px;'>" + parts[1] + "</span></div></html>";
                    String university = parts[2];
                    String year = parts[3];
                    String classification = "Khá"; 
                    
                    // ĐÃ SỬA: Giữ nguyên tên file gốc từ Server, KHÔNG dùng substring cắt chuỗi nữa
                    String fileName = parts[4];
                    
                    String status = parts[5];
                    
                    // Thêm mảng 7 phần tử (Thêm fileName nguyên bản vào vị trí index 4)
                    degTableModel.addRow(new Object[]{firstCol, university, year, classification, fileName, status, ""});
                }
            }
        });
    }

    public void loadCertificatesList(java.util.List<String> data) {
        SwingUtilities.invokeLater(() -> {
            if (certTableModel == null) return;
            certTableModel.setRowCount(0);
            for (String row : data) {
                String[] parts = row.split("\\|");
                if (parts.length >= 6) {
                    // ĐÃ SỬA: Giữ nguyên tên file gốc từ Server, KHÔNG dùng substring cắt chuỗi nữa
                    String fileName = parts[4];
                    
                    // Thêm phần tử thứ 7 ("") cho cột Thao tác
                    certTableModel.addRow(new Object[]{parts[0], parts[1], parts[2], parts[3], fileName, parts[5], ""});
                }
            }
        });
    }
    

   public void loadExperiencesList(java.util.List<String> data) { 
        SwingUtilities.invokeLater(() -> { 
            if (timelineListPanel == null) return; 
            
            // 1. Xóa sạch dữ liệu hiển thị cũ
            timelineListPanel.removeAll(); 
            
            // 2. Nếu server trả về rỗng
            if (data == null || data.isEmpty()) {
                JLabel lblEmpty = new JLabel("Chưa có kinh nghiệm giảng dạy nào được cập nhật.");
                lblEmpty.setFont(new Font("Segoe UI", Font.ITALIC, 13));
                lblEmpty.setForeground(TEXT_MUTED);
                timelineListPanel.add(lblEmpty);
            } 
            // 3. Nếu có dữ liệu từ server
            else {
                for (String row : data) { 
                    // YÊU CẦU SERVER GỬI CHUỖI THEO ĐỊNH DẠNG SAU:
                    // Thời gian | Tiêu đề | Trạng thái | Loại hình | Khu vực | Mô tả | Môn1,Môn2...
                    // VD: "2020 - 2022|Gia sư Toán Online|Đã duyệt|Tự do|Đà Nẵng|Dạy Toán lớp 10|Toán,THPT"
                    String[] parts = row.split("\\|"); 
                    
                    String time = parts.length > 0 ? parts[0] : "";
                    String title = parts.length > 1 ? parts[1] : "Kinh nghiệm giảng dạy";
                    String status = parts.length > 2 ? parts[2] : "Chờ duyệt";
                    String type = parts.length > 3 ? parts[3] : "Cá nhân";
                    String loc = parts.length > 4 ? parts[4] : "Không rõ";
                    String desc = parts.length > 5 ? parts[5].replace("\\n", "\n") : "";
                    String tagsStr = parts.length > 6 ? parts[6] : "";
                    String[] tags = tagsStr.trim().isEmpty() ? new String[0] : tagsStr.split(",");
                    
                    // Render card mới với dữ liệu thực
                    timelineListPanel.add(createTimelineCard(time, title, status, type, loc, desc, tags)); 
                    timelineListPanel.add(Box.createVerticalStrut(15));
                } 
            }
            
            // 4. Refresh lại giao diện để nó vẽ lại
            timelineListPanel.revalidate();
            timelineListPanel.repaint();
        }); 
    }
   private void showImageDialog(byte[] imageData, String title) {
        try {
            Image img = new ImageIcon(imageData).getImage();
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
            
            // Giới hạn kích thước hiển thị tối đa để không tràn màn hình (vd: 800x600)
            int maxW = 800; int maxH = 600;
            int imgW = img.getWidth(null); int imgH = img.getHeight(null);
            
            if (imgW > maxW || imgH > maxH) {
                double scale = Math.min((double) maxW / imgW, (double) maxH / imgH);
                img = img.getScaledInstance((int)(imgW * scale), (int)(imgH * scale), Image.SCALE_SMOOTH);
            }
            
            JLabel lblImage = new JLabel(new ImageIcon(img));
            lblImage.setHorizontalAlignment(SwingConstants.CENTER);
            lblImage.setBorder(new EmptyBorder(10, 10, 10, 10));
            
            JScrollPane scroll = new JScrollPane(lblImage);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(Color.decode("#F8FAFC"));
            
            dialog.getContentPane().add(scroll, BorderLayout.CENTER);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Không thể hiển thị ảnh!");
        }
    }
   // --- HÀM TẢI XUỐNG FILE TỪ SERVER ---
   // --- HÀM TẢI XUỐNG FILE TỪ SERVER (ĐÃ SỬA LỖI RACE CONDITION) ---
    private void downloadAttachedFile(String rawFileName) {
        if (rawFileName == null || rawFileName.isEmpty() || rawFileName.equals("-") || rawFileName.equals("NO_FILE")) return;
        
        String cleanFileName = rawFileName.replaceAll("<[^>]*>", "");
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Lưu tệp đính kèm");
        fileChooser.setSelectedFile(new File(cleanFileName));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            // Lưu đường dẫn người dùng chọn vào biến tạm để luồng ngầm sử dụng sau
            pendingDownloadFile = fileChooser.getSelectedFile();
            
            new Thread(() -> {
                try {
                    // CHỈ GỬI YÊU CẦU, KHÔNG GỌI receivePacket() Ở ĐÂY NỮA
                    NetworkManager.getInstance().sendPacket(new Packet("DOWNLOAD_FILE", cleanFileName));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi gửi yêu cầu tải: " + ex.getMessage(), "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE));
                }
            }).start();
        }
    }
}
