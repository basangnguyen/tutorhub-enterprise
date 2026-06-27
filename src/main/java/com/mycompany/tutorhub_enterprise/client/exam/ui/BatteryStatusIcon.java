package com.mycompany.tutorhub_enterprise.client.exam.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * BatteryStatusIcon – Pure Graphics2D battery widget.
 *
 * Draws:
 *   ┌──────────────┐ ▌
 *   │  ████████    │ ▌   (filled body + nub on right)
 *   └──────────────┘ ▌
 *
 * Fill colour:
 *   >= 50 %  →  green   #22C55E
 *   >= 20 %  →  amber   #F59E0B
 *   <  20 %  →  red     #EF4444
 *
 * When isCharging = true, a lightning bolt ⚡ is painted in gold
 * over the fill area using Path2D (no SVG dependency).
 *
 * API:
 *   setBatteryPercent(int)   – 0..100
 *   setCharging(boolean)
 */
public class BatteryStatusIcon extends JComponent {

    // ── colour constants ──────────────────────────────────────────
    private final Color colOutline;
    private static final Color COL_GREEN   = Color.decode("#22C55E");
    private static final Color COL_AMBER   = Color.decode("#F59E0B");
    private static final Color COL_RED     = Color.decode("#EF4444");
    private static final Color COL_BOLT    = Color.decode("#FACC15");  // yellow-400

    // ── state ─────────────────────────────────────────────────────
    private int     batteryPercent = 100;
    private boolean isCharging     = false;

    // ── preferred size ────────────────────────────────────────────
    private static final int PREF_W = 36;
    private static final int PREF_H = 28;

    public BatteryStatusIcon() {
        this(new Color(255, 255, 255, 180));
    }

    public BatteryStatusIcon(Color outlineColor) {
        this.colOutline = outlineColor;
        setOpaque(false);
        setPreferredSize(new Dimension(PREF_W, PREF_H));
        setMinimumSize(new Dimension(PREF_W, PREF_H));
        setMaximumSize(new Dimension(PREF_W, PREF_H));
    }

    // ── public API ────────────────────────────────────────────────
    public void setBatteryPercent(int percent) {
        this.batteryPercent = Math.max(0, Math.min(100, percent));
        repaint();
    }

    public void setCharging(boolean charging) {
        this.isCharging = charging;
        repaint();
    }

    public int getBatteryPercent()  { return batteryPercent; }
    public boolean isCharging()     { return isCharging; }

    // ── painting ──────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = getWidth();
        int h = getHeight();

        // ── geometry ─────────────────────────────────────────────
        // Fix the battery icon to a standard size, e.g. 18x10, centered in the component
        int drawW = 18;
        int drawH = 10;
        
        // Center the drawing
        float bx = (w - drawW) / 2f;
        float by = (h - drawH) / 2f;
        
        int nubW   = 1;   // width of the + terminal nub
        int nubH   = 3;   // height of nub (centred vertically)
        int bodyW  = drawW - nubW;         // body width
        int bodyH  = drawH;
        int arc    = 2;                    // body corner arc

        // body bounding rect
        float bw = bodyW;
        float bh = bodyH;

        // inner fill area (1 px inset from stroke)
        int pad    = 1;
        float ix   = bx + pad;
        float iy   = by + pad;
        float iw   = bw - pad * 2;
        float ih   = bh - pad * 2;

        // fill width according to percent
        float fillW = iw * (batteryPercent / 100f);

        // fill colour
        Color fillCol = batteryPercent >= 50 ? COL_GREEN
                      : batteryPercent >= 20 ? COL_AMBER
                      : COL_RED;

        // ── draw body outline ─────────────────────────────────────
        g2.setColor(colOutline);
        g2.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new RoundRectangle2D.Float(bx, by, bw, bh, arc, arc));

        // ── draw nub (+terminal) on right ────────────────────────
        float nubX  = bx + bw;
        float nubY  = by + (bh - nubH) / 2f;
        g2.setStroke(new BasicStroke(1.0f));
        g2.setColor(colOutline);
        g2.fill(new RoundRectangle2D.Float(nubX, nubY, nubW, nubH, 1, 1));

        // ── draw fill ─────────────────────────────────────────────
        if (fillW > 0) {
            g2.setColor(fillCol);
            // clip to inner area so fill stays inside body
            Shape innerClip = new RoundRectangle2D.Float(ix, iy, iw, ih, Math.max(1, arc - pad), Math.max(1, arc - pad));
            g2.setClip(innerClip);
            g2.fill(new Rectangle2D.Float(ix, iy, fillW, ih));
            g2.setClip(null);
        }

        // ── draw lightning bolt when charging ────────────────────
        if (isCharging) {
            drawBolt(g2, (int)(bx + bw * 0.5f), (int)(by + bh * 0.5f),
                         (int)(bw * 0.55f), (int)(bh * 0.90f));
        }

        g2.dispose();
    }

    /**
     * Draws a ⚡ lightning bolt centred at (cx, cy).
     * boltW / boltH define the bounding box of the bolt.
     */
    private void drawBolt(Graphics2D g2, int cx, int cy, int boltW, int boltH) {
        int hw = boltW / 2;
        int hh = boltH / 2;

        // Classic lightning bolt: top-right → middle-left → middle-right → bottom-left
        Path2D bolt = new Path2D.Float();
        bolt.moveTo(cx + hw * 0.3,  cy - hh);           // top-right
        bolt.lineTo(cx - hw * 0.1,  cy + hh * 0.05);    // mid-left notch
        bolt.lineTo(cx + hw * 0.25, cy + hh * 0.05);    // mid-right notch
        bolt.lineTo(cx - hw * 0.3,  cy + hh);           // bottom-left
        bolt.lineTo(cx + hw * 0.0,  cy - hh * 0.1);     // mid lower-right
        bolt.lineTo(cx - hw * 0.15, cy - hh * 0.1);     // mid lower-left
        bolt.closePath();

        // filled white bolt
        g2.setColor(Color.WHITE);
        g2.fill(bolt);

        // thin dark outline for legibility on light fill
        g2.setColor(new Color(0, 0, 0, 180));
        g2.setStroke(new BasicStroke(0.8f));
        g2.draw(bolt);
    }
}
