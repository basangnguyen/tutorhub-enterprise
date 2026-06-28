package com.mycompany.tutorhub_enterprise.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.URL;
import java.text.DecimalFormat;

public class UpgradeDialog extends JDialog {

    private static final Color PRIMARY = Color.decode("#6366F1");
    private static final Color PRIMARY_DARK = Color.decode("#4F46E5");
    private static final Color TEXT_MAIN = Color.decode("#1E1B4B");
    private static final Color TEXT_SOFT = Color.decode("#475467");
    private static final Color TEXT_MUTED = Color.decode("#667085");
    private static final Color BORDER = Color.decode("#E5E7EB");
    private static final Color SURFACE = Color.WHITE;
    private static final Color SURFACE_SOFT = Color.decode("#F8FAFC");
    private static final Color SUCCESS = Color.decode("#10B981");
    private static final Color DANGER = Color.decode("#EF4444");

    private final String packageName;
    private final double basePrice;
    private double discount;
    private int timeLeft = 15 * 60;

    private JLabel lblTimer;
    private JLabel lblTotalAmount;
    private JLabel lblDiscountAmount;
    private Timer countdownTimer;

    public UpgradeDialog(Frame parent, String packageName, double basePrice) {
        super(parent, "Thanh toán nâng cấp", true);
        this.packageName = packageName;
        this.basePrice = basePrice;

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(buildContent());
        setSize(1030, 640);
        setLocationRelativeTo(parent);
        startTimer();
    }

