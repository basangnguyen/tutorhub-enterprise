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
 *
 * Visual style: Google Search-inspired clean aesthetic.
 */
public class SearchDropdownWindow {

    // === Design Tokens (Google-inspired clean palette) ===
    private static final Color BG             = new Color(0xFFFFFF);
    private static final Color BORDER         = new Color(0xDADCE0);
    private static final Color TEXT_PRIMARY   = new Color(0x202124);
    private static final Color TEXT_MUTED     = new Color(0x5F6368);
    private static final Color TEXT_FAINT     = new Color(0x9AA0A6);
    private static final Color HOVER_BG       = new Color(0xF1F3F4);
    private static final Color SELECTED_ICON  = new Color(0x1A73E8);
    private static final Color DIVIDER        = new Color(0xE8EAED);
    private static final Color WEB_COLOR      = new Color(0x1A73E8);
    private static final Color HIST_COLOR     = new Color(0x5F6368);

    private static final int ARC         = 40;
    private static final int ROW_H       = 34;
    private static final int SECTION_H   = 24;
    private static final int PADDING     = 4;
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
    private Runnable onResultDeleted;

    public SearchDropdownWindow(Window ownerWindow) {
        window = new JWindow(ownerWindow);
        window.setType(Window.Type.POPUP);
        // CRITICAL: do NOT make window focusable – search field keeps focus
        window.setFocusableWindowState(false);
        window.setFocusable(false);

        contentPanel = new DropdownContentPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, 3, 20, 3));

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

    public void setOnResultDeleted(Runnable cb) {
        this.onResultDeleted = cb;
    }

    /** Dispose resources when the parent is closing. */
    public void dispose() {
        window.dispose();
    }

    public boolean isVisible() {
        return window.isVisible();
    }

    // =====================================================================
    // Internal – build content (same logic as original)
    // =====================================================================

    public void showLoadingState(int x, int y, SearchQuery query) {
        contentPanel.removeAll();
        contentPanel.setPreferredSize(null);
        resultList.clear();
        rowPanels.clear();
        selectedIndex = -1;
        currentQueryRaw = (query != null) ? query.getRawText() : "";

        contentPanel.add(new LoadingRowsPanel());

        applyPreferredSize(100);

        window.setLocation(x, y);
        if (!window.isVisible()) {
            window.setVisible(true);
        }
    }

    private void buildContent(List<SearchResult> results, SearchQuery query) {
        contentPanel.removeAll();
        contentPanel.setPreferredSize(null);
        resultList.clear();
        rowPanels.clear();
        selectedIndex = -1;
        currentQueryRaw = (query != null) ? query.getRawText() : "";

        if (results == null || results.isEmpty()) {
            contentPanel.add(buildEmptyState(query));
            applyPreferredSize(120);
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
            contentPanel.add(buildSectionHeader("Ph\u00f9 h\u1ee3p nh\u1ea5t"));
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

        applyPreferredSize(80);
    }

    private void addRow(SearchResult result) {
        int idx = resultList.size();
        resultList.add(result);

        ResultRow row = new ResultRow(result, dropdownWidth - PADDING * 2, currentQueryRaw, onResultDeleted);
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

    private void applyPreferredSize(int minHeight) {
        contentPanel.revalidate();
        int naturalHeight = contentPanel.getPreferredSize().height;
        applySize(Math.min(MAX_HEIGHT, Math.max(minHeight, naturalHeight)));
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

    // Header removed — Google Search doesn't have one. Straight to results.

    private static JComponent buildEmptyState(SearchQuery query) {
        String text = (query == null || query.isBlank())
                ? "T\u00ecm l\u1edbp h\u1ecdc, tin nh\u1eafn, l\u1ecbch..."
                : "Kh\u00f4ng t\u00ecm th\u1ea5y k\u1ebft qu\u1ea3";
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Arial", Font.PLAIN, 12));
        lbl.setForeground(TEXT_FAINT);
        lbl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 4));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        return lbl;
    }

    private static JComponent buildDivider() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);
        outer.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        outer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        outer.setPreferredSize(new Dimension(100, 5));
        outer.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel line = new JPanel();
        line.setBackground(DIVIDER);
        line.setPreferredSize(new Dimension(1, 1));
        outer.add(line, BorderLayout.CENTER);
        return outer;
    }

    private static JComponent buildSectionHeader(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Arial", Font.PLAIN, 10));
        lbl.setForeground(TEXT_FAINT);
        lbl.setBorder(BorderFactory.createEmptyBorder(4, 10, 2, 4));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, SECTION_H));
        return lbl;
    }

    private static String labelFor(SearchResultType type) {
        switch (type) {
            case COMMAND:   return "L\u1ec7nh nhanh";
            case CHAT:      return "Tin nh\u1eafn";
            case CLASS:     return "L\u1edbp h\u1ecdc";
            case DOCUMENT:  return "T\u00e0i li\u1ec7u";
            case CALENDAR:  return "L\u1ecbch";
            case TASK:      return "Nhi\u1ec7m v\u1ee5";
            case BLACKBOARD:return "B\u1ea3ng v\u1ebd";
            case PROFILE:   return "H\u1ed3 s\u01a1";
            case WEB:       return "T\u00ecm tr\u00ean web";
            case HISTORY:   return "T\u00ecm ki\u1ebfm g\u1ea7n \u0111\u00e2y";
            case EMPTY:     return "Kh\u00e1c";
            default:        return "Kh\u00e1c";
        }
    }

    private static final class LoadingRowsPanel extends JPanel {
        LoadingRowsPanel() {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(2, 8, 6, 8));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
            setPreferredSize(new Dimension(100, 78));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color shimmer = new Color(0xE8EAED);
            int y = 4;
            for (int i = 0; i < 3; i++) {
                // Small icon placeholder (no circle, just square)
                g2.setColor(shimmer);
                g2.fillRoundRect(0, y + 4, 16, 16, 3, 3);
                // Text bar
                g2.setColor(new Color(0xF1F3F4));
                g2.fillRoundRect(24, y + 7, Math.max(100, getWidth() / 2), 10, 4, 4);
                y += 24;
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // =====================================================================
    // Inner: ResultRow (Google-style: clean icon + text, flat hover)
    // =====================================================================

    private static final class ResultRow extends JPanel {
        private boolean highlighted = false;

        ResultRow(SearchResult result, int rowWidth, String queryRaw, Runnable onDeleteCallback) {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setLayout(new BorderLayout(4, 0));
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 4));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
            setPreferredSize(new Dimension(rowWidth, ROW_H));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            // --- Icon (Google style: raw SVG, NO background) ---
            Color iconColor = iconColorFor(result.getType());
            String iconPath = svgPathFor(result.getType());
            com.formdev.flatlaf.extras.FlatSVGIcon svgIcon = new com.formdev.flatlaf.extras.FlatSVGIcon(iconPath, 16, 16);
            svgIcon.setColorFilter(new com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter(color -> iconColor));

            JLabel icon = new JLabel(svgIcon, SwingConstants.CENTER);
            icon.setPreferredSize(new Dimension(20, ROW_H));
            add(icon, BorderLayout.WEST);

            // --- Title ---
            String titleHtml = SearchTextHighlighter.highlightForLabel(result.getTitle(), queryRaw);
            JLabel titleLbl = new JLabel(titleHtml);
            titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            titleLbl.setForeground(TEXT_PRIMARY);
            add(titleLbl, BorderLayout.CENTER);

            // --- Delete Button for History ---
            if (result.getType() == SearchResultType.HISTORY) {
                JButton btnDelete = new JButton("×");
                btnDelete.setFont(new Font("Segoe UI", Font.BOLD, 15));
                btnDelete.setForeground(TEXT_FAINT);
                btnDelete.setContentAreaFilled(false);
                btnDelete.setBorderPainted(false);
                btnDelete.setFocusPainted(false);
                btnDelete.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btnDelete.setMargin(new Insets(0, 0, 0, 0));
                btnDelete.setPreferredSize(new Dimension(24, 24));
                btnDelete.setVisible(false); // Only show on hover
                
                btnDelete.addActionListener(e -> {
                    SearchHistoryStore.removeSearch(result.getTitle());
                    if (onDeleteCallback != null) onDeleteCallback.run();
                });
                
                btnDelete.addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { 
                        btnDelete.setForeground(new Color(0xDA2222)); 
                        btnDelete.setVisible(true);
                    }
                    @Override public void mouseExited(MouseEvent e)  { 
                        btnDelete.setForeground(TEXT_FAINT); 
                        btnDelete.setVisible(false);
                    }
                });
                
                this.addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { btnDelete.setVisible(true); }
                    @Override public void mouseExited(MouseEvent e)  { btnDelete.setVisible(false); }
                });
                
                add(btnDelete, BorderLayout.EAST);
            }
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
                g2.setColor(HOVER_BG);
                int h = getHeight();
                g2.fillRoundRect(6, 2, getWidth() - 12, h - 4, h - 4, h - 4);
                g2.dispose();
            }
            super.paintComponent(g);
        }

        private static Color iconColorFor(SearchResultType type) {
            switch (type) {
                case CHAT:       return new Color(0x1A73E8);
                case CLASS:      return new Color(0x188038);
                case DOCUMENT:   return new Color(0xC5221F);
                case CALENDAR:   return new Color(0x7C3AED);
                case TASK:       return new Color(0xE37400);
                case BLACKBOARD: return new Color(0x5F6368);
                case PROFILE:    return new Color(0x8B5CF6);
                case WEB:        return WEB_COLOR;
                case HISTORY:    return HIST_COLOR;
                case EMPTY:      return HIST_COLOR;
                case COMMAND:
                default:         return new Color(0x7C3AED);
            }
        }

        private static String svgPathFor(SearchResultType type) {
            switch (type) {
                case COMMAND:   return "images/icon/search_command.svg";
                case CHAT:      return "images/icon/search_chat.svg";
                case CLASS:     return "images/icon/search_class.svg";
                case DOCUMENT:  return "images/icon/search_document.svg";
                case CALENDAR:  return "images/icon/search_calendar.svg";
                case TASK:      return "images/icon/search_task.svg";
                case BLACKBOARD:return "images/icon/search_blackboard.svg";
                case PROFILE:   return "images/icon/search_profile.svg";
                case WEB:       return "images/icon/search_web.svg";
                case HISTORY:   return "images/icon/search_history.svg";
                case EMPTY:     return "images/icon/search_empty.svg";
                default:        return "images/icon/search_empty.svg";
            }
        }
    }

    // =====================================================================
    // Inner: DropdownContentPanel (Google-style rounded card with soft shadow)
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
            int inset = 3;

            int r = ARC / 2;

            for (int i = 3; i >= 1; i--) {
                g2.setColor(new Color(0, 0, 0, Math.max(0, 10 - i * 2)));
                java.awt.geom.Path2D.Float shadowPath = new java.awt.geom.Path2D.Float();
                int sr = r + i;
                shadowPath.moveTo(inset - i, 0);
                shadowPath.lineTo(inset - i, h - inset + i - sr);
                shadowPath.quadTo(inset - i, h - inset + i, inset - i + sr, h - inset + i);
                shadowPath.lineTo(w - inset + i - sr, h - inset + i);
                shadowPath.quadTo(w - inset + i, h - inset + i, w - inset + i, h - inset + i - sr);
                shadowPath.lineTo(w - inset + i, 0);
                shadowPath.closePath();
                g2.fill(shadowPath);
            }

            g2.setColor(BG);
            java.awt.geom.Path2D.Float bgPath = new java.awt.geom.Path2D.Float();
            bgPath.moveTo(inset, 0);
            bgPath.lineTo(inset, h - inset - r);
            bgPath.quadTo(inset, h - inset, inset + r, h - inset);
            bgPath.lineTo(w - inset - r, h - inset);
            bgPath.quadTo(w - inset, h - inset, w - inset, h - inset - r);
            bgPath.lineTo(w - inset, 0);
            bgPath.closePath();
            g2.fill(bgPath);

            Color themeA  = new Color(174, 204, 246);
            Color themeB  = new Color(204, 153, 255);
            GradientPaint focusGrad = new GradientPaint(
                    inset, -40, themeA, w - inset, 400, themeB);
            g2.setPaint(focusGrad);
            g2.setStroke(new BasicStroke(1.5f));
            java.awt.geom.Path2D.Float borderPath = new java.awt.geom.Path2D.Float();
            borderPath.moveTo(inset, 0);
            borderPath.lineTo(inset, h - inset - 1 - r);
            borderPath.quadTo(inset, h - inset - 1, inset + r, h - inset - 1);
            borderPath.lineTo(w - inset - 1 - r, h - inset - 1);
            borderPath.quadTo(w - inset - 1, h - inset - 1, w - inset - 1, h - inset - 1 - r);
            borderPath.lineTo(w - inset - 1, 0);
            g2.draw(borderPath);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
