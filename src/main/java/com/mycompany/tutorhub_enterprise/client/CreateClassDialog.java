package com.mycompany.tutorhub_enterprise.client;

import com.mycompany.tutorhub_enterprise.models.Packet; 

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

public class CreateClassDialog extends JDialog {

    private CardLayout stepLayout;
    private JPanel stepContainer;
    private int currentStep = 1;
    private Runnable onSuccessCallback; // Hàm gọi ngược để refresh bảng sau khi tạo xong

    // Data State
    private String selectedType = "Gia sư tại nhà";
    private String selectedIconUrl = "https://img.icons8.com/color/96/home.png";
    private String selectedDesc = "Dạy kèm 1:1 tại địa điểm của học viên";

    // Form Fields
    private JTextField txtTitle, txtSubject, txtTuition, txtLocation, txtSchedule;
    private JTextArea txtDesc;
    
    // UI Elements
    private JPanel stepperPanel;
    private JPanel livePreviewContainer;
    private JLabel lblSelectedTypeTitle, lblSelectedTypeDesc, lblSelectedTypeIcon;
    private List<TypeCard> typeCards = new ArrayList<>();

    // Colors
    private final Color PRIMARY = Color.decode("#246AF3");
    private final Color TEXT_DARK = Color.decode("#111827");
    private final Color TEXT_MUTED = Color.decode("#6B7280");
    private final Color BORDER_COLOR = Color.decode("#E5E7EB");
    private final Color BG_WHITE = Color.WHITE;

