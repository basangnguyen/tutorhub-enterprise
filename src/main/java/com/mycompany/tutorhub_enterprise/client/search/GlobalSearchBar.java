package com.mycompany.tutorhub_enterprise.client.search;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Google-style global search bar.
 *
 * Key improvements over previous version:
 *  - Uses SearchDropdownWindow (JWindow) instead of JPopupMenu → no more flickering
 *  - Debounced search query (150ms) → no excessive provider calls on fast typing
 *  - Clean focus logic: dropdown closes only when focus leaves to something outside
 *    the dropdown window itself
 *  - animTimer only repaints when alpha values actually change
 *  - Dropdown is initialized lazily on first show (needs owner Window)
 */
public class GlobalSearchBar extends JPanel {

    // =========================================================
    // Fields
    // =========================================================

    private final GhostTextTextField searchField;
    private final ThumbnailPane thumbnailPane;

    /** Lazy-initialized on first show so we have an owner Window. */
    private SearchDropdownWindow dropdownWindow;

    private SearchHighlight currentHighlight;
    private final List<Consumer<String>>  queryChangeListeners  = new ArrayList<>();
    private final List<Consumer<String>>  submitListeners       = new ArrayList<>();
    private final List<Consumer<Boolean>> focusChangeListeners  = new ArrayList<>();

    private Function<SearchQuery, java.util.concurrent.CompletableFuture<List<SearchResult>>> dropdownResultsProvider =
            query -> java.util.concurrent.CompletableFuture.completedFuture(Collections.emptyList());
    private BooleanSupplier globalDropdownEnabledSupplier = () -> false;

    // State
    private boolean isFocused  = false;
    private boolean isHovered  = false;
    private boolean isExpanded = true;

    // Animation values (0.0–1.0)
    private float focusAlpha  = 0.0f;
    private float hoverAlpha  = 0.0f;
    private float expandAlpha = 1.0f;
    private Timer animTimer;

    // Debounce timer for search
    private Timer debounceTimer;
    private static final int DEBOUNCE_MS = 150;

    // Dimensions
    private static final int PILL_X           = 10;
    private static final int PILL_Y           = 6;
    private static final int PILL_W_EXPANDED  = 440;
    private static final int PILL_W_COLLAPSED = 40;
    private static final int PILL_H           = 40;
    private static final int ARC              = 40;

    // =========================================================
    // Constructor
    // =========================================================

    public GlobalSearchBar() {
        super(null);
        setOpaque(false);
        setPreferredSize(new Dimension(460, 52));

        currentHighlight = SearchHighlightProvider.getTodayHighlight();

        // ── 1. Search icon (left) ────────────────────────────
        JLabel searchIcon = buildSearchIcon();
        searchIcon.setBounds(PILL_X + 2, PILL_Y, 36, PILL_H);
        add(searchIcon);

        // ── 2. Search field ──────────────────────────────────
        searchField = new GhostTextTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Tìm kiếm trong TutorHub...");
        searchField.putClientProperty("JTextField.showClearButton", true);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setForeground(new Color(0x111827));
        searchField.setOpaque(false);
        searchField.setBorder(new EmptyBorder(0, 0, 0, 0));
        searchField.setBounds(PILL_X + 44, PILL_Y, 230, PILL_H);
        searchField.setVisible(false);
        add(searchField);

        // ── 3. Thumbnail (right) ─────────────────────────────
        thumbnailPane = new ThumbnailPane();
        int thumbW = 150, thumbH = 32;
        thumbnailPane.setBounds(PILL_X + PILL_W_EXPANDED - thumbW - 4,
                                PILL_Y + (PILL_H - thumbH) / 2, thumbW, thumbH);
        thumbnailPane.setVisible(false);
        add(thumbnailPane);

        // ── 4. Animation engine ──────────────────────────────
        animTimer = new Timer(16, e -> tickAnimation(searchIcon));
        animTimer.start();

        // ── 5. Search field events ───────────────────────────
        wireSearchFieldEvents();

        // ── 6. Mouse listeners ────────────────────────────────
        wireMouseListeners(searchIcon);

        // ── 7. Thumbnail rotation (every 60s) ────────────────
        Timer rotationTimer = new Timer(60_000, e -> rotateThumbnail());
        rotationTimer.start();

        // ── 8. Global outside-click detection ────────────────
        wireOutsideClickDetection();
    }

    // =========================================================
    // Public API
    // =========================================================

    public JTextField getField()       { return searchField; }
    public JTextField getSearchField() { return searchField; }

