package com.mycompany.tutorhub_enterprise.client.search;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.function.Consumer;
import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * A compact menu popup for search uploading.
 * Google-clean aesthetic.
 */
public class LensUploadPopup {

    private static final Color BG          = new Color(255, 255, 255);
    private static final Color BORDER      = new Color(0xDADCE0);
    private static final Color TEXT_PRIMARY = new Color(0x202124);
    private static final Color TEXT_MUTED   = new Color(0x5F6368);
    private static final int ARC = 16;
    private static final int POPUP_W = 220;
    private static final int POPUP_H = 90;

    private final JWindow window;
    private final JPanel contentPanel;

    private Consumer<File> onImageSelected;
    private Consumer<File> onDocumentSelected;

    public LensUploadPopup(Window owner) {
        window = new JWindow(owner);
        window.setType(Window.Type.POPUP);
        window.setFocusableWindowState(true);

        contentPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Shadow
                for (int i = 0; i < 4; i++) {
                    g2.setColor(new Color(0, 0, 0, Math.max(0, 8 - i * 2)));
                    g2.fillRoundRect(4 - i, 4 - i + 2, getWidth() - 8 + i * 2, getHeight() - 8 + i * 2, ARC + i * 2, ARC + i * 2);
                }

                // Background
                g2.setColor(BG);
                g2.fillRoundRect(4, 4, getWidth() - 8, getHeight() - 8, ARC, ARC);

                // Border
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(4, 4, getWidth() - 9, getHeight() - 9, ARC, ARC);

                g2.dispose();
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(10, 4, 10, 4));

        buildUI();

        window.setContentPane(contentPanel);
        window.setBackground(new Color(0, 0, 0, 0));
        window.setSize(POPUP_W, POPUP_H);

        // Close on outside click
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (!(event instanceof MouseEvent)) return;
            MouseEvent me = (MouseEvent) event;
            if (me.getID() != MouseEvent.MOUSE_PRESSED) return;
            Component clicked = me.getComponent();
            if (clicked == null) return;
            if (SwingUtilities.isDescendingFrom(clicked, window)) return;
            hide();
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    public void setOnImageSelected(Consumer<File> callback) {
        this.onImageSelected = callback;
    }

    public void setOnDocumentSelected(Consumer<File> callback) {
        this.onDocumentSelected = callback;
    }

    public void show(Component anchor, int yOffset) {
        Point loc = anchor.getLocationOnScreen();
        // Align left of popup with anchor left (like + icon)
        int x = loc.x - 4;
        int y = loc.y + anchor.getHeight() + yOffset;

        // Keep on screen
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        if (x + POPUP_W > screen.width) x = screen.width - POPUP_W - 10;
        if (x < 0) x = 10;

        window.setLocation(x, y);
        window.setVisible(true);
        window.toFront();
    }

    public void hide() {
        window.setVisible(false);
    }

    public boolean isShowing() {
        return window.isVisible();
    }

    private void buildUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setOpaque(false);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // 1. Tải hình ảnh lên
        FlatSVGIcon imgIcon = new FlatSVGIcon("images/icon/search_camera.svg", 15, 15);
        imgIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> TEXT_MUTED));
        MenuItemPanel itemImage = new MenuItemPanel("Tải hình ảnh lên", new JLabel(imgIcon), () -> {
            hide();
            openFileChooser(true);
        });
        mainPanel.add(itemImage);

        // 2. Tải tệp lên
        FlatSVGIcon docIcon = new FlatSVGIcon("images/icon/search_document.svg", 15, 15);
        docIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> TEXT_MUTED));
        MenuItemPanel itemFile = new MenuItemPanel("Tải tệp lên", new JLabel(docIcon), () -> {
            hide();
            openFileChooser(false);
        });
        mainPanel.add(itemFile);

        contentPanel.add(mainPanel, BorderLayout.CENTER);
    }

    private void openFileChooser(boolean imageOnly) {
        JFileChooser chooser = new JFileChooser();
        if (imageOnly) {
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Hình ảnh", "jpg", "jpeg", "png", "gif", "bmp"
            ));
        } else {
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Tài liệu & Tệp", "pdf", "doc", "docx", "txt", "xls", "xlsx", "zip", "rar"
            ));
        }
        if (chooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            if (imageOnly) {
                if (onImageSelected != null) onImageSelected.accept(f);
            } else {
                if (onDocumentSelected != null) onDocumentSelected.accept(f);
            }
        }
    }

    private static class MenuItemPanel extends JPanel {
        private boolean isHovered = false;
        private final Runnable action;

        MenuItemPanel(String text, JComponent iconComp, Runnable action) {
            this.action = action;
            setOpaque(false);
            setLayout(new BorderLayout(10, 0));
            setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            add(iconComp, BorderLayout.WEST);

            JLabel lblText = new JLabel(text);
            lblText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblText.setForeground(TEXT_PRIMARY);
            add(lblText, BorderLayout.CENTER);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (action != null) action.run();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (isHovered) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xF1F3F4));
                g2.fillRoundRect(4, 1, getWidth() - 8, getHeight() - 2, 6, 6);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
