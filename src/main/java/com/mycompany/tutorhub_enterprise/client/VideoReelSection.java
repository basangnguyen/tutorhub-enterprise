package com.mycompany.tutorhub_enterprise.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.net.URI;
import java.net.URL;
import java.io.File;

public class VideoReelSection extends JPanel {

    private JPanel cardsContainer;
    private java.util.List<String> currentReelsData;
    private static boolean b2WarningLogged = false;

    // --- BẢNG MÀU SAAS PREMIUM ---
    private final Color TEXT_TITLE = Color.decode("#0F172A");
    private final Color TEXT_MUTED = Color.decode("#94A3B8");
    private final Color PRIMARY = Color.decode("#3B82F6");
    private final Color BORDER = Color.decode("#E2E8F0");

    public VideoReelSection() {
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(new EmptyBorder(16, 24, 16, 24));

        // 1. HEADER
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titleContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleContainer.setOpaque(false);

        JLabel icoCamera = new JLabel();
        icoCamera.setIcon(new com.formdev.flatlaf.extras.FlatSVGIcon(
                "images/icon/camera-photo-recording-svgrepo-com.svg", 22, 22));

        JLabel title = new JLabel("Locket Class");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(TEXT_TITLE);

        titleContainer.add(icoCamera);
        titleContainer.add(title);

        JLabel seeAll = new JLabel("Xem tất cả");
        seeAll.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        seeAll.setForeground(PRIMARY);
        seeAll.setCursor(new Cursor(Cursor.HAND_CURSOR));

        header.add(titleContainer, BorderLayout.WEST);
        header.add(seeAll, BorderLayout.EAST);

        // 2. DANH SÁCH VIDEO (ĐÃ SỬA: Dùng GridLayout 5 cột để thẳng hàng tuyệt đối với
        // lớp học)
        cardsContainer = new JPanel(new GridLayout(1, 5, 16, 0));
        cardsContainer.setOpaque(false);

        // Dữ liệu sẽ được nạp động từ loadReels()

        // Đã bỏ JScrollPane để thẻ tự do căn chỉnh theo lưới cha
        add(header, BorderLayout.NORTH);
        add(cardsContainer, BorderLayout.CENTER);
    }

    public void loadReels(java.util.List<String> data) {
        this.currentReelsData = data;
        cardsContainer.removeAll();
        if (data == null || data.isEmpty()) {
            // Empty state card
            VideoCard emptyCard = new VideoCard("Chia sẻ khoảnh khắc đầu tiên!", "@you", "/images/icon/icon_locket.png",
                    0);
            cardsContainer.add(emptyCard);
        } else {
            int count = Math.min(5, data.size());
            for (int i = 0; i < count; i++) {
                String[] parts = data.get(i).split(";;");
                if (parts.length >= 3) {
                    String title = parts[2];
                    String author = "@tutor";
                    if (parts.length >= 5 && parts[4] != null && !parts[4].isEmpty())
                        author = parts[4];
                    String imgPath = parts[1]; // Locket giờ chỉ tải ảnh, nên link chính là ảnh
                    addVideoCard(title, author, imgPath, i);
                }
            }
        }
        cardsContainer.revalidate();
        cardsContainer.repaint();
    }

    private void addVideoCard(String title, String author, String imgPath, int index) {
        VideoCard card = new VideoCard(title, author, imgPath, index);
        cardsContainer.add(card);
    }

    // =========================================================
    // GIAO DIỆN TỪNG THẺ ẢNH LOCKET
    // =========================================================
    class VideoCard extends JPanel {
        private Image thumbnail;
        private final int THUMB_HEIGHT = 120;
        private boolean isHover = false;

        private JLabel lblTitle;
        private JLabel lblAuthor;
        private String rawTitle;
        private int reelIndex;

