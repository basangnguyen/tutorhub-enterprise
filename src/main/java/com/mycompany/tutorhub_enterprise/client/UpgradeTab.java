package com.mycompany.tutorhub_enterprise.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

public class UpgradeTab extends JPanel {

    private final Color TEXT_MAIN = Color.decode("#1E1B4B");
    private final Color TEXT_MUTED = Color.decode("#6B7280");
    private final Color PRIMARY = Color.decode("#6366F1"); // Chuyển sang tông Xanh/Tím Indigo
    private final Color BORDER_COLOR = Color.decode("#E5E7EB");
    private Runnable onBackListener;

    public UpgradeTab() {
        setLayout(new BorderLayout());

        // --- 1. HEADER (Title & Back Button) ---
        add(createHeader(), BorderLayout.NORTH);

        // --- 2. MAIN CONTENT (Scrollable) ---
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(0, 30, 20, 30));

        // 2.1 Top Features Banner
        centerPanel.add(createTopFeaturesBar());
        centerPanel.add(Box.createVerticalStrut(20));

        // 2.2 Toggle Thanh toán Tháng/Năm
        centerPanel.add(createBillingToggle());
        centerPanel.add(Box.createVerticalStrut(20));

        // 2.3 Bảng Giá (Pricing Cards Grid)
        centerPanel.add(createPricingGrid());
        centerPanel.add(Box.createVerticalStrut(30));

        // 2.4 Footer Guarantees
        centerPanel.add(createFooterGuarantees());

