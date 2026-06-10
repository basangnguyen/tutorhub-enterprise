package com.mycompany.tutorhub_enterprise.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class BannerSlider extends JPanel {
    private List<Image> images;
    private int currentIndex = 0;
    private int nextIndex = 1;
    private float alpha = 1.0f;
    private Timer slideTimer;
    private Timer fadeTimer;
    private boolean isHovered = false;

    public BannerSlider() {
        setOpaque(false);
        // Tỉ lệ 2400x520 tương ứng với chiều cao khoảng 215px khi hiển thị trên màn hình Desktop
        setPreferredSize(new Dimension(0, 215)); 
        // Bo góc nhẹ để giao diện mềm mại hơn
        putClientProperty("JComponent.arc", 20);

        images = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            try {
                URL url = getClass().getResource("/images/slide" + i + ".png");
                if (url == null) url = getClass().getResource("/images/slide" + i + ".jpg");
                if (url != null) images.add(new ImageIcon(url).getImage());
            } catch (Exception e) {}
        }

        slideTimer = new Timer(4000, e -> {
            if (images.size() <= 1 || isHovered) return;
            nextIndex = (currentIndex + 1) % images.size();
            alpha = 0.0f; 
            fadeTimer.start();
        });

        fadeTimer = new Timer(30, e -> {
            alpha += 0.06f;
            if (alpha >= 1.0f) {
                alpha = 1.0f;
                currentIndex = nextIndex;
                fadeTimer.stop();
            }
            repaint();
        });

        if (images.size() > 1) slideTimer.start();

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { isHovered = true; }
            @Override public void mouseExited(MouseEvent e) { isHovered = false; }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // Vẽ ảnh lấp đầy khung ngang và dọc (Object-fit: Cover style)
        if (!images.isEmpty()) {
            drawClippedImage(g2, images.get(currentIndex), 1.0f);
            if (alpha < 1.0f) {
                drawClippedImage(g2, images.get(nextIndex), alpha);
            }
        }
        
        // Vẽ các chấm điều hướng thon gọn
        drawNavigationDots(g2);
        g2.dispose();
    }

    private void drawClippedImage(Graphics2D g2, Image img, float currentAlpha) {
        int w = getWidth();
        int h = getHeight();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, currentAlpha));
        // Vẽ ảnh lấp đầy toàn bộ diện tích khung chứa
        g2.drawImage(img, 0, 0, w, h, null);
    }

    private void drawNavigationDots(Graphics2D g2) {
        if(images.size() <= 1) return;
        int dotSize = 7;
        int activeWidth = 20;
        int spacing = 6;
        int totalWidth = activeWidth + (images.size() - 1) * dotSize + (images.size() - 1) * spacing;
        int x = (getWidth() - totalWidth) / 2;
        int y = getHeight() - 15;

        for (int i = 0; i < images.size(); i++) {
            boolean isActive = (i == (alpha < 0.5f ? currentIndex : nextIndex));
            g2.setColor(isActive ? PRIMARY : new Color(200, 200, 200, 150));
            int w = isActive ? activeWidth : dotSize;
            g2.fillRoundRect(x, y, w, dotSize, dotSize, dotSize);
            x += w + spacing;
        }
    }
    private final Color PRIMARY = Color.decode("#246AF3");
}