package com.mycompany.tutorhub_enterprise.client;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javax.swing.*;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.utils.B2Helper;

public class ReelsTabPanel extends JPanel {

    private final java.awt.Color MAIN_BG = java.awt.Color.decode("#FAF8FF");
    private final java.awt.Color TEXT_MAIN = java.awt.Color.decode("#17172F");
    private final java.awt.Color TEXT_SECONDARY = java.awt.Color.decode("#73708A");
    private final java.awt.Color PRIMARY_PURPLE = java.awt.Color.decode("#7C3AED");
    private final java.awt.Color LIGHT_PURPLE = java.awt.Color.decode("#EDE7FF");
    private final java.awt.Color BORDER_COLOR = java.awt.Color.decode("#E4DAFF");
    private final java.awt.Color CATEGORY_HOVER = java.awt.Color.decode("#F4EEFF");
    private final java.awt.Color WHITE = java.awt.Color.WHITE;

    private MediaPlayer mediaPlayer;
    private boolean isActive = false;
    private int currentVideoIndex = 0;
    private List<String> realReelsData = new ArrayList<>();
    
    // UI Elements for JavaFX
    private Label lblName, lblCaption, lblTags, lblLikes, lblProductLink, lblLocationFX;
    private javafx.scene.image.ImageView imgAvatarFX;
    private MediaView mediaView;
    private ProgressBar pb;
    private Label lblTime, lblPause, lblVolume;
    private Label btnPauseOverlay, btnVolumeOverlay;
    private javafx.scene.control.Slider volumeSlider;
    private double currentVolume = 0.7;
    private JLabel swingLblLikes, swingLblLikeIcon, swingLblCommentCount, pillLblCommentCount;
    private boolean isSceneInitialized = false;
    private boolean isDraggingProgress = false;
    private String currentVideoUrl = null;
    private final java.util.Map<String, Integer> commentReactionsMap = new java.util.concurrent.ConcurrentHashMap<>();
    private javafx.animation.PauseTransition previewDebouncer = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
    private long lastScrollTime = 0;
    private boolean isMuted = false;
    
    private StackPane fxRoot;
    private BorderPane topOverlay;
    private VBox bottomOverlay;
    private javafx.animation.PauseTransition hideTimer;
    private StackPane contentPane;
    
    private JPanel leftCategoryPanel;
    private JPanel centerContent;
    private JPanel videoCardContainer;
    private JFXPanel fxPanel;
    private JFrame fullScreenFrame;
    private boolean isFullscreen = false;
    private JLabel loadingLabel;
    
    private JPanel commentsPanel;
    private JPanel commentsListPanel;
    private JTextField txtComment;
    
    private JPanel bottomContent;
    private boolean isCommentPanelOpen = false;
    private JPanel commentActiveHighlight;
    private boolean isReelLiked = false;
    private static java.util.List<String> recentEmojisReels = new java.util.ArrayList<>();

    public void setActive(boolean active) {
        this.isActive = active;
        Platform.runLater(() -> {
            if (mediaPlayer != null) {
                if (active) {
                    mediaPlayer.play();
                    if (lblPause != null) lblPause.setGraphic(createFXIcon("/images/icon/pause.png", 20));
                if (lblPause != null) lblPause.setText("");
                    if (btnPauseOverlay != null) btnPauseOverlay.setGraphic(createFXIcon("/images/icon/pause.png", 22));
                if (btnPauseOverlay != null) btnPauseOverlay.setText("");
                } else {
                    mediaPlayer.pause();
                    if (lblPause != null) {
                        lblPause.setGraphic(createFXIcon("/images/icon/play.png", 20));
                        lblPause.setText("");
                    }
                    if (btnPauseOverlay != null) {
                        btnPauseOverlay.setGraphic(createFXIcon("/images/icon/play.png", 22));
                        btnPauseOverlay.setText("");
                    }
                }
            }
        });
    }

