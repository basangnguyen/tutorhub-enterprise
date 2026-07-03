package com.mycompany.tutorhub_enterprise.client.search;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class GhostTextTextField extends JTextField {
    private String ghostText = "";

    public GhostTextTextField() {
        super();
        setFocusTraversalKeysEnabled(false); // So we can capture TAB

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    if (!ghostText.isEmpty()) {
                        acceptGhostText();
                        e.consume();
                    } else {
                        // Normally Tab transfers focus forward
                        transferFocus();
                        e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    // Only accept ghost text if caret is at the very end
                    if (!ghostText.isEmpty() && getCaretPosition() == getText().length()) {
                        acceptGhostText();
                        e.consume();
                    }
                }
            }
        });
    }

    public void setGhostText(String text) {
        this.ghostText = (text == null) ? "" : text;
        repaint();
    }

    public String getGhostText() {
        return ghostText;
    }

    public void acceptGhostText() {
        if (!ghostText.isEmpty()) {
            String newText = getText() + ghostText;
            setText(newText);
            setGhostText("");
            setCaretPosition(newText.length());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (!ghostText.isEmpty() && getText() != null && !getText().isEmpty()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            FontMetrics fm = g2.getFontMetrics(getFont());
            // Swing's BasicTextUI often paints text starting at insets.left, but depends on margin too.
            // Using fm.stringWidth is a close approximation.
            int textWidth = fm.stringWidth(getText());
            
            Insets insets = getInsets();
            int scrollOffset = getHorizontalVisibility().getValue();
            
            // Typical X position for text
            int x = insets.left + textWidth - scrollOffset;
            
            // Baseline Y position
            int y = insets.top + fm.getAscent();

            g2.setFont(getFont());
            g2.setColor(new Color(156, 163, 175)); // A muted gray color

            g2.drawString(ghostText, x, y);
            g2.dispose();
        }
    }
}