    public CreateClassDialog(Frame owner, Runnable onSuccessCallback) {
        super(owner, "", true);
        this.onSuccessCallback = onSuccessCallback;
        setUndecorated(true);
        setSize(1100, 800); // Tăng nhẹ height để chứa thêm field Lịch học
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        setBackground(new Color(0, 0, 0, 0)); 

        // Main Wrapper with Rounded Corners
        JPanel mainWrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(BORDER_COLOR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        mainWrapper.setOpaque(false);
        add(mainWrapper, BorderLayout.CENTER);

        // ==========================================
        // HEADER
        // ==========================================
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);
        topHeader.setBorder(new EmptyBorder(25, 40, 0, 40));
        
        JLabel lblMiniTitle = new JLabel("Hệ thống vận hành Trung tâm");
        lblMiniTitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblMiniTitle.setForeground(TEXT_MUTED);
        topHeader.add(lblMiniTitle, BorderLayout.WEST);

        JLabel btnClose = new JLabel("✕");
        btnClose.setFont(new Font("Arial", Font.PLAIN, 20));
        btnClose.setForeground(Color.decode("#9CA3AF"));
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { dispose(); }
            @Override public void mouseEntered(MouseEvent e) { btnClose.setForeground(Color.RED); }
            @Override public void mouseExited(MouseEvent e) { btnClose.setForeground(Color.decode("#9CA3AF")); }
        });
        topHeader.add(btnClose, BorderLayout.EAST);
        mainWrapper.add(topHeader, BorderLayout.NORTH);

        // ==========================================
        // TITLE & STEPPER
        // ==========================================
        JPanel contentArea = new JPanel(new BorderLayout());
        contentArea.setOpaque(false);

        JPanel titleAndStepper = new JPanel(new BorderLayout());
        titleAndStepper.setOpaque(false);
        titleAndStepper.setBorder(new EmptyBorder(15, 40, 20, 40));

        JLabel lblMainTitle = new JLabel("Khởi tạo Lớp mới");
        lblMainTitle.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblMainTitle.setForeground(TEXT_DARK);
        titleAndStepper.add(lblMainTitle, BorderLayout.WEST);

        stepperPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        stepperPanel.setOpaque(false);
        updateStepperUI();
        titleAndStepper.add(stepperPanel, BorderLayout.EAST);

        contentArea.add(titleAndStepper, BorderLayout.NORTH);

        // ==========================================
        // STEPS CONTAINER
        // ==========================================
        stepLayout = new CardLayout();
        stepContainer = new JPanel(stepLayout);
        stepContainer.setOpaque(false);

        stepContainer.add(createStep1(), "Step1");
        stepContainer.add(createStep2(), "Step2");

        contentArea.add(stepContainer, BorderLayout.CENTER);
        mainWrapper.add(contentArea, BorderLayout.CENTER);
    }

    private void updateStepperUI() {
        stepperPanel.removeAll();
        boolean isStep2 = (currentStep == 2);

        if (isStep2) {
            stepperPanel.add(createStepBadge("✓", PRIMARY, Color.WHITE, true));
            JLabel lbl1 = new JLabel("Chọn loại hình"); lbl1.setFont(new Font("Segoe UI", Font.PLAIN, 14)); lbl1.setForeground(TEXT_MUTED);
            stepperPanel.add(lbl1);
        } else {
            stepperPanel.add(createStepBadge("1", PRIMARY, Color.WHITE, false));
            JLabel lbl1 = new JLabel("Chọn loại hình"); lbl1.setFont(new Font("Segoe UI", Font.BOLD, 14)); lbl1.setForeground(PRIMARY);
            stepperPanel.add(lbl1);
        }

        JLabel divider = new JLabel("  ──────  "); divider.setForeground(BORDER_COLOR);
        stepperPanel.add(divider);

        stepperPanel.add(createStepBadge("2", isStep2 ? PRIMARY : Color.decode("#F3F4F6"), isStep2 ? Color.WHITE : TEXT_MUTED, false));
        JLabel lbl2 = new JLabel("Thông tin lớp & xem trước");
        lbl2.setFont(new Font("Segoe UI", isStep2 ? Font.BOLD : Font.PLAIN, 14));
        lbl2.setForeground(isStep2 ? PRIMARY : TEXT_MUTED);
        stepperPanel.add(lbl2);

        stepperPanel.revalidate(); stepperPanel.repaint();
    }

    private JPanel createStepBadge(String text, Color bgColor, Color fgColor, boolean isCheck) {
        JPanel badge = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor); g2.fillOval(0, 0, 24, 24);
                if (isCheck) {
                    g2.setColor(fgColor); g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(7, 12, 11, 16); g2.drawLine(11, 16, 17, 9);
                }
                g2.dispose();
            }
        };
        badge.setOpaque(false); badge.setPreferredSize(new Dimension(24, 24));
        if (!isCheck) {
            JLabel lbl = new JLabel(text, SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12)); lbl.setForeground(fgColor);
            badge.add(lbl, BorderLayout.CENTER);
        }
        return badge;
    }

    private JPanel createStep1() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JPanel cardGrid = new JPanel(new GridLayout(1, 3, 24, 0));
        cardGrid.setOpaque(false);
        cardGrid.setBorder(new EmptyBorder(10, 50, 30, 50));

        typeCards.add(new TypeCard("Gia sư tại nhà", "Dạy kèm 1:1 tại địa điểm\ncủa học viên", "https://img.icons8.com/color/96/home.png", true));
        typeCards.add(new TypeCard("Lớp online", "Học trực tuyến qua\nVideo Call / Jitsi", "https://img.icons8.com/color/96/video-call--v1.png", false));
        typeCards.add(new TypeCard("Nhóm học", "Dành cho nhóm từ\n3 người trở lên", "https://img.icons8.com/color/96/people-working-together.png", false));

        for (TypeCard card : typeCards) cardGrid.add(card);
        p.add(cardGrid, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout()); footer.setOpaque(false); footer.setBorder(new EmptyBorder(0, 50, 40, 50));
        JLabel lblInfo = new JLabel("ⓘ Chọn loại hình phù hợp để tiếp tục tạo lớp học", SwingConstants.CENTER);
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 14)); lblInfo.setForeground(TEXT_MUTED); lblInfo.setBorder(new EmptyBorder(0, 0, 20, 0));
        footer.add(lblInfo, BorderLayout.NORTH);

        JPanel btnGroup = new JPanel(new BorderLayout()); btnGroup.setOpaque(false);
        JButton btnBack = createCustomButton("❮ Hủy bỏ", false); btnBack.addActionListener(e -> dispose());
        JButton btnNext = createCustomButton("Tiếp tục ❯", true);
        btnNext.addActionListener(e -> {
            lblSelectedTypeTitle.setText(selectedType);
            lblSelectedTypeDesc.setText(selectedDesc);
            setNetworkIcon(lblSelectedTypeIcon, selectedIconUrl, 48, 48);
            currentStep = 2; updateStepperUI(); stepLayout.show(stepContainer, "Step2");
        });

        btnGroup.add(btnBack, BorderLayout.WEST); btnGroup.add(btnNext, BorderLayout.EAST);
        footer.add(btnGroup, BorderLayout.CENTER);
        p.add(footer, BorderLayout.SOUTH);
        return p;
    }

    class TypeCard extends JPanel {
        private String title, desc, iconUrl;
        private boolean isSelected;
        
        public TypeCard(String title, String desc, String iconUrl, boolean selected) {
            this.title = title; this.desc = desc; this.iconUrl = iconUrl; this.isSelected = selected;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(false); setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(40, 20, 30, 20));

            JLabel lblIcon = new JLabel(); lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT); setNetworkIcon(lblIcon, iconUrl, 100, 100);
            JLabel lblTitle = new JLabel(title); lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22)); lblTitle.setForeground(TEXT_DARK); lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel lblDesc = new JLabel("<html><div style='text-align: center; line-height: 1.4;'>" + desc.replace("\n", "<br>") + "</div></html>"); lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 14)); lblDesc.setForeground(TEXT_MUTED); lblDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel checkmark = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (isSelected) {
                        g2.setColor(PRIMARY); g2.fillOval(0, 0, 28, 28);
                        g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.drawLine(8, 14, 13, 19); g2.drawLine(13, 19, 20, 10);
                    } else {
                        g2.setColor(Color.WHITE); g2.fillOval(0, 0, 28, 28);
                        g2.setColor(Color.decode("#D1D5DB")); g2.setStroke(new BasicStroke(1.5f)); g2.drawOval(1, 1, 26, 26);
                    } g2.dispose();
                }
            };
            checkmark.setOpaque(false); checkmark.setMinimumSize(new Dimension(30, 30)); checkmark.setPreferredSize(new Dimension(30, 30)); checkmark.setMaximumSize(new Dimension(30, 30)); checkmark.setAlignmentX(Component.CENTER_ALIGNMENT);

            add(lblIcon); add(Box.createVerticalStrut(30));
            add(lblTitle); add(Box.createVerticalStrut(15));
            add(lblDesc); add(Box.createVerticalGlue());
            add(checkmark);

            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    for (TypeCard c : typeCards) c.setSelected(false);
                    setSelected(true); selectedType = title; selectedIconUrl = iconUrl; selectedDesc = desc.replace("\n", " ");
                }
            });
        }
        public void setSelected(boolean s) { this.isSelected = s; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isSelected ? Color.decode("#F5F8FF") : Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g2.setColor(isSelected ? PRIMARY : BORDER_COLOR); g2.setStroke(new BasicStroke(isSelected ? 2f : 1.2f)); g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 20, 20);
            g2.dispose(); super.paintComponent(g);
        }
    }

    private JPanel createStep2() {
        JPanel mainP = new JPanel(new BorderLayout(20, 0));
        mainP.setOpaque(false); mainP.setBorder(new EmptyBorder(0, 40, 20, 40));

        JPanel formWrapper = new JPanel(new BorderLayout()); formWrapper.setOpaque(false);
        JPanel formContainer = new JPanel(); formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS)); formContainer.setOpaque(false);
        formContainer.setBorder(new EmptyBorder(0, 0, 20, 10));

        JPanel selectedTypeBox = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode("#F9FAFB")); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(BORDER_COLOR); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16); g2.dispose();
            }
        };
        selectedTypeBox.setOpaque(false); selectedTypeBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80)); selectedTypeBox.setBorder(new EmptyBorder(12, 16, 12, 16));
        
        lblSelectedTypeIcon = new JLabel(); setNetworkIcon(lblSelectedTypeIcon, selectedIconUrl, 48, 48);
        JPanel typeInfo = new JPanel(); typeInfo.setLayout(new BoxLayout(typeInfo, BoxLayout.Y_AXIS)); typeInfo.setOpaque(false); typeInfo.setBorder(new EmptyBorder(0, 15, 0, 0));
        JLabel lblLbl = new JLabel("Loại hình đã chọn"); lblLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblLbl.setForeground(TEXT_MUTED);
        lblSelectedTypeTitle = new JLabel(selectedType); lblSelectedTypeTitle.setFont(new Font("Segoe UI", Font.BOLD, 15)); lblSelectedTypeTitle.setForeground(TEXT_DARK);
        lblSelectedTypeDesc = new JLabel(selectedDesc); lblSelectedTypeDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblSelectedTypeDesc.setForeground(TEXT_MUTED);
        typeInfo.add(lblLbl); typeInfo.add(lblSelectedTypeTitle); typeInfo.add(lblSelectedTypeDesc);
        
        JLabel btnChange = new JLabel("✏ Đổi loại hình"); btnChange.setFont(new Font("Segoe UI", Font.BOLD, 13)); btnChange.setForeground(PRIMARY); btnChange.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnChange.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { currentStep = 1; updateStepperUI(); stepLayout.show(stepContainer, "Step1"); }});
        
        selectedTypeBox.add(lblSelectedTypeIcon, BorderLayout.WEST); selectedTypeBox.add(typeInfo, BorderLayout.CENTER); selectedTypeBox.add(btnChange, BorderLayout.EAST);
        formContainer.add(selectedTypeBox); formContainer.add(Box.createVerticalStrut(20));

        txtTitle = createInputBox(formContainer, "Tiêu đề lớp học *", "Ví dụ: Lớp Toán 11 Cơ bản", "0/80", null);
        txtSubject = createInputBox(formContainer, "Môn học *", "Toán học, Vật lý...", null, null);

        JLabel lblDescTitle = new JLabel("Mô tả nhu cầu *"); lblDescTitle.setFont(new Font("Segoe UI", Font.BOLD, 13)); lblDescTitle.setForeground(TEXT_DARK);
        formContainer.add(lblDescTitle); formContainer.add(Box.createVerticalStrut(8));
        
        JPanel descWrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(BORDER_COLOR); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12); g2.dispose();
            }
        };
        descWrapper.setOpaque(false); descWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100)); descWrapper.setBorder(new EmptyBorder(8, 12, 8, 12));
        txtDesc = new JTextArea(3, 20); txtDesc.setFont(new Font("Segoe UI", Font.PLAIN, 14)); txtDesc.setForeground(TEXT_DARK); txtDesc.setLineWrap(true); txtDesc.setWrapStyleWord(true); txtDesc.setBorder(null);
        txtDesc.setText("Nhập mô tả chi tiết về lớp học..."); txtDesc.setForeground(Color.decode("#9CA3AF"));
        txtDesc.addFocusListener(new java.awt.event.FocusAdapter() { public void focusGained(java.awt.event.FocusEvent evt) { if(txtDesc.getText().startsWith("Nhập")) {txtDesc.setText(""); txtDesc.setForeground(TEXT_DARK);} } });
        
        descWrapper.add(new JScrollPane(txtDesc){{setBorder(null); setOpaque(false); getViewport().setOpaque(false);}}, BorderLayout.CENTER);
        formContainer.add(descWrapper); formContainer.add(Box.createVerticalStrut(20));

        // Bổ sung Lịch học
        txtSchedule = createInputBox(formContainer, "Lịch học dự kiến *", "Thứ 2, 4, 6 - 19:00", null, "https://img.icons8.com/fluency-systems-regular/48/9CA3AF/clock--v1.png");
        txtLocation = createInputBox(formContainer, "Địa điểm / Nền tảng học *", "Quận Cầu Giấy / Google Meet", null, "https://img.icons8.com/fluency-systems-regular/48/9CA3AF/marker.png");
        txtTuition = createInputBox(formContainer, "Học phí (VNĐ/buổi) *", "250.000", "đ", null);

        JScrollPane scrollForm = new JScrollPane(formContainer); scrollForm.setBorder(null); scrollForm.setOpaque(false); scrollForm.getViewport().setOpaque(false); scrollForm.getVerticalScrollBar().setUnitIncrement(16);
        formWrapper.add(scrollForm, BorderLayout.CENTER);

        JPanel formFooter = new JPanel(new BorderLayout()); formFooter.setOpaque(false); formFooter.setBorder(new EmptyBorder(15, 0, 10, 0));
        JButton btnBack2 = createCustomButton("❮ Quay lại", false); btnBack2.addActionListener(e -> { currentStep = 1; updateStepperUI(); stepLayout.show(stepContainer, "Step1"); });
        
        JButton btnSubmit = createCustomButton("Tạo lớp ngay", true); 
        btnSubmit.addActionListener(e -> { 
            String title = txtTitle.getText().trim();
            String subj = txtSubject.getText().trim();
            String tuition = txtTuition.getText().trim();
            String loc = txtLocation.getText().trim();
            String desc = txtDesc.getText().trim();
            String schedule = txtSchedule.getText().trim(); // Lấy data Lịch học

            if(title.isEmpty() || subj.isEmpty() || loc.isEmpty() || schedule.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin bắt buộc (*)", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Gửi dữ liệu: Gộp schedule vào cuối payload
            String payload = selectedType + "|" + subj + "|" + tuition + "|" + loc + "|" + title + "|" + desc + "|" + schedule;
            try {
                NetworkManager.getInstance().sendPacket(new Packet("CREATE_CLASS", payload));
                JOptionPane.showMessageDialog(this, "🎉 Đã khởi tạo lớp học thành công trên hệ thống!");
                dispose(); 
                if (onSuccessCallback != null) onSuccessCallback.run(); // Cập nhật lại UI ở CenterDashboard
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể kết nối đến máy chủ!", "Lỗi mạng", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        formFooter.add(btnBack2, BorderLayout.WEST); formFooter.add(btnSubmit, BorderLayout.EAST);
        formWrapper.add(formFooter, BorderLayout.SOUTH);

        mainP.add(formWrapper, BorderLayout.CENTER);

        // --- CỘT PHẢI: LIVE PREVIEW ---
        JPanel previewPanel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode("#F9FAFB")); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24); g2.dispose();
            }
        };
        previewPanel.setOpaque(false);
        previewPanel.setPreferredSize(new Dimension(380, 0));
        previewPanel.setBorder(new EmptyBorder(25, 30, 40, 30));

        JLabel lblPreview = new JLabel("XEM TRƯỚC BẢNG TIN 👁", SwingConstants.LEFT);
        lblPreview.setFont(new Font("Segoe UI", Font.BOLD, 12)); lblPreview.setForeground(TEXT_MUTED); lblPreview.setBorder(new EmptyBorder(0,0,20,0));
        previewPanel.add(lblPreview, BorderLayout.NORTH);

        livePreviewContainer = new JPanel(new BorderLayout());
        livePreviewContainer.setOpaque(false);
        updatePreview(); 
        previewPanel.add(livePreviewContainer, BorderLayout.CENTER);

        JLabel lblPreviewInfo = new JLabel("ⓘ Giao diện hiển thị trên bảng tin gia sư.", SwingConstants.CENTER);
        lblPreviewInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblPreviewInfo.setForeground(TEXT_MUTED); lblPreviewInfo.setBorder(new EmptyBorder(15, 0, 0, 0));
        previewPanel.add(lblPreviewInfo, BorderLayout.SOUTH);

        JPanel splitWrapper = new JPanel(new BorderLayout(20, 0));
        splitWrapper.setOpaque(false);
        splitWrapper.setBorder(new EmptyBorder(0, 0, 0, 40));
        splitWrapper.add(formWrapper, BorderLayout.CENTER);
        splitWrapper.add(previewPanel, BorderLayout.EAST);

        mainP.add(splitWrapper, BorderLayout.CENTER);

        DocumentListener sync = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updatePreview(); }
            public void removeUpdate(DocumentEvent e) { updatePreview(); }
            public void changedUpdate(DocumentEvent e) { updatePreview(); }
        };
        txtTitle.getDocument().addDocumentListener(sync); txtSubject.getDocument().addDocumentListener(sync);
        txtLocation.getDocument().addDocumentListener(sync); txtTuition.getDocument().addDocumentListener(sync);
        txtSchedule.getDocument().addDocumentListener(sync); // Đồng bộ Lịch học

        return mainP;
    }

    private JTextField createInputBox(JPanel parent, String label, String placeholder, String rightText, String rightIconUrl) {
        JLabel lbl = new JLabel(label); lbl.setFont(new Font("Segoe UI", Font.BOLD, 13)); lbl.setForeground(TEXT_DARK);
        parent.add(lbl); parent.add(Box.createVerticalStrut(8));
        
        JPanel inputWrapper = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(BORDER_COLOR); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12); g2.dispose();
            }
        };
        inputWrapper.setOpaque(false); inputWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46)); inputWrapper.setBorder(new EmptyBorder(0, 15, 0, 15));
        
        JTextField field = new JTextField(); field.setBorder(null); field.setOpaque(false); field.setFont(new Font("Segoe UI", Font.PLAIN, 15)); field.setForeground(TEXT_DARK); field.putClientProperty("JTextField.placeholderText", placeholder);
        inputWrapper.add(field, BorderLayout.CENTER);

        if (rightText != null) {
            JLabel lblRight = new JLabel(rightText); lblRight.setFont(new Font("Segoe UI", Font.PLAIN, 13)); lblRight.setForeground(TEXT_MUTED); inputWrapper.add(lblRight, BorderLayout.EAST);
        } else if (rightIconUrl != null) {
            JLabel iconRight = new JLabel(); setNetworkIcon(iconRight, rightIconUrl, 20, 20); inputWrapper.add(iconRight, BorderLayout.EAST);
        }

        parent.add(inputWrapper); parent.add(Box.createVerticalStrut(20));
        return field;
    }

    private void updatePreview() {
        livePreviewContainer.removeAll();
        String title = txtTitle.getText().trim().isEmpty() ? "Lớp Toán học 11" : txtTitle.getText();
        String subj = txtSubject.getText().trim().isEmpty() ? "Môn học..." : txtSubject.getText();
        String tuition = txtTuition.getText().trim().isEmpty() ? "0" : txtTuition.getText();
        String loc = txtLocation.getText().trim().isEmpty() ? "Địa điểm..." : txtLocation.getText();
        String schedule = txtSchedule.getText().trim().isEmpty() ? "Thời gian..." : txtSchedule.getText();

        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(BORDER_COLOR); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 24, 24); g2.dispose();
            }
        };
        card.setOpaque(false);

        String imgPath = getSubjectImagePath(subj);
        Image bannerImg = null;
        try {
            URL url = getClass().getResource(imgPath);
            if (url != null) bannerImg = new ImageIcon(url).getImage();
        } catch (Exception ignored) {}
        final Image finalImg = bannerImg;

        JPanel banner = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); 
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (finalImg != null) {
                    Shape clip = new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight() + 24, 24, 24);
                    g2.setClip(clip);
                    double scale = Math.max((double) getWidth() / finalImg.getWidth(null), (double) getHeight() / finalImg.getHeight(null));
                    int drawW = (int) (finalImg.getWidth(null) * scale); int drawH = (int) (finalImg.getHeight(null) * scale);
                    int x = (getWidth() - drawW) / 2; int y = (getHeight() - drawH) / 2;
                    g2.drawImage(finalImg, x, y, drawW, drawH, null);
                    g2.setColor(new Color(17, 24, 39, 70)); 
                    g2.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    g2.setColor(Color.decode("#475569")); 
                    g2.fillRoundRect(0, 0, getWidth(), getHeight() + 24, 24, 24);
                }
                g2.dispose();
            }
        };
        banner.setOpaque(false); banner.setPreferredSize(new Dimension(0, 126)); banner.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        JPanel topTags = new JPanel(new BorderLayout()); topTags.setOpaque(false);
        topTags.add(createBadge("MỚI", "#10B981", Color.WHITE), BorderLayout.WEST);
        topTags.add(createBadge("● Chờ gia sư", "#FEF3C7", Color.decode("#D97706")), BorderLayout.EAST);
        banner.add(topTags, BorderLayout.NORTH);

        JPanel body = new JPanel(); body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS)); body.setOpaque(false); body.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); titleRow.setOpaque(false); titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel iconBook = new JLabel(); setNetworkIcon(iconBook, "https://img.icons8.com/fluency-systems-regular/48/246AF3/open-book.png", 22, 22);
        JLabel lblSubj = new JLabel(subj); lblSubj.setFont(new Font("Segoe UI", Font.BOLD, 18)); lblSubj.setForeground(TEXT_DARK);
        titleRow.add(iconBook); titleRow.add(lblSubj); body.add(titleRow); body.add(Box.createVerticalStrut(15));

        body.add(createPreviewRow("marker", loc, false)); body.add(Box.createVerticalStrut(10));
        body.add(createPreviewRow("clock--v1", schedule, false)); body.add(Box.createVerticalStrut(10)); // Thêm Lịch học vào Preview
        body.add(createPreviewRow("money-bag", tuition + "đ/buổi", true)); body.add(Box.createVerticalStrut(10));
        body.add(createPreviewRow("user", title, false));

        JPanel footer = new JPanel(new BorderLayout(15, 0)); footer.setOpaque(false); footer.setBorder(new EmptyBorder(0, 20, 20, 20));
        JButton btnAccept = createCustomButton("Phân công", true);
        footer.add(btnAccept, BorderLayout.CENTER);

        card.add(banner, BorderLayout.NORTH); card.add(body, BorderLayout.CENTER); card.add(footer, BorderLayout.SOUTH);
        
        livePreviewContainer.add(card, BorderLayout.NORTH);
        livePreviewContainer.revalidate(); livePreviewContainer.repaint();
    }

    private JPanel createBadge(String text, String bgColor, Color fgColor) {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode(bgColor)); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12); g2.dispose();
            }
        };
        p.setOpaque(false); p.setBorder(new EmptyBorder(4, 10, 4, 10));
        JLabel l = new JLabel(text); l.setFont(new Font("Segoe UI", Font.BOLD, 11)); l.setForeground(fgColor); p.add(l, BorderLayout.CENTER); return p;
    }

    private JPanel createPreviewRow(String icon, String text, boolean isGreen) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0)); p.setOpaque(false); p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblI = new JLabel(); setNetworkIcon(lblI, "https://img.icons8.com/fluency-systems-regular/48/" + (isGreen ? "059669" : "6B7280") + "/" + icon + ".png", 20, 20);
        JLabel lblT = new JLabel(text); lblT.setFont(new Font("Segoe UI", isGreen ? Font.BOLD : Font.PLAIN, 14)); lblT.setForeground(isGreen ? Color.decode("#059669") : TEXT_MUTED);
        p.add(lblI); p.add(lblT); return p;
    }

    private JButton createCustomButton(String text, boolean isPrimary) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isPrimary) { g2.setColor(Color.decode("#246AF3")); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12); } 
                else { g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12); g2.setColor(Color.decode("#D1D5DB")); g2.setStroke(new BasicStroke(1.2f)); g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 12, 12); }
                g2.dispose(); super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", isPrimary ? Font.BOLD : Font.PLAIN, 15)); btn.setForeground(isPrimary ? Color.WHITE : Color.decode("#374151")); btn.setContentAreaFilled(false); btn.setFocusPainted(false); btn.setBorderPainted(false); btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); btn.setPreferredSize(new Dimension(140, 46)); return btn;
    }

    private void setNetworkIcon(JLabel label, String urlStr, int width, int height) {
        new Thread(() -> { try { ImageIcon icon = new ImageIcon(new URL(urlStr)); Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH); SwingUtilities.invokeLater(() -> label.setIcon(new ImageIcon(img))); } catch (Exception ignored) {} }).start();
    }

    private String currentPreviewCategory = "";
    private String currentPreviewImagePath = "/images/general/general1.png";

    private String getSubjectImagePath(String subj) {
        if (subj == null || subj.trim().isEmpty()) return getStableImage("GENERAL");
        String s = subj.toLowerCase();
        if (s.contains("ielts")) return getStableImage("IELTS");
        if (s.contains("anh") || s.contains("toeic") || s.contains("toefl")) return getStableImage("ENGLISH");
        if (s.contains("toán") || s.contains("đại số") || s.contains("hình học") || s.contains("giải tích")) return getStableImage("MATH");
        if (s.contains("lý") || s.contains("vật lý") || s.contains("cơ học")) return getStableImage("PHYSICS");
        if (s.contains("hóa")) return getStableImage("CHEMISTRY");
        if (s.contains("văn") || s.contains("ngữ văn") || s.contains("tiếng việt")) return getStableImage("LITERATURE");
        if (s.contains("tin") || s.contains("lập trình") || s.contains("java") || s.contains("python") || s.contains("it")) return getStableImage("IT");
        return getStableImage("GENERAL");
    }

    private String getStableImage(String category) {
        if (!category.equals(currentPreviewCategory)) {
            currentPreviewCategory = category;
            java.util.Random rand = new java.util.Random();
            int index = rand.nextInt(6) + 1;
            switch(category) {
                case "IELTS": currentPreviewImagePath = "/images/IELTS/IELTS" + index + ".jpg"; break;
                case "ENGLISH": currentPreviewImagePath = "/images/english/english" + index + ".jpg"; break;
                case "MATH": currentPreviewImagePath = "/images/math/math" + index + ".jpg"; break;
                case "PHYSICS": currentPreviewImagePath = "/images/physics/physics" + index + ".jpg"; break;
                case "CHEMISTRY": currentPreviewImagePath = "/images/chemistry/chemistry" + index + ".jpg"; break;
                case "LITERATURE": currentPreviewImagePath = "/images/literature/literature" + index + ".jpg"; break;
                case "IT": currentPreviewImagePath = "/images/it/it" + index + ".jpg"; break;
                default: currentPreviewImagePath = "/images/general/general1.png"; break;
            }
        }
        return currentPreviewImagePath;
    }
}