        public VideoCard(String title, String author, String imgPath, int index) {
            this.rawTitle = title;
            this.reelIndex = index;
            // Chiều rộng = 0 để GridLayout tự quyết định và co giãn, chiều cao chừa 12px
            // cho shadow
            setPreferredSize(new Dimension(0, 205 + 12));
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setLayout(null);

            // Load ảnh GIF hoặc ảnh từ HTTP
            new Thread(() -> {
                try {
                    URL url = null;
                    if (imgPath.startsWith("http")) {
                        if (!com.mycompany.tutorhub_enterprise.utils.B2Helper.isConfigured()) {
                            if (!b2WarningLogged) {
                                System.err.println(
                                        "[WARNING] Video service not configured. Missing Backblaze B2 credentials.");
                                b2WarningLogged = true;
                            }
                            url = null;
                        } else {
                            String presigned = com.mycompany.tutorhub_enterprise.utils.B2Helper
                                    .getPresignedUrl(imgPath);
                            url = new URL(presigned);
                        }
                    } else {
                        url = getClass().getResource(imgPath);
                    }
                    if (url != null) {
                        if (imgPath.endsWith(".gif")) {
                            thumbnail = new ImageIcon(url).getImage();
                        } else {
                            if (imgPath.startsWith("http")) {
                                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                                if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
                                    try (java.io.InputStream in = conn.getInputStream()) {
                                        byte[] bytes = in.readAllBytes();
                                        thumbnail = new ImageIcon(bytes).getImage();
                                    }
                                }
                            } else {
                                thumbnail = new ImageIcon(url).getImage();
                            }
                        }
                        if (thumbnail != null) {
                            repaint();
                        }
                    }
                } catch (Exception e) {
                }
            }).start();

            lblTitle = new JLabel();
            add(lblTitle);

            lblAuthor = new JLabel(author);
            lblAuthor.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblAuthor.setForeground(TEXT_MUTED);
            add(lblAuthor);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHover = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    openVideo(reelIndex);
                }
            });
        }

        // ĐÃ CHỈNH SỬA: Lắng nghe sự kiện co giãn của thẻ để tự động bóp chữ và căn
        // chỉnh
        @Override
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);
            lblTitle.setBounds(14, THUMB_HEIGHT + 12, width - 28, 40);
            lblTitle.setText("<html><div style='width:" + (width - 28)
                    + "px; font-family: Segoe UI; font-size: 13px; font-weight: normal; color: #1E293B; line-height: 1.4;'>"
                    + rawTitle + "</div></html>");
            lblAuthor.setBounds(14, THUMB_HEIGHT + 54, width - 28, 20);
        }

        private void openVideo(int index) {
            try {
                JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(VideoReelSection.this);
                NativeReelPlayer player = new NativeReelPlayer(parentFrame, currentReelsData, index);
                if (parentFrame instanceof MainDashboard) {
                    ((MainDashboard) parentFrame).activeLocketPlayer = player;
                }
                player.setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể khởi tạo trình phát video: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight() - 12; // Trừ khoảng trống cho bóng đổ
            int arc = 20;
            int yOffset = isHover ? 2 : 6;

            if (isHover) {
                g2.setColor(new Color(15, 23, 42, 14));
                g2.fillRoundRect(2, yOffset + 4, w - 4, h - 4, arc, arc);
            } else {
                g2.setColor(new Color(15, 23, 42, 8));
                g2.fillRoundRect(2, yOffset + 2, w - 4, h - 2, arc, arc);
            }

            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, yOffset, w, h, arc, arc);
            g2.setColor(isHover ? PRIMARY : BORDER);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(0, yOffset, w - 1, h - 1, arc, arc);

            Shape oldClip = g2.getClip();
            Area clipArea = new Area(new RoundRectangle2D.Float(0, yOffset, w, THUMB_HEIGHT + 10, arc, arc));
            Area bottomRect = new Area(new Rectangle(0, yOffset + THUMB_HEIGHT - 10, w, 20));
            clipArea.add(bottomRect);

            g2.setClip(clipArea);
            if (thumbnail != null) {
                g2.drawImage(thumbnail, 0, yOffset, w, THUMB_HEIGHT, this);
                g2.setColor(new Color(15, 23, 42, isHover ? 30 : 15));
                g2.fillRect(0, yOffset, w, THUMB_HEIGHT);
            } else {
                g2.setColor(Color.decode("#F8FAFC"));
                g2.fillRect(0, yOffset, w, THUMB_HEIGHT);
            }
            g2.setClip(oldClip);

            g2.setColor(BORDER);
            g2.drawLine(0, yOffset + THUMB_HEIGHT, w, yOffset + THUMB_HEIGHT);

            g2.dispose();
        }
    }
}