    public void setDropdownResultsProvider(Function<SearchQuery, java.util.concurrent.CompletableFuture<List<SearchResult>>> provider) {
        this.dropdownResultsProvider = provider == null
                ? query -> java.util.concurrent.CompletableFuture.completedFuture(Collections.emptyList()) : provider;
    }

    public void setGlobalDropdownEnabled(boolean enabled) {
        this.globalDropdownEnabledSupplier = () -> enabled;
        if (!enabled) hideDropdown();
    }

    public void setGlobalDropdownEnabledSupplier(BooleanSupplier supplier) {
        this.globalDropdownEnabledSupplier = supplier == null ? () -> false : supplier;
        if (!isGlobalDropdownEnabled()) hideDropdown();
    }

    public void hideDropdown() {
        if (dropdownWindow != null) dropdownWindow.hide();
    }

    public void addQueryChangeListener(Consumer<String> listener) {
        if (listener != null) queryChangeListeners.add(listener);
    }

    public void addSubmitListener(Consumer<String> listener) {
        if (listener != null) submitListeners.add(listener);
    }

    public void addFocusChangeListener(Consumer<Boolean> listener) {
        if (listener != null) focusChangeListeners.add(listener);
    }

    /** Called by commands to bind keyboard shortcuts (Ctrl+K). */
    public void addGlobalShortcut(JComponent root) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK), "globalSearch");
        root.getActionMap().put("globalSearch", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                searchField.requestFocusInWindow();
                searchField.selectAll();
            }
        });
    }

    public void setExpanded(boolean expanded) {
        if (!expanded) return; // NEVER COLLAPSE in current design
        if (this.isExpanded == expanded) return;
        this.isExpanded = expanded;
        searchField.requestFocusInWindow();
    }

    // =========================================================
    // Private – wiring
    // =========================================================

    private void wireSearchFieldEvents() {
        // Focus
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                isFocused = true;
                notifyFocusChanged(true);
                scheduleSearch();
            }

            @Override
            public void focusLost(FocusEvent e) {
                isFocused = false;
                notifyFocusChanged(false);
                // Only hide if focus went to something outside the dropdown
                Component opposite = e.getOppositeComponent();
                if (!isInsideDropdown(opposite)) {
                    hideDropdown();
                }
            }
        });

        // Typing → debounced search
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { onTextChanged(); }
            @Override public void removeUpdate(DocumentEvent e)  { onTextChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onTextChanged(); }
        });

        // Keyboard navigation within dropdown
        bindDropdownKeyboard();
    }

    private void onTextChanged() {
        searchField.setGhostText("");
        thumbnailPane.fade(searchField.getText().isEmpty());
        notifyQueryChanged(searchField.getText());
        scheduleSearch();
    }

    /**
     * Debounces the actual search so we don't hammer providers on every keystroke.
     * 150ms after the last keystroke the search executes.
     */
    private void scheduleSearch() {
        if (debounceTimer != null && debounceTimer.isRunning()) {
            debounceTimer.stop();
        }
        debounceTimer = new Timer(DEBOUNCE_MS, e -> performSearch());
        debounceTimer.setRepeats(false);
        debounceTimer.start();
    }

    private java.util.concurrent.CompletableFuture<List<SearchResult>> currentSearchFuture = null;

    private void performSearch() {
        if (!isGlobalDropdownEnabled() || !searchField.hasFocus() || !isShowing()) {
            hideDropdown();
            return;
        }

        SearchQuery query = SearchQuery.of(searchField.getText());
        
        // Hủy request cũ nếu có
        if (currentSearchFuture != null && !currentSearchFuture.isDone()) {
            currentSearchFuture.cancel(true);
        }

        // Get screen position right below the pill
        Point screenPt = getLocationOnScreen();
        int sx = screenPt.x + PILL_X;
        int sy = screenPt.y + getHeight() - 4;

        SearchDropdownWindow win = getOrCreateDropdown();
        win.setDropdownWidth(PILL_W_EXPANDED);
        
        // Hiển thị trạng thái loading trước
        win.showLoadingState(sx, sy, query);

        // Gọi provider
        currentSearchFuture = dropdownResultsProvider.apply(query);
        currentSearchFuture.thenAccept(results -> {
            SwingUtilities.invokeLater(() -> {
                // Kiểm tra xem dropdown có còn đang mở không
                if (win.isVisible() && searchField.hasFocus()) {
                    win.updateAndShow(sx, sy, results, query);
                    
                    // Tính toán ghost text từ Top Match
                    String ghost = "";
                    if (results != null && !results.isEmpty() && query != null && !query.isBlank()) {
                        SearchResult topMatch = results.get(0);
                        if (topMatch.getScore() > 0 && topMatch.getType() != SearchResultType.WEB) {
                            String title = topMatch.getTitle();
                            String qText = searchField.getText();
                            if (title != null && !qText.isEmpty()) {
                                if (title.toLowerCase().startsWith(qText.toLowerCase())) {
                                    ghost = title.substring(qText.length());
                                }
                            }
                        }
                    }
                    searchField.setGhostText(ghost);
                }
            });
        }).exceptionally(ex -> {
            if (!(ex.getCause() instanceof java.util.concurrent.CancellationException)) {
                ex.printStackTrace();
            }
            return null;
        });
    }

    private void wireMouseListeners(JLabel searchIcon) {
        MouseAdapter hover = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { isHovered = true; }
            @Override public void mouseExited(MouseEvent e)  { isHovered = false; }
        };

        MouseAdapter clickToFocus = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!isExpanded) setExpanded(true);
                else searchField.requestFocusInWindow();
            }
        };

        addMouseListener(hover);
        addMouseListener(clickToFocus);
        searchIcon.addMouseListener(hover);
        searchIcon.addMouseListener(clickToFocus);
        searchField.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { isHovered = true; }
            @Override public void mouseExited(MouseEvent e)  { isHovered = false; }
        });
    }

    private void wireOutsideClickDetection() {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (!(event instanceof MouseEvent)) return;
            MouseEvent me = (MouseEvent) event;
            if (me.getID() != MouseEvent.MOUSE_PRESSED) return;
            Component clicked = me.getComponent();
            if (clicked == null) return;
            if (SwingUtilities.isDescendingFrom(clicked, GlobalSearchBar.this)) return;
            if (isInsideDropdown(clicked)) return;
            // Clicked truly outside
            hideDropdown();
            KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    private void bindDropdownKeyboard() {
        InputMap im = searchField.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = searchField.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "dd_down");
        am.put("dd_down", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!isGlobalDropdownEnabled()) return;
                if (dropdownWindow == null || !dropdownWindow.isShowing()) scheduleSearch();
                else dropdownWindow.moveDown();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "dd_up");
        am.put("dd_up", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (isGlobalDropdownEnabled() && dropdownWindow != null && dropdownWindow.isShowing())
                    dropdownWindow.moveUp();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "dd_enter");
        am.put("dd_enter", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (isGlobalDropdownEnabled() && dropdownWindow != null && dropdownWindow.isShowing()) {
                    String txt = searchField.getText();
                    dropdownWindow.activateSelected();
                    if (!txt.isBlank()) notifySubmitted(txt);
                } else {
                    notifySubmitted(searchField.getText());
                }
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "dd_esc");
        am.put("dd_esc", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                hideDropdown();
                searchField.setText("");
            }
        });
    }

    private void rotateThumbnail() {
        SwingWorker<SearchHighlight, Void> worker = new SwingWorker<>() {
            @Override protected SearchHighlight doInBackground() {
                return SearchHighlightProvider.getNextHighlight();
            }
            @Override protected void done() {
                try {
                    SearchHighlight h = get();
                    if (h != null) thumbnailPane.animateTransition(h);
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    // =========================================================
    // Private – helpers
    // =========================================================

    private SearchDropdownWindow getOrCreateDropdown() {
        if (dropdownWindow == null) {
            Window ownerWindow = SwingUtilities.getWindowAncestor(this);
            dropdownWindow = new SearchDropdownWindow(ownerWindow);
        }
        return dropdownWindow;
    }

    private boolean isInsideDropdown(Component c) {
        if (c == null || dropdownWindow == null) return false;
        Window w = SwingUtilities.getWindowAncestor(c);
        return w != null && w.equals(dropdownWindow.getWindow());
    }

    private boolean isGlobalDropdownEnabled() {
        try {
            return globalDropdownEnabledSupplier != null && globalDropdownEnabledSupplier.getAsBoolean();
        } catch (Exception ex) {
            return false;
        }
    }

    private void notifyQueryChanged(String q) {
        queryChangeListeners.forEach(l -> l.accept(q));
    }

    private void notifySubmitted(String q) {
        submitListeners.forEach(l -> l.accept(q));
    }

    private void notifyFocusChanged(boolean f) {
        focusChangeListeners.forEach(l -> l.accept(f));
    }

    // =========================================================
    // Animation tick
    // =========================================================

    private void tickAnimation(JLabel searchIcon) {
        boolean changed = false;

        float targetFocus  = isFocused  ? 1f : 0f;
        float targetHover  = isHovered  ? 1f : 0f;
        float targetExpand = isExpanded  ? 1f : 0f;

        if (Math.abs(focusAlpha - targetFocus) > 0.005f) {
            focusAlpha += (targetFocus - focusAlpha) * 0.15f;
            changed = true;
        } else if (focusAlpha != targetFocus) {
            focusAlpha = targetFocus;
            changed = true;
        }

        if (Math.abs(hoverAlpha - targetHover) > 0.005f) {
            hoverAlpha += (targetHover - hoverAlpha) * 0.15f;
            changed = true;
        } else if (hoverAlpha != targetHover) {
            hoverAlpha = targetHover;
            changed = true;
        }

        if (Math.abs(expandAlpha - targetExpand) > 0.005f) {
            expandAlpha += (targetExpand - expandAlpha) * 0.15f;
            changed = true;
        } else if (expandAlpha != targetExpand) {
            expandAlpha = targetExpand;
            changed = true;
        }

        if (changed) {
            int currentW = (int)(PILL_W_COLLAPSED + (PILL_W_EXPANDED - PILL_W_COLLAPSED) * expandAlpha);
            int thumbW = 150, thumbH = 32;

            if (expandAlpha > 0.3f && !searchField.isVisible()) {
                searchField.setVisible(true);
                thumbnailPane.setVisible(true);
            } else if (expandAlpha <= 0.3f && searchField.isVisible()) {
                searchField.setVisible(false);
                thumbnailPane.setVisible(false);
            }

            thumbnailPane.setBounds(PILL_X + currentW - thumbW - 4,
                                    PILL_Y + (PILL_H - thumbH) / 2, thumbW, thumbH);
            repaint();
            searchIcon.repaint();
        }
    }

    // =========================================================
    // paintComponent
    // =========================================================

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int currentW = (int)(PILL_W_COLLAPSED + (PILL_W_EXPANDED - PILL_W_COLLAPSED) * expandAlpha);

        // Shadow
        int shadowBaseAlpha = (int)(20 + hoverAlpha * 15 + focusAlpha * 10);
        for (int i = 0; i < 4; i++) {
            g2.setColor(new Color(0, 0, 0, Math.max(0, shadowBaseAlpha / (i + 1))));
            g2.fillRoundRect(PILL_X - i, PILL_Y - i + 2, currentW + i*2, PILL_H + i*2, ARC, ARC);
        }

        // Background (glassmorphism)
        Color bgN = new Color(255, 255, 255, 210);
        Color bgH = new Color(255, 255, 255, 240);
        Color bgF = new Color(255, 255, 255, 255);
        g2.setColor(blendColor(blendColor(bgN, bgH, hoverAlpha), bgF, focusAlpha));
        g2.fillRoundRect(PILL_X, PILL_Y, currentW, PILL_H, ARC, ARC);

        // Border
        Color borderN = new Color(0, 0, 0, 20);
        Color borderH = new Color(0, 0, 0, 40);
        Color themeA  = new Color(74, 144, 226);
        Color themeB  = new Color(144, 19, 254);
        GradientPaint focusGrad = new GradientPaint(
                PILL_X, PILL_Y, themeA, PILL_X + currentW, PILL_Y + PILL_H, themeB);

        if (focusAlpha > 0.01f) {
            if (focusAlpha < 1.0f) {
                g2.setPaint(blendColor(borderN, borderH, hoverAlpha));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(PILL_X, PILL_Y, currentW - 1, PILL_H - 1, ARC, ARC);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, focusAlpha));
            }
            g2.setPaint(focusGrad);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(PILL_X, PILL_Y, currentW - 1, PILL_H - 1, ARC, ARC);
        } else {
            g2.setPaint(blendColor(borderN, borderH, hoverAlpha));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(PILL_X, PILL_Y, currentW - 1, PILL_H - 1, ARC, ARC);
        }

        g2.dispose();
    }

    private JLabel buildSearchIcon() {
        return new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(blendColor(new Color(0x64748B), new Color(0, 103, 192), focusAlpha));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2, cy = getHeight() / 2;
                int r = 6;
                g2.drawOval(cx - r - 2, cy - r - 2, r * 2, r * 2);
                g2.drawLine(cx + 2, cy + 2, cx + 7, cy + 7);
                g2.dispose();
            }
        };
    }

    private Color blendColor(Color c1, Color c2, float ratio) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        float r = c1.getRed()   + ratio * (c2.getRed()   - c1.getRed());
        float g = c1.getGreen() + ratio * (c2.getGreen() - c1.getGreen());
        float b = c1.getBlue()  + ratio * (c2.getBlue()  - c1.getBlue());
        float a = c1.getAlpha() + ratio * (c2.getAlpha() - c1.getAlpha());
        return new Color(Math.round(r), Math.round(g), Math.round(b), Math.round(a));
    }

    // =========================================================
    // Inner: ThumbnailPane (unchanged logic, same as before)
    // =========================================================

    private class ThumbnailPane extends JComponent {
        private boolean isThumbHovered = false;
        private float opacity = 1.0f;
        private float imageScale = 1.0f;
        private Timer fadeTimer;
        private final Timer thumbAnimTimer;

        ThumbnailPane() {
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            updateTooltip();

            thumbAnimTimer = new Timer(16, e -> {
                boolean changed = false;
                if (isThumbHovered && imageScale < 1.03f) { imageScale = Math.min(1.03f, imageScale + 0.003f); changed = true; }
                if (!isThumbHovered && imageScale > 1.0f)  { imageScale = Math.max(1.0f,  imageScale - 0.003f); changed = true; }
                if (changed) repaint();
            });
            thumbAnimTimer.start();

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { isThumbHovered = true;  isHovered = true;  GlobalSearchBar.this.repaint(); }
                @Override public void mouseExited(MouseEvent e)  { isThumbHovered = false; isHovered = false; GlobalSearchBar.this.repaint(); }
                @Override public void mouseClicked(MouseEvent e) {
                    if (currentHighlight != null && opacity > 0.5f && isExpanded) {
                        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(GlobalSearchBar.this);
                        if (parent != null) {
                            SearchHighlightGalleryDialog dialog =
                                    new SearchHighlightGalleryDialog(parent, SearchHighlightProvider.getCurrentIndex());
                            dialog.setVisible(true);
                        }
                    }
                }
            });
        }

        private void updateTooltip() {
            if (currentHighlight != null)
                setToolTipText(currentHighlight.getTitle() + " – Nhấn để khám phá ↗");
        }

        void animateTransition(SearchHighlight newHighlight) {
            Timer t = new Timer(15, null);
            t.addActionListener(new ActionListener() {
                boolean out = true;
                @Override public void actionPerformed(ActionEvent e) {
                    if (out) {
                        opacity -= 0.05f;
                        if (opacity <= 0f) {
                            opacity = 0f;
                            currentHighlight = newHighlight;
                            updateTooltip();
                            out = false;
                        }
                    } else {
                        opacity += 0.05f;
                        if (opacity >= 1f) { opacity = 1f; t.stop(); }
                    }
                    repaint();
                }
            });
            t.start();
        }

        void fade(boolean in) {
            if (fadeTimer != null && fadeTimer.isRunning()) fadeTimer.stop();
            float step = in ? 0.08f : -0.08f;
            fadeTimer = new Timer(16, e -> {
                opacity += step;
                if (opacity >= 1f) { opacity = 1f; fadeTimer.stop(); }
                if (opacity <= 0f) { opacity = 0f; fadeTimer.stop(); }
                repaint();
            });
            fadeTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (opacity <= 0f || currentHighlight == null || expandAlpha < 0.3f) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            float finalOpacity = opacity * ((expandAlpha - 0.3f) / 0.7f);
            if (finalOpacity <= 0) return;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, finalOpacity));

            int w = getWidth(), h = getHeight(), arc = 32;
            Shape clip = new RoundRectangle2D.Float(1, 1, w - 2, h - 2, arc, arc);
            g2.setClip(clip);

            BufferedImage img = currentHighlight.getImage();
            if (img != null) {
                double baseScale = Math.max((double) w / img.getWidth(), (double) h / img.getHeight());
                double finalScale = baseScale * imageScale;
                int imgW = (int)(img.getWidth()  * finalScale);
                int imgH = (int)(img.getHeight() * finalScale);
                g2.drawImage(img, (w - imgW) / 2, (h - imgH) / 2, imgW, imgH, null);
            }

            if (isThumbHovered) { g2.setColor(new Color(0, 0, 0, 30)); g2.fillRect(0, 0, w, h); }

            g2.setClip(null);
            g2.setColor(new Color(255, 255, 255, 220));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 1, w - 2, h - 2, arc, arc);

            // VN badge
            int bW = 20, bH = 14, bX = w - bW - 8, bY = h - bH - 6;
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(bX, bY, bW, bH, 6, 6);
            g2.setColor(new Color(255, 255, 255, 220));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
            FontMetrics fm = g2.getFontMetrics();
            int tx = bX + (bW - fm.stringWidth("VN")) / 2;
            int ty = bY + ((bH - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString("VN", tx, ty);

            g2.dispose();
        }
    }
}
