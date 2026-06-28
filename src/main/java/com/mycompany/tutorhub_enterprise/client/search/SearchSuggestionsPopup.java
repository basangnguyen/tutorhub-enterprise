package com.mycompany.tutorhub_enterprise.client.search;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Popup gợi ý tìm kiếm (autocomplete) dành cho GlobalSearchBar.
 *
 * <h3>Tại sao JPopupMenu thay vì WebView dropdown?</h3>
 * JPopupMenu sử dụng cửa sổ native riêng (heavyweight) khi popup bị khuất
 * bởi component cha. Điều này cho phép nó vẽ tràn ra ngoài HeaderPanel 64px
 * mà không bị clipping — đây là tính năng không thể có với WebView.
 *
 * <h3>Cách mở rộng:</h3>
 * Thay thế {@link #filter(String)} bằng lời gọi API thực tế để lấy gợi ý
 * từ server (chạy trong SwingWorker để không block EDT).
 */
@Deprecated
public class SearchSuggestionsPopup {

    // ─── Layout constants ────────────────────────────────────────────────────
    private static final int ITEM_H     = 40;    // chiều cao mỗi dòng gợi ý
    private static final int MAX_ITEMS  = 7;     // số gợi ý tối đa hiển thị
    private static final int POP_W      = 500;   // khớp với độ rộng search bar
    private static final int ARC        = 10;    // bo góc popup
    private static final int SHADOW_R   = 6;     // độ sâu shadow

    // ─── Colors ───────────────────────────────────────────────────────────────
    private static final Color C_BG      = Color.WHITE;
    private static final Color C_BORDER  = new Color(0xD4D4D4);
    private static final Color C_HOVER   = new Color(0xF0F0F0);
    private static final Color C_TEXT    = new Color(0x1A1A1A);
    private static final Color C_BOLD    = new Color(0x0067C0);  // phần khớp query (xanh Win11)
    private static final Color C_ICON    = new Color(0xAAAAAA);
    private static final Color C_DIVIDER = new Color(0xECECEC);
    private static final Color C_SHADOW  = new Color(0, 0, 0, 40);

    // ─── Dữ liệu gợi ý mẫu ───────────────────────────────────────────────────
    // Trong production: thay thế bằng API call trả về List<String>
    private static final List<String> POOL = Arrays.asList(
        "Toán học lớp 10", "Toán học lớp 11", "Toán học lớp 12",
        "Vật lý đại cương", "Vật lý lượng tử",
        "Hóa học hữu cơ", "Hóa học vô cơ",
        "Lịch sử Việt Nam", "Lịch sử thế giới",
        "Địa lý tự nhiên", "Địa lý kinh tế",
        "Ngữ văn lớp 12", "Tiếng Anh giao tiếp", "Tiếng Anh IELTS",
        "Lập trình Java cơ bản", "Lập trình Python", "Lập trình C++",
        "Giải tích vi tích phân", "Đại số tuyến tính", "Xác suất thống kê",
        "Sinh học tế bào", "Sinh học phân tử",
        "Kinh tế vi mô", "Kinh tế vĩ mô", "Tài chính doanh nghiệp",
        "Machine Learning cơ bản", "Deep Learning", "Web development",
        "Thiết kế đồ họa", "Marketing căn bản", "Quản trị nhân lực"
    );

    // ─── State ───────────────────────────────────────────────────────────────
    private final JPopupMenu   menu;
    private final GlobalSearchBar owner;
    private Consumer<String>   onSelect;
    private List<String>       currentItems = Collections.emptyList();
    private boolean enabled = false;

    // ─────────────────────────────────────────────────────────────────────────
    public SearchSuggestionsPopup(GlobalSearchBar owner) {
        this.owner = owner;

        // Ép JPopupMenu dùng heavyweight window để vẽ ngoài HeaderPanel
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createEmptyBorder());
        menu.setBackground(new Color(0, 0, 0, 0));
        menu.setLayout(new BorderLayout());
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Cập nhật gợi ý theo query hiện tại.
     * Gọi từ DocumentListener của JTextField (đang trên EDT, an toàn).
     */
    public void update(String query) {
        if (!enabled) {
            hide();
            return;
        }
        if (query == null || query.isBlank()) {
            hide();
            return;
        }
        currentItems = filter(query.trim());
        if (currentItems.isEmpty()) {
            hide();
            return;
        }
        rebuild(currentItems, query.trim());
        // show() tự tính toán vị trí relative to `owner`
        // → (0, 48) = ngay bên dưới search bar 44px + 4px gap
        menu.show(owner, 0, owner.getHeight() + 4);
    }

    /** Ẩn popup. */
    public void hide() {
        if (menu.isVisible()) menu.setVisible(false);
    }

    /** Callback khi người dùng chọn một gợi ý. */
    public void setOnSelect(Consumer<String> handler) {
        this.onSelect = handler;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            hide();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Component getPopupComponent() {
        return menu;
    }

    // ─── Private ──────────────────────────────────────────────────────────────

    /** Lọc danh sách gợi ý theo query (case-insensitive, contains). */
    private List<String> filter(String q) {
        String lq = q.toLowerCase();
        return POOL.stream()
                   .filter(s -> s.toLowerCase().contains(lq))
                   .limit(MAX_ITEMS)
                   .collect(Collectors.toList());
    }

    /** Xây dựng lại nội dung popup. */
    private void rebuild(List<String> items, String query) {
        menu.removeAll();
        int popH = items.size() * ITEM_H + SHADOW_R + ARC;
        SuggestionsPanel panel = new SuggestionsPanel(items, query);
        panel.setPreferredSize(new Dimension(POP_W, popH));
        menu.add(panel, BorderLayout.CENTER);
        menu.pack();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner class: SuggestionsPanel — panel vẽ hoàn toàn custom
    // ─────────────────────────────────────────────────────────────────────────
    private class SuggestionsPanel extends JPanel {

        private final List<String> items;
        private final String       query;
        private int hoveredIdx = -1;

        SuggestionsPanel(List<String> items, String query) {
            super(null);
            this.items = items;
            this.query = query;
            setOpaque(false);

            // ── Mouse hover tracking ──────────────────────────────────────
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int idx = rowAt(e.getY());
                    if (idx != hoveredIdx) { hoveredIdx = idx; repaint(); }
                }
            });

            // ── Click → chọn gợi ý ───────────────────────────────────────
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int idx = rowAt(e.getY());
                    if (idx >= 0 && idx < items.size()) {
                        String selected = items.get(idx);
                        menu.setVisible(false);
                        owner.getField().setText(selected);
                        if (onSelect != null) onSelect.accept(selected);
                    }
                }
                @Override
                public void mouseExited(MouseEvent e) { hoveredIdx = -1; repaint(); }
            });

            // ── Con trỏ hand khi hover item ───────────────────────────────
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    setCursor(rowAt(e.getY()) >= 0
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = makeG2(g);
            int w = getWidth();
            int contentH = items.size() * ITEM_H;

            // ── Drop shadow ───────────────────────────────────────────────
            for (int i = SHADOW_R; i > 0; i--) {
                int alpha = (int)(50.0 * ((double)(SHADOW_R - i + 1) / SHADOW_R));
                g2.setColor(new Color(0, 0, 0, alpha));
                g2.fill(new RoundRectangle2D.Float(
                    i * 0.5f, SHADOW_R * 0.5f + i * 0.3f,
                    w - i, contentH - i * 0.3f,
                    ARC, ARC
                ));
            }

            // ── Nền trắng bo góc ──────────────────────────────────────────
            Shape bg = new RoundRectangle2D.Float(0, 0, w, contentH, ARC, ARC);
            g2.setColor(C_BG);
            g2.fill(bg);

            // ── Render từng item ──────────────────────────────────────────
            for (int i = 0; i < items.size(); i++) {
                int iy   = i * ITEM_H;
                boolean hov = (i == hoveredIdx);

                // Hover background (clip vào rounded shape)
                if (hov) {
                    g2.setClip(bg);
                    g2.setColor(C_HOVER);
                    g2.fillRect(0, iy, w, ITEM_H);
                    g2.setClip(null);
                }

                // Icon kính lúp nhỏ (bên trái)
                drawMiniSearchIcon(g2, 18, iy + ITEM_H / 2, 14);

                // Text với phần match in đậm màu accent
                drawHighlighted(g2, items.get(i), query, 44, iy + (ITEM_H + 14) / 2, w - 56);

                // Divider (trừ item cuối)
                if (i < items.size() - 1) {
                    g2.setColor(C_DIVIDER);
                    g2.drawLine(44, iy + ITEM_H - 1, w - 14, iy + ITEM_H - 1);
                }
            }

            // ── Viền ngoài ────────────────────────────────────────────────
            g2.setColor(C_BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(bg);

            g2.dispose();
        }

        /**
         * Vẽ text: phần khớp với query → in đậm màu xanh Win11,
         * phần còn lại → bình thường màu đen.
         */
        private void drawHighlighted(Graphics2D g2, String text, String query,
                                      int x, int y, int maxW) {
            String lText = text.toLowerCase();
            String lQuery = query.toLowerCase();
            int start = lText.indexOf(lQuery);

            Font plain = new Font("Segoe UI", Font.PLAIN, 13);
            Font bold  = new Font("Segoe UI", Font.BOLD,  13);

            if (start < 0) {
                g2.setFont(plain);
                g2.setColor(C_TEXT);
                drawClipped(g2, text, x, y, maxW);
                return;
            }

            String before = text.substring(0, start);
            String match  = text.substring(start, start + query.length());
            String after  = text.substring(start + query.length());

            int xCursor = x;

            g2.setFont(plain);
            g2.setColor(C_TEXT);
            g2.drawString(before, xCursor, y);
            xCursor += g2.getFontMetrics().stringWidth(before);

            g2.setFont(bold);
            g2.setColor(C_BOLD);
            g2.drawString(match, xCursor, y);
            xCursor += g2.getFontMetrics().stringWidth(match);

            g2.setFont(plain);
            g2.setColor(C_TEXT);
            g2.drawString(after, xCursor, y);
        }

        /** Vẽ icon kính lúp nhỏ bằng Graphics2D (sharp, không cần file ảnh). */
        private void drawMiniSearchIcon(Graphics2D g2, int cx, int cy, int size) {
            g2.setColor(C_ICON);
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int r = size / 3;
            g2.drawOval(cx - r - 1, cy - r - 1, r * 2, r * 2);
            int hx = (int)(cx - 1 + r * 0.7);
            int hy = (int)(cy - 1 + r * 0.7);
            g2.drawLine(hx, hy, cx + r, cy + r);
        }

        /** Cắt text bằng ellipsis nếu quá dài. */
        private void drawClipped(Graphics2D g2, String text, int x, int y, int maxW) {
            FontMetrics fm = g2.getFontMetrics();
            if (fm.stringWidth(text) <= maxW) {
                g2.drawString(text, x, y);
                return;
            }
            while (text.length() > 0 && fm.stringWidth(text + "…") > maxW) {
                text = text.substring(0, text.length() - 1);
            }
            g2.drawString(text + "…", x, y);
        }

        private int rowAt(int mouseY) {
            if (mouseY < 0) return -1;
            int idx = mouseY / ITEM_H;
            return idx < items.size() ? idx : -1;
        }
    }

    // ─── Static helper ────────────────────────────────────────────────────────
    private static Graphics2D makeG2(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
        return g2;
    }
}
