package com.mycompany.tutorhub_enterprise.client.search;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.net.URI;

public class SearchHighlightGalleryDialog extends JDialog {

    private int targetIndex;
    private float currentOffset;
    private Timer animTimer;

    public SearchHighlightGalleryDialog(JFrame parent, int startIndex) {
        super(parent, true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 210)); // Glassmorphism đen mờ

        targetIndex = startIndex;
        currentOffset = startIndex;

        // Cover Flow Panel
        GalleryPanel galleryPanel = new GalleryPanel();
        setContentPane(galleryPanel);

        // Close button (Custom painted X)
        JButton closeBtn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(255, 100, 100) : Color.WHITE);
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int s = 14; // size
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.drawLine(cx - s/2, cy - s/2, cx + s/2, cy + s/2);
                g2.drawLine(cx + s/2, cy - s/2, cx - s/2, cy + s/2);
                g2.dispose();
            }
        };
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.setBounds(parent.getWidth() - 60, 20, 40, 40);
        galleryPanel.add(closeBtn);
        closeBtn.addActionListener(e -> dispose());

        // Keyboard navigation
        galleryPanel.setFocusable(true);
        galleryPanel.requestFocusInWindow();
        galleryPanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose();
                } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    navigate(-1);
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    navigate(1);
                }
            }
        });

        // Mouse click to navigate/close
        galleryPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int cx = getWidth() / 2;
                int centerWidth = 800; // Expected center width
                if (e.getX() < cx - centerWidth / 2) {
                    navigate(-1);
                } else if (e.getX() > cx + centerWidth / 2) {
                    navigate(1);
                } else {
                    // Click center: open URL
                    SearchHighlight h = SearchHighlightProvider.getAt(targetIndex);
                    if (h != null && h.getInfoUrl() != null) {
                        try {
                            Desktop.getDesktop().browse(new URI(h.getInfoUrl()));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });

        // Animation Timer (60 fps)
        animTimer = new Timer(16, e -> {
            boolean changed = false;
            float diff = targetIndex - currentOffset;
            if (Math.abs(diff) > 0.01f) {
                currentOffset += diff * 0.15f; // Easing function
                changed = true;
            } else if (currentOffset != targetIndex) {
                currentOffset = targetIndex;
                changed = true;
            }
            if (changed) {
                galleryPanel.repaint();
            }
        });
        animTimer.start();

        // Size & Location
        setSize(parent.getWidth(), parent.getHeight());
        setLocationRelativeTo(parent);
    }

    private void navigate(int delta) {
        int total = SearchHighlightProvider.count();
        // Allow wrap around
        targetIndex = (targetIndex + delta) % total;
        if (targetIndex < 0) targetIndex += total;

        // To make animation shortest path wrap-around, adjust currentOffset
        // For example if going from 10 to 0, it's better to animate offset from -1 to 0
        if (delta == 1 && currentOffset > targetIndex) {
            currentOffset -= total;
        } else if (delta == -1 && currentOffset < targetIndex) {
            currentOffset += total;
        }
    }

    private class GalleryPanel extends JPanel {
        public GalleryPanel() {
            setLayout(null);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2 - 40; // Shift up a bit to make room for text
            int total = SearchHighlightProvider.count();

            // We draw from back to front.
            // Items further from center should be drawn first.
            // For a 5-item visible window, we check relative index.

            // Determine visible range (targetIndex - 2 to targetIndex + 2)
            // But we must sort them by absolute distance from currentOffset (descending) so center is drawn last.
            int[] indicesToDraw = new int[5];
            for (int i = 0; i < 5; i++) {
                indicesToDraw[i] = targetIndex - 2 + i;
            }

            // Sort by distance from currentOffset (descending -> furthest drawn first)
            for (int i = 0; i < 5; i++) {
                for (int j = i + 1; j < 5; j++) {
                    float distI = Math.abs(indicesToDraw[i] - currentOffset);
                    float distJ = Math.abs(indicesToDraw[j] - currentOffset);
                    if (distI < distJ) {
                        int temp = indicesToDraw[i];
                        indicesToDraw[i] = indicesToDraw[j];
                        indicesToDraw[j] = temp;
                    }
                }
            }

            for (int logicalIndex : indicesToDraw) {
                float distance = logicalIndex - currentOffset; // positive means to the right
                int realIndex = logicalIndex % total;
                if (realIndex < 0) realIndex += total;

                SearchHighlight highlight = SearchHighlightProvider.getAt(realIndex);
                if (highlight == null || highlight.getImage() == null) continue;

                drawCarouselItem(g2, highlight, distance, cx, cy);
            }

            // Draw text for the closest item (center)
            SearchHighlight centerHighlight = SearchHighlightProvider.getAt(targetIndex);
            if (centerHighlight != null) {
                // Fade text based on how close we are to integer index
                float centerDist = Math.abs(targetIndex - currentOffset);
                float textAlpha = Math.max(0f, 1f - centerDist * 2f);
                if (textAlpha > 0) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, textAlpha));
                    
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 36));
                    g2.setColor(Color.WHITE);
                    FontMetrics fm1 = g2.getFontMetrics();
                    int tX = cx - fm1.stringWidth(centerHighlight.getTitle()) / 2;
                    int tY = cy + 280;
                    g2.drawString(centerHighlight.getTitle(), tX, tY);

                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 18));
                    g2.setColor(new Color(200, 200, 200));
                    FontMetrics fm2 = g2.getFontMetrics();
                    String sub = centerHighlight.getSubtitle() + " • " + centerHighlight.getCategory();
                    int sX = cx - fm2.stringWidth(sub) / 2;
                    int sY = tY + 30;
                    g2.drawString(sub, sX, sY);

                    g2.setFont(new Font("Segoe UI", Font.ITALIC, 14));
                    g2.setColor(new Color(150, 150, 255));
                    FontMetrics fm3 = g2.getFontMetrics();
                    String link = "Nhấn vào ảnh để xem trên Wikipedia";
                    int lX = cx - fm3.stringWidth(link) / 2;
                    int lY = sY + 30;
                    g2.drawString(link, lX, lY);
                }
            }

            g2.dispose();
        }

        private void drawCarouselItem(Graphics2D g2, SearchHighlight highlight, float distance, int cx, int cy) {
            float absDist = Math.abs(distance);
            if (absDist > 2.5f) return; // Don't draw too far

            BufferedImage img = highlight.getPreviewImage();
            if (img == null) img = highlight.getImage(); // fallback if preview is missing
            if (img == null) return;

            // Constants
            int maxW = 860;
            int maxH = 480;
            double imgRatio = (double) img.getWidth() / img.getHeight();
            int baseW = maxW;
            int baseH = (int) (maxW / imgRatio);
            if (baseH > maxH) {
                baseH = maxH;
                baseW = (int) (maxH * imgRatio);
            }

            float spacing = 320f; // distance between items
            
            // Calculate scale (center = 1.0, sides = 0.6, outer = 0.4)
            float scale = 1.0f - absDist * 0.3f;
            if (scale < 0) scale = 0;

            // Calculate X offset
            float xOffset = Math.signum(distance) * (1f - (float)Math.pow(0.5f, absDist)) * spacing * 2.5f;

            // Calculate Alpha
            float alpha = 1.0f - absDist * 0.4f;
            if (alpha < 0) alpha = 0;
            if (alpha > 1) alpha = 1;

            int w = (int) (baseW * scale);
            int h = (int) (baseH * scale);
            int x = cx + (int) xOffset - w / 2;
            int y = cy - h / 2;
            int arc = (int) (30 * scale);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            // Clip
            Shape oldClip = g2.getClip();
            Shape clipShape = new RoundRectangle2D.Float(x, y, w, h, arc, arc);
            g2.setClip(clipShape);

            // Draw image Cover
            g2.drawImage(img, x, y, w, h, null);

            g2.setClip(oldClip);

            // Border
            g2.setColor(new Color(255, 255, 255, (int)(200 * alpha)));
            g2.setStroke(new BasicStroke(2.0f * scale));
            g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
        }
    }
}
