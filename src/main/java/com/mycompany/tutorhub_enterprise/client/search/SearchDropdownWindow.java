package com.mycompany.tutorhub_enterprise.client.search;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Search dropdown implemented as a JWindow (NOT JPopupMenu) to avoid flickering.
 * JWindow does not steal focus from the search field and can update its content
 * without closing/reopening, which eliminates the flicker completely.
 */
public class SearchDropdownWindow {

    // === Design Tokens ===
    private static final Color BG             = new Color(0xFFFFFF);
    private static final Color BORDER         = new Color(0xDDE1E7);
    private static final Color TEXT_PRIMARY   = new Color(0x1C1E21);
    private static final Color TEXT_MUTED     = new Color(0x6B7280);
    private static final Color SELECTED_BG    = new Color(0xF0F3FF);
    private static final Color SELECTED_ICON  = new Color(0x6D5DF6);
    private static final Color DIVIDER        = new Color(0xF1F3F5);
    private static final Color WEB_COLOR      = new Color(0x2563EB);
    private static final Color HIST_COLOR     = new Color(0x9CA3AF);
    private static final Color SHADOW_COLOR   = new Color(0, 0, 0, 18);

    private static final int ARC         = 14;
    private static final int ROW_H       = 44;
    private static final int SECTION_H   = 28;
    private static final int PADDING     = 6;
    private static final int MAX_HEIGHT  = 400;

    private static final int MAX_PER_GROUP = 3;

    // === State ===
    private final JWindow window;
    private final JPanel contentPanel;
    private final List<SearchResult> resultList  = new ArrayList<>();
    private final List<ResultRow>    rowPanels   = new ArrayList<>();
    private int selectedIndex = -1;
    private int dropdownWidth = 440;
    private String currentQueryRaw = "";

    // Callback when a result is chosen
    private Runnable onResultActivated;

    public SearchDropdownWindow(Window ownerWindow) {
        window = new JWindow(ownerWindow);
        window.setType(Window.Type.POPUP);
        // CRITICAL: do NOT make window focusable – search field keeps focus
        window.setFocusableWindowState(false);
        window.setFocusable(false);

        contentPanel = new DropdownContentPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        window.setContentPane(contentPanel);
        window.setBackground(new Color(0, 0, 0, 0)); // transparent frame for shadow
    }

    // =====================================================================
    // Public API
    // =====================================================================

    /** Returns the underlying JWindow for ancestor comparisons. */
    public Window getWindow() {
        return window;
    }

    public void setDropdownWidth(int width) {
        this.dropdownWidth = Math.max(300, width);
    }

    /** Show or update the dropdown at screen coordinates (screenX, screenY). */
    public void updateAndShow(int screenX, int screenY, List<SearchResult> results, SearchQuery query) {
        SwingUtilities.invokeLater(() -> {
            buildContent(results, query);
            positionWindow(screenX, screenY);
            if (!window.isVisible()) {
                window.setVisible(true);
            }
            contentPanel.repaint();
        });
    }

    /** Hide the dropdown without flickering. */
    public void hide() {
        SwingUtilities.invokeLater(() -> window.setVisible(false));
    }

    public boolean isShowing() {
        return window.isVisible();
    }

    public void moveDown() {
        if (resultList.isEmpty()) return;
        selectedIndex = (selectedIndex + 1) % resultList.size();
        updateSelection();
    }

    public void moveUp() {
        if (resultList.isEmpty()) return;
        selectedIndex = selectedIndex <= 0 ? resultList.size() - 1 : selectedIndex - 1;
        updateSelection();
    }

    public void activateSelected() {
        if (selectedIndex < 0 || selectedIndex >= resultList.size()) return;
        SearchResult r = resultList.get(selectedIndex);
        hide();
        if (r.getAction() != null) r.getAction().execute();
    }

    public void setOnResultActivated(Runnable cb) {
        this.onResultActivated = cb;
    }

