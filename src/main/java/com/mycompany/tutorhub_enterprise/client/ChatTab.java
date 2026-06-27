package com.mycompany.tutorhub_enterprise.client;

import com.mycompany.tutorhub_enterprise.client.services.MessageSyncService;
import com.mycompany.tutorhub_enterprise.models.ConversationInfo;
import com.mycompany.tutorhub_enterprise.models.Message;
import com.mycompany.tutorhub_enterprise.models.MessageStatus;
import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.models.UserInfo;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;



import java.io.File;
import java.util.Map;

public class ChatTab extends JPanel {

    // ===== THEME GIAO DIỆN CHUẨN MOCKUP =====
    private final Color PRIMARY = Color.decode("#2563EB"); 
    private final Color BG_MAIN = Color.decode("#F0F4FA"); // Nền xám xanh nhạt sang trọng
    private final Color BG_LEFT = Color.decode("#FFFFFF"); 
    private final Color BG_ME = Color.decode("#007AFF"); // Tin nhắn của mình màu Xanh Primary
    private final Color BG_OTHER = Color.decode("#FFFFFF"); // Tin nhắn người khác màu Trắng
    private final Color BORDER_COLOR = Color.decode("#E2E8F0");
    private final Color TEXT_MAIN = Color.decode("#0F172A");
    private final Color TEXT_MUTED = Color.decode("#64748B");
    private final Color ONLINE_COLOR = Color.decode("#10B981"); 
    private final Color OFFLINE_COLOR = Color.decode("#94A3B8"); 
    private final Color BADGE_COLOR = Color.decode("#EF4444");

    // ===== DATA MODELS =====
    private List<ConversationInfo> conversations = new ArrayList<>();
    private List<UserInfo> searchResults = new ArrayList<>(); 
    private ConversationInfo activeConversation = null;
    private List<Message> currentMessages = new ArrayList<>();
    
    private int CURRENT_USER_ID;
    private long lastSearchCloseTime = 0;
    private String searchKeyword = "";
    private javax.swing.Timer searchDebounce;
    private Runnable onSwitchToChatCallback; 
    private int selectedPopupIndex = -1;

    // ===== UI COMPONENTS =====
    private JPanel leftListPanel;
    private JPanel centerChatPanel;
    private JPanel messageArea;
    private JScrollPane chatScrollPane;
    private JTextArea txtChatInput; 
    private JLabel lblTypingIndicator;
    private javax.swing.Timer typingTimer;
    private long lastTypingSent = 0;
    private int loadedMessageLimit = 50;
    private final MessageSyncService messageSyncService = MessageSyncService.getInstance();
    
    // Biến hứng Control từ HeaderPanel
    private JTextField txtGlobalSearch;
    private JPanel globalSearchContainer;

    private JPopupMenu searchPopup;
    private JPanel popupContentPanel;
    private List<JPanel> popupClickableItems = new ArrayList<>();
    private int audioCallDurationSeconds = 0;
    
    // --- LAVIE AI INTEGRATION ---
    // --- LAVIE AI INTEGRATION ---
    // ----------------------------
    // ----------------------------

    private String currentFilter = "ALL";
    private static final Map<String, ImageIcon> iconCache = new HashMap<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private final SimpleDateFormat dateSearchFormat = new SimpleDateFormat("dd/MM");
    // ===== LƯU TRỮ EMOJI GẦN ĐÂY =====
    private static java.util.List<String> recentEmojis = new java.util.ArrayList<>();
    // ===== UI COMPONENTS =====
    private java.util.function.IntConsumer onUnreadCountChanged; // Thêm dòng này
    private CardLayout centerCardLayout;
    private JPanel activeChatContainer; // Container mới chứa giao diện nhắn tin

   public ChatTab(int userId) {
        this.CURRENT_USER_ID = userId;
        com.mycompany.tutorhub_enterprise.client.managers.CallManager.getInstance().init(this);
        setLayout(new BorderLayout());
        setBackground(BG_LEFT);

        initSearchPopup();

        searchDebounce = new javax.swing.Timer(300, e -> executeSearch());
        searchDebounce.setRepeats(false);

        JPanel leftColumn = createConversationListColumn();
        JPanel centerColumn = createChatCenterColumn();

        JSplitPane mainSplit = createBorderlessSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftColumn, centerColumn);
        mainSplit.setResizeWeight(0.0); 
        mainSplit.setDividerSize(1); // Mẹo: Bạn có thể đổi số 1 thành số 4 để có thể dùng chuột kéo qua lại nhé

        // ========================================================
        // THÊM 2 DÒNG NÀY ĐỂ ÉP CỨNG ĐỘ RỘNG CỘT TRÁI LÀ 400px
        // ========================================================
        leftColumn.setMinimumSize(new Dimension(400, 0)); 
        mainSplit.setDividerLocation(400); 

        add(mainSplit, BorderLayout.CENTER);

        renderActiveChatStructure();
        fetchConversationListFromServer();
        
