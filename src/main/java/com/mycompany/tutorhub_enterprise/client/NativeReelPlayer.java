package com.mycompany.tutorhub_enterprise.client;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.mycompany.tutorhub_enterprise.utils.B2Helper;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class NativeReelPlayer extends JDialog {
    
    // Model Data Classes
    class LocketVideo {
        String videoUrl;
        String title;
        String mediaType;
        int originalIndex;
    }
    class LocketUser {
        String authorName;
        String avatarB64;
        List<LocketVideo> videos = new ArrayList<>();
    }
    
    private List<LocketUser> groupedUsers = new ArrayList<>();
    private int currentUserIndex = 0;
    private int currentVideoIndex = 0;

    private JPanel imagePanel;
    private javax.swing.Timer imageTimer;
    private int currentProgressTime = 0;
    
    // Components
    private JPanel listContainer;
    private JPanel centerPanel;
    private JPanel centerContentWrapper;
    
    // Webcam mode
    private boolean isCameraMode = false;
    private Webcam webcam;
    private WebcamPanel webcamPanel;
    
    // UI Colors
    private final java.awt.Color PRIMARY = java.awt.Color.decode("#7C3AED");
    private final java.awt.Color BG_COLOR = java.awt.Color.decode("#F8FAFC");
    private final java.awt.Color MUTED = java.awt.Color.decode("#64748B");
    private final java.awt.Color TEXT_TITLE = java.awt.Color.decode("#0F172A");
    
    private JFrame parentFrame;

    public NativeReelPlayer(JFrame parent, List<String> reelsData, int initialIndex) {
        super(parent, true);
        this.parentFrame = parent;
        
        parseAndGroupData(reelsData, initialIndex);

        setUndecorated(true);
        setSize(parent.getSize());
        setLocationRelativeTo(parent);
        setBackground(new java.awt.Color(0, 0, 0, 180)); // Đen trong suốt, hơi đậm
        ((JComponent) getContentPane()).setOpaque(false);

        setLayout(new GridBagLayout()); // Căn giữa modal

        JPanel modalPanel = new JPanel(new BorderLayout(24, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_COLOR);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.dispose();
            }
        };
        modalPanel.setOpaque(false);
        modalPanel.setPreferredSize(new Dimension(1250, 750));
        modalPanel.setBorder(new EmptyBorder(24, 24, 24, 24));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point p = e.getPoint();
                if (getComponentCount() > 0) {
                    Component modal = getComponent(0);
                    if (!modal.getBounds().contains(p)) {
                        closePlayer();
                    }
                }
            }
        });

        buildUI(modalPanel);
        add(modalPanel);
        
        loadVideoAtCurrentIndices();
    }
    
    private FlatSVGIcon createColoredIcon(String svgName, int size, java.awt.Color color) {
        FlatSVGIcon icon = new FlatSVGIcon("images/icon_svg/" + svgName + ".svg", size, size);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> {
            if (c == null) return null;
            return new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue(), c.getAlpha());
        }));
        return icon;
    }
    
    private void parseAndGroupData(List<String> reelsData, int initialIndex) {
        for (int i = 0; i < reelsData.size(); i++) {
            String[] parts = reelsData.get(i).split(";;");
            String videoUrl = parts.length > 1 ? parts[1] : "";
            String title = parts.length > 2 ? parts[2] : "";
            String mediaType = parts.length > 3 ? parts[3] : "video";
            String author = "TutorHub";
            if (parts.length > 4 && parts[4] != null && !parts[4].isEmpty()) author = parts[4];
            String avatarB64 = "";
            if (parts.length > 5 && parts[5] != null && !parts[5].isEmpty() && !parts[5].equals("DEFAULT")) avatarB64 = parts[5];
            
            LocketUser foundUser = null;
            for (LocketUser u : groupedUsers) {
                if (u.authorName.equals(author)) {
                    foundUser = u;
                    break;
                }
            }
            if (foundUser == null) {
                foundUser = new LocketUser();
                foundUser.authorName = author;
                foundUser.avatarB64 = avatarB64;
                groupedUsers.add(foundUser);
            }
            
            LocketVideo v = new LocketVideo();
            v.videoUrl = videoUrl;
            v.title = title;
            v.mediaType = mediaType;
            v.originalIndex = i;
            foundUser.videos.add(v);
            
            if (i == initialIndex) {
                this.currentUserIndex = groupedUsers.indexOf(foundUser);
                this.currentVideoIndex = foundUser.videos.size() - 1;
            }
        }
        
        if (groupedUsers.isEmpty()) {
            LocketUser u = new LocketUser();
            u.authorName = "No Data";
            LocketVideo v = new LocketVideo();
            u.videos.add(v);
            groupedUsers.add(u);
        }
    }

    public void reloadVideos(List<String> data) {
        if (data == null) return;
        this.groupedUsers.clear();
        parseAndGroupData(data, 0); 
        renderList();
        closeCameraAndPlayVideo();
        loadVideoAtCurrentIndices();
    }


    private void buildUI(JPanel modalPanel) {
        // --- 1. Cột Trái (Sidebar) ---
        JPanel sidebarPanel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(java.awt.Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);
                g2.dispose();
            }
        };
        sidebarPanel.setOpaque(false);
        sidebarPanel.setPreferredSize(new Dimension(280, 700));
        sidebarPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel sideHeader = new JPanel();
        sideHeader.setLayout(new BoxLayout(sideHeader, BoxLayout.Y_AXIS));
        // Tiêu đề Locket Class
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titleRow.setOpaque(false);
        JLabel lblTitleIcon = new JLabel(loadActionIcon("icon_locket.png", 28));
        JLabel lblTitleTxt = new JLabel("Locket Class");
        lblTitleTxt.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 22));
        lblTitleTxt.setForeground(PRIMARY);
        titleRow.add(lblTitleIcon);
        titleRow.add(lblTitleTxt);
        sideHeader.add(titleRow);
        sideHeader.add(Box.createVerticalStrut(24));

        JPanel btnCreate = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(java.awt.Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new java.awt.Color(124, 58, 237, 30));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
            }
        };
        btnCreate.setOpaque(false);
        btnCreate.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCreate.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnCreate.setMaximumSize(new Dimension(999, 70));

        JPanel iconPlusBg = new JPanel(new BorderLayout());
        iconPlusBg.setOpaque(false);
        iconPlusBg.setPreferredSize(new Dimension(42, 42));
        JLabel lblPlus = new JLabel(loadActionIcon("plus1.png", 42)); // Đổi thành png vì SVG chứa base64
        lblPlus.setHorizontalAlignment(SwingConstants.CENTER);
        iconPlusBg.add(lblPlus, BorderLayout.CENTER);

        JPanel textCreateBox = new JPanel();
        textCreateBox.setLayout(new BoxLayout(textCreateBox, BoxLayout.Y_AXIS));
        textCreateBox.setOpaque(false);
        JLabel lblCreateTitle = new JLabel("Tạo thước phim");
        lblCreateTitle.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
        lblCreateTitle.setForeground(PRIMARY);
        JLabel lblCreateDesc = new JLabel("Chia sẻ kiến thức với lớp");
        lblCreateDesc.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        lblCreateDesc.setForeground(MUTED);
        textCreateBox.add(lblCreateTitle);
        textCreateBox.add(Box.createVerticalStrut(2));
        textCreateBox.add(lblCreateDesc);

        btnCreate.add(iconPlusBg);
        btnCreate.add(textCreateBox);
        
        btnCreate.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { openUploadLocket(); }
        });
        
        sideHeader.add(btnCreate);
        sideHeader.add(Box.createVerticalStrut(30));

        JLabel lblRecent = new JLabel("Gần đây");
        lblRecent.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
        lblRecent.setForeground(TEXT_TITLE);
        lblRecent.setAlignmentX(Component.LEFT_ALIGNMENT);
        sideHeader.add(lblRecent);
        sideHeader.add(Box.createVerticalStrut(12));

        sidebarPanel.add(sideHeader, BorderLayout.NORTH);

        // Danh sách Reels group by user
        listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setOpaque(false);

        JScrollPane scroll = new JScrollPane(listContainer);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        
        // Pill chứa List Gần đây
        JPanel listWrapper = new JPanel(new BorderLayout());
        listWrapper.setOpaque(false);
        listWrapper.setBorder(new EmptyBorder(8, 0, 8, 0));
        listWrapper.add(scroll, BorderLayout.CENTER);
        
        sidebarPanel.add(listWrapper, BorderLayout.CENTER);

        // --- 2. Cột Giữa (Center Video/Camera) ---
        centerPanel = new JPanel(new BorderLayout(0, 16));
        centerPanel.setOpaque(false);
        
        // Khung Media
        centerContentWrapper = new JPanel(new BorderLayout()) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 24, 24));
                super.paint(g2);
                g2.setColor(java.awt.Color.decode("#E2E8F0"));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        centerContentWrapper.setOpaque(false);
        centerContentWrapper.setBackground(java.awt.Color.BLACK);
        
        // Center is initially empty until initImagePlayer is called
        centerPanel.add(centerContentWrapper, BorderLayout.CENTER);

        // Thanh Reaction Bottom — chỉ Emoji + nút nhắn tin đến người đăng
        JPanel chatRow = new JPanel(new BorderLayout(12, 0));
        chatRow.setOpaque(false);
        chatRow.setPreferredSize(new Dimension(100, 58));

        // Pill Emoji (bên trái, chiếm phần lớn)
        JPanel emojiPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        emojiPanel.setOpaque(false);
        String[] REACTION_FILES = {"like.gif", "love.gif", "care.gif", "haha.gif", "wow.gif", "sad.gif", "angry.gif"};
        for (String file : REACTION_FILES) {
            emojiPanel.add(createEmojiReactionGif(file));
        }
        JPanel emojiWrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(java.awt.Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
                g2.dispose();
            }
        };
        emojiWrapper.setOpaque(false);
        emojiWrapper.setBorder(new EmptyBorder(4, 14, 4, 14));
        emojiWrapper.add(emojiPanel, BorderLayout.CENTER);

        // Nút "Nhắn tin" — pill tím compact (bên phải)
        JPanel btnChat = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new java.awt.Color(0xA78BFA),
                        getWidth(), getHeight(), new java.awt.Color(0x7C3AED));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
                g2.dispose();
            }
        };
        btnChat.setOpaque(false);
        btnChat.setPreferredSize(new Dimension(130, 50));
        btnChat.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JLabel icoChat = new JLabel(createColoredIcon("message", 18, java.awt.Color.WHITE));
        JLabel lblChatBtn = new JLabel("Nhắn tin");
        lblChatBtn.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
        lblChatBtn.setForeground(java.awt.Color.WHITE);
        btnChat.add(icoChat);
        btnChat.add(lblChatBtn);
        btnChat.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { sendMessageAndOpenChat(); }
            @Override public void mouseEntered(MouseEvent e) { btnChat.repaint(); }
        });

        chatRow.add(emojiWrapper, BorderLayout.CENTER);
        chatRow.add(btnChat, BorderLayout.EAST);
        centerPanel.add(chatRow, BorderLayout.SOUTH);

        // --- 3. Cột Phải (Action Panel) ---
        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        actionPanel.setOpaque(false);
        actionPanel.setBorder(new EmptyBorder(16, 0, 16, 0));
        
        actionPanel.add(createRightActionBtn("phat.png", "Phát", false));
        actionPanel.add(Box.createVerticalStrut(24));
        actionPanel.add(createRightActionBtn("camera.svg", "Ảnh", false));
        actionPanel.add(Box.createVerticalStrut(32));
        
        // Nút OK to bự
        JPanel btnOK = createRightActionBtn("check-circle-2.svg", "OK", true);
        actionPanel.add(btnOK);
        
        actionPanel.add(Box.createVerticalStrut(32));
        actionPanel.add(createRightActionBtn("tuychon.svg", "Tùy chọn", false));
        actionPanel.add(Box.createVerticalStrut(24));
        actionPanel.add(createRightActionBtn("trash.svg", "Xóa", false));

        // Pill chứa toàn bộ cột phải
        JPanel actionPanelWrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(java.awt.Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);
                g2.dispose();
            }
        };
        actionPanelWrapper.setOpaque(false);
        actionPanelWrapper.setPreferredSize(new Dimension(80, 700));
        actionPanelWrapper.add(actionPanel, BorderLayout.CENTER);

        modalPanel.add(sidebarPanel, BorderLayout.WEST);
        modalPanel.add(centerPanel, BorderLayout.CENTER);
        modalPanel.add(actionPanelWrapper, BorderLayout.EAST);
    }
    
    private Icon loadActionIcon(String filename, int size) {
        if (filename.endsWith(".png")) {
            try {
                java.net.URL url = getClass().getResource("/images/icon/" + filename);
                if (url != null) {
                    Image img = new ImageIcon(url).getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    return new ImageIcon(img);
                }
            } catch (Exception ex) {}
        } else if (filename.endsWith(".svg")) {
            return new FlatSVGIcon("images/icon/" + filename, size, size);
        }
        return null;
    }

    private JPanel createRightActionBtn(String filename, String text, boolean isLarge) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setCursor(new Cursor(Cursor.HAND_CURSOR));
        p.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JPanel circle = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                if (isLarge) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    // Viền Halo mờ
                    g2.setColor(new java.awt.Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 40));
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    // Hình tròn tím ở trong
                    g2.setColor(PRIMARY);
                    g2.fillOval(6, 6, getWidth()-12, getHeight()-12);
                    g2.dispose();
                }
            }
        };
        circle.setOpaque(false);
        int size = isLarge ? 64 : 48;
        circle.setPreferredSize(new Dimension(size, size));
        circle.setMaximumSize(new Dimension(size, size));
        
        if (isLarge) {
            JLabel lblTxt = new JLabel(text, SwingConstants.CENTER);
            lblTxt.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
            lblTxt.setForeground(java.awt.Color.WHITE); // Chữ trắng
            circle.add(lblTxt, BorderLayout.CENTER);
        } else {
            JLabel lblIcon = new JLabel(loadActionIcon(filename, 28));
            lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
            circle.add(lblIcon, BorderLayout.CENTER);
        }
        
        circle.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(circle);
        
        if (!isLarge) {
            p.add(Box.createVerticalStrut(8));
            JLabel lblTxt = new JLabel(text, SwingConstants.CENTER);
            lblTxt.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
            lblTxt.setForeground(MUTED);
            lblTxt.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(lblTxt);
        }
        
        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (filename.equals("camera.svg")) toggleCameraMode();
                else if (filename.equals("phat.png")) closeCameraAndPlayVideo();
                else if (filename.equals("trash.svg")) closePlayer();
                else if (isLarge) captureWebcamOrSubmit();
            }
        });
        
        return p;
    }
    
    private JPanel createEmojiReactionGif(String gifFile) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(42, 42));
        p.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        java.net.URL url = getClass().getResource("/images/reactions/" + gifFile);
        String gifNormal = url != null ? "<html><img src='" + url + "' width='32' height='32'></html>" : "";
        String gifHover = url != null ? "<html><img src='" + url + "' width='42' height='42'></html>" : "";
        
        JLabel lbl = new JLabel(gifNormal, SwingConstants.CENTER);
        p.add(lbl, BorderLayout.CENTER);
        
        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { lbl.setText(gifHover); }
            @Override public void mouseExited(MouseEvent e) { lbl.setText(gifNormal); }
            @Override public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(NativeReelPlayer.this, "Đã thả cảm xúc thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        return p;
    }

    private void renderList() {
        listContainer.removeAll();
        for (int i = 0; i < groupedUsers.size(); i++) {
            LocketUser user = groupedUsers.get(i);
            
            String timeStr = "Vừa xong";
            if (i == 1) timeStr = "1 giờ trước";
            if (i == 2) timeStr = "3 giờ trước";

            boolean isActive = (i == currentUserIndex && !isCameraMode);
            
            JPanel item = new JPanel(new BorderLayout(12, 0)) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (isActive) {
                        g2.setColor(new java.awt.Color(124, 58, 237, 10)); // Nhạt primary
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            item.setOpaque(false);
            item.setBorder(new EmptyBorder(12, 12, 12, 12));
            item.setMaximumSize(new Dimension(999, 70));
            item.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            JPanel avaBox = new JPanel(new BorderLayout()) {
                @Override public void paint(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, getWidth(), getHeight()));
                    super.paint(g2);
                    g2.dispose();
                }
            };
            avaBox.setOpaque(false);
            avaBox.setPreferredSize(new Dimension(46, 46));
            JLabel avaImg = new JLabel();
            setBase64Avatar(avaImg, user.avatarB64, 46, 46);
            avaBox.add(avaImg, BorderLayout.CENTER);
            
            JPanel textCol = new JPanel();
            textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
            textCol.setOpaque(false);
            JLabel lblName = new JLabel(user.authorName);
            lblName.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
            lblName.setForeground(TEXT_TITLE);
            JLabel lblTime = new JLabel(timeStr);
            lblTime.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
            lblTime.setForeground(MUTED);
            textCol.add(lblName);
            textCol.add(Box.createVerticalStrut(2));
            textCol.add(lblTime);
            
            item.add(avaBox, BorderLayout.WEST);
            item.add(textCol, BorderLayout.CENTER);
            
            if (isActive) {
                JPanel dot = new JPanel() {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(PRIMARY);
                        g2.fillOval(0, 0, 8, 8);
                        g2.dispose();
                    }
                };
                dot.setOpaque(false);
                dot.setPreferredSize(new Dimension(8, 8));
                JPanel dotWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 18));
                dotWrap.setOpaque(false);
                dotWrap.add(dot);
                item.add(dotWrap, BorderLayout.EAST);
            }
            
            final int idx = i;
            item.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    currentUserIndex = idx;
                    currentVideoIndex = 0;
                    closeCameraAndPlayVideo();
                    loadVideoAtCurrentIndices();
                }
            });
            
            listContainer.add(item);
            listContainer.add(Box.createVerticalStrut(4));
        }
        listContainer.revalidate();
        listContainer.repaint();
    }
    
    private void setBase64Avatar(JLabel label, String b64, int width, int height) {
        if (b64 == null || b64.isEmpty() || b64.equals("DEFAULT")) {
            label.setIcon(new FlatSVGIcon("images/icon_svg/user.svg", width, height));
            return;
        }
        new Thread(() -> {
            try {
                byte[] decoded = java.util.Base64.getDecoder().decode(b64);
                Image img = new ImageIcon(decoded).getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                SwingUtilities.invokeLater(() -> label.setIcon(new ImageIcon(img)));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> label.setIcon(new FlatSVGIcon("images/icon_svg/user.svg", width, height)));
            }
        }).start();
    }

    private void prevVideo() {
        if (currentVideoIndex > 0) {
            currentVideoIndex--;
            loadVideoAtCurrentIndices();
        } else if (currentUserIndex > 0) {
            currentUserIndex--;
            currentVideoIndex = groupedUsers.get(currentUserIndex).videos.size() - 1;
            loadVideoAtCurrentIndices();
        }
    }
    
    private void nextVideo() {
        if (currentVideoIndex < groupedUsers.get(currentUserIndex).videos.size() - 1) {
            currentVideoIndex++;
            loadVideoAtCurrentIndices();
        } else if (currentUserIndex < groupedUsers.size() - 1) {
            currentUserIndex++;
            currentVideoIndex = 0;
            loadVideoAtCurrentIndices();
        }
    }

    private void loadVideoAtCurrentIndices() {
        if (currentUserIndex < 0 || currentUserIndex >= groupedUsers.size()) return;
        LocketUser user = groupedUsers.get(currentUserIndex);
        if (currentVideoIndex < 0 || currentVideoIndex >= user.videos.size()) return;
        
        LocketVideo video = user.videos.get(currentVideoIndex);
        
        renderList(); 
        
        final String finalAuthor = user.authorName;
        final String finalAvatarB64 = user.avatarB64;
        final String videoUrl = video.videoUrl;
        final String title = video.title;
        final String mediaType = video.mediaType;
        final int userVideoCount = user.videos.size();
        final int curVidIdx = currentVideoIndex;
        
        initImagePlayer(videoUrl, title, finalAuthor, finalAvatarB64, curVidIdx, userVideoCount, mediaType);
    }

    private void initImagePlayer(String videoUrl, String title, String authorName, String avatarB64, int vIndex, int vCount, String mediaType) {
        if (imageTimer != null) {
            imageTimer.stop();
            imageTimer = null;
        }
        
        if (imagePanel == null) {
            imagePanel = new JPanel(new BorderLayout());
            imagePanel.setOpaque(true);
            imagePanel.setBackground(java.awt.Color.BLACK);
        }
        imagePanel.removeAll();
        
        JLabel imgLabel = new JLabel("", SwingConstants.CENTER);
        imgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        imgLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        
        final JPanel overlayPane = new JPanel();
        overlayPane.setLayout(new OverlayLayout(overlayPane));
        overlayPane.setOpaque(false);
        
        new Thread(() -> {
            try {
                Image img = null;
                if (videoUrl.startsWith("http")) {
                    String presigned = com.mycompany.tutorhub_enterprise.utils.B2Helper.getPresignedUrl(videoUrl);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(presigned).openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
                        try (java.io.InputStream in = conn.getInputStream()) {
                            byte[] bytes = in.readAllBytes();
                            img = new ImageIcon(bytes).getImage();
                        }
                    }
                } else {
                    img = new ImageIcon(videoUrl).getImage();
                }
                if (img != null && img.getWidth(null) > 0) {
                    Image scaled = img.getScaledInstance(800, 600, Image.SCALE_SMOOTH);
                    SwingUtilities.invokeLater(() -> {
                        imgLabel.setIcon(new ImageIcon(scaled));
                        imgLabel.revalidate();
                        if (overlayPane != null) {
                            overlayPane.revalidate();
                            overlayPane.repaint();
                        }
                    });
                }
            } catch (Exception e) {}
        }).start();
        
        
        JPanel uiLayer = new JPanel(new BorderLayout());
        uiLayer.setOpaque(false);
        
        JPanel topUI = new JPanel();
        topUI.setLayout(new BoxLayout(topUI, BoxLayout.Y_AXIS));
        topUI.setOpaque(false);
        topUI.setBorder(new EmptyBorder(16, 24, 0, 24));
        
        JPanel progressRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        progressRow.setOpaque(false);
        
        JPanel[] pBars = new JPanel[vCount];
        for (int i = 0; i < vCount; i++) {
            final int idx = i;
            pBars[i] = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (idx < vIndex) {
                        g2.setColor(java.awt.Color.WHITE);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 3, 3);
                    } else if (idx == vIndex) {
                        g2.setColor(new java.awt.Color(255, 255, 255, 100));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 3, 3);
                        g2.setColor(java.awt.Color.WHITE);
                        int w = (int) (getWidth() * (currentProgressTime / 5000.0));
                        g2.fillRoundRect(0, 0, w, getHeight(), 3, 3);
                    } else {
                        g2.setColor(new java.awt.Color(255, 255, 255, 100));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 3, 3);
                    }
                    g2.dispose();
                }
            };
            pBars[i].setOpaque(false);
            pBars[i].setPreferredSize(new Dimension((800 - 48) / vCount - 4, 3));
            progressRow.add(pBars[i]);
        }
        
        boolean[] isPaused = {false};
        JLabel btnPause = new JLabel();
        try {
            java.net.URL u = getClass().getResource("/images/icon/pause.png");
            if (u != null) btnPause.setIcon(new ImageIcon(new ImageIcon(u).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH)));
        } catch(Exception ignored){}
        btnPause.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPause.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { isPaused[0] = !isPaused[0]; }
        });
        
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.add(progressRow, BorderLayout.CENTER);
        topBar.add(btnPause, BorderLayout.EAST);
        
        topUI.add(topBar);
        topUI.add(Box.createVerticalStrut(16));
        
        JPanel avaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        avaRow.setOpaque(false);
        JLabel avaImg = new JLabel();
        setBase64Avatar(avaImg, avatarB64, 40, 40);
        avaRow.add(avaImg);
        
        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);
        JLabel nameLbl = new JLabel(authorName);
        nameLbl.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
        nameLbl.setForeground(java.awt.Color.WHITE);
        JLabel timeLbl = new JLabel("Vừa xong");
        timeLbl.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        timeLbl.setForeground(new java.awt.Color(255, 255, 255, 200));
        textCol.add(nameLbl);
        textCol.add(timeLbl);
        avaRow.add(textCol);
        topUI.add(avaRow);
        
        JPanel bottomUI = new JPanel();
        bottomUI.setLayout(new BoxLayout(bottomUI, BoxLayout.Y_AXIS));
        bottomUI.setOpaque(false);
        bottomUI.setBorder(new EmptyBorder(0, 24, 24, 24));
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 18));
        titleLbl.setForeground(java.awt.Color.WHITE);
        bottomUI.add(titleLbl);
        
        uiLayer.add(topUI, BorderLayout.NORTH);
        uiLayer.add(bottomUI, BorderLayout.SOUTH);
        
        JPanel navLayer = new JPanel(new BorderLayout());
        navLayer.setOpaque(false);
        JButton btnPrev = new JButton("<");
        btnPrev.setOpaque(false);
        btnPrev.setContentAreaFilled(false);
        btnPrev.setForeground(java.awt.Color.WHITE);
        btnPrev.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 24));
        btnPrev.setBorder(new EmptyBorder(0,16,0,0));
        btnPrev.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrev.addActionListener(e -> prevVideo());
        
        JButton btnNext = new JButton(">");
        btnNext.setOpaque(false);
        btnNext.setContentAreaFilled(false);
        btnNext.setForeground(java.awt.Color.WHITE);
        btnNext.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 24));
        btnNext.setBorder(new EmptyBorder(0,0,0,16));
        btnNext.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNext.addActionListener(e -> nextVideo());
        
        navLayer.add(btnPrev, BorderLayout.WEST);
        navLayer.add(btnNext, BorderLayout.EAST);
        
        overlayPane.add(navLayer);
        overlayPane.add(uiLayer);
        overlayPane.add(imgLabel); 
        
        imagePanel.add(overlayPane, BorderLayout.CENTER);
        
        centerContentWrapper.removeAll();
        centerContentWrapper.add(imagePanel, BorderLayout.CENTER);
        centerContentWrapper.revalidate();
        centerContentWrapper.repaint();
        
        currentProgressTime = 0;
        imageTimer = new javax.swing.Timer(50, e -> {
            if (!isPaused[0]) {
                currentProgressTime += 50;
                if (vIndex >= 0 && vIndex < pBars.length) {
                    pBars[vIndex].repaint();
                }
                if (currentProgressTime >= 5000) {
                    imageTimer.stop();
                    nextVideo();
                }
            }
        });
        imageTimer.start();
    }
    
    // ================= WEBCAM MODE ==================
    private void toggleCameraMode() {
        if (!isCameraMode) {
            isCameraMode = true;
            if (imageTimer != null) {
                imageTimer.stop();
            }
            renderList();
            
            centerContentWrapper.removeAll();
            
            new Thread(() -> {
                if (webcam == null) {
                    webcam = Webcam.getDefault();
                    if (webcam != null) {
                        webcam.setViewSize(WebcamResolution.VGA.getSize());
                        webcamPanel = new WebcamPanel(webcam);
                        webcamPanel.setImageSizeDisplayed(true);
                        webcamPanel.setMirrored(true);
                    }
                }
                SwingUtilities.invokeLater(() -> {
                    if (webcamPanel != null) {
                        centerContentWrapper.add(webcamPanel, BorderLayout.CENTER);
                    } else {
                        JLabel err = new JLabel("Không tìm thấy Webcam!", SwingConstants.CENTER);
                        err.setForeground(java.awt.Color.WHITE);
                        centerContentWrapper.add(err, BorderLayout.CENTER);
                    }
                    centerContentWrapper.revalidate();
                    centerContentWrapper.repaint();
                });
            }).start();
        }
    }
    
    private void closeCameraAndPlayVideo() {
        if (isCameraMode) {
            isCameraMode = false;
            if (webcam != null && webcam.isOpen()) {
                webcam.close();
            }
            centerContentWrapper.removeAll();
            centerContentWrapper.add(imagePanel, BorderLayout.CENTER);
            centerContentWrapper.revalidate();
            centerContentWrapper.repaint();
            
            if (imageTimer != null) {
                imageTimer.start();
            }
            renderList();
        }
    }
    
    private void captureWebcamOrSubmit() {
        if (isCameraMode && webcam != null && webcam.isOpen()) {
            BufferedImage image = webcam.getImage();
            JOptionPane.showMessageDialog(this, "Đã chụp ảnh Locket thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            closeCameraAndPlayVideo();
        }
    }

    private void openUploadLocket() {
        UploadLocketDialog dialog = new UploadLocketDialog(parentFrame);
        dialog.setVisible(true);
        if (dialog.isApproved()) {
            java.io.File file = dialog.getSelectedFile();
            String title = dialog.getTitle();
            String mediaType = dialog.getMediaType(); // "image" or "video"
            if (file == null) return;

            // Loading overlay
            JWindow loadingOverlay = new JWindow(parentFrame);
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
            overlayPanel.setBorder(new EmptyBorder(24, 36, 24, 36));
            JLabel lblLoading = new JLabel("⌛  Đang tải lên...");
            lblLoading.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
            lblLoading.setForeground(new java.awt.Color(0x8B5CF6));
            overlayPanel.add(lblLoading);
            loadingOverlay.setContentPane(overlayPanel);
            loadingOverlay.pack();
            loadingOverlay.setLocationRelativeTo(this);
            loadingOverlay.setOpacity(0.97f);
            loadingOverlay.setVisible(true);

            new Thread(() -> {
                try {
                    // Nếu là video thì nén trước, ảnh thì upload thẳng
                    java.io.File uploadFile = file;
                    if ("video".equals(mediaType)) {
                        java.io.File compressed = new java.io.File(file.getParentFile(),
                                "locket_" + System.currentTimeMillis() + "_" + file.getName());
                        SwingUtilities.invokeLater(() -> lblLoading.setText("⏳  Đang tối ưu video... 0%"));
                        // Chờ compress xong trước khi upload
                        final java.io.File[] result = {null};
                        Object lock = new Object();
                        com.mycompany.tutorhub_enterprise.utils.FFmpegUtils.compressVideo(file, compressed,
                                pct -> SwingUtilities.invokeLater(() -> lblLoading.setText("⏳  Tối ưu video... " + pct + "%")),
                                () -> { synchronized(lock) { result[0] = compressed; lock.notifyAll(); } },
                                err -> { synchronized(lock) { result[0] = file; lock.notifyAll(); } }
                        );
                        synchronized(lock) {
                            while (result[0] == null) lock.wait(30000);
                        }
                        uploadFile = result[0];
                    }

                    SwingUtilities.invokeLater(() -> lblLoading.setText("🚀  Đang tải lên Backblaze... 0%"));
                    final java.io.File finalUploadFile = uploadFile;
                    String url = uploadToBackblazeLocket(finalUploadFile, pct ->
                            SwingUtilities.invokeLater(() -> lblLoading.setText("🚀  Đang tải lên... " + pct + "%"))
                    );

                    if (url != null) {
                        String payload = url + ";;" + title + ";;" + mediaType;
                        com.mycompany.tutorhub_enterprise.client.NetworkManager.getInstance()
                                .sendPacket(new com.mycompany.tutorhub_enterprise.models.Packet("UPLOAD_LOCKET", payload));
                        Thread.sleep(800);
                        com.mycompany.tutorhub_enterprise.client.NetworkManager.getInstance()
                                .sendPacket(new com.mycompany.tutorhub_enterprise.models.Packet("GET_LOCKET_VIDEOS", ""));
                        SwingUtilities.invokeLater(() -> {
                            loadingOverlay.dispose();
                            JOptionPane.showMessageDialog(NativeReelPlayer.this,
                                    "🎉  Đã đăng Locket thành công!", "Thành công", JOptionPane.PLAIN_MESSAGE);
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            loadingOverlay.dispose();
                            JOptionPane.showMessageDialog(NativeReelPlayer.this, "Tải lên thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        });
                    }

                    // Cleanup compressed
                    if (!"video".equals(mediaType) == false && !uploadFile.equals(file) && uploadFile.exists()) {
                        uploadFile.delete();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        loadingOverlay.dispose();
                        JOptionPane.showMessageDialog(NativeReelPlayer.this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        }
    }

    /** Upload file ảnh/video lên Backblaze bucket cũ, folder locket/ */
    private String uploadToBackblazeLocket(java.io.File file, java.util.function.Consumer<Integer> progressCallback) {
        try {
            if (!B2Helper.isConfigured()) {
                System.err.println("[B2] Missing Backblaze credentials for locket upload.");
                return null;
            }

            String bucketName = B2Helper.getBucketName();
            software.amazon.awssdk.services.s3.S3Client s3 = B2Helper.createS3Client();

            String fileName = "locket/" + System.currentTimeMillis() + "_" + file.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
            long fileSize = file.length();
            long partSize = 5 * 1024 * 1024L;

            if (fileSize < partSize) {
                s3.putObject(
                    software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                        .bucket(bucketName).key(fileName).build(),
                    software.amazon.awssdk.core.sync.RequestBody.fromFile(file));
                if (progressCallback != null) progressCallback.accept(100);
            } else {
                // Multipart upload
                software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse cr = s3.createMultipartUpload(
                    software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest.builder()
                        .bucket(bucketName).key(fileName).build());
                String uploadId = cr.uploadId();
                int totalChunks = (int)((fileSize + partSize - 1) / partSize);
                java.util.concurrent.atomic.AtomicInteger done = new java.util.concurrent.atomic.AtomicInteger(0);
                java.util.List<software.amazon.awssdk.services.s3.model.CompletedPart> parts = new java.util.ArrayList<>();
                java.util.concurrent.ExecutorService exec = java.util.concurrent.Executors.newFixedThreadPool(8);
                java.util.List<java.util.concurrent.Future<software.amazon.awssdk.services.s3.model.CompletedPart>> futures = new java.util.ArrayList<>();
                long pos = 0; int pn = 1;
                while (pos < fileSize) {
                    long sz = Math.min(partSize, fileSize - pos);
                    final int curPn = pn; final long curPos = pos; final long curSz = sz;
                    futures.add(exec.submit(() -> {
                        byte[] buf = new byte[(int)curSz];
                        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r")) {
                            raf.seek(curPos); raf.readFully(buf);
                        }
                        var resp = s3.uploadPart(
                            software.amazon.awssdk.services.s3.model.UploadPartRequest.builder()
                                .bucket(bucketName).key(fileName).uploadId(uploadId).partNumber(curPn).build(),
                            software.amazon.awssdk.core.sync.RequestBody.fromBytes(buf));
                        int c = done.incrementAndGet();
                        if (progressCallback != null) progressCallback.accept((int)(c * 100.0 / totalChunks));
                        return software.amazon.awssdk.services.s3.model.CompletedPart.builder()
                            .partNumber(curPn).eTag(resp.eTag()).build();
                    }));
                    pos += sz; pn++;
                }
                for (var f : futures) parts.add(f.get());
                exec.shutdown();
                s3.completeMultipartUpload(
                    software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest.builder()
                        .bucket(bucketName).key(fileName).uploadId(uploadId)
                        .multipartUpload(software.amazon.awssdk.services.s3.model.CompletedMultipartUpload.builder().parts(parts).build())
                        .build());
            }
            return B2Helper.getPublicBaseUrl() + "/" + fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendMessageAndOpenChat() {
        closePlayer();
        if (parentFrame instanceof MainDashboard) {
            ((MainDashboard) parentFrame).switchToCard("Chat");
        }
    }

    private void closePlayer() {
        if (imageTimer != null) {
            imageTimer.stop();
        }
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
        SwingUtilities.invokeLater(this::dispose);
    }
}
