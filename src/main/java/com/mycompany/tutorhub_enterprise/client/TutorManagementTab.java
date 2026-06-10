package com.mycompany.tutorhub_enterprise.client;

import com.mycompany.tutorhub_enterprise.models.Packet;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TutorManagementTab extends JPanel {

    private final Color BG_MAIN = Color.decode("#F8FAFC");
    private final Color BG_WHITE = Color.WHITE;
    private final Color PRIMARY_BLUE = Color.decode("#2563EB");
    private final Color SUCCESS_GREEN = Color.decode("#10B981");
    private final Color WARNING_ORANGE = Color.decode("#F59E0B");
    private final Color DANGER_RED = Color.decode("#EF4444");
    private final Color TEXT_MAIN = Color.decode("#0F172A");
    private final Color TEXT_MUTED = Color.decode("#64748B");
    private final Color BORDER_COLOR = Color.decode("#E2E8F0");

    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JFrame parentFrame;

    // Các thành phần UI cần truy cập toàn cục
    private JTextField txtSearch;
    private JComboBox<String> cbSubject, cbLocation, cbStatus;
    private JLabel valActive = new JLabel("0"), valPending = new JLabel("0"), valPaused = new JLabel("0"), valLocked = new JLabel("0"), valTotal = new JLabel("0");
    private JLabel pctActive = new JLabel("0%"), pctPending = new JLabel("0%"), pctPaused = new JLabel("0%"), pctLocked = new JLabel("0%");

    public TutorManagementTab(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout(0, 20));
        setBackground(BG_MAIN);
        setBorder(new EmptyBorder(24, 30, 24, 30));

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.setOpaque(false);
        
        northPanel.add(createHeaderSection());
        northPanel.add(Box.createVerticalStrut(20));
        northPanel.add(createFilterSection());
        northPanel.add(Box.createVerticalStrut(20));
        northPanel.add(createKPISection());
        northPanel.add(Box.createVerticalStrut(20));

        add(northPanel, BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);
        
        setupFilters(); // Kích hoạt bộ lọc
    }

    private JPanel createHeaderSection() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JPanel titleWrap = new JPanel(); titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS)); titleWrap.setOpaque(false);
        JLabel lblTitle = new JLabel("Quản lý gia sư"); lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26)); lblTitle.setForeground(TEXT_MAIN);
        JLabel lblSub = new JLabel("Quản lý thông tin và hiệu suất của toàn bộ gia sư trên hệ thống"); lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 14)); lblSub.setForeground(TEXT_MUTED);
        titleWrap.add(lblTitle); titleWrap.add(Box.createVerticalStrut(4)); titleWrap.add(lblSub);
        headerPanel.add(titleWrap, BorderLayout.WEST);
        return headerPanel;
    }

    private JPanel createFilterSection() {
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        filterPanel.setOpaque(false);

        JPanel searchBox = new JPanel(new BorderLayout(10, 0));
        searchBox.setBackground(BG_WHITE);
        searchBox.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1), new EmptyBorder(8, 15, 8, 15)));
        searchBox.setPreferredSize(new Dimension(320, 42));
        JLabel searchIcon = new JLabel(); setNetworkIcon(searchIcon, "https://img.icons8.com/fluency-systems-regular/48/94A3B8/search.png", 18, 18);
        txtSearch = new JTextField(); txtSearch.setBorder(null); txtSearch.putClientProperty("JTextField.placeholderText", "Tìm kiếm gia sư theo tên, email..."); txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchBox.add(searchIcon, BorderLayout.WEST); searchBox.add(txtSearch, BorderLayout.CENTER);

        cbSubject = createCustomComboBox(new String[]{"Tất cả môn học", "Toán học", "Tiếng Anh", "Vật lý", "Hóa học", "Sinh học"});
        cbLocation = createCustomComboBox(new String[]{"Tất cả khu vực", "Toàn quốc", "Hà Nội", "TP. Hồ Chí Minh", "Đà Nẵng", "Hải Phòng"});
        cbStatus = createCustomComboBox(new String[]{"Tất cả trạng thái", "Đang dạy", "Chờ duyệt", "Tạm nghỉ", "Đã khóa"});

        JPanel btnReset = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0)); btnReset.setOpaque(false); btnReset.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JLabel iconReset = new JLabel(); setNetworkIcon(iconReset, "https://img.icons8.com/fluency-systems-regular/48/64748B/refresh.png", 16, 16);
        JLabel lblReset = new JLabel("Đặt lại"); lblReset.setForeground(TEXT_MUTED); lblReset.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnReset.add(iconReset); btnReset.add(lblReset);
        
        // Sự kiện Đặt lại
        btnReset.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                txtSearch.setText(""); cbSubject.setSelectedIndex(0); cbLocation.setSelectedIndex(0); cbStatus.setSelectedIndex(0);
            }
        });

        JButton btnAdd = new JButton("+ Thêm gia sư");
        btnAdd.setBackground(PRIMARY_BLUE); btnAdd.setForeground(Color.WHITE); btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAdd.setFocusPainted(false); btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR)); btnAdd.setPreferredSize(new Dimension(140, 42)); btnAdd.setBorder(new EmptyBorder(0,0,0,0));
        btnAdd.addActionListener(e -> new CreateTutorDialog(parentFrame, () -> { try { NetworkManager.getInstance().sendPacket(new Packet("GET_ALL_TUTORS", "")); } catch (Exception ex) {} }).setVisible(true));

        filterPanel.add(searchBox); filterPanel.add(cbSubject); filterPanel.add(cbLocation); filterPanel.add(cbStatus); filterPanel.add(btnReset); filterPanel.add(Box.createHorizontalStrut(10)); filterPanel.add(btnAdd);
        return filterPanel;
    }

    private JComboBox<String> createCustomComboBox(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items); cb.setFont(new Font("Segoe UI", Font.PLAIN, 14)); cb.setBackground(BG_WHITE); cb.setForeground(TEXT_MAIN); cb.setPreferredSize(new Dimension(160, 42)); cb.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        return cb;
    }

    private JPanel createKPISection() {
        JPanel kpiRow = new JPanel(new GridLayout(1, 5, 20, 0)); kpiRow.setOpaque(false);
        kpiRow.add(createKPICard("Đang dạy", valActive, pctActive, "https://img.icons8.com/fluency-systems-regular/48/10B981/graduation-cap.png", SUCCESS_GREEN, Color.decode("#D1FAE5")));
        kpiRow.add(createKPICard("Chờ duyệt", valPending, pctPending, "https://img.icons8.com/fluency-systems-regular/48/F59E0B/time.png", WARNING_ORANGE, Color.decode("#FEF3C7")));
        kpiRow.add(createKPICard("Tạm nghỉ", valPaused, pctPaused, "https://img.icons8.com/fluency-systems-regular/48/64748B/pause.png", TEXT_MUTED, Color.decode("#F1F5F9")));
        kpiRow.add(createKPICard("Đã khóa", valLocked, pctLocked, "https://img.icons8.com/fluency-systems-regular/48/EF4444/lock.png", DANGER_RED, Color.decode("#FEE2E2")));
        kpiRow.add(createKPICard("Tổng gia sư", valTotal, new JLabel("100%"), "https://img.icons8.com/fluency-systems-regular/48/2563EB/conference-call.png", PRIMARY_BLUE, Color.decode("#DBEAFE")));
        return kpiRow;
    }

    private JPanel createKPICard(String title, JLabel lblVal, JLabel lblPct, String iconUrl, Color mainColor, Color bgLightColor) {
        RoundedPanel card = new RoundedPanel(16, BG_WHITE); card.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 20)); card.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        JPanel iconWrap = new JPanel(new BorderLayout()) { @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(bgLightColor); g2.fillOval(0,0,getWidth(),getHeight()); g2.dispose(); } };
        iconWrap.setPreferredSize(new Dimension(54, 54)); iconWrap.setOpaque(false);
        JLabel lblIcon = new JLabel("", SwingConstants.CENTER); setNetworkIcon(lblIcon, iconUrl, 28, 28); iconWrap.add(lblIcon, BorderLayout.CENTER);
        JPanel textP = new JPanel(); textP.setLayout(new BoxLayout(textP, BoxLayout.Y_AXIS)); textP.setOpaque(false);
        JLabel lblTitle = new JLabel(title); lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13)); lblTitle.setForeground(TEXT_MAIN);
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, 26)); lblVal.setForeground(TEXT_MAIN);
        lblPct.setFont(new Font("Segoe UI", Font.BOLD, 12)); lblPct.setForeground(mainColor);
        textP.add(lblTitle); textP.add(lblVal); textP.add(lblPct);
        card.add(iconWrap); card.add(textP); return card;
    }

    private JPanel createMainContent() {
        RoundedPanel mainPanel = new RoundedPanel(16, BG_WHITE);
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));

        String[] columns = {"Gia sư", "Mã GS", "Môn học", "Khu vực", "Hiệu suất", "Trạng thái", "Thao tác"};
        tableModel = new DefaultTableModel(columns, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } };
        table = new JTable(tableModel);
        rowSorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(rowSorter);

        table.setRowHeight(80); table.setShowGrid(false); table.setIntercellSpacing(new Dimension(0, 0)); table.setSelectionBackground(Color.decode("#F8FAFC")); table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JTableHeader header = table.getTableHeader(); header.setFont(new Font("Segoe UI", Font.BOLD, 13)); header.setBackground(BG_WHITE); header.setForeground(TEXT_MUTED); header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR)); header.setPreferredSize(new Dimension(0, 45));

        table.getColumnModel().getColumn(0).setCellRenderer(new TutorInfoRenderer()); table.getColumnModel().getColumn(0).setPreferredWidth(280);
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTextRenderer(PRIMARY_BLUE, true));
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTextRenderer(TEXT_MAIN, false));
        table.getColumnModel().getColumn(3).setCellRenderer(new LocationRenderer());
        table.getColumnModel().getColumn(4).setCellRenderer(new PerformanceRenderer()); table.getColumnModel().getColumn(4).setPreferredWidth(180);
        table.getColumnModel().getColumn(5).setCellRenderer(new BadgeRenderer());
        table.getColumnModel().getColumn(6).setCellRenderer(new ActionRenderer());

        // --- BẮT SỰ KIỆN CLICK VÀO CỘT THAO TÁC (ĐÃ FIX LỖI LẶP) ---
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 6) {
                    int modelRow = table.convertRowIndexToModel(row);
                    String tutorId = tableModel.getValueAt(modelRow, 1).toString().replace("GS", "");
                    String tutorInfo = tableModel.getValueAt(modelRow, 0).toString();
                    String tutorName = tutorInfo.split("\\|")[1];
                    String currentStatus = tableModel.getValueAt(modelRow, 5).toString();

                    Rectangle cellRect = table.getCellRect(row, col, false);
                    int clickX = e.getX() - cellRect.x;

                    // Tọa độ vẽ icon (FlowLayout Left 8px, gap 8px, size 30px)
                    if (clickX >= 8 && clickX <= 38) {
                        // NÚT 1: XEM CHI TIẾT
                        if (parentFrame instanceof CenterDashboard) {
                            ((CenterDashboard) parentFrame).showTutorProfileDetail(tutorId);
                        }
                    } else if (clickX >= 46 && clickX <= 76) {
                        // NÚT 2: SỬA TRẠNG THÁI
                        String[] options = {"Đang dạy", "Chờ duyệt", "Tạm nghỉ", "Đã khóa"};
                        String newStatus = (String) JOptionPane.showInputDialog(TutorManagementTab.this, "Cập nhật trạng thái cho " + tutorName + ":", "Cập nhật trạng thái", JOptionPane.QUESTION_MESSAGE, null, options, currentStatus);
                        if (newStatus != null && !newStatus.equals(currentStatus)) {
                            updateTutorStatus(tutorId, newStatus);
                        }
                    } else if (clickX >= 84 && clickX <= 114) {
                        // NÚT 3: KHÓA TÀI KHOẢN (Đỏ)
                        int confirm = JOptionPane.showConfirmDialog(TutorManagementTab.this, "Bạn có chắc chắn muốn khóa tài khoản " + tutorName + "?", "Khóa tài khoản", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (confirm == JOptionPane.YES_OPTION) {
                            updateTutorStatus(tutorId, "Đã khóa");
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table); scrollPane.setBorder(null); scrollPane.getViewport().setBackground(BG_WHITE);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        return mainPanel;
    }

    // --- LOGIC LỌC DỮ LIỆU ---
    private void setupFilters() {
        DocumentListener searchListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilters(); }
            public void removeUpdate(DocumentEvent e) { applyFilters(); }
            public void changedUpdate(DocumentEvent e) { applyFilters(); }
        };
        txtSearch.getDocument().addDocumentListener(searchListener);
        cbSubject.addActionListener(e -> applyFilters());
        cbLocation.addActionListener(e -> applyFilters());
        cbStatus.addActionListener(e -> applyFilters());
    }

    private void applyFilters() {
        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        String text = txtSearch.getText().trim();
        if (!text.isEmpty()) filters.add(RowFilter.regexFilter("(?i)" + text, 0)); // Lọc theo Tên/Email ở cột 0
        if (cbSubject.getSelectedIndex() > 0) filters.add(RowFilter.regexFilter("(?i)^" + cbSubject.getSelectedItem().toString() + "$", 2));
        if (cbLocation.getSelectedIndex() > 0) filters.add(RowFilter.regexFilter("(?i)^" + cbLocation.getSelectedItem().toString() + "$", 3));
        if (cbStatus.getSelectedIndex() > 0) filters.add(RowFilter.regexFilter("(?i)^" + cbStatus.getSelectedItem().toString() + "$", 5));

        if (filters.isEmpty()) rowSorter.setRowFilter(null);
        else rowSorter.setRowFilter(RowFilter.andFilter(filters));
    }

    // --- GỬI LỆNH UPDATE LÊN SERVER ---
    private void updateTutorStatus(String targetUserId, String newStatus) {
        try {
            NetworkManager.getInstance().sendPacket(new Packet("UPDATE_TUTOR_STATUS", targetUserId + "|" + newStatus));
            JOptionPane.showMessageDialog(this, "Đã gửi yêu cầu cập nhật lên Server!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối mạng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void updateTutorList(List<String> tutors) {
        tableModel.setRowCount(0); 
        int active = 0, pending = 0, paused = 0, locked = 0, total = tutors.size();
        for (String row : tutors) {
            String[] parts = row.split(";;");
            if (parts.length >= 6) {
                tableModel.addRow(new Object[]{parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], ""});
                String status = parts[5].trim();
                if (status.equals("Đang dạy")) active++; else if (status.equals("Chờ duyệt")) pending++; else if (status.equals("Tạm nghỉ")) paused++; else if (status.equals("Đã khóa")) locked++;
            }
        }
        valTotal.setText(String.valueOf(total)); valActive.setText(String.valueOf(active)); valPending.setText(String.valueOf(pending)); valPaused.setText(String.valueOf(paused)); valLocked.setText(String.valueOf(locked));
        if (total > 0) {
            pctActive.setText(String.format("%.1f%%", (active * 100.0) / total)); pctPending.setText(String.format("%.1f%%", (pending * 100.0) / total)); pctPaused.setText(String.format("%.1f%%", (paused * 100.0) / total)); pctLocked.setText(String.format("%.1f%%", (locked * 100.0) / total));
        }
    }

    // =========================================================================
    // CÁC CUSTOM RENDERER VẼ BẢNG
    // =========================================================================
    class TutorInfoRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private JLabel lblAva, lblName, lblEmail, lblPhone;
        public TutorInfoRenderer() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 15, 10)); setOpaque(true);
            lblAva = new JLabel(); lblAva.setPreferredSize(new Dimension(48, 48));
            JPanel textPanel = new JPanel(); textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS)); textPanel.setOpaque(false);
            lblName = new JLabel(); lblName.setFont(new Font("Segoe UI", Font.BOLD, 14)); lblName.setForeground(TEXT_MAIN);
            lblEmail = new JLabel(); lblEmail.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblEmail.setForeground(TEXT_MUTED);
            lblPhone = new JLabel(); lblPhone.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblPhone.setForeground(TEXT_MUTED);
            textPanel.add(lblName); textPanel.add(lblEmail); textPanel.add(lblPhone);
            add(lblAva); add(textPanel);
        }
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : BG_WHITE); setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
            if (value != null) {
                String[] p = value.toString().split("\\|");
                if(p.length >= 4) {
                    if (p[0].equals("DEFAULT")) setAvatarNetworkIcon(lblAva, "https://img.icons8.com/color/96/circled-user-male-skin-type-4--v1.png", 48);
                    else { try { byte[] bytes = java.util.Base64.getDecoder().decode(p[0]); Image img = new ImageIcon(bytes).getImage(); BufferedImage cb = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB); Graphics2D g2 = cb.createGraphics(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.fillOval(0, 0, 48, 48); g2.setComposite(AlphaComposite.SrcIn); g2.drawImage(img, 0, 0, 48, 48, null); g2.dispose(); lblAva.setIcon(new ImageIcon(cb)); } catch (Exception e) {} }
                    lblName.setText(p[1]); lblEmail.setText(p[2]); lblPhone.setText(p[3]);
                }
            } return this;
        }
    }

    class DefaultTextRenderer extends DefaultTableCellRenderer {
        private Color color; private boolean isBold;
        public DefaultTextRenderer(Color color, boolean isBold) { this.color = color; this.isBold = isBold; }
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0, BORDER_COLOR), new EmptyBorder(0, 10, 0, 10)));
            lbl.setBackground(isSelected ? table.getSelectionBackground() : BG_WHITE); lbl.setForeground(color); lbl.setFont(new Font("Segoe UI", isBold ? Font.BOLD : Font.PLAIN, 14)); return lbl;
        }
    }

    class LocationRenderer extends DefaultTableCellRenderer {
        public LocationRenderer() { setIconTextGap(8); }
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0, BORDER_COLOR), new EmptyBorder(0, 10, 0, 10)));
            lbl.setBackground(isSelected ? table.getSelectionBackground() : BG_WHITE); lbl.setForeground(TEXT_MUTED); lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            setNetworkIcon(lbl, "https://img.icons8.com/fluency-systems-regular/48/94A3B8/marker.png", 16, 16); return lbl;
        }
    }

    class PerformanceRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private JLabel lblPct, lblSub; private JProgressBar bar;
        public PerformanceRenderer() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(true); setBorder(new EmptyBorder(15, 10, 15, 10));
            lblPct = new JLabel(); lblPct.setFont(new Font("Segoe UI", Font.BOLD, 13)); lblPct.setForeground(TEXT_MAIN);
            bar = new JProgressBar(0, 100); bar.setPreferredSize(new Dimension(100, 6)); bar.setMaximumSize(new Dimension(120, 6)); bar.setBorderPainted(false);
            lblSub = new JLabel(); lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11)); lblSub.setForeground(TEXT_MUTED);
            add(lblPct); add(Box.createVerticalStrut(4)); add(bar); add(Box.createVerticalStrut(4)); add(lblSub);
        }
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : BG_WHITE); setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR), new EmptyBorder(15, 10, 15, 10)));
            if (value != null) {
                String[] p = value.toString().split("\\|");
                if (p.length >= 3) {
                    int pct = Integer.parseInt(p[0]); lblPct.setText(pct + "%"); bar.setValue(pct); lblSub.setText(p[1] + " lớp  •  ⭐ " + p[2]);
                    if(pct >= 80) bar.setForeground(SUCCESS_GREEN); else if(pct >= 50) bar.setForeground(WARNING_ORANGE); else bar.setForeground(DANGER_RED);
                }
            } return this;
        }
    }

    class BadgeRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private JLabel lbl; private RoundedPanel badge;
        public BadgeRenderer() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 10, 25)); setOpaque(true);
            lbl = new JLabel(); lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            badge = new RoundedPanel(12, BG_WHITE); badge.setBorder(new EmptyBorder(4, 10, 4, 10)); badge.add(lbl); add(badge);
        }
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : BG_WHITE); setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
            String status = value != null ? value.toString() : ""; lbl.setText("● " + status);
            if(status.equals("Đang dạy")) { badge.bgColor = Color.decode("#DCFCE7"); lbl.setForeground(Color.decode("#15803D")); } 
            else if(status.equals("Chờ duyệt")) { badge.bgColor = Color.decode("#FEF3C7"); lbl.setForeground(Color.decode("#B45309")); } 
            else if(status.equals("Bị khóa")) { badge.bgColor = Color.decode("#FEE2E2"); lbl.setForeground(Color.decode("#B91C1C")); }
            else { badge.bgColor = Color.decode("#F1F5F9"); lbl.setForeground(Color.decode("#475569")); }
            return this;
        }
    }

    class ActionRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        public ActionRenderer() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 8, 25)); setOpaque(true);
            add(createIconLabel("https://img.icons8.com/fluency-systems-regular/48/94A3B8/visible.png"));
            add(createIconLabel("https://img.icons8.com/fluency-systems-regular/48/94A3B8/edit.png"));
            add(createIconLabel("https://img.icons8.com/fluency-systems-regular/48/EF4444/lock.png")); // Nút khóa màu đỏ
        }
        private JLabel createIconLabel(String url) { JLabel l = new JLabel(); setNetworkIcon(l, url, 18, 18); l.setBorder(BorderFactory.createLineBorder(BORDER_COLOR)); l.setPreferredSize(new Dimension(30, 30)); l.setHorizontalAlignment(SwingConstants.CENTER); return l; }
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : BG_WHITE); setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR)); return this;
        }
    }

    class RoundedPanel extends JPanel { public Color bgColor; private int radius; public RoundedPanel(int radius, Color bgColor) { this.radius = radius; this.bgColor = bgColor; setOpaque(false); } @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(bgColor); g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius); g2.dispose(); } }
    private void setNetworkIcon(JLabel label, String urlStr, int width, int height) { new Thread(() -> { try { ImageIcon raw = new ImageIcon(new URL(urlStr)); Image img = raw.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH); SwingUtilities.invokeLater(() -> label.setIcon(new ImageIcon(img))); } catch (Exception ignored) {} }).start(); }
    private void setAvatarNetworkIcon(JLabel label, String urlStr, int size) { new Thread(() -> { try { ImageIcon raw = new ImageIcon(new URL(urlStr)); BufferedImage cb = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB); Graphics2D g2 = cb.createGraphics(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.fillOval(0, 0, size, size); g2.setComposite(AlphaComposite.SrcIn); g2.drawImage(raw.getImage(), 0, 0, size, size, null); g2.dispose(); SwingUtilities.invokeLater(() -> label.setIcon(new ImageIcon(cb))); } catch (Exception ignored) {} }).start(); }
}