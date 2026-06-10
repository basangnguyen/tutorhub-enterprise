package com.mycompany.tutorhub_enterprise.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class UpgradeDialog extends JDialog {

    private final Color PRIMARY = Color.decode("#6366F1"); // Màu tím Indigo chủ đạo
    private final Color PRIMARY_DARK = Color.decode("#4F46E5");
    private final Color TEXT_MAIN = Color.decode("#1E1B4B"); // Khớp với UpgradeTab
    private final Color TEXT_MUTED = Color.decode("#6B7280");
    private final Color BORDER = Color.decode("#E5E7EB");
    private final Color BG_LIGHT = Color.decode("#F8FAFC");
    private final Color SUCCESS = Color.decode("#10B981");

    // Bộ nhớ đệm Icon giúp giao diện load trong 0.1s
    private static final Map<String, ImageIcon> iconCache = new HashMap<>();

    private JLabel lblTotalAmount;
    private JLabel lblDiscountAmount;
    private JLabel lblTimer;
    private int timeLeft = 900; // 15 phút đếm ngược
    private Timer countdownTimer;
    
    private String packageName;
    private double basePrice;
    private double discount = 0;

    public UpgradeDialog(Frame parent, String packageName, double basePrice) {
        super(parent, "Thanh toán Nâng cấp", true);
        this.packageName = packageName;
        this.basePrice = basePrice;

        setSize(1000, 720);
        setLocationRelativeTo(parent);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        // --- KHUNG NỀN CHÍNH (Bo tròn 24px) ---
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        mainPanel.setOpaque(false);

        // --- NÚT ĐÓNG (Góc trên phải) ---
        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        closePanel.setOpaque(false);
        JLabel closeBtn = new JLabel();
        setIcon(closeBtn, "https://img.icons8.com/material-rounded/48/9CA3AF/delete-sign.png", 28, 28);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { dispose(); }
            @Override public void mouseEntered(MouseEvent e) { setIcon(closeBtn, "https://img.icons8.com/material-rounded/48/EF4444/delete-sign.png", 28, 28); }
            @Override public void mouseExited(MouseEvent e) { setIcon(closeBtn, "https://img.icons8.com/material-rounded/48/9CA3AF/delete-sign.png", 28, 28); }
        });
        closePanel.add(closeBtn);
        mainPanel.add(closePanel, BorderLayout.NORTH);

        // --- CHIA 2 CỘT (TRÁI / PHẢI) ---
        JPanel contentSplit = new JPanel(new BorderLayout());
        contentSplit.setOpaque(false);
        contentSplit.add(createLeftPanel(), BorderLayout.WEST);
        contentSplit.add(createRightPanel(), BorderLayout.CENTER);
        
        mainPanel.add(contentSplit, BorderLayout.CENTER);
        setContentPane(mainPanel);
        startTimer();
    }

    // ==========================================
    // CỘT TRÁI: THÔNG TIN GÓI KHỚP VỚI UPGRADETAB
    // ==========================================
    private JPanel createLeftPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        
        // 1. Tăng nhẹ chiều rộng để tạo không gian thở cho tên gói dài
        wrapper.setPreferredSize(new Dimension(450, 0)); 
        
        // 2. Thêm padding trên/dưới để nội dung không bị dính sát mép cửa sổ
        wrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, Color.decode("#F3F4F6")),
            new EmptyBorder(35, 35, 35, 35) 
        ));

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        // Tiêu đề chính
        JLabel title1 = new JLabel("Thanh toán gói " + packageName);
        title1.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title1.setForeground(TEXT_MAIN);
        title1.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel subTitle = new JLabel("Trải nghiệm toàn diện, nâng tầm cơ hội.");
        subTitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subTitle.setForeground(TEXT_MUTED);
        subTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(title1); 
        p.add(Box.createVerticalStrut(8)); 
        p.add(subTitle); 
        p.add(Box.createVerticalStrut(30));

        // --- KHUNG THẺ LỬNG CHỨA THÔNG TIN GÓI ---
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Đổ bóng tím nhạt phía sau giống hiệu ứng hover
                g2.setColor(new Color(139, 92, 246, 20));
                g2.fillRoundRect(2, 6, getWidth()-4, getHeight()-6, 24, 24);

                // Nền trắng tinh khiết
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                
                // Viền tím sáng biểu thị đang được chọn
                g2.setColor(PRIMARY);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));
        card.setBorder(new EmptyBorder(25, 25, 25, 25));

        // --- Nửa trên của Thẻ ---
        JPanel topHalf = new JPanel();
        topHalf.setLayout(new BoxLayout(topHalf, BoxLayout.Y_AXIS));
        topHalf.setOpaque(false);
        
        // Badge
        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)); 
        badgeWrap.setOpaque(false);
        JPanel badge = new JPanel(new BorderLayout()){ 
            @Override protected void paintComponent(Graphics g) { 
                Graphics2D g2 = (Graphics2D) g.create(); 
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
                g2.setColor(PRIMARY); 
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16); 
                g2.dispose(); 
            } 
        };
        badge.setOpaque(false); 
        badge.setBorder(new EmptyBorder(4, 12, 4, 12));
        JLabel lblBadge = new JLabel("GÓI BẠN CHỌN", SwingConstants.CENTER); 
        lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 10)); 
        lblBadge.setForeground(Color.WHITE);
        badge.add(lblBadge); badgeWrap.add(badge); topHalf.add(badgeWrap); 
        topHalf.add(Box.createVerticalStrut(15));

        // Tên gói & Giá
        JLabel lblName = new JLabel(packageName); 
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 24)); 
        lblName.setForeground(packageName.equals("VIP") ? Color.decode("#8B5CF6") : PRIMARY); 
        lblName.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JPanel pricePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0)); 
        pricePanel.setOpaque(false);
        JLabel lblPrice = new JLabel(String.format("%,.0fđ", basePrice).replace(",", ".")); 
        lblPrice.setFont(new Font("Segoe UI", Font.BOLD, 32)); 
        lblPrice.setForeground(TEXT_MAIN);
        JLabel lblMonth = new JLabel("/ tháng"); 
        lblMonth.setFont(new Font("Segoe UI", Font.PLAIN, 14)); 
        lblMonth.setForeground(TEXT_MUTED);
        pricePanel.add(lblPrice); pricePanel.add(lblMonth);

        topHalf.add(lblName); 
        topHalf.add(Box.createVerticalStrut(15)); 
        topHalf.add(pricePanel); 
        topHalf.add(Box.createVerticalStrut(25));
        card.add(topHalf, BorderLayout.NORTH);

        // --- Nửa dưới (Tính năng động theo packageName) ---
        JPanel featureList = new JPanel(); 
        featureList.setLayout(new BoxLayout(featureList, BoxLayout.Y_AXIS)); 
        featureList.setOpaque(false);

        // Mảng object động chứa thông tin gói tương ứng với bảng giá trong UpgradeTab
        Object[][] features;
        switch (packageName) {
            case "Basic":
                features = new Object[][]{
                    {"Nhận tối đa 15 lớp / tháng", true}, 
                    {"Tìm kiếm lớp học nâng cao", true}, 
                    {"Nhắn tin không giới hạn", true},
                    {"Ưu tiên hiển thị lớp học", true}, 
                    {"Thống kê thu nhập chi tiết", false}, 
                    {"Hỗ trợ ưu tiên", false}
                };
                break;
            case "Premium":
                features = new Object[][]{
                    {"Nhận không giới hạn lớp", true}, 
                    {"Tìm kiếm lớp học nâng cao", true}, 
                    {"Nhắn tin không giới hạn", true},
                    {"Ưu tiên hiển thị lớp học", true}, 
                    {"Thống kê thu nhập chi tiết", true}, 
                    {"Hỗ trợ ưu tiên 24/7", true}
                };
                break;
            case "VIP":
                features = new Object[][]{
                    {"Nhận không giới hạn lớp", true}, 
                    {"Tìm kiếm lớp học nâng cao", true}, 
                    {"Nhắn tin không giới hạn", true},
                    {"Ưu tiên hiển thị lớp học (Top)", true}, 
                    {"Thống kê thu nhập chi tiết", true}, 
                    {"Hỗ trợ trợ riêng 1-1", true}
                };
                break;
            default: // Miễn phí hoặc mặc định
                features = new Object[][]{
                    {"Nhận tối đa 3 lớp / tháng", true}, 
                    {"Tìm kiếm lớp học cơ bản", true}, 
                    {"Nhắn tin với phụ huynh", true},
                    {"Ưu tiên hiển thị lớp học", false}, 
                    {"Thống kê thu nhập chi tiết", false}, 
                    {"Hỗ trợ ưu tiên", false}
                };
                break;
        }

        for (Object[] feat : features) {
            String text = (String) feat[0];
            boolean isCheck = (boolean) feat[1];

            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8)); 
            row.setOpaque(false); 
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            JLabel icon = new JLabel(); 
            // Nếu true hiện dấu check tím, false hiện dấu x xám (Khớp với UpgradeTab)
            String iconUrl = isCheck ? "https://img.icons8.com/fluency-systems-regular/48/8B5CF6/checkmark.png" 
                                     : "https://img.icons8.com/fluency-systems-regular/48/9CA3AF/multiply.png";
            setIcon(icon, iconUrl, 16, 16);
            
            JLabel lblFeat = new JLabel(text); 
            lblFeat.setFont(new Font("Segoe UI", Font.PLAIN, 13)); 
            lblFeat.setForeground(isCheck ? TEXT_MAIN : TEXT_MUTED); 
            
            row.add(icon); 
            row.add(lblFeat); 
            featureList.add(row);
        }
        card.add(featureList, BorderLayout.CENTER);

        p.add(card);
        p.add(Box.createVerticalGlue());

        // --- Footer bảo mật ---
        JPanel secureRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        secureRow.setOpaque(false);
        secureRow.setAlignmentX(Component.LEFT_ALIGNMENT); 
        
        JLabel lockIcon = new JLabel(); 
        setIcon(lockIcon, "https://img.icons8.com/fluency-systems-regular/48/9CA3AF/lock.png", 16, 16);
        lockIcon.setBorder(new EmptyBorder(0, 0, 0, 8)); 
        
        JLabel lblSecure = new JLabel("Thanh toán bảo mật với SSL 256-bit");
        lblSecure.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSecure.setForeground(TEXT_MUTED);
        
        secureRow.add(lockIcon); 
        secureRow.add(lblSecure);
        p.add(secureRow);

        wrapper.add(p, BorderLayout.CENTER);
        return wrapper;
    }

    // ==========================================
    // CỘT PHẢI: LUỒNG THANH TOÁN
    // ==========================================
    private JPanel createRightPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 30, 30, 30));

        JPanel centerContent = new JPanel();
        centerContent.setLayout(new BoxLayout(centerContent, BoxLayout.Y_AXIS));
        centerContent.setOpaque(false);

        // 1. Progress Steps (Breadcrumb)
        JPanel stepsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        stepsPanel.setOpaque(false);
        stepsPanel.add(createStepLbl("1. Chọn gói", TEXT_MUTED, Font.PLAIN));
        stepsPanel.add(createStepLbl(">", TEXT_MUTED, Font.PLAIN));
        stepsPanel.add(createStepLbl("2. Thanh toán", PRIMARY, Font.BOLD));
        stepsPanel.add(createStepLbl(">", TEXT_MUTED, Font.PLAIN));
        stepsPanel.add(createStepLbl("3. Hoàn tất", TEXT_MUTED, Font.PLAIN));
        centerContent.add(stepsPanel); centerContent.add(Box.createVerticalStrut(20));

        // 2. Chữ Phương thức thanh toán được làm đẹp
        JPanel methodHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        methodHeader.setOpaque(false);
        JLabel iconMethod = new JLabel(); setIcon(iconMethod, "https://img.icons8.com/fluency-systems-filled/48/6366F1/wallet.png", 22, 22);
        JLabel lblMethod = new JLabel("Phương thức thanh toán"); 
        lblMethod.setFont(new Font("Segoe UI", Font.BOLD, 16)); 
        lblMethod.setForeground(TEXT_MAIN);
        methodHeader.add(iconMethod); methodHeader.add(lblMethod);
        centerContent.add(methodHeader); centerContent.add(Box.createVerticalStrut(10));
        
        // --- Nút thanh toán ---
        JPanel methodsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0)); methodsRow.setOpaque(false);
        methodsRow.add(createPaymentMethodBtn("QR Banking", "https://img.icons8.com/color/48/bank-building.png", true, null));
        methodsRow.add(createPaymentMethodBtn("MoMo", "/images/momo.png", false, "https://momo.vn/"));
        methodsRow.add(createPaymentMethodBtn("ZaloPay", "/images/zalo.png", false, "https://zalopay.vn/"));
        centerContent.add(methodsRow); centerContent.add(Box.createVerticalStrut(20));

        // 3. Khung QR Code & Chuyển khoản
        JPanel transferBox = new JPanel(new BorderLayout(25, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_LIGHT); 
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(BORDER); 
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16); g2.dispose();
            }
        };
        transferBox.setOpaque(false);
        transferBox.setBorder(new EmptyBorder(25, 25, 25, 25));

        // Ảnh QR Local
        JPanel qrPanel = new JPanel(new BorderLayout()); qrPanel.setOpaque(false);
        JLabel qrImage = new JLabel(); 
        setIcon(qrImage, "/images/qr_code.png", 140, 140);
        qrImage.setHorizontalAlignment(SwingConstants.CENTER);
        qrPanel.add(qrImage, BorderLayout.CENTER);
        
        JLabel lblNapas = new JLabel(); setIcon(lblNapas, "https://img.icons8.com/color/48/napas.png", 60, 20);
        lblNapas.setHorizontalAlignment(SwingConstants.CENTER); lblNapas.setBorder(new EmptyBorder(5, 0, 0, 0));
        qrPanel.add(lblNapas, BorderLayout.SOUTH);
        transferBox.add(qrPanel, BorderLayout.WEST);

        // Chi tiết CK
        JPanel detailsPanel = new JPanel(); detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS)); detailsPanel.setOpaque(false);
        JLabel qrtitle = new JLabel("Quét mã QR để thanh toán"); 
        qrtitle.setFont(new Font("Segoe UI", Font.BOLD, 15)); 
        qrtitle.setForeground(TEXT_MAIN);
        qrtitle.setAlignmentX(Component.CENTER_ALIGNMENT); // <-- Thêm dòng này để căn giữa
        
        detailsPanel.add(qrtitle); detailsPanel.add(Box.createVerticalStrut(15));
        
        detailsPanel.add(createCopyRow("Ngân hàng", "PVCOM Bank"));
        detailsPanel.add(createCopyRow("Số tài khoản", "103002695853"));
        detailsPanel.add(createCopyRow("Chủ tài khoản", "NGUYEN BA SANG"));
        detailsPanel.add(createCopyRow("Nội dung CK", "THU" + packageName.toUpperCase() + " 123"));
        
        transferBox.add(detailsPanel, BorderLayout.CENTER);
        centerContent.add(transferBox); centerContent.add(Box.createVerticalStrut(20));

        // 4. Timer & Tổng tiền
        JPanel summaryBox = new JPanel(new BorderLayout()); summaryBox.setOpaque(false);
        
        JPanel timerPanel = new JPanel(); timerPanel.setLayout(new BoxLayout(timerPanel, BoxLayout.Y_AXIS)); timerPanel.setOpaque(false);
        JLabel lblTimeTxt = new JLabel("Thời gian còn lại"); lblTimeTxt.setFont(new Font("Segoe UI", Font.PLAIN, 13)); lblTimeTxt.setForeground(TEXT_MAIN);
        lblTimer = new JLabel("15 : 00"); lblTimer.setFont(new Font("Segoe UI", Font.BOLD, 26)); lblTimer.setForeground(PRIMARY);
        timerPanel.add(lblTimeTxt); timerPanel.add(Box.createVerticalStrut(5)); timerPanel.add(lblTimer);
        summaryBox.add(timerPanel, BorderLayout.WEST);

        JPanel rightSummary = new JPanel(); rightSummary.setLayout(new BoxLayout(rightSummary, BoxLayout.Y_AXIS)); rightSummary.setOpaque(false);
        JPanel rowGoi = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0)); rowGoi.setOpaque(false);
        JLabel lblNameGoi = new JLabel("Gói " + packageName); lblNameGoi.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JLabel lblGia = new JLabel(String.format("%,.0fđ", basePrice).replace(",", ".")); lblGia.setFont(new Font("Segoe UI", Font.BOLD, 14));
        rowGoi.add(lblNameGoi); rowGoi.add(lblGia);
        
        JPanel rowUuDai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0)); rowUuDai.setOpaque(false);
        JLabel lblTxtUudai = new JLabel("Ưu đãi"); lblTxtUudai.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblDiscountAmount = new JLabel("- 0đ"); lblDiscountAmount.setFont(new Font("Segoe UI", Font.BOLD, 14)); lblDiscountAmount.setForeground(SUCCESS);
        rowUuDai.add(lblTxtUudai); rowUuDai.add(lblDiscountAmount);

        JPanel rowTotal = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0)); rowTotal.setOpaque(false);
        JLabel lblTxtTotal = new JLabel("Tổng tiền"); lblTxtTotal.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTotalAmount = new JLabel(String.format("%,.0fđ", basePrice).replace(",", ".")); 
        lblTotalAmount.setFont(new Font("Segoe UI", Font.BOLD, 22)); lblTotalAmount.setForeground(PRIMARY);
        rowTotal.add(lblTxtTotal); rowTotal.add(lblTotalAmount);
        
        rightSummary.add(rowGoi); rightSummary.add(Box.createVerticalStrut(5));
        rightSummary.add(rowUuDai); rightSummary.add(Box.createVerticalStrut(10));
        rightSummary.add(rowTotal);
        
        summaryBox.add(rightSummary, BorderLayout.EAST);
        
        centerContent.add(summaryBox); centerContent.add(Box.createVerticalStrut(10));
        centerContent.add(new JSeparator()); centerContent.add(Box.createVerticalStrut(15));

        // 5. Box Nhập mã Giảm giá
        JPanel promoWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); promoWrap.setOpaque(false);
        JTextField txtPromo = new JTextField(15); txtPromo.setPreferredSize(new Dimension(180, 38));
        txtPromo.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), new EmptyBorder(0, 10, 0, 10)));
        JButton btnApply = new JButton("Áp dụng"); 
        btnApply.setFont(new Font("Segoe UI", Font.BOLD, 13)); btnApply.setForeground(Color.decode("#374151")); btnApply.setBackground(Color.WHITE); 
        btnApply.setPreferredSize(new Dimension(90, 38)); btnApply.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnApply.addActionListener(e -> {
            if(txtPromo.getText().trim().equalsIgnoreCase("GIAM30K")) { discount = 30000; updateTotal(); JOptionPane.showMessageDialog(this, "Áp dụng mã thành công!"); } 
            else { JOptionPane.showMessageDialog(this, "Mã giảm giá không hợp lệ."); }
        });
        promoWrap.add(txtPromo); promoWrap.add(btnApply);
        centerContent.add(promoWrap);

        p.add(centerContent, BorderLayout.CENTER);

        // 6. Nút Xác nhận Cuối cùng
        JPanel footer = new JPanel(new BorderLayout()); footer.setOpaque(false); footer.setBorder(new EmptyBorder(25, 0, 0, 0));
        
        JPanel btnConfirmWrap = new JPanel(new BorderLayout()){ 
            @Override protected void paintComponent(Graphics g) { 
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
                GradientPaint gp = new GradientPaint(0, 0, PRIMARY, getWidth(), getHeight(), PRIMARY_DARK);
                g2.setPaint(gp); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16); g2.dispose(); 
            } 
        };
        btnConfirmWrap.setOpaque(false); btnConfirmWrap.setPreferredSize(new Dimension(0, 56)); btnConfirmWrap.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JPanel centerBtn = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 15)); centerBtn.setOpaque(false);
        JLabel lockIcon2 = new JLabel(); setIcon(lockIcon2, "https://img.icons8.com/fluency-systems-regular/48/FFFFFF/lock.png", 22, 22);
        JLabel lblBtn = new JLabel("Xác nhận thanh toán"); lblBtn.setFont(new Font("Segoe UI", Font.BOLD, 16)); lblBtn.setForeground(Color.WHITE); 
        centerBtn.add(lockIcon2); centerBtn.add(lblBtn);
        btnConfirmWrap.add(centerBtn, BorderLayout.CENTER);

        btnConfirmWrap.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                lblBtn.setText("Đang xử lý giao dịch...");
                Timer t = new Timer(2000, ev -> {
                    JOptionPane.showMessageDialog(UpgradeDialog.this, "Thanh toán thành công! Chào mừng bạn đến với gói " + packageName + ".");
                    dispose();
                });
                t.setRepeats(false); t.start();
            }
        });
        
        footer.add(btnConfirmWrap, BorderLayout.CENTER);
        p.add(footer, BorderLayout.SOUTH);

        return p;
    }

    private JLabel createStepLbl(String text, Color c, int fontStyle) {
        JLabel l = new JLabel(text); l.setFont(new Font("Segoe UI", fontStyle, 14)); l.setForeground(c); return l;
    }

    private void updateTotal() {
        lblDiscountAmount.setText(String.format("- %,.0fđ", discount).replace(",", "."));
        lblTotalAmount.setText(String.format("%,.0fđ", basePrice - discount).replace(",", "."));
    }

    private void startTimer() {
        countdownTimer = new Timer(1000, e -> {
            timeLeft--;
            int m = timeLeft / 60;
            int s = timeLeft % 60;
            lblTimer.setText(String.format("%02d : %02d", m, s));
            if (timeLeft <= 0) {
                countdownTimer.stop();
                JOptionPane.showMessageDialog(this, "Hết thời gian thanh toán. Vui lòng thử lại!");
                dispose();
            }
        });
        countdownTimer.start();
    }

    private JPanel createPaymentMethodBtn(String text, String iconPath, boolean isSelected, String urlLink) {
        JPanel btn = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSelected ? Color.decode("#EEF2FF") : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(isSelected ? PRIMARY : BORDER);
                g2.setStroke(new BasicStroke(isSelected ? 1.5f : 1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10); g2.dispose();
            }
        };
        btn.setOpaque(false); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        if (iconPath != null) {
            JLabel icon = new JLabel(); setIcon(icon, iconPath, 24, 24);
            btn.add(icon);
        }

        JLabel lbl = new JLabel(text); lbl.setFont(new Font("Segoe UI", isSelected ? Font.BOLD : Font.PLAIN, 14));
        lbl.setForeground(isSelected ? PRIMARY : TEXT_MAIN);
        btn.add(lbl);
        
        if (urlLink != null && !urlLink.isEmpty()) {
            btn.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    try { Desktop.getDesktop().browse(new URI(urlLink)); } 
                    catch(Exception ex) { JOptionPane.showMessageDialog(UpgradeDialog.this, "Không thể mở trình duyệt."); }
                }
            });
        }
        return btn;
    }

    private JPanel createCopyRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout()); row.setOpaque(false); row.setBorder(new EmptyBorder(6, 0, 6, 0));
        JLabel lblT = new JLabel(label); lblT.setFont(new Font("Segoe UI", Font.PLAIN, 13)); lblT.setForeground(TEXT_MUTED); lblT.setPreferredSize(new Dimension(100, 20));
        JLabel lblV = new JLabel(value); lblV.setFont(new Font("Segoe UI", Font.BOLD, 13)); lblV.setForeground(TEXT_MAIN);
        
        JLabel copyBtn = new JLabel(); 
        setIcon(copyBtn, "https://img.icons8.com/fluency-systems-regular/48/6366F1/copy.png", 18, 18);
        copyBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        copyBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), null);
                JOptionPane.showMessageDialog(UpgradeDialog.this, "Đã sao chép: " + value);
            }
        });
        
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); right.setOpaque(false);
        right.add(lblV); right.add(copyBtn);
        
        row.add(lblT, BorderLayout.WEST); row.add(right, BorderLayout.CENTER);
        row.add(new JSeparator(), BorderLayout.SOUTH);
        return row;
    }

    // --- HÀM TẢI ICON CHUNG (Xử lý cả file local và link internet) ---
    private void setIcon(JLabel label, String path, int width, int height) {
        if (path == null) return;
        
        // Nếu là link internet
        if (path.startsWith("http")) {
            String key = path + "_" + width + "x" + height;
            if (iconCache.containsKey(key)) {
                label.setIcon(iconCache.get(key));
                return;
            }
            new Thread(() -> {
                try {
                    ImageIcon raw = new ImageIcon(new URL(path));
                    Image img = raw.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    ImageIcon icon = new ImageIcon(img);
                    iconCache.put(key, icon);
                    SwingUtilities.invokeLater(() -> label.setIcon(icon));
                } catch (Exception ignored) {}
            }).start();
        } 
        // Nếu là đường dẫn tới thư mục local trong Netbeans
        else {
            try {
                URL url = getClass().getResource(path);
                if (url != null) {
                    ImageIcon raw = new ImageIcon(url);
                    Image img = raw.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    label.setIcon(new ImageIcon(img));
                } else {
                    label.setText("[Icon]");
                }
            } catch (Exception e) {}
        }
    }
}