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
    }
}