    /** Dispose resources when the parent is closing. */
    public void dispose() {
        window.dispose();
    }

    public boolean isVisible() {
        return window.isVisible();
    }

    // =====================================================================
    // Internal – build content
    // =====================================================================

    public void showLoadingState(int x, int y, SearchQuery query) {
        contentPanel.removeAll();
        resultList.clear();
        rowPanels.clear();
        selectedIndex = -1;
        currentQueryRaw = (query != null) ? query.getRawText() : "";

        JPanel skeletonPanel = new JPanel();
        skeletonPanel.setLayout(new BoxLayout(skeletonPanel, BoxLayout.Y_AXIS));
        skeletonPanel.setOpaque(false);
        skeletonPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel loadingLbl = new JLabel("Đang tìm kiếm...");
        loadingLbl.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        loadingLbl.setForeground(new Color(150, 150, 150));
        
        skeletonPanel.add(loadingLbl);
        contentPanel.add(skeletonPanel);

        applySize(70);

        window.setLocation(x, y);
        if (!window.isVisible()) {
            window.setVisible(true);
        }
    }

    private void buildContent(List<SearchResult> results, SearchQuery query) {
        contentPanel.removeAll();
        resultList.clear();
        rowPanels.clear();
        selectedIndex = -1;
        currentQueryRaw = (query != null) ? query.getRawText() : "";

        if (results == null || results.isEmpty()) {
            contentPanel.add(buildEmptyState(query));
            applySize(90);
            return;
        }

        // --- Extract Top Match: highest score result (if query is not blank) ---
        SearchResult topMatch = null;
        List<SearchResult> remaining = new ArrayList<>(results);
        if (query != null && !query.isBlank() && remaining.size() > 1) {
            topMatch = remaining.get(0); // already sorted by score desc
            if (topMatch.getScore() > 0 && topMatch.getType() != SearchResultType.WEB) {
                remaining.remove(0);
            } else {
                topMatch = null;
            }
        }

        int sectionCount = 0;

        // --- Render Top Match ---
        if (topMatch != null) {
            contentPanel.add(buildSectionHeader("Kết quả hàng đầu"));
            addRow(topMatch);
            sectionCount++;
        }

        // --- Group remaining by type ---
        Map<SearchResultType, List<SearchResult>> grouped = new EnumMap<>(SearchResultType.class);
        for (SearchResult r : remaining) {
            grouped.computeIfAbsent(r.getType(), k -> new ArrayList<>()).add(r);
        }

        for (SearchResultType type : SearchResultType.values()) {
            List<SearchResult> group = grouped.get(type);
            if (group == null || group.isEmpty()) continue;

            if (sectionCount > 0) {
                contentPanel.add(buildDivider());
            }
            sectionCount++;

            contentPanel.add(buildSectionHeader(labelFor(type)));
            int limit = Math.min(group.size(), MAX_PER_GROUP);
            for (int i = 0; i < limit; i++) {
                addRow(group.get(i));
            }
        }

        // Auto-select first result
        if (!resultList.isEmpty()) {
            selectedIndex = 0;
            updateSelection();
        }

        // --- G7: Keyboard Hints ---
        JPanel hintPanel = new JPanel(new java.awt.BorderLayout());
        hintPanel.setOpaque(false);
        hintPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
        JLabel hintLabel = new JLabel("↵ để mở  •  Esc để đóng", SwingConstants.CENTER);
        hintLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hintLabel.setForeground(new Color(156, 163, 175));
        hintPanel.add(hintLabel, java.awt.BorderLayout.CENTER);
        contentPanel.add(hintPanel);

        int totalH = PADDING * 2
                + sectionCount * SECTION_H
                + Math.max(0, sectionCount - 1) * 5
                + resultList.size() * ROW_H
                + (topMatch != null ? 4 : 0)
                + 28; // space for hint
        applySize(Math.min(MAX_HEIGHT, Math.max(80, totalH)));
    }