        JScrollPane scroll = new JScrollPane(centerPanel);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        add(scroll, BorderLayout.CENTER);
    }

    // --- Override để vẽ nền Gradient Trắng -> Tím Pastel ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint gp = new GradientPaint(0, 0, Color.WHITE, 0, getHeight(), Color.decode("#F5F3FF"));
        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    public void setOnBackListener(Runnable listener) {
        this.onBackListener = listener;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(20, 30, 15, 30));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftPanel.setOpaque(false);

        JPanel btnBack = new JPanel(new BorderLayout()) {
            boolean hover = false;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hover ? Color.decode("#E2E8F0") : Color.WHITE); g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(BORDER_COLOR); g2.setStroke(new BasicStroke(1.2f)); g2.drawOval(1, 1, getWidth()-3, getHeight()-3); g2.dispose();
            }
        };
        btnBack.setPreferredSize(new Dimension(40, 40)); btnBack.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JLabel iconBack = new JLabel("<", SwingConstants.CENTER); iconBack.setFont(new Font("Segoe UI", Font.BOLD, 16)); iconBack.setForeground(TEXT_MAIN);
        btnBack.add(iconBack, BorderLayout.CENTER);
        btnBack.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { try { btnBack.getClass().getDeclaredField("hover").set(btnBack, true); btnBack.repaint(); } catch(Exception ex){} }
            @Override public void mouseExited(MouseEvent e) { try { btnBack.getClass().getDeclaredField("hover").set(btnBack, false); btnBack.repaint(); } catch(Exception ex){} }
            @Override public void mouseClicked(MouseEvent e) { if(onBackListener != null) onBackListener.run(); }
        });

        JPanel titlePanel = new JPanel(); titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS)); titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Nâng cấp tài khoản"); lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24)); lblTitle.setForeground(TEXT_MAIN);
        JLabel lblSub = new JLabel("Chọn gói phù hợp để trải nghiệm đầy đủ các tính năng cao cấp của TutorHub"); lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13)); lblSub.setForeground(TEXT_MUTED);
        titlePanel.add(lblTitle); titlePanel.add(Box.createVerticalStrut(4)); titlePanel.add(lblSub);

        leftPanel.add(btnBack); leftPanel.add(titlePanel);
        header.add(leftPanel, BorderLayout.WEST);
        return header;
    }

    private JPanel createTopFeaturesBar() {
        JPanel bar = new JPanel(new GridLayout(1, 4, 15, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16); g2.dispose();
            }
        };
        bar.setOpaque(false); bar.setBorder(new EmptyBorder(15, 20, 15, 20)); bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        bar.add(createFeatureItem("https://img.icons8.com/fluency/48/shield.png", "Tiết kiệm thời gian", "Quản lý lớp học hiệu quả"));
        bar.add(createFeatureItem("https://img.icons8.com/fluency/48/money-bag.png", "Tăng thu nhập", "Nhận nhiều lớp phù hợp"));
        bar.add(createFeatureItem("https://img.icons8.com/fluency/48/star.png", "Ưu tiên hiển thị", "Tìm kiếm dễ dàng hơn"));
        bar.add(createFeatureItem("https://img.icons8.com/fluency/48/headset.png", "Hỗ trợ 24/7", "Đội ngũ chuyên nghiệp"));
        return bar;
    }

    private JPanel createFeatureItem(String icon, String title, String desc) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0)); p.setOpaque(false);
        JLabel lblIcon = new JLabel(); setNetworkIcon(lblIcon, icon, 28, 28);
        JPanel textP = new JPanel(); textP.setLayout(new BoxLayout(textP, BoxLayout.Y_AXIS)); textP.setOpaque(false);
        JLabel lblTitle = new JLabel(title); lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13)); lblTitle.setForeground(TEXT_MAIN);
        JLabel lblDesc = new JLabel(desc); lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 11)); lblDesc.setForeground(TEXT_MUTED);
        textP.add(lblTitle); textP.add(Box.createVerticalStrut(2)); textP.add(lblDesc);
        p.add(lblIcon); p.add(textP); return p;
    }

    private JPanel createBillingToggle() {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); wrapper.setOpaque(false);
        JPanel toggle = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 36, 36);
                g2.setColor(BORDER_COLOR); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 36, 36); g2.dispose();
            }
        };
        toggle.setOpaque(false); toggle.setBorder(new EmptyBorder(3, 3, 3, 3));

        JLabel btnMonth = new JLabel("Thanh toán theo tháng", SwingConstants.CENTER);
        btnMonth.setFont(new Font("Segoe UI", Font.BOLD, 12)); btnMonth.setForeground(PRIMARY);
        btnMonth.setPreferredSize(new Dimension(160, 32)); btnMonth.setOpaque(true); btnMonth.setBackground(Color.decode("#EEF2FF"));
        btnMonth.putClientProperty("JComponent.arc", 32);

        JLabel btnYear = new JLabel("Thanh toán theo năm", SwingConstants.CENTER);
        btnYear.setFont(new Font("Segoe UI", Font.PLAIN, 12)); btnYear.setForeground(TEXT_MUTED);
        btnYear.setPreferredSize(new Dimension(160, 32)); btnYear.setOpaque(false);

        JPanel badge = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode("#D1FAE5")); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16); g2.dispose();
            }
        };
        badge.setOpaque(false); badge.setBorder(new EmptyBorder(2, 8, 2, 8));
        JLabel lblBadge = new JLabel("Tiết kiệm 20%"); lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 10)); lblBadge.setForeground(Color.decode("#059669"));
        badge.add(lblBadge, BorderLayout.CENTER);

        toggle.add(btnMonth); toggle.add(btnYear); wrapper.add(toggle); wrapper.add(Box.createHorizontalStrut(10)); wrapper.add(badge);
        return wrapper;
    }

    private JPanel createPricingGrid() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 20, 0)); grid.setOpaque(false);

        grid.add(createPricingCard("Miễn phí", "Dành cho gia sư mới bắt đầu", "0đ", "Gói hiện tại", "#F3F4F6", "#6B7280", false, false, new Object[][]{
            {"Nhận tối đa 3 lớp / tháng", true}, {"Tìm kiếm lớp học cơ bản", true}, {"Nhắn tin với phụ huynh", true},
            {"Ưu tiên hiển thị lớp học", false}, {"Thống kê thu nhập chi tiết", false}, {"Hỗ trợ ưu tiên", false}
        }));

        grid.add(createPricingCard("Basic", "Mở rộng cơ hội nhận lớp", "59.000đ", "Nâng cấp gói Basic", "#EEF2FF", "#6366F1", false, false, new Object[][]{
            {"Nhận tối đa 15 lớp / tháng", true}, {"Tìm kiếm lớp học nâng cao", true}, {"Nhắn tin không giới hạn", true},
            {"Ưu tiên hiển thị lớp học", true}, {"Thống kê thu nhập chi tiết", false}, {"Hỗ trợ ưu tiên", false}
        }));

        grid.add(createPricingCard("Premium", "Trải nghiệm toàn diện", "129.000đ", "Nâng cấp gói Premium", "#6366F1", "#FFFFFF", true, true, new Object[][]{
            {"Nhận không giới hạn lớp", true}, {"Tìm kiếm lớp học nâng cao", true}, {"Nhắn tin không giới hạn", true},
            {"Ưu tiên hiển thị lớp học", true}, {"Thống kê thu nhập chi tiết", true}, {"Hỗ trợ ưu tiên 24/7", true}
        }));

        grid.add(createPricingCard("VIP", "Dành cho gia sư chuyên nghiệp", "249.000đ", "Nâng cấp gói VIP", "#8B5CF6", "#FFFFFF", false, false, new Object[][]{
            {"Nhận không giới hạn lớp", true}, {"Tìm kiếm lớp học nâng cao", true}, {"Nhắn tin không giới hạn", true},
            {"Ưu tiên hiển thị lớp học (Top)", true}, {"Thống kê thu nhập chi tiết", true}, {"Hỗ trợ trợ riêng 1-1", true}
        }));

        return grid;
    }

    private JPanel createPricingCard(String name, String desc, String price, String btnText, String btnBg, String btnFg, boolean isPopular, boolean isPrimary, Object[][] features) {
        
        // --- 1. WRAPPER NGOÀI (Chứa Padding & Logic Hover) ---
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(10, 0, 10, 0)); // Khởi tạo biên cơ bản để lấy không gian cho hiệu ứng nổi lên

        // --- 2. THẺ CARD CHÍNH ---
        JPanel card = new JPanel(new BorderLayout()) {
            boolean isHovered = false;
            float offsetY = 0; // Trạng thái tọa độ Y
            float targetY = 0; // Mục tiêu tọa độ Y
            Timer t;
            
            {
                setOpaque(false);
                // Cài đặt Timer nội bộ cho mỗi thẻ để tính toán Animation mượt 60fps
                t = new Timer(16, e -> {
                    offsetY += (targetY - offsetY) * 0.3f; // Easing effect
                    if(Math.abs(targetY - offsetY) < 0.5f) { 
                        offsetY = targetY; 
                        t.stop(); 
                    }
                    // Bóp margin để thẻ trượt lên
                    wrapper.setBorder(new EmptyBorder(10 + (int)offsetY, 0, 10 - (int)offsetY, 0));
                    wrapper.revalidate();
                });
            }

            public void setHoveredState(boolean hover) {
                if (isHovered == hover) return;
                isHovered = hover;
                targetY = hover ? -8 : 0; // Khi hover trượt lên 8px
                t.start();
                repaint();
            }

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); 
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();

                // Hiệu ứng Glow/Shadow màu tím nhẹ
                if (isPrimary || isHovered) {
                    g2.setColor(new Color(139, 92, 246, isHovered ? 40 : 20));
                    g2.fillRoundRect(0, 6, w, h-6, 24, 24);
                }

                g2.setColor(Color.WHITE); 
                g2.fillRoundRect(0, 0, w, h, 24, 24); 
                
                // Viền thẻ
                g2.setColor(isHovered ? PRIMARY : BORDER_COLOR); 
                g2.setStroke(new BasicStroke(isHovered ? 1.8f : 1f));
                g2.drawRoundRect(0, 0, w-1, h-1, 24, 24); 
                
                g2.dispose();
            }
        };
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Logic bắt sự kiện chuột chính xác
        MouseAdapter hoverAdapter = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { 
                try { card.getClass().getMethod("setHoveredState", boolean.class).invoke(card, true); } catch (Exception ex){} 
            }
            @Override public void mouseExited(MouseEvent e) { 
                Point p = e.getPoint();
                // Chỉ hủy hover khi con chuột thực sự đi ra khỏi đường biên của card
                if (p.x <= 0 || p.x >= card.getWidth() || p.y <= 0 || p.y >= card.getHeight()) {
                    try { card.getClass().getMethod("setHoveredState", boolean.class).invoke(card, false); } catch (Exception ex){}
                }
            }
        };
        card.addMouseListener(hoverAdapter);

        // --- KHU VỰC TRÊN CÙNG (Thông tin gói) ---
        JPanel top = new JPanel(); 
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS)); 
        top.setOpaque(false);
        
        if (isPopular) {
            JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)); badgeWrap.setOpaque(false);
            JPanel badge = new JPanel(new BorderLayout()){ 
                @Override protected void paintComponent(Graphics g) { 
                    Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
                    g2.setColor(PRIMARY); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16); g2.dispose(); 
                } 
            };
            badge.setOpaque(false); badge.setBorder(new EmptyBorder(4, 12, 4, 12));
            JLabel lblBadge = new JLabel("Phổ biến nhất", SwingConstants.CENTER); lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 10)); lblBadge.setForeground(Color.WHITE);
            badge.add(lblBadge); badgeWrap.add(badge); top.add(badgeWrap); 
            top.add(Box.createVerticalStrut(10));
        } else {
            top.add(Box.createVerticalStrut(34)); // Bù trừ chiều cao để cân bằng với thẻ có badge
        }

        JLabel lblName = new JLabel(name); 
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 20)); 
        lblName.setForeground(name.equals("VIP") ? Color.decode("#8B5CF6") : (isPrimary ? PRIMARY : TEXT_MAIN)); 
        lblName.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel lblDesc = new JLabel(desc); 
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 11)); 
        lblDesc.setForeground(TEXT_MUTED); 
        lblDesc.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JPanel pricePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0)); 
        pricePanel.setOpaque(false);
        JLabel lblPrice = new JLabel(price); lblPrice.setFont(new Font("Segoe UI", Font.BOLD, 28)); lblPrice.setForeground(TEXT_MAIN);
        JLabel lblMonth = new JLabel("/ tháng"); lblMonth.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblMonth.setForeground(TEXT_MUTED);
        pricePanel.add(lblPrice); pricePanel.add(lblMonth);

        top.add(lblName); top.add(Box.createVerticalStrut(4)); top.add(lblDesc); 
        top.add(Box.createVerticalStrut(12)); top.add(pricePanel); top.add(Box.createVerticalStrut(20));
        
        card.add(top, BorderLayout.NORTH);

       // --- KHU VỰC GIỮA (Quyền lợi - Khôi phục vòng lặp gốc) ---
        JPanel featureList = new JPanel(); 
        featureList.setLayout(new BoxLayout(featureList, BoxLayout.Y_AXIS)); 
        featureList.setOpaque(false);

        // Đổ dữ liệu quyền lợi ra giao diện từ tham số `features`
        for (Object[] feat : features) {
            String text = (String) feat[0]; 
            boolean isCheck = (boolean) feat[1];
            
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4)); 
            row.setOpaque(false); 
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            JLabel icon = new JLabel(); 
            // Nếu true hiện dấu check tím, false hiện dấu x xám
            String iconUrl = isCheck ? "https://img.icons8.com/fluency-systems-regular/48/8B5CF6/checkmark.png" 
                                     : "https://img.icons8.com/fluency-systems-regular/48/9CA3AF/multiply.png";
            setNetworkIcon(icon, iconUrl, 14, 14);
            
            JLabel lblFeat = new JLabel(text); 
            lblFeat.setFont(new Font("Segoe UI", Font.PLAIN, 12)); 
            lblFeat.setForeground(isCheck ? TEXT_MAIN : TEXT_MUTED); 
            
            row.add(icon); 
            row.add(lblFeat); 
            featureList.add(row);
        }
        card.add(featureList, BorderLayout.CENTER);

        // --- KHU VỰC DƯỚI CÙNG (Nút Hành Động) ---
        JPanel btnWrap = new JPanel(new BorderLayout()){ 
            @Override protected void paintComponent(Graphics g) { 
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
                g2.setColor(Color.decode(btnBg)); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12); g2.dispose(); 
            } 
        };
        btnWrap.setOpaque(false); btnWrap.setPreferredSize(new Dimension(0, 40)); btnWrap.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JLabel lblBtn = new JLabel(btnText, SwingConstants.CENTER); lblBtn.setFont(new Font("Segoe UI", Font.BOLD, 13)); lblBtn.setForeground(Color.decode(btnFg)); 
        btnWrap.add(lblBtn, BorderLayout.CENTER);
        
        btnWrap.addMouseListener(new MouseAdapter() { 
            @Override public void mouseClicked(MouseEvent e) { 
                if (!name.equals("Miễn phí")) {
                    double amount = 0;
                    try { amount = Double.parseDouble(price.replaceAll("[^0-9]", "")); } catch (Exception ex) {}
                    Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(UpgradeTab.this);
                    UpgradeDialog upgradeModal = new UpgradeDialog(parentFrame, name, amount);
                    upgradeModal.setVisible(true);
                }
            } 
            @Override public void mouseEntered(MouseEvent e) { hoverAdapter.mouseEntered(e); }
            @Override public void mouseExited(MouseEvent e) { hoverAdapter.mouseExited(e); }
        });

        JPanel bottomWrapper = new JPanel(new BorderLayout());
        bottomWrapper.setOpaque(false);
        bottomWrapper.setBorder(new EmptyBorder(15, 0, 0, 0));
        bottomWrapper.add(btnWrap, BorderLayout.CENTER);
        
        card.add(bottomWrapper, BorderLayout.SOUTH);

        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createFooterGuarantees() {
        JPanel footer = new JPanel(new GridLayout(1, 3, 20, 0)); footer.setOpaque(false); footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        footer.add(createGuaranteeItem("https://img.icons8.com/fluency-systems-regular/48/6366F1/security-checked.png", "Thanh toán an toàn", "Thông tin của bạn được bảo mật tuyệt đối"));
        footer.add(createGuaranteeItem("https://img.icons8.com/fluency-systems-regular/48/6366F1/cancel.png", "Hủy bất kỳ lúc nào", "Bạn có thể nâng cấp hoặc hủy gói bất cứ lúc nào"));
        footer.add(createGuaranteeItem("https://img.icons8.com/fluency-systems-regular/48/6366F1/money-box.png", "Hoàn tiền 100%", "Hoàn tiền trong 7 ngày nếu không hài lòng"));
        return footer;
    }

    private JPanel createGuaranteeItem(String icon, String title, String desc) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0)); p.setOpaque(false);
        JLabel lblIcon = new JLabel(); setNetworkIcon(lblIcon, icon, 32, 32);
        JPanel textP = new JPanel(); textP.setLayout(new BoxLayout(textP, BoxLayout.Y_AXIS)); textP.setOpaque(false);
        JLabel lblTitle = new JLabel(title); lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13)); lblTitle.setForeground(TEXT_MAIN);
        JLabel lblDesc = new JLabel("<html><div style='width:180px;'>" + desc + "</div></html>"); lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 11)); lblDesc.setForeground(TEXT_MUTED);
        textP.add(lblTitle); textP.add(Box.createVerticalStrut(4)); textP.add(lblDesc);
        p.add(lblIcon); p.add(textP); return p;
    }

    private void setNetworkIcon(JLabel label, String urlStr, int width, int height) {
        new Thread(() -> { try { ImageIcon raw = new ImageIcon(new URL(urlStr)); Image img = raw.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH); SwingUtilities.invokeLater(() -> label.setIcon(new ImageIcon(img))); } catch (Exception ignored) {} }).start();
    }
}