    private JComponent buildContent() {
        JPanel shadowWrap = new JPanel(new BorderLayout());
        shadowWrap.setOpaque(false);
        shadowWrap.setBorder(new EmptyBorder(10, 10, 14, 10));

        RoundedPanel shell = new RoundedPanel(SURFACE, 22, BORDER);
        shell.setLayout(new BorderLayout());
        shell.setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel header = createHeader();
        shell.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);

        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = 0;
        left.weightx = 0.42;
        left.weighty = 1;
        left.fill = GridBagConstraints.BOTH;
        body.add(createLeftPanel(), left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = 0;
        right.weightx = 0.58;
        right.weighty = 1;
        right.fill = GridBagConstraints.BOTH;
        body.add(createRightPanel(), right);

        shell.add(body, BorderLayout.CENTER);
        shadowWrap.add(shell, BorderLayout.CENTER);
        return shadowWrap;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(16, 24, 4, 18));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("TutorHub Premium");
        eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 11));
        eyebrow.setForeground(PRIMARY);
        eyebrow.setBorder(new EmptyBorder(0, 0, 2, 0));

        JLabel title = new JLabel("Thanh toán gói " + packageName);
        title.setFont(new Font("Segoe UI", Font.BOLD, 23));
        title.setForeground(TEXT_MAIN);

        titleBlock.add(eyebrow);
        titleBlock.add(title);
        header.add(titleBlock, BorderLayout.WEST);

        JButton close = createIconButton("×");
        close.setToolTipText("Đóng");
        close.addActionListener(e -> dispose());
        header.add(close, BorderLayout.EAST);

        return header;
    }

    private JPanel createLeftPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(new Dimension(420, 0));
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Color.decode("#F1F5F9")),
                new EmptyBorder(10, 30, 28, 30)
        ));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel subtitle = new JLabel("Kiểm tra gói đã chọn trước khi thanh toán.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(subtitle);
        content.add(Box.createVerticalStrut(18));

        RoundedPanel planCard = new RoundedPanel(Color.WHITE, 20, PRIMARY);
        planCard.setLayout(new BoxLayout(planCard, BoxLayout.Y_AXIS));
        planCard.setBorder(new EmptyBorder(22, 24, 22, 24));
        planCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        planCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 410));

        JLabel selected = new JLabel("GÓI BẠN CHỌN");
        selected.setFont(new Font("Segoe UI", Font.BOLD, 10));
        selected.setForeground(Color.WHITE);
        selected.setOpaque(true);
        selected.setBackground(PRIMARY);
        selected.setBorder(new EmptyBorder(5, 12, 5, 12));
        selected.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel name = new JLabel(packageName);
        name.setFont(new Font("Segoe UI", Font.BOLD, 27));
        name.setForeground(isVip() ? Color.decode("#7C3AED") : PRIMARY);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel priceRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        priceRow.setOpaque(false);
        JLabel price = new JLabel(currency(basePrice));
        price.setFont(new Font("Segoe UI", Font.BOLD, 34));
        price.setForeground(TEXT_MAIN);
        JLabel period = new JLabel("/ tháng");
        period.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        period.setForeground(TEXT_MUTED);
        priceRow.add(price);
        priceRow.add(period);

        planCard.add(centerWrap(selected));
        planCard.add(Box.createVerticalStrut(18));
        planCard.add(name);
        planCard.add(Box.createVerticalStrut(10));
        planCard.add(priceRow);
        planCard.add(Box.createVerticalStrut(18));

        for (PlanFeature feature : getFeatures()) {
            planCard.add(createFeatureRow(feature.text, feature.enabled));
        }

        content.add(planCard);
        content.add(Box.createVerticalGlue());
        content.add(createSecureLine());

        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createRightPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(8, 30, 28, 30));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        content.add(createSteps());
        content.add(Box.createVerticalStrut(18));
        content.add(createMethodHeader());
        content.add(Box.createVerticalStrut(10));
        content.add(createPaymentMethods());
        content.add(Box.createVerticalStrut(18));
        content.add(createTransferBox());
        content.add(Box.createVerticalStrut(18));
        content.add(createSummaryBox());
        content.add(Box.createVerticalStrut(12));
        content.add(createPromoBox());

        wrapper.add(content, BorderLayout.CENTER);
        wrapper.add(createConfirmButton(), BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel createSteps() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        panel.setOpaque(false);
        panel.add(step("1. Chọn gói", TEXT_MUTED, Font.PLAIN));
        panel.add(step(">", TEXT_MUTED, Font.PLAIN));
        panel.add(step("2. Thanh toán", PRIMARY, Font.BOLD));
        panel.add(step(">", TEXT_MUTED, Font.PLAIN));
        panel.add(step("3. Hoàn tất", TEXT_MUTED, Font.PLAIN));
        return panel;
    }

    private JLabel step(String text, Color color, int style) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, 14));
        label.setForeground(color);
        return label;
    }

    private JPanel createMethodHeader() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.setOpaque(false);
        panel.add(new SymbolIcon("▣", PRIMARY, 20));
        JLabel label = new JLabel("Phương thức thanh toán");
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        label.setForeground(TEXT_MAIN);
        panel.add(label);
        return panel;
    }

    private JPanel createPaymentMethods() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        row.add(createPaymentMethod("QR Banking", "QR", true, null));
        row.add(createPaymentMethod("MoMo", "M", false, "https://momo.vn/"));
        row.add(createPaymentMethod("ZaloPay", "Z", false, "https://zalopay.vn/"));
        return row;
    }

    private JComponent createPaymentMethod(String text, String symbol, boolean selected, String url) {
        RoundedPanel button = new RoundedPanel(selected ? Color.decode("#EEF2FF") : Color.WHITE, 12, selected ? PRIMARY : BORDER);
        button.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 8));
        button.setBorder(new EmptyBorder(0, 10, 0, 10));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel icon = new JLabel(symbol);
        icon.setFont(new Font("Segoe UI", Font.BOLD, 12));
        icon.setForeground(selected ? PRIMARY : TEXT_SOFT);
        icon.setOpaque(true);
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        icon.setPreferredSize(new Dimension(24, 24));
        icon.setBackground(selected ? Color.WHITE : SURFACE_SOFT);
        button.add(icon);

        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", selected ? Font.BOLD : Font.PLAIN, 14));
        label.setForeground(selected ? PRIMARY : TEXT_MAIN);
        button.add(label);

        if (url != null) {
            button.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    openLink(url);
                }
            });
        }
        return button;
    }

    private JPanel createTransferBox() {
        RoundedPanel box = new RoundedPanel(SURFACE_SOFT, 18, BORDER);
        box.setLayout(new BorderLayout(24, 0));
        box.setBorder(new EmptyBorder(22, 24, 22, 24));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);

        RoundedPanel qrWrap = new RoundedPanel(Color.WHITE, 14, Color.decode("#F1F5F9"));
        qrWrap.setLayout(new BorderLayout());
        qrWrap.setPreferredSize(new Dimension(158, 158));
        qrWrap.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel qr = new JLabel();
        setLocalIcon(qr, "/images/qr_code.png", 138, 138);
        qr.setHorizontalAlignment(SwingConstants.CENTER);
        qrWrap.add(qr, BorderLayout.CENTER);
        box.add(qrWrap, BorderLayout.WEST);

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Quét mã QR để thanh toán");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(TEXT_MAIN);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.add(title);
        details.add(Box.createVerticalStrut(12));
        details.add(createCopyRow("Ngân hàng", "PVCOM Bank"));
        details.add(createCopyRow("Số tài khoản", "103002695853"));
        details.add(createCopyRow("Chủ tài khoản", "NGUYEN BA SANG"));
        details.add(createCopyRow("Nội dung CK", "THU" + packageName.toUpperCase() + " 123"));

        box.add(details, BorderLayout.CENTER);
        return box;
    }

    private JPanel createCopyRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(6, 0, 6, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel key = new JLabel(label);
        key.setPreferredSize(new Dimension(105, 22));
        key.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        key.setForeground(TEXT_MUTED);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 13));
        val.setForeground(TEXT_MAIN);

        JButton copy = createSmallButton("Sao chép");
        copy.addActionListener(e -> copyToClipboard(value));

        row.add(key, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        row.add(copy, BorderLayout.EAST);
        return row;
    }

    private JPanel createSummaryBox() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel timerPanel = new JPanel();
        timerPanel.setOpaque(false);
        timerPanel.setLayout(new BoxLayout(timerPanel, BoxLayout.Y_AXIS));
        JLabel timeText = new JLabel("Thời gian còn lại");
        timeText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        timeText.setForeground(TEXT_SOFT);
        lblTimer = new JLabel("15 : 00");
        lblTimer.setFont(new Font("Segoe UI", Font.BOLD, 27));
        lblTimer.setForeground(PRIMARY);
        timerPanel.add(timeText);
        timerPanel.add(Box.createVerticalStrut(4));
        timerPanel.add(lblTimer);

        JPanel totals = new JPanel();
        totals.setOpaque(false);
        totals.setLayout(new BoxLayout(totals, BoxLayout.Y_AXIS));
        totals.add(summaryRow("Gói " + packageName, currency(basePrice), false));
        lblDiscountAmount = new JLabel("- 0đ");
        totals.add(summaryRowWithValue("Ưu đãi", lblDiscountAmount, false, SUCCESS));
        lblTotalAmount = new JLabel(currency(basePrice));
        totals.add(summaryRowWithValue("Tổng tiền", lblTotalAmount, true, PRIMARY));

        panel.add(timerPanel, BorderLayout.WEST);
        panel.add(totals, BorderLayout.EAST);
        return panel;
    }

    private JPanel summaryRow(String label, String value, boolean large) {
        JLabel valueLabel = new JLabel(value);
        return summaryRowWithValue(label, valueLabel, large, TEXT_MAIN);
    }

    private JPanel summaryRowWithValue(String label, JLabel valueLabel, boolean large, Color valueColor) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        row.setOpaque(false);
        JLabel name = new JLabel(label);
        name.setFont(new Font("Segoe UI", large ? Font.BOLD : Font.PLAIN, large ? 16 : 14));
        name.setForeground(TEXT_MAIN);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, large ? 22 : 14));
        valueLabel.setForeground(valueColor);
        row.add(name);
        row.add(valueLabel);
        return row;
    }

    private JPanel createPromoBox() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panel.setOpaque(false);

        JTextField input = new JTextField(16);
        input.setPreferredSize(new Dimension(190, 38));
        input.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(0, 12, 0, 12)
        ));

        JButton apply = createActionButton("Áp dụng", false);
        apply.setPreferredSize(new Dimension(92, 38));
        apply.addActionListener(e -> {
            if ("GIAM30K".equalsIgnoreCase(input.getText().trim())) {
                discount = 30000;
                updateTotal();
                showMessage("Áp dụng mã thành công.", SUCCESS);
            } else {
                showMessage("Mã giảm giá không hợp lệ.", DANGER);
            }
        });

        panel.add(input);
        panel.add(apply);
        return panel;
    }

    private JComponent createConfirmButton() {
        GradientButton button = new GradientButton("▣  Xác nhận thanh toán", PRIMARY, PRIMARY_DARK);
        button.setPreferredSize(new Dimension(0, 54));
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setForeground(Color.WHITE);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> {
            button.setText("Đang xử lý giao dịch...");
            Timer t = new Timer(1200, ev -> {
                showMessage("Thanh toán thành công. Gói " + packageName + " đã sẵn sàng.", SUCCESS);
                dispose();
            });
            t.setRepeats(false);
            t.start();
        });
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(18, 0, 0, 0));
        footer.add(button, BorderLayout.CENTER);
        return footer;
    }

    private JPanel createFeatureRow(String text, boolean enabled) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel icon = new JLabel(enabled ? "✓" : "–");
        icon.setFont(new Font("Segoe UI", Font.BOLD, 14));
        icon.setForeground(enabled ? PRIMARY : Color.decode("#A8B0BD"));
        icon.setPreferredSize(new Dimension(18, 18));

        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(enabled ? TEXT_MAIN : TEXT_MUTED);

        row.add(icon);
        row.add(label);
        return row;
    }

    private JPanel createSecureLine() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(new SymbolIcon("▣", Color.decode("#98A2B3"), 14));
        JLabel text = new JLabel("Thanh toán bảo mật, thông tin chuyển khoản rõ ràng.");
        text.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        text.setForeground(TEXT_MUTED);
        row.add(text);
        return row;
    }

    private JButton createIconButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 30));
        button.setForeground(Color.decode("#98A2B3"));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(40, 40));
        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                button.setForeground(DANGER);
            }

            @Override public void mouseExited(MouseEvent e) {
                button.setForeground(Color.decode("#98A2B3"));
            }
        });
        return button;
    }

    private JButton createSmallButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setForeground(PRIMARY);
        button.setBackground(Color.decode("#EEF2FF"));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.decode("#C7D2FE")),
                new EmptyBorder(3, 8, 3, 8)
        ));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createActionButton(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(primary ? Color.WHITE : TEXT_SOFT);
        button.setBackground(primary ? PRIMARY : Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? PRIMARY : Color.decode("#D0D5DD")),
                new EmptyBorder(8, 14, 8, 14)
        ));
        return button;
    }

    private JPanel centerWrap(JComponent component) {
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrap.setOpaque(false);
        wrap.add(component);
        return wrap;
    }

    private void setLocalIcon(JLabel label, String path, int width, int height) {
        try {
            URL url = getClass().getResource(path);
            if (url == null) {
                label.setText("QR");
                label.setFont(new Font("Segoe UI", Font.BOLD, 28));
                label.setForeground(PRIMARY);
                return;
            }
            ImageIcon raw = new ImageIcon(url);
            Image image = raw.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            label.setIcon(new ImageIcon(image));
        } catch (Exception ex) {
            label.setText("QR");
        }
    }

    private void copyToClipboard(String value) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), null);
        showMessage("Đã sao chép: " + value, PRIMARY);
    }

    private void openLink(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            showMessage("Không thể mở trình duyệt.", DANGER);
        }
    }

    private void showMessage(String message, Color color) {
        JOptionPane.showMessageDialog(this, message);
    }

    private void updateTotal() {
        lblDiscountAmount.setText("- " + currency(discount));
        lblTotalAmount.setText(currency(basePrice - discount));
    }

    private void startTimer() {
        countdownTimer = new Timer(1000, e -> {
            timeLeft--;
            int minutes = timeLeft / 60;
            int seconds = timeLeft % 60;
            lblTimer.setText(String.format("%02d : %02d", minutes, seconds));
            if (timeLeft <= 0) {
                countdownTimer.stop();
                showMessage("Hết thời gian thanh toán. Vui lòng thử lại.", DANGER);
                dispose();
            }
        });
        countdownTimer.start();
    }

    @Override public void dispose() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        super.dispose();
    }

    private boolean isVip() {
        return "VIP".equalsIgnoreCase(packageName);
    }

    private PlanFeature[] getFeatures() {
        if ("Basic".equalsIgnoreCase(packageName)) {
            return new PlanFeature[] {
                    new PlanFeature("Nhận tối đa 15 lớp / tháng", true),
                    new PlanFeature("Tìm kiếm lớp học nâng cao", true),
                    new PlanFeature("Nhắn tin không giới hạn", true),
                    new PlanFeature("Ưu tiên hiển thị lớp học", true),
                    new PlanFeature("Thống kê thu nhập chi tiết", false),
                    new PlanFeature("Hỗ trợ ưu tiên", false)
            };
        }
        if ("Premium".equalsIgnoreCase(packageName)) {
            return new PlanFeature[] {
                    new PlanFeature("Nhận không giới hạn lớp", true),
                    new PlanFeature("Tìm kiếm lớp học nâng cao", true),
                    new PlanFeature("Nhắn tin không giới hạn", true),
                    new PlanFeature("Ưu tiên hiển thị lớp học", true),
                    new PlanFeature("Thống kê thu nhập chi tiết", true),
                    new PlanFeature("Hỗ trợ ưu tiên 24/7", true)
            };
        }
        if ("VIP".equalsIgnoreCase(packageName)) {
            return new PlanFeature[] {
                    new PlanFeature("Nhận không giới hạn lớp", true),
                    new PlanFeature("Tìm kiếm lớp học nâng cao", true),
                    new PlanFeature("Nhắn tin không giới hạn", true),
                    new PlanFeature("Ưu tiên hiển thị Top VIP", true),
                    new PlanFeature("Thống kê thu nhập chi tiết", true),
                    new PlanFeature("Hỗ trợ riêng 1-1", true)
            };
        }
        return new PlanFeature[] {
                new PlanFeature("Nhận tối đa 3 lớp / tháng", true),
                new PlanFeature("Tìm kiếm lớp học cơ bản", true),
                new PlanFeature("Nhắn tin với phụ huynh", true),
                new PlanFeature("Ưu tiên hiển thị lớp học", false),
                new PlanFeature("Thống kê thu nhập chi tiết", false),
                new PlanFeature("Hỗ trợ ưu tiên", false)
        };
    }

    private String currency(double value) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(value).replace(",", ".") + "đ";
    }

    private static final class PlanFeature {
        private final String text;
        private final boolean enabled;

        private PlanFeature(String text, boolean enabled) {
            this.text = text;
            this.enabled = enabled;
        }
    }

    private static class RoundedPanel extends JPanel {
        private final Color fill;
        private final int radius;
        private final Color stroke;

        RoundedPanel(Color fill, int radius, Color stroke) {
            this.fill = fill;
            this.radius = radius;
            this.stroke = stroke;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            if (stroke != null) {
                g2.setColor(stroke);
                g2.setStroke(new BasicStroke(1.1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class GradientButton extends JButton {
        private final Color start;
        private final Color end;

        GradientButton(String text, Color start, Color end) {
            super(text);
            this.start = start;
            this.end = end;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, start, getWidth(), getHeight(), end));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class SymbolIcon extends JLabel {
        SymbolIcon(String text, Color color, int size) {
            super(text);
            setFont(new Font("Segoe UI", Font.BOLD, size));
            setForeground(color);
        }
    }
}