    public ReelsTabPanel() {
        setLayout(new BorderLayout());
        setBackground(MAIN_BG);

        leftCategoryPanel = createReelsCategoryPanel();
        leftCategoryPanel.setVisible(false); 
        add(leftCategoryPanel, BorderLayout.WEST);

        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(MAIN_BG);
        
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));
        headerPanel.setOpaque(false);
        // Remove left padding completely to push it to the boundary
        headerPanel.setBorder(new EmptyBorder(4, 0, 4, 16));
        
        JPanel btnToggle = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(WHITE);
                g2.fillOval(0,0,getWidth(),getHeight());
                g2.setColor(java.awt.Color.decode("#E2E8F0"));
                g2.drawOval(0,0,getWidth()-1,getHeight()-1);
                g2.dispose();
            }
        };
        btnToggle.setOpaque(false);
        btnToggle.setPreferredSize(new Dimension(28, 28));
        btnToggle.setMaximumSize(new Dimension(28, 28));
        btnToggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JLabel lblToggleIcon = new JLabel("\u00BB", SwingConstants.CENTER);
        lblToggleIcon.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
        lblToggleIcon.setForeground(java.awt.Color.decode("#73708A"));
        btnToggle.add(lblToggleIcon, BorderLayout.CENTER);
        
        btnToggle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean isVis = leftCategoryPanel.isVisible();
                leftCategoryPanel.setVisible(!isVis);
                lblToggleIcon.setText(isVis ? "\u00BB" : "\u00AB");
            }
        });
        
        JLabel lblTitle = new JLabel("Reels");
        lblTitle.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 28));
        lblTitle.setForeground(TEXT_MAIN);
        
        headerPanel.add(btnToggle);
        headerPanel.add(Box.createHorizontalStrut(16));
        headerPanel.add(lblTitle);
        
        mainContent.add(headerPanel, BorderLayout.NORTH);

        centerContent = createReelsContent();
        mainContent.add(centerContent, BorderLayout.CENTER);
        
        add(mainContent, BorderLayout.CENTER);
        
        try { NetworkManager.getInstance().sendPacket(new Packet("GET_REELS", "")); } catch (Exception e) {}
    }

    public static void setLocalIcon(JLabel label, String path, int width, int height) {
        setLocalIcon(label, path, width, height, false);
    }

    public static void setLocalIcon(JLabel label, String path, int width, int height, boolean bold) {
        new Thread(() -> {
            try {
                java.net.URL url = ReelsTabPanel.class.getResource(path);
                if (url != null) {
                    java.awt.Image image = javax.imageio.ImageIO.read(url);
                    if (image != null) {
                        java.awt.Image scaled = image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
                        if (bold) {
                            java.awt.image.BufferedImage bImage = new java.awt.image.BufferedImage(width + 1, height + 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                            Graphics2D g2 = bImage.createGraphics();
                            g2.drawImage(scaled, 0, 0, null);
                            g2.drawImage(scaled, 1, 0, null);
                            g2.drawImage(scaled, 0, 1, null);
                            g2.drawImage(scaled, 1, 1, null);
                            g2.dispose();
                            scaled = bImage;
                        }
                        final java.awt.Image finalScaled = scaled;
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            label.setIcon(new javax.swing.ImageIcon(finalScaled));
                            label.setText("");
                        });
                    }
                }
            } catch (Exception e) {}
        }).start();
    }

    public static void setNetworkIcon(JLabel label, String urlString, int width, int height) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(urlString);
                java.awt.Image image = javax.imageio.ImageIO.read(url);
                if (image != null) {
                    java.awt.Image scaled = image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        label.setIcon(new javax.swing.ImageIcon(scaled));
                        label.setText("");
                    });
                }
            } catch (Exception e) {}
        }).start();
    }

    public void loadReels(List<String> data) {
        if (data != null && !data.isEmpty()) {
            this.realReelsData = data;
            this.currentVideoIndex = 0;
            
            SwingUtilities.invokeLater(() -> {
                updateRecentWatched();
                
                if (loadingLabel != null && videoCardContainer != null) {
                    videoCardContainer.remove(loadingLabel);
                    if (fxPanel == null) {
                        fxPanel = new JFXPanel();
                        fxPanel.setOpaque(false);
                        fxPanel.setBackground(new java.awt.Color(0,0,0,0));
                    }
                    videoCardContainer.add(fxPanel, BorderLayout.CENTER);
                    videoCardContainer.revalidate();
                    videoCardContainer.repaint();
                    
                    Platform.runLater(() -> {
                        String reelData = realReelsData.get(0);
                        String[] parts = reelData.split(";;");
                        String url = parts.length > 1 ? parts[1] : "";
                        if (!url.isEmpty()) {
                            initFXScene(fxPanel, url, parts);
                            
                            // Tải trước 3 video tiếp theo để chuẩn bị lướt
                            int preloadCount = Math.min(3, realReelsData.size());
                            for (int i = 1; i < preloadCount; i++) {
                                String[] p = realReelsData.get(i).split(";;");
                                if (p.length > 1) {
                                    com.mycompany.tutorhub_enterprise.utils.VideoCacheManager.cacheVideo(p[1]);
                                }
                            }
                        }
                    });
                }
            });
        } else {
            SwingUtilities.invokeLater(() -> {
                if (loadingLabel != null) {
                    loadingLabel.setText("Chưa có dữ liệu Reels nào. Hãy tạo mới!");
                }
            });
        }
    }
    
    private void updateRecentWatched() {
        if (bottomContent == null) return;
        bottomContent.removeAll();
        
        JLabel lblRecent = new JLabel("Đã xem gần đây");
        lblRecent.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
        lblRecent.setForeground(TEXT_MAIN);
        lblRecent.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottomContent.add(lblRecent);
        bottomContent.add(Box.createVerticalStrut(16));
        
        int count = Math.min(3, realReelsData.size());
        for (int i = 0; i < count; i++) {
            String[] parts = realReelsData.get(i).split(";;");
            if (parts.length >= 3) {
                String title = parts[2];
                if (title.length() > 25) title = title.substring(0, 25) + "...";
                String thumbnailUrl = "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=200&q=80";
                if(parts.length >= 7 && parts[6] != null && !parts[6].isEmpty() && parts[6].startsWith("http")) {
                    thumbnailUrl = parts[6]; 
                }
                bottomContent.add(createRecentItem(title, "0:30", thumbnailUrl));
                bottomContent.add(Box.createVerticalStrut(12));
            }
        }
        
        if(count > 0) {
            JButton btnMore = new JButton("Xem thêm");
            btnMore.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
            btnMore.setForeground(PRIMARY_PURPLE);
            btnMore.setBackground(WHITE);
            btnMore.setFocusPainted(false);
            btnMore.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                    new EmptyBorder(10, 0, 10, 0)
            ));
            btnMore.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnMore.setAlignmentX(Component.LEFT_ALIGNMENT);
            btnMore.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            bottomContent.add(btnMore);
        }
        
        bottomContent.revalidate();
        bottomContent.repaint();
    }

    private JPanel createReelsCategoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(280, 0));
        panel.setBackground(MAIN_BG);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR));

        JPanel topContent = new JPanel();
        topContent.setLayout(new BoxLayout(topContent, BoxLayout.Y_AXIS));
        topContent.setOpaque(false);
        topContent.setBorder(new EmptyBorder(24, 20, 16, 20));

        topContent.add(createCategoryItem("https://img.icons8.com/fluency-systems-filled/96/7C3AED/fire-element.png", "Dành cho bạn", true));
        topContent.add(Box.createVerticalStrut(6));
        topContent.add(createCategoryItem("https://img.icons8.com/fluency-systems-regular/96/73708A/graduation-cap.png", "Giáo dục & Học tập", false));
        topContent.add(Box.createVerticalStrut(6));
        topContent.add(createCategoryItem("https://img.icons8.com/fluency-systems-regular/96/73708A/idea.png", "Kỹ năng mềm", false));
        topContent.add(Box.createVerticalStrut(6));
        topContent.add(createCategoryItem("https://img.icons8.com/fluency-systems-regular/96/73708A/language.png", "IELTS & Ngoại ngữ", false));
        topContent.add(Box.createVerticalStrut(6));
        topContent.add(createCategoryItem("https://img.icons8.com/fluency-systems-regular/96/73708A/math.png", "Toán học", false));
        topContent.add(Box.createVerticalStrut(6));
        topContent.add(createCategoryItem("https://img.icons8.com/fluency-systems-regular/96/73708A/microscope.png", "Khoa học", false));
        topContent.add(Box.createVerticalStrut(6));
        topContent.add(createCategoryItem("https://img.icons8.com/fluency-systems-regular/96/73708A/monitor.png", "Công nghệ", false));

        panel.add(topContent, BorderLayout.NORTH);

        bottomContent = new JPanel();
        bottomContent.setLayout(new BoxLayout(bottomContent, BoxLayout.Y_AXIS));
        bottomContent.setOpaque(false);
        bottomContent.setBorder(new EmptyBorder(16, 20, 24, 20));
        
        JLabel lblLoadingRecent = new JLabel("Đang tải dữ liệu...");
        lblLoadingRecent.setForeground(TEXT_SECONDARY);
        lblLoadingRecent.setFont(new java.awt.Font("Segoe UI", java.awt.Font.ITALIC, 12));
        bottomContent.add(lblLoadingRecent);

        panel.add(bottomContent, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createCategoryItem(String iconUrl, String title, boolean isActive) {
        JPanel item = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (isActive) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(LIGHT_PURPLE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.setColor(PRIMARY_PURPLE);
                    g2.fillRoundRect(0, 8, 4, getHeight() - 16, 4, 4);
                    g2.dispose();
                }
            }
        };
        item.setOpaque(false);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        item.setPreferredSize(new Dimension(220, 46));
        item.setBorder(new EmptyBorder(0, 16, 0, 16));
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblIcon = new JLabel();
        setNetworkIcon(lblIcon, iconUrl, 20, 20);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new java.awt.Font("Segoe UI", isActive ? java.awt.Font.BOLD : java.awt.Font.PLAIN, 14));
        lblTitle.setForeground(isActive ? PRIMARY_PURPLE : TEXT_SECONDARY);

        item.add(lblIcon, BorderLayout.WEST);
        item.add(lblTitle, BorderLayout.CENTER);

        if (!isActive) {
            item.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) { 
                    item.setOpaque(true); 
                    item.setBackground(CATEGORY_HOVER); 
                    lblTitle.setForeground(PRIMARY_PURPLE); 
                    item.repaint(); 
                }
                @Override
                public void mouseExited(MouseEvent e) { 
                    item.setOpaque(false); 
                    lblTitle.setForeground(TEXT_SECONDARY); 
                    item.repaint(); 
                }
            });
        }
        return item;
    }

    private JPanel createRecentItem(String title, String duration, String thumbnailUrl) {
        JPanel item = new JPanel(new BorderLayout(12, 0));
        item.setOpaque(false);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel thumb = new JPanel(new BorderLayout()) {
            private Image img;
            {
                new Thread(() -> {
                    try { 
                        img = javax.imageio.ImageIO.read(new java.net.URL(thumbnailUrl));
                        if (img != null) {
                            repaint();
                        }
                    } catch(Exception e) {}
                }).start();
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (img != null) {
                    g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                    g2.drawImage(img, 0, 0, getWidth(), getHeight(), this);
                    g2.setClip(null);
                } else {
                    g2.setPaint(new GradientPaint(0, 0, java.awt.Color.decode("#D8B4FE"), getWidth(), getHeight(), java.awt.Color.decode("#C084FC")));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }
                g2.setColor(new java.awt.Color(255, 255, 255, 80));
                g2.fillRect(0, getHeight() - 4, getWidth(), 4);
                g2.setColor(PRIMARY_PURPLE);
                g2.fillRect(0, getHeight() - 4, (int)(getWidth() * 0.7), 4);
                g2.dispose();
            }
        };
        thumb.setOpaque(false);
        thumb.setPreferredSize(new Dimension(100, 60));

        JLabel lblDur = new JLabel(duration);
        lblDur.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 10));
        lblDur.setForeground(java.awt.Color.WHITE);
        lblDur.setBorder(new EmptyBorder(2, 6, 2, 6));
        lblDur.setOpaque(true);
        lblDur.setBackground(new java.awt.Color(0, 0, 0, 120));
        
        JPanel durPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        durPanel.setOpaque(false);
        durPanel.add(lblDur);
        thumb.add(durPanel, BorderLayout.SOUTH);

        JLabel lblTitle = new JLabel("<html><div style='width:90px; font-family:Segoe UI; font-size:13px; font-weight:600; color:#17172F;'>" + title + "</div></html>");
        lblTitle.setVerticalAlignment(SwingConstants.TOP);

        item.add(thumb, BorderLayout.WEST);
        item.add(lblTitle, BorderLayout.CENTER);
        return item;
    }

    private JPanel createReelsContent() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(MAIN_BG);

        GridBagConstraints gbc = new GridBagConstraints();
        
        videoCardContainer = new JPanel(new BorderLayout());
        videoCardContainer.setOpaque(false);
        videoCardContainer.setPreferredSize(new Dimension(1300, 850)); 
        videoCardContainer.setMinimumSize(new Dimension(800, 500));
        videoCardContainer.setBorder(new EmptyBorder(0, 0, 0, 0));
        
        loadingLabel = new JLabel("Đang tải dữ liệu thực...", SwingConstants.CENTER);
        loadingLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 18));
        loadingLabel.setForeground(PRIMARY_PURPLE);
        videoCardContainer.add(loadingLabel, BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new java.awt.Insets(0, 16, 0, 8);
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(videoCardContainer, gbc);

        JPanel actionBar = new JPanel();
        actionBar.setLayout(new BoxLayout(actionBar, BoxLayout.Y_AXIS));
        actionBar.setOpaque(false);
        
        // Upload button
        JPanel btnUpload = createSvgActionIcon("plus-circle", false);
        btnUpload.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnUpload.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { handleUploadReel(); }
        });
        actionBar.add(btnUpload);
        actionBar.add(Box.createVerticalStrut(16));
        
        // Like group
        JPanel btnLike = new JPanel();
        btnLike.setLayout(new BoxLayout(btnLike, BoxLayout.Y_AXIS));
        btnLike.setOpaque(false);
        btnLike.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel btnCircleLike = createSvgActionIcon("heart", false);
        swingLblLikeIcon = (JLabel) btnCircleLike.getComponent(0);
        btnCircleLike.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCircleLike.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { handleLikeReel(); }
        });
        swingLblLikes = new JLabel("0");
        swingLblLikes.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
        swingLblLikes.setForeground(TEXT_MAIN);
        swingLblLikes.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLike.add(btnCircleLike);
        btnLike.add(Box.createVerticalStrut(4));
        btnLike.add(swingLblLikes);
        actionBar.add(btnLike);
        actionBar.add(Box.createVerticalStrut(16));
        
        // Comment group (highlight-able)
        JPanel btnComment = new JPanel();
        btnComment.setLayout(new BoxLayout(btnComment, BoxLayout.Y_AXIS));
        btnComment.setOpaque(false);
        btnComment.setAlignmentX(Component.CENTER_ALIGNMENT);
        commentActiveHighlight = createSvgActionIcon("message-circle", false);
        commentActiveHighlight.setAlignmentX(Component.CENTER_ALIGNMENT);
        commentActiveHighlight.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { toggleComments(); }
        });
        pillLblCommentCount = new JLabel("0");
        pillLblCommentCount.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
        pillLblCommentCount.setForeground(TEXT_MAIN);
        pillLblCommentCount.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnComment.add(commentActiveHighlight);
        btnComment.add(Box.createVerticalStrut(4));
        btnComment.add(pillLblCommentCount);
        actionBar.add(btnComment);
        actionBar.add(Box.createVerticalStrut(16));
        
        // Share
        JPanel btnShare = createSvgActionIcon("send", false);
        btnShare.setAlignmentX(Component.CENTER_ALIGNMENT);
        actionBar.add(btnShare);
        actionBar.add(Box.createVerticalStrut(16));
        
        // Save/Bookmark
        JPanel btnSave = createSvgActionIcon("bookmark", false);
        btnSave.setAlignmentX(Component.CENTER_ALIGNMENT);
        actionBar.add(btnSave);
        actionBar.add(Box.createVerticalStrut(16));
        
        // More
        JPanel btnMore = createSvgActionIcon("more-horizontal", false);
        btnMore.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnMore.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { showMoreMenu(btnMore); }
        });
        actionBar.add(btnMore);

        // Comments Panel - Card-style white rounded panel
        commentsPanel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                // Subtle border
                g2.setColor(java.awt.Color.decode("#E8E0F8"));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 24, 24);
                g2.dispose();
            }
        };
        commentsPanel.setOpaque(false);
        commentsPanel.setPreferredSize(new Dimension(400, 0));
        commentsPanel.setMinimumSize(new Dimension(400, 0));
        commentsPanel.setBorder(new EmptyBorder(0, 8, 0, 0));
        commentsPanel.setVisible(false);
        
        // Card inner
        JPanel commentsCard = new JPanel(new BorderLayout());
        commentsCard.setOpaque(false);
        
        // Header
        JPanel commentsHeader = new JPanel(new BorderLayout());
        commentsHeader.setOpaque(false);
        commentsHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.decode("#F0E8FF")),
            BorderFactory.createEmptyBorder(18, 20, 16, 16)
        ));
        
        swingLblCommentCount = new JLabel("Bình luận (0)");
        swingLblCommentCount.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 18));
        swingLblCommentCount.setForeground(TEXT_MAIN);
        
        // SVG Close button
        JPanel btnClosePanel = new JPanel(new BorderLayout());
        btnClosePanel.setOpaque(false);
        btnClosePanel.setPreferredSize(new Dimension(32, 32));
        btnClosePanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        FlatSVGIcon closeIcon = new FlatSVGIcon("images/icon_svg/x.svg", 20, 20);
        closeIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> TEXT_SECONDARY));
        JLabel lblClose = new JLabel(closeIcon, SwingConstants.CENTER);
        btnClosePanel.add(lblClose, BorderLayout.CENTER);
        btnClosePanel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { toggleComments(); }
        });
        
        commentsHeader.add(swingLblCommentCount, BorderLayout.WEST);
        commentsHeader.add(btnClosePanel, BorderLayout.EAST);
        commentsCard.add(commentsHeader, BorderLayout.NORTH);
        
        // Comment list - dùng BoxLayout với glue để dồn comment lên trên
        commentsListPanel = new JPanel();
        commentsListPanel.setLayout(new BoxLayout(commentsListPanel, BoxLayout.Y_AXIS));
        commentsListPanel.setBackground(WHITE);
        commentsListPanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        
        JScrollPane scrollComments = new JScrollPane(commentsListPanel);
        scrollComments.setBorder(null);
        scrollComments.setOpaque(false);
        scrollComments.getViewport().setOpaque(false);
        scrollComments.getVerticalScrollBar().setUnitIncrement(16);
        scrollComments.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        // Thin modern scrollbar
        scrollComments.getVerticalScrollBar().setPreferredSize(new Dimension(4, 0));
        commentsCard.add(scrollComments, BorderLayout.CENTER);
        
        // Input footer
        JPanel commentInputPanel = new JPanel(new BorderLayout(8, 0));
        commentInputPanel.setOpaque(false);
        commentInputPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, java.awt.Color.decode("#F0E8FF")),
            BorderFactory.createEmptyBorder(14, 16, 16, 16)
        ));
        
        JPanel txtWrapper = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(java.awt.Color.decode("#F8F4FF"));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.dispose();
            }
        };
        txtWrapper.setOpaque(false);
        txtWrapper.setBorder(new EmptyBorder(0, 14, 0, 8));
        
        txtComment = new JTextField();
        txtComment.setOpaque(false);
        txtComment.setBorder(null);
        txtComment.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        txtComment.setForeground(TEXT_MAIN);
        txtComment.putClientProperty("JTextField.placeholderText", "Viết bình luận...");
        txtComment.addActionListener(e -> sendComment());
        
        // Emoji icon
        FlatSVGIcon emojiIcon = new FlatSVGIcon("images/icon_svg/smile.svg", 20, 20);
        emojiIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> TEXT_SECONDARY));
        JLabel lblEmoji = new JLabel(emojiIcon);
        lblEmoji.setBorder(new EmptyBorder(10, 0, 10, 8));
        lblEmoji.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblEmoji.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { showEmojiPicker(lblEmoji); }
        });
        
        txtWrapper.add(txtComment, BorderLayout.CENTER);
        txtWrapper.add(lblEmoji, BorderLayout.EAST);
        
        // Send button
        JPanel btnSendPanel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PRIMARY_PURPLE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        btnSendPanel.setOpaque(false);
        btnSendPanel.setPreferredSize(new Dimension(44, 44));
        btnSendPanel.setMaximumSize(new Dimension(44, 44));
        btnSendPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        FlatSVGIcon sendIcon = new FlatSVGIcon("images/icon_svg/send.svg", 18, 18);
        sendIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> java.awt.Color.WHITE));
        JLabel lblSend = new JLabel(sendIcon, SwingConstants.CENTER);
        btnSendPanel.add(lblSend, BorderLayout.CENTER);
        btnSendPanel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { sendComment(); }
        });
        
        commentInputPanel.add(txtWrapper, BorderLayout.CENTER);
        commentInputPanel.add(btnSendPanel, BorderLayout.EAST);
        commentsCard.add(commentInputPanel, BorderLayout.SOUTH);
        
        commentsPanel.add(commentsCard, BorderLayout.CENTER);

        // Add video margin to reflect rounded corners
        gbc.insets = new java.awt.Insets(24, 24, 24, 12);
        
        // Wrap actionBar in White Pill
        JPanel pillContainer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);
                g2.setColor(new java.awt.Color(0,0,0,15)); // shadow
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 40, 40);
                g2.dispose();
            }
        };
        pillContainer.setOpaque(false);
        pillContainer.setBorder(new EmptyBorder(16, 12, 16, 16)); // inner padding with offset for shadow
        pillContainer.add(actionBar, BorderLayout.CENTER);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.insets = new java.awt.Insets(24, 6, 24, 6);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(pillContainer, gbc);
        
        // Add Up/Down arrows
        JPanel navBar = new JPanel();
        navBar.setLayout(new BoxLayout(navBar, BoxLayout.Y_AXIS));
        navBar.setOpaque(false);
        
        JPanel btnUp = createNavArrowButton(true);
        btnUp.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { changeVideo(-1, fxPanel); } });
        
        JPanel btnDown = createNavArrowButton(false);
        btnDown.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { changeVideo(1, fxPanel); } });

        navBar.add(btnUp);
        navBar.add(Box.createVerticalStrut(12));
        navBar.add(btnDown);

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.insets = new java.awt.Insets(24, 0, 24, 6);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(navBar, gbc);

        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.insets = new java.awt.Insets(0, 0, 0, 0);
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(commentsPanel, gbc);

        return panel;
    }
    
    private JPanel createActionIcon(String path, boolean isLocal) {
        return createSvgActionIcon(path.contains("plus") ? "plus-circle" : path.contains("heart") ? "heart" : path.contains("message") ? "message-circle" : path.contains("share") ? "send" : path.contains("save") ? "bookmark" : "more-horizontal", false);
    }

    private JPanel createSvgActionIcon(String svgName, boolean isActive) {
        java.awt.Color ICON_COLOR = isActive ? PRIMARY_PURPLE : java.awt.Color.decode("#64748B"); // Xám để bớt đậm
        JPanel btnCircle = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (isActive) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(LIGHT_PURPLE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                    g2.dispose();
                }
            }
        };
        btnCircle.setOpaque(false);
        btnCircle.setPreferredSize(new Dimension(54, 54));
        btnCircle.setMaximumSize(new Dimension(54, 54));
        btnCircle.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lblIcon = new JLabel("", SwingConstants.CENTER);
        FlatSVGIcon svgIcon = new FlatSVGIcon("images/icon_svg/" + svgName + ".svg", 28, 28);
        svgIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> ICON_COLOR));
        lblIcon.setIcon(svgIcon);
        btnCircle.add(lblIcon, BorderLayout.CENTER);
        return btnCircle;
    }

    private JPanel createActionButton(String path, String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel btnCircle = createActionIcon(path, true);
        panel.add(btnCircle);
        
        if (title != null && !title.isEmpty()) {
            JLabel lblTitle = new JLabel(title);
            lblTitle.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
            lblTitle.setForeground(TEXT_MAIN);
            lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
            lblTitle.setBorder(new EmptyBorder(4, 0, 0, 0));
            panel.add(lblTitle);
        }

        return panel;
    }
    
    private void showMoreMenu(Component invoker) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(WHITE);
        menu.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        
        JMenuItem item1 = new JMenuItem("🚩 Báo cáo video");
        item1.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        item1.setBackground(WHITE);
        item1.setForeground(java.awt.Color.RED);
        
        JMenuItem item2 = new JMenuItem("🚫 Không quan tâm");
        item2.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        item2.setBackground(WHITE);
        
        menu.add(item1);
        menu.add(item2);
        menu.show(invoker, -40, invoker.getHeight() + 5);
    }


    private JPanel createNavArrowButton(boolean isUp) {
        JPanel btn = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Shadow
                g2.setColor(new java.awt.Color(0, 0, 0, 10));
                g2.fillOval(2, 4, getWidth()-2, getHeight()-2);
                // White fill
                g2.setColor(java.awt.Color.WHITE);
                g2.fillOval(0, 0, getWidth()-1, getHeight()-1);
                // Border
                g2.setColor(java.awt.Color.decode("#E2E8F0"));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawOval(0, 0, getWidth()-2, getHeight()-2);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setPreferredSize(new Dimension(48, 48));
        btn.setMinimumSize(new Dimension(48, 48));
        btn.setMaximumSize(new Dimension(48, 48));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        FlatSVGIcon icon = new FlatSVGIcon(isUp ? "images/icon_svg/arrow-up.svg" : "images/icon_svg/arrow-down.svg", 20, 20);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> java.awt.Color.decode("#64748B")));
        JLabel lbl = new JLabel(icon, SwingConstants.CENTER);
        btn.add(lbl, BorderLayout.CENTER);
        return btn;
    }

    private void toggleComments() {
        isCommentPanelOpen = !isCommentPanelOpen;
        commentsPanel.setVisible(isCommentPanelOpen);
        if (isCommentPanelOpen) {
            fetchComments();
            // Force JavaFX overlay to show when panel is open
            if (topOverlay != null && bottomOverlay != null) {
                javafx.application.Platform.runLater(() -> {
                    topOverlay.setOpacity(1.0);
                    bottomOverlay.setOpacity(1.0);
                });
            }
        }
        // Update comment button highlight
        if (commentActiveHighlight != null) {
            commentActiveHighlight.repaint();
        }
        revalidate();
        repaint();
    }

    private void fetchComments() {
        if (realReelsData.isEmpty()) return;
        String[] parts = realReelsData.get(currentVideoIndex).split(";;");
        if (parts.length > 0) {
            try {
                int reelId = Integer.parseInt(parts[0]);
                NetworkManager.getInstance().sendPacket(new Packet("GET_REEL_COMMENTS", String.valueOf(reelId)));
            } catch (Exception e) {}
        }
    }

    private void sendComment() {
        if (realReelsData.isEmpty() || txtComment.getText().trim().isEmpty()) return;
        String[] parts = realReelsData.get(currentVideoIndex).split(";;");
        if (parts.length > 0) {
            try {
                int reelId = Integer.parseInt(parts[0]);
                String content = txtComment.getText().trim();
                
                SwingUtilities.invokeLater(() -> {
                    if (commentsListPanel != null) {
                        JPanel commentItem = createCommentItem("Tôi", content, "DEFAULT", System.currentTimeMillis());
                        int count = commentsListPanel.getComponentCount();
                        if (count > 0 && commentsListPanel.getComponent(count - 1) instanceof javax.swing.Box.Filler) {
                            commentsListPanel.add(commentItem, count - 1);
                            commentsListPanel.add(Box.createVerticalStrut(6), count - 1);
                        } else {
                            commentsListPanel.add(commentItem);
                            commentsListPanel.add(Box.createVerticalStrut(6));
                        }
                        commentsListPanel.revalidate();
                        commentsListPanel.repaint();
                        if (commentsListPanel.getParent() instanceof JViewport) {
                            JViewport viewport = (JViewport) commentsListPanel.getParent();
                            SwingUtilities.invokeLater(() -> viewport.setViewPosition(new java.awt.Point(0, commentsListPanel.getHeight())));
                        }
                        if (pillLblCommentCount != null) {
                            try {
                                int currentCount = Integer.parseInt(pillLblCommentCount.getText().replaceAll("[^0-9]", ""));
                                pillLblCommentCount.setText(String.valueOf(currentCount + 1));
                            } catch(Exception e) {}
                        }
                    }
                });
                
                NetworkManager.getInstance().sendPacket(new Packet("ADD_REEL_COMMENT", reelId + ";;" + content));
                txtComment.setText("");
            } catch (Exception e) {}
        }
    }

    public void loadComments(List<String> comments) {
        SwingUtilities.invokeLater(() -> {
            if (swingLblCommentCount != null) {
                swingLblCommentCount.setText("Bình luận (" + comments.size() + ")");
            }
            if (pillLblCommentCount != null) {
                pillLblCommentCount.setText(String.valueOf(comments.size()));
            }
            
            if (commentsListPanel != null) {
                commentsListPanel.removeAll();
                for (String c : comments) {
                    String[] parts = c.split(";;");
                    if (parts.length >= 4) {
                        String name = parts[0];
                        String text = parts[1];
                        String avatarB64 = parts[2];
                        long timeMillis = Long.parseLong(parts[3]);
                        
                        JPanel commentItem = createCommentItem(name, text, avatarB64, timeMillis);
                        commentsListPanel.add(commentItem);
                        commentsListPanel.add(Box.createVerticalStrut(6)); // giảm khoảng cách
                    } else if (parts.length >= 2) {
                        JPanel commentItem = createCommentItem(parts[0], parts[1], "DEFAULT", System.currentTimeMillis());
                        commentsListPanel.add(commentItem);
                        commentsListPanel.add(Box.createVerticalStrut(6));
                    }
                }
                // Thêm glue để đẩy comment lên trên
                commentsListPanel.add(Box.createVerticalGlue());
                commentsListPanel.revalidate();
                commentsListPanel.repaint();
                
                // Scroll to bottom
                if (commentsListPanel.getParent() instanceof JViewport) {
                    JViewport viewport = (JViewport) commentsListPanel.getParent();
                    SwingUtilities.invokeLater(() -> {
                        viewport.setViewPosition(new java.awt.Point(0, commentsListPanel.getHeight()));
                    });
                }
            }
        });
    }

    private JPanel createCommentItem(String name, String text, String avatarB64, long timeMillis) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300)); 
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(new EmptyBorder(6, 0, 6, 0));

        // Avatar
        JLabel lblAvatar = new JLabel();
        lblAvatar.setPreferredSize(new Dimension(36, 36));
        lblAvatar.setMinimumSize(new Dimension(36, 36));
        lblAvatar.setMaximumSize(new Dimension(36, 36));
        lblAvatar.setVerticalAlignment(SwingConstants.TOP);
        if (!avatarB64.equals("DEFAULT") && !avatarB64.equals("NO_AVATAR")) {
            try {
                byte[] bytes = java.util.Base64.getDecoder().decode(avatarB64);
                java.awt.Image img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                if (img != null) {
                    lblAvatar.setIcon(new ImageIcon(getCircularImage(img, 36)));
                }
            } catch(Exception e){}
        }
        if (lblAvatar.getIcon() == null) {
            setLocalIcon(lblAvatar, "/images/avatar_placeholder.png", 36, 36);
        }

        // Content Wrapper
        JPanel contentWrapper = new JPanel();
        contentWrapper.setLayout(new BoxLayout(contentWrapper, BoxLayout.Y_AXIS));
        contentWrapper.setOpaque(false);

        // Bubble Panel
        JPanel bubblePanel = new JPanel();
        bubblePanel.setLayout(new BoxLayout(bubblePanel, BoxLayout.Y_AXIS));
        bubblePanel.setOpaque(false);
        
        JPanel bubbleContainer = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(java.awt.Color.decode("#F0F2F5"));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
            }
        };
        bubbleContainer.setOpaque(false);
        bubbleContainer.setBorder(new EmptyBorder(8, 12, 8, 12));

        // Name
        JLabel lblName = new JLabel(name);
        lblName.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
        lblName.setForeground(TEXT_MAIN);
        lblName.setAlignmentX(Component.LEFT_ALIGNMENT);
        bubblePanel.add(lblName);
        bubblePanel.add(Box.createVerticalStrut(2));

        long diff = System.currentTimeMillis() - timeMillis;
        String timeAgo;
        if (diff < 60000) timeAgo = "Vừa xong";
        else if (diff < 3600000) timeAgo = (diff/60000) + " phút";
        else if (diff < 86400000) timeAgo = (diff/3600000) + " giờ";
        else if (diff < 172800000) timeAgo = "Hôm qua";
        else timeAgo = (diff/86400000) + " ngày";
        
        JLabel lblTime = new JLabel(timeAgo);
        lblTime.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        lblTime.setForeground(TEXT_SECONDARY);

        // Text
        javax.swing.JTextPane txtPane = new javax.swing.JTextPane();
        txtPane.setOpaque(false);
        txtPane.setEditable(false);
        txtPane.setBorder(null);
        txtPane.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        javax.swing.text.StyledDocument doc = txtPane.getStyledDocument();
        javax.swing.text.Style normal = txtPane.addStyle("normal", null);
        javax.swing.text.StyleConstants.setForeground(normal, TEXT_MAIN);
        javax.swing.text.StyleConstants.setFontFamily(normal, "Segoe UI");
        javax.swing.text.StyleConstants.setFontSize(normal, 13);
        javax.swing.text.Style mention = txtPane.addStyle("mention", null);
        javax.swing.text.StyleConstants.setForeground(mention, java.awt.Color.decode("#1877F2"));
        javax.swing.text.StyleConstants.setFontFamily(mention, "Segoe UI");
        javax.swing.text.StyleConstants.setFontSize(mention, 13);
        javax.swing.text.StyleConstants.setBold(mention, true);

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(@[\\p{Lu}\\u00C0-\\u1EF8][\\p{L}\\u00E0-\\u1EF9]*(?:\\s+[\\p{Lu}\\u00C0-\\u1EF8][\\p{L}\\u00E0-\\u1EF9]*)*)|(:[a-zA-Z]+_\\d+:)");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        int lastEnd = 0;
        try {
            while (matcher.find()) {
                if (matcher.start() > lastEnd) {
                    doc.insertString(doc.getLength(), text.substring(lastEnd, matcher.start()), normal);
                }
                String match = matcher.group();
                if (match.startsWith("@")) {
                    doc.insertString(doc.getLength(), match, mention);
                } else if (match.startsWith(":") && match.endsWith(":")) {
                    String tag = match.substring(1, match.length() - 1);
                    String[] p = tag.split("_", 2);
                    if (p.length == 2) {
                        String path = "/images/emoji/" + p[0] + "/" + p[0] + " (" + p[1] + ").png";
                        java.net.URL url = getClass().getResource(path);
                        if (url != null) {
                            java.awt.Image img = javax.imageio.ImageIO.read(url);
                            if (img != null) {
                                txtPane.insertIcon(new ImageIcon(img.getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH)));
                            } else {
                                doc.insertString(doc.getLength(), match, normal);
                            }
                        } else {
                            doc.insertString(doc.getLength(), match, normal);
                        }
                    } else {
                        doc.insertString(doc.getLength(), match, normal);
                    }
                }
                lastEnd = matcher.end();
            }
            if (lastEnd < text.length()) {
                doc.insertString(doc.getLength(), text.substring(lastEnd), normal);
            }
        } catch (Exception ex) {
            txtPane.setText(text);
        }
        txtPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        bubblePanel.add(txtPane);
        
        bubbleContainer.add(bubblePanel, BorderLayout.CENTER);
        
        // Wrap bubble in a left-aligned panel
        JPanel bubbleWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bubbleWrap.setOpaque(false);
        bubbleWrap.add(bubbleContainer);
        bubbleWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        contentWrapper.add(bubbleWrap);
        contentWrapper.add(Box.createVerticalStrut(2));

        // Actions row - Facebook style
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Helper function to load scaled GIF via HTML text to preserve animation
        java.util.function.BiFunction<String, Integer, String> getGifHtml = (file, size) -> {
            java.net.URL url = getClass().getResource("/images/reactions/" + file);
            if (url != null) {
                return "<html><img src='" + url.toString() + "' width='" + size + "' height='" + size + "'></html>";
            }
            return "";
        };
        
        String commentKey = name + "_" + timeMillis;
        final int[] reactionState = { commentReactionsMap.getOrDefault(commentKey, 0) };
        
        JLabel lblLikeBtn = new JLabel();
        lblLikeBtn.setText("Thích");
        final String[] REACTION_FILES = {"like.gif", "love.gif", "care.gif", "haha.gif", "wow.gif", "sad.gif", "angry.gif"};
        final String[] REACTION_LABELS = {"Thích", "Yêu thích", "Thương thương", "Hà hà", "Wow", "Buồn", "Phẫn nộ"};
        final java.awt.Color[] REACTION_COLORS = {
            java.awt.Color.decode("#1877F2"),
            java.awt.Color.decode("#F33E58"),
            java.awt.Color.decode("#F7B928"),
            java.awt.Color.decode("#F7B928"),
            java.awt.Color.decode("#F7B928"),
            java.awt.Color.decode("#5B92CA"),
            java.awt.Color.decode("#E05E2E")
        };

        lblLikeBtn.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        lblLikeBtn.setForeground(TEXT_SECONDARY);
        lblLikeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblLikeBtn.setIconTextGap(4);
        
        JLabel lblLikeCount = new JLabel();
        lblLikeCount.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        lblLikeCount.setForeground(TEXT_SECONDARY);
        
        // Restore saved reaction state
        if (reactionState[0] > 0) {
            int rIdx = reactionState[0] - 1;
            lblLikeBtn.setText(getGifHtml.apply(REACTION_FILES[rIdx], 18) + " " + REACTION_LABELS[rIdx]);
            lblLikeBtn.setForeground(REACTION_COLORS[rIdx]);
            lblLikeCount.setText("1"); 
            lblLikeCount.setForeground(REACTION_COLORS[rIdx]);
        }
        
        // Reaction popup
        JPopupMenu reactionPopup = new JPopupMenu();
        reactionPopup.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        reactionPopup.setOpaque(false);
        reactionPopup.setBackground(new java.awt.Color(0, 0, 0, 0));
        
        JPanel reactionBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 8)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(java.awt.Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 32, 32);
                g2.setColor(java.awt.Color.decode("#E4E6EB"));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 32, 32);
                g2.dispose();
            }
        };
        reactionBar.setOpaque(false);
        reactionBar.setBorder(new EmptyBorder(4, 10, 4, 10));
        
        for (int ri = 0; ri < REACTION_FILES.length; ri++) {
            final int idx = ri;
            final String gifFile = REACTION_FILES[ri];
            
            String gifNormal = getGifHtml.apply(gifFile, 38);
            String gifHover = getGifHtml.apply(gifFile, 48);
            
            JLabel reactionBtn = new JLabel(gifNormal);
            reactionBtn.setPreferredSize(new Dimension(52, 60));
            reactionBtn.setHorizontalAlignment(SwingConstants.CENTER);
            reactionBtn.setVerticalAlignment(SwingConstants.CENTER);
            reactionBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            JLabel lblReactionName = new JLabel(REACTION_LABELS[ri], SwingConstants.CENTER);
            lblReactionName.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 10));
            lblReactionName.setForeground(java.awt.Color.decode("#65676B"));
            lblReactionName.setVisible(false);
            
            JPanel reactionCell = new JPanel(new BorderLayout(0, 2));
            reactionCell.setOpaque(false);
            reactionCell.add(reactionBtn, BorderLayout.CENTER);
            reactionCell.add(lblReactionName, BorderLayout.SOUTH);
            
            reactionBtn.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    reactionBtn.setText(gifHover);
                    lblReactionName.setVisible(true);
                    reactionBtn.setPreferredSize(new Dimension(52, 52));
                    reactionCell.revalidate();
                }
                @Override public void mouseExited(MouseEvent e) {
                    reactionBtn.setText(gifNormal);
                    lblReactionName.setVisible(false);
                    reactionBtn.setPreferredSize(new Dimension(52, 60));
                    reactionCell.revalidate();
                }
                @Override public void mouseClicked(MouseEvent e) {
                    reactionPopup.setVisible(false);
                    if (reactionState[0] == idx + 1) {
                        reactionState[0] = 0;
                        commentReactionsMap.put(commentKey, 0);
                        lblLikeBtn.setText(getGifHtml.apply("like.gif", 18) + " Thích");
                        lblLikeBtn.setForeground(TEXT_SECONDARY);
                        int cur = 0;
                        try { cur = Integer.parseInt(lblLikeCount.getText()); } catch(Exception ex){}
                        lblLikeCount.setText(cur > 1 ? String.valueOf(cur - 1) : "");
                        lblLikeCount.setForeground(TEXT_SECONDARY);
                    } else {
                        reactionState[0] = idx + 1;
                        commentReactionsMap.put(commentKey, reactionState[0]);
                        lblLikeBtn.setText(getGifHtml.apply(gifFile, 18) + " " + REACTION_LABELS[idx]);
                        lblLikeBtn.setForeground(REACTION_COLORS[idx]);
                        int cur = 0;
                        try { cur = Integer.parseInt(lblLikeCount.getText()); } catch(Exception ex){}
                        lblLikeCount.setText(String.valueOf(cur == 0 ? 1 : cur + 1));
                        lblLikeCount.setForeground(REACTION_COLORS[idx]);
                    }
                }
            });
            reactionBar.add(reactionCell);
        }
        reactionPopup.add(reactionBar);
        
        // Long-press timer for reactions
        javax.swing.Timer[] longPressTimer = {null};
        lblLikeBtn.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                longPressTimer[0] = new javax.swing.Timer(400, ev -> {
                    longPressTimer[0].stop();
                    reactionPopup.show(lblLikeBtn, 0, -reactionBar.getPreferredSize().height - 14);
                });
                longPressTimer[0].setRepeats(false);
                longPressTimer[0].start();
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (longPressTimer[0] != null && longPressTimer[0].isRunning()) {
                    longPressTimer[0].stop();
                    // Short click = quick like
                    if (reactionState[0] == 0) {
                        reactionState[0] = 1;
                        commentReactionsMap.put(commentKey, 1);
                        lblLikeBtn.setText(getGifHtml.apply("like.gif", 18) + " Thích");
                        lblLikeBtn.setForeground(java.awt.Color.decode("#1877F2"));
                        int cur = 0;
                        try { cur = Integer.parseInt(lblLikeCount.getText()); } catch(Exception ex){}
                        lblLikeCount.setText(String.valueOf(cur == 0 ? 1 : cur + 1));
                        lblLikeCount.setForeground(java.awt.Color.decode("#1877F2"));
                    } else {
                        reactionState[0] = 0;
                        commentReactionsMap.put(commentKey, 0);
                        lblLikeBtn.setText("Thích");
                        lblLikeBtn.setForeground(TEXT_SECONDARY);
                        int cur = 0;
                        try { cur = Integer.parseInt(lblLikeCount.getText()); } catch(Exception ex){}
                        lblLikeCount.setText(cur > 1 ? String.valueOf(cur - 1) : "");
                        lblLikeCount.setForeground(TEXT_SECONDARY);
                    }
                }
            }
            @Override public void mouseExited(MouseEvent e) {
                if (longPressTimer[0] != null) longPressTimer[0].stop();
            }
        });
        
        // Reply button
        JLabel lblReply = new JLabel("Trả lời");
        lblReply.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        lblReply.setForeground(TEXT_SECONDARY);
        lblReply.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblReply.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (txtComment != null) {
                    txtComment.setText("@" + name + " ");
                    txtComment.requestFocus();
                }
            }
            @Override public void mouseEntered(MouseEvent e) { lblReply.setForeground(PRIMARY_PURPLE); }
            @Override public void mouseExited(MouseEvent e) { lblReply.setForeground(TEXT_SECONDARY); }
        });
        
        actions.add(lblTime);
        actions.add(lblLikeBtn);
        actions.add(lblReply);
        
        if (lblLikeCount.getText() != null && !lblLikeCount.getText().isEmpty()) {
            JPanel likeCountPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            likeCountPanel.setOpaque(false);
            JLabel smallLikeIcon = new JLabel(getGifHtml.apply("like.gif", 14));
            likeCountPanel.add(lblLikeCount);
            likeCountPanel.add(smallLikeIcon);
            actions.add(Box.createHorizontalStrut(8));
            actions.add(likeCountPanel);
        }
        
        contentWrapper.add(actions);

        panel.add(lblAvatar, BorderLayout.WEST);
        panel.add(contentWrapper, BorderLayout.CENTER);
        
        return panel;
    }

    private java.awt.image.BufferedImage getCircularImage(java.awt.Image img, int size) {
        java.awt.image.BufferedImage circleBuffer = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = circleBuffer.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.fillOval(0, 0, size, size);
        g2.setComposite(AlphaComposite.SrcIn);
        g2.drawImage(img.getScaledInstance(size, size, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
        g2.dispose();
        return circleBuffer;
    }

    private void handleUploadReel() {
        setActive(false); // Pause current reel video to avoid overlapping audio
        UploadReelDialog dialog = new UploadReelDialog((JFrame) SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        if (dialog.isApproved()) {
            File file = dialog.getSelectedFile();
            String caption = dialog.getCaption();
            String hashtags = dialog.getHashtags();
            if (file == null) return;
            
            // Show minimal loading overlay instead of JOptionPane
            JWindow loadingOverlay = new JWindow((JFrame) SwingUtilities.getWindowAncestor(this));
            JPanel overlayPanel = new JPanel(new GridBagLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(java.awt.Color.WHITE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                    g2.setColor(new java.awt.Color(0xE5E7EB));
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                    g2.dispose();
                }
            };
            overlayPanel.setOpaque(false);
            overlayPanel.setBorder(new javax.swing.border.EmptyBorder(24, 36, 24, 36));
            JLabel lblLoading = new JLabel("⌛  Đang tải video lên...");
            lblLoading.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
            lblLoading.setForeground(new java.awt.Color(0x8B5CF6));
            overlayPanel.add(lblLoading);
            loadingOverlay.setContentPane(overlayPanel);
            loadingOverlay.pack();
            loadingOverlay.setLocationRelativeTo(this);
            loadingOverlay.setOpacity(0.97f);
            loadingOverlay.setVisible(true);
            
            System.out.println("[UPLOAD_REEL] Bắt đầu xử lý nén video...");
            File compressedFile = new File(file.getParentFile(), "compressed_" + System.currentTimeMillis() + "_" + file.getName());
            
            com.mycompany.tutorhub_enterprise.utils.FFmpegUtils.compressVideo(
                file, 
                compressedFile, 
                pct -> {
                    SwingUtilities.invokeLater(() -> {
                        lblLoading.setText("⏳  Đang tối ưu hóa video... " + pct + "%");
                    });
                },
                () -> {
                    // On Complete Compression
                    SwingUtilities.invokeLater(() -> {
                        lblLoading.setText("🚀  Đang chuẩn bị tải lên mây... 0%");
                    });
                    new Thread(() -> {
                        try {
                            System.out.println("[UPLOAD_REEL] Nén xong. Bắt đầu tải lên B2...");
                            String url = uploadToS3Backblaze(compressedFile, pct -> {
                                SwingUtilities.invokeLater(() -> {
                                    lblLoading.setText("🚀  Đang tải lên mây... " + pct + "%");
                                });
                            });
                            System.out.println("[UPLOAD_REEL] B2 URL: " + url);
                            
                            if (url != null) {
                                String loc = dialog.getSelectedLocationText();
                                String prod = dialog.getProductLink();
                                String payload = url + ";;" + caption + ";;" + hashtags + ";;" + loc + ";;" + prod;
                                
                                try {
                                    java.io.File cacheFile = com.mycompany.tutorhub_enterprise.utils.VideoCacheManager.getCachedFile(url);
                                    if (cacheFile != null && compressedFile != null && compressedFile.exists()) {
                                        java.nio.file.Files.copy(compressedFile.toPath(), cacheFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                        System.out.println("[UPLOAD_REEL] Đã tự động lưu cache cho video vừa tải lên để tiết kiệm băng thông!");
                                    }
                                } catch (Exception ignore) {}
                                
                                System.out.println("[UPLOAD_REEL] Gửi UPLOAD_REEL tới Server...");
                                NetworkManager.getInstance().sendPacket(new Packet("UPLOAD_REEL", payload));
                                Thread.sleep(1000); 
                                System.out.println("[UPLOAD_REEL] Gửi GET_REELS để làm mới danh sách...");
                                NetworkManager.getInstance().sendPacket(new Packet("GET_REELS", ""));
                                SwingUtilities.invokeLater(() -> {
                                    loadingOverlay.dispose();
                                    JOptionPane.showMessageDialog(ReelsTabPanel.this, "\uD83C\uDF89  Video đã được đăng tải thành công!", "Thành công", JOptionPane.PLAIN_MESSAGE);
                                    try {
                                        NetworkManager.getInstance().sendPacket(new Packet("GET_REELS", ""));
                                    } catch (Exception e2) {}
                                });
                            } else {
                                System.err.println("[UPLOAD_REEL] Tải lên B2 thất bại!");
                                SwingUtilities.invokeLater(() -> {
                                    loadingOverlay.dispose();
                                    JOptionPane.showMessageDialog(ReelsTabPanel.this, "Tải lên thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                                });
                            }
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                            SwingUtilities.invokeLater(() -> {
                                loadingOverlay.dispose();
                                JOptionPane.showMessageDialog(ReelsTabPanel.this, "Tải lên thất bại: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                            });
                        } finally {
                            if (compressedFile.exists()) {
                                compressedFile.delete();
                            }
                        }
                    }).start();
                },
                (err) -> {
                    // On Error
                    err.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        loadingOverlay.dispose();
                        JOptionPane.showMessageDialog(ReelsTabPanel.this, "Lỗi nén video: " + err.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    });
                    if (compressedFile.exists()) compressedFile.delete();
                }
            );
        }
    }

    private void handleLikeReel() {
        if (realReelsData.isEmpty()) return;
        String reelData = realReelsData.get(currentVideoIndex);
        String[] parts = reelData.split(";;");
        if (parts.length > 4) {
            int reelId = Integer.parseInt(parts[0]);
            isReelLiked = !isReelLiked;
            try {
                NetworkManager.getInstance().sendPacket(new Packet("LIKE_REEL", String.valueOf(reelId)));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            SwingUtilities.invokeLater(() -> {
                if (swingLblLikeIcon != null) {
                    if (isReelLiked) {
                        // Filled heart, fully red
                        FlatSVGIcon filledHeart = new FlatSVGIcon("images/icon_svg/heart-fill.svg", 26, 26);
                        filledHeart.setColorFilter(new FlatSVGIcon.ColorFilter(c -> java.awt.Color.RED));
                        swingLblLikeIcon.setIcon(filledHeart);
                        // Scale animation
                        new Thread(() -> {
                            try {
                                for (int s = 26; s <= 32; s++) {
                                    FlatSVGIcon sc = new FlatSVGIcon("images/icon_svg/heart-fill.svg", s, s);
                                    sc.setColorFilter(new FlatSVGIcon.ColorFilter(c -> java.awt.Color.RED));
                                    final FlatSVGIcon fsc = sc;
                                    SwingUtilities.invokeLater(() -> swingLblLikeIcon.setIcon(fsc));
                                    Thread.sleep(20);
                                }
                                for (int s = 32; s >= 26; s--) {
                                    FlatSVGIcon sc = new FlatSVGIcon("images/icon_svg/heart-fill.svg", s, s);
                                    sc.setColorFilter(new FlatSVGIcon.ColorFilter(c -> java.awt.Color.RED));
                                    final FlatSVGIcon fsc = sc;
                                    SwingUtilities.invokeLater(() -> swingLblLikeIcon.setIcon(fsc));
                                    Thread.sleep(20);
                                }
                            } catch (Exception ex2) {}
                        }).start();
                    } else {
                        FlatSVGIcon outlineHeart = new FlatSVGIcon("images/icon_svg/heart.svg", 26, 26);
                        outlineHeart.setColorFilter(new FlatSVGIcon.ColorFilter(c -> java.awt.Color.decode("#64748B")));
                        swingLblLikeIcon.setIcon(outlineHeart);
                    }
                }
                try {
                    int currentLikes = Integer.parseInt(swingLblLikes.getText());
                    if (isReelLiked) {
                        swingLblLikes.setText(String.valueOf(currentLikes + 1));
                        swingLblLikes.setForeground(java.awt.Color.RED);
                        parts[4] = String.valueOf(currentLikes + 1);
                    } else {
                        swingLblLikes.setText(String.valueOf(Math.max(0, currentLikes - 1)));
                        swingLblLikes.setForeground(TEXT_MAIN);
                        parts[4] = String.valueOf(Math.max(0, currentLikes - 1));
                    }
                    realReelsData.set(currentVideoIndex, String.join(";;", parts));
                } catch(Exception ex) {}
            });
        }
    }

    public String uploadToS3Backblaze(File file, java.util.function.Consumer<Integer> progressCallback) {
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get("d:\\Ban_sao_du_an\\upload_trace.log"), 
                ("[TRACE] Bắt đầu uploadToS3Backblaze\n").getBytes(), 
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);

            if (!B2Helper.isConfigured()) {
                System.err.println("[B2] Missing Backblaze credentials for reels upload.");
                return null;
            }

            String bucketName = B2Helper.getBucketName();
            software.amazon.awssdk.services.s3.S3Client s3 = B2Helper.createS3Client();
                
            java.nio.file.Files.write(java.nio.file.Paths.get("d:\\Ban_sao_du_an\\upload_trace.log"), 
                ("[TRACE] Đã build S3Client\n").getBytes(), 
                java.nio.file.StandardOpenOption.APPEND);
                
            String fileName = "reels/" + System.currentTimeMillis() + "_" + file.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
            
            long fileSize = file.length();
            long partSize = 5 * 1024 * 1024; // 5 MB chunk size
            
            java.nio.file.Files.write(java.nio.file.Paths.get("d:\\Ban_sao_du_an\\upload_trace.log"), 
                ("[TRACE] Kích thước file: " + fileSize + " bytes\n").getBytes(), 
                java.nio.file.StandardOpenOption.APPEND);
            
            if (fileSize < partSize) {
                java.nio.file.Files.write(java.nio.file.Paths.get("d:\\Ban_sao_du_an\\upload_trace.log"), 
                    ("[TRACE] Bắt đầu putObject (<5MB)\n").getBytes(), 
                    java.nio.file.StandardOpenOption.APPEND);
                    
                software.amazon.awssdk.services.s3.model.PutObjectRequest putOb = software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
                    
                s3.putObject(putOb, software.amazon.awssdk.core.sync.RequestBody.fromFile(file));
                
                java.nio.file.Files.write(java.nio.file.Paths.get("d:\\Ban_sao_du_an\\upload_trace.log"), 
                    ("[TRACE] putObject hoàn tất\n").getBytes(), 
                    java.nio.file.StandardOpenOption.APPEND);
                    
                if (progressCallback != null) progressCallback.accept(100);
            } else {
                java.nio.file.Files.write(java.nio.file.Paths.get("d:\\Ban_sao_du_an\\upload_trace.log"), 
                    ("[TRACE] Bắt đầu createMultipartUpload (>5MB)\n").getBytes(), 
                    java.nio.file.StandardOpenOption.APPEND);
                    
                software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse createRes = s3.createMultipartUpload(
                    software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(fileName)
                        .build()
                );
                
                java.nio.file.Files.write(java.nio.file.Paths.get("d:\\Ban_sao_du_an\\upload_trace.log"), 
                    ("[TRACE] createMultipartUpload hoàn tất. UploadID: " + createRes.uploadId() + "\n").getBytes(), 
                    java.nio.file.StandardOpenOption.APPEND);
                    
                String uploadId = createRes.uploadId();
                long totalChunksLong = (fileSize + partSize - 1) / partSize;
                int totalChunks = (int) totalChunksLong;
                java.util.concurrent.atomic.AtomicInteger completedChunks = new java.util.concurrent.atomic.AtomicInteger(0);
                
                java.util.List<software.amazon.awssdk.services.s3.model.CompletedPart> completedParts = new java.util.ArrayList<>();
                java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(16); // Tăng lên 16 luồng song song
                java.util.List<java.util.concurrent.Future<software.amazon.awssdk.services.s3.model.CompletedPart>> futures = new java.util.ArrayList<>();
                
                long filePosition = 0;
                int partNumber = 1;
                
                while (filePosition < fileSize) {
                    long currentPartSize = Math.min(partSize, fileSize - filePosition);
                    final int currentPartNumber = partNumber;
                    final long currentFilePosition = filePosition;
                    final long currentSize = currentPartSize;
                    
                    futures.add(executor.submit(() -> {
                        byte[] buffer = new byte[(int) currentSize];
                        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r")) {
                            raf.seek(currentFilePosition);
                            raf.readFully(buffer);
                        }
                        
                        software.amazon.awssdk.services.s3.model.UploadPartResponse partRes = s3.uploadPart(
                            software.amazon.awssdk.services.s3.model.UploadPartRequest.builder()
                                .bucket(bucketName)
                                .key(fileName)
                                .uploadId(uploadId)
                                .partNumber(currentPartNumber)
                                .build(),
                            software.amazon.awssdk.core.sync.RequestBody.fromBytes(buffer)
                        );
                            
                        System.out.println("[UPLOAD_REEL] Đã tải xong phần " + currentPartNumber);
                        
                        int comp = completedChunks.incrementAndGet();
                        if (progressCallback != null) {
                            int pct = (int) ((comp * 100.0) / totalChunks);
                            progressCallback.accept(pct);
                        }
                            
                        return software.amazon.awssdk.services.s3.model.CompletedPart.builder()
                            .partNumber(currentPartNumber)
                            .eTag(partRes.eTag())
                            .build();
                    }));
                    
                    filePosition += currentPartSize;
                    partNumber++;
                }
                
                for (java.util.concurrent.Future<software.amazon.awssdk.services.s3.model.CompletedPart> f : futures) {
                    completedParts.add(f.get());
                }
                executor.shutdown();
                
                s3.completeMultipartUpload(
                    software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(fileName)
                        .uploadId(uploadId)
                        .multipartUpload(
                            software.amazon.awssdk.services.s3.model.CompletedMultipartUpload.builder()
                                .parts(completedParts)
                                .build()
                        )
                        .build()
                );
                System.out.println("[UPLOAD_REEL] Đã ghép và hoàn tất tải lên!");
            }
            
            String publicUrl = B2Helper.getPublicBaseUrl() + "/" + fileName;
            return publicUrl;
        } catch (Throwable e) {
            try {
                java.nio.file.Files.write(java.nio.file.Paths.get("d:\\Ban_sao_du_an\\upload_trace.log"), 
                    ("[TRACE] LỖI NGHIÊM TRỌNG: " + e.getMessage() + "\n").getBytes(), 
                    java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception ignored) {}
            e.printStackTrace();
            return null;
        }
    }

    private void changeVideo(int direction, JFXPanel fxPanelTarget) {
        if (realReelsData.isEmpty()) return;
        
        currentVideoIndex += direction;
        if (currentVideoIndex < 0) currentVideoIndex = realReelsData.size() - 1;
        if (currentVideoIndex >= realReelsData.size()) currentVideoIndex = 0;
        
        String reelData = realReelsData.get(currentVideoIndex);
        String[] parts = reelData.split(";;");
        String url = parts.length > 1 ? parts[1] : "";
        
        if (commentsPanel != null && commentsPanel.isVisible()) {
            fetchComments();
        }
        
        Platform.runLater(() -> {
            updateFXSceneData(url, parts);
            
            // Tải trước (Preload) 2 video tiếp theo và 1 video trước đó
            int next1 = (currentVideoIndex + 1) % realReelsData.size();
            int next2 = (currentVideoIndex + 2) % realReelsData.size();
            int prev = (currentVideoIndex - 1 < 0) ? realReelsData.size() - 1 : currentVideoIndex - 1;
            
            if (next1 != currentVideoIndex) {
                String[] p = realReelsData.get(next1).split(";;");
                if (p.length > 1) com.mycompany.tutorhub_enterprise.utils.VideoCacheManager.cacheVideo(p[1]);
            }
            if (next2 != currentVideoIndex && next2 != next1) {
                String[] p = realReelsData.get(next2).split(";;");
                if (p.length > 1) com.mycompany.tutorhub_enterprise.utils.VideoCacheManager.cacheVideo(p[1]);
            }
            if (prev != currentVideoIndex) {
                String[] p = realReelsData.get(prev).split(";;");
                if (p.length > 1) com.mycompany.tutorhub_enterprise.utils.VideoCacheManager.cacheVideo(p[1]);
            }
        });
    }

    
    private javafx.scene.image.ImageView createFXIcon(String path, int size) {
        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView();
        try {
            java.net.URL url = ReelsTabPanel.class.getResource(path);
            if (url != null) {
                iv.setImage(new javafx.scene.image.Image(url.toExternalForm(), size, size, true, true));
                javafx.scene.effect.ColorAdjust whiteEffect = new javafx.scene.effect.ColorAdjust();
                whiteEffect.setBrightness(1.0);
                iv.setEffect(whiteEffect);
            }
        } catch (Exception e) {}
        return iv;
    }

    
    private void loadPreviewImage(String url, double seconds, javafx.scene.image.ImageView imgView) {
        // Backblaze B2 không hỗ trợ Cloudinary transformation trên mây.
        // Tạm thời ẩn ảnh preview để tránh lỗi màn hình, video sẽ phát ngay lập tức nhờ Local Cache.
    }

    private void updateFXSceneData(String url, String[] parts) {
        currentVideoUrl = url;
        if(lblName == null || mediaView == null) return;
        
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
        
        try {
            Media media = new Media(com.mycompany.tutorhub_enterprise.utils.VideoCacheManager.getAvailableVideoUrl(url));
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.setMute(isMuted);
            
            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (isDraggingProgress) return;
            javafx.util.Duration total = mediaPlayer.getMedia().getDuration();
            if (total != null && !total.isUnknown() && !total.isIndefinite() && total.toSeconds() > 0 && pb != null && lblTime != null) {
                pb.setProgress(newTime.toSeconds() / total.toSeconds());
                javafx.application.Platform.runLater(() -> {
                    if (lblTime != null) {
                        lblTime.setText(formatTime(newTime) + " / " + formatTime(total));
                    }
                });
            }
        });
        
        mediaPlayer.statusProperty().addListener((obs, oldStatus, newStatus) -> {
            if (hideTimer != null && topOverlay != null && bottomOverlay != null) {
                if (newStatus == MediaPlayer.Status.PLAYING) {
                    hideTimer.playFromStart();
                } else {
                    hideTimer.stop();
                    topOverlay.setOpacity(1.0);
                    bottomOverlay.setOpacity(1.0);
                    contentPane.setCursor(javafx.scene.Cursor.DEFAULT);
                }
            }
        });
        
        mediaPlayer.setOnReady(() -> {
            if (mediaView != null && mediaView.getScene() != null) {
                adjustVideoCover(mediaView, mediaView.getScene());
            }
            if (isActive) {
                mediaPlayer.play();
                if (lblPause != null) lblPause.setGraphic(createFXIcon("/images/icon/pause.png", 28));
                if (btnPauseOverlay != null) btnPauseOverlay.setGraphic(createFXIcon("/images/icon/pause.png", 28));
            }
            if (volumeSlider != null) {
                mediaPlayer.setVolume(isMuted ? 0 : volumeSlider.getValue());
            }
        });
        
            mediaView.setMediaPlayer(mediaPlayer);
        } catch (Exception e) {
            System.err.println("Failed to load video: " + url + " - " + e.getMessage());
            mediaView.setMediaPlayer(null);
        }
        
        if (lblProductLink != null) {
            bottomOverlay.getChildren().remove(lblProductLink);
        }
        if (lblLocationFX != null) {
            bottomOverlay.getChildren().remove(lblLocationFX);
        }
        
        if (parts != null && parts.length >= 7) {
            String baseCaption = parts.length >= 3 ? parts[2] : "";
            String hashtags = parts.length >= 4 ? parts[3] : "";
            String location = parts.length >= 10 ? parts[9] : "";
            String productLink = parts.length >= 11 ? parts[10] : "";

            final String shortCaption = baseCaption.length() > 50 ? baseCaption.substring(0, 50) + "..." : baseCaption;
            final String expandText = " | Xem thêm";
            final String collapseText = " | Thu gọn";
            
            setCaptionWithEmojis(lblCaption, shortCaption + expandText);
            
            lblCaption.setOnMouseClicked(e -> {
                if (lblCaption.getText().endsWith(expandText)) {
                    StringBuilder full = new StringBuilder(baseCaption);
                    if (hashtags != null && !hashtags.isEmpty() && !hashtags.equals("null")) {
                        full.append("\n").append(hashtags);
                    }
                    full.append(collapseText);
                    setCaptionWithEmojis(lblCaption, full.toString());
                    
                    int insertIndex = 2;
                    if (location != null && !location.isEmpty() && !location.equals("null")) {
                        if (lblLocationFX != null && !bottomOverlay.getChildren().contains(lblLocationFX)) {
                            javafx.scene.image.ImageView mapIconFX = createSVGImageView("images/icon/map.svg", 16, 16);
                            if (mapIconFX != null) {
                                lblLocationFX.setGraphic(mapIconFX);
                                lblLocationFX.setText(" " + location);
                            } else {
                                lblLocationFX.setText("📍 " + location);
                            }
                            lblLocationFX.setOnMouseClicked(ev -> {
                                try {
                                    java.awt.Desktop.getDesktop().browse(new java.net.URI("https://www.google.com/maps/search/?api=1&query=" + java.net.URLEncoder.encode(location, "UTF-8")));
                                } catch (Exception ex) { ex.printStackTrace(); }
                            });
                            bottomOverlay.getChildren().add(insertIndex++, lblLocationFX);
                        }
                    }
                    
                    if (productLink != null && !productLink.isEmpty() && !productLink.equals("null") && !productLink.equals("Link sản phẩm (Tùy chọn)...")) {
                        if (lblProductLink != null && !bottomOverlay.getChildren().contains(lblProductLink)) {
                            bottomOverlay.getChildren().add(insertIndex++, lblProductLink);
                            lblProductLink.setOnMouseClicked(ev -> {
                                try {
                                    java.awt.Desktop.getDesktop().browse(new java.net.URI(productLink));
                                } catch (Exception ex) { ex.printStackTrace(); }
                            });
                        }
                    }
                } else {
                    setCaptionWithEmojis(lblCaption, shortCaption + expandText);
                    if (lblProductLink != null) {
                        bottomOverlay.getChildren().remove(lblProductLink);
                    }
                    if (lblLocationFX != null) {
                        bottomOverlay.getChildren().remove(lblLocationFX);
                    }
                }
            });
            
            lblName.setText(parts[5] + " \u2714");
            
            // Avatar: parts[6] là Base64 string từ server
            String avatarB64 = parts[6];
            if (avatarB64 != null && !avatarB64.isEmpty() && !avatarB64.equals("DEFAULT") && imgAvatarFX != null) {
                try {
                    byte[] avatarBytes = java.util.Base64.getDecoder().decode(avatarB64);
                    javafx.scene.image.Image fxImg = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(avatarBytes));
                    imgAvatarFX.setImage(fxImg);
                } catch(Exception e) {
                    imgAvatarFX.setImage(new javafx.scene.image.Image("https://ui-avatars.com/api/?name=User&background=random", true));
                }
            } else if (imgAvatarFX != null) {
                imgAvatarFX.setImage(new javafx.scene.image.Image("https://ui-avatars.com/api/?name=User&background=random", true));
            }
            
            // Like state: parts[7] = 1 nếu user đã like, 0 nếu chưa
            final boolean liked = parts.length >= 8 && "1".equals(parts[7]);
            SwingUtilities.invokeLater(() -> {
                if (swingLblLikes != null) {
                    swingLblLikes.setText(parts[4]);
                    isReelLiked = liked;
                    if (liked) {
                        FlatSVGIcon filledHeart = new FlatSVGIcon("images/icon_svg/heart-fill.svg", 26, 26);
                        filledHeart.setColorFilter(new FlatSVGIcon.ColorFilter(c -> java.awt.Color.RED));
                        swingLblLikeIcon.setIcon(filledHeart);
                        swingLblLikes.setForeground(java.awt.Color.RED);
                    } else {
                        FlatSVGIcon outlineHeart = new FlatSVGIcon("images/icon_svg/heart.svg", 26, 26);
                        outlineHeart.setColorFilter(new FlatSVGIcon.ColorFilter(c -> java.awt.Color.decode("#64748B")));
                        swingLblLikeIcon.setIcon(outlineHeart);
                        swingLblLikes.setForeground(TEXT_MAIN);
                    }
                    if (parts.length >= 9) {
                        if (pillLblCommentCount != null) {
                            pillLblCommentCount.setText(parts[8]);
                        }
                    }
                }
            });
        }
    }

    private String formatTime(javafx.util.Duration d) {
        if (d == null || d.isUnknown() || d.isIndefinite()) return "0:00";
        int s = (int) d.toSeconds();
        return String.format("%d:%02d", s / 60, s % 60);
    }

    private void initFXScene(JFXPanel fxPanelTarget, String videoUrl, String[] parts) {
        currentVideoUrl = videoUrl;
        if (isSceneInitialized) {
            updateFXSceneData(videoUrl, parts);
            return;
        }
        
        try {
            Media media = new Media(com.mycompany.tutorhub_enterprise.utils.VideoCacheManager.getAvailableVideoUrl(videoUrl));
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.setMute(isMuted);
            
            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (isDraggingProgress) return;
            javafx.util.Duration total = mediaPlayer.getMedia().getDuration();
            if (total != null && !total.isUnknown() && !total.isIndefinite() && total.toSeconds() > 0 && pb != null && lblTime != null) {
                pb.setProgress(newTime.toSeconds() / total.toSeconds());
                javafx.application.Platform.runLater(() -> {
                    if (lblTime != null) {
                        lblTime.setText(formatTime(newTime) + " / " + formatTime(total));
                    }
                });
            }
        });
        
        mediaPlayer.statusProperty().addListener((obs, oldStatus, newStatus) -> {
            if (hideTimer != null && topOverlay != null && bottomOverlay != null) {
                if (newStatus == MediaPlayer.Status.PLAYING) {
                    hideTimer.playFromStart();
                } else {
                    hideTimer.stop();
                    topOverlay.setOpacity(1.0);
                    bottomOverlay.setOpacity(1.0);
                    contentPane.setCursor(javafx.scene.Cursor.DEFAULT);
                }
            }
        });

        mediaPlayer.setOnReady(() -> {
            if (mediaView != null && mediaView.getScene() != null) {
                adjustVideoCover(mediaView, mediaView.getScene());
            }
            if (isActive) mediaPlayer.play();
        });

            mediaView = new MediaView(mediaPlayer);
        } catch (Exception e) {
            System.err.println("Failed to load video item: " + videoUrl + " - " + e.getMessage());
            mediaView = new MediaView();
        }
        mediaView.setPreserveRatio(true);

        topOverlay = new BorderPane();
        topOverlay.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        topOverlay.setPadding(new Insets(16, 16, 40, 16));
        topOverlay.setPickOnBounds(false);
        topOverlay.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(0,0,0,0.38), transparent);");
        
        HBox topLeft = new HBox(10);
        topLeft.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        topLeft.setFillHeight(false);
        topLeft.setPickOnBounds(false);
        
        btnPauseOverlay = new Label();
        btnPauseOverlay.setGraphic(createFXIcon("/images/icon/pause.png", 22));
        btnPauseOverlay.setAlignment(Pos.CENTER);
        btnPauseOverlay.setMinSize(44, 44);
        btnPauseOverlay.setMaxSize(44, 44);
        btnPauseOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.45); -fx-background-radius: 999px; -fx-cursor: hand; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 999px;");
        btnPauseOverlay.setOnMouseEntered(e -> btnPauseOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.62); -fx-background-radius: 999px; -fx-cursor: hand; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 999px;"));
        btnPauseOverlay.setOnMouseExited(e -> btnPauseOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.45); -fx-background-radius: 999px; -fx-cursor: hand; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 999px;"));
        btnPauseOverlay.setOnMousePressed(e -> { btnPauseOverlay.setScaleX(0.94); btnPauseOverlay.setScaleY(0.94); });
        btnPauseOverlay.setOnMouseReleased(e -> { btnPauseOverlay.setScaleX(1.0); btnPauseOverlay.setScaleY(1.0); });
        
        btnPauseOverlay.setOnMouseClicked(e -> {
            if (mediaPlayer != null) {
                if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.pause();
                    btnPauseOverlay.setGraphic(createFXIcon("/images/icon/play.png", 22));
                    if (lblPause != null) lblPause.setGraphic(createFXIcon("/images/icon/play.png", 20));
                } else {
                    mediaPlayer.play();
                    btnPauseOverlay.setGraphic(createFXIcon("/images/icon/pause.png", 22));
                    if (lblPause != null) lblPause.setGraphic(createFXIcon("/images/icon/pause.png", 20));
                }
            }
            e.consume();
        });
        
        btnVolumeOverlay = new Label();
        btnVolumeOverlay.setGraphic(createFXIcon(isMuted ? "/images/icon/volumeoff.png" : "/images/icon/volumeon.png", 22));
        btnVolumeOverlay.setAlignment(Pos.CENTER);
        btnVolumeOverlay.setMinSize(44, 44);
        btnVolumeOverlay.setMaxSize(44, 44);
        btnVolumeOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.45); -fx-background-radius: 999px; -fx-cursor: hand; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 999px;");
        btnVolumeOverlay.setOnMouseEntered(e -> btnVolumeOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.62); -fx-background-radius: 999px; -fx-cursor: hand; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 999px;"));
        btnVolumeOverlay.setOnMouseExited(e -> btnVolumeOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.45); -fx-background-radius: 999px; -fx-cursor: hand; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 999px;"));
        btnVolumeOverlay.setOnMousePressed(e -> { btnVolumeOverlay.setScaleX(0.94); btnVolumeOverlay.setScaleY(0.94); });
        btnVolumeOverlay.setOnMouseReleased(e -> { btnVolumeOverlay.setScaleX(1.0); btnVolumeOverlay.setScaleY(1.0); });

        btnVolumeOverlay.setOnMouseClicked(e -> {
            isMuted = !isMuted;
            if (mediaPlayer != null) {
                mediaPlayer.setMute(isMuted);
            }
            btnVolumeOverlay.setGraphic(createFXIcon(isMuted ? "/images/icon/volumeoff.png" : "/images/icon/volumeon.png", 22));
            e.consume();
        });
        
        topLeft.getChildren().addAll(btnPauseOverlay, btnVolumeOverlay);
        
        Label btnSearch = new Label();
        btnSearch.setGraphic(createFXIcon("/images/icon/search.png", 22));
        btnSearch.setAlignment(Pos.CENTER);
        btnSearch.setMinSize(44, 44);
        btnSearch.setMaxSize(44, 44);
        btnSearch.setStyle("-fx-background-color: rgba(0,0,0,0.45); -fx-background-radius: 999px; -fx-cursor: hand; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 999px;");
        btnSearch.setOnMouseEntered(e -> btnSearch.setStyle("-fx-background-color: rgba(0,0,0,0.62); -fx-background-radius: 999px; -fx-cursor: hand; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 999px;"));
        btnSearch.setOnMouseExited(e -> btnSearch.setStyle("-fx-background-color: rgba(0,0,0,0.45); -fx-background-radius: 999px; -fx-cursor: hand; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 999px;"));
        btnSearch.setOnMousePressed(e -> { btnSearch.setScaleX(0.94); btnSearch.setScaleY(0.94); });
        btnSearch.setOnMouseReleased(e -> { btnSearch.setScaleX(1.0); btnSearch.setScaleY(1.0); });
        
        topOverlay.setLeft(topLeft);
        topOverlay.setRight(btnSearch);

        bottomOverlay = new VBox(8);
        bottomOverlay.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        bottomOverlay.setMinWidth(0);
        bottomOverlay.setPickOnBounds(false);
        bottomOverlay.setPadding(new Insets(40, 24, 24, 24));
        bottomOverlay.setAlignment(Pos.BOTTOM_LEFT);
        bottomOverlay.setStyle("-fx-background-color: linear-gradient(to top, rgba(0,0,0,0.9) 0%, rgba(0,0,0,0) 100%);");
        
        HBox authorRow = new HBox(12);
        authorRow.setAlignment(Pos.CENTER_LEFT);
        
        StackPane avatarPane = new StackPane();
        avatarPane.setStyle("-fx-background-color: white; -fx-background-radius: 20px; -fx-padding: 2;");
        javafx.scene.shape.Circle clipCircle = new javafx.scene.shape.Circle(18, 18, 18);
        
        imgAvatarFX = new javafx.scene.image.ImageView();
        imgAvatarFX.setFitWidth(36);
        imgAvatarFX.setFitHeight(36);
        imgAvatarFX.setClip(clipCircle);
        if (parts != null && parts.length >= 7 && parts[6] != null && !parts[6].isEmpty() && !parts[6].equals("DEFAULT")) {
            try {
                byte[] avatarBytes = java.util.Base64.getDecoder().decode(parts[6]);
                imgAvatarFX.setImage(new javafx.scene.image.Image(new java.io.ByteArrayInputStream(avatarBytes)));
            } catch(Exception e) {
                imgAvatarFX.setImage(new javafx.scene.image.Image("https://ui-avatars.com/api/?name=User&background=random", true));
            }
        } else {
            imgAvatarFX.setImage(new javafx.scene.image.Image("https://ui-avatars.com/api/?name=User&background=random", true));
        }
        avatarPane.getChildren().add(imgAvatarFX);
        
        lblName = new Label((parts != null && parts.length >= 6 ? parts[5] : "TutorHub") + " \u2714");
        lblName.setFont(Font.font("Segoe UI Emoji", FontWeight.BOLD, 16));
        lblName.setTextFill(Color.WHITE);
        lblName.setMinWidth(0);
        
        Label btnFollow = new Label("+ Theo dõi");
        btnFollow.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        btnFollow.setTextFill(Color.WHITE);
        btnFollow.setMinWidth(Region.USE_PREF_SIZE);
        btnFollow.setStyle("-fx-border-color: rgba(255,255,255,0.6); -fx-border-radius: 12px; -fx-padding: 4 12 4 12; -fx-cursor: hand;");
        
        authorRow.getChildren().addAll(avatarPane, lblName, btnFollow);

        lblCaption = new Label((parts != null && parts.length >= 3 ? parts[2] : "") + " | Xem thêm");
        lblCaption.setFont(Font.font("Segoe UI Emoji", 14));
        lblCaption.setTextFill(Color.WHITE);
        lblCaption.setMinWidth(0);
        lblCaption.setWrapText(true);
        lblCaption.setCursor(javafx.scene.Cursor.HAND);

        lblProductLink = new Label("🛒 Mua ngay (Click để xem)");
        lblProductLink.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lblProductLink.setTextFill(Color.WHITE);
        lblProductLink.setStyle("-fx-background-color: #EE4D2D; -fx-padding: 8 16 8 16; -fx-background-radius: 8px;");
        lblProductLink.setCursor(javafx.scene.Cursor.HAND);
        lblProductLink.setWrapText(true);

        lblLocationFX = new Label();
        lblLocationFX.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblLocationFX.setTextFill(Color.WHITE);
        lblLocationFX.setCursor(javafx.scene.Cursor.HAND);
        lblLocationFX.setWrapText(true);

        HBox progressRow = new HBox(16);
        progressRow.setAlignment(Pos.CENTER_LEFT);
        progressRow.setPadding(new Insets(12, 0, 0, 0));
        progressRow.setPickOnBounds(false);
        
        lblPause = new Label();
        lblPause.setGraphic(createFXIcon("/images/icon/pause.png", 28));
        lblPause.setCursor(javafx.scene.Cursor.HAND);
        lblPause.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        lblPause.setOnMouseClicked(e -> {
            if (mediaPlayer != null) {
                if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.pause();
                    lblPause.setGraphic(createFXIcon("/images/icon/play.png", 28));
                    if (btnPauseOverlay != null) btnPauseOverlay.setGraphic(createFXIcon("/images/icon/play.png", 24));
                } else {
                    mediaPlayer.play();
                    lblPause.setGraphic(createFXIcon("/images/icon/pause.png", 28));
                    if (btnPauseOverlay != null) btnPauseOverlay.setGraphic(createFXIcon("/images/icon/pause.png", 24));
                }
            }
        });
        
        // Wrapper for ProgressBar and Thumb
        StackPane pbWrapper = new StackPane();
        pbWrapper.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(pbWrapper, Priority.ALWAYS);
        pbWrapper.setMaxWidth(Double.MAX_VALUE);
        pbWrapper.setCursor(javafx.scene.Cursor.HAND);

        pb = new ProgressBar(0);
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setPrefHeight(5);
        pb.setStyle(
            "-fx-accent: white;" +
            "-fx-control-inner-background: rgba(255,255,255,0.35);" +
            "-fx-background-color: rgba(255,255,255,0.35);" +
            "-fx-background-radius: 4px;" +
            "-fx-padding: 0;"
        );
        pb.setMouseTransparent(true); // Let pbWrapper handle mouse
        
        // Custom painted track: thiết kế thu�™ng thanh chạy rieng
        javafx.scene.canvas.Canvas pbCanvas = new javafx.scene.canvas.Canvas();
        pbCanvas.setMouseTransparent(true);
        pbCanvas.widthProperty().bind(pbWrapper.widthProperty());
        pbCanvas.heightProperty().set(6);
        // Bind drawing to progress updates
        pb.progressProperty().addListener((obs, ov, nv) -> {
            double w = pbCanvas.getWidth();
            double h = pbCanvas.getHeight();
            double prog = nv.doubleValue();
            javafx.scene.canvas.GraphicsContext gc = pbCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, w, h);
            // Track background (gray)
            gc.setFill(javafx.scene.paint.Color.rgb(255, 255, 255, 0.30));
            gc.fillRoundRect(0, 1, w, 4, 4, 4);
            // Played portion (white)
            gc.setFill(javafx.scene.paint.Color.WHITE);
            gc.fillRoundRect(0, 1, w * Math.max(0, Math.min(1, prog)), 4, 4, 4);
        });
        pb.setVisible(false); // Hide original, use canvas instead
        
        javafx.scene.shape.Circle pbThumb = new javafx.scene.shape.Circle(7, Color.WHITE);
        pbThumb.setMouseTransparent(true);
        pbThumb.setVisible(false); // Only visible on hover
        pbThumb.translateXProperty().bind(
            pb.progressProperty().multiply(pbWrapper.widthProperty().subtract(14)) // 14 is diameter
        );
        
        pbWrapper.getChildren().addAll(pb, pbCanvas, pbThumb);
        pbWrapper.setMinWidth(10);
        
        // Preview Popup
        StackPane previewPopup = new StackPane();
        previewPopup.setVisible(false);
        previewPopup.setMouseTransparent(true);
        
        javafx.scene.image.ImageView previewImg = new javafx.scene.image.ImageView();
        previewImg.setFitWidth(160);
        previewImg.setFitHeight(90);
        previewImg.setPreserveRatio(true);
        previewImg.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 0);");
        
        Label lblPreviewTime = new Label("0:00");
        lblPreviewTime.setTextFill(Color.WHITE);
        lblPreviewTime.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblPreviewTime.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 2 4; -fx-background-radius: 4;");
        StackPane.setAlignment(lblPreviewTime, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(lblPreviewTime, new Insets(0, 4, 4, 0));
        
        previewPopup.getChildren().addAll(previewImg, lblPreviewTime);
        StackPane.setAlignment(previewPopup, Pos.BOTTOM_LEFT);
        StackPane.setMargin(previewPopup, new Insets(0, 0, 90, 24)); // Position above bottomOverlay
        
        javafx.event.EventHandler<javafx.scene.input.MouseEvent> updatePreview = e -> {
            if (mediaPlayer != null) {
                javafx.util.Duration total = mediaPlayer.getMedia().getDuration();
                if (total != null && !total.isUnknown() && !total.isIndefinite()) {
                    double percent = Math.max(0, Math.min(1, e.getX() / pbWrapper.getWidth()));
                    double timeSeconds = total.toSeconds() * percent;
                    lblPreviewTime.setText(formatTime(javafx.util.Duration.seconds(timeSeconds)));
                    
                    double popupX = e.getX() - 80;
                    if (popupX < 0) popupX = 0;
                    if (popupX > pbWrapper.getWidth() - 160) popupX = pbWrapper.getWidth() - 160;
                    previewPopup.setTranslateX(popupX);
                    
                    if (currentVideoUrl != null && currentVideoUrl.contains("res.cloudinary.com")) {
                        loadPreviewImage(currentVideoUrl, timeSeconds, previewImg);
                    } else {
                        previewImg.setImage(null);
                    }
                }
            }
        };

        javafx.event.EventHandler<javafx.scene.input.MouseEvent> seekHandler = e -> {
            if (mediaPlayer != null) {
                javafx.util.Duration total = mediaPlayer.getMedia().getDuration();
                if (total != null && !total.isUnknown() && !total.isIndefinite()) {
                    double percent = Math.max(0, Math.min(1, e.getX() / pbWrapper.getWidth()));
                    pb.setProgress(percent);
                    lblTime.setText(formatTime(total.multiply(percent)) + " / " + formatTime(total));
                    mediaPlayer.seek(total.multiply(percent));
                }
            }
        };
        
        pbWrapper.setOnMousePressed(e -> { isDraggingProgress = true; seekHandler.handle(e); updatePreview.handle(e); });
        pbWrapper.setOnMouseDragged(e -> { isDraggingProgress = true; seekHandler.handle(e); updatePreview.handle(e); });
        pbWrapper.setOnMouseReleased(e -> { 
            isDraggingProgress = false; 
            if (!pbWrapper.isHover()) { 
                pbCanvas.heightProperty().set(4);
                pbThumb.setVisible(false); 
                previewPopup.setVisible(false); 
            }
        });
        
        pbWrapper.setOnMouseEntered(e -> { 
            pbCanvas.heightProperty().set(7);
            pbThumb.setVisible(true); 
            previewPopup.setVisible(true); 
            updatePreview.handle(e); 
        });
        pbWrapper.setOnMouseExited(e -> { 
            if(!isDraggingProgress) { 
                pbCanvas.heightProperty().set(4);
                pbThumb.setVisible(false); 
                previewPopup.setVisible(false); 
            }
        });
        pbWrapper.setOnMouseMoved(updatePreview);
        
        
        lblTime = new Label("0:00 / 0:00");
        lblTime.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblTime.setTextFill(Color.WHITE);
        lblTime.setMinSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
        
        Label btnFullscreen = new Label("\u26F6"); // Fullscreen symbol
        btnFullscreen.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        btnFullscreen.setTextFill(Color.WHITE);
        btnFullscreen.setCursor(javafx.scene.Cursor.HAND);
        btnFullscreen.setMinWidth(Region.USE_PREF_SIZE);
        
        btnFullscreen.setOnMouseClicked(e -> {
            SwingUtilities.invokeLater(() -> {
                if (!isFullscreen) {
                    fullScreenFrame = new JFrame();
                    fullScreenFrame.setUndecorated(true);
                    fullScreenFrame.setLayout(new BorderLayout());
                    fullScreenFrame.getContentPane().setBackground(java.awt.Color.BLACK);
                    
                    videoCardContainer.remove(fxPanel);
                    fullScreenFrame.add(fxPanel, BorderLayout.CENTER);
                    
                    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    GraphicsDevice gd = ge.getDefaultScreenDevice();
                    if (gd.isFullScreenSupported()) {
                        gd.setFullScreenWindow(fullScreenFrame);
                    } else {
                        fullScreenFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                        fullScreenFrame.setVisible(true);
                    }
                    isFullscreen = true;
                    
                    fullScreenFrame.addKeyListener(new java.awt.event.KeyAdapter() {
                        @Override
                        public void keyPressed(java.awt.event.KeyEvent ke) {
                            if (ke.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                                gd.setFullScreenWindow(null);
                                fullScreenFrame.dispose();
                                videoCardContainer.add(fxPanel, BorderLayout.CENTER);
                                videoCardContainer.revalidate();
                                videoCardContainer.repaint();
                                isFullscreen = false;
                            }
                        }
                    });
                } else {
                    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    GraphicsDevice gd = ge.getDefaultScreenDevice();
                    gd.setFullScreenWindow(null);
                    fullScreenFrame.dispose();
                    videoCardContainer.add(fxPanel, BorderLayout.CENTER);
                    videoCardContainer.revalidate();
                    videoCardContainer.repaint();
                    isFullscreen = false;
                }
            });
        });
        
        progressRow.getChildren().addAll(lblPause, pbWrapper, lblTime, btnFullscreen);

        bottomOverlay.getChildren().addAll(authorRow, lblCaption, progressRow);

        VBox searchPopup = new VBox(10);
        searchPopup.setVisible(false);
        searchPopup.setStyle("-fx-background-color: rgba(20, 20, 24, 0.88); -fx-background-radius: 18px; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 18px; -fx-padding: 14px;");
        searchPopup.setMaxSize(320, 120);
        
        HBox searchInputBox = new HBox(8);
        searchInputBox.setAlignment(Pos.CENTER_LEFT);
        searchInputBox.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 12px; -fx-padding: 8px;");
        Label searchIconSmall = new Label();
        searchIconSmall.setGraphic(createFXIcon("/images/icon/search.png", 16));
        javafx.scene.control.TextField searchInput = new javafx.scene.control.TextField();
        searchInput.setPromptText("Tìm video, gia sư, chủ Ä‘ề...");
        searchInput.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-prompt-text-fill: gray;");
        HBox.setHgrow(searchInput, Priority.ALWAYS);
        Label searchClose = new Label("âœ•");
        searchClose.setTextFill(Color.WHITE);
        searchClose.setCursor(javafx.scene.Cursor.HAND);
        searchClose.setOnMouseClicked(e -> { searchPopup.setVisible(false); e.consume(); });
        searchInputBox.getChildren().addAll(searchIconSmall, searchInput, searchClose);
        
        Label searchHints = new Label("Gợi ý: #Toán, #Lý, #Hoá...");
        searchHints.setTextFill(Color.GRAY);
        searchHints.setFont(Font.font("Segoe UI", 12));
        
        searchPopup.getChildren().addAll(searchInputBox, searchHints);
        
        StackPane.setAlignment(searchPopup, Pos.TOP_RIGHT);
        StackPane.setMargin(searchPopup, new Insets(62, 16, 0, 0));
        
        btnSearch.setOnMouseClicked(e -> {
            searchPopup.setVisible(!searchPopup.isVisible());
            if (searchPopup.isVisible()) searchInput.requestFocus();
            e.consume();
        });

        contentPane = new StackPane();
        contentPane.setStyle("-fx-background-color: black;");
        javafx.scene.shape.Rectangle contentClip = new javafx.scene.shape.Rectangle();
        contentClip.widthProperty().bind(contentPane.widthProperty());
        contentClip.heightProperty().bind(contentPane.heightProperty());
        contentClip.setArcWidth(32);
        contentClip.setArcHeight(32);
        contentPane.setClip(contentClip);
        contentPane.getChildren().addAll(mediaView, topOverlay, bottomOverlay, previewPopup, searchPopup);
        topOverlay.maxWidthProperty().bind(contentPane.widthProperty());
        bottomOverlay.maxWidthProperty().bind(contentPane.widthProperty());
        StackPane.setAlignment(topOverlay, Pos.TOP_CENTER);
        StackPane.setAlignment(bottomOverlay, Pos.BOTTOM_CENTER);
        
        mediaView.setOnMouseClicked(e -> {
            if (mediaPlayer != null) {
                if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.pause();
                    btnPauseOverlay.setGraphic(createFXIcon("/images/icon/play.png", 28));
                    if (lblPause != null) lblPause.setGraphic(createFXIcon("/images/icon/play.png", 28));
                } else {
                    mediaPlayer.play();
                    btnPauseOverlay.setGraphic(createFXIcon("/images/icon/pause.png", 28));
                    if (lblPause != null) lblPause.setGraphic(createFXIcon("/images/icon/pause.png", 28));
                }
            }
        });
        
        hideTimer = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(9));
        hideTimer.setOnFinished(e -> {
            if (isCommentPanelOpen) return;
            if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                javafx.animation.FadeTransition ft1 = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), topOverlay);
                ft1.setToValue(0); ft1.play();
                javafx.animation.FadeTransition ft2 = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), bottomOverlay);
                ft2.setToValue(0); ft2.play();
                contentPane.setCursor(javafx.scene.Cursor.NONE);
            }
        });

        contentPane.setOnMouseMoved(e -> {
            topOverlay.setOpacity(1.0);
            bottomOverlay.setOpacity(1.0);
            contentPane.setCursor(javafx.scene.Cursor.DEFAULT);
            if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                hideTimer.playFromStart();
            }
        });

        contentPane.setOnMouseExited(e -> {
            if (isCommentPanelOpen) return;
            if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                javafx.animation.FadeTransition ft1 = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), topOverlay);
                ft1.setToValue(0); ft1.play();
                javafx.animation.FadeTransition ft2 = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), bottomOverlay);
                ft2.setToValue(0); ft2.play();
            }
        });

        fxRoot = new StackPane();
        fxRoot.setStyle("-fx-background-color: transparent;");
        fxRoot.getChildren().add(contentPane);

        Scene scene = new Scene(fxRoot, Color.TRANSPARENT);
        
        scene.setOnScroll(e -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastScrollTime > 800) {
                if (e.getDeltaY() < 0) changeVideo(1, fxPanelTarget);
                else changeVideo(-1, fxPanelTarget);
                lastScrollTime = currentTime;
            }
        });
        
        fxPanelTarget.setScene(scene);
        
        scene.widthProperty().addListener((obs, oldV, newV) -> adjustVideoCover(mediaView, scene));
        scene.heightProperty().addListener((obs, oldV, newV) -> adjustVideoCover(mediaView, scene));
        
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(scene.widthProperty());
        clip.heightProperty().bind(scene.heightProperty());
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        fxRoot.setClip(clip);
        
        isSceneInitialized = true;
    }

    public void stopVideo() {
        if (mediaPlayer != null) {
            Platform.runLater(() -> mediaPlayer.stop());
        }
    }

    private void adjustVideoCover(MediaView mediaView, Scene scene) {
        if (mediaPlayer == null || mediaPlayer.getMedia() == null || mediaView == null || scene == null || contentPane == null) return;
        double videoW = mediaPlayer.getMedia().getWidth();
        double videoH = mediaPlayer.getMedia().getHeight();
        if (videoW == 0 || videoH == 0) return;
        double sceneW = scene.getWidth();
        double sceneH = scene.getHeight();
        if (sceneW == 0 || sceneH == 0) return;
        
        double scaleX = sceneW / videoW;
        double scaleY = sceneH / videoH;
        double scale = Math.min(scaleX, scaleY);
        
        double actualW = videoW * scale;
        double actualH = videoH * scale;
        
        contentPane.setMaxSize(actualW, actualH);
        contentPane.setMinSize(actualW, actualH);
        contentPane.setPrefSize(actualW, actualH);
        
        mediaView.setFitWidth(actualW);
        mediaView.setFitHeight(actualH);
        mediaView.setTranslateX(0);
        mediaView.setTranslateY(0);
    }

    // ===== EMOJI PICKER (shared with Reels comment panel) =====
    class EmojiCategory {
        String title, prefix, navIconUrl;
        public EmojiCategory(String title, String prefix, String navIconUrl) { 
            this.title = title; this.prefix = prefix; this.navIconUrl = navIconUrl; 
        }
    }

    private void showEmojiPicker(Component invoker) {
        JPopupMenu emojiPopup = new JPopupMenu();
        emojiPopup.setBackground(java.awt.Color.WHITE);
        emojiPopup.setBorder(BorderFactory.createLineBorder(java.awt.Color.decode("#E5E7EB"), 1));

        EmojiCategory[] categories = {
            new EmojiCategory("Cảm xúc", "mat", ""),
            new EmojiCategory("Cử ch�‰", "cuchi", ""),
            new EmojiCategory("Con người", "connguoi", ""),
            new EmojiCategory("Tự nhiên", "tunhien", ""),
            new EmojiCategory("Con vật", "convat", ""),
            new EmojiCategory("Trang trí", "trangtri", ""),
            new EmojiCategory("Ä‚n u�‘ng", "anuong", ""),
            new EmojiCategory("Trò chơi", "trochoi", ""),
            new EmojiCategory("Công cụ", "congcu", ""),
            new EmojiCategory("Cờ", "co", "")
        };

        JPanel mainScrollContent = new JPanel();
        mainScrollContent.setLayout(new BoxLayout(mainScrollContent, BoxLayout.Y_AXIS));
        mainScrollContent.setBackground(java.awt.Color.WHITE);

        if (!recentEmojisReels.isEmpty()) {
            mainScrollContent.add(createReelsEmojiSectionTitle("Gần Ä‘ây"));
            JPanel recentGrid = new JPanel(new java.awt.GridLayout(0, 8, 2, 2));
            recentGrid.setBackground(java.awt.Color.WHITE);
            for (String tag : recentEmojisReels) {
                recentGrid.add(createReelsSingleEmojiButton(tag, emojiPopup));
            }
            mainScrollContent.add(recentGrid);
        }

        for (EmojiCategory cat : categories) {
            JPanel titlePanel = createReelsEmojiSectionTitle(cat.title);
            mainScrollContent.add(titlePanel);
            JPanel gridPanel = new JPanel(new java.awt.GridLayout(0, 8, 2, 2));
            gridPanel.setBackground(java.awt.Color.WHITE);
            int maxEmojis = 40;
            for (int i = 1; i <= maxEmojis; i++) {
                String path = "/images/emoji/" + cat.prefix + "/" + cat.prefix + " (" + i + ").png";
                java.net.URL url = getClass().getResource(path);
                if (url == null) break;
                String tag = cat.prefix + "_" + i;
                gridPanel.add(createReelsSingleEmojiButton(tag, emojiPopup));
            }
            mainScrollContent.add(gridPanel);
        }

        JScrollPane scrollPane = new JScrollPane(mainScrollContent);
        scrollPane.setPreferredSize(new Dimension(300, 280));
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel container = new JPanel(new BorderLayout());
        container.setPreferredSize(new Dimension(300, 300));
        container.add(scrollPane, BorderLayout.CENTER);

        emojiPopup.add(container);
        emojiPopup.show(invoker, -260, -310);
    }

    private JPanel createReelsEmojiSectionTitle(String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setBackground(java.awt.Color.WHITE);
        JLabel lbl = new JLabel(text);
        lbl.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        lbl.setForeground(java.awt.Color.decode("#73708A"));
        p.add(lbl);
        return p;
    }

    private JButton createReelsSingleEmojiButton(String tag, JPopupMenu popup) {
        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(34, 34));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Derive path from tag
        String path;
        if (tag.contains("_")) {
            String[] parts = tag.split("_");
            String prefix = parts[0];
            String num = parts[1];
            path = "/images/emoji/" + prefix + "/" + prefix + " (" + num + ").png";
        } else {
            path = "/images/emoji/" + tag + ".png";
        }
        java.net.URL url = getClass().getResource(path);
        if (url != null) {
            try {
                java.awt.Image img = javax.imageio.ImageIO.read(url);
                if (img != null) {
                    btn.setIcon(new ImageIcon(img.getScaledInstance(28, 28, java.awt.Image.SCALE_SMOOTH)));
                }
            } catch (Exception e) { btn.setText("ðŸ˜Š"); }
        } else {
            btn.setText("â˜º");
        }

        btn.addActionListener(ae -> {
            if (txtComment != null) {
                int pos = txtComment.getCaretPosition();
                String cur = txtComment.getText();
                String emojiText = url != null ? ":" + tag + ":" : "â˜º";
                txtComment.setText(cur.substring(0, pos) + emojiText + cur.substring(pos));
                txtComment.setCaretPosition(pos + emojiText.length());
            }
            // Track recent
            recentEmojisReels.remove(tag);
            recentEmojisReels.add(0, tag);
            if (recentEmojisReels.size() > 16) recentEmojisReels.remove(recentEmojisReels.size() - 1);
            popup.setVisible(false);
        });
        return btn;
    }

    private javafx.scene.image.ImageView createSVGImageView(String path, int width, int height) {
        try {
            java.net.URL url = getClass().getClassLoader().getResource(path);
            if (url == null) return null;
            com.formdev.flatlaf.extras.FlatSVGIcon svgIcon = new com.formdev.flatlaf.extras.FlatSVGIcon(url).derive(width, height);
            java.awt.image.BufferedImage bImg = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2 = bImg.createGraphics();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            svgIcon.paintIcon(null, g2, 0, 0);
            g2.dispose();
            return new javafx.scene.image.ImageView(javafx.embed.swing.SwingFXUtils.toFXImage(bImg, null));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setCaptionWithEmojis(javafx.scene.control.Label label, String text) {
        label.setText(text);
        label.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        javafx.scene.text.TextFlow flow = new javafx.scene.text.TextFlow();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\[e:([a-zA-Z0-9_]+):(\\d+)\\]");
        java.util.regex.Matcher m = p.matcher(text);
        int lastEnd = 0;
        while(m.find()) {
            if(m.start() > lastEnd) {
                javafx.scene.text.Text t = new javafx.scene.text.Text(text.substring(lastEnd, m.start()));
                t.setFill(javafx.scene.paint.Color.WHITE);
                t.setFont(javafx.scene.text.Font.font("Segoe UI Emoji", 14));
                flow.getChildren().add(t);
            }
            String prefix = m.group(1);
            String id = m.group(2);
            String imgPath = "/images/emoji/" + prefix + "/" + prefix + " (" + id + ").png";
            try {
                java.io.InputStream is = getClass().getResourceAsStream(imgPath);
                if (is != null) {
                    javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(new javafx.scene.image.Image(is));
                    iv.setFitWidth(20);
                    iv.setFitHeight(20);
                    iv.setTranslateY(4);
                    flow.getChildren().add(iv);
                } else {
                    javafx.scene.text.Text t = new javafx.scene.text.Text(m.group());
                    t.setFill(javafx.scene.paint.Color.WHITE);
                    t.setFont(javafx.scene.text.Font.font("Segoe UI Emoji", 14));
                    flow.getChildren().add(t);
                }
            } catch(Exception e) {}
            lastEnd = m.end();
        }
        if(lastEnd < text.length()) {
            javafx.scene.text.Text t = new javafx.scene.text.Text(text.substring(lastEnd));
            t.setFill(javafx.scene.paint.Color.WHITE);
            t.setFont(javafx.scene.text.Font.font("Segoe UI Emoji", 14));
            flow.getChildren().add(t);
        }
        flow.setMaxWidth(label.getWidth() > 0 ? label.getWidth() : 350);
        flow.maxWidthProperty().bind(label.widthProperty());
        label.setGraphic(flow);
    }
}