    private void addRow(SearchResult result) {
        int idx = resultList.size();
        resultList.add(result);

        ResultRow row = new ResultRow(result, dropdownWidth - PADDING * 2, currentQueryRaw);
        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                selectedIndex = idx;
                updateSelection();
            }
            @Override public void mousePressed(MouseEvent e) {
                selectedIndex = idx;
                activateSelected();
            }
        });
        rowPanels.add(row);
        contentPanel.add(row);
    }

    private void updateSelection() {
        for (int i = 0; i < rowPanels.size(); i++) {
            rowPanels.get(i).setHighlighted(i == selectedIndex);
        }
    }

    private void applySize(int height) {
        Dimension d = new Dimension(dropdownWidth, height);
        contentPanel.setPreferredSize(d);
        window.setSize(d);
    }

    private void positionWindow(int screenX, int screenY) {
        // Make sure the dropdown doesn't go off screen
        GraphicsConfiguration gc = window.getGraphicsConfiguration();
        Rectangle screen = (gc != null)
                ? gc.getBounds()
                : new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

        int x = screenX;
        int y = screenY;
        int w = window.getWidth();
        int h = window.getHeight();

        if (x + w > screen.x + screen.width)  x = screen.x + screen.width  - w;
        if (y + h > screen.y + screen.height) y = screen.y + screen.height - h;
        if (x < screen.x) x = screen.x;
        if (y < screen.y) y = screen.y;

        window.setLocation(x, y);
    }

    // =====================================================================
    // Small builder helpers
    // =====================================================================

    private static JComponent buildEmptyState(SearchQuery query) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        String msg = (query == null || query.isBlank())
                ? "Nhập từ khóa để tìm kiếm..."
                : "Không tìm thấy kết quả phù hợp";
        JLabel lbl = new JLabel(msg, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(TEXT_MUTED);
        p.add(lbl, BorderLayout.CENTER);
        return p;
    }

    private static JComponent buildDivider() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setBorder(BorderFactory.createMatteBorder(0, 4, 0, 4, DIVIDER));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        p.setPreferredSize(new Dimension(100, 5));
        return p;
    }

    private static JComponent buildSectionHeader(String text) {
        JLabel lbl = new JLabel(text.toUpperCase());
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(TEXT_MUTED);
        lbl.setBorder(BorderFactory.createEmptyBorder(6, 10, 3, 10));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, SECTION_H));
        return lbl;
    }

    private static String labelFor(SearchResultType type) {
        switch (type) {
            case COMMAND:   return "Lệnh nhanh";
            case CHAT:      return "Tin nhắn";
            case CLASS:     return "Lớp học";
            case DOCUMENT:  return "Tài liệu";
            case CALENDAR:  return "Lịch";
            case TASK:      return "Nhiệm vụ";
            case BLACKBOARD:return "Bảng vẽ";
            case PROFILE:   return "Hồ sơ";
            case WEB:       return "Tìm trên web";
            case HISTORY:   return "Tìm kiếm gần đây";
            case EMPTY:     return "Khác";
            default:        return "Khác";
        }
    }

    // =====================================================================
    // Inner: ResultRow
    // =====================================================================

    private static final class ResultRow extends JPanel {
        private boolean highlighted = false;

        ResultRow(SearchResult result, int rowWidth, String queryRaw) {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setLayout(new BorderLayout(8, 0));
            setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
            setPreferredSize(new Dimension(rowWidth, ROW_H));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            // --- Icon badge ---
            Color iconColor = iconColorFor(result.getType());
            String iconPath = svgPathFor(result.getType());
            com.formdev.flatlaf.extras.FlatSVGIcon svgIcon = new com.formdev.flatlaf.extras.FlatSVGIcon(iconPath, 16, 16);
            svgIcon.setColorFilter(new com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter(color -> iconColor));

            JLabel icon = new JLabel(svgIcon, SwingConstants.CENTER) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color bg = new Color(iconColor.getRed(), iconColor.getGreen(), iconColor.getBlue(), 20);
                    g2.setColor(bg);
                    g2.fillRoundRect(2, 3, getWidth()-4, getHeight()-6, 8, 8);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            icon.setPreferredSize(new Dimension(38, ROW_H - 8));
            add(icon, BorderLayout.WEST);

            // --- Text with highlight ---
            JPanel textArea = new JPanel();
            textArea.setOpaque(false);
            textArea.setLayout(new BoxLayout(textArea, BoxLayout.Y_AXIS));
            textArea.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

            String titleHtml = SearchTextHighlighter.highlightForLabel(result.getTitle(), queryRaw);
            JLabel titleLbl = new JLabel(titleHtml);
            titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            titleLbl.setForeground(TEXT_PRIMARY);
            textArea.add(titleLbl);

            if (result.getSubtitle() != null && !result.getSubtitle().isBlank()) {
                String subHtml = SearchTextHighlighter.highlightForLabel(result.getSubtitle(), queryRaw);
                JLabel subLbl = new JLabel(subHtml);
                subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                subLbl.setForeground(TEXT_MUTED);
                textArea.add(subLbl);
            }
            add(textArea, BorderLayout.CENTER);

            // --- Keyboard hint ---
            JLabel hint = new JLabel("↵", SwingConstants.CENTER);
            hint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            hint.setForeground(new Color(0xBCC0C8));
            hint.setPreferredSize(new Dimension(22, ROW_H - 8));
            add(hint, BorderLayout.EAST);
        }

        void setHighlighted(boolean h) {
            if (this.highlighted == h) return;
            this.highlighted = h;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (highlighted) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SELECTED_BG);
                g2.fillRoundRect(1, 1, getWidth()-2, getHeight()-2, 8, 8);
                g2.dispose();
            }
            super.paintComponent(g);
        }

        private static Color iconColorFor(SearchResultType type) {
            switch (type) {
                case WEB:     return WEB_COLOR;
                case HISTORY: return HIST_COLOR;
                case EMPTY:   return HIST_COLOR;
                default:      return SELECTED_ICON;
            }
        }

        private static String svgPathFor(SearchResultType type) {
            switch (type) {
                case COMMAND:   return "/images/icon/search_command.svg";
                case CHAT:      return "/images/icon/search_chat.svg";
                case CLASS:     return "/images/icon/search_class.svg";
                case DOCUMENT:  return "/images/icon/search_document.svg";
                case CALENDAR:  return "/images/icon/search_calendar.svg";
                case TASK:      return "/images/icon/search_task.svg";
                case BLACKBOARD:return "/images/icon/search_blackboard.svg";
                case PROFILE:   return "/images/icon/search_profile.svg";
                case WEB:       return "/images/icon/search_web.svg";
                case HISTORY:   return "/images/icon/search_history.svg";
                case EMPTY:     return "/images/icon/search_empty.svg";
                default:        return "/images/icon/search_empty.svg";
            }
        }
    }

    // =====================================================================
    // Inner: DropdownContentPanel (renders rounded rect + shadow)
    // =====================================================================

    private static final class DropdownContentPanel extends JPanel {
        DropdownContentPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int inset = 4; // shadow margin

            // Multi-layer shadow
            for (int i = 3; i >= 0; i--) {
                int alpha = (int)(SHADOW_COLOR.getAlpha() * (1.0 - i * 0.2));
                g2.setColor(new Color(0, 0, 0, Math.max(0, alpha)));
                g2.fillRoundRect(inset - i, inset - i + 1, w - inset*2 + i*2, h - inset*2 + i*2, ARC + i, ARC + i);
            }

            // White background
            g2.setColor(BG);
            g2.fillRoundRect(inset, inset, w - inset*2, h - inset*2, ARC, ARC);

            // Border
            g2.setColor(BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(inset, inset, w - inset*2 - 1, h - inset*2 - 1, ARC, ARC);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
