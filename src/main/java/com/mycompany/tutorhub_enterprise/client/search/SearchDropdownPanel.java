package com.mycompany.tutorhub_enterprise.client.search;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SearchDropdownPanel {
    private static final int ROW_HEIGHT = 46;
    private static final int MAX_VISIBLE_HEIGHT = 420;
    private static final int ARC = 16;
    private static final Color BG = Color.WHITE;
    private static final Color BORDER = new Color(0xE2E5EA);
    private static final Color TEXT = new Color(0x1A1D23);
    private static final Color MUTED = new Color(0x6B7280);
    private static final Color SELECTED_BG = new Color(0xF0F3FF);
    private static final Color ACCENT = new Color(0x6D5DF6);
    private static final Color DIVIDER = new Color(0xF0F1F3);
    private static final Color SHADOW = new Color(0, 0, 0, 20);
    private static final Color HIST_ICON_COLOR = new Color(0x9CA3AF);
    private static final Color WEB_ICON_COLOR = new Color(0x3B82F6);

    private final JPopupMenu popup;
    private final JPanel content;
    private final List<SearchResult> visibleResults = new ArrayList<>();
    private final List<ResultRow> rowPanels = new ArrayList<>();
    private int selectedIndex = -1;
    private int popupWidth = 440; // default, will be set dynamically

    public SearchDropdownPanel() {
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createEmptyBorder());
        popup.setOpaque(false);
        popup.setLayout(new BorderLayout());

        content = new RoundedContentPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        popup.add(content, BorderLayout.CENTER);
    }

    /** Update the popup to match invoker width */
    public void setPopupWidth(int width) {
        this.popupWidth = Math.max(300, width);
    }

    public void updateAndShow(Component invoker, int x, int y, List<SearchResult> results, SearchQuery query) {
        setResults(results, query);
        if (!popup.isVisible()) {
            popup.show(invoker, x, y);
        }
    }

    /** @deprecated Use {@link #updateAndShow} instead */
    public void show(Component invoker, int x, int y, List<SearchResult> results, SearchQuery query) {
        updateAndShow(invoker, x, y, results, query);
    }

    public void showDemo(Component invoker, int x, int y, SearchQuery query) {
        updateAndShow(invoker, x, y, Collections.emptyList(), query);
    }

    public void setResults(List<SearchResult> results, SearchQuery query) {
        content.removeAll();
        visibleResults.clear();
        rowPanels.clear();
        selectedIndex = -1;

        if (results == null || results.isEmpty()) {
            content.add(createEmptyState(query));
            refreshSize(120);
            return;
        }

        Map<SearchResultType, List<SearchResult>> grouped = groupResults(results);
        boolean isFirstGroup = true;
        for (SearchResultType type : SearchResultType.values()) {
            List<SearchResult> group = grouped.get(type);
            if (group == null || group.isEmpty()) {
                continue;
            }
            if (!isFirstGroup) {
                content.add(createDivider());
            }
            isFirstGroup = false;
            content.add(createGroupTitle(labelFor(type)));
            for (SearchResult result : group) {
                addResultRow(result);
            }
        }

        if (!visibleResults.isEmpty()) {
            selectedIndex = 0;
            updateSelection();
        }

        int groupCount = grouped.size();
        int height = 16 + rowPanels.size() * ROW_HEIGHT + groupCount * 26;
        if (groupCount > 1) {
            height += (groupCount - 1) * 9; // divider space
        }
        refreshSize(Math.min(MAX_VISIBLE_HEIGHT, Math.max(100, height)));
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
        if (selected.getAction() != null) {
            selected.getAction().execute();
        }
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

        ResultRow row = new ResultRow(result, popupWidth);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
        row.setPreferredSize(new Dimension(popupWidth - 16, ROW_HEIGHT));
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
        content.setPreferredSize(new Dimension(popupWidth, height));
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

    private static JComponent createDivider() {
        JPanel divider = new JPanel();
        divider.setOpaque(false);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setPreferredSize(new Dimension(100, 1));
        divider.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER));
        return divider;
    }

    private static JComponent createGroupTitle(String text) {
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        label.setForeground(MUTED);
        label.setBorder(BorderFactory.createEmptyBorder(8, 10, 4, 10));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JComponent createEmptyState(SearchQuery query) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        String raw = query == null || query.isBlank()
                ? "Nhập từ khóa để tìm kiếm"
                : "Không tìm thấy kết quả phù hợp";
        JLabel title = new JLabel(raw, SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        title.setForeground(MUTED);
        panel.add(title, BorderLayout.CENTER);
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
                return "Tìm trên web";
            case EMPTY:
                return "Gần đây";
            default:
                return "Khác";
        }
    }

    private static final class ResultRow extends JPanel {
        private final SearchResult result;
        private boolean selected;

        ResultRow(SearchResult result, int parentWidth) {
            this.result = result;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            setLayout(new BorderLayout(10, 0));

            // Icon badge
            Color iconColor = getIconColor(result.getType());
            JLabel icon = new JLabel(result.getIconText(), SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    // Draw background pill
                    Color bg = new Color(iconColor.getRed(), iconColor.getGreen(), iconColor.getBlue(), 18);
                    g2.setColor(bg);
                    g2.fillRoundRect(2, 4, getWidth() - 4, getHeight() - 8, 10, 10);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            icon.setFont(new Font("Segoe UI", Font.BOLD, 10));
            icon.setForeground(iconColor);
            icon.setPreferredSize(new Dimension(40, 34));
            add(icon, BorderLayout.WEST);

            // Text
            JPanel textPanel = new JPanel();
            textPanel.setOpaque(false);
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            JLabel title = new JLabel(result.getTitle());
            title.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            title.setForeground(TEXT);
            textPanel.add(title);
            if (result.getSubtitle() != null && !result.getSubtitle().isEmpty()) {
                textPanel.add(Box.createVerticalStrut(1));
                JLabel subtitle = new JLabel(result.getSubtitle());
                subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                subtitle.setForeground(MUTED);
                textPanel.add(subtitle);
            }
            add(textPanel, BorderLayout.CENTER);

            // Arrow hint on the right
            JLabel arrow = new JLabel("↵", SwingConstants.CENTER);
            arrow.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            arrow.setForeground(new Color(0xC0C4CC));
            arrow.setPreferredSize(new Dimension(20, 34));
            add(arrow, BorderLayout.EAST);
        }

        private static Color getIconColor(SearchResultType type) {
            switch (type) {
                case COMMAND: return ACCENT;
                case WEB: return WEB_ICON_COLOR;
                case EMPTY: return HIST_ICON_COLOR;
                default: return ACCENT;
            }
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
                g2.fillRoundRect(2, 1, getWidth() - 4, getHeight() - 2, 10, 10);
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
            // Shadow
            g2.setColor(SHADOW);
            g2.fillRoundRect(2, 3, getWidth() - 4, getHeight() - 4, ARC, ARC);
            // Background
            g2.setColor(BG);
            g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, ARC, ARC);
            // Border
            g2.setColor(BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, getWidth() - 5, getHeight() - 5, ARC, ARC);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
