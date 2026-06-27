package com.mycompany.tutorhub_enterprise.client.exam.ui;

import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

import com.mycompany.tutorhub_enterprise.client.exam.models.*;
import com.mycompany.tutorhub_enterprise.client.exam.services.*;
import java.util.List;

/**
 * TSEConfigListPanel – Config file list screen.
 */
public class TSEConfigListPanel extends KioskBgPanel {

    private static final Color TITLE_COLOR  = Color.decode("#1A1A2E");
    private static final Color LINK_BLUE    = Color.decode("#2563EB");
    private static final Color RED_WARN     = Color.decode("#C62828");
    private static final Color TABLE_HEADER = Color.decode("#F3F4FF");
    private static final Color ROW_ALT      = Color.decode("#F9FAFF");
    private static final Color CARD_BG      = new Color(255, 255, 255, 240);

    private final int requestedExamId;

    public TSEConfigListPanel(TSEExamService service, TSEExamContext context, int requestedExamId, Consumer<TSEExamConfig> onSelect, Runnable onExit) {
        super(new BorderLayout(0, 0));
        this.requestedExamId = requestedExamId;

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildCenterCard(service, context, onSelect), BorderLayout.CENTER);
        
        ExamFooterStatusBar footer = new ExamFooterStatusBar("VIE",
            btn -> TSEInputModeManager.getInstance().toggleMode(),
            (source, comp) -> {
                System.out.println("[TSE_PARENT_FOOTER] Quick Settings requested from CONFIG source=" + source);
                TSEParentHtmlQuickSettingsPopup.showPopup(comp, "CONFIG");
            },
            onExit
        );
        System.out.println("[TSE_PARENT_FOOTER] Footer attached to CONFIG card.");
        TSEInputModeManager.getInstance().addModeChangeListener(() -> {
            footer.setLanguageLabel(TSEInputModeManager.getInstance().getFooterLabel());
        });
        add(footer, BorderLayout.SOUTH);

