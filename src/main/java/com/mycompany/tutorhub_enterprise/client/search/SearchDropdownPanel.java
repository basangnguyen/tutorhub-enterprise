package com.mycompany.tutorhub_enterprise.client.search;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SearchDropdownPanel {
    private static final int POPUP_WIDTH = 520;
    private static final int ROW_HEIGHT = 58;
    private static final Color BG = Color.WHITE;
    private static final Color BORDER = new Color(0xE5E7EB);
    private static final Color TEXT = new Color(0x111827);
    private static final Color MUTED = new Color(0x64748B);
    private static final Color SELECTED_BG = new Color(0xF1F5FF);
    private static final Color ACCENT = new Color(0x6D5DF6);

    private final JPopupMenu popup;
    private final JPanel content;
    private final List<SearchResult> visibleResults = new ArrayList<>();
    private final List<ResultRow> rowPanels = new ArrayList<>();
    private int selectedIndex = -1;

    public SearchDropdownPanel() {
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createEmptyBorder());
        popup.setOpaque(false);
        popup.setLayout(new BorderLayout());

        content = new RoundedContentPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        popup.add(content, BorderLayout.CENTER);
    }

    public void show(Component invoker, int x, int y, List<SearchResult> results, SearchQuery query) {
        setResults(results, query);
        popup.show(invoker, x, y);
    }

    public void showDemo(Component invoker, int x, int y, SearchQuery query) {
        show(invoker, x, y, createDemoResults(query), query);
    }

    public void setResults(List<SearchResult> results, SearchQuery query) {
        content.removeAll();
        visibleResults.clear();
        rowPanels.clear();
        selectedIndex = -1;

        if (results == null || results.isEmpty()) {
            content.add(createEmptyState(query));
            refreshSize(150);
            return;
        }

        Map<SearchResultType, List<SearchResult>> grouped = groupResults(results);
        for (SearchResultType type : SearchResultType.values()) {
            List<SearchResult> group = grouped.get(type);
            if (group == null || group.isEmpty()) {
                continue;
            }
            content.add(createGroupTitle(labelFor(type)));
            for (SearchResult result : group) {
                addResultRow(result);
            }
            content.add(Box.createVerticalStrut(4));
        }

        if (!visibleResults.isEmpty()) {
            selectedIndex = 0;
            updateSelection();
        }

        int height = Math.min(420, 40 + rowPanels.size() * ROW_HEIGHT + grouped.size() * 28);
        refreshSize(Math.max(150, height));
    }

    public void moveUp() {
        if (visibleResults.isEmpty()) {
            return;
        }
        selectedIndex = selectedIndex <= 0 ? visibleResults.size() - 1 : selectedIndex - 1;
        updateSelection();
    }

    public void moveDown() {
        if (visibleResults.isEmpty()) {
            return;
        }
        selectedIndex = (selectedIndex + 1) % visibleResults.size();
        updateSelection();
    }

    public void activateSelected() {
        if (selectedIndex < 0 || selectedIndex >= visibleResults.size()) {
            return;
        }
        SearchResult selected = visibleResults.get(selectedIndex);
        hide();
        selected.getAction().execute();
    }

    public void hide() {
        popup.setVisible(false);
    }

    public boolean isVisible() {
        return popup.isVisible();
    }

    private void addResultRow(SearchResult result) {
        int rowIndex = visibleResults.size();
        visibleResults.add(result);

        ResultRow row = new ResultRow(result);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
        row.setPreferredSize(new Dimension(POPUP_WIDTH - 20, ROW_HEIGHT));
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                selectedIndex = rowIndex;
                updateSelection();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                selectedIndex = rowIndex;
                activateSelected();
            }
        });

        rowPanels.add(row);
        content.add(row);
    }

    private void updateSelection() {
        for (int i = 0; i < rowPanels.size(); i++) {
            rowPanels.get(i).setSelected(i == selectedIndex);
        }
        content.repaint();
    }

    private void refreshSize(int height) {
        content.setPreferredSize(new Dimension(POPUP_WIDTH, height));
        content.revalidate();
        content.repaint();
        popup.pack();
    }

    private static Map<SearchResultType, List<SearchResult>> groupResults(List<SearchResult> results) {
        Map<SearchResultType, List<SearchResult>> grouped = new EnumMap<>(SearchResultType.class);
        for (SearchResult result : results) {
            grouped.computeIfAbsent(result.getType(), key -> new ArrayList<>()).add(result);
        }
        return grouped;
    }

    private static List<SearchResult> createDemoResults(SearchQuery query) {
        String rawQuery = query == null ? "" : query.getRawText();
        return Arrays.asList(
                command("Mở Tin nhắn", "Xem hội thoại và tìm bạn bè", "MSG"),
                command("Mở Lịch", "Xem lịch học và lịch dạy", "CAL"),
                command("Mở Lớp học", "Quản lý lớp học của bạn", "CLS"),
                command("Mở Hồ sơ", "Xem thông tin tài khoản", "USR"),
                SearchResult.builder()
                        .title(rawQuery.isBlank() ? "Tìm trên web" : "Tìm trên web: " + rawQuery)
                        .subtitle("Dòng demo cho phase sau, chưa mở trình duyệt")
                        .type(SearchResultType.WEB)
                        .score(0.1)
                        .iconText("WEB")
                        .action(SearchAction.noop())
                        .build()
        );
    }

    private static SearchResult command(String title, String subtitle, String iconText) {
        return SearchResult.builder()
                .title(title)
                .subtitle(subtitle)
                .type(SearchResultType.COMMAND)
                .score(1.0)
                .iconText(iconText)
                .action(SearchAction.noop())
                .build();
    }

    private static JComponent createGroupTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(MUTED);
        label.setBorder(BorderFactory.createEmptyBorder(8, 12, 5, 12));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JComponent createEmptyState(SearchQuery query) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(26, 24, 26, 24));
        String raw = query == null || query.isBlank() ? "Nhập từ khóa để tìm kiếm" : "Không tìm thấy kết quả cho \"" + query.getRawText() + "\"";
        JLabel title = new JLabel(raw, SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(TEXT);
        JLabel subtitle = new JLabel("TutorHub sẽ hỗ trợ tìm lớp học, tin nhắn, tài liệu và lệnh nhanh ở các phase sau.", SwingConstants.CENTER);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(MUTED);
        panel.add(title, BorderLayout.CENTER);
        panel.add(subtitle, BorderLayout.SOUTH);
        return panel;
    }

    private static String labelFor(SearchResultType type) {
        switch (type) {
            case COMMAND:
                return "Lệnh nhanh";
            case CHAT:
                return "Tin nhắn";
            case CLASS:
                return "Lớp học";
            case DOCUMENT:
                return "Tài liệu";
            case CALENDAR:
                return "Lịch";
            case TASK:
                return "Nhiệm vụ";
            case BLACKBOARD:
                return "Bảng vẽ";
            case PROFILE:
                return "Hồ sơ";
            case WEB:
                return "Web";
            default:
                return "Khác";
        }
    }

    private static final class ResultRow extends JPanel {
        private final SearchResult result;
        private boolean selected;

        ResultRow(SearchResult result) {
            this.result = result;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            setLayout(new BorderLayout(12, 0));

            JLabel icon = new JLabel(result.getIconText(), SwingConstants.CENTER);
            icon.setFont(new Font("Segoe UI", Font.BOLD, 11));
            icon.setForeground(ACCENT);
            icon.setPreferredSize(new Dimension(42, 42));
            add(icon, BorderLayout.WEST);

            JPanel textPanel = new JPanel();
            textPanel.setOpaque(false);
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            JLabel title = new JLabel(result.getTitle());
            title.setFont(new Font("Segoe UI", Font.BOLD, 14));
            title.setForeground(TEXT);
            JLabel subtitle = new JLabel(result.getSubtitle());
            subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            subtitle.setForeground(MUTED);
            textPanel.add(title);
            textPanel.add(Box.createVerticalStrut(3));
            textPanel.add(subtitle);
            add(textPanel, BorderLayout.CENTER);
        }

        void setSelected(boolean selected) {
            this.selected = selected;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (selected) {
                g2.setColor(SELECTED_BG);
                g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 14, 14);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class RoundedContentPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 0, 0, 28));
            g2.fillRoundRect(4, 5, getWidth() - 8, getHeight() - 8, 22, 22);
            g2.setColor(BG);
            g2.fillRoundRect(0, 0, getWidth() - 6, getHeight() - 8, 20, 20);
            g2.setColor(BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, getWidth() - 7, getHeight() - 9, 20, 20);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