        setupDragAndDrop();
        messageSyncService.startPendingRetryTimer();
    }
    
    // HÀM MỚI: CHUYỂN TOÀN BỘ TIN NHẮN ĐANG HIỂN THỊ THÀNH "ĐÃ XEM"
    public void markConversationAsRead(String conversationId) {
        messageSyncService.markConversationAsRead(conversationId);
        SwingUtilities.invokeLater(() -> {
            // Kiểm tra xem mình có đang mở đúng cái đoạn chat đó không
            if (activeConversation != null && String.valueOf(activeConversation.conversationId).equals(conversationId)) {
                if (currentMessages != null) {
                    boolean needsRefresh = false;
                    for (Message m : currentMessages) {
                        // Chỉ đổi những tin nhắn của Mình gửi đi (ME) và đang ở trạng thái chưa xem (isRead = false)
                        if ("ME".equals(m.senderType) && !m.isReadStatus()) {
                            m.markRead();
                            needsRefresh = true;
                        }
                    }
                    // Nếu có tin nhắn được chuyển trạng thái -> Load lại khung chat để hiện chữ Xanh
                    if (needsRefresh) {
                        refreshMessages(); 
                    }
                }
            }
        });
    }
    
    public String uploadToS3Backblaze(File file) {
        String bucketName = com.mycompany.tutorhub_enterprise.utils.B2Helper.getBucketName();
        String publicBaseUrl = com.mycompany.tutorhub_enterprise.utils.B2Helper.getPublicBaseUrl();
        
        try {
            if (!com.mycompany.tutorhub_enterprise.utils.B2Helper.isConfigured()) {
                throw new IllegalStateException("Missing Backblaze B2 config. Set TUTORHUB_B2_BUCKET, TUTORHUB_B2_ACCESS_KEY, TUTORHUB_B2_SECRET_KEY and TUTORHUB_B2_PUBLIC_BASE_URL.");
            }
            software.amazon.awssdk.services.s3.S3Client s3 = com.mycompany.tutorhub_enterprise.utils.B2Helper.getS3Client();
                
            String fileName = "chat/" + System.currentTimeMillis() + "_" + file.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
            long fileSize = file.length();
            long partSize = 5 * 1024 * 1024;
            
            if (fileSize < partSize) {
                software.amazon.awssdk.services.s3.model.PutObjectRequest putOb = software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
                s3.putObject(putOb, software.amazon.awssdk.core.sync.RequestBody.fromFile(file));
            } else {
                software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse createRes = s3.createMultipartUpload(
                    software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(fileName)
                        .build()
                );
                String uploadId = createRes.uploadId();
                long totalChunksLong = (fileSize + partSize - 1) / partSize;
                int totalChunks = (int) totalChunksLong;
                
                java.util.List<software.amazon.awssdk.services.s3.model.CompletedPart> completedParts = new java.util.ArrayList<>();
                java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(8);
                java.util.List<java.util.concurrent.Future<software.amazon.awssdk.services.s3.model.CompletedPart>> futures = new java.util.ArrayList<>();
                
                for (int i = 0; i < totalChunks; i++) {
                    final int partNumber = i + 1;
                    final long offset = i * partSize;
                    final long length = Math.min(partSize, fileSize - offset);
                    
                    futures.add(executor.submit(() -> {
                        java.io.File tempChunk = java.io.File.createTempFile("chunk", ".tmp");
                        tempChunk.deleteOnExit();
                        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r");
                             java.io.FileOutputStream fos = new java.io.FileOutputStream(tempChunk)) {
                            raf.seek(offset);
                            byte[] buffer = new byte[8192];
                            long bytesToRead = length;
                            while (bytesToRead > 0) {
                                int read = raf.read(buffer, 0, (int) Math.min(buffer.length, bytesToRead));
                                if (read == -1) break;
                                fos.write(buffer, 0, read);
                                bytesToRead -= read;
                            }
                        }
                        
                        software.amazon.awssdk.services.s3.model.UploadPartResponse uploadRes = s3.uploadPart(
                            software.amazon.awssdk.services.s3.model.UploadPartRequest.builder()
                                .bucket(bucketName)
                                .key(fileName)
                                .uploadId(uploadId)
                                .partNumber(partNumber)
                                .build(),
                            software.amazon.awssdk.core.sync.RequestBody.fromFile(tempChunk)
                        );
                        
                        tempChunk.delete();
                        return software.amazon.awssdk.services.s3.model.CompletedPart.builder()
                            .partNumber(partNumber)
                            .eTag(uploadRes.eTag())
                            .build();
                    }));
                }
                
                for (java.util.concurrent.Future<software.amazon.awssdk.services.s3.model.CompletedPart> future : futures) {
                    completedParts.add(future.get());
                }
                executor.shutdown();
                
                completedParts.sort((p1, p2) -> Integer.compare(p1.partNumber(), p2.partNumber()));
                s3.completeMultipartUpload(
                    software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(fileName)
                        .uploadId(uploadId)
                        .multipartUpload(software.amazon.awssdk.services.s3.model.CompletedMultipartUpload.builder().parts(completedParts).build())
                        .build()
                );
            }
            return publicBaseUrl.replaceAll("/+$", "") + "/" + fileName;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }



    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
    // Thêm 2 hàm này vào
    public void setOnUnreadCountChanged(java.util.function.IntConsumer listener) {
        this.onUnreadCountChanged = listener;
    }

    private void notifyUnreadCountChanged() {
        if (onUnreadCountChanged != null && conversations != null) {
            int totalUnread = 0;
            for (ConversationInfo c : conversations) {
                totalUnread += c.unreadCount;
            }
            final int finalTotal = totalUnread;
            SwingUtilities.invokeLater(() -> onUnreadCountChanged.accept(finalTotal));
        }
    }

    public void setOnSwitchToChatCallback(Runnable callback) {
        this.onSwitchToChatCallback = callback;
    }

    private void fetchConversationListFromServer() {
        try { NetworkManager.getInstance().sendPacket(new Packet("GET_CONVO_LIST", String.valueOf(CURRENT_USER_ID))); } catch (Exception e) {}
    }

    private void executeSearch() {
        // Cập nhật giao diện Chat ngay khi gõ (Lọc tin nhắn)
        refreshMessages();
        
        if (searchKeyword.isEmpty()) {
            searchPopup.setVisible(false);
            refreshConversationList();
            return;
        }
        renderSearchLoading();
        try { NetworkManager.getInstance().sendPacket(new Packet("SEARCH_USER", searchKeyword)); } catch (Exception e) {}
    }

    public void updateConversationList(List<ConversationInfo> list) {
        this.conversations = list;
        refreshConversationList(); 
        if (searchPopup.isVisible() && !searchKeyword.isEmpty()) renderSearchPopupResults();
        notifyUnreadCountChanged();
    }

    public void updateSearchResults(List<UserInfo> users) {
        this.searchResults = users;
        renderSearchPopupResults();
    }

    private void fetchMessagesFromServer(int conversationId) {
        // --- OFFLINE-FIRST: Lấy tin nhắn từ SQLite (Zero-latency) ---
        java.util.List<Message> localMsgs = messageSyncService.getCachedMessages(String.valueOf(conversationId), loadedMessageLimit, 0);
        if (localMsgs != null && !localMsgs.isEmpty()) {
            this.currentMessages = localMsgs;
            refreshMessages();
        }
        // --- Sau đó gọi API để đồng bộ ---
        try { NetworkManager.getInstance().sendPacket(new Packet("GET_MESSAGES", CURRENT_USER_ID + "|" + conversationId)); } catch (Exception e) {}
    }

    public void updateMessages(List<Message> msgs) {
        List<Message> displayMessages = msgs;
        if (activeConversation != null) {
            String conversationId = String.valueOf(activeConversation.conversationId);
            displayMessages = messageSyncService.mergeServerMessages(conversationId, msgs);
        }
        this.currentMessages = displayMessages;
        refreshMessages();
    }

    private String formatLastSeen(boolean isOnline, Date lastSeen) {
        if (isOnline) return "Online";
        if (lastSeen == null) return "Ngoại tuyến";
        long diff = System.currentTimeMillis() - lastSeen.getTime();
        long mins = diff / 60000;
        if (mins < 1) return "Vừa truy cập";
        if (mins < 60) return "Hoạt động " + mins + " phút trước";
        long hours = mins / 60;
        if (hours < 24) return "Hoạt động " + hours + " giờ trước";
        return "Hoạt động " + (hours / 24) + " ngày trước";
    }

    private String removeAccents(String str) {
        if (str == null) return "";
        String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD); 
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("").replace("đ", "d").replace("Đ", "D").toLowerCase();
    }

    // =========================================================================
    // HOOK VÀO THANH TÌM KIẾM HEADER CỦA MAIN DASHBOARD
    // =========================================================================
    public void bindGlobalSearchBar(JTextField searchInput, JPanel searchContainer) {
        if (searchInput == null || searchContainer == null) return;
        this.txtGlobalSearch = searchInput;
        this.globalSearchContainer = searchContainer;

        txtGlobalSearch.putClientProperty("JTextField.placeholderText", "Tìm kiếm Biểu tượng");

        txtGlobalSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { handleTyping(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { handleTyping(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { }
            private void handleTyping() {
                searchKeyword = txtGlobalSearch.getText().trim().toLowerCase();
                searchDebounce.restart();
            }
        });

        txtGlobalSearch.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (System.currentTimeMillis() - lastSearchCloseTime < 200) return;
                if (!searchKeyword.isEmpty() && !searchPopup.isVisible()) {
                    searchPopup.show(globalSearchContainer, 0, globalSearchContainer.getHeight() + 8);
                }
            }
        });

        txtGlobalSearch.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ESCAPE) { searchPopup.setVisible(false); }
                else if (searchPopup.isVisible() && !popupClickableItems.isEmpty()) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        selectedPopupIndex = (selectedPopupIndex + 1) % popupClickableItems.size();
                        updatePopupSelection();
                    } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                        selectedPopupIndex = (selectedPopupIndex - 1 < 0) ? popupClickableItems.size() - 1 : selectedPopupIndex - 1;
                        updatePopupSelection();
                    } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        if(selectedPopupIndex >= 0) {
                            for(MouseListener ml : popupClickableItems.get(selectedPopupIndex).getMouseListeners()) ml.mouseClicked(null);
                        }
                    }
                }
            }
        });
    }

    // =========================================================================
    // 1. CỘT TRÁI (DANH SÁCH CHAT)
    // =========================================================================
    // =========================================================================
    // 1. CỘT TRÁI (DANH SÁCH CHAT) - ĐÃ TĂNG CHIỀU RỘNG
    // =========================================================================
    private JPanel createConversationListColumn() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_LEFT);
        
        // !!! CHÚ Ý: ĐÃ SỬA 360 THÀNH 450 Ở ĐÂY ĐỂ NỚI RỘNG CỘT DANH SÁCH !!!
        panel.setPreferredSize(new Dimension(400, 0)); 
        
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR));

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(BG_LEFT);
        headerPanel.setBorder(new EmptyBorder(10, 16, 5, 16)); 

        headerPanel.add(createFilterTabs());
        panel.add(headerPanel, BorderLayout.NORTH);

        leftListPanel = new JPanel(); 
        leftListPanel.setLayout(new BoxLayout(leftListPanel, BoxLayout.Y_AXIS));
        leftListPanel.setBackground(BG_LEFT);
        
        JScrollPane scroll = new JScrollPane(leftListPanel); 
        scroll.setBorder(null); 
        scroll.getViewport().setBackground(BG_LEFT);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0)); 
        
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createFilterTabs() {
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); tabs.setOpaque(false);
        tabs.add(createFilterTab("Tất cả", "ALL")); 
        tabs.add(Box.createHorizontalStrut(20));
        tabs.add(createFilterTab("Ưu tiên", "PRIORITY")); 
        tabs.add(Box.createHorizontalStrut(20));
        tabs.add(createFilterTab("Chưa đọc", "UNREAD"));
        return tabs;
    }

    private JPanel createFilterTab(String text, String filterType) {
        JPanel p = new JPanel(new BorderLayout(5, 0)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (currentFilter.equals(filterType)) { Graphics2D g2 = (Graphics2D) g.create(); g2.setColor(PRIMARY); g2.fillRect(0, getHeight() - 2, getWidth(), 2); g2.dispose(); }
            }
        };
        p.setOpaque(false); p.setCursor(new Cursor(Cursor.HAND_CURSOR)); p.setBorder(new EmptyBorder(0, 0, 8, 0));
        JLabel lbl = new JLabel(text); lbl.setFont(new Font("Segoe UI", currentFilter.equals(filterType) ? Font.BOLD : Font.PLAIN, 13));
        lbl.setForeground(currentFilter.equals(filterType) ? PRIMARY : TEXT_MUTED); p.add(lbl, BorderLayout.CENTER);
        p.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { currentFilter = filterType; Container parent = p.getParent(); parent.repaint(); refreshConversationList(); } });
        return p;
    }

    private void refreshConversationList() {
       if (conversations != null) {
            for (ConversationInfo c : conversations) {
                if (c.lastMessage != null) {
                    // THÊM ĐIỀU KIỆN [IMG_URL] Ở DÒNG NÀY
                    if (c.lastMessage.startsWith("[IMG]") || c.lastMessage.startsWith("[IMG_URL]")) {
                        c.lastMessage = "Đã gửi một hình ảnh";
                    } else if (c.lastMessage.contains("[e:")) {
                        c.lastMessage = "Đã gửi một biểu tượng cảm xúc";
                    }
                }
            }
        }
        leftListPanel.removeAll();
        boolean hasItem = false;

        if (conversations != null) {
            for (ConversationInfo c : conversations) {
                if (currentFilter.equals("UNREAD") && c.unreadCount == 0) continue;
                if (currentFilter.equals("PRIORITY") && !c.isPriority) continue;
                leftListPanel.add(createConversationItem(c));
                hasItem = true;
            }
        }

        if (!hasItem) {
            JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setOpaque(false); p.setBorder(new EmptyBorder(40, 20, 20, 20));
            JLabel icon = new JLabel(); setNetworkIcon(icon, "https://img.icons8.com/fluency-systems-regular/48/CBD5E1/chat.png", 48, 48); icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel lblTitle = new JLabel("Hộp thư trống"); lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15)); lblTitle.setForeground(TEXT_MAIN); lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(icon); p.add(Box.createVerticalStrut(15)); p.add(lblTitle);
            leftListPanel.add(p);
        }

        leftListPanel.revalidate(); leftListPanel.repaint();
    }

   private JPanel createConversationItem(ConversationInfo c) {
        boolean isActive = activeConversation != null && activeConversation.conversationId == c.conversationId;
        boolean hasUnread = c.unreadCount > 0; // Biến kiểm tra tin nhắn chưa đọc
        
        JPanel p = new JPanel(new BorderLayout(12, 0)); 
        p.setBackground(isActive ? Color.decode("#E8F0FE") : BG_LEFT);
        p.setBorder(new EmptyBorder(12, 16, 12, 16)); 
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74)); 
        p.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLayeredPane avatarPane = new JLayeredPane(); avatarPane.setPreferredSize(new Dimension(48, 48));
        JLabel lblAvatar = new JLabel(); setAvatarIcon(lblAvatar, c.avatarUrl, 48); lblAvatar.setBounds(0, 0, 48, 48); avatarPane.add(lblAvatar, Integer.valueOf(0));
        JPanel dot = new CircleDot(c.isOnline ? ONLINE_COLOR : OFFLINE_COLOR);
        dot.setBounds(34, 34, 14, 14); avatarPane.add(dot, Integer.valueOf(1));
        p.add(avatarPane, BorderLayout.WEST);

        JPanel center = new JPanel(); center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS)); center.setOpaque(false);
        
        // BÔI ĐẬM VÀ ĐỔI MÀU ĐEN TUYỀN CHO TÊN NẾU CHƯA ĐỌC
        JLabel lblName = new JLabel(c.displayName); 
        lblName.setFont(new Font("Segoe UI", hasUnread ? Font.BOLD : Font.BOLD, 15)); 
        lblName.setForeground(hasUnread ? Color.BLACK : TEXT_MAIN);
        
        String msgContent = c.lastMessage != null ? c.lastMessage : "Bắt đầu trò chuyện!";
        
        // BÔI ĐẬM VÀ ĐỔI MÀU ĐEN TUYỀN CHO NỘI DUNG NẾU CHƯA ĐỌC
        JLabel lblMsg = new JLabel(msgContent); 
        lblMsg.setFont(new Font("Segoe UI", hasUnread ? Font.BOLD : Font.PLAIN, 13)); 
        lblMsg.setForeground(hasUnread ? Color.BLACK : TEXT_MUTED); 
        
        center.add(lblName); center.add(Box.createVerticalStrut(4)); center.add(lblMsg); p.add(center, BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout()); right.setOpaque(false);
        String timeStr = c.lastMessageTime != null ? timeFormat.format(c.lastMessageTime) : "";
        
        // BÔI ĐẬM VÀ ĐỔI MÀU XANH CHO THỜI GIAN NẾU CHƯA ĐỌC
        JLabel lblTime = new JLabel(timeStr); 
        lblTime.setFont(new Font("Segoe UI", hasUnread ? Font.BOLD : Font.PLAIN, 11)); 
        lblTime.setForeground(hasUnread ? PRIMARY : TEXT_MUTED); 
        right.add(lblTime, BorderLayout.NORTH);
        
        if (hasUnread) { 
            JPanel badgeWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); badgeWrapper.setOpaque(false); 
            badgeWrapper.add(new PillBadge(String.valueOf(c.unreadCount), BADGE_COLOR, Color.WHITE)); right.add(badgeWrapper, BorderLayout.SOUTH); 
        }
        p.add(right, BorderLayout.EAST);

        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (!isActive) { p.setBackground(Color.decode("#F8F9FA")); p.repaint(); } }
            @Override public void mouseExited(MouseEvent e) { if (!isActive) { p.setBackground(BG_LEFT); p.repaint(); } }
            @Override public void mouseReleased(MouseEvent e) { 
                setActiveConversation(c); 
                refreshConversationList(); 
            }
        });
        
        return p;
    }

    private void initSearchPopup() {
        searchPopup = new JPopupMenu();
        searchPopup.setFocusable(false); 
        searchPopup.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        searchPopup.setBackground(Color.WHITE);
        
        popupContentPanel = new JPanel();
        popupContentPanel.setLayout(new BoxLayout(popupContentPanel, BoxLayout.Y_AXIS));
        popupContentPanel.setBackground(Color.WHITE);
        
        JScrollPane scroll = new JScrollPane(popupContentPanel);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(360, 420));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(5, 0));
        
        searchPopup.add(scroll);
        searchPopup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {}
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) { 
                lastSearchCloseTime = System.currentTimeMillis(); 
            }
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
        });
    }

    private void renderSearchLoading() {
        if(globalSearchContainer == null) return;
        popupContentPanel.removeAll();
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER)); p.setBackground(Color.WHITE); p.setBorder(new EmptyBorder(30, 0, 30, 0));
        JLabel lbl = new JLabel("Đang tìm kiếm..."); lbl.setFont(new Font("Segoe UI", Font.ITALIC, 13)); lbl.setForeground(TEXT_MUTED);
        p.add(lbl); popupContentPanel.add(p);
        
        searchPopup.pack();
        if(!searchPopup.isVisible()) searchPopup.show(globalSearchContainer, 0, globalSearchContainer.getHeight() + 8);
    }

   private void renderSearchPopupResults() {
        if(globalSearchContainer == null) return;
        popupContentPanel.removeAll();
        popupClickableItems.clear();
        selectedPopupIndex = -1;
        boolean hasAnyResult = false;
        String rawKey = removeAccents(searchKeyword);

        // 1. ƯU TIÊN KIỂM TRA TIN NHẮN TRONG ĐOẠN CHAT HIỆN TẠI (LOCAL MATCH)
        int localMatchCount = 0;
        if (activeConversation != null && currentMessages != null && !searchKeyword.isEmpty()) {
            for (Message m : currentMessages) {
                if (m.content != null && m.content.toLowerCase().contains(searchKeyword)) {
                    localMatchCount++;
                }
            }
        }

        // NẾU CÓ TIN NHẮN PHÙ HỢP -> HIỂN THỊ NÚT TRUY CẬP NHANH VÀO CHAT
        if (localMatchCount > 0) {
            popupContentPanel.add(createPopupSectionTitle("TRONG ĐOẠN CHAT HIỆN TẠI"));
            JPanel pLocal = new JPanel(new BorderLayout(12, 0)); 
            pLocal.setBackground(Color.WHITE); 
            pLocal.setBorder(new EmptyBorder(8, 15, 8, 15)); 
            pLocal.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50)); 
            pLocal.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            JLabel lblIcon = new JLabel(); 
            setNetworkIcon(lblIcon, "https://img.icons8.com/fluency-systems-regular/48/2563EB/search-message.png", 28, 28); 
            pLocal.add(lblIcon, BorderLayout.WEST);
            
            JLabel lblText = new JLabel("<html>Tìm thấy <b style='color:#2563EB;'>" + localMatchCount + "</b> tin nhắn phù hợp (Nhấn để xem)</html>"); 
            lblText.setFont(new Font("Segoe UI", Font.PLAIN, 13)); 
            lblText.setForeground(TEXT_MAIN); 
            pLocal.add(lblText, BorderLayout.CENTER);

            // Bấm vào kết quả này sẽ ĐÓNG Popup lại để người dùng xem tin nhắn
            pLocal.addMouseListener(new MouseAdapter() { 
                @Override public void mouseEntered(MouseEvent e) { pLocal.setBackground(Color.decode("#F1F5F9")); } 
                @Override public void mouseExited(MouseEvent e) { pLocal.setBackground(Color.WHITE); } 
                @Override public void mouseClicked(MouseEvent e) { searchPopup.setVisible(false); } 
            });
            
            popupClickableItems.add(pLocal);
            popupContentPanel.add(pLocal);
            hasAnyResult = true;
        }

        // 2. TÌM TRONG LỊCH SỬ BẠN BÈ / NHÓM CHAT (BÊN TRÁI)
        List<ConversationInfo> localMatches = new ArrayList<>();
        if (conversations != null && !searchKeyword.isEmpty()) {
            for(ConversationInfo c : conversations) {
                if (removeAccents(c.displayName).contains(rawKey) || 
                   (c.lastMessage != null && removeAccents(c.lastMessage).contains(rawKey))) {
                    localMatches.add(c);
                }
            }
        }

        if (!localMatches.isEmpty()) {
            if (hasAnyResult) popupContentPanel.add(createPopupSeparator());
            popupContentPanel.add(createPopupSectionTitle("TIN NHẮN & BẠN BÈ KHÁC"));
            for (ConversationInfo c : localMatches) {
                JPanel item = createPopupMessageItem(c);
                popupClickableItems.add(item);
                popupContentPanel.add(item);
            }
            hasAnyResult = true;
        }

        // 3. TÌM KIẾM NGƯỜI DÙNG LẠ TỪ SERVER
        if (searchResults != null && !searchResults.isEmpty()) {
            if (hasAnyResult) popupContentPanel.add(createPopupSeparator());
            popupContentPanel.add(createPopupSectionTitle("KẾT QUẢ TỪ HỆ THỐNG"));
            for (UserInfo u : searchResults) {
                boolean isAlreadyFriend = localMatches.stream().anyMatch(c -> removeAccents(c.displayName).equals(removeAccents(u.fullName)));
                if(!isAlreadyFriend) {
                    JPanel item = createPopupUserItem(u);
                    popupClickableItems.add(item);
                    popupContentPanel.add(item);
                    hasAnyResult = true;
                }
            }
        }

        // 4. NẾU KHÔNG CÓ BẤT KỲ KẾT QUẢ NÀO Ở CẢ 3 MỤC TRÊN -> MỚI BÁO LỖI
        if (!hasAnyResult) {
            JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setBackground(Color.WHITE); p.setBorder(new EmptyBorder(40, 20, 40, 20));
            JLabel icon = new JLabel(); setNetworkIcon(icon, "https://img.icons8.com/fluency-systems-regular/48/CBD5E1/search.png", 36, 36); icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel lbl1 = new JLabel("Không tìm thấy kết quả phù hợp"); lbl1.setFont(new Font("Segoe UI", Font.BOLD, 13)); lbl1.setForeground(TEXT_MAIN); lbl1.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(icon); p.add(Box.createVerticalStrut(10)); p.add(lbl1);
            popupContentPanel.add(p);
        }

        searchPopup.pack();
        if(!searchPopup.isVisible()) searchPopup.show(globalSearchContainer, 0, globalSearchContainer.getHeight() + 8);
    }
    
    private void updatePopupSelection() {
        for(int i = 0; i < popupClickableItems.size(); i++) {
            int selectedPopupIndex = 0;
            popupClickableItems.get(i).setBackground(i == selectedPopupIndex ? Color.decode("#F1F5F9") : Color.WHITE);
        }
        popupContentPanel.repaint();
    }

    private JPanel createPopupSectionTitle(String title) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0)); p.setBackground(Color.WHITE); p.setBorder(new EmptyBorder(12, 0, 4, 0));
        JLabel l = new JLabel(title); l.setFont(new Font("Segoe UI", Font.BOLD, 11)); l.setForeground(TEXT_MUTED); p.add(l);
        return p;
    }

    private JSeparator createPopupSeparator() { JSeparator sep = new JSeparator(); sep.setForeground(BORDER_COLOR); return sep; }

    private JPanel createPopupUserItem(UserInfo u) {
        JPanel p = new JPanel(new BorderLayout(12, 0)); p.setBackground(Color.WHITE); p.setBorder(new EmptyBorder(8, 15, 8, 15)); p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60)); p.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JLayeredPane avatarPane = new JLayeredPane(); avatarPane.setPreferredSize(new Dimension(38, 38));
        JLabel lblAvatar = new JLabel(); setAvatarIcon(lblAvatar, u.avatarUrl, 38); lblAvatar.setBounds(0, 0, 38, 38); avatarPane.add(lblAvatar, Integer.valueOf(0));
        JPanel dot = new CircleDot(u.isOnline ? ONLINE_COLOR : OFFLINE_COLOR); dot.setBounds(26, 26, 12, 12); avatarPane.add(dot, Integer.valueOf(1)); 
        p.add(avatarPane, BorderLayout.WEST);
        
        JPanel center = new JPanel(); center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS)); center.setOpaque(false);
        JLabel lblName = new JLabel("<html>" + highlightKeyword(u.fullName, searchKeyword) + "</html>"); lblName.setFont(new Font("Segoe UI", Font.BOLD, 13)); lblName.setForeground(TEXT_MAIN);
        JLabel lblEmail = new JLabel("<html>" + highlightKeyword(u.email, searchKeyword) + "</html>"); lblEmail.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblEmail.setForeground(TEXT_MUTED);
        center.add(lblName); center.add(Box.createVerticalStrut(2)); center.add(lblEmail); p.add(center, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); right.setOpaque(false);
        JLabel lblStatus = new JLabel(u.friendshipStatus.equals("FRIEND") ? "Bạn bè" : (u.isOnline ? "Online" : "Offline"));
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11)); lblStatus.setForeground(u.isOnline ? ONLINE_COLOR : TEXT_MUTED); right.add(lblStatus);
        p.add(right, BorderLayout.EAST);

        p.addMouseListener(new MouseAdapter() { 
            @Override public void mouseEntered(MouseEvent e) { p.setBackground(Color.decode("#F1F5F9")); int selectedPopupIndex = popupClickableItems.indexOf(p);
updatePopupSelection(); } 
            @Override public void mouseExited(MouseEvent e) { p.setBackground(Color.WHITE); } 
            @Override public void mouseClicked(MouseEvent e) { openChatWithStranger(u); }
        });
        return p;
    }

    private JPanel createPopupMessageItem(ConversationInfo c) {
        JPanel p = new JPanel(new BorderLayout(12, 0)); p.setBackground(Color.WHITE); p.setBorder(new EmptyBorder(8, 15, 8, 15)); p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60)); p.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JLayeredPane avatarPane = new JLayeredPane(); avatarPane.setPreferredSize(new Dimension(38, 38));
        JLabel lblAvatar = new JLabel(); setAvatarIcon(lblAvatar, c.avatarUrl, 38); lblAvatar.setBounds(0, 0, 38, 38); avatarPane.add(lblAvatar, Integer.valueOf(0));
        JPanel dot = new CircleDot(c.isOnline ? ONLINE_COLOR : OFFLINE_COLOR); dot.setBounds(26, 26, 12, 12); avatarPane.add(dot, Integer.valueOf(1)); 
        p.add(avatarPane, BorderLayout.WEST);
        
        JPanel center = new JPanel(); center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS)); center.setOpaque(false);
        JLabel lblName = new JLabel("<html>" + highlightKeyword(c.displayName, searchKeyword) + "</html>"); lblName.setFont(new Font("Segoe UI", Font.BOLD, 13)); lblName.setForeground(TEXT_MAIN);
        String msgPreview = c.lastMessage != null ? c.lastMessage : "";
        JLabel lblMsg = new JLabel("<html>" + highlightKeyword(msgPreview, searchKeyword) + "</html>"); lblMsg.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lblMsg.setForeground(TEXT_MUTED);
        center.add(lblName); center.add(Box.createVerticalStrut(2)); center.add(lblMsg); p.add(center, BorderLayout.CENTER);

        if(c.lastMessageTime != null) {
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); right.setOpaque(false);
            JLabel lblTime = new JLabel(dateSearchFormat.format(c.lastMessageTime)); lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 10)); lblTime.setForeground(Color.decode("#94A3B8"));
            right.add(lblTime); p.add(right, BorderLayout.EAST);
        }

        p.addMouseListener(new MouseAdapter() { 
            @Override public void mouseEntered(MouseEvent e) { p.setBackground(Color.decode("#F1F5F9")); int selectedPopupIndex = popupClickableItems.indexOf(p);
updatePopupSelection(); } 
            @Override public void mouseExited(MouseEvent e) { p.setBackground(Color.WHITE); } 
            @Override public void mouseClicked(MouseEvent e) { openChatWith(c); }
        });
        return p;
    }

    private String highlightKeyword(String text, String keyword) {
        if (text == null || keyword == null || keyword.isEmpty()) return text;
        return text.replaceAll("(?i)(" + keyword + ")", "<font color='#2563EB'><b>$1</b></font>");
    }

    private void openChatWith(ConversationInfo c) {
        searchPopup.setVisible(false);
        if(txtGlobalSearch != null) txtGlobalSearch.setText("");
        setActiveConversation(c);
        if(txtChatInput != null) txtChatInput.requestFocusInWindow();
        if(onSwitchToChatCallback != null) onSwitchToChatCallback.run();
    }
    
    private void openChatWithStranger(UserInfo u) {
        searchPopup.setVisible(false);
        if(txtGlobalSearch != null) txtGlobalSearch.setText("");
        ConversationInfo temp = new ConversationInfo();
        temp.conversationId = -u.userId; 
        temp.displayName = u.fullName;
        temp.avatarUrl = u.avatarUrl;
        temp.isOnline = u.isOnline;
        temp.lastMessageTime = new Date();
        setActiveConversation(temp);
        if(txtChatInput != null) txtChatInput.requestFocusInWindow();
        if(onSwitchToChatCallback != null) onSwitchToChatCallback.run(); 
    }

    // =========================================================================
    // 2. CỘT GIỮA (KHUNG CHAT CHÍNH)
    // =========================================================================
   private JPanel createChatCenterColumn() {
        centerCardLayout = new CardLayout();
        centerChatPanel = new JPanel(centerCardLayout); 
        centerChatPanel.setBackground(BG_MAIN); 

        // --- LÁ BÀI 1: MÀN HÌNH CHÀO MỪNG (SLIDER) ---
        JPanel welcomeWrapper = new JPanel(new BorderLayout());
        welcomeWrapper.setBackground(BG_MAIN);
        welcomeWrapper.add(new WelcomeSliderPanel(), BorderLayout.CENTER);
        
        // --- LÁ BÀI 2: KHUNG CHAT THỰC TẾ ---
        activeChatContainer = new JPanel(new BorderLayout());
        activeChatContainer.setBackground(BG_MAIN);

        // Nạp 2 lá bài vào hệ thống (gắn tên để dễ gọi)
        centerChatPanel.add(welcomeWrapper, "WELCOME_CARD");
        centerChatPanel.add(activeChatContainer, "CHAT_CARD");

        return centerChatPanel;
    }
    // =========================================================================
    // HÀM RENDER (LẬT BÀI CARDLAYOUT - KHÔNG CHE TIN NHẮN)
    // =========================================================================
   // =========================================================================
    // HÀM RENDER (LẬT BÀI CARDLAYOUT - BẢN FULL CODE 100%)
    // =========================================================================
    // =========================================================================
    // HÀM RENDER (LẬT BÀI CARDLAYOUT - ĐÃ FIX TRÀN VIỀN NGANG)
    // =========================================================================
    private void renderActiveChatStructure() {
        // 1. NẾU CHƯA CHỌN AI -> LẬT LÁ BÀI SLIDER LÊN RỒI DỪNG LẠI
        if (activeConversation == null) {
            if (centerCardLayout != null) centerCardLayout.show(centerChatPanel, "WELCOME_CARD");
            return;
        }

        // 2. NẾU ĐÃ CHỌN NGƯỜI -> LẬT LÁ BÀI CHAT LÊN
        if (centerCardLayout != null) centerCardLayout.show(centerChatPanel, "CHAT_CARD");

        // Xóa sạch khung chat cũ trước khi vẽ người mới
        activeChatContainer.removeAll(); 

        // ==========================================
        // KHU VỰC 1: HEADER (ẢNH ĐẠI DIỆN & NÚT GỌI)
        // ==========================================
        JPanel header = new JPanel(new BorderLayout()); 
        header.setBackground(Color.WHITE); 
        header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR), new EmptyBorder(12, 24, 12, 24)));
        
        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0)); 
        leftHeader.setOpaque(false);
        
        JLayeredPane avatarPane = new JLayeredPane(); 
        avatarPane.setPreferredSize(new Dimension(46, 46));
        JLabel lblAvatar = new JLabel(); 
        setAvatarIcon(lblAvatar, activeConversation.avatarUrl, 46);
        lblAvatar.setBounds(0, 0, 46, 46); 
        avatarPane.add(lblAvatar, Integer.valueOf(0));
        JPanel dot = new CircleDot(activeConversation.isOnline ? ONLINE_COLOR : OFFLINE_COLOR);
        dot.setBounds(33, 33, 13, 13); 
        avatarPane.add(dot, Integer.valueOf(1));

        JPanel info = new JPanel(); 
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS)); 
        info.setOpaque(false);
        JLabel lblName = new JLabel(activeConversation.displayName); 
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 18)); 
        lblName.setForeground(TEXT_MAIN);
        
        String statusStr = activeConversation.conversationId < 0 ? "Người dùng hệ thống" : formatLastSeen(activeConversation.isOnline, activeConversation.lastMessageTime);
        JLabel lblStatus = new JLabel(statusStr); 
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13)); 
        lblStatus.setForeground(activeConversation.isOnline ? ONLINE_COLOR : TEXT_MUTED);
        
        info.add(lblName); 
        info.add(Box.createVerticalStrut(2)); 
        info.add(lblStatus); 
        leftHeader.add(avatarPane); 
        leftHeader.add(info);

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0)); 
        rightHeader.setOpaque(false);
        
        JPanel btnSearch = createActionIcon("https://img.icons8.com/fluency-systems-regular/48/64748B/search.png"); 
        btnSearch.addMouseListener(new MouseAdapter() { 
            @Override public void mouseClicked(MouseEvent e) { 
                if (txtGlobalSearch != null) txtGlobalSearch.requestFocusInWindow(); 
            } 
        });
        rightHeader.add(btnSearch); 
        
        JPanel btnAudioCall = createActionIcon("https://img.icons8.com/fluency-systems-regular/48/64748B/phone.png"); 
        btnAudioCall.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { startAudioCall(); } });
        rightHeader.add(btnAudioCall); 
        
        JPanel btnVideoCall = createActionIcon("https://img.icons8.com/fluency-systems-regular/48/64748B/video-call.png"); 
        btnVideoCall.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { startVideoCall(); } });
        rightHeader.add(btnVideoCall); 
        
        header.add(leftHeader, BorderLayout.WEST); 
        header.add(rightHeader, BorderLayout.EAST); 
        activeChatContainer.add(header, BorderLayout.NORTH);

        // ==========================================
        // KHU VỰC 2: CUỘN TIN NHẮN (ĐÃ FIX TRÀN VIỀN BẰNG WRAPPER)
        // ==========================================
        messageArea = new JPanel(); 
        messageArea.setLayout(new BoxLayout(messageArea, BoxLayout.Y_AXIS)); 
        messageArea.setBackground(BG_MAIN); 
        messageArea.setBorder(new EmptyBorder(15, 30, 15, 30));
        
        // --- LỚP KHIÊN BẢO VỆ: Ép khung chat dính lên trên và KHÓA CHIỀU NGANG ---
        JPanel scrollContentWrapper = new JPanel(new BorderLayout());
        scrollContentWrapper.setBackground(BG_MAIN);
        scrollContentWrapper.add(messageArea, BorderLayout.NORTH); 

        // Nạp Wrapper vào ScrollPane thay vì nạp trực tiếp messageArea
        chatScrollPane = new JScrollPane(scrollContentWrapper); 
        chatScrollPane.setBorder(null); 
        chatScrollPane.getViewport().setBackground(BG_MAIN); 
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        chatScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        
        // --- CHỐT CHẶN CUỐI CÙNG: CẤM CUỘN NGANG ---
        chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        activeChatContainer.add(chatScrollPane, BorderLayout.CENTER);

        // ==========================================
        // KHU VỰC 3: PHÍA DƯỚI (NÚT CUỘN & THANH INPUT)
        // ==========================================
        JPanel btnScrollDown = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 230)); 
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(BORDER_COLOR);
                g2.drawOval(0, 0, getWidth()-1, getHeight()-1);
                g2.dispose();
            }
        };
        btnScrollDown.setOpaque(false);
        btnScrollDown.setPreferredSize(new Dimension(36, 36));
        btnScrollDown.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JLabel iconDown = new JLabel();
        setNetworkIcon(iconDown, "https://img.icons8.com/fluency-systems-filled/48/2563EB/down.png", 20, 20);
        btnScrollDown.add(iconDown, BorderLayout.CENTER);
        btnScrollDown.setVisible(false); 
        
        btnScrollDown.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                JScrollBar vBar = chatScrollPane.getVerticalScrollBar();
                vBar.setValue(vBar.getMaximum());
            }
        });

        chatScrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            JScrollBar vBar = (JScrollBar) e.getAdjustable();
            int max = vBar.getMaximum() - vBar.getVisibleAmount();
            if (vBar.getValue() < max - 150) {
                btnScrollDown.setVisible(true);
            } else {
                btnScrollDown.setVisible(false);
            }
        });

        JPanel scrollOverlay = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        scrollOverlay.setOpaque(false);
        scrollOverlay.add(btnScrollDown);
        
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setOpaque(false);
        southPanel.add(scrollOverlay, BorderLayout.NORTH); 
        southPanel.add(createMessageInputBar(), BorderLayout.SOUTH); 

        activeChatContainer.add(southPanel, BorderLayout.SOUTH);

        // Refresh lại giao diện lá bài Chat
        activeChatContainer.revalidate(); 
        activeChatContainer.repaint();
    }
   private void refreshMessages() {
        if (messageArea == null) return;
        messageArea.removeAll(); 
        
        messageArea.add(createDateDivider("Hôm nay"));
        JPanel firstMatchedPanel = null; 

        if (currentMessages != null) {
            if (currentMessages.size() > loadedMessageLimit) {
                JPanel loadMorePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                loadMorePanel.setOpaque(false);
                JButton btnLoadMore = new JButton("Tải thêm tin nhắn cũ");
                btnLoadMore.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                btnLoadMore.setForeground(TEXT_MAIN);
                btnLoadMore.setCursor(new Cursor(Cursor.HAND_CURSOR));
                btnLoadMore.addActionListener(e -> {
                    loadedMessageLimit += 50;
                    refreshMessages();
                });
                loadMorePanel.add(btnLoadMore);
                messageArea.add(loadMorePanel);
            }

            int startIndex = Math.max(0, currentMessages.size() - loadedMessageLimit);
            String lastSenderType = "";
            for (int i = startIndex; i < currentMessages.size(); i++) {
                Message m = currentMessages.get(i);
                boolean isFirstInGroup = !m.senderType.equals(lastSenderType);
                
                JPanel msgPanel;
                
                // ĐÃ XÓA NHÁNH FILE CŨ! CHỈ CÒN NHÁNH VIDEO/AUDIO CALL VÀ TIN NHẮN THƯỜNG
                if ("VIDEO_CALL".equals(m.messageType) || "AUDIO_CALL".equals(m.messageType)) {
                    msgPanel = createCallBubble(m, isFirstInGroup);
                } else {
                    // MỌI TIN NHẮN CHỮ, EMOJI, HÌNH ẢNH VÀ FILE ĐỀU ĐI QUA ĐÂY
                    msgPanel = "ME".equals(m.senderType) ? createOutgoingMessage(m, isFirstInGroup) : createIncomingMessage(m, isFirstInGroup); 
                }

                if (!searchKeyword.isEmpty() && m.content != null && !"IMAGE".equals(m.messageType) && m.content.toLowerCase().contains(searchKeyword)) {
                    if (firstMatchedPanel == null) firstMatchedPanel = msgPanel;
                }

                messageArea.add(msgPanel);
                messageArea.add(Box.createVerticalStrut(isFirstInGroup ? 16 : 4)); 
                lastSenderType = m.senderType;
            }
        }
        messageArea.revalidate(); 
        messageArea.repaint();
        
        final JPanel targetPanel = firstMatchedPanel;
        SwingUtilities.invokeLater(() -> { 
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar(); 
            if (targetPanel != null && !searchKeyword.isEmpty()) {
                chatScrollPane.getViewport().setViewPosition(new Point(0, targetPanel.getY() - 50));
            } else if (searchKeyword.isEmpty()) {
                vertical.setValue(vertical.getMaximum()); 
            }
        });
    }

    // =========================================================================
    // KHU VỰC NHẬP TIN NHẮN CHUẨN SAAS
    // =========================================================================
    private JPanel createMessageInputBar() {
        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setBackground(BG_MAIN);
        footer.setBorder(new EmptyBorder(10, 20, 15, 20)); 

        JPanel inputBubble = new JPanel(new BorderLayout(10, 0)) {
            private boolean isFocused = false;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); 
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); 
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24); 
                g2.setColor(isFocused ? PRIMARY : Color.decode("#CBD5E1")); 
                g2.setStroke(new BasicStroke(isFocused ? 1.5f : 1.0f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 24, 24); 
                g2.dispose();
            }
            public void setFocused(boolean f) { this.isFocused = f; repaint(); }
        };
        inputBubble.setOpaque(false);
        inputBubble.setBorder(new EmptyBorder(6, 16, 6, 10));

        JPanel leftIcons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftIcons.setOpaque(false);
        
        JPanel btnPlus = createActionIcon("https://img.icons8.com/fluency-systems-regular/48/64748B/plus-math.png");
        
        // Gắn sự kiện CHỌN ẢNH vào icon Ảnh
        JPanel btnImage = createActionIcon("https://img.icons8.com/fluency-systems-regular/48/64748B/image.png");
        btnImage.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { chooseAndSendImage(); }
        });
        
       JPanel btnAttach = createActionIcon("https://img.icons8.com/fluency-systems-regular/48/64748B/attach.png");
        
        // --- BƯỚC 3: BẮT SỰ KIỆN CLICK VÀO NÚT GHIM GIẤY (GỬI TỆP) ---
        btnAttach.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (activeConversation == null || activeConversation.conversationId < 0) {
                    JOptionPane.showMessageDialog(ChatTab.this, "Vui lòng chọn một người bạn để gửi tài liệu!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Chọn tài liệu để gửi...");
                
                fc.setMultiSelectionEnabled(true);
                // Bạn có thể lọc định dạng nếu muốn, ở đây cho phép chọn tất cả
                if (fc.showOpenDialog(ChatTab.this) == JFileChooser.APPROVE_OPTION) {
                    uploadDocumentFiles(fc.getSelectedFiles());
                }
            }
        });
        leftIcons.add(btnPlus);
        leftIcons.add(btnImage);
        leftIcons.add(btnAttach);
        inputBubble.add(leftIcons, BorderLayout.WEST);

        txtChatInput = new JTextArea();
        txtChatInput.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtChatInput.setBackground(new Color(0,0,0,0)); 
        txtChatInput.setBorder(null);
        txtChatInput.setForeground(TEXT_MAIN);
        txtChatInput.setLineWrap(true);
        txtChatInput.setWrapStyleWord(true);
        txtChatInput.setOpaque(false);
        
        txtChatInput.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { try { inputBubble.getClass().getMethod("setFocused", boolean.class).invoke(inputBubble, true); } catch(Exception ex){} }
            @Override public void focusLost(FocusEvent e) { try { inputBubble.getClass().getMethod("setFocused", boolean.class).invoke(inputBubble, false); } catch(Exception ex){} }
        });
        
        txtChatInput.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                if (activeConversation != null) {
                    long now = System.currentTimeMillis();
                    if (now - lastTypingSent > 3000) {
                        try {
                            NetworkManager.getInstance().sendPacket(new Packet("TYPING", activeConversation.conversationId));
                            lastTypingSent = now;
                        } catch (Exception ex) {}
                    }
                }
            }
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == java.awt.event.KeyEvent.VK_V) {
                    try {
                        java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        if (clipboard.isDataFlavorAvailable(java.awt.datatransfer.DataFlavor.imageFlavor)) {
                            Image img = (Image) clipboard.getData(java.awt.datatransfer.DataFlavor.imageFlavor);
                            if (img != null) {
                                java.io.File tempFile = java.io.File.createTempFile("clipboard_", ".png");
                                java.awt.image.BufferedImage bImg = new java.awt.image.BufferedImage(img.getWidth(null), img.getHeight(null), java.awt.image.BufferedImage.TYPE_INT_ARGB);
                                Graphics2D g = bImg.createGraphics();
                                g.drawImage(img, 0, 0, null);
                                g.dispose();
                                javax.imageio.ImageIO.write(bImg, "png", tempFile);
                                uploadImageFiles(new java.io.File[]{tempFile});
                                e.consume();
                                return;
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    if (e.isShiftDown()) {
                        txtChatInput.append("\n");
                    } else {
                        e.consume(); 
                        sendCurrentMessage();
                    }
                }
            }
        });
        
        JScrollPane scrollInput = new JScrollPane(txtChatInput);
        scrollInput.setBorder(null);
        scrollInput.setOpaque(false);
        scrollInput.getViewport().setOpaque(false);
        scrollInput.setPreferredSize(new Dimension(0, 45)); 
        
        inputBubble.add(scrollInput, BorderLayout.CENTER);
        
        // Thêm Icon Emoji vào cuối khung chat
        JPanel btnEmoji = createActionIcon("https://img.icons8.com/fluency-systems-regular/48/64748B/happy.png");
        btnEmoji.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { showEmojiPicker(btnEmoji); }
        });
        inputBubble.add(btnEmoji, BorderLayout.EAST);
        
        lblTypingIndicator = new JLabel(" ");
        lblTypingIndicator.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblTypingIndicator.setForeground(TEXT_MUTED);
        lblTypingIndicator.setBorder(new EmptyBorder(0, 10, 5, 0));
        footer.add(lblTypingIndicator, BorderLayout.NORTH);
        
        footer.add(inputBubble, BorderLayout.CENTER);

        // Nút Gửi Tin Nhắn
        JPanel btnSendWrapper = new JPanel() {
            private Image iconImage; private boolean isHover = false;
            {
                try { URL iconUrl = getClass().getResource("/images/icon/iconnutgui.png"); if (iconUrl != null) iconImage = new ImageIcon(iconUrl).getImage(); } catch (Exception e) {}
                setOpaque(false); setPreferredSize(new Dimension(46, 46)); setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g); Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (iconImage != null) { int drawSize = 64; int offset = (getWidth() - drawSize) / 2; g2.drawImage(iconImage, offset, offset, drawSize, drawSize, null); } 
                else { g2.setColor(PRIMARY); g2.fillOval(0, 0, getWidth(), getHeight()); }
                if (isHover) { g2.setColor(new Color(255, 255, 255, 50)); g2.fillOval(2, 2, getWidth()-4, getHeight()-4); } g2.dispose();
            }
            public void setHover(boolean h) { this.isHover = h; repaint(); }
        };

        btnSendWrapper.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { try { btnSendWrapper.getClass().getMethod("setHover", boolean.class).invoke(btnSendWrapper, true); } catch(Exception ex){} }
            @Override public void mouseExited(MouseEvent e) { try { btnSendWrapper.getClass().getMethod("setHover", boolean.class).invoke(btnSendWrapper, false); } catch(Exception ex){} }
            @Override public void mousePressed(MouseEvent e) { btnSendWrapper.setLocation(btnSendWrapper.getX(), btnSendWrapper.getY() + 1); } 
            @Override public void mouseReleased(MouseEvent e) { btnSendWrapper.setLocation(btnSendWrapper.getX(), btnSendWrapper.getY() - 1); }
            @Override public void mouseClicked(MouseEvent e) { sendCurrentMessage(); }
        });

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightActions.setOpaque(false);
        rightActions.add(Box.createHorizontalStrut(5));
        rightActions.add(btnSendWrapper);
        footer.add(rightActions, BorderLayout.EAST);

        return footer;
    }

    private JPanel createActionIcon(String iconUrl) {
        JPanel p = new JPanel(new BorderLayout()) {
            boolean hover = false;
            @Override protected void paintComponent(Graphics g) {
                if(hover) {
                    Graphics2D g2 = (Graphics2D) g.create(); 
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.decode("#E2E8F0")); 
                    g2.fillOval(0, 0, getWidth(), getHeight()); 
                    g2.dispose();
                }
            }
            public void setHover(boolean h) { this.hover = h; repaint(); }
        };
        p.setOpaque(false); 
        p.setPreferredSize(new Dimension(32, 32)); 
        p.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JLabel lbl = new JLabel("", SwingConstants.CENTER); 
        setNetworkIcon(lbl, iconUrl, 18, 18); 
        p.add(lbl, BorderLayout.CENTER);
        
        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { try { p.getClass().getMethod("setHover", boolean.class).invoke(p, true); } catch(Exception ex){} }
            @Override public void mouseExited(MouseEvent e) { try { p.getClass().getMethod("setHover", boolean.class).invoke(p, false); } catch(Exception ex){} }
        });
        return p;
    }

    // =========================================================================
    // UI TIN NHẮN THEO THIẾT KẾ MỚI
    // =========================================================================
   private JPanel createIncomingMessage(Message m, boolean isFirstInGroup) {
        JPanel p = new JPanel(new BorderLayout(12, 0)); 
        p.setOpaque(false); 
        // !!! CHÚ Ý: ĐÃ SỬA 200 THÀNH 9999 Ở ĐÂY !!!
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 9999)); 
        
        JPanel avatarWrap = new JPanel(new BorderLayout()); 
        avatarWrap.setOpaque(false); 
        avatarWrap.setPreferredSize(new Dimension(36, 36));
        if (isFirstInGroup) { 
            JLabel lblAvatar = new JLabel(); 
            setAvatarIcon(lblAvatar, activeConversation.avatarUrl, 36); 
            avatarWrap.add(lblAvatar, BorderLayout.NORTH); 
        }
        p.add(avatarWrap, BorderLayout.WEST);
        
        JPanel bubbleWrap = new JPanel(); 
        bubbleWrap.setLayout(new BoxLayout(bubbleWrap, BoxLayout.Y_AXIS)); 
        bubbleWrap.setOpaque(false);
        boolean isImage = m.content != null && (m.content.startsWith("[IMG]") || m.content.startsWith("[IMG_URL]"));
        boolean isFile = m.content != null && m.content.startsWith("[FILE]");

        RoundedPanel bubble = new RoundedPanel(20, isImage ? new Color(0,0,0,0) : Color.WHITE); 
        if (isImage) bubble.setBorder(new EmptyBorder(0, 0, 0, 0));
        else bubble.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.decode("#E5E7EB"), 1), new EmptyBorder(10, 16, 10, 16)));
        bubble.setLayout(new BorderLayout());
        
        if (isImage) {
            String cacheKey = "CHAT_IMG_" + m.content.hashCode();
            String cacheKeyOrig = "CHAT_ORIG_" + m.content.hashCode();
            
            if (iconCache.containsKey(cacheKey) && iconCache.containsKey(cacheKeyOrig)) {
                ImageIcon roundedIcon = iconCache.get(cacheKey);
                ImageIcon originalIcon = iconCache.get(cacheKeyOrig);
                JLabel lblImg = new JLabel();
                lblImg.setPreferredSize(new Dimension(roundedIcon.getIconWidth(), roundedIcon.getIconHeight()));
                lblImg.setIcon(roundedIcon);
                lblImg.setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                JPopupMenu imgMenu = new JPopupMenu();
                JMenuItem mnuCopy = new JMenuItem("  Sao chép hình ảnh");
                mnuCopy.addActionListener(ev -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageSelection(originalIcon.getImage()), null));
                JMenuItem mnuSave = new JMenuItem("  Lưu ảnh về máy...");
                mnuSave.addActionListener(ev -> {
                    JFileChooser fc = new JFileChooser(); fc.setDialogTitle("Chọn nơi lưu ảnh");
                    if (fc.showSaveDialog(ChatTab.this) == JFileChooser.APPROVE_OPTION) {
                        try {
                            java.io.File f = fc.getSelectedFile();
                            if(!f.getName().toLowerCase().endsWith(".png")) f = new java.io.File(f.getAbsolutePath() + ".png");
                            javax.imageio.ImageIO.write((java.awt.image.BufferedImage)originalIcon.getImage(), "png", f);
                            JOptionPane.showMessageDialog(ChatTab.this, "Đã lưu ảnh thành công!");
                        } catch(Exception ex) { JOptionPane.showMessageDialog(ChatTab.this, "Lỗi khi lưu ảnh!"); }
                    }
                });
                imgMenu.add(mnuCopy); imgMenu.addSeparator(); imgMenu.add(mnuSave);
                lblImg.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseReleased(java.awt.event.MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) imgMenu.show(lblImg, e.getX(), e.getY());
                        else if (SwingUtilities.isLeftMouseButton(e)) showImagePreview(m); 
                    }
                    public void mousePressed(java.awt.event.MouseEvent e) { if (e.isPopupTrigger()) imgMenu.show(lblImg, e.getX(), e.getY()); }
                });
                bubble.add(lblImg, BorderLayout.CENTER);
            } else {
                JLabel lblImg = new JLabel("Đang tải ảnh...", SwingConstants.CENTER);
                lblImg.setForeground(Color.decode("#0F172A")); 
                lblImg.setPreferredSize(new Dimension(200, 150)); 
                bubble.add(lblImg, BorderLayout.CENTER);

                new Thread(() -> {
                    try {
                        Image finalImg;
                        if (m.content.startsWith("[IMG_URL]")) finalImg = javax.imageio.ImageIO.read(new java.net.URL(m.content.substring(9)));
                        else finalImg = new ImageIcon(java.util.Base64.getDecoder().decode(m.content.substring(5))).getImage();

                        ImageIcon originalIcon = new ImageIcon(finalImg);
                        int width = finalImg.getWidth(null), height = finalImg.getHeight(null);
                        if (width > 250) { height = (int) (height * (250.0 / width)); width = 250; }
                        
                        final int finalWidth = width, finalHeight = height;
                        ImageIcon roundedIcon = new ImageIcon(makeRoundedImage(finalImg, width, height, 16));
                        
                        iconCache.put(cacheKey, roundedIcon);
                        iconCache.put(cacheKeyOrig, originalIcon);

                        SwingUtilities.invokeLater(() -> {
                            lblImg.setText(""); lblImg.setPreferredSize(new Dimension(finalWidth, finalHeight));
                            lblImg.setIcon(roundedIcon); lblImg.setCursor(new Cursor(Cursor.HAND_CURSOR));
                            
                            JPopupMenu imgMenu = new JPopupMenu();
                            JMenuItem mnuCopy = new JMenuItem("  Sao chép hình ảnh");
                            mnuCopy.addActionListener(ev -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageSelection(originalIcon.getImage()), null));
                            JMenuItem mnuSave = new JMenuItem("  Lưu ảnh về máy...");
                            mnuSave.addActionListener(ev -> {
                                JFileChooser fc = new JFileChooser(); fc.setDialogTitle("Chọn nơi lưu ảnh");
                                if (fc.showSaveDialog(ChatTab.this) == JFileChooser.APPROVE_OPTION) {
                                    try {
                                        java.io.File f = fc.getSelectedFile();
                                        if(!f.getName().toLowerCase().endsWith(".png")) f = new java.io.File(f.getAbsolutePath() + ".png");
                                        javax.imageio.ImageIO.write((java.awt.image.BufferedImage)originalIcon.getImage(), "png", f);
                                        JOptionPane.showMessageDialog(ChatTab.this, "Đã lưu ảnh thành công!");
                                    } catch(Exception ex) { JOptionPane.showMessageDialog(ChatTab.this, "Lỗi khi lưu ảnh!"); }
                                }
                            });
                            imgMenu.add(mnuCopy); imgMenu.addSeparator(); imgMenu.add(mnuSave);
                            lblImg.addMouseListener(new java.awt.event.MouseAdapter() {
                                public void mouseReleased(java.awt.event.MouseEvent e) {
                                    if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) imgMenu.show(lblImg, e.getX(), e.getY());
                                    else if (SwingUtilities.isLeftMouseButton(e)) showImagePreview(m); 
                                }
                                public void mousePressed(java.awt.event.MouseEvent e) { if (e.isPopupTrigger()) imgMenu.show(lblImg, e.getX(), e.getY()); }
                            });
                            lblImg.getParent().revalidate(); lblImg.getParent().repaint();
                            if (chatScrollPane != null) { JScrollBar vBar = chatScrollPane.getVerticalScrollBar(); vBar.setValue(vBar.getMaximum()); }
                        });
                    } catch (Exception e) { SwingUtilities.invokeLater(() -> lblImg.setText("<html><i style='color:#0F172A;'>Lỗi hiển thị ảnh</i></html>")); }
                }).start();
            }
        } else if (isFile) {
            String[] parts = m.content.substring(6).split("\\|"); 
            String fileName = parts[0]; 
            String fileUrl = parts.length > 1 ? parts[1] : "";
            String fileSize = parts.length > 2 ? parts[2] : "";
            JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5)); filePanel.setOpaque(false);
            String iconUrl = "https://img.icons8.com/fluency/48/document.png"; String lowerName = fileName.toLowerCase();
            if (lowerName.endsWith(".pdf")) iconUrl = "https://img.icons8.com/fluency/48/pdf.png";
            else if (lowerName.endsWith(".docx") || lowerName.endsWith(".doc")) iconUrl = "https://img.icons8.com/fluency/48/microsoft-word-2019.png";
            else if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls") || lowerName.endsWith(".csv")) iconUrl = "https://img.icons8.com/fluency/48/microsoft-excel-2019.png";
            else if (lowerName.endsWith(".zip") || lowerName.endsWith(".rar")) iconUrl = "https://img.icons8.com/fluency/48/archive.png";
            else if (lowerName.endsWith(".pptx") || lowerName.endsWith(".ppt")) iconUrl = "https://img.icons8.com/fluency/48/microsoft-powerpoint-2019.png";
            JLabel lblIcon = new JLabel(); final String finalIconUrl = iconUrl;
            new Thread(() -> { try { Image img = new ImageIcon(new java.net.URL(finalIconUrl)).getImage().getScaledInstance(36, 36, Image.SCALE_SMOOTH); SwingUtilities.invokeLater(() -> lblIcon.setIcon(new ImageIcon(img))); } catch (Exception e) {} }).start();
            String textDisplay = "<html><u>" + fileName + (fileSize.isEmpty() ? "" : " <span style='color:#64748B;'>(" + fileSize + ")</span>") + "</u></html>";
            JLabel lblName = new JLabel(textDisplay); lblName.setFont(new Font("Segoe UI", Font.BOLD, 13)); lblName.setForeground(Color.decode("#2563EB")); lblName.setCursor(new Cursor(Cursor.HAND_CURSOR)); lblName.setToolTipText("Nhấp vào đây để tải xuống tài liệu");
            lblName.addMouseListener(new java.awt.event.MouseAdapter() { public void mouseClicked(java.awt.event.MouseEvent e) { if (!fileUrl.isEmpty()) { try { java.awt.Desktop.getDesktop().browse(new java.net.URI(fileUrl)); } catch(Exception ex) { JOptionPane.showMessageDialog(ChatTab.this, "Lỗi tải file!"); } } } });
            filePanel.add(lblIcon); filePanel.add(lblName); bubble.add(filePanel, BorderLayout.CENTER);
        } else {
            String displayContent = m.content.replace("\n", "<br>");
            if (!searchKeyword.isEmpty() && displayContent.toLowerCase().contains(searchKeyword)) displayContent = displayContent.replaceAll("(?i)(" + searchKeyword + ")", "<span style='background-color: #FEF08A; color: #B45309; font-weight: bold;'>$1</span>");
            displayContent = parseEmojis(displayContent);
            String htmlAuto = "<html><div style='font-size:14px; font-family: Segoe UI; color:#0F172A; line-height: 1.5; margin:0; padding:0;'>" + displayContent + "</div></html>";
            JLabel lblText = new JLabel(htmlAuto); 
            if (lblText.getPreferredSize().width > 280) lblText.setText("<html><div style='width: 280px; font-size:14px; font-family: Segoe UI; color:#0F172A; line-height: 1.5; margin:0; padding:0;'>" + displayContent + "</div></html>");
            bubble.add(lblText, BorderLayout.CENTER);
        }
        
        bubbleWrap.add(bubble); 
        
        // !!! CŨNG XÓA LỖI ĐIỀU KIỆN IF Ở ĐÂY ĐỂ ĐỒNG BỘ THỜI GIAN !!!
        String timeStr = m.sentAt != null ? timeFormat.format(m.sentAt) : ""; 
        JLabel lblTime = new JLabel(timeStr); 
        lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 11)); 
        lblTime.setForeground(TEXT_MUTED); 
        bubbleWrap.add(Box.createVerticalStrut(4)); 
        bubbleWrap.add(lblTime); 
        
        JPanel alignLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); alignLeft.setOpaque(false); alignLeft.add(bubbleWrap); p.add(alignLeft, BorderLayout.CENTER); 
        return p;
    }
    
 private JPanel createOutgoingMessage(Message m, boolean isFirstInGroup) {
        JPanel p = new JPanel(new BorderLayout(12, 0)); 
        p.setOpaque(false); 
        // !!! CHÚ Ý: ĐÃ SỬA 200 THÀNH 9999 Ở ĐÂY ĐỂ ẢNH KHÔNG BỊ CẮT LẸM !!!
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 9999)); 
        
        JPanel bubbleWrap = new JPanel(); 
        bubbleWrap.setLayout(new BoxLayout(bubbleWrap, BoxLayout.Y_AXIS)); 
        bubbleWrap.setOpaque(false);
        
        boolean isImage = m.content != null && (m.content.startsWith("[IMG]") || m.content.startsWith("[IMG_URL]"));
        boolean isFile = m.content != null && m.content.startsWith("[FILE]");
        boolean isUploading = m.content != null && m.content.contains("UPLOADING"); // Khai báo isUploading CHUẨN ở đây

        Color zaloBlue = Color.decode("#E5EFFF");
        RoundedPanel bubble = new RoundedPanel(20, isImage ? new Color(0,0,0,0) : zaloBlue); 
        bubble.setBorder(new EmptyBorder(isImage ? 0 : 10, isImage ? 0 : 16, isImage ? 0 : 10, isImage ? 0 : 16)); 
        bubble.setLayout(new BorderLayout());
        
        if (isImage) {
            if (isUploading) {
                JPanel loadPanel = new JPanel(new BorderLayout()); loadPanel.setOpaque(false); loadPanel.setPreferredSize(new Dimension(200, 150));
                JLabel lblIcon = new JLabel("", SwingConstants.CENTER); setNetworkIcon(lblIcon, "https://img.icons8.com/fluency-systems-regular/48/94A3B8/cloud-sync.png", 40, 40);
                JLabel lblLoading = new JLabel("Đang tải lên...", SwingConstants.CENTER); lblLoading.setForeground(Color.decode("#94A3B8")); lblLoading.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                loadPanel.add(lblIcon, BorderLayout.CENTER); loadPanel.add(lblLoading, BorderLayout.SOUTH); bubble.add(loadPanel, BorderLayout.CENTER);
            } else {
                String cacheKey = "CHAT_IMG_" + m.content.hashCode();
                String cacheKeyOrig = "CHAT_ORIG_" + m.content.hashCode();
                
                if (iconCache.containsKey(cacheKey) && iconCache.containsKey(cacheKeyOrig)) {
                    ImageIcon roundedIcon = iconCache.get(cacheKey); ImageIcon originalIcon = iconCache.get(cacheKeyOrig);
                    JLabel lblImg = new JLabel(); lblImg.setPreferredSize(new Dimension(roundedIcon.getIconWidth(), roundedIcon.getIconHeight()));
                    lblImg.setIcon(roundedIcon); lblImg.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    
                    JPopupMenu imgMenu = new JPopupMenu();
                    JMenuItem mnuCopy = new JMenuItem("  Sao chép hình ảnh"); mnuCopy.addActionListener(ev -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageSelection(originalIcon.getImage()), null));
                    JMenuItem mnuSave = new JMenuItem("  Lưu ảnh về máy...");
                    mnuSave.addActionListener(ev -> {
                        JFileChooser fc = new JFileChooser(); fc.setDialogTitle("Chọn nơi lưu ảnh");
                        if (fc.showSaveDialog(ChatTab.this) == JFileChooser.APPROVE_OPTION) {
                            try { java.io.File f = fc.getSelectedFile(); if(!f.getName().toLowerCase().endsWith(".png")) f = new java.io.File(f.getAbsolutePath() + ".png"); javax.imageio.ImageIO.write((java.awt.image.BufferedImage)originalIcon.getImage(), "png", f); JOptionPane.showMessageDialog(ChatTab.this, "Đã lưu ảnh thành công!"); } catch(Exception ex) { JOptionPane.showMessageDialog(ChatTab.this, "Lỗi khi lưu ảnh!"); }
                        }
                    });
                    imgMenu.add(mnuCopy); imgMenu.addSeparator(); imgMenu.add(mnuSave);
                    lblImg.addMouseListener(new java.awt.event.MouseAdapter() {
                        public void mouseReleased(java.awt.event.MouseEvent e) { if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) imgMenu.show(lblImg, e.getX(), e.getY()); else if (SwingUtilities.isLeftMouseButton(e)) showImagePreview(m); }
                        public void mousePressed(java.awt.event.MouseEvent e) { if (e.isPopupTrigger()) imgMenu.show(lblImg, e.getX(), e.getY()); }
                    });
                    bubble.add(lblImg, BorderLayout.CENTER);
                } else {
                    JLabel lblImg = new JLabel("Đang xử lý ảnh...", SwingConstants.CENTER); lblImg.setForeground(Color.decode("#64748B")); lblImg.setPreferredSize(new Dimension(200, 150)); bubble.add(lblImg, BorderLayout.CENTER);
                    new Thread(() -> {
                        try {
                            Image finalImg;
                            if (m.content.startsWith("[IMG_URL]")) finalImg = javax.imageio.ImageIO.read(new java.net.URL(m.content.substring(9)));
                            else finalImg = new ImageIcon(java.util.Base64.getDecoder().decode(m.content.substring(5))).getImage();
                            
                            ImageIcon originalIcon = new ImageIcon(finalImg);
                            int width = finalImg.getWidth(null), height = finalImg.getHeight(null);
                            if (width > 250) { height = (int) (height * (250.0 / width)); width = 250; }
                            final int finalWidth = width, finalHeight = height;
                            ImageIcon roundedIcon = new ImageIcon(makeRoundedImage(finalImg, width, height, 16));
                            
                            iconCache.put(cacheKey, roundedIcon); iconCache.put(cacheKeyOrig, originalIcon);
                            
                            SwingUtilities.invokeLater(() -> {
                                lblImg.setText(""); lblImg.setPreferredSize(new Dimension(finalWidth, finalHeight)); lblImg.setIcon(roundedIcon); lblImg.setCursor(new Cursor(Cursor.HAND_CURSOR));
                                JPopupMenu imgMenu = new JPopupMenu(); JMenuItem mnuCopy = new JMenuItem("  Sao chép hình ảnh"); mnuCopy.addActionListener(ev -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageSelection(originalIcon.getImage()), null));
                                JMenuItem mnuSave = new JMenuItem("  Lưu ảnh về máy..."); mnuSave.addActionListener(ev -> {
                                    JFileChooser fc = new JFileChooser(); fc.setDialogTitle("Chọn nơi lưu ảnh");
                                    if (fc.showSaveDialog(ChatTab.this) == JFileChooser.APPROVE_OPTION) { try { java.io.File f = fc.getSelectedFile(); if(!f.getName().toLowerCase().endsWith(".png")) f = new java.io.File(f.getAbsolutePath() + ".png"); javax.imageio.ImageIO.write((java.awt.image.BufferedImage)originalIcon.getImage(), "png", f); JOptionPane.showMessageDialog(ChatTab.this, "Đã lưu ảnh thành công!"); } catch(Exception ex) { JOptionPane.showMessageDialog(ChatTab.this, "Lỗi khi lưu ảnh!"); } }
                                });
                                imgMenu.add(mnuCopy); imgMenu.addSeparator(); imgMenu.add(mnuSave);
                                lblImg.addMouseListener(new java.awt.event.MouseAdapter() {
                                    public void mouseReleased(java.awt.event.MouseEvent e) { if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) imgMenu.show(lblImg, e.getX(), e.getY()); else if (SwingUtilities.isLeftMouseButton(e)) showImagePreview(m); }
                                    public void mousePressed(java.awt.event.MouseEvent e) { if (e.isPopupTrigger()) imgMenu.show(lblImg, e.getX(), e.getY()); }
                                });
                                lblImg.getParent().revalidate(); lblImg.getParent().repaint();
                                if (chatScrollPane != null) { JScrollBar vBar = chatScrollPane.getVerticalScrollBar(); vBar.setValue(vBar.getMaximum()); }
                            });
                        } catch (Exception e) { SwingUtilities.invokeLater(() -> lblImg.setText("<html><i style='color:red;'>Lỗi hiển thị ảnh</i></html>")); }
                    }).start();
                }
            }
        } else if (isFile) {
            String[] parts = m.content.substring(6).split("\\|"); 
            String fileName = parts[0]; 
            String fileUrl = parts.length > 1 ? parts[1] : "";
            String fileSize = parts.length > 2 ? parts[2] : "";
            
            JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5)); filePanel.setOpaque(false);
            String iconUrl = "https://img.icons8.com/fluency/48/document.png"; String lowerName = fileName.toLowerCase();
            if (lowerName.endsWith(".pdf")) iconUrl = "https://img.icons8.com/fluency/48/pdf.png";
            else if (lowerName.endsWith(".docx") || lowerName.endsWith(".doc")) iconUrl = "https://img.icons8.com/fluency/48/microsoft-word-2019.png";
            else if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls") || lowerName.endsWith(".csv")) iconUrl = "https://img.icons8.com/fluency/48/microsoft-excel-2019.png";
            else if (lowerName.endsWith(".zip") || lowerName.endsWith(".rar")) iconUrl = "https://img.icons8.com/fluency/48/archive.png";
            else if (lowerName.endsWith(".pptx") || lowerName.endsWith(".ppt")) iconUrl = "https://img.icons8.com/fluency/48/microsoft-powerpoint-2019.png";
            JLabel lblIcon = new JLabel(); final String finalIconUrl = iconUrl;
            new Thread(() -> { try { Image img = new ImageIcon(new java.net.URL(finalIconUrl)).getImage().getScaledInstance(36, 36, Image.SCALE_SMOOTH); SwingUtilities.invokeLater(() -> lblIcon.setIcon(new ImageIcon(img))); } catch (Exception e) {} }).start();
            String sizeHtml = fileSize.isEmpty() ? "" : " <span style='color:#64748B;'>(" + fileSize + ")</span>";
            String textDisplay = isUploading ? "<html><u>" + fileName + "</u> <i style='color:#64748B;'>(Đang gửi...)</i></html>" : "<html><u>" + fileName + "</u>" + sizeHtml + "</html>";
            JLabel lblName = new JLabel(textDisplay); lblName.setFont(new Font("Segoe UI", Font.BOLD, 13)); lblName.setForeground(isUploading ? Color.decode("#94A3B8") : Color.decode("#2563EB")); 
            if (!isUploading) {
                lblName.setCursor(new Cursor(Cursor.HAND_CURSOR)); lblName.setToolTipText("Nhấp vào đây để tải xuống tài liệu");
                lblName.addMouseListener(new java.awt.event.MouseAdapter() { public void mouseClicked(java.awt.event.MouseEvent e) { try { java.awt.Desktop.getDesktop().browse(new java.net.URI(fileUrl)); } catch(Exception ex) { JOptionPane.showMessageDialog(ChatTab.this, "Lỗi tải file!"); } } });
            }
            filePanel.add(lblIcon); filePanel.add(lblName); bubble.add(filePanel, BorderLayout.CENTER);
        } else {
            String displayContent = m.content.replace("\n", "<br>");
            if (!searchKeyword.isEmpty() && displayContent.toLowerCase().contains(searchKeyword)) displayContent = displayContent.replaceAll("(?i)(" + searchKeyword + ")", "<span style='background-color: #FBBF24; color: #000000; font-weight: bold;'>$1</span>");
            displayContent = parseEmojis(displayContent);
            String htmlAuto = "<html><div style='font-size:14px; font-family: Segoe UI; color:#0F172A; line-height: 1.5; margin:0; padding:0;'>" + displayContent + "</div></html>";
            JLabel lblText = new JLabel(htmlAuto); 
            if (lblText.getPreferredSize().width > 280) lblText.setText("<html><div style='width: 280px; font-size:14px; font-family: Segoe UI; color:#0F172A; line-height: 1.5; margin:0; padding:0;'>" + displayContent + "</div></html>");
            bubble.add(lblText, BorderLayout.CENTER);
        }
        
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0)); timePanel.setOpaque(false);
        String timeStr = m.sentAt != null ? timeFormat.format(m.sentAt) : ""; 
        JLabel lblTime = new JLabel(timeStr); lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 11)); lblTime.setForeground(TEXT_MUTED);
        
        MessageStatus deliveryStatus = m.getDeliveryStatus();
        boolean isPending = deliveryStatus == MessageStatus.PENDING_SEND;
        boolean isDelivered = deliveryStatus == MessageStatus.DELIVERED;
        boolean isRead = deliveryStatus == MessageStatus.READ;
        String statusText = isUploading ? "Đang tải tệp..." : (isPending ? "Đang gửi..." : (isRead ? "Đã xem" : "Đã nhận"));
        statusText = isUploading
                ? "\u0110ang t\u1ea3i t\u1ec7p..."
                : (isPending ? "\u0110ang g\u1eedi..." : (isRead ? "\u0110\u00e3 xem" : (isDelivered ? "\u0110\u00e3 nh\u1eadn" : "\u0110\u00e3 g\u1eedi")));
        JLabel lblStatus = new JLabel(statusText);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblStatus.setForeground((isRead && !isUploading && !isPending) ? PRIMARY : TEXT_MUTED);

        JLabel checkIcon = new JLabel(); 
        if (isUploading || isPending) {
            setNetworkIcon(checkIcon, "https://img.icons8.com/fluency-systems-regular/48/94A3B8/clock--v1.png", 14, 14); // Icon đồng hồ mờ
        } else {
            setNetworkIcon(checkIcon, isRead ? "https://img.icons8.com/fluency-systems-filled/48/2563EB/double-tick.png" : "https://img.icons8.com/fluency-systems-filled/48/94A3B8/double-tick.png", 14, 14); 
        }

        timePanel.add(lblTime); 
        timePanel.add(Box.createHorizontalStrut(4));
        timePanel.add(lblStatus); 
        timePanel.add(checkIcon);
        
        bubbleWrap.add(bubble); 
        bubbleWrap.add(Box.createVerticalStrut(4)); 
        bubbleWrap.add(timePanel); 
        
        JPanel alignRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); alignRight.setOpaque(false); alignRight.add(bubbleWrap); p.add(alignRight, BorderLayout.CENTER); 
        return p;
    }
    private JPanel createDateDivider(String text) { 
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER)); 
        p.setOpaque(false); 
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); 
        RoundedPanel badge = new RoundedPanel(999, Color.WHITE); 
        badge.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1), new EmptyBorder(4, 12, 4, 12))); 
        JLabel lbl = new JLabel(text); 
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11)); 
        lbl.setForeground(TEXT_MUTED); 
        badge.add(lbl); 
        p.add(badge); 
        return p; 
    }

   

    // Giao diện bong bóng gọi giống 100% Mockup
    private JPanel createCallBubble(Message m, boolean isFirstInGroup) {
        JPanel p = new JPanel(new BorderLayout(12, 0)); 
        p.setOpaque(false); 
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        
        boolean isMe = "ME".equals(m.senderType);
        boolean isVideo = "VIDEO_CALL".equals(m.messageType);
        boolean isEnded = m.content.contains("kết thúc"); 
        
        // Màu Nền và Viền theo Mockup
        Color bubbleBg = isMe ? Color.decode("#F5F8FF") : Color.WHITE;
        Color borderColor = isMe ? Color.decode("#D1E0FF") : Color.decode("#E2E8F0");
        if (!isMe && !isEnded) borderColor = Color.decode("#A7F3D0"); // Viền xanh lá mờ cho cuộc gọi đến

        RoundedPanel bubble = new RoundedPanel(24, bubbleBg); 
        bubble.setLayout(new BorderLayout(15, 0)); 
        bubble.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1), 
            new EmptyBorder(12, 20, 12, 20)
        ));
        
        // Vòng tròn chứa Icon
        Color iconBgColor = isEnded ? Color.decode("#F1F5F9") : (isMe ? Color.decode("#E0E7FF") : Color.decode("#D1FAE5"));
        String iconColor = isEnded ? "94A3B8" : (isMe ? "2563EB" : "10B981");
        String iconUrl = isVideo ? "https://img.icons8.com/fluency-systems-filled/48/"+iconColor+"/video-call.png" : "https://img.icons8.com/fluency-systems-filled/48/"+iconColor+"/phone.png";
        
        JPanel iconWrapper = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(iconBgColor);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        iconWrapper.setOpaque(false);
        iconWrapper.setPreferredSize(new Dimension(44, 44));
        JLabel icon = new JLabel(); 
        setNetworkIcon(icon, iconUrl, 24, 24); 
        iconWrapper.add(icon);
        
        JPanel info = new JPanel(); 
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS)); 
        info.setOpaque(false); 
        
        JLabel lTitle = new JLabel(m.content); 
        lTitle.setFont(new Font("Segoe UI", Font.BOLD, 15)); 
        lTitle.setForeground(TEXT_MAIN); 
        
        JLabel lAction = new JLabel(isEnded ? "Cuộc gọi đã kết thúc" : (isMe ? "Cuộc gọi đi" : "Nhấn để tham gia!")); 
        lAction.setFont(new Font("Segoe UI", Font.PLAIN, 13)); 
        Color actionColor = isEnded ? TEXT_MUTED : (isMe ? PRIMARY : ONLINE_COLOR);
        lAction.setForeground(actionColor); 
        
        info.add(lTitle); info.add(Box.createVerticalStrut(2)); info.add(lAction);
        bubble.add(iconWrapper, BorderLayout.WEST); bubble.add(info, BorderLayout.CENTER); 
        
        if (!isEnded) {
            bubble.setCursor(new Cursor(Cursor.HAND_CURSOR));
            bubble.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (!isMe) { if (isVideo) startVideoCall(); else startAudioCall(); }
                }
            });
        }
        
        JPanel wrapper = new JPanel(new FlowLayout(isMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0)); wrapper.setOpaque(false); wrapper.add(bubble);
        if (!isMe) {
            JPanel avaWrap = new JPanel(new BorderLayout()); avaWrap.setOpaque(false); avaWrap.setPreferredSize(new Dimension(36, 36));
            if(isFirstInGroup) { JLabel ava = new JLabel(); setAvatarIcon(ava, activeConversation.avatarUrl, 36); avaWrap.add(ava, BorderLayout.NORTH); }
            p.add(avaWrap, BorderLayout.WEST);
        }
        p.add(wrapper, BorderLayout.CENTER); return p;
    }

    private void setActiveConversation(ConversationInfo c) { 
        activeConversation = c; 
        if(c.unreadCount > 0) { 
            c.unreadCount = 0; 
            refreshConversationList(); 
            notifyUnreadCountChanged();
            try {
                NetworkManager.getInstance().sendPacket(new Packet("MARK_AS_READ", String.valueOf(c.conversationId)));
            } catch (Exception e) {}
            // -------------------------------------------------------------------------
        } 
        renderActiveChatStructure(); 
        if (c.conversationId > 0) { 
            fetchMessagesFromServer(c.conversationId); 
        } else { 
            currentMessages = new ArrayList<>();
            refreshMessages(); 
        } 
    }

    public void showTypingIndicator(String convoId, String senderName) {
        if (activeConversation == null || !String.valueOf(activeConversation.conversationId).equals(convoId)) return;
        lblTypingIndicator.setText(senderName + " đang soạn tin...");
        if (typingTimer != null) {
            typingTimer.stop();
        }
        typingTimer = new javax.swing.Timer(3000, e -> {
            lblTypingIndicator.setText(" ");
        });
        typingTimer.setRepeats(false);
        typingTimer.start();
    }

  private void sendCurrentMessage() { 
        String text = txtChatInput.getText().trim(); 
        if (text.isEmpty() || activeConversation == null) return; 

        
        Message m = new Message(); 
        m.clientMessageId = java.util.UUID.randomUUID().toString();
        m.senderId = CURRENT_USER_ID;
        m.senderType = "ME"; 
        m.messageType = "TEXT"; 
        m.content = text; 
        m.sentAt = new Date(); 
        m.markPendingSend();
        
        if (currentMessages == null) currentMessages = new ArrayList<>(); 
        currentMessages.add(m); 
        
        messageSyncService.saveOutgoingMessage(activeConversation.conversationId, m);
        
        // --- XỬ LÝ HIỂN THỊ TIN NHẮN GẦN NHẤT Ở CỘT BÊN TRÁI ---
        if (text.contains("[e:")) {
            activeConversation.lastMessage = "Bạn: [Biểu tượng cảm xúc]";
        } else {
            activeConversation.lastMessage = "Bạn: " + text; 
        }
        
        activeConversation.lastMessageTime = m.sentAt; 
        
        txtChatInput.setText(""); 
        refreshMessages(); 
        refreshConversationList(); 
        
        // Gửi chuỗi chứa mã [e:... lên Server để máy bên kia đọc được
        new SwingWorker<Void, Void>() { 
            @Override protected Void doInBackground() throws Exception { 
                messageSyncService.sendChatMessage(activeConversation.conversationId, m);
                return null; 
            } 
        }.execute(); 
    }

    // Hàm mới: Vẽ tin nhắn hệ thống (Cuộc gọi) của bản thân lên màn hình
    public void appendLocalMessage(String messageType, String content) {
        SwingUtilities.invokeLater(() -> {
            Message m = new Message();
            m.senderType = "ME"; // Xác nhận đây là tin nhắn của MÌNH (Hiện bên phải)
            m.messageType = messageType; // Giữ nguyên loại là VIDEO_CALL hoặc AUDIO_CALL
            m.content = content;
            m.sentAt = new Date();

            if (currentMessages == null) currentMessages = new ArrayList<>();
            currentMessages.add(m);

            if (activeConversation != null) {
                activeConversation.lastMessage = "Bạn: " + content;
                activeConversation.lastMessageTime = m.sentAt;
            }

            refreshMessages();
            refreshConversationList();
        });
    }

    public void handleSendAck(String ackPayload) {
        String[] parts = ackPayload == null ? new String[0] : ackPayload.split("\\|", 3);
        String conversationId = parts.length > 0 ? parts[0] : "";
        String clientMessageId = parts.length > 1 ? parts[1] : "";
        int serverMessageId = parts.length > 2 ? parseIntOrDefault(parts[2], 0) : 0;

        messageSyncService.markMessageAsSent(conversationId, clientMessageId, serverMessageId);
        // Cập nhật memory state thay vì gọi GET_MESSAGES
        if (currentMessages == null || currentMessages.isEmpty()) {
            return;
        }

        Message target = findPendingMessageByClientId(clientMessageId);
        if (target == null) {
            target = findLastPendingMessage();
        }

        if (target != null) {
            target.markSent(serverMessageId);
            refreshMessages();
            fetchConversationListFromServer();
        }
    }

    public void handleDeliveredAck(String ackPayload) {
        String[] parts = ackPayload == null ? new String[0] : ackPayload.split("\\|", 3);
        String conversationId = parts.length > 0 ? parts[0] : "";
        String clientMessageId = parts.length > 1 ? parts[1] : "";
        int serverMessageId = parts.length > 2 ? parseIntOrDefault(parts[2], 0) : 0;

        messageSyncService.markMessageAsDelivered(conversationId, clientMessageId, serverMessageId);
        if (currentMessages == null || currentMessages.isEmpty()) {
            return;
        }

        Message target = findOutgoingMessageByAck(clientMessageId, serverMessageId);
        if (target != null) {
            target.markDelivered(serverMessageId);
            refreshMessages();
        }
    }

    private Message findPendingMessageByClientId(String clientMessageId) {
        if (clientMessageId == null || clientMessageId.trim().isEmpty() || currentMessages == null) {
            return null;
        }
        for (int i = currentMessages.size() - 1; i >= 0; i--) {
            Message message = currentMessages.get(i);
            if (clientMessageId.equals(message.clientMessageId) && message.isPendingSend()) {
                return message;
            }
        }
        return null;
    }

    private Message findOutgoingMessageByAck(String clientMessageId, int serverMessageId) {
        if (currentMessages == null) {
            return null;
        }
        for (int i = currentMessages.size() - 1; i >= 0; i--) {
            Message message = currentMessages.get(i);
            if (!"ME".equals(message.senderType)) {
                continue;
            }
            boolean matchesClientId = clientMessageId != null
                    && !clientMessageId.trim().isEmpty()
                    && clientMessageId.equals(message.clientMessageId);
            boolean matchesServerId = serverMessageId > 0
                    && (message.serverMessageId == serverMessageId || message.messageId == serverMessageId);
            if (matchesClientId || matchesServerId) {
                return message;
            }
        }
        return null;
    }

    private Message findLastPendingMessage() {
        if (currentMessages == null) {
            return null;
        }
        for (int i = currentMessages.size() - 1; i >= 0; i--) {
            Message message = currentMessages.get(i);
            if (message.isPendingSend()) {
                return message;
            }
        }
        return null;
    }

    private int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private void setupDragAndDrop() {
        java.awt.dnd.DropTarget dt = new java.awt.dnd.DropTarget() {
            @Override
            public synchronized void drop(java.awt.dnd.DropTargetDropEvent dtde) {
                if (activeConversation == null || activeConversation.conversationId < 0) {
                    dtde.rejectDrop();
                    JOptionPane.showMessageDialog(ChatTab.this, "Vui lòng chọn một người bạn trước khi thả tệp!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                try {
                    dtde.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY);
                    java.util.List<File> droppedFiles = (java.util.List<File>) dtde.getTransferable().getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                    
                    java.util.List<File> imageList = new ArrayList<>();
                    java.util.List<File> docList = new ArrayList<>();
                    
                    for (File f : droppedFiles) {
                        String name = f.getName().toLowerCase();
                        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")) {
                            imageList.add(f);
                        } else {
                            docList.add(f);
                        }
                    }
                    
                    if (!imageList.isEmpty()) {
                        uploadImageFiles(imageList.toArray(new File[0]));
                    }
                    if (!docList.isEmpty()) {
                        uploadDocumentFiles(docList.toArray(new File[0]));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        this.setDropTarget(dt);
        if (txtChatInput != null) txtChatInput.setDropTarget(dt);
    }
    
   public void appendIncomingMessage(String conversationId, String senderName, String content) { 
        SwingUtilities.invokeLater(() -> { 
            boolean isProcessed = false; 
            boolean foundConvo = false; 
            
            // 1. Nhận diện loại tin nhắn mặc định là TEXT
            String msgType = "TEXT";
            String lowerContent = content.toLowerCase();
            
            // 2. Tích hợp phân loại các tin nhắn đặc biệt
            if (content.startsWith("[IMG]") || content.startsWith("[IMG_URL]")) {
                msgType = "IMAGE";
            } else if (content.startsWith("[FILE]")) {
                msgType = "FILE";
            } else if (lowerContent.contains("cuộc gọi video")) {
                msgType = "VIDEO_CALL";
            } else if (lowerContent.contains("cuộc gọi thoại")) {
                msgType = "AUDIO_CALL";
            } else if (lowerContent.contains("kết thúc")) {
                msgType = "VIDEO_CALL"; // Mặc định ban đầu nếu không rõ
                
                // TÍCH HỢP THÔNG MINH: Nhìn lại tin nhắn ngay trước đó 
                // để đồng bộ chuẩn xác icon Audio hay Video bị xám đi
                if (currentMessages != null && !currentMessages.isEmpty()) {
                    Message prevMsg = currentMessages.get(currentMessages.size() - 1);
                    if ("AUDIO_CALL".equals(prevMsg.messageType)) {
                        msgType = "AUDIO_CALL";
                    }
                }
            }

            // 2.5 OFFLINE-FIRST: Lưu tin nhắn mới nhận vào SQLite
            Message newMsg = new Message(); 
            newMsg.senderType = "OTHER"; 
            newMsg.messageType = msgType; 
            newMsg.content = content; 
            newMsg.sentAt = new Date(); 
            newMsg.senderName = senderName;
            messageSyncService.saveIncomingMessage(conversationId, newMsg);

            // 3. Hiển thị vào khung chat hiện tại (nếu đang mở đúng cuộc trò chuyện)
            if (activeConversation != null && String.valueOf(activeConversation.conversationId).equals(conversationId)) { 
                if (currentMessages == null) currentMessages = new ArrayList<>(); 
                currentMessages.add(newMsg); 
                refreshMessages(); 
                isProcessed = true; 
                foundConvo = true; 
                
                // =================================================================
                // !!! FIX LỖI REAL-TIME READ RECEIPT Ở ĐÂY !!!
                // Nếu mình đang trực tiếp mở khung chat này và nhìn thấy tin nhắn tới,
                // lập tức báo cho Server là "Tôi đã đọc nó rồi!" để máy bên kia Xanh lên
                // =================================================================
                try {
                    NetworkManager.getInstance().sendPacket(new Packet("MARK_AS_READ", conversationId));
                } catch (Exception e) {}
            } 
            
            // 4. Cập nhật Sidebar (Cột danh sách bên trái)
            for (ConversationInfo c : conversations) {
                if (String.valueOf(c.conversationId).equals(conversationId)) { 
                    
                    // Xử lý hiển thị thân thiện ở cột bên trái
                    if (content.startsWith("[IMG]") || content.startsWith("[IMG_URL]")) {
                        c.lastMessage = "Đã gửi một hình ảnh";
                    } else if (content.startsWith("[FILE]")) {
                        String[] parts = content.substring(6).split("\\|");
                        c.lastMessage = "Đã gửi tệp: " + parts[0];
                    } else if (content.contains("[e:")) {
                        c.lastMessage = "Đã gửi một biểu tượng cảm xúc";
                    } else {
                        c.lastMessage = content;
                    }
                    
                    c.lastMessageTime = new Date(); 
                    if (!isProcessed) c.unreadCount++; 
                    conversations.remove(c); 
                    conversations.add(0, c); 
                    foundConvo = true; 
                    break; 
                } 
            }
            
            // 5. Làm mới giao diện
            if (!foundConvo) {
                fetchConversationListFromServer(); 
            } else {
                refreshConversationList(); 
                notifyUnreadCountChanged(); // Cập nhật dấu chấm đỏ ở Header
            }
        }); 
    }

    private JSplitPane createBorderlessSplitPane(int orient, Component c1, Component c2) { 
        JSplitPane p = new JSplitPane(orient, c1, c2); 
        p.setBorder(null); p.setOpaque(false); p.setDividerSize(1); 
        return p; 
    }

    // =========================================================================
    // HELPER & CÁC LỚP TIỆN ÍCH UI
    // =========================================================================
    private void setNetworkIcon(JLabel label, String urlStr, int width, int height) { 
        if (urlStr == null || urlStr.trim().isEmpty()) urlStr = "https://img.icons8.com/color/48/circled-user-male-skin-type-4--v1.png"; 
        final String finalUrl = urlStr; String key = finalUrl + "_" + width + "x" + height; 
        if (iconCache.containsKey(key)) { label.setIcon(iconCache.get(key)); return; } 
        new Thread(() -> { 
            try { 
                Image img = finalUrl.startsWith("http") ? new ImageIcon(new URL(finalUrl)).getImage() : new ImageIcon(finalUrl).getImage(); 
                if (img != null) { 
                    Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH); 
                    ImageIcon scaled = new ImageIcon(scaledImg); iconCache.put(key, scaled); 
                    SwingUtilities.invokeLater(() -> { label.setIcon(scaled); if (label.getParent() != null) { label.getParent().revalidate(); label.getParent().repaint(); } }); 
                } 
            } catch (Exception e) {} 
        }).start(); 
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
    
    // =========================================================================
    // CHỨC NĂNG GỬI ẢNH VÀ EMOJI
    // =========================================================================
    // 1. Popup chọn Emoji từ thư mục /images/emoji/ trong NetBeans
   // 1. Popup chọn Emoji từ thư mục /com/images/emoji/
    // 1. Popup chọn Emoji
    // Lớp hỗ trợ khai báo chủ đề Emoji
  // Lớp hỗ trợ khai báo chủ đề Emoji (Đã thêm icon cho thanh điều hướng đáy)
    class EmojiCategory {
        String title;
        String prefix;
        String navIconUrl;
        public EmojiCategory(String title, String prefix, String navIconUrl) { 
            this.title = title; this.prefix = prefix; this.navIconUrl = navIconUrl;
        }
    }

    // 1. Popup bảng chọn Emoji: Cuộn liên tục + Mục Gần Đây + Thanh Bottom Nav
    // 1. Popup bảng chọn Emoji: Cuộn liên tục + Mục Gần Đây + Thanh Bottom Nav (Hỗ trợ cuộn ngang)
    private void showEmojiPicker(Component invoker) {
        JPopupMenu emojiPopup = new JPopupMenu();
        emojiPopup.setBackground(Color.WHITE);
        emojiPopup.setBorder(BorderFactory.createLineBorder(Color.decode("#E5E7EB"), 1));
        
        // ĐÃ ĐẢO LẠI THỨ TỰ THEO ĐÚNG YÊU CẦU CỦA BẠN
        EmojiCategory[] categories = {
            new EmojiCategory("Cảm xúc", "mat", "https://img.icons8.com/fluency-systems-regular/48/64748B/happy.png"),
            new EmojiCategory("Cử chỉ", "cuchi", "https://img.icons8.com/fluency-systems-regular/48/64748B/hand.png"),
            new EmojiCategory("Con người", "connguoi", "https://img.icons8.com/fluency-systems-regular/48/64748B/user.png"),
            new EmojiCategory("Tự nhiên", "tunhien", "https://img.icons8.com/fluency-systems-regular/48/64748B/deciduous-tree.png"),
            new EmojiCategory("Con vật", "convat", "https://img.icons8.com/fluency-systems-regular/48/64748B/cat-profile.png"),
            new EmojiCategory("Trang trí", "trangtri", "https://img.icons8.com/fluency-systems-regular/48/64748B/party-baloon.png"),
            new EmojiCategory("Ăn uống", "anuong", "https://img.icons8.com/fluency-systems-regular/48/64748B/hamburger.png"), 
            new EmojiCategory("Trò chơi", "trochoi", "https://img.icons8.com/fluency-systems-regular/48/64748B/football2.png"),
            new EmojiCategory("Công cụ", "congcu", "https://img.icons8.com/fluency-systems-regular/48/64748B/light.png"),
            new EmojiCategory("Cờ", "co", "https://img.icons8.com/fluency-systems-regular/48/64748B/flag.png")
        };

        // Panel chính chứa TẤT CẢ các mục, xếp theo chiều dọc
        JPanel mainScrollContent = new JPanel();
        mainScrollContent.setLayout(new BoxLayout(mainScrollContent, BoxLayout.Y_AXIS));
        mainScrollContent.setBackground(Color.WHITE);
        mainScrollContent.setBorder(new EmptyBorder(5, 10, 10, 10));

        // --- MỤC 1: GẦN ĐÂY ---
        if (!recentEmojis.isEmpty()) {
            mainScrollContent.add(createEmojiSectionTitle("Gần đây"));
            JPanel recentGrid = new JPanel(new GridLayout(0, 8, 2, 6)); 
            recentGrid.setBackground(Color.WHITE);
            for (String tag : recentEmojis) {
                recentGrid.add(createSingleEmojiButton(tag, emojiPopup));
            }
            int fill = 8 - (recentEmojis.size() % 8);
            if (fill < 8) for(int i=0; i<fill; i++) { JPanel p = new JPanel(); p.setOpaque(false); recentGrid.add(p); }
            
            JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.add(recentGrid, BorderLayout.WEST);
            mainScrollContent.add(wrap);
            mainScrollContent.add(Box.createVerticalStrut(10));
        }

        // --- MỤC 2: TẤT CẢ CÁC CHỦ ĐỀ KHÁC ---
        java.util.Map<String, JPanel> categoryPanels = new java.util.HashMap<>();

        for (EmojiCategory cat : categories) {
            JPanel titlePanel = createEmojiSectionTitle(cat.title);
            categoryPanels.put(cat.prefix, titlePanel); 
            mainScrollContent.add(titlePanel);
            
            JPanel gridPanel = new JPanel(new GridLayout(0, 8, 2, 6)); 
            gridPanel.setBackground(Color.WHITE);
            
            // THUẬT TOÁN MỚI: Chống lỗi thiếu file (Bỏ qua tối đa 15 file lỗi liên tiếp mới dừng)
            int missingCount = 0;
            for (int i = 1; i <= 200; i++) {
                String path = "/images/emoji/" + cat.prefix + "/" + cat.prefix + " (" + i + ").png";
                URL url = getClass().getResource(path);
                
                if (url == null) {
                    missingCount++;
                    if (missingCount > 15) break; // Trượt 15 số liên tiếp mới xác nhận là hết ảnh
                    continue; // Bỏ qua file lỗi, chạy tiếp vòng lặp
                }
                missingCount = 0; // Reset bộ đếm nếu tìm thấy ảnh
                
                String tag = "[e:" + cat.prefix + ":" + i + "]";
                gridPanel.add(createSingleEmojiButton(tag, emojiPopup));
            }
            
            JPanel alignLeftWrap = new JPanel(new BorderLayout()); 
            alignLeftWrap.setOpaque(false); 
            alignLeftWrap.add(gridPanel, BorderLayout.WEST);
            
            mainScrollContent.add(alignLeftWrap);
            mainScrollContent.add(Box.createVerticalStrut(15));
        }

        JScrollPane scrollPane = new JScrollPane(mainScrollContent);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(320, 260)); 
        scrollPane.getVerticalScrollBar().setUnitIncrement(25);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // --- MỤC 3: THANH ĐIỀU HƯỚNG DƯỚI ĐÁY (HỖ TRỢ CUỘN NGANG) ---
        JPanel bottomNav = new JPanel();
        bottomNav.setLayout(new BoxLayout(bottomNav, BoxLayout.X_AXIS)); // Ép các Icon nằm trên 1 hàng ngang duy nhất
        bottomNav.setBackground(Color.WHITE);
        bottomNav.setBorder(new EmptyBorder(4, 4, 4, 4));
        
        // Thêm nút "Gần đây"
        bottomNav.add(createNavIconButton("https://img.icons8.com/fluency-systems-regular/48/2563EB/time.png", () -> {
            scrollPane.getViewport().setViewPosition(new Point(0, 0));
        }));
        bottomNav.add(Box.createHorizontalStrut(6));

        // Thêm các nút Chủ đề
        for (EmojiCategory cat : categories) {
            bottomNav.add(createNavIconButton(cat.navIconUrl, () -> {
                JPanel target = categoryPanels.get(cat.prefix);
                if (target != null) {
                    SwingUtilities.invokeLater(() -> scrollPane.getViewport().setViewPosition(new Point(0, target.getY())));
                }
            }));
            bottomNav.add(Box.createHorizontalStrut(6));
        }
        
        // Gắn thanh Nav vào một JScrollPane để có thể cuộn ngang nếu thiếu chỗ
        JScrollPane bottomScroll = new JScrollPane(bottomNav);
        bottomScroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.decode("#E5E7EB")));
        bottomScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        bottomScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        bottomScroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 6)); // Thu gọn thanh cuộn ngang
        bottomScroll.setPreferredSize(new Dimension(320, 46));

        // CHỨC NĂNG ZALO: Hỗ trợ dùng con lăn chuột (Mouse Wheel) để cuộn thanh ngang dưới đáy
        bottomScroll.addMouseWheelListener(e -> {
            if (e.getScrollType() == java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                JScrollBar hBar = bottomScroll.getHorizontalScrollBar();
                hBar.setValue(hBar.getValue() + e.getUnitsToScroll() * 20); // Tốc độ cuộn ngang
            }
        });

        // Bố cục tổng thể
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.add(scrollPane, BorderLayout.CENTER);
        container.add(bottomScroll, BorderLayout.SOUTH);

        emojiPopup.add(container);
        emojiPopup.show(invoker, -280, -320); 
    }

    // --- CÁC HÀM TIỆN ÍCH HỖ TRỢ GIAO DIỆN EMOJI MỚI ---

    // Hàm tạo tiêu đề (VD: "Cảm xúc", "Gần đây")
    private JPanel createEmojiSectionTitle(String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        p.setBackground(Color.WHITE);
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(Color.decode("#374151"));
        p.add(lbl);
        return p;
    }

    // Hàm tạo 1 Icon Emoji và gán sự kiện Click
    private JLabel createSingleEmojiButton(String tag, JPopupMenu popup) {
        JLabel lbl = new JLabel("", SwingConstants.CENTER);
        lbl.setPreferredSize(new Dimension(34, 34));
        lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Cắt tag [e:mat:1] để lấy đường dẫn thật
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[e:([a-zA-Z0-9_]+):(\\d+)\\]").matcher(tag);
            if (m.find()) {
                String prefix = m.group(1);
                String num = m.group(2);
                URL url = getClass().getResource("/images/emoji/" + prefix + "/" + prefix + " (" + num + ").png");
                if (url != null) lbl.setIcon(new ImageIcon(new ImageIcon(url).getImage().getScaledInstance(26, 26, Image.SCALE_SMOOTH)));
            }
        } catch (Exception e) {}

        // Hiệu ứng Hover background
        lbl.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { lbl.setOpaque(true); lbl.setBackground(Color.decode("#F1F5F9")); lbl.repaint(); }
            @Override public void mouseExited(MouseEvent e) { lbl.setOpaque(false); lbl.repaint(); }
            @Override public void mouseClicked(MouseEvent e) {
                // Thêm chữ vào khung chat
                txtChatInput.setText(txtChatInput.getText() + tag);
                txtChatInput.requestFocus();
                
                // Logic cập nhật mảng "Gần đây"
                recentEmojis.remove(tag); // Xóa nếu đã có để đôn lên đầu
                recentEmojis.add(0, tag); // Thêm lên vị trí số 1
                if (recentEmojis.size() > 16) recentEmojis.remove(16); // Giới hạn lưu 16 cái gần nhất (2 dòng)
                
                popup.setVisible(false);
            }
        });
        return lbl;
    }

    // Hàm tạo nút Icon điều hướng dưới đáy
    private JPanel createNavIconButton(String iconUrl, Runnable onClick) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setPreferredSize(new Dimension(32, 32));
        p.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JLabel lbl = new JLabel("", SwingConstants.CENTER);
        setNetworkIcon(lbl, iconUrl, 18, 18);
        p.add(lbl, BorderLayout.CENTER);
        
        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { p.setBackground(Color.decode("#F1F5F9")); }
            @Override public void mouseExited(MouseEvent e) { p.setBackground(Color.WHITE); }
            @Override public void mouseClicked(MouseEvent e) { onClick.run(); }
        });
        return p;
    }

    // 2. Thuật toán Regex render Emoji (Không dùng vòng lặp -> Siêu nhanh)
    // Thuật toán Regex render Emoji (Hỗ trợ cả cú pháp mới và cũ)
    private String parseEmojis(String text) {
        if (text == null || (!text.contains("[e:") && !text.contains("[e"))) return text;
        
        StringBuffer sb = new StringBuffer();
        
        // --- LỚP 1: Quét cú pháp MỚI (Ví dụ: [e:mat:1]) ---
        java.util.regex.Pattern patternNew = java.util.regex.Pattern.compile("\\[e:([a-zA-Z0-9_]+):(\\d+)\\]");
        java.util.regex.Matcher matcherNew = patternNew.matcher(text);
        
        while (matcherNew.find()) {
            String prefix = matcherNew.group(1); 
            String num = matcherNew.group(2);    
            URL url = getClass().getResource("/images/emoji/" + prefix + "/" + prefix + " (" + num + ").png");
            
            if (url != null) {
                matcherNew.appendReplacement(sb, "<img src='" + url.toString() + "' width='24' height='24'>");
            } else {
                matcherNew.appendReplacement(sb, matcherNew.group(0)); 
            }
        }
        matcherNew.appendTail(sb);
        
        // Lấy kết quả sau khi quét Lớp 1 để quét tiếp Lớp 2
        String tempText = sb.toString();
        sb = new StringBuffer();
        
        // --- LỚP 2: Quét cú pháp CŨ tương thích ngược (Ví dụ: [e1]) ---
        // Thuật toán sẽ tự động gán các mã cũ này về thư mục "mat"
        java.util.regex.Pattern patternOld = java.util.regex.Pattern.compile("\\[e(\\d+)\\]");
        java.util.regex.Matcher matcherOld = patternOld.matcher(tempText);
        
        while (matcherOld.find()) {
            String num = matcherOld.group(1);
            URL url = getClass().getResource("/images/emoji/mat/mat (" + num + ").png");
            
            if (url != null) {
                matcherOld.appendReplacement(sb, "<img src='" + url.toString() + "' width='24' height='24'>");
            } else {
                matcherOld.appendReplacement(sb, matcherOld.group(0));
            }
        }
        matcherOld.appendTail(sb);
        
        return sb.toString();
    }
   private void chooseAndSendImage() {
        if (activeConversation == null) return;

        if (activeConversation.conversationId < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một người bạn để gửi ảnh!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Hình ảnh (JPG, PNG)", "jpg", "png", "jpeg"));
        chooser.setMultiSelectionEnabled(true);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            uploadImageFiles(chooser.getSelectedFiles());
        }
    }

    private void uploadDocumentFiles(java.io.File[] files) {
        if (activeConversation == null || activeConversation.conversationId < 0) return;
        for (java.io.File file : files) {
            String fileName = file.getName();
            if (file.length() > 10 * 1024 * 1024) {
                JOptionPane.showMessageDialog(this, "Tệp " + fileName + " vượt quá 10MB và sẽ bị bỏ qua!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            
            long fileSizeKb = file.length() / 1024;
            String sizeStr = fileSizeKb > 1024 ? String.format("%.1f MB", fileSizeKb / 1024.0) : fileSizeKb + " KB";

            Message m = new Message();
            m.clientMessageId = java.util.UUID.randomUUID().toString();
            m.senderId = CURRENT_USER_ID;
            m.senderType = "ME";
            m.messageType = "FILE";
            m.content = "[FILE]" + fileName + "|UPLOADING|" + sizeStr; 
            m.sentAt = new Date();
            m.markPendingSend();

            if (currentMessages == null) currentMessages = new ArrayList<>();
            currentMessages.add(m);

            activeConversation.lastMessage = "Bạn: [Tài liệu] " + fileName;
            activeConversation.lastMessageTime = m.sentAt;

            refreshMessages();
            refreshConversationList();

            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() throws Exception { return uploadToS3Backblaze(file); }
                @Override protected void done() {
                    try {
                        String fileUrl = get();
                        if (fileUrl != null) {
                            m.content = "[FILE]" + fileName + "|" + fileUrl + "|" + sizeStr;
                            refreshMessages();
                            messageSyncService.saveOutgoingMessage(activeConversation.conversationId, m);
                            messageSyncService.sendChatMessage(activeConversation.conversationId, m);
                        } else {
                            currentMessages.remove(m); refreshMessages();
                            JOptionPane.showMessageDialog(ChatTab.this, "Lỗi tải tệp " + fileName + " lên máy chủ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
            }.execute();
        }
    }

    private void uploadImageFiles(java.io.File[] files) {
        if (activeConversation == null || activeConversation.conversationId < 0) return;
        for (java.io.File file : files) {
            Message m = new Message();
            m.clientMessageId = java.util.UUID.randomUUID().toString();
            m.senderId = CURRENT_USER_ID;
            m.senderType = "ME";
            m.messageType = "IMAGE";
            m.content = "[IMG_URL]UPLOADING"; 
            m.sentAt = new Date();
            m.markPendingSend();

            if (currentMessages == null) currentMessages = new ArrayList<>();
            currentMessages.add(m);

            activeConversation.lastMessage = "Bạn: [Đang gửi ảnh...]";
            activeConversation.lastMessageTime = m.sentAt;

            refreshMessages();
            refreshConversationList();

            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() throws Exception { return uploadToS3Backblaze(file); }
                @Override protected void done() {
                    try {
                        String imageUrl = get();
                        if (imageUrl != null) {
                            m.content = "[IMG_URL]" + imageUrl;
                            activeConversation.lastMessage = "Bạn: [Hình ảnh]";
                            refreshMessages(); refreshConversationList();
                            messageSyncService.saveOutgoingMessage(activeConversation.conversationId, m);
                            messageSyncService.sendChatMessage(activeConversation.conversationId, m);
                        } else {
                            currentMessages.remove(m); activeConversation.lastMessage = "Gửi ảnh thất bại!";
                            refreshMessages(); refreshConversationList();
                            JOptionPane.showMessageDialog(ChatTab.this, "Lỗi khi tải ảnh " + file.getName() + " lên máy chủ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
            }.execute();
        }
    }

    private JPanel createImageMessage(Message m, boolean isFirstInGroup) {
        JPanel p = new JPanel(new BorderLayout(12, 0)); 
        p.setOpaque(false); 
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300)); // Tăng size cho đẹp
        
        boolean isMe = "ME".equals(m.senderType);
        
        JPanel bubbleWrap = new JPanel(); 
        bubbleWrap.setLayout(new BoxLayout(bubbleWrap, BoxLayout.Y_AXIS)); 
        bubbleWrap.setOpaque(false);
        
        // Bọc viền ảnh (Màu xanh Primary giống trong mockup của bạn)
        RoundedPanel bubble = new RoundedPanel(12, Color.WHITE); 
        bubble.setBorder(BorderFactory.createLineBorder(isMe ? PRIMARY : BORDER_COLOR, 2)); 
        bubble.setLayout(new BorderLayout());
        
        JLabel lblImg = new JLabel("", SwingConstants.CENTER); 
        Image finalImg = null;
        try {
            String base64 = m.content.replace("[IMG]", "");
            byte[] bytes = Base64.getDecoder().decode(base64);
            finalImg = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            lblImg.setIcon(new ImageIcon(finalImg));
        } catch (Exception e) {
            lblImg.setText(" [Lỗi hiển thị ảnh] ");
            lblImg.setForeground(Color.RED);
        }
        bubble.add(lblImg, BorderLayout.CENTER);
        
        // --- THÊM MENU CHUỘT PHẢI: COPY / LƯU ẢNH ---
        if (finalImg != null) {
            Image currentImg = finalImg;
            JPopupMenu imgMenu = new JPopupMenu();
            
            JMenuItem mnuCopy = new JMenuItem("  Sao chép hình ảnh");
            mnuCopy.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageSelection(currentImg), null));
            
            JMenuItem mnuSave = new JMenuItem("  Lưu ảnh về máy...");
            mnuSave.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Chọn nơi lưu ảnh");
                if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    try {
                        java.io.File f = fc.getSelectedFile();
                        if(!f.getName().toLowerCase().endsWith(".png")) f = new java.io.File(f.getAbsolutePath() + ".png");
                        javax.imageio.ImageIO.write((java.awt.image.BufferedImage)currentImg, "png", f);
                        JOptionPane.showMessageDialog(this, "Đã lưu ảnh thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    } catch(Exception ex) { JOptionPane.showMessageDialog(this, "Lỗi khi lưu ảnh!", "Lỗi", JOptionPane.ERROR_MESSAGE); }
                }
            });
            imgMenu.add(mnuCopy); 
            imgMenu.addSeparator();
            imgMenu.add(mnuSave);

            lblImg.setCursor(new Cursor(Cursor.HAND_CURSOR));
            lblImg.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    // Kích chuột phải để hiện Menu
                    if (SwingUtilities.isRightMouseButton(e)) {
                        imgMenu.show(lblImg, e.getX(), e.getY());
                    }
                }
            });
        }
        // ------------------------------------------

        bubbleWrap.add(bubble); 
        if(isFirstInGroup) { 
            JPanel timePanel = new JPanel(new FlowLayout(isMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 4, 0)); 
            timePanel.setOpaque(false);
            String timeStr = m.sentAt != null ? timeFormat.format(m.sentAt) : ""; 
            JLabel lblTime = new JLabel(timeStr); 
            lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 11)); 
            lblTime.setForeground(TEXT_MUTED);
            timePanel.add(lblTime);
            if (isMe) {
                JLabel checkIcon = new JLabel(); 
                setNetworkIcon(checkIcon, m.isReadStatus() ? "https://img.icons8.com/fluency-systems-filled/48/2563EB/double-tick.png" : "https://img.icons8.com/fluency-systems-filled/48/94A3B8/double-tick.png", 14, 14); 
                timePanel.add(checkIcon);
            }
            bubbleWrap.add(Box.createVerticalStrut(4)); 
            bubbleWrap.add(timePanel); 
        }
        
        JPanel align = new JPanel(new FlowLayout(isMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0)); 
        align.setOpaque(false); align.add(bubbleWrap); 
        
        if (!isMe) {
            JPanel avaWrap = new JPanel(new BorderLayout()); avaWrap.setOpaque(false); avaWrap.setPreferredSize(new Dimension(36, 36));
            if(isFirstInGroup) { JLabel ava = new JLabel(); setAvatarIcon(ava, activeConversation.avatarUrl, 36); avaWrap.add(ava, BorderLayout.NORTH); }
            p.add(avaWrap, BorderLayout.WEST);
        }
        p.add(align, BorderLayout.CENTER); 
        return p;
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

    class RoundedPanel extends JPanel { 
        private int radius; private Color bgColor; 
        public RoundedPanel(int radius, Color bgColor) { this.radius = radius; this.bgColor = bgColor; setOpaque(false); } 
        @Override protected void paintComponent(Graphics g) { 
            Graphics2D g2 = (Graphics2D) g.create(); 
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
            g2.setColor(bgColor); g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius); 
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
    
    // =========================================================================
    // LỚP HỖ TRỢ COPY ẢNH VÀO CLIPBOARD (NHƯ ZALO)
    // =========================================================================
    static class ImageSelection implements java.awt.datatransfer.Transferable {
        private Image image;
        public ImageSelection(Image image) { this.image = image; }
        public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() { return new java.awt.datatransfer.DataFlavor[]{java.awt.datatransfer.DataFlavor.imageFlavor}; }
        public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor flavor) { return java.awt.datatransfer.DataFlavor.imageFlavor.equals(flavor); }
        public Object getTransferData(java.awt.datatransfer.DataFlavor flavor) throws java.awt.datatransfer.UnsupportedFlavorException {
            if (!java.awt.datatransfer.DataFlavor.imageFlavor.equals(flavor)) throw new java.awt.datatransfer.UnsupportedFlavorException(flavor);
            return image;
        }
    }

    public java.util.List<ConversationInfo> getConversations() {
        return this.conversations;
    }
    
    // =========================================================================
    // TÍCH HỢP ĐỘNG CƠ ELECTRON VIDEO CALL & AUDIO CALL
    // =========================================================================
   // =========================================================================
    // TÍCH HỢP ĐỘNG CƠ ELECTRON VIDEO CALL & AUDIO CALL
    // =========================================================================
    private void startVideoCall() {
        if (activeConversation == null || activeConversation.conversationId < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng kết bạn để gọi video!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String roomName = "ChatRoom_Private_" + activeConversation.conversationId;
        String myName = "User_" + CURRENT_USER_ID; 

        // Khởi động luồng Signaling Gọi điện qua WebRTC
        com.mycompany.tutorhub_enterprise.client.managers.CallManager.getInstance().initCall(
            String.valueOf(activeConversation.conversationId),
            roomName,
            myName,
            activeConversation.displayName,
            true
        );
    }

    private void startAudioCall() {
        if (activeConversation == null || activeConversation.conversationId < 0) return;

        String roomName = "ChatRoom_Private_" + activeConversation.conversationId;
        String myName = "User_" + CURRENT_USER_ID; 

        // Khởi động luồng Signaling Gọi điện
        com.mycompany.tutorhub_enterprise.client.managers.CallManager.getInstance().initCall(
            String.valueOf(activeConversation.conversationId),
            roomName,
            myName,
            activeConversation.displayName,
            false
        );
    }
    // Hàm hiển thị Popup xem ảnh full size
    // --- HÀM TẠO CỬA SỔ XEM ẢNH FULL SIZE ---
   // --- HÀM TẠO CỬA SỔ XEM ẢNH FULL SIZE (BẢN CHỐNG LỖI) ---
   // --- HÀM TẠO CỬA SỔ XEM ẢNH FULL SIZE (CÓ THU PHÓNG, XOAY, CHIA SẺ) ---
    private void showImagePreview(Message startMsg) {
        java.util.List<Message> imgMsgs = new ArrayList<>();
        for(Message msg : currentMessages) {
            if ("IMAGE".equals(msg.messageType) && msg.content != null && msg.content.startsWith("[IMG_URL]") && !msg.content.contains("UPLOADING")) {
                if (iconCache.containsKey("CHAT_ORIG_" + msg.content.hashCode())) {
                    imgMsgs.add(msg);
                }
            }
        }
        
        final int[] currentIndex = { imgMsgs.indexOf(startMsg) };
        if (currentIndex[0] == -1) {
            if(imgMsgs.isEmpty()) return;
            currentIndex[0] = 0;
        }

        JDialog viewer = new JDialog((java.awt.Frame) null, true);
        viewer.setUndecorated(true);
        viewer.getContentPane().setBackground(new Color(24, 24, 24)); 
        viewer.setLayout(new BorderLayout());

        final double[] state = {1.0, 0.0}; 

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 20));
        topPanel.setOpaque(false);
        JPanel btnClose = createDarkToolButton("https://img.icons8.com/fluency-systems-regular/24/FFFFFF/multiply.png", "Đóng (Esc)");
        btnClose.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { viewer.dispose(); }
        });
        topPanel.add(btnClose);
        viewer.add(topPanel, BorderLayout.NORTH);

        JLabel imgLabel = new JLabel();
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JScrollPane scrollPane = new JScrollPane(imgLabel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));
        viewer.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPrev = createDarkToolButton("https://img.icons8.com/fluency-systems-regular/24/FFFFFF/back.png", "Ảnh trước");
        JPanel btnNext = createDarkToolButton("https://img.icons8.com/fluency-systems-regular/24/FFFFFF/forward.png", "Ảnh tiếp theo");

        Runnable loadCurrentIcon = () -> {
            Message currentMsg = imgMsgs.get(currentIndex[0]);
            ImageIcon icon = iconCache.get("CHAT_ORIG_" + currentMsg.content.hashCode());
            Image rawImg = icon.getImage();
            int rawW = rawImg.getWidth(null);
            int rawH = rawImg.getHeight(null);

            java.awt.image.BufferedImage originalImage = new java.awt.image.BufferedImage(rawW, rawH, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = originalImage.createGraphics();
            g2.drawImage(rawImg, 0, 0, null);
            g2.dispose();
            
            state[1] = 0;
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int maxWidth = (int) (screenSize.width * 0.9); 
            int maxHeight = (int) (screenSize.height * 0.75);
            if (rawW > maxWidth || rawH > maxHeight) {
                double widthRatio = (double) maxWidth / rawW;
                double heightRatio = (double) maxHeight / rawH;
                state[0] = Math.min(widthRatio, heightRatio);
            } else {
                state[0] = 1.0;
            }
            
            imgLabel.putClientProperty("originalImage", originalImage);
            imgLabel.putClientProperty("rawW", rawW);
            imgLabel.putClientProperty("rawH", rawH);
            
            btnPrev.setVisible(currentIndex[0] > 0);
            btnNext.setVisible(currentIndex[0] < imgMsgs.size() - 1);
        };

        Runnable updateDisplay = () -> {
            java.awt.image.BufferedImage originalImage = (java.awt.image.BufferedImage) imgLabel.getClientProperty("originalImage");
            if(originalImage == null) return;
            int rawW = (int) imgLabel.getClientProperty("rawW");
            int rawH = (int) imgLabel.getClientProperty("rawH");

            int currentRot = (int) state[1] % 360;
            if (currentRot < 0) currentRot += 360;

            boolean swap = (currentRot == 90 || currentRot == 270);
            int baseW = swap ? rawH : rawW;
            int baseH = swap ? rawW : rawH;

            int newW = (int) (baseW * state[0]);
            int newH = (int) (baseH * state[0]);
            
            if (newW < 50 || newH < 50) return;

            java.awt.image.BufferedImage transformed = new java.awt.image.BufferedImage(newW, newH, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = transformed.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.translate(newW / 2.0, newH / 2.0);
            g.rotate(Math.toRadians(state[1]));
            g.scale(state[0], state[0]);
            g.translate(-rawW / 2.0, -rawH / 2.0);

            g.drawImage(originalImage, 0, 0, null);
            g.dispose();

            imgLabel.setIcon(new ImageIcon(transformed));
            imgLabel.revalidate();
            imgLabel.repaint();
        };

        loadCurrentIcon.run();
        updateDisplay.run();

        btnPrev.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { 
                if (currentIndex[0] > 0) { currentIndex[0]--; loadCurrentIcon.run(); updateDisplay.run(); }
            }
        });
        btnNext.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { 
                if (currentIndex[0] < imgMsgs.size() - 1) { currentIndex[0]++; loadCurrentIcon.run(); updateDisplay.run(); }
            }
        });

        JPanel leftPanel = new JPanel(new GridBagLayout()); leftPanel.setOpaque(false); leftPanel.add(btnPrev); leftPanel.setBorder(new EmptyBorder(0, 20, 0, 20));
        JPanel rightPanel = new JPanel(new GridBagLayout()); rightPanel.setOpaque(false); rightPanel.add(btnNext); rightPanel.setBorder(new EmptyBorder(0, 20, 0, 20));
        viewer.add(leftPanel, BorderLayout.WEST);
        viewer.add(rightPanel, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 20));
        bottomPanel.setOpaque(false);

        JPanel btnSave = createDarkToolButton("https://img.icons8.com/fluency-systems-regular/24/FFFFFF/download.png", "Lưu về máy");
        btnSave.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                JFileChooser fc = new JFileChooser(); fc.setDialogTitle("Chọn nơi lưu ảnh");
                if (fc.showSaveDialog(viewer) == JFileChooser.APPROVE_OPTION) {
                    try {
                        java.io.File f = fc.getSelectedFile(); if(!f.getName().toLowerCase().endsWith(".png")) f = new java.io.File(f.getAbsolutePath() + ".png");
                        javax.imageio.ImageIO.write((java.awt.image.BufferedImage) imgLabel.getClientProperty("originalImage"), "png", f); 
                        JOptionPane.showMessageDialog(viewer, "Đã lưu ảnh thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    } catch(Exception ex) { JOptionPane.showMessageDialog(viewer, "Lỗi khi lưu ảnh!", "Lỗi", JOptionPane.ERROR_MESSAGE); }
                }
            }
        });

        JPanel btnCopy = createDarkToolButton("https://img.icons8.com/fluency-systems-regular/24/FFFFFF/copy.png", "Sao chép ảnh");
        btnCopy.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageSelection((java.awt.image.BufferedImage) imgLabel.getClientProperty("originalImage")), null);
                JOptionPane.showMessageDialog(viewer, "Đã sao chép ảnh gốc!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JLabel separator = new JLabel("|"); separator.setForeground(new Color(255, 255, 255, 100)); separator.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        JPanel btnZoomIn = createDarkToolButton("https://img.icons8.com/fluency-systems-regular/24/FFFFFF/zoom-in.png", "Phóng to");
        btnZoomIn.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { state[0] *= 1.25; updateDisplay.run(); } });
        JPanel btnZoomOut = createDarkToolButton("https://img.icons8.com/fluency-systems-regular/24/FFFFFF/zoom-out.png", "Thu nhỏ");
        btnZoomOut.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { state[0] /= 1.25; updateDisplay.run(); } });
        JPanel btnRotLeft = createDarkToolButton("https://img.icons8.com/fluency-systems-regular/24/FFFFFF/rotate-left.png", "Xoay trái");
        btnRotLeft.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { state[1] -= 90; updateDisplay.run(); } });
        JPanel btnRotRight = createDarkToolButton("https://img.icons8.com/fluency-systems-regular/24/FFFFFF/rotate-right.png", "Xoay phải");
        btnRotRight.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { state[1] += 90; updateDisplay.run(); } });

        bottomPanel.add(btnSave); bottomPanel.add(btnCopy); bottomPanel.add(Box.createHorizontalStrut(10));
        bottomPanel.add(separator); bottomPanel.add(Box.createHorizontalStrut(10));
        bottomPanel.add(btnZoomIn); bottomPanel.add(btnZoomOut); bottomPanel.add(btnRotLeft); bottomPanel.add(btnRotRight);
        viewer.add(bottomPanel, BorderLayout.SOUTH);

        viewer.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ESCAPE");
        viewer.getRootPane().getActionMap().put("ESCAPE", new AbstractAction() { public void actionPerformed(ActionEvent e) { viewer.dispose(); } });
        
        viewer.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "LEFT");
        viewer.getRootPane().getActionMap().put("LEFT", new AbstractAction() { public void actionPerformed(ActionEvent e) { 
            if (currentIndex[0] > 0) { currentIndex[0]--; loadCurrentIcon.run(); updateDisplay.run(); }
        } });
        viewer.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "RIGHT");
        viewer.getRootPane().getActionMap().put("RIGHT", new AbstractAction() { public void actionPerformed(ActionEvent e) { 
            if (currentIndex[0] < imgMsgs.size() - 1) { currentIndex[0]++; loadCurrentIcon.run(); updateDisplay.run(); }
        } });

        scrollPane.addMouseWheelListener(e -> {
            if (e.getPreciseWheelRotation() < 0) state[0] *= 1.1; 
            else state[0] /= 1.1; 
            updateDisplay.run();
        });

        viewer.setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        viewer.setLocationRelativeTo(null); 
        viewer.setVisible(true); 
    }

    // --- HÀM HỖ TRỢ TẠO NÚT BẤM THANH LỊCH (HELPER METHOD) ---
  private JPanel createDarkToolButton(String iconUrl, String toolTip) {
        JPanel p = new JPanel(new BorderLayout()) {
            boolean hover = false;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); 
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if(hover) {
                    g2.setColor(new Color(255, 255, 255, 60)); // Rê chuột vào thì sáng lên
                } else {
                    g2.setColor(new Color(255, 255, 255, 30)); // Bình thường mờ mờ trong suốt
                }
                g2.fillOval(0, 0, getWidth(), getHeight()); 
                g2.dispose();
            }
            public void setHover(boolean h) { this.hover = h; repaint(); }
        };
        p.setOpaque(false); 
        p.setPreferredSize(new Dimension(48, 48)); // Kích thước nút chuẩn Zalo
        p.setCursor(new Cursor(Cursor.HAND_CURSOR));
        p.setToolTipText(toolTip);
        
        JLabel lbl = new JLabel("", SwingConstants.CENTER); 
        
        // Tải ảnh trực tiếp từ Icon8 thay vì dùng DatabaseManager
        new Thread(() -> { 
            try { 
                Image img = new ImageIcon(new URL(iconUrl)).getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH); 
                SwingUtilities.invokeLater(() -> lbl.setIcon(new ImageIcon(img))); 
            } catch (Exception e) {} 
        }).start();

        p.add(lbl, BorderLayout.CENTER);
        
        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { try { p.getClass().getMethod("setHover", boolean.class).invoke(p, true); } catch(Exception ex){} }
            @Override public void mouseExited(MouseEvent e) { try { p.getClass().getMethod("setHover", boolean.class).invoke(p, false); } catch(Exception ex){} }
        });
        
        return p;
    }
    // --- HÀM TẠO ẢNH BO GÓC & THU NHỎ (TÍCH HỢP 2 TRONG 1) ---
    private Image makeRoundedImage(Image img, int width, int height, int radius) {
        // Tạo một bức ảnh rỗng có nền trong suốt (ARGB)
        java.awt.image.BufferedImage bimage = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = bimage.createGraphics();
        
        // Bật chế độ khử răng cưa để góc bo được mịn màng
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Vẽ một khối hình chữ nhật bo góc làm khuôn
        g2.fillRoundRect(0, 0, width, height, radius, radius);
        
        // Áp dụng chế độ SrcIn: Chỉ vẽ bức ảnh gốc lọt thỏm vào trong cái khuôn bo góc vừa tạo
        g2.setComposite(java.awt.AlphaComposite.SrcIn);
        g2.drawImage(img, 0, 0, width, height, null);
        g2.dispose();
        
        return bimage;
    }
    
    // =========================================================================
    // LỚP INNER CLASS: BẢNG GIỚI THIỆU CHUẨN ZALO PC
    // =========================================================================
    // =========================================================================
    // LỚP INNER CLASS: BẢNG GIỚI THIỆU CHUẨN ZALO PC (DÙNG ẢNH LOCAL)
    // =========================================================================
   // =========================================================================
    // LỚP INNER CLASS: OVERHAUL SLIDER GIỚI THIỆU PHÓNG TO (DÙNG ẢNH 16:9 LOCAL)
    // =========================================================================
    class WelcomeSliderPanel extends JPanel {
        private int currentSlide = 0;
        private javax.swing.Timer timer;
        private JLabel lblImage, lblWelcome, lblSub, lblTitle, lblDesc;
        private JPanel dotsPanel;

        // --- CẤU HÌNH KÍCH THƯỚC ĐỂ LẤP ĐẦY KHOẢNG TRỐNG ---
        // Giữ đúng tỷ lệ 1365x768 (xấp xỉ 16:9), phóng to chiều ngang lên 860px
        private final int TARGET_WIDTH = 860; 
        private final int TARGET_HEIGHT = (int)(TARGET_WIDTH * (768.0 / 1365.0)); // = ~484px

        // BƯỚC QUAN TRỌNG: Sửa lại đường dẫn đến 4 file ảnh trong NetBeans của bạn
        private String[][] slides = {
           {"/images/br1.png", "Nhắn tin mượt mà", "Trò chuyện, trao đổi tài liệu học tập theo thời gian thực."},
            {"/images/br2.png", "Gọi Video sắc nét", "Tương tác trực tiếp với học sinh qua hệ thống Video Call chuẩn HD."},
            {"/images/br3.png", "Gửi emotion thả ga", "Hỗ trợ gửi đa dạng các loại emotion, biểu diễn cảm xúc trọn vẹn."},
            {"/images/br4.png", "Trợ lý ảo thông minh", "Kết hợp AI chat bot tác trực tuyến giúp giải đáp mọi thắc mắc."}
        };

        public WelcomeSliderPanel() {
            // Dùng BorderLayout bên ngoài để Slider "ôm" trọn panel cha
            setLayout(new BorderLayout());
            setOpaque(false);

            // --- HEADER: TIÊU ĐỀ CHÀO MỪNG ---
            JPanel headerWrap = new JPanel();
            headerWrap.setLayout(new BoxLayout(headerWrap, BoxLayout.Y_AXIS));
            headerWrap.setOpaque(false);
            headerWrap.setBorder(new EmptyBorder(30, 20, 20, 20)); // Padding phía trên

            lblWelcome = new JLabel("Chào mừng đến với TutorHub Enterprise!");
            lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 28)); // Phóng to font
            lblWelcome.setForeground(TEXT_MAIN);
            lblWelcome.setAlignmentX(Component.CENTER_ALIGNMENT);

            lblSub = new JLabel("Khám phá hệ thống quản lý và tương tác giáo dục tối ưu nhất dành cho bạn.");
            lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 16)); // Phóng to font
            lblSub.setForeground(TEXT_MUTED);
            lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

            headerWrap.add(lblWelcome);
            headerWrap.add(Box.createVerticalStrut(12)); // Khoảng cách
            headerWrap.add(lblSub);
            
            add(headerWrap, BorderLayout.NORTH); // Đặt ở phía trên

            // --- CENTER: KHU VỰC ẢNH VÀ NỘI DUNG ---
            JPanel centerWrap = new JPanel();
            centerWrap.setLayout(new BoxLayout(centerWrap, BoxLayout.Y_AXIS));
            centerWrap.setOpaque(false);

            // 1. Panel chứa ảnh và nút mũi tên (Tăng kích thước)
            lblImage = new JLabel("", SwingConstants.CENTER);
            lblImage.setPreferredSize(new Dimension(TARGET_WIDTH, TARGET_HEIGHT));
            // Tạo border để ảnh không bị dính sát lề
            lblImage.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 1, 1, 1, Color.decode("#E2E8F0")), // Viền mảnh xám
                    new EmptyBorder(0,0,0,0) // Padding trong ảnh
            ));

            JPanel imgNavWrap = new JPanel(new BorderLayout(15, 0)); // Tăng khoảng cách giữa mũi tên và ảnh
            imgNavWrap.setOpaque(false);
            // Kích thước tối đa của cụm ảnh + mũi tên
            imgNavWrap.setMaximumSize(new Dimension(TARGET_WIDTH + 120, TARGET_HEIGHT + 10));

            JLabel btnPrev = new JLabel();
            setNetworkIcon(btnPrev, "https://img.icons8.com/fluency-systems-regular/48/94A3B8/back.png", 36, 36);
            btnPrev.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnPrev.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { currentSlide = (currentSlide - 1 + slides.length) % slides.length; updateSlide(); timer.restart(); }
            });

            JLabel btnNext = new JLabel();
            setNetworkIcon(btnNext, "https://img.icons8.com/fluency-systems-regular/48/Forward.png", 36, 36);
            btnNext.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnNext.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { currentSlide = (currentSlide + 1) % slides.length; updateSlide(); timer.restart(); }
            });

            imgNavWrap.add(btnPrev, BorderLayout.WEST);
            imgNavWrap.add(lblImage, BorderLayout.CENTER);
            imgNavWrap.add(btnNext, BorderLayout.EAST);

            // 2. Nội dung text (Dưới ảnh)
            lblTitle = new JLabel("", SwingConstants.CENTER);
            lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 19)); // Phóng to font
            lblTitle.setForeground(PRIMARY);
            lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

            lblDesc = new JLabel("", SwingConstants.CENTER);
            lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 15)); // Phóng to font
            lblDesc.setForeground(TEXT_MUTED);
            lblDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

            // 3. Thanh dấu chấm (Dưới text)
            dotsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    int dotWidth = 8;
                    int activeWidth = 24;
                    int startX = (getWidth() - (activeWidth + (slides.length - 1) * dotWidth + (slides.length - 1) * 8)) / 2;
                    
                    for (int i = 0; i < slides.length; i++) {
                        if (i == currentSlide) {
                            g2.setColor(PRIMARY);
                            g2.fillRoundRect(startX, 0, activeWidth, dotWidth, dotWidth, dotWidth);
                            startX += activeWidth + 8;
                        } else {
                            g2.setColor(Color.decode("#CBD5E1"));
                            g2.fillOval(startX, 0, dotWidth, dotWidth);
                            startX += dotWidth + 8;
                        }
                    }
                    g2.dispose();
                }
            };
            dotsPanel.setOpaque(false);
            dotsPanel.setPreferredSize(new Dimension(200, 10));
            dotsPanel.setMaximumSize(new Dimension(200, 10));
            dotsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Lắp ráp cụm center (Box.createVerticalGlue để tự động giãn cách)
            centerWrap.add(Box.createVerticalGlue()); // Đẩy cụm center vào giữa
            centerWrap.add(imgNavWrap);
            centerWrap.add(Box.createVerticalStrut(25)); // Khoảng cách text - ảnh
            centerWrap.add(lblTitle);
            centerWrap.add(Box.createVerticalStrut(10)); // Khoảng cách text - mô tả
            centerWrap.add(lblDesc);
            centerWrap.add(Box.createVerticalStrut(30)); // Khoảng cách mô tả - dots
            centerWrap.add(dotsPanel);
            centerWrap.add(Box.createVerticalGlue()); // Đẩy cụm center vào giữa

            add(centerWrap, BorderLayout.CENTER); // Đặt cụm nội dung chính ở giữa

            updateSlide();

            timer = new javax.swing.Timer(3500, e -> {
                currentSlide = (currentSlide + 1) % slides.length;
                updateSlide();
            });
            timer.start();
        }

        private void updateSlide() {
            SwingUtilities.invokeLater(() -> {
                lblTitle.setText(slides[currentSlide][1]);
                lblDesc.setText(slides[currentSlide][2]);
                
                String imagePath = slides[currentSlide][0];
                try {
                    // PHÓNG TO ẢNH FULL THEO KÍCH THƯỚC TARGET
                    java.net.URL imgUrl = getClass().getResource(imagePath);
                    if (imgUrl != null) {
                        Image img = new ImageIcon(imgUrl).getImage().getScaledInstance(TARGET_WIDTH, TARGET_HEIGHT, Image.SCALE_SMOOTH);
                        lblImage.setIcon(new ImageIcon(img));
                        lblImage.setText(""); 
                    } else {
                        lblImage.setIcon(null);
                        lblImage.setText("<html><i style='color:red;'>Không tìm thấy ảnh: " + imagePath + "</i></html>");
                    }
                } catch (Exception e) {
                    lblImage.setIcon(null);
                    lblImage.setText("Lỗi hiển thị ảnh");
                }
                
                dotsPanel.repaint();
            });
        }
    }
}

