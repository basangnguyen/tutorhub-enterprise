package com.mycompany.tutorhub_enterprise.client;

import com.mycompany.tutorhub_enterprise.models.ConversationInfo;
import com.mycompany.tutorhub_enterprise.client.search.SearchAction;
import com.mycompany.tutorhub_enterprise.client.search.SearchQuery;
import com.mycompany.tutorhub_enterprise.client.search.SearchResult;
import com.mycompany.tutorhub_enterprise.client.search.SearchResultType;

import javax.swing.*;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeaderPanel extends JPanel {
    
    public static class NotificationModel {
        public String iconUrl, title, desc, time, bgIconColor;
        public boolean isUnread;

        public NotificationModel(String iconUrl, String title, String desc, String time, boolean isUnread, String bgIconColor) {
            this.iconUrl = iconUrl; this.title = title; this.desc = desc; 
            this.time = time; this.isUnread = isUnread; this.bgIconColor = bgIconColor;
        }
    }

    // Dữ liệu trống, sẽ nhận dữ liệu thực từ MainDashboard
    private List<NotificationModel> notifications = new ArrayList<>();
    private int unreadNotifCount = 0;

    private AvatarPanel avatarPanel;
    private MainDashboard dashboard; 
    private ChatTab chatTabRef; 
    private static Map<String, ImageIcon> iconCache = new HashMap<>();
    private boolean hasCustomAvatar = false; 

    private com.mycompany.tutorhub_enterprise.client.search.GlobalSearchBar globalSearchBar;
    private JPanel globalSearchContainer;
    
    private ActionBox chatBox;
    private ActionBox bellBox;

    private List<ConversationInfo> recentConversations = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    // XỬ LÝ VÒNG LẶP TOGGLE CHUẨN XÁC
    private long lastChatCloseTime = 0;
    private long lastNotifCloseTime = 0;
    private long lastProfileCloseTime = 0;

    public HeaderPanel(MainDashboard dashboard, String userName) {
        this.dashboard = dashboard;
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 0, 0));
        setPreferredSize(new Dimension(0, 64));

        // --- 1. THANH TÌM KIẾM TOÀN CỤC (REACT WEB BANNER) ---
        JPanel searchWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 40, 6));
        searchWrapper.setOpaque(false);
        
        globalSearchContainer = new JPanel(new BorderLayout());
        globalSearchContainer.setOpaque(false);
        globalSearchContainer.setPreferredSize(new Dimension(460, 52)); 
        
        globalSearchBar = new com.mycompany.tutorhub_enterprise.client.search.GlobalSearchBar();
        configureGlobalSearchCommands();
        globalSearchContainer.add(globalSearchBar, BorderLayout.CENTER);
        searchWrapper.add(globalSearchContainer);

        // --- 2. CÁC NÚT ĐIỀU KHIỂN BÊN PHẢI ---
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        rightControls.setOpaque(false);
        /*
        JButton btnNewClass = new JButton("+ Đăng lớp mới");
        btnNewClass.setBackground(Color.decode("#246AF3"));
        btnNewClass.setForeground(Color.WHITE);
        btnNewClass.putClientProperty("JComponent.arc", 20);
        btnNewClass.setPreferredSize(new Dimension(140, 40));
        btnNewClass.setFocusPainted(false);
        btnNewClass.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNewClass.addActionListener(e -> new CreateClassDialog(dashboard).setVisible(true));
        rightControls.add(btnNewClass);
        */
        // --- NÚT TIN NHẮN ---
        chatBox = new ActionBox("https://img.icons8.com/fluency-systems-regular/48/4B5563/chat.png", 0);
        chatBox.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { 
                if (System.currentTimeMillis() - lastChatCloseTime < 200) return; // Chống vòng lặp
                showMessagePopup(chatBox); 
            }
        });
        rightControls.add(chatBox);
        
        // --- NÚT THÔNG BÁO ---
        bellBox = new ActionBox("https://img.icons8.com/fluency-systems-regular/48/4B5563/appointment-reminders.png", 0);
        bellBox.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { 
                if (System.currentTimeMillis() - lastNotifCloseTime < 200) return; // Chống vòng lặp
                showNotificationPopup(bellBox); 
            }
        });
        rightControls.add(bellBox);

        // --- NÚT PROFILE ---
        JPanel profileWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)) {
            boolean isHovered = false;
            @Override protected void paintComponent(Graphics g) {
                if(isHovered) {
                    Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.decode("#F8FAFC")); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16); g2.dispose();
                } super.paintComponent(g);
            }
            public void setHover(boolean h) { this.isHovered = h; repaint(); }
        };
        profileWrapper.setOpaque(false); profileWrapper.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profileWrapper.setBorder(new EmptyBorder(2, 6, 2, 10)); 

        avatarPanel = new AvatarPanel();
        new Thread(() -> { 
            try { 
                Image rawImg = javax.imageio.ImageIO.read(new java.net.URL("https://img.icons8.com/color/96/circled-user-male-skin-type-4--v1.png"));
                SwingUtilities.invokeLater(() -> { if (!hasCustomAvatar) { avatarPanel.setAvatar(rawImg); } }); 
            } catch (Exception ignored) {} 
        }).start();

        String displayName = userName != null ? userName : "Gia sư";
        if (displayName.contains("@")) displayName = displayName.split("@")[0]; 
        if (displayName.length() > 16) displayName = displayName.substring(0, 14) + "..."; 

        JPanel profileInfo = new JPanel(); profileInfo.setLayout(new BoxLayout(profileInfo, BoxLayout.Y_AXIS)); profileInfo.setOpaque(false);
        JLabel lblName = new JLabel(displayName); lblName.setFont(new Font("Segoe UI", Font.BOLD, 14)); lblName.setForeground(Color.decode("#111827"));
        JLabel lblRole = new JLabel("Gia sư"); lblRole.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblRole.setForeground(Color.decode("#9CA3AF"));
        profileInfo.add(lblName); profileInfo.add(lblRole);

        JLabel lblArrow = new JLabel(); setNetworkIcon(lblArrow, "https://img.icons8.com/fluency-systems-regular/48/9CA3AF/expand-arrow.png", 14, 14);

        profileWrapper.add(avatarPanel);
        profileWrapper.add(profileInfo);
        profileWrapper.add(lblArrow);

        JPopupMenu profileMenu = new JPopupMenu(); profileMenu.setBorder(BorderFactory.createLineBorder(Color.decode("#E5E7EB"), 1));
        JMenuItem mnuProfile = createProfileMenuMenuItem("Hồ sơ cá nhân", "user");
        mnuProfile.addActionListener(e -> dashboard.switchToCard("Profile"));
        profileMenu.add(mnuProfile);
        profileMenu.add(createProfileMenuMenuItem("Lớp của tôi", "book"));
        profileMenu.add(createProfileMenuMenuItem("Ví thu nhập", "wallet"));
        profileMenu.addSeparator();
        profileMenu.add(createProfileMenuMenuItem("Cài đặt", "settings"));
        profileMenu.addSeparator();
        JMenuItem logoutItem = createProfileMenuMenuItem("Đăng xuất", "exit");
        logoutItem.setForeground(Color.decode("#EF4444")); 
        
        logoutItem.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn đăng xuất?", "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                System.out.println("[LOGOUT] Cleaning session...");
                
                com.mycompany.tutorhub_enterprise.models.auth.SessionInfo sessionInfo = com.mycompany.tutorhub_enterprise.client.auth.ClientSessionManager.getSession();
                if (sessionInfo != null) {
                    new Thread(() -> {
                        try {
                            new AuthClient().logout(sessionInfo.getSessionId(), sessionInfo.getAccessToken());
                        } catch (Exception ex) {
                            System.err.println("[LOGOUT] Server revoke failed: " + ex.getMessage());
                        }
                    }).start();
                }
                com.mycompany.tutorhub_enterprise.client.auth.ClientSessionManager.clear();
                
                System.out.println("[LOGOUT] Stopping social login timers...");
                com.mycompany.tutorhub_enterprise.client.oauth.FacebookLoginFlow.stop();
                com.mycompany.tutorhub_enterprise.client.oauth.OAuthLoginFlow.stop();
                System.out.println("[LOGOUT] Disconnecting websocket...");
                System.out.println("[LOGOUT] Reset NetworkManager/AuthClient...");
                NetworkManager.resetInstance(); dashboard.dispose(); 
                SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
            }
        });
        profileMenu.add(logoutItem);

        profileMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { lastProfileCloseTime = System.currentTimeMillis(); }
            @Override public void popupMenuCanceled(PopupMenuEvent e) {}
        });

        profileWrapper.addMouseListener(new MouseAdapter(){ 
            public void mouseEntered(MouseEvent e) { try{ profileWrapper.getClass().getMethod("setHover", boolean.class).invoke(profileWrapper, true); } catch(Exception ex){}}
            public void mouseExited(MouseEvent e) { try{ profileWrapper.getClass().getMethod("setHover", boolean.class).invoke(profileWrapper, false); } catch(Exception ex){}}
            public void mousePressed(MouseEvent e) { 
                if (System.currentTimeMillis() - lastProfileCloseTime < 200) return; // Chống vòng lặp
                profileMenu.show(profileWrapper, 0, profileWrapper.getHeight() + 5); 
            } 
        });

        rightControls.add(profileWrapper);
        add(searchWrapper, BorderLayout.WEST);
        add(rightControls, BorderLayout.EAST);
    }

    public void bindChatTab(ChatTab chatTab) {
        this.chatTabRef = chatTab;
    }

    public void addNotification(String iconUrl, String title, String desc) {
        SwingUtilities.invokeLater(() -> {
            String safeIcon = (iconUrl == null || iconUrl.isEmpty()) ? "https://img.icons8.com/fluency/48/ok.png" : iconUrl;
            notifications.add(0, new NotificationModel(safeIcon, title, desc, "Vừa xong", true, "#EEF2FF"));
            if (notifications.size() > 20) notifications.remove(notifications.size() - 1);
            unreadNotifCount++;
            if (bellBox != null) bellBox.setBadge(unreadNotifCount);
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
        g2.dispose();
        super.paintComponent(g);
    }

    // HÀM MỚI: Dùng để cập nhật trực tiếp con số trên Icon Chat từ bên ngoài
    public void updateChatBadge(int unreadCount) {
        SwingUtilities.invokeLater(() -> {
            if (chatBox != null) {
                chatBox.setBadge(unreadCount);
            }
            if (dashboard != null) {
                dashboard.updateMessageTabBadge(unreadCount);
            }
        });
    }
    
    public JTextField getGlobalSearchInput() { return globalSearchBar.getField(); }
    public JPanel getGlobalSearchContainer() { return globalSearchContainer; }
    public void updateAvatar(Image img) { this.hasCustomAvatar = true; if (avatarPanel != null) { avatarPanel.setAvatar(img); } }
    public Image getAvatar() { return avatarPanel != null ? avatarPanel.getAvatarImage() : null; }

    private void configureGlobalSearchCommands() {
        globalSearchBar.setGlobalDropdownEnabledSupplier(() -> dashboard != null && !dashboard.isCurrentCard("Chat"));
        globalSearchBar.setDropdownResultsProvider(this::buildGlobalCommandResults);
    }

    private List<SearchResult> buildGlobalCommandResults(SearchQuery query) {
        List<SearchResult> results = new ArrayList<>();
        addCommandIfMatches(results, query, "Mở Bảng tin", "Đi tới màn hình tổng quan", "HOME", "Home", "bang tin home dashboard tong quan");
        addCommandIfMatches(results, query, "Mở Tin nhắn", "Mở hội thoại và tìm bạn bè", "MSG", "Chat", "tin nhan chat message hoi thoai");
        addCommandIfMatches(results, query, "Mở Lớp học", "Quản lý lớp học của tôi", "CLS", "Saved", "lop hoc class classroom quan ly");
        addCommandIfMatches(results, query, "Mở Lịch", "Xem lịch học và lịch dạy", "CAL", "Schedule", "lich calendar schedule");
        addCommandIfMatches(results, query, "Mở QuizHub", "Ôn tập và luyện quiz", "QUIZ", "QuizHub", "quiz quizhub on tap luyen tap");
        addCommandIfMatches(results, query, "Mở Tài liệu", "Mở drive tài liệu học tập", "DOC", "Docs", "tai lieu document docs drive");
        addCommandIfMatches(results, query, "Mở Hồ sơ", "Xem thông tin tài khoản", "USR", "Profile", "ho so profile tai khoan user");
        addCommandIfMatches(results, query, "Mở Nâng cấp", "Xem các gói TutorHub Premium", "PRO", "Upgrade", "nang cap upgrade premium vip");

        if (query != null && !query.isBlank()) {
            results.add(SearchResult.builder()
                    .title("Tìm trong TutorHub: " + query.getRawText())
                    .subtitle("Kết quả nội bộ đầy đủ sẽ được mở ở phase sau")
                    .type(SearchResultType.WEB)
                    .score(0.05)
                    .iconText("TH")
                    .action(SearchAction.noop())
                    .build());
        }
        return results;
    }

    private void addCommandIfMatches(List<SearchResult> results, SearchQuery query, String title,
                                     String subtitle, String iconText, String cardKey, String aliases) {
        if (!matchesSearch(query, title, subtitle, aliases)) {
            return;
        }
        results.add(SearchResult.builder()
                .title(title)
                .subtitle(subtitle)
                .type(SearchResultType.COMMAND)
                .score(1.0)
                .iconText(iconText)
                .action(switchCardAction(cardKey))
                .build());
    }

    private boolean matchesSearch(SearchQuery query, String title, String subtitle, String aliases) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String haystack = SearchQuery.of(title + " " + subtitle + " " + aliases).getNormalizedText();
        return haystack.contains(query.getNormalizedText());
    }

    private SearchAction switchCardAction(String cardKey) {
        return () -> SwingUtilities.invokeLater(() -> {
            if (dashboard != null) {
                dashboard.switchToCard(cardKey);
            }
            if (globalSearchBar != null) {
                globalSearchBar.getField().setText("");
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                globalSearchBar.hideDropdown();
            }
        });
    }

    private void showNotificationPopup(Component invoker) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(Color.decode("#F9FAFB"));
        popup.setBorder(BorderFactory.createLineBorder(Color.decode("#E5E7EB"), 1));
        
        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { lastNotifCloseTime = System.currentTimeMillis(); }
            @Override public void popupMenuCanceled(PopupMenuEvent e) {}
        });
        
        int popupWidth = 460;
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.setPreferredSize(new Dimension(popupWidth, 520)); 
        
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(16, 20, 10, 20));
        
        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); leftHeader.setOpaque(false);
        JPanel iconWrap = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode("#F3F4F6")); g2.fillOval(0,0,getWidth(),getHeight()); g2.dispose();
            }
        };
        iconWrap.setPreferredSize(new Dimension(36, 36)); iconWrap.setOpaque(false);
        JLabel bellIcon = new JLabel("", SwingConstants.CENTER); 
        setNetworkIcon(bellIcon, "https://img.icons8.com/fluency-systems-regular/48/4B5563/appointment-reminders.png", 20, 20);
        iconWrap.add(bellIcon);
        
        JLabel title = new JLabel("Thông báo");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.decode("#111827"));
        leftHeader.add(iconWrap); leftHeader.add(title);
        
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 8)); rightHeader.setOpaque(false);
        JLabel markRead = new JLabel("Đánh dấu tất cả là đã đọc");
        markRead.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        markRead.setForeground(Color.decode("#2563EB"));
        markRead.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        markRead.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                for (NotificationModel n : notifications) n.isUnread = false;
                unreadNotifCount = 0;
                if (bellBox != null) bellBox.setBadge(0);
                popup.setVisible(false);
            }
        });

        JLabel settingsIcon = new JLabel(); setNetworkIcon(settingsIcon, "https://img.icons8.com/fluency-systems-regular/48/4B5563/settings.png", 20, 20);
        settingsIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rightHeader.add(markRead); rightHeader.add(settingsIcon);
        
        header.add(leftHeader, BorderLayout.WEST);
        header.add(rightHeader, BorderLayout.EAST);
        
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(10, 16, 10, 16));
        
        if (notifications.isEmpty()) {
            JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setBackground(Color.WHITE); p.setBorder(new EmptyBorder(40, 20, 20, 20));
            JLabel icon = new JLabel(); 
            setNetworkIcon(icon, "https://img.icons8.com/fluency-systems-regular/48/CBD5E1/appointment-reminders.png", 48, 48); 
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel lblEmpty = new JLabel("Không có thông báo nào"); lblEmpty.setFont(new Font("Segoe UI", Font.BOLD, 15)); lblEmpty.setForeground(Color.decode("#0F172A")); lblEmpty.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(icon); p.add(Box.createVerticalStrut(15)); p.add(lblEmpty);
            body.add(p);
        } else {
            for (int i = 0; i < notifications.size(); i++) {
                NotificationModel n = notifications.get(i);
                body.add(createNotificationCard(n.iconUrl, n.bgIconColor, n.title, n.desc, n.time, n.isUnread, popup));
                if (i < notifications.size() - 1) body.add(Box.createVerticalStrut(8));
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(body);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.decode("#F8FAFC"));
        footer.setBorder(new EmptyBorder(15, 0, 15, 0));
        JLabel viewAll = new JLabel("Xem tất cả thông báo  >", SwingConstants.CENTER);
        viewAll.setFont(new Font("Segoe UI", Font.BOLD, 14));
        viewAll.setForeground(Color.decode("#2563EB"));
        viewAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        footer.add(viewAll, BorderLayout.CENTER);
        
        container.add(header, BorderLayout.NORTH);
        container.add(scrollPane, BorderLayout.CENTER);
        container.add(footer, BorderLayout.SOUTH);
        
        popup.add(container);
        popup.show(HeaderPanel.this, HeaderPanel.this.getWidth() - popupWidth - 25, HeaderPanel.this.getHeight() - 5);
    }

    private JPanel createNotificationCard(String iconUrl, String bgIconColor, String title, String desc, String time, boolean isUnread, JPopupMenu parentPopup) {
        JPanel card = new JPanel(new BorderLayout(15, 0)) {
            boolean isHover = false;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isHover ? Color.decode("#F9FAFB") : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(Color.decode("#F3F4F6"));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose(); super.paintComponent(g);
            }
            public void setHover(boolean h) { this.isHover = h; repaint(); }
        };
        card.setOpaque(false); card.setBorder(new EmptyBorder(16, 16, 16, 16)); card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setMaximumSize(new Dimension(999, 90));

        JPanel iconPanel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode(bgIconColor != null ? bgIconColor : "#FFFFFF")); g2.fillOval(0, 0, getWidth(), getHeight()); g2.dispose();
            }
        };
        iconPanel.setOpaque(false); iconPanel.setPreferredSize(new Dimension(48, 48));
        JLabel lblIcon = new JLabel("", SwingConstants.CENTER); setNetworkIcon(lblIcon, iconUrl, 24, 24);
        iconPanel.add(lblIcon, BorderLayout.CENTER);
        card.add(iconPanel, BorderLayout.WEST);

        JPanel textPanel = new JPanel(); textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS)); textPanel.setOpaque(false);
        String redDot = isUnread ? "<font color='#EF4444'>● </font>" : "";
        textPanel.add(new JLabel("<html>" + redDot + "<b style='color:#111827; font-size:14px; font-family:Segoe UI;'>" + title + "</b></html>"));
        textPanel.add(Box.createVerticalStrut(6));
        textPanel.add(new JLabel("<html><span style='color:#4B5563; font-size:13px; font-family:Segoe UI;'>" + desc + "</span></html>"));
        textPanel.add(Box.createVerticalStrut(6));
        textPanel.add(new JLabel("<html><span style='color:#9CA3AF; font-size:11px; font-family:Segoe UI;'>" + time + "</span></html>"));
        card.add(textPanel, BorderLayout.CENTER);

        JLabel rightArrow = new JLabel(); setNetworkIcon(rightArrow, "https://img.icons8.com/material-rounded/24/9CA3AF/forward.png", 16, 16);
        card.add(rightArrow, BorderLayout.EAST);

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { try{ card.getClass().getMethod("setHover", boolean.class).invoke(card, true); }catch(Exception ex){} }
            @Override public void mouseExited(MouseEvent e) { try{ card.getClass().getMethod("setHover", boolean.class).invoke(card, false); }catch(Exception ex){} }
        });
        return card;
    }

    private void showMessagePopup(Component invoker) {
        if (chatTabRef != null && chatTabRef.getConversations() != null) {
            this.recentConversations = chatTabRef.getConversations();
            int unread = recentConversations.stream().mapToInt(c -> c.unreadCount).sum();
            if (chatBox != null) chatBox.setBadge(unread);
            if (dashboard != null) dashboard.updateMessageTabBadge(unread);
        }

        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(Color.WHITE);
        popup.setBorder(BorderFactory.createLineBorder(Color.decode("#E5E7EB"), 1));
        
        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { lastChatCloseTime = System.currentTimeMillis(); }
            @Override public void popupMenuCanceled(PopupMenuEvent e) {}
        });
        
        int popupWidth = 380;
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.setPreferredSize(new Dimension(popupWidth, 520));
        
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Color.WHITE);
        titlePanel.setBorder(new EmptyBorder(15, 20, 10, 20));
        JLabel title = new JLabel("Tin nhắn");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.decode("#111827"));
        
        JLabel newMsg = new JLabel(); 
        setNetworkIcon(newMsg, "https://img.icons8.com/fluency-systems-regular/48/4B5563/edit.png", 20, 20);
        newMsg.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        titlePanel.add(title, BorderLayout.WEST);
        titlePanel.add(newMsg, BorderLayout.EAST);
        
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        tabs.setBackground(Color.WHITE);
        tabs.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.decode("#E5E7EB")));
        
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        
        JScrollPane scrollPane = new JScrollPane(body);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        Runnable renderFilter = new Runnable() {
            String currentFilter = "ALL";
            public void run() {
                body.removeAll();
                tabs.removeAll();
                tabs.add(createFilterTab("Tất cả", "ALL", this));
                tabs.add(createFilterTab("Ưu tiên", "PRIORITY", this));
                tabs.add(createFilterTab("Chưa đọc", "UNREAD", this));
                
                boolean hasItem = false;
                for (ConversationInfo c : recentConversations) {
                    if (currentFilter.equals("UNREAD") && c.unreadCount == 0) continue;
                    if (currentFilter.equals("PRIORITY") && !c.isPriority) continue;
                    body.add(createMessageItem(c, popup));
                    hasItem = true;
                }
                
                if (!hasItem) {
                    JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setBackground(Color.WHITE); p.setBorder(new EmptyBorder(40, 20, 20, 20));
                    JLabel icon = new JLabel(); setNetworkIcon(icon, "https://img.icons8.com/fluency-systems-regular/48/CBD5E1/chat.png", 48, 48); icon.setAlignmentX(Component.CENTER_ALIGNMENT);
                    JLabel lblEmpty = new JLabel("Hộp thư trống"); lblEmpty.setFont(new Font("Segoe UI", Font.BOLD, 15)); lblEmpty.setForeground(Color.decode("#0F172A")); lblEmpty.setAlignmentX(Component.CENTER_ALIGNMENT);
                    p.add(icon); p.add(Box.createVerticalStrut(15)); p.add(lblEmpty);
                    body.add(p);
                }
                
                tabs.revalidate(); tabs.repaint();
                body.revalidate(); body.repaint();
            }
            public Runnable setFilter(String filter) { this.currentFilter = filter; return this; }
        }.setFilter("ALL");

        renderFilter.run();
        header.add(titlePanel, BorderLayout.NORTH);
        header.add(tabs, BorderLayout.SOUTH);
        
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.decode("#F3F4F6")));
        JLabel viewAll = new JLabel("Xem tất cả trong Messenger");
        viewAll.setFont(new Font("Segoe UI", Font.BOLD, 14));
        viewAll.setForeground(Color.decode("#2563EB"));
        viewAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        viewAll.addMouseListener(new MouseAdapter() { 
            @Override public void mouseClicked(MouseEvent e) { 
                popup.setVisible(false); if(dashboard != null) dashboard.switchToCard("Chat"); 
            }
        });
        footer.add(viewAll);
        
        container.add(header, BorderLayout.NORTH);
        container.add(scrollPane, BorderLayout.CENTER);
        container.add(footer, BorderLayout.SOUTH);
        
        popup.add(container);
        popup.show(HeaderPanel.this, HeaderPanel.this.getWidth() - popupWidth - 25, HeaderPanel.this.getHeight() - 5);
    }

    private JPanel createFilterTab(String text, String filterType, Runnable reRenderCallback) {
        String activeFilter = "";
        try {
            activeFilter = (String) reRenderCallback.getClass().getDeclaredField("currentFilter").get(reRenderCallback);
        } catch (Exception e) {}
        boolean isActive = activeFilter.equals(filterType);

        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (isActive) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(Color.decode("#2563EB"));
                    g2.fillRect(0, getHeight() - 2, getWidth(), 2);
                    g2.dispose();
                }
            }
        };
        p.setOpaque(false); p.setBorder(new EmptyBorder(12, 0, 12, 0)); p.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", isActive ? Font.BOLD : Font.PLAIN, 14));
        lbl.setForeground(isActive ? Color.decode("#2563EB") : Color.decode("#64748B"));
        p.add(lbl, BorderLayout.CENTER);
        
        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                try { 
                    reRenderCallback.getClass().getDeclaredMethod("setFilter", String.class).invoke(reRenderCallback, filterType); 
                    reRenderCallback.run();
                } catch(Exception ex) {}
            }
        });
        return p;
    }

    private JPanel createMessageItem(ConversationInfo c, JPopupMenu parentPopup) {
        JPanel p = new JPanel(new BorderLayout(12, 0)) {
            boolean isHover = false;
            @Override protected void paintComponent(Graphics g) {
                if (isHover) { g.setColor(Color.decode("#F8F9FA")); g.fillRect(0, 0, getWidth(), getHeight()); }
                else { g.setColor(Color.WHITE); g.fillRect(0, 0, getWidth(), getHeight()); }
            }
            public void setHover(boolean h) { this.isHover = h; repaint(); }
        };
        p.setOpaque(false); p.setBorder(new EmptyBorder(12, 16, 12, 16)); p.setCursor(new Cursor(Cursor.HAND_CURSOR));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));

        JLayeredPane avatarPane = new JLayeredPane(); avatarPane.setPreferredSize(new Dimension(48, 48));
        JLabel lblAvatar = new JLabel(); setAvatarIcon(lblAvatar, c.avatarUrl, 48); lblAvatar.setBounds(0, 0, 48, 48); avatarPane.add(lblAvatar, Integer.valueOf(0));
        JPanel dot = new CircleDot(c.isOnline ? Color.decode("#10B981") : Color.decode("#94A3B8"));
        dot.setBounds(34, 34, 14, 14); avatarPane.add(dot, Integer.valueOf(1));
        p.add(avatarPane, BorderLayout.WEST);

        JPanel center = new JPanel(); center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS)); center.setOpaque(false);
        JLabel lblName = new JLabel(c.displayName); lblName.setFont(new Font("Segoe UI", Font.BOLD, 15)); lblName.setForeground(Color.decode("#0F172A"));
        
        String msgContent = c.lastMessage != null ? c.lastMessage : "Bắt đầu trò chuyện!";
        JLabel lblMsg = new JLabel("<html><div style='width: 200px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;'>" + msgContent + "</div></html>"); 
        lblMsg.setFont(new Font("Segoe UI", c.unreadCount > 0 ? Font.BOLD : Font.PLAIN, 13)); 
        lblMsg.setForeground(c.unreadCount > 0 ? Color.decode("#0F172A") : Color.decode("#64748B"));
        center.add(lblName); center.add(Box.createVerticalStrut(4)); center.add(lblMsg); p.add(center, BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout()); right.setOpaque(false);
        String timeStr = c.lastMessageTime != null ? timeFormat.format(c.lastMessageTime) : "";
        JLabel lblTime = new JLabel(timeStr); lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 11)); lblTime.setForeground(Color.decode("#64748B")); right.add(lblTime, BorderLayout.NORTH);
        
        if (c.unreadCount > 0) { 
            JPanel badgeWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); badgeWrapper.setOpaque(false); 
            badgeWrapper.add(new PillBadge(String.valueOf(c.unreadCount), Color.decode("#EF4444"), Color.WHITE)); right.add(badgeWrapper, BorderLayout.SOUTH); 
        }
        p.add(right, BorderLayout.EAST);

        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { try{ p.getClass().getMethod("setHover", boolean.class).invoke(p, true); }catch(Exception ex){} }
            @Override public void mouseExited(MouseEvent e) { try{ p.getClass().getMethod("setHover", boolean.class).invoke(p, false); }catch(Exception ex){} }
            @Override public void mouseClicked(MouseEvent e) { 
                parentPopup.setVisible(false); 
                if(dashboard != null) dashboard.switchToCard("Chat"); 
            }
        });
        return p;
    }

    // =========================================================================
    // HELPER CLASSES
    // =========================================================================

    // Removed JavaSearchInterface since we don't use JS interop anymore

    class ActionBox extends JPanel {
        private int badgeCount = 0;
        private JLabel lblBadge;
        private JPanel badgePanel;
        private boolean isHovered = false;

        public ActionBox(String iconUrl, int initialBadge) {
            setOpaque(false); setPreferredSize(new Dimension(40, 40)); setCursor(new Cursor(Cursor.HAND_CURSOR));
            setLayout(new BorderLayout());
            
            JLabel lblIcon = new JLabel(); setNetworkIcon(lblIcon, iconUrl, 22, 22);
            JLayeredPane layeredPane = new JLayeredPane(); layeredPane.setPreferredSize(new Dimension(40, 40));
            lblIcon.setBounds(9, 9, 22, 22); layeredPane.add(lblIcon, Integer.valueOf(0));
            
            badgePanel = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.decode("#EF4444")); 
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight()); 
                    g2.dispose();
                }
            };
            badgePanel.setOpaque(false);
            
            lblBadge = new JLabel("", SwingConstants.CENTER); 
            lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 10)); 
            lblBadge.setForeground(Color.WHITE);
            badgePanel.add(lblBadge, BorderLayout.CENTER);
            
            layeredPane.add(badgePanel, Integer.valueOf(1));
            add(layeredPane, BorderLayout.CENTER);
            
            addMouseListener(new MouseAdapter(){ 
                public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
            });
            
            setBadge(initialBadge);
        }

        public void setBadge(int count) {
            this.badgeCount = count;
            if(count > 0) {
                lblBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                int width = count > 9 ? 22 : 16;
                badgePanel.setBounds(21, 5, width, 16); 
                badgePanel.setVisible(true);
            } else {
                badgePanel.setVisible(false);
            }
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            if(isHovered) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode("#F5F7FB")); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16); g2.dispose();
            } super.paintComponent(g);
        }
    }

    private JMenuItem createProfileMenuMenuItem(String text, String iconName) {
        JMenuItem item = new JMenuItem("  " + text);
        item.setFont(new Font("Segoe UI", Font.PLAIN, 14)); item.setBackground(Color.WHITE); item.setForeground(Color.decode("#374151"));
        item.setPreferredSize(new Dimension(200, 40)); item.setCursor(new Cursor(Cursor.HAND_CURSOR));
        setNetworkIcon(item, "https://img.icons8.com/fluency-systems-regular/48/4B5563/" + iconName + ".png", 18, 18);
        return item;
    }

    private void setNetworkIcon(JLabel label, String urlStr, int width, int height) {
        String iconName = getSvgNameFromUrl(urlStr);
        FlatSVGIcon svg = new FlatSVGIcon("images/icon_svg/" + iconName + ".svg", width, height);
        // Default color for header icons
        svg.setColorFilter(new FlatSVGIcon.ColorFilter(c -> java.awt.Color.decode("#64748B")));
        label.setIcon(svg);
    }
    private void setNetworkIcon(JMenuItem item, String urlStr, int width, int height) {
        String iconName = getSvgNameFromUrl(urlStr);
        FlatSVGIcon svg = new FlatSVGIcon("images/icon_svg/" + iconName + ".svg", width, height);
        if (iconName.equals("log-out")) {
            svg.setColorFilter(new FlatSVGIcon.ColorFilter(c -> java.awt.Color.decode("#EF4444")));
        } else {
            svg.setColorFilter(new FlatSVGIcon.ColorFilter(c -> java.awt.Color.decode("#64748B")));
        }
        item.setIcon(svg);
    }
    private String getSvgNameFromUrl(String url) {
        if (url.contains("search")) return "search";
        if (url.contains("chat")) return "message-circle";
        if (url.contains("reminders")) return "bell";
        if (url.contains("expand-arrow")) return "chevron-down";
        if (url.contains("user")) return "user";
        if (url.contains("book")) return "book";
        if (url.contains("wallet")) return "wallet";
        if (url.contains("settings")) return "settings";
        if (url.contains("exit")) return "log-out";
        if (url.contains("edit")) return "edit";
        if (url.contains("forward")) return "chevron-down"; // not exact, but ok for now
        return "check-circle-2";
    }
    
    private void setAvatarIcon(JLabel label, String urlStr, int size) { 
        if (urlStr == null || urlStr.trim().isEmpty()) urlStr = "https://img.icons8.com/color/96/circled-user-male-skin-type-4--v1.png"; 
        final String finalUrl = urlStr; String key = "circle_" + finalUrl + "_" + size; 
        if (iconCache.containsKey(key)) { label.setIcon(iconCache.get(key)); return; } 
        new Thread(() -> { 
            try { 
                Image img = finalUrl.startsWith("http") ? new ImageIcon(new URL(finalUrl)).getImage() : new ImageIcon(finalUrl).getImage(); 
                if (img != null) { 
                    MediaTracker tracker = new MediaTracker(label); tracker.addImage(img, 0); tracker.waitForID(0); 
                    java.awt.image.BufferedImage circleBuffer = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB); 
                    Graphics2D g2 = circleBuffer.createGraphics(); 
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); 
                    g2.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, size, size)); 
                    g2.drawImage(img, 0, 0, size, size, null); 
                    g2.dispose(); 
                    ImageIcon circularIcon = new ImageIcon(circleBuffer); iconCache.put(key, circularIcon); 
                    SwingUtilities.invokeLater(() -> { label.setIcon(circularIcon); if (label.getParent() != null) { label.getParent().revalidate(); label.getParent().repaint(); } }); 
                } 
            } catch (Exception e) {} 
        }).start(); 
    }

    class CircleDot extends JPanel { 
        private Color color; 
        public CircleDot(Color color) { this.color = color; setOpaque(false); } 
        @Override protected void paintComponent(Graphics g) { 
            super.paintComponent(g); 
            Graphics2D g2 = (Graphics2D) g.create(); 
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
            int padding = 1; int diameter = getWidth() - padding * 2; 
            g2.setColor(color); g2.fillOval(padding, padding, diameter, diameter); 
            g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(1.5f)); g2.drawOval(padding, padding, diameter, diameter); 
            g2.dispose(); 
        } 
    }

    class PillBadge extends JPanel { 
        public PillBadge(String text, Color bg, Color fg) { 
            setOpaque(false); setBorder(new EmptyBorder(2, 6, 2, 6)); 
            JLabel l = new JLabel(text); l.setFont(new Font("Segoe UI", Font.BOLD, 11)); l.setForeground(fg); add(l); setBackground(bg); 
        } 
        @Override protected void paintComponent(Graphics g) { 
            Graphics2D g2 = (Graphics2D) g.create(); 
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
            g2.setColor(getBackground()); g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight()); 
            g2.dispose(); 
        } 
    }
    
    class AvatarPanel extends JPanel {
        private Image avatarImg;

        public AvatarPanel() {
            setOpaque(false); setPreferredSize(new Dimension(42, 42)); 
        }

        public void setAvatar(Image img) { this.avatarImg = img; repaint(); }
        public Image getAvatarImage() { return this.avatarImg; }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            if (avatarImg != null) {
                int size = 42;
                BufferedImage circleBuffer = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = circleBuffer.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.fillOval(0, 0, size, size);
                g2d.setComposite(AlphaComposite.SrcIn);
                
                int imgW = avatarImg.getWidth(null); 
                int imgH = avatarImg.getHeight(null);
                if (imgW > 0 && imgH > 0) {
                    double scale = Math.max((double) size / imgW, (double) size / imgH);
                    int drawW = (int) (imgW * scale); 
                    int drawH = (int) (imgH * scale);
                    int x = (size - drawW) / 2; 
                    int y = (size - drawH) / 2;
                    Image scaledImage = avatarImg.getScaledInstance(drawW, drawH, Image.SCALE_SMOOTH);
                    g2d.drawImage(scaledImage, x, y, drawW, drawH, null);
                } else {
                    Image scaledImage = avatarImg.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    g2d.drawImage(scaledImage, 0, 0, size, size, null);
                }
                g2d.dispose();
                g2.drawImage(circleBuffer, 0, 0, null);
            }

            int dotSize = 12; int x = getWidth() - dotSize - 2; int y = getHeight() - dotSize - 2;
            g2.setColor(Color.decode("#10B981")); g2.fillOval(x, y, dotSize, dotSize);
            g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(2f)); g2.drawOval(x, y, dotSize, dotSize);
            g2.dispose();
        }
    }
}
