package com.mycompany.tutorhub_enterprise.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TodoListTab extends JPanel {

    // ===== BẢNG MÀU CHUẨN MỰC SAAS =====
    private final Color BG_MAIN = Color.decode("#F8FAFC"); // Nền xám xanh cực nhạt
    private final Color TEXT_MAIN = Color.decode("#111827");
    private final Color TEXT_MUTED = Color.decode("#6B7280");
    private final Color PRIMARY = Color.decode("#2563EB");
    private final Color BORDER_COLOR = Color.decode("#E5E7EB");

    // Dữ liệu từ Database
    private List<Task> tasks = new ArrayList<>();
    private JPanel listPanel;
    private String currentFilter = "ALL";
    private Runnable onBackListener;

    public TodoListTab() {
        setLayout(new BorderLayout());
        setBackground(BG_MAIN);

        // 1. HEADER (Tiêu đề & Nút Back)
        add(createHeader(), BorderLayout.NORTH);

        // 2. MAIN CONTENT (Bộ lọc + Danh sách)
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(0, 40, 20, 40));

        centerPanel.add(createFilterSection(), BorderLayout.NORTH);
        
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0)); // Thanh cuộn ẩn
        
        centerPanel.add(scroll, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // 3. PAGINATION (Phân trang dưới cùng)
        add(createPagination(), BorderLayout.SOUTH);
        
        // Khởi tạo giao diện rỗng ban đầu (chờ Server gửi data về)
        renderTaskList();
    }

    public void setOnBackListener(Runnable listener) {
        this.onBackListener = listener;
    }

    // =========================================================================
    // HÀM NHẬN DỮ LIỆU TỪ SERVER VÀ CẬP NHẬT GIAO DIỆN
    // =========================================================================
    public void updateTasksFromServer(String payload) {
        SwingUtilities.invokeLater(() -> {
            tasks.clear(); // Xóa dữ liệu cũ
            
            if (payload == null || payload.trim().isEmpty()) {
                renderTaskList();
                return;
            }

            String[] rows = payload.split(";;");
            for (String row : rows) {
                String[] cols = row.split("\\|");
                // Format mong đợi: id | category | title | time | location | is_completed
                if (cols.length >= 6) {
                    String id = cols[0];
                    String category = cols[1];
                    String title = cols[2];
                    String time = cols[3];
                    String loc = cols[4];
                    boolean isCompleted = Boolean.parseBoolean(cols[5]);

                    // Auto-map UI dựa vào trạng thái và Category từ Database
                    String statusText = isCompleted ? "Đã hoàn thành" : "Sắp diễn ra";
                    String statusColor = isCompleted ? "#059669" : "#2563EB";
                    String timeLeft = isCompleted ? "Xong" : "Đang chờ";
                    
                    // Mặc định là Lớp dạy (TEACH)
                    String iconUrl = "https://img.icons8.com/fluency-systems-regular/48/2563EB/open-book.png";
                    String iconBg = "#EFF6FF";

                    if (category.equals("DOCS")) {
                        iconUrl = "https://img.icons8.com/fluency-systems-regular/48/10B981/document.png";
                        iconBg = "#ECFDF5";
                        if (!isCompleted) { statusText = "Sắp đến hạn"; statusColor = "#D97706"; }
                    } else if (category.equals("NOTIFY")) {
                        iconUrl = "https://img.icons8.com/fluency-systems-regular/48/F59E0B/bell.png";
                        iconBg = "#FFFBEB";
                        if (!isCompleted) { statusText = "Mới"; statusColor = "#DC2626"; }
                    } else if (category.equals("HW")) {
                        iconUrl = "https://img.icons8.com/fluency-systems-regular/48/8B5CF6/clipboard.png";
                        iconBg = "#F5F3FF";
                        if (!isCompleted) { statusText = "Cần làm"; statusColor = "#8B5CF6"; }
                    }

                    // Chèn vào list hiển thị
                    tasks.add(new Task(category, title, time, loc, statusText, statusColor, timeLeft, iconUrl, iconBg));
                }
            }
            renderTaskList(); // Vẽ lại giao diện với data mới
        });
    }

    // =========================================================================
    // 1. HEADER BÊN TRÊN
    // =========================================================================
    private JPanel createHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(30, 40, 20, 40));

        // Nút Back tròn
        JPanel btnBack = new JPanel(new BorderLayout()) {
            boolean hover = false;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hover ? Color.decode("#E2E8F0") : Color.WHITE);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(BORDER_COLOR);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawOval(1, 1, getWidth()-3, getHeight()-3);
                g2.dispose();
            }
        };
        btnBack.setPreferredSize(new Dimension(44, 44));
        btnBack.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JLabel iconBack = new JLabel("<", SwingConstants.CENTER);
        iconBack.setFont(new Font("Segoe UI", Font.BOLD, 18));
        iconBack.setForeground(TEXT_MAIN);
        btnBack.add(iconBack, BorderLayout.CENTER);
        
        btnBack.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { try { btnBack.getClass().getDeclaredField("hover").set(btnBack, true); btnBack.repaint(); } catch(Exception ex){} }
            @Override public void mouseExited(MouseEvent e) { try { btnBack.getClass().getDeclaredField("hover").set(btnBack, false); btnBack.repaint(); } catch(Exception ex){} }
            @Override public void mouseClicked(MouseEvent e) { if(onBackListener != null) onBackListener.run(); }
        });

        // Tiêu đề
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Việc cần làm");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(TEXT_MAIN);
        JLabel lblSub = new JLabel("Danh sách tất cả công việc cần xử lý");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSub.setForeground(TEXT_MUTED);
        
        titlePanel.add(lblTitle);
        titlePanel.add(Box.createVerticalStrut(4));
        titlePanel.add(lblSub);

        header.add(btnBack);
        header.add(titlePanel);
        return header;
    }

    // =========================================================================
    // 2. KHU VỰC BỘ LỌC (TABS & DROPDOWNS)
    // =========================================================================
    private JPanel createFilterSection() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, 20, 0));

        // Tabs bên trái
        JPanel tabsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        tabsPanel.setOpaque(false);
        tabsPanel.add(createFilterTab("Tất cả", "ALL"));
        tabsPanel.add(createFilterTab("Lớp dạy", "TEACH"));
        tabsPanel.add(createFilterTab("Nộp tài liệu", "DOCS"));
        tabsPanel.add(createFilterTab("Bài tập", "HW"));
        tabsPanel.add(createFilterTab("Thông báo", "NOTIFY"));
        p.add(tabsPanel, BorderLayout.WEST);

        // Dropdowns bên phải
        JPanel rightFilter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightFilter.setOpaque(false);
        
        JComboBox<String> cbStatus = new JComboBox<>(new String[]{"Tất cả trạng thái", "Sắp diễn ra", "Sắp đến hạn", "Đã hoàn thành"});
        cbStatus.setPreferredSize(new Dimension(160, 38));
        cbStatus.setBackground(Color.WHITE);
        cbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        JComboBox<String> cbSort = new JComboBox<>(new String[]{"Mới nhất", "Gần deadline", "Ưu tiên cao"});
        cbSort.setPreferredSize(new Dimension(130, 38));
        cbSort.setBackground(Color.WHITE);
        cbSort.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        rightFilter.add(cbStatus);
        rightFilter.add(cbSort);
        p.add(rightFilter, BorderLayout.EAST);

        return p;
    }

    private JPanel createFilterTab(String title, String type) {
        JPanel tab = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isActive = currentFilter.equals(type);
                g2.setColor(isActive ? Color.WHITE : BG_MAIN);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 36, 36);
                g2.setColor(isActive ? PRIMARY : BORDER_COLOR);
                g2.setStroke(new BasicStroke(isActive ? 1.5f : 1f));
                g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 36, 36);
                g2.dispose();
            }
        };
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(8, 20, 8, 20));
        tab.setCursor(new Cursor(Cursor.HAND_CURSOR));

        boolean isActive = currentFilter.equals(type);
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", isActive ? Font.BOLD : Font.PLAIN, 14));
        lbl.setForeground(isActive ? PRIMARY : TEXT_MAIN);
        tab.add(lbl, BorderLayout.CENTER);

        tab.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                currentFilter = type;
                Container parent = tab.getParent();
                parent.repaint();
                renderTaskList();
            }
        });

        return tab;
    }

    // =========================================================================
    // 3. DANH SÁCH TASK (CARDS)
    // =========================================================================
    private void renderTaskList() {
        listPanel.removeAll();
        
        if (tasks.isEmpty()) {
            JLabel lblEmpty = new JLabel("Chưa có công việc nào.");
            lblEmpty.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            lblEmpty.setForeground(TEXT_MUTED);
            lblEmpty.setBorder(new EmptyBorder(40, 20, 0, 0));
            listPanel.add(lblEmpty);
        } else {
            for (Task t : tasks) {
                if (currentFilter.equals("ALL") || currentFilter.equals(t.categoryType)) {
                    listPanel.add(createTaskCard(t));
                    listPanel.add(Box.createVerticalStrut(12)); // Khoảng cách giữa các card
                }
            }
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel createTaskCard(Task t) {
        JPanel card = new JPanel(new BorderLayout(20, 0)) {
            boolean hover = false;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Đổ bóng (Shadow) nhẹ khi hover
                if (hover) {
                    g2.setColor(new Color(0, 0, 0, 10));
                    g2.fillRoundRect(2, 4, getWidth()-4, getHeight()-4, 16, 16);
                }
                
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth()-2, getHeight()-2, 16, 16);
                g2.setColor(hover ? PRIMARY : BORDER_COLOR);
                g2.setStroke(new BasicStroke(hover ? 1.5f : 1f));
                g2.drawRoundRect(1, 1, getWidth()-4, getHeight()-4, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(20, 24, 20, 24));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // --- CỘT TRÁI: ICON ---
        JPanel iconWrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode(t.iconBgColor));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        iconWrapper.setOpaque(false);
        iconWrapper.setPreferredSize(new Dimension(48, 48));
        JLabel icon = new JLabel();
        setNetworkIcon(icon, t.iconUrl, 24, 24);
        iconWrapper.add(icon, BorderLayout.CENTER);
        card.add(iconWrapper, BorderLayout.WEST);

        // --- CỘT GIỮA: NỘI DUNG ---
        JPanel centerInfo = new JPanel();
        centerInfo.setLayout(new BoxLayout(centerInfo, BoxLayout.Y_AXIS));
        centerInfo.setOpaque(false);
        
        JLabel lblTitle = new JLabel(t.title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(TEXT_MAIN);
        
        JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        metaRow.setOpaque(false);
        metaRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        metaRow.add(createIconTextRow("https://img.icons8.com/fluency-systems-regular/48/6B7280/calendar--v1.png", t.time));
        if (t.location != null && !t.location.isEmpty()) {
            metaRow.add(createIconTextRow("https://img.icons8.com/fluency-systems-regular/48/6B7280/marker.png", t.location));
        }

        centerInfo.add(lblTitle);
        centerInfo.add(Box.createVerticalStrut(6));
        centerInfo.add(metaRow);
        card.add(centerInfo, BorderLayout.CENTER);

        // --- CỘT PHẢI: TRẠNG THÁI & ARROW ---
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 0));
        rightPanel.setOpaque(false);
        
        JPanel statusWrap = new JPanel();
        statusWrap.setLayout(new BoxLayout(statusWrap, BoxLayout.Y_AXIS));
        statusWrap.setOpaque(false);
        
        JLabel lblStatus = new JLabel(t.statusText);
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setForeground(Color.decode(t.statusColor));
        lblStatus.setAlignmentX(Component.RIGHT_ALIGNMENT);
        
        JLabel lblTimeLeft = new JLabel(t.timeLeft);
        lblTimeLeft.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTimeLeft.setForeground(TEXT_MUTED);
        lblTimeLeft.setAlignmentX(Component.RIGHT_ALIGNMENT);
        
        statusWrap.add(lblStatus);
        statusWrap.add(Box.createVerticalStrut(4));
        statusWrap.add(lblTimeLeft);
        
        JLabel arrow = new JLabel("›");
        arrow.setFont(new Font("Segoe UI", Font.PLAIN, 28));
        arrow.setForeground(Color.decode("#9CA3AF"));

        rightPanel.add(statusWrap);
        rightPanel.add(arrow);
        
        card.add(rightPanel, BorderLayout.EAST);

        // Hiệu ứng hover cho thẻ
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { try { card.getClass().getDeclaredField("hover").set(card, true); card.repaint(); } catch(Exception ex){} }
            @Override public void mouseExited(MouseEvent e) { try { card.getClass().getDeclaredField("hover").set(card, false); card.repaint(); } catch(Exception ex){} }
            @Override public void mouseClicked(MouseEvent e) { JOptionPane.showMessageDialog(card, "Chi tiết nhiệm vụ: " + t.title); }
        });

        return card;
    }

    private JPanel createIconTextRow(String iconUrl, String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);
        JLabel icon = new JLabel();
        setNetworkIcon(icon, iconUrl, 16, 16);
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(TEXT_MUTED);
        p.add(icon);
        p.add(lbl);
        return p;
    }

    // =========================================================================
    // 4. PHÂN TRANG (PAGINATION)
    // =========================================================================
    private JPanel createPagination() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        p.setOpaque(false);
        
        p.add(createPageButton("<", false, false));
        p.add(createPageButton("1", true, false));
        p.add(createPageButton("2", false, false));
        p.add(createPageButton("3", false, false));
        p.add(new JLabel("..."));
        p.add(createPageButton("8", false, false));
        p.add(createPageButton(">", false, false));
        
        return p;
    }

    private JPanel createPageButton(String text, boolean isActive, boolean isDisabled) {
        JPanel btn = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isActive) {
                    g2.setColor(Color.decode("#DBEAFE")); // Light Blue BG
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                } else if (!isDisabled) {
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(BORDER_COLOR);
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                }
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setPreferredSize(new Dimension(36, 36));
        if (!isDisabled) btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", isActive ? Font.BOLD : Font.PLAIN, 14));
        lbl.setForeground(isActive ? PRIMARY : (isDisabled ? Color.decode("#D1D5DB") : TEXT_MAIN));
        btn.add(lbl, BorderLayout.CENTER);
        
        return btn;
    }

    // =========================================================================
    // TIỆN ÍCH DỮ LIỆU & HÌNH ẢNH
    // =========================================================================
    private void setNetworkIcon(JLabel label, String urlStr, int width, int height) {
        new Thread(() -> { 
            try { 
                ImageIcon icon = new ImageIcon(new URL(urlStr)); 
                Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH); 
                SwingUtilities.invokeLater(() -> label.setIcon(new ImageIcon(img))); 
            } catch (Exception ignored) {} 
        }).start();
    }

    // Model chứa dữ liệu cho mỗi Task
    class Task {
        String categoryType, title, time, location, statusText, statusColor, timeLeft, iconUrl, iconBgColor;
        public Task(String type, String title, String time, String loc, String stText, String stColor, String tLeft, String icon, String iconBg) {
            this.categoryType = type; this.title = title; this.time = time; this.location = loc;
            this.statusText = stText; this.statusColor = stColor; this.timeLeft = tLeft;
            this.iconUrl = icon; this.iconBgColor = iconBg;
        }
    }
}