        // Attach Swing Adapter for Vietnamese input (Opt-in)
        SwingUtilities.invokeLater(() -> TSEInputSwingAdapter.installForOptIn(this));
    }

    // ── top bar ────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new MigLayout("insets 8 14, fillx, aligny center", "[left]push[right]"));
        bar.setOpaque(false);
        bar.add(TSELoginPanel.iconBtn("images/exam/icons/refresh-cw.svg", 22), "cell 0 0");

        JPanel right = new JPanel(new MigLayout("insets 0, gap 0"));
        right.setOpaque(false);
        JButton btnHelp = TSELoginPanel.iconBtn("images/exam/icons/circle-help.svg", 18);
        btnHelp.setToolTipText("Thông tin ứng dụng");
        btnHelp.addActionListener(e -> TSEAboutDialog.showDialog(this, "Danh sách cấu hình thi"));
        right.add(btnHelp);
        bar.add(right, "cell 1 0");
        return bar;
    }

    // ── center card ────────────────────────────────────────────
    private JPanel buildCenterCard(TSEExamService service, TSEExamContext context, Consumer<TSEExamConfig> onSelect) {
        JPanel wrapper = new JPanel(new MigLayout("fill, align center center"));
        wrapper.setOpaque(false);

        JPanel card = new JPanel(new MigLayout(
            "wrap 1, insets 36 44 36 44, gapy 10",
            "[grow]"
        )) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };

        // Header row: logo icon + title
        JPanel headerRow = new JPanel(new MigLayout("insets 0, gap 16, aligny center", "[56!][grow]"));
        headerRow.setOpaque(false);

        LogoIconLabel logo = new LogoIconLabel(56);
        JPanel textCol = new JPanel(new MigLayout("wrap 1, insets 0, gapy 2", "[grow]"));
        textCol.setOpaque(false);

        JLabel lblTitle = new JLabel("TSE Configuration Files");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(TITLE_COLOR);

        JLabel lblSub = new JLabel("Hãy chọn tập tin cấu hình phù hợp");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(Color.decode("#555555"));

        textCol.add(lblTitle, "growx");
        textCol.add(lblSub,   "growx");
        headerRow.add(logo,    "cell 0 0, w 56!, h 56!");
        headerRow.add(textCol, "cell 1 0, grow");
        card.add(headerRow, "growx");

        // Red warning
        JLabel lblWarn = new JLabel(
            "<html><i>Khi thấy trong danh sách này có phiên bản TSE mới hơn so với phiên bản trên máy của bạn, "
            + "hãy khẩn trương cập nhật TSE lên phiên bản tương ứng, "
            + "vì phiên bản cũ sẽ sớm bị loại khỏi danh sách được hỗ trợ!</i></html>");
        lblWarn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblWarn.setForeground(RED_WARN);
        card.add(lblWarn, "growx");

        // Table
        card.add(buildTable(service, context, onSelect), "growx, gaptop 8");

        wrapper.add(card, "w 86%!");
        return wrapper;
    }

    // ── table ──────────────────────────────────────────────────
    private JPanel buildTable(TSEExamService service, TSEExamContext context, Consumer<TSEExamConfig> onSelect) {
        String[] cols = {"#", "Kỳ thi", "Thời gian", "Phiên bản TSE", "Tập tin cấu hình"};

        DefaultTableModel model = new DefaultTableModel(null, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : ROW_ALT);
                }
                return c;
            }
        };
        table.setRowHeight(44);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(Color.decode("#E5E7EB"));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setSelectionBackground(Color.decode("#DBEAFE"));
        table.setSelectionForeground(TITLE_COLOR);
        table.setIntercellSpacing(new Dimension(12, 0));
        table.setFocusable(false);

        // Header
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(TABLE_HEADER);
        header.setForeground(TITLE_COLOR);
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.decode("#D1D5DB")));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);

        // Column widths
        int[] widths = {36, 160, 110, 110, 360};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Link-blue renderer for last column
        table.getColumnModel().getColumn(4).setCellRenderer((t, val, sel, foc, r, c) -> {
            JLabel lbl = new JLabel(val != null ? val.toString() : "");
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lbl.setForeground(sel ? TITLE_COLOR : LINK_BLUE);
            lbl.setBackground(sel ? table.getSelectionBackground()
                                   : (r % 2 == 0 ? Color.WHITE : ROW_ALT));
            lbl.setOpaque(true);
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            return lbl;
        });

        // Center align # column
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        java.util.List<TSEExamConfig> loadedConfigs = new java.util.ArrayList<>();

        // Double-click to select
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row >= 0 && row < loadedConfigs.size() && onSelect != null) {
                        onSelect.accept(loadedConfigs.get(row));
                    }
                }
            }
        });

        // Load data dynamically
        service.getConfigList(context != null ? context.userId : 0).thenAccept(list -> {
            SwingUtilities.invokeLater(() -> {
                loadedConfigs.clear();
                model.setRowCount(0);
                
                java.util.List<TSEExamConfig> filteredList = new java.util.ArrayList<>();
                if (requestedExamId > 0) {
                    for (TSEExamConfig cfg : list) {
                        if (cfg.examId == requestedExamId) {
                            filteredList.add(cfg);
                        }
                    }
                    if (filteredList.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Kỳ thi được chọn (" + requestedExamId + ") không khả dụng hoặc chưa mở. Vui lòng kiểm tra lại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else {
                    filteredList.addAll(list);
                }
                
                loadedConfigs.addAll(filteredList);
                for (int i = 0; i < filteredList.size(); i++) {
                    TSEExamConfig cfg = filteredList.get(i);
                    model.addRow(new Object[]{
                        String.valueOf(i + 1),
                        cfg.examTitle,
                        cfg.durationMinutes + " phút",
                        cfg.configVersion,
                        cfg.configName
                    });
                }
                
                // Preselect if requestedExamId was provided
                if (requestedExamId > 0 && loadedConfigs.size() == 1) {
                    table.setRowSelectionInterval(0, 0);
                    System.out.println("[TSE_UI] Preselected requested examId: " + requestedExamId);
                }
            });
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Color.decode("#E5E7EB"), 1));
        scroll.getViewport().setBackground(Color.WHITE);
        
        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.setOpaque(false);
        container.add(scroll, BorderLayout.CENTER);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        JButton btnStart = new JButton("Bắt đầu");
        btnStart.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnStart.setBackground(LINK_BLUE);
        btnStart.setForeground(Color.WHITE);
        btnStart.setFocusPainted(false);
        btnStart.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));
        btnStart.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnStart.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0 && row < loadedConfigs.size() && onSelect != null) {
                onSelect.accept(loadedConfigs.get(row));
            } else {
                JOptionPane.showMessageDialog(container, "Vui lòng chọn một kỳ thi.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
        });
        
        btnPanel.add(btnStart);
        container.add(btnPanel, BorderLayout.SOUTH);
        
        return container;
    }

    private void showQuickSettingsSwingPopup(JComponent anchor) {
        // Obsolete, now using JCEF Popup
    }
}
