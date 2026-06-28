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
import java.util.List;
import java.util.function.Consumer;

public class GlobalSearchBar extends JPanel {

    private final JTextField searchField;
    private final ThumbnailPane thumbnailPane;
    private SearchHighlight currentHighlight;
    private final List<Consumer<String>> queryChangeListeners = new ArrayList<>();
    private final List<Consumer<String>> submitListeners = new ArrayList<>();
    private final List<Consumer<Boolean>> focusChangeListeners = new ArrayList<>();
    
    private boolean isFocused = false;
    private boolean isHovered = false;
    private boolean isExpanded = true;

    // Animation values (0.0 to 1.0)
    private float focusAlpha = 0.0f;
    private float hoverAlpha = 0.0f;
    private float expandAlpha = 1.0f; // Always expanded
    private Timer animTimer;

    // Dimensions
    private final int PILL_X = 10;
    private final int PILL_Y = 6;
    private final int PILL_W_EXPANDED = 440;
    private final int PILL_W_COLLAPSED = 40;
    private final int PILL_H = 40;
    private final int ARC = 40;

    public GlobalSearchBar() {
        super(null); 
        setOpaque(false);
        setPreferredSize(new Dimension(460, 52));

        currentHighlight = SearchHighlightProvider.getTodayHighlight();

        // 1. Search Icon (Left)
        JLabel searchIcon = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(blendColor(new Color(0x64748B), new Color(0, 103, 192), focusAlpha));
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2, cy = getHeight() / 2;
                int r = 6;
                g2.drawOval(cx - r - 2, cy - r - 2, r * 2, r * 2);
                g2.drawLine(cx + 2, cy + 2, cx + 7, cy + 7);
                g2.dispose();
            }
        };
        searchIcon.setBounds(PILL_X + 2, PILL_Y + 0, 36, PILL_H);
        add(searchIcon);

        // 2. Search Field (Middle)
        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Tìm kiếm trong TutorHub...");
        searchField.putClientProperty("JTextField.showClearButton", true);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setForeground(new Color(0x111827));
        searchField.setOpaque(false);
        searchField.setBorder(new EmptyBorder(0, 0, 0, 0));
        searchField.setBounds(PILL_X + 44, PILL_Y + 0, 230, PILL_H);
        searchField.setVisible(false); // Hidden when collapsed
        
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                isFocused = true;
                notifyFocusChanged(true);
            }
            @Override
            public void focusLost(FocusEvent e) {
                isFocused = false;
                notifyFocusChanged(false);
            }
        });
        add(searchField);

        // 3. Thumbnail Pane (Right)
        thumbnailPane = new ThumbnailPane();
        int thumbW = 150;
        int thumbH = 32;
        thumbnailPane.setBounds(PILL_X + PILL_W_EXPANDED - thumbW - 4, PILL_Y + (PILL_H - thumbH) / 2, thumbW, thumbH);
        thumbnailPane.setVisible(false); // Hidden when collapsed
        add(thumbnailPane);

        // Animation Engine
        animTimer = new Timer(16, e -> {
            boolean changed = false;
            if (isFocused && focusAlpha < 1.0f) { focusAlpha = Math.min(1.0f, focusAlpha + 0.1f); changed = true; }
            if (!isFocused && focusAlpha > 0.0f) { focusAlpha = Math.max(0.0f, focusAlpha - 0.1f); changed = true; }
            
            if (isHovered && hoverAlpha < 1.0f) { hoverAlpha = Math.min(1.0f, hoverAlpha + 0.1f); changed = true; }
            if (!isHovered && hoverAlpha > 0.0f) { hoverAlpha = Math.max(0.0f, hoverAlpha - 0.1f); changed = true; }

            if (isExpanded && expandAlpha < 1.0f) { expandAlpha = Math.min(1.0f, expandAlpha + 0.12f); changed = true; }
            if (!isExpanded && expandAlpha > 0.0f) { expandAlpha = Math.max(0.0f, expandAlpha - 0.12f); changed = true; }

            if (changed) {
                // Update bounds for expanding/collapsing
                int currentW = (int) (PILL_W_COLLAPSED + (PILL_W_EXPANDED - PILL_W_COLLAPSED) * expandAlpha);
                
                // Show/hide components based on expansion
                if (expandAlpha > 0.3f && !searchField.isVisible()) {
                    searchField.setVisible(true);
                    thumbnailPane.setVisible(true);
                } else if (expandAlpha <= 0.3f && searchField.isVisible()) {
                    searchField.setVisible(false);
                    thumbnailPane.setVisible(false);
                }

                // Smoothly slide thumbnail in
                thumbnailPane.setBounds(PILL_X + currentW - thumbW - 4, PILL_Y + (PILL_H - thumbH) / 2, thumbW, thumbH);

                repaint();
                searchIcon.repaint();
            }
        });
        animTimer.start();

        // Mouse Listener to Trigger Expansion
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { isHovered = true; }
            @Override public void mouseExited(MouseEvent e) { isHovered = false; }
            @Override public void mousePressed(MouseEvent e) { 
                if (!isExpanded) {
                    setExpanded(true);
                } else {
                    searchField.requestFocusInWindow();
                }
            }
        });
        searchIcon.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { isHovered = true; }
            @Override public void mouseExited(MouseEvent e) { isHovered = false; }
            @Override public void mousePressed(MouseEvent e) {
                if (!isExpanded) {
                    setExpanded(true);
                } else {
                    searchField.requestFocusInWindow();
                }
            }
        });
        searchField.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { isHovered = true; }
            @Override public void mouseExited(MouseEvent e) { isHovered = false; }
        });

        // Rotation Timer (60s)
        Timer rotationTimer = new Timer(60000, e -> {
            SwingWorker<SearchHighlight, Void> worker = new SwingWorker<>() {
                @Override
                protected SearchHighlight doInBackground() throws Exception {
                    return SearchHighlightProvider.getNextHighlight();
                }
                @Override
                protected void done() {
                    try {
                        SearchHighlight newHighlight = get();
                        if (newHighlight != null) {
                            thumbnailPane.animateTransition(newHighlight);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };
            worker.execute();
        });
        rotationTimer.start();

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updatePopup(); }
            @Override public void removeUpdate(DocumentEvent e) { updatePopup(); }
            @Override public void changedUpdate(DocumentEvent e) { updatePopup(); }
            private void updatePopup() {
                String text = searchField.getText();
                thumbnailPane.fade(text.isEmpty());
                notifyQueryChanged(text);
            }
        });

        searchField.addActionListener(e -> {
            notifySubmitted(searchField.getText());
        });

        // Global AWT listener to detect outside clicks and collapse
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) event;
                if (me.getID() == MouseEvent.MOUSE_PRESSED) {
                    Component clickedComponent = me.getComponent();
                    if (clickedComponent != null) {
                        if (!SwingUtilities.isDescendingFrom(clickedComponent, GlobalSearchBar.this)) {
                            // Clicked outside!
                            if (isExpanded) {
                                // Clear focus but do NOT collapse
                                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                            }
                        }
                    }
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    public void setExpanded(boolean expanded) {
        if (!expanded) return; // NEVER COLLAPSE
        if (this.isExpanded == expanded) return;
        this.isExpanded = expanded;
        searchField.requestFocusInWindow();
    }
    
    public JTextField getField() {
        return searchField;
    }

    public JTextField getSearchField() {
        return searchField;
    }

    public void addQueryChangeListener(Consumer<String> listener) {
        if (listener != null) {
            queryChangeListeners.add(listener);
        }
    }

    public void addSubmitListener(Consumer<String> listener) {
        if (listener != null) {
            submitListeners.add(listener);
        }
    }

    public void addFocusChangeListener(Consumer<Boolean> listener) {
        if (listener != null) {
            focusChangeListeners.add(listener);
        }
    }

    private void notifyQueryChanged(String query) {
        for (Consumer<String> listener : queryChangeListeners) {
            listener.accept(query);
        }
    }

    private void notifySubmitted(String query) {
        for (Consumer<String> listener : submitListeners) {
            listener.accept(query);
        }
    }

    private void notifyFocusChanged(boolean focused) {
        for (Consumer<Boolean> listener : focusChangeListeners) {
            listener.accept(focused);
        }
    }

    private Color blendColor(Color c1, Color c2, float ratio) {
        float r = c1.getRed() + ratio * (c2.getRed() - c1.getRed());
        float g = c1.getGreen() + ratio * (c2.getGreen() - c1.getGreen());
        float b = c1.getBlue() + ratio * (c2.getBlue() - c1.getBlue());
        float a = c1.getAlpha() + ratio * (c2.getAlpha() - c1.getAlpha());
        return new Color(Math.round(r), Math.round(g), Math.round(b), Math.round(a));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int currentW = (int) (PILL_W_COLLAPSED + (PILL_W_EXPANDED - PILL_W_COLLAPSED) * expandAlpha);

        // 1. Draw Soft Shadow (Multi-pass blur simulation)
        int shadowBaseAlpha = (int)(20 + hoverAlpha * 15 + focusAlpha * 10);
        Color shadowColor = new Color(0, 0, 0, shadowBaseAlpha);
        for (int i = 0; i < 4; i++) {
            g2.setColor(new Color(shadowColor.getRed(), shadowColor.getGreen(), shadowColor.getBlue(), shadowBaseAlpha / (i + 1)));
            g2.fillRoundRect(PILL_X - i, PILL_Y - i + 2, currentW + i*2, PILL_H + i*2, ARC, ARC);
        }

        // 2. Draw Glassmorphism Background
        Color bgNormal = new Color(255, 255, 255, 210);
        Color bgHover = new Color(255, 255, 255, 240);
        Color bgFocus = new Color(255, 255, 255, 255);
        Color currentBg = blendColor(blendColor(bgNormal, bgHover, hoverAlpha), bgFocus, focusAlpha);
        g2.setColor(currentBg);
        g2.fillRoundRect(PILL_X, PILL_Y, currentW, PILL_H, ARC, ARC);

        // 3. Draw Gradient Border
        Color borderNormal = new Color(0, 0, 0, 20);
        Color borderHover = new Color(0, 0, 0, 40);
        
        // TutorHub Theme Colors
        Color themeStart = new Color(74, 144, 226); // Light Blue
        Color themeEnd = new Color(144, 19, 254);   // Purple
        
        GradientPaint focusGradient = new GradientPaint(PILL_X, PILL_Y, themeStart, PILL_X + currentW, PILL_Y + PILL_H, themeEnd);
        
        g2.setPaint(focusAlpha > 0.01f ? focusGradient : blendColor(borderNormal, borderHover, hoverAlpha));
        g2.setStroke(new BasicStroke(1.0f + focusAlpha * 0.5f)); // Thicker border on focus
        
        if (focusAlpha > 0.0f && focusAlpha < 1.0f) {
            g2.setPaint(blendColor(borderNormal, borderHover, hoverAlpha));
            g2.drawRoundRect(PILL_X, PILL_Y, currentW - 1, PILL_H - 1, ARC, ARC);
            
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, focusAlpha));
            g2.setPaint(focusGradient);
            g2.drawRoundRect(PILL_X, PILL_Y, currentW - 1, PILL_H - 1, ARC, ARC);
        } else {
            g2.drawRoundRect(PILL_X, PILL_Y, currentW - 1, PILL_H - 1, ARC, ARC);
        }

        g2.dispose();
    }

    // Inner class for thumbnail drawing
    private class ThumbnailPane extends JComponent {
        private boolean isThumbHovered = false;
        private float opacity = 1.0f;
        private float imageScale = 1.0f;
        private Timer fadeTimer;
        private Timer thumbAnimTimer;

        public ThumbnailPane() {
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            updateTooltip();

            thumbAnimTimer = new Timer(16, e -> {
                boolean changed = false;
                if (isThumbHovered && imageScale < 1.03f) { imageScale = Math.min(1.03f, imageScale + 0.003f); changed = true; }
                if (!isThumbHovered && imageScale > 1.0f) { imageScale = Math.max(1.0f, imageScale - 0.003f); changed = true; }
                if (changed) repaint();
            });
            thumbAnimTimer.start();

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { isThumbHovered = true; isHovered = true; GlobalSearchBar.this.repaint(); }
                @Override public void mouseExited(MouseEvent e) { isThumbHovered = false; isHovered = false; GlobalSearchBar.this.repaint(); }
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (currentHighlight != null && opacity > 0.5f && isExpanded) {
                        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(GlobalSearchBar.this);
                        if (parent != null) {
                            SearchHighlightGalleryDialog dialog = new SearchHighlightGalleryDialog(parent, SearchHighlightProvider.getCurrentIndex());
                            dialog.setVisible(true);
                        }
                    }
                }
            });
        }
        
        private void updateTooltip() {
            if (currentHighlight != null) {
                setToolTipText(currentHighlight.getTitle() + " - Nhấn để khám phá \u2197");
            }
        }

        public void animateTransition(SearchHighlight newHighlight) {
            Timer t = new Timer(15, null);
            t.addActionListener(new ActionListener() {
                boolean out = true;
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (out) {
                        opacity -= 0.05f;
                        if (opacity <= 0.0f) {
                            opacity = 0.0f;
                            currentHighlight = newHighlight;
                            updateTooltip();
                            out = false;
                        }
                    } else {
                        opacity += 0.05f;
                        if (opacity >= 1.0f) {
                            opacity = 1.0f;
                            t.stop();
                        }
                    }
                    repaint();
                }
            });
            t.start();
        }

        public void fade(boolean in) {
            if (fadeTimer != null && fadeTimer.isRunning()) fadeTimer.stop();
            float step = in ? 0.08f : -0.08f;
            fadeTimer = new Timer(16, e -> {
                opacity += step;
                if (opacity >= 1.0f) { opacity = 1.0f; fadeTimer.stop(); }
                if (opacity <= 0.0f) { opacity = 0.0f; fadeTimer.stop(); }
                repaint();
            });
            fadeTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (opacity <= 0.0f || currentHighlight == null || expandAlpha < 0.3f) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            
            // Fade in alongside expansion
            float finalOpacity = opacity * ((expandAlpha - 0.3f) / 0.7f);
            if (finalOpacity <= 0) return;
            
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, finalOpacity));

            int w = getWidth();
            int h = getHeight();
            int arc = 32;

            Shape clipShape = new RoundRectangle2D.Float(1, 1, w - 2, h - 2, arc, arc);
            g2.setClip(clipShape);

            BufferedImage img = currentHighlight.getImage();
            if (img != null) {
                double baseScale = Math.max((double) w / img.getWidth(), (double) h / img.getHeight());
                double finalScale = baseScale * imageScale;
                
                int imgW = (int) (img.getWidth() * finalScale);
                int imgH = (int) (img.getHeight() * finalScale);
                int imgX = (w - imgW) / 2;
                int imgY = (h - imgH) / 2;
                
                g2.drawImage(img, imgX, imgY, imgW, imgH, null);
            }

            if (isThumbHovered) {
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillRect(0, 0, w, h);
            }
            
            g2.setClip(null);

            g2.setColor(new Color(255, 255, 255, 220));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 1, w - 2, h - 2, arc, arc);

            g2.setColor(new Color(255, 255, 255, 50));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(2, 2, w - 4, h - 4, arc - 2, arc - 2);

            int badgeW = 20;
            int badgeH = 14;
            int badgeX = w - badgeW - 8;
            int badgeY = h - badgeH - 6;
            
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 6, 6);
            
            g2.setColor(new Color(255, 255, 255, 220));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
            FontMetrics fm = g2.getFontMetrics();
            int textW = fm.stringWidth("VN");
            int textX = badgeX + (badgeW - textW) / 2;
            int textY = badgeY + ((badgeH - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString("VN", textX, textY);

            g2.dispose();
        }
    }
}
