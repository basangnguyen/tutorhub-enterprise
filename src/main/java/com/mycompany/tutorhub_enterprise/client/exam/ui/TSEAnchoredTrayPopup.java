package com.mycompany.tutorhub_enterprise.client.exam.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * TSEAnchoredTrayPopup - A custom lightweight popup mimicking the Windows System Tray flyouts.
 * - Anchors to a trigger component.
 * - Draws a rounded rectangle with a caret (triangle) pointing at the trigger.
 * - Automatically closes when clicking outside.
 */
public class TSEAnchoredTrayPopup extends JPanel {

    private static final Color BG_COLOR = new Color(30, 30, 30);
    private static final Color BORDER_COLOR = new Color(80, 80, 80);
    private static final int CORNER_RADIUS = 12;
    private static final int CARET_WIDTH = 16;
    private static final int CARET_HEIGHT = 8;
    
    private final JLayeredPane layeredPane;
    private final JPanel blocker;
    private Component trigger;
    private int caretXOffset = 0;

    public TSEAnchoredTrayPopup(JComponent content, JLayeredPane layeredPane) {
        this.layeredPane = layeredPane;
        setOpaque(false);
        setLayout(new BorderLayout());
        
        // Wrap the content with some padding
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setOpaque(false);
        contentWrapper.setBorder(BorderFactory.createEmptyBorder(12, 12, 12 + CARET_HEIGHT, 12));
        contentWrapper.add(content, BorderLayout.CENTER);
        add(contentWrapper, BorderLayout.CENTER);

        // Create a blocker panel that covers the entire layered pane to catch clicks outside
        blocker = new JPanel();
        blocker.setOpaque(false);
        blocker.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                hidePopup();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // The main rounded rect body
        int bodyHeight = h - CARET_HEIGHT - 1;
        RoundRectangle2D.Float body = new RoundRectangle2D.Float(0, 0, w - 1, bodyHeight, CORNER_RADIUS, CORNER_RADIUS);

        // The caret (triangle) pointing down
        Path2D.Float caret = new Path2D.Float();
        int cx = caretXOffset;
        
        // Ensure caret is within bounds of the body
        cx = Math.max(CORNER_RADIUS + CARET_WIDTH / 2, Math.min(w - CORNER_RADIUS - CARET_WIDTH / 2, cx));

        caret.moveTo(cx - CARET_WIDTH / 2.0, bodyHeight);
        caret.lineTo(cx + CARET_WIDTH / 2.0, bodyHeight);
        caret.lineTo(cx, bodyHeight + CARET_HEIGHT);
        caret.closePath();

        // Combine shapes
        g2.setColor(BG_COLOR);
        g2.fill(body);
        g2.fill(caret);

        g2.setColor(BORDER_COLOR);
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(body);
        g2.draw(caret); // Draw caret outline
        
        // Overdraw the line separating body and caret to make them seamless
        g2.setColor(BG_COLOR);
        g2.drawLine(cx - CARET_WIDTH / 2 + 1, bodyHeight, cx + CARET_WIDTH / 2 - 1, bodyHeight);

        g2.dispose();
    }

    public void showPopup(Component triggerComponent) {
        this.trigger = triggerComponent;
        
        System.out.println("[TSE_TRAY] Showing popup anchored to: " + triggerComponent.getClass().getSimpleName());

        if (layeredPane == null) {
            System.out.println("[TSE_TRAY] Cannot show popup: layeredPane is null");
            return;
        }

        // Calculate preferred size based on content
        Dimension prefSize = getPreferredSize();
        setSize(prefSize);

        // Calculate location relative to the layeredPane
        Point triggerLoc = SwingUtilities.convertPoint(triggerComponent.getParent(), triggerComponent.getLocation(), layeredPane);
        
        int triggerCenterX = triggerLoc.x + triggerComponent.getWidth() / 2;
        int triggerTopY = triggerLoc.y;

        int popupX = triggerCenterX - prefSize.width / 2;
        int popupY = triggerTopY - prefSize.height - 4; // 4px padding above trigger

        // Clamp X to screen bounds (layeredPane bounds)
        int minX = 8;
        int maxX = layeredPane.getWidth() - prefSize.width - 8;
        
        int clampedX = Math.max(minX, Math.min(maxX, popupX));
        
        // Calculate where the caret should be relative to the popup's top-left
        caretXOffset = triggerCenterX - clampedX;
        
        System.out.println(String.format("[TSE_TRAY] Popup bounds: x=%d, y=%d, w=%d, h=%d", clampedX, popupY, prefSize.width, prefSize.height));
        setLocation(clampedX, popupY);

        // Setup blocker
        blocker.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
        
        // Add to layered pane
        layeredPane.add(blocker, JLayeredPane.POPUP_LAYER);
        layeredPane.add(this, Integer.valueOf(JLayeredPane.POPUP_LAYER + 1));
        
        layeredPane.revalidate();
        layeredPane.repaint();
        
        // Request focus so we can close on Escape key
        setFocusable(true);
        requestFocusInWindow();
        
        // Handle Escape key
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closePopup");
        actionMap.put("closePopup", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hidePopup();
            }
        });
    }

    public void hidePopup() {
        layeredPane.remove(this);
        layeredPane.remove(blocker);
        layeredPane.revalidate();
        layeredPane.repaint();
    }
}
