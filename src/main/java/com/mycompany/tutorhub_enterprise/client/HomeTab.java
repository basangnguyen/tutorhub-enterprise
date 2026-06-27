package com.mycompany.tutorhub_enterprise.client;

import com.mycompany.tutorhub_enterprise.client.home.HomeSocialWebPanel;
import com.mycompany.tutorhub_enterprise.client.home.HomeBannerItem;
import com.mycompany.tutorhub_enterprise.client.home.HomeLocketItem;
import com.mycompany.tutorhub_enterprise.client.home.HomeSocialState;
import com.mycompany.tutorhub_enterprise.models.Packet;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.net.URI;
import java.net.URLEncoder;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HomeTab extends JPanel {

    public static class ClassModel {
        public String id, subj, sal, addr, time, req, tagText, tagColor;
        public boolean isSaved = false;
        public boolean isTaken = false;

        public ClassModel(String id, String subj, String sal, String addr, String time, String req, String tagText, String tagColor) {
            this.id = id; this.subj = subj; this.sal = sal; this.addr = addr; 
            this.time = time; this.req = req; this.tagText = tagText; this.tagColor = tagColor;
        }
    }

    private final List<ClassModel> classModels = new ArrayList<>();
    private static final java.util.Map<String, ImageIcon> iconCache = new java.util.HashMap<>();
    
    // --- UI COMPONENTS ---
    private JPanel classGridPanel;
    private JScrollPane mainScrollPane; 
    private JPanel emptyStatePanel;
    private JPanel activeTagsPanel;
    private JPanel filterArea; 

    // --- BẢNG MÀU MỚI (SAAS MINIMAL THEME) ---
    private final Color UI_BG = Color.decode("#F8FAFC");        
    private final Color CARD_BG = Color.WHITE;
    private final Color BORDER = Color.decode("#E2E8F0");       
    private final Color PRIMARY = Color.decode("#3B82F6");      
    private final Color PRIMARY_DARK = Color.decode("#2563EB"); 
    private final Color TEXT = Color.decode("#1E293B");         
    private final Color MUTED = Color.decode("#64748B");        
    private final Color SUCCESS = Color.decode("#10B981");

    // --- QUẢN LÝ TRẠNG THÁI ---
    private String filterSubject = null, filterLocation = null, filterSort = "Mới nhất";
    private String filterSalary = null, filterSchedule = null, filterStatus = null;
    private String quickFilter = "ALL"; 
    
    private boolean isGridView = true; 

    private PillButton btnAll, btnNear, btnHighSalary, btnTakenQuick, btnAdvancedFilter, btnSort;
    private PillButton btnViewToggle;
    private HomeSocialWebPanel homeSocialWebPanel;
    private final java.util.List<HomeLocketItem> currentLocketItems = new ArrayList<>();
    private Runnable openMessagesHandler;

    public HomeTab() {
        setLayout(new BorderLayout(0, 0));
        setBackground(UI_BG);
        setBorder(null);

        initEmptyState();
        requestLocketPosts(); // Load global feed

        JPanel mainContent = new JPanel(new BorderLayout(0, 0)); 
        mainContent.setOpaque(false);

        // --- 1. HOME SOCIAL WEB PROTOTYPE: Banner + global Locket feed ---
        homeSocialWebPanel = new HomeSocialWebPanel();
        java.util.List<HomeLocketItem> defaultLocketItems = createDefaultHomeLocketItems();
        currentLocketItems.clear();
        currentLocketItems.addAll(defaultLocketItems);
        homeSocialWebPanel.setHomeSocialState(new HomeSocialState(
                createDefaultHomeBanners(),
                defaultLocketItems,
                true
        ));
        
        homeSocialWebPanel.setEventListener((type, payload) -> {
            try {
                if ("LOCKET_POST_REACT".equals(type)) {
                    com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(payload).getAsJsonObject();
                    Long postId = parseLocketPostId(json);
                    if (postId == null) {
                        System.out.println("[HOME_TAB] Skip backend reaction for local/sample Locket post: " + payload);
                        return;
                    }
                    
                    com.google.gson.JsonObject reqPayload = new com.google.gson.JsonObject();
                    reqPayload.addProperty("postId", postId);
                    reqPayload.addProperty("reactionType", "HEART");
                    
                    com.mycompany.tutorhub_enterprise.models.Packet request = new com.mycompany.tutorhub_enterprise.models.Packet(
                        "LOCKET_POST_REACT", reqPayload.toString()
                    );
                    NetworkManager.getInstance().sendPacket(request);
                } else if ("LOCKET_CREATE_OPEN".equals(type)) {
                    openLocketPopup(0);
                } else if ("LOCKET_VIEW_OPEN".equals(type)) {
                    openLocketPopup(resolveLocketIndex(payload));
                } else if ("LOCKET_COMMENT_OPEN".equals(type)) {
                    // Mở danh sách bình luận
                    com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(payload).getAsJsonObject();
                    Long postId = parseLocketPostId(json);
                    if (postId != null) {
                        com.google.gson.JsonObject reqPayload = new com.google.gson.JsonObject();
                        reqPayload.addProperty("postId", postId);
                        reqPayload.addProperty("limit", 20);
                        com.mycompany.tutorhub_enterprise.models.Packet request = new com.mycompany.tutorhub_enterprise.models.Packet(
                            "LOCKET_COMMENT_LIST", reqPayload.toString()
                        );
                        NetworkManager.getInstance().sendPacket(request);
                    } else {
                        // Trả về mảng rỗng cho bài đăng mẫu không tồn tại trong CSDL
                        if (homeSocialWebPanel != null) {
                            homeSocialWebPanel.updateLocketComments("[]");
                        }
                    }
                } else if ("LOCKET_COMMENT_CREATE".equals(type)) {
                    com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(payload).getAsJsonObject();
                    Long postId = parseLocketPostId(json);
                    if (postId != null && json.has("content")) {
                        com.google.gson.JsonObject reqPayload = new com.google.gson.JsonObject();
                        reqPayload.addProperty("postId", postId);
                        reqPayload.addProperty("content", json.get("content").getAsString());
                        com.mycompany.tutorhub_enterprise.models.Packet request = new com.mycompany.tutorhub_enterprise.models.Packet(
                            "LOCKET_COMMENT_CREATE", reqPayload.toString()
                        );
                        NetworkManager.getInstance().sendPacket(request);
                    } else if (json.has("content")) {
                        // Fake comment creation cho bài đăng mẫu
                        com.google.gson.JsonObject fakeComment = new com.google.gson.JsonObject();
                        fakeComment.addProperty("id", System.currentTimeMillis());
                        fakeComment.addProperty("postId", json.has("id") ? json.get("id").getAsString() : "");
                        fakeComment.addProperty("authorName", "Bạn (Chế độ xem thử)");
                        fakeComment.addProperty("content", json.get("content").getAsString());
                        fakeComment.addProperty("timeAgo", "Vừa xong");
                        if (homeSocialWebPanel != null) {
                            homeSocialWebPanel.addLocketComment(fakeComment.toString());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // --- 3. TIÊU ĐỀ LỚP HỌC & BỘ LỌC ---
        filterArea = new JPanel();
        filterArea.setLayout(new BoxLayout(filterArea, BoxLayout.Y_AXIS));
        filterArea.setOpaque(false);
        filterArea.setBorder(new EmptyBorder(20, 24, 12, 24)); 

        JPanel classHeaderPanel = new JPanel(new BorderLayout());
        classHeaderPanel.setOpaque(false);
        classHeaderPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titlePanel.setOpaque(false);
        
        JLabel lblBefore = new JLabel();
        lblBefore.setIcon(new com.formdev.flatlaf.extras.FlatSVGIcon("images/icon/scenario-screenplay-script-svgrepo-com.svg", 26, 26));
        
        JLabel classTitle = new JLabel("Lớp học nổi bật");
        classTitle.setFont(new Font("Segoe UI", Font.BOLD, 22)); 
        classTitle.setForeground(Color.decode("#e11d48"));
        
        JLabel lblAfter = new JLabel();
        lblAfter.setIcon(new com.formdev.flatlaf.extras.FlatSVGIcon("images/icon/fire-like-trend-svgrepo-com (1).svg", 24, 24));
        
        titlePanel.add(lblBefore);
        titlePanel.add(classTitle);
        titlePanel.add(lblAfter);
        
        JLabel seeAllClasses = new JLabel("Xem tất cả");
        seeAllClasses.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        seeAllClasses.setForeground(PRIMARY);
        seeAllClasses.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // --- HIỆU ỨNG SMOOTH SCROLL KHI CLICK ---
        seeAllClasses.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int targetY = filterArea.getY();
                smoothScrollTo(targetY);
            }
        });
        
        classHeaderPanel.add(titlePanel, BorderLayout.WEST);
        classHeaderPanel.add(seeAllClasses, BorderLayout.EAST);
        
        filterArea.add(classHeaderPanel);
        filterArea.add(Box.createVerticalStrut(12)); 
        filterArea.add(createModernFilterBar());

        activeTagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        activeTagsPanel.setOpaque(false);
        activeTagsPanel.setVisible(false);
        filterArea.add(Box.createVerticalStrut(8));
        filterArea.add(activeTagsPanel);

        // --- 4. THẺ LỚP HỌC ---
        classGridPanel = new JPanel();
        classGridPanel.setOpaque(false);
        classGridPanel.setBorder(new EmptyBorder(0, 24, 40, 24)); 

        JPanel topElementsWrapper = new JPanel();
        topElementsWrapper.setLayout(new BoxLayout(topElementsWrapper, BoxLayout.Y_AXIS));
        topElementsWrapper.setOpaque(false);
        topElementsWrapper.add(homeSocialWebPanel);
        topElementsWrapper.add(filterArea);

        WebScrollPanel scrollContent = new WebScrollPanel(new BorderLayout());
        scrollContent.setOpaque(false);
        scrollContent.add(topElementsWrapper, BorderLayout.NORTH); 
        scrollContent.add(classGridPanel, BorderLayout.CENTER);    

        mainScrollPane = new JScrollPane(scrollContent);
        mainScrollPane.setBorder(null);
        mainScrollPane.setOpaque(false);
        mainScrollPane.getViewport().setOpaque(false);
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(24); 
        mainScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0)); 
        mainScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        mainContent.add(mainScrollPane, BorderLayout.CENTER);
        add(mainContent, BorderLayout.CENTER);
        updateUIState();
    }

    public void setOpenMessagesHandler(Runnable handler) {
        this.openMessagesHandler = handler;
    }

    private void openMessagesFromLocket() {
        if (openMessagesHandler != null) {
            openMessagesHandler.run();
        } else {
            System.out.println("[HOME_TAB] Locket message requested, but no dashboard callback is bound.");
        }
    }

    private void openLocketPopup(int startIndex) {
        SwingUtilities.invokeLater(() -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            Frame frame = owner instanceof Frame ? (Frame) owner : null;
            com.mycompany.tutorhub_enterprise.client.home.LocketWebPopupDialog dialog =
                    new com.mycompany.tutorhub_enterprise.client.home.LocketWebPopupDialog(
                            frame,
                            new ArrayList<>(currentLocketItems),
                            Math.max(0, startIndex),
                            this::refreshLocketPosts,
                            this::openMessagesFromLocket
                    );
            dialog.setVisible(true);
        });
    }

    private int resolveLocketIndex(String payload) {
        try {
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(payload == null ? "{}" : payload).getAsJsonObject();
            if (json.has("id")) {
                String id = json.get("id").getAsString();
                for (int i = 0; i < currentLocketItems.size(); i++) {
                    if (id.equals(currentLocketItems.get(i).id)) {
                        return i;
                    }
                }
            }
            if (json.has("index")) {
                return json.get("index").getAsInt();
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private Long parseLocketPostId(com.google.gson.JsonObject json) {
        try {
            if (json == null) {
                return null;
            }
            if (json.has("postId")) {
                return json.get("postId").getAsLong();
            }
            if (json.has("id")) {
                return Long.parseLong(json.get("id").getAsString());
            }
        } catch (Exception ignored) {
        }
        return null;
    }
    
    // =========================================================
    // HÀM TẠO HIỆU ỨNG SMOOTH SCROLL (WEB-LIKE)
    // =========================================================
    private void smoothScrollTo(int targetY) {
        JViewport viewport = mainScrollPane.getViewport();
        Point currentPos = viewport.getViewPosition();
        int startY = currentPos.y;
        int distance = targetY - startY;
        
        if (distance == 0) return;

        int steps = 40; 
        int delay = 10; 

        Timer timer = new Timer(delay, null);
        timer.addActionListener(new ActionListener() {
            int currentStep = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                currentStep++;
                float t = (float) currentStep / steps;
                
                float easedT = t < 0.5f ? 4 * t * t * t : 1 - (float)Math.pow(-2 * t + 2, 3) / 2;
                int newY = startY + (int)(distance * easedT);

                int maxY = viewport.getView().getHeight() - viewport.getHeight();
                if (maxY < 0) maxY = 0;
                if (newY > maxY) newY = maxY;
                if (newY < 0) newY = 0;

                viewport.setViewPosition(new Point(currentPos.x, newY));

                if (currentStep >= steps) {
                    timer.stop();
                }
            }
        });
        timer.start();
    }

    public ClassModel getClassModelById(String id) {
        for (ClassModel m : classModels) {
            if (m.id.equals(id)) return m;
        }
        return null;
    }

    public void loadReelsToVideoSection(java.util.List<String> data) {
        java.util.List<HomeLocketItem> mappedItems = mapLegacyLocketItems(data);
        SwingUtilities.invokeLater(() -> {
            currentLocketItems.clear();
            currentLocketItems.addAll(mappedItems);
            if (homeSocialWebPanel != null) {
                homeSocialWebPanel.notifyLegacyLocketDataAvailable(data == null ? 0 : data.size());
                homeSocialWebPanel.setLocketItems(mappedItems);
            }
        });
    }

    // --- PHASE 4A/4B: BACKEND INTEGRATION ---
    public void handleCreateSuccess() {
        SwingUtilities.invokeLater(() -> {
            com.mycompany.tutorhub_enterprise.client.home.LocketWebPopupDialog.closeActiveInstance();
            refreshLocketPosts();
        });
    }

    public void handleCreateError(String errorMsg) {
        com.mycompany.tutorhub_enterprise.client.home.LocketWebPopupDialog.handleUploadError(errorMsg);
    }

    public void setCurrentClassContext(Integer classId, String className) {
        // Locket is a global photo feed now; live classroom context is intentionally ignored here.
    }

    public void refreshLocketPosts() {
        requestLocketPosts();
    }

    public void requestLocketPosts() {
        try {
            com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
            payload.addProperty("limit", 20);
            payload.addProperty("cursor", 0);
            
            com.mycompany.tutorhub_enterprise.models.Packet request = new com.mycompany.tutorhub_enterprise.models.Packet(
                "LOCKET_POST_LIST", payload.toString()
            );
            NetworkManager.getInstance().sendPacket(request);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String presignIfB2Url(String url) {
        if (url == null || url.trim().isEmpty()) return url;
        if (url.startsWith("data:image/")) return url;
        
        // Handle absolute local files by converting to Base64
        if (url.startsWith("file://") || url.startsWith("/") || url.matches("^[A-Za-z]:[\\\\/].*")) {
            String path = url.replace("file://", "");
            if (path.startsWith("/") && path.matches("^/[A-Za-z]:.*")) {
                path = path.substring(1);
            }
            try {
                java.io.File f = new java.io.File(path);
                if (f.exists() && f.isFile()) {
                    byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
                    String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
                    String mimeType = "image/png";
                    String lowerPath = path.toLowerCase();
                    if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) mimeType = "image/jpeg";
                    else if (lowerPath.endsWith(".gif")) mimeType = "image/gif";
                    else if (lowerPath.endsWith(".webp")) mimeType = "image/webp";
                    return "data:" + mimeType + ";base64," + base64;
                }
            } catch (Exception ex) {
                System.err.println("[LOCKET_IMAGE] Error converting local file to base64: " + ex.getMessage());
            }
        }
        
        if (!url.startsWith("http")) return url;
        if (!url.contains("backblazeb2.com")) return url;
        if (url.contains("X-Amz-Signature") || url.contains("x-amz-signature") || url.contains("Signature=")) return url;
        try {
            return com.mycompany.tutorhub_enterprise.utils.B2Helper.getPresignedUrl(url);
        } catch (Exception e) {
            String safeUrl = url.length() > 50 ? url.substring(0, 50) + "..." : url;
            System.err.println("[LOCKET_IMAGE][PRESIGN_ERROR] " + safeUrl + " -> " + e.getMessage());
            return url;
        }
    }

    public void handleLocketPostListSuccess(java.util.List<HomeLocketItem> items) {
        if (items != null) {
            for (HomeLocketItem item : items) {
                if (item.authorAvatar == null || item.authorAvatar.isEmpty() || "null".equals(item.authorAvatar)) {
                    item.authorInitials = getInitials(item.authorName);
                }
                item.imageUrl = presignIfB2Url(item.imageUrl);
                item.thumbnailUrl = presignIfB2Url(item.thumbnailUrl);
            }
        }
        SwingUtilities.invokeLater(() -> {
            currentLocketItems.clear();
            if (items != null) {
                currentLocketItems.addAll(items);
            }
            if (homeSocialWebPanel != null) {
                homeSocialWebPanel.setLocketItems(items);
            }
        });
    }

    public void handleLocketReactionSuccess(long postId, boolean reacted) {
        SwingUtilities.invokeLater(() -> {
            if (homeSocialWebPanel != null) {
                homeSocialWebPanel.updateLocketReaction(postId, reacted);
            }
        });
    }

    public void handleLocketCommentListSuccess(String payload) {
        SwingUtilities.invokeLater(() -> {
            if (homeSocialWebPanel != null) {
                homeSocialWebPanel.updateLocketComments(payload);
            }
        });
    }

    public void handleLocketCommentCreateSuccess(String payload) {
        SwingUtilities.invokeLater(() -> {
            if (homeSocialWebPanel != null) {
                homeSocialWebPanel.addLocketComment(payload);
            }
        });
    }

    public void handleLocketCommentDeleteSuccess(long commentId) {
        SwingUtilities.invokeLater(() -> {
            if (homeSocialWebPanel != null) {
                homeSocialWebPanel.deleteLocketComment(commentId);
            }
        });
    }
    
    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        } else {
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        }
    }

    private java.util.List<HomeBannerItem> createDefaultHomeBanners() {
        java.util.List<HomeBannerItem> banners = new ArrayList<>();
        banners.add(new HomeBannerItem(
                "home-banner-1",
                "../images/slide1.png",
                "Explore Dream Platform",
                "Bang tin lop, lich hoc va khoanh khac hoc tap trong mot khong gian gon gang.",
                "Kham pha ngay"
        ));
        banners.add(new HomeBannerItem(
                "home-banner-2",
                "../images/slide2.png",
                "Lop hoc thong minh hon",
                "Theo doi lop dang dien ra, lich sap toi va noi dung noi bat cua lop.",
                "Mo lop hoc"
        ));
        banners.add(new HomeBannerItem(
                "home-banner-3",
                "../images/slide3.png",
                "Giu lai buoi hoc dang nho",
                "Locket giup luu lai khoanh khac hoc tap bang anh, cam xuc va tin nhan.",
                "Xem Locket"
        ));
        return banners;
    }

    private java.util.List<HomeLocketItem> createDefaultHomeLocketItems() {
        java.util.List<HomeLocketItem> items = new ArrayList<>();
        items.add(new HomeLocketItem("sample-locket-1", "../images/general/general1.png", "../images/general/general1.png", "Buoi hoc hom nay that hieu qua", "Nguyen Ngoc Le Vy", "", "LV", "2 gio truoc", 128, 24, true, false));
        items.add(new HomeLocketItem("sample-locket-2", "../images/english/english1.jpg", "../images/english/english1.jpg", "Hoc tieng Anh moi ngay", "Minh Anh", "", "MA", "1 ngay truoc", 96, 18, false, false));
        items.add(new HomeLocketItem("sample-locket-3", "../images/chemistry/chemistry1.jpg", "../images/chemistry/chemistry1.jpg", "Hoa hoc that thu vi", "Gia Han", "", "GH", "2 ngay truoc", 104, 20, true, false));
        items.add(new HomeLocketItem("sample-locket-4", "../images/math/math1.jpg", "../images/math/math1.jpg", "Giai bai tap toan nang cao", "Quang Huy", "", "QH", "3 ngay truoc", 88, 16, false, false));
        items.add(new HomeLocketItem("sample-locket-5", "../images/IELTS/IELTS1.jpg", "../images/IELTS/IELTS1.jpg", "Co gang tung ngay de dat muc tieu", "Bao Tran", "", "BT", "4 ngay truoc", 112, 22, false, false));
        return items;
    }

    private java.util.List<HomeLocketItem> mapLegacyLocketItems(java.util.List<String> data) {
        java.util.List<HomeLocketItem> items = new ArrayList<>();
        if (data == null) {
            return items;
        }
        for (int i = 0; i < data.size(); i++) {
            String raw = data.get(i);
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            String[] parts = raw.split(";;", -1);
            String id = safePart(parts, 0, "legacy-locket-" + (i + 1));
            String imageUrl = normalizeHomeSocialImageUrl(safePart(parts, 1, ""), i);
            String caption = safePart(parts, 2, "");
            String authorName = safePart(parts, 4, "TutorHub");
            String authorAvatar = toDataImageUrl(safePart(parts, 5, ""));
            String initials = initialsFromName(authorName);
            
            // Presign if necessary
            imageUrl = presignIfB2Url(imageUrl);
            
            items.add(new HomeLocketItem(
                    id,
                    imageUrl,
                    imageUrl,
                    caption,
                    authorName,
                    authorAvatar,
                    initials,
                    "Vua xong",
                    0,
                    0,
                    false,
                    false
            ));
        }
        return items;
    }

    private String safePart(String[] parts, int index, String fallback) {
        if (parts == null || index < 0 || index >= parts.length) {
            return fallback;
        }
        String value = safeTrim(parts[index]);
        return value.isEmpty() ? fallback : value;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeHomeSocialImageUrl(String value, int index) {
        String imageUrl = safeTrim(value);
        if (imageUrl.isEmpty()) {
            return fallbackHomeSocialImage(index);
        }
        String lower = imageUrl.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")
                || lower.startsWith("data:") || lower.startsWith("file:")
                || lower.startsWith("jar:")) {
            return imageUrl;
        }
        if (imageUrl.startsWith("/")) {
            return ".." + imageUrl;
        }
        if (lower.startsWith("images/")) {
            return "../" + imageUrl;
        }
        return imageUrl;
    }

    private String fallbackHomeSocialImage(int index) {
        String[] localImages = {
            "../images/general/general1.png",
            "../images/english/english1.jpg",
            "../images/chemistry/chemistry1.jpg",
            "../images/math/math1.jpg",
            "../images/IELTS/IELTS1.jpg"
        };
        return localImages[Math.abs(index) % localImages.length];
    }

    private String toDataImageUrl(String avatarBase64) {
        String value = safeTrim(avatarBase64);
        if (value.isEmpty()) {
            return "";
        }
        if (value.startsWith("data:") || value.startsWith("http") || value.startsWith("/") || value.startsWith("../")) {
            return value;
        }
        return "data:image/png;base64," + value;
    }

    private String initialsFromName(String name) {
        String value = safeTrim(name);
        if (value.isEmpty()) {
            return "TH";
        }
        String[] parts = value.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty() && out.length() < 2) {
                out.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return out.length() == 0 ? "TH" : out.toString();
    }

    private JPanel createModernFilterBar() {
        JPanel wrapper = new JPanel(new BorderLayout()); wrapper.setOpaque(false); wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); left.setOpaque(false);
        btnAll = createQuickPill("Tất cả", "ALL"); 
        btnNear = createQuickPill("Gần bạn", "NEAR"); 
        btnHighSalary = createQuickPill("Lương cao", "HIGH_SALARY"); 
        btnTakenQuick = createQuickPill("Đã nhận", "TAKEN");
        left.add(btnAll); left.add(btnNear); left.add(btnHighSalary); left.add(btnTakenQuick);
        
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); right.setOpaque(false);
        
        btnViewToggle = createActionBadge("", "https://img.icons8.com/fluency-systems-regular/48/4B5563/list.png");
        btnViewToggle.setPreferredSize(new Dimension(40, 36));
        btnViewToggle.addActionListener(e -> {
            isGridView = !isGridView;
            setNetworkIcon(btnViewToggle, isGridView ? "https://img.icons8.com/fluency-systems-regular/48/4B5563/list.png" : "https://img.icons8.com/fluency-systems-regular/48/4B5563/grid.png", 18, 18);
            applyFilterAndSort(); 
        });

        btnAdvancedFilter = createActionBadge("Lọc", "https://img.icons8.com/fluency-systems-regular/30/4B5563/filter-and-sort.png"); 
        btnAdvancedFilter.setPreferredSize(new Dimension(90, 36));
        btnAdvancedFilter.addActionListener(e -> openAdvancedFilterModal());
        
        btnSort = createDropdownBadge("Sắp xếp ▼"); 
        btnSort.setPreferredSize(new Dimension(110, 36)); 
        setupDropdownMenu(btnSort, new String[]{"Mới nhất", "Lương cao đến thấp", "Lương thấp đến cao", "Lớp HOT"}, "SORT");
        
        right.add(btnViewToggle); right.add(new JLabel("  ")); 
        right.add(btnAdvancedFilter); right.add(btnSort);
        
        wrapper.add(left, BorderLayout.WEST); wrapper.add(right, BorderLayout.EAST); return wrapper;
    }

    private PillButton createQuickPill(String text, String filterKey) {
        PillButton btn = new PillButton(text); 
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13)); 
        int calculatedWidth = Math.max(85, text.length() * 10 + 20); 
        btn.setPreferredSize(new Dimension(calculatedWidth, 34)); 
        btn.addActionListener(e -> { quickFilter = filterKey; updateUIState(); }); return btn;
    }

    private PillButton createDropdownBadge(String text) { 
        PillButton btn = new PillButton(text); btn.setFont(new Font("Segoe UI", Font.PLAIN, 13)); 
        btn.setBackground(Color.WHITE); btn.setForeground(TEXT); btn.setBorderColor(BORDER); 
        return btn; 
    }
    
    private PillButton createActionBadge(String text, String iconUrl) { 
        PillButton btn = new PillButton(" " + text); setNetworkIcon(btn, iconUrl, 16, 16); 
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13)); btn.setBackground(Color.WHITE); 
        btn.setForeground(TEXT); btn.setBorderColor(BORDER); 
        return btn; 
    }

    private String getSubjectImagePath(String subj) {
        if (subj == null || subj.trim().isEmpty()) return "/images/general/general1.png";
        String s = subj.toLowerCase(); Random rand = new Random(); int index = rand.nextInt(6) + 1;
        if (s.contains("ielts")) return "/images/IELTS/IELTS" + index + ".jpg";
        if (s.contains("anh") || s.contains("toeic") || s.contains("toefl")) return "/images/english/english" + index + ".jpg";
        if (s.contains("toán") || s.contains("đại số") || s.contains("hình học") || s.contains("giải tích")) return "/images/math/math" + index + ".jpg";
        if (s.contains("lý") || s.contains("vật lý") || s.contains("cơ học")) return "/images/physics/physics" + index + ".jpg";
        if (s.contains("hóa")) return "/images/chemistry/chemistry" + index + ".jpg";
        if (s.contains("văn") || s.contains("ngữ văn") || s.contains("tiếng việt")) return "/images/literature/literature" + index + ".jpg";
        if (s.contains("tin") || s.contains("lập trình") || s.contains("java") || s.contains("python") || s.contains("it")) return "/images/it/it" + index + ".jpg";
        return "/images/general/general1.png";
    }

    private JPanel createCompactGridCard(ClassModel m) {
        JPanel card = new JPanel(new BorderLayout()) {
            public boolean isHover = false;
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = 16; 
                int shadowGap = 10; 
                int w = getWidth();
                int h = getHeight() - shadowGap;

                if (isHover) {
                    g2.setColor(new Color(0, 0, 0, 4)); 
                    g2.fillRoundRect(2, 6, w - 4, h, arc, arc);
                    g2.setColor(new Color(0, 0, 0, 6)); 
                    g2.fillRoundRect(1, 3, w - 2, h, arc, arc);
                    g2.translate(0, -3); 
                }
                
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                
                g2.setColor(isHover ? PRIMARY : BORDER);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

                Shape oldClip = g2.getClip();
                g2.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, w, h, arc, arc));
                super.paint(g2);
                g2.setClip(oldClip);
                g2.dispose();
            }
        }; 
        card.setOpaque(false); 
        card.setBorder(new EmptyBorder(0, 0, 10, 0)); 

        ImageHeaderPanel imgHeader = new ImageHeaderPanel(getSubjectImagePath(m.subj), Color.decode("#1F2937"), Color.decode("#475569"));
        // ĐÃ CHỈNH SỬA: Giảm chiều cao ảnh thumbnail cho thẻ hẹp
        imgHeader.setPreferredSize(new Dimension(0, 110)); 
        imgHeader.setLayout(new BorderLayout());
        
        JPanel tagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8)); tagPanel.setOpaque(false);
        tagPanel.add(createTagBadge(m.tagText, m.tagColor, "#FFFFFF")); 
        tagPanel.add(createTagBadge(m.isTaken ? "Đã chốt" : "Còn lớp", m.isTaken ? "#FEE2E2" : "#D1FAE5", m.isTaken ? "#DC2626" : "#059669"));
        imgHeader.add(tagPanel, BorderLayout.NORTH);

        JPanel body = new JPanel(); body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS)); body.setOpaque(false); 
        // ĐÃ CHỈNH SỬA: Giảm viền trong body
        body.setBorder(new EmptyBorder(12, 12, 6, 12)); 
        JLabel titleLbl = new JLabel("<html><div style='width:100%; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;'>" + m.subj + "</div></html>");
        // ĐÃ CHỈNH SỬA: Kích thước tiêu đề nhỏ lại 14px
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14)); titleLbl.setForeground(TEXT);
        body.add(titleLbl); body.add(Box.createVerticalStrut(10));
        
        // ĐÃ CHỈNH SỬA: Rút ngắn độ dài chữ (truncate) và giảm size font
        body.add(createIconTextRow("https://img.icons8.com/fluency-systems-regular/48/64748b/marker.png", truncate(m.addr, 18), "#64748b", 11, false)); body.add(Box.createVerticalStrut(6)); 
        body.add(createIconTextRow("https://img.icons8.com/fluency-systems-regular/48/10b981/money-bag.png", m.sal, "#10b981", 12, true)); body.add(Box.createVerticalStrut(6)); 
        body.add(createIconTextRow("https://img.icons8.com/fluency-systems-regular/48/64748b/clock--v1.png", truncate(m.time, 20), "#64748b", 11, false)); body.add(Box.createVerticalStrut(6)); 
        body.add(createIconTextRow("https://img.icons8.com/fluency-systems-regular/48/64748b/note.png", truncate(m.req, 20), "#94a3b8", 11, false));

        JPanel footer = new JPanel(new BorderLayout(8, 0)); footer.setOpaque(false); 
        footer.setBorder(new EmptyBorder(4, 12, 14, 12)); 

        RoundedButton btnHeart = new RoundedButton("", 8); setNetworkIcon(btnHeart, m.isSaved ? "https://img.icons8.com/fluency/48/like.png" : "https://img.icons8.com/fluency-systems-regular/48/94a3b8/like--v1.png", 16, 16); 
        btnHeart.setBackground(Color.WHITE); btnHeart.setBorderColor(m.isSaved ? Color.decode("#FCA5A5") : BORDER); btnHeart.setPreferredSize(new Dimension(32, 32));
        btnHeart.addActionListener(e -> { m.isSaved = !m.isSaved; applyFilterAndSort(); });

        RoundedButton btnAccept = new RoundedButton(m.isTaken ? "Đã nhận" : "Nhận lớp", 8); 
        btnAccept.setBackground(m.isTaken ? Color.decode("#F1F5F9") : PRIMARY); btnAccept.setForeground(m.isTaken ? MUTED : Color.WHITE); btnAccept.setBorderColor(m.isTaken ? Color.decode("#F1F5F9") : PRIMARY); 
        btnAccept.setFont(new Font("Segoe UI", Font.BOLD, 12)); btnAccept.setPreferredSize(new Dimension(0, 32));
        if(!m.isTaken) btnAccept.addActionListener(e -> showClassDetailModal(m.id, m.subj, m.sal, m.addr, m.time, m.req, m.tagText, m.tagColor, m));
        
        footer.add(btnHeart, BorderLayout.WEST); footer.add(btnAccept, BorderLayout.CENTER); 
        card.add(imgHeader, BorderLayout.NORTH); card.add(body, BorderLayout.CENTER); card.add(footer, BorderLayout.SOUTH);
        HoverEffect(card); return card;
    }

    private JPanel createListCard(ClassModel m) {
        JPanel card = new JPanel(new BorderLayout(16, 0)) {
            public boolean isHover = false;
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = 16; 
                int shadowGap = 10;
                int w = getWidth();
                int h = getHeight() - shadowGap;

                if (isHover) {
                    g2.setColor(new Color(0, 0, 0, 4)); 
                    g2.fillRoundRect(2, 6, w - 4, h, arc, arc);
                    g2.setColor(new Color(0, 0, 0, 6)); 
                    g2.fillRoundRect(1, 3, w - 2, h, arc, arc);
                    g2.translate(0, -3);
                }
                
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                
                g2.setColor(isHover ? PRIMARY : BORDER);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

                Shape oldClip = g2.getClip();
                g2.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, w, h, arc, arc));
                super.paint(g2);
                g2.setClip(oldClip);
                g2.dispose();
            }
        }; 
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 16, 12 + 10, 16)); 
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115)); 

        ImageHeaderPanel thumb = new ImageHeaderPanel(getSubjectImagePath(m.subj), Color.decode("#1E293B"), Color.decode("#475569"));
        thumb.setPreferredSize(new Dimension(110, 0)); 
        card.add(thumb, BorderLayout.WEST);

        JPanel centerCol = new JPanel(new GridLayout(2, 1, 0, 6)); centerCol.setOpaque(false);
        
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); titleRow.setOpaque(false);
        JLabel titleLbl = new JLabel(m.subj); titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 15)); titleLbl.setForeground(TEXT);
        titleRow.add(titleLbl);
        titleRow.add(createTagBadge(m.tagText, m.tagColor, "#FFFFFF"));
        titleRow.add(createTagBadge(m.isTaken ? "Đã chốt" : "Còn lớp", m.isTaken ? "#FEE2E2" : "#DC2626", m.isTaken ? "#DC2626" : "#059669"));
        
        JPanel infoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0)); infoRow.setOpaque(false);
        infoRow.add(createIconTextRow("https://img.icons8.com/fluency-systems-regular/48/64748b/marker.png", truncate(m.addr, 20), "#64748b", 12, false));
        infoRow.add(createIconTextRow("https://img.icons8.com/fluency-systems-regular/48/64748b/clock--v1.png", truncate(m.time, 25), "#64748b", 12, false));
        infoRow.add(createIconTextRow("https://img.icons8.com/fluency-systems-regular/48/64748b/note.png", truncate(m.req, 30), "#94a3b8", 12, false));
        
        centerCol.add(titleRow); centerCol.add(infoRow); card.add(centerCol, BorderLayout.CENTER);

        JPanel rightCol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 18)); rightCol.setOpaque(false);
        JLabel salLbl = new JLabel(m.sal); salLbl.setFont(new Font("Segoe UI", Font.BOLD, 15)); salLbl.setForeground(SUCCESS);
        RoundedButton btnHeart = new RoundedButton("", 8); setNetworkIcon(btnHeart, m.isSaved ? "https://img.icons8.com/fluency/48/like.png" : "https://img.icons8.com/fluency-systems-regular/48/94a3b8/like--v1.png", 18, 18); 
        btnHeart.setBackground(Color.WHITE); btnHeart.setBorderColor(m.isSaved ? Color.decode("#FCA5A5") : BORDER); btnHeart.setPreferredSize(new Dimension(34, 34));
        btnHeart.addActionListener(e -> { m.isSaved = !m.isSaved; applyFilterAndSort(); });

        RoundedButton btnAccept = new RoundedButton(m.isTaken ? "Đã nhận" : "Nhận lớp", 8); 
        btnAccept.setBackground(m.isTaken ? Color.decode("#F1F5F9") : PRIMARY); btnAccept.setForeground(m.isTaken ? MUTED : Color.WHITE); btnAccept.setBorderColor(m.isTaken ? Color.decode("#F1F5F9") : PRIMARY); 
        btnAccept.setFont(new Font("Segoe UI", Font.BOLD, 12)); btnAccept.setPreferredSize(new Dimension(100, 34));
        if(!m.isTaken) btnAccept.addActionListener(e -> showClassDetailModal(m.id, m.subj, m.sal, m.addr, m.time, m.req, m.tagText, m.tagColor, m));

        rightCol.add(salLbl); rightCol.add(new JLabel("   ")); rightCol.add(btnHeart); rightCol.add(btnAccept);
        card.add(rightCol, BorderLayout.EAST);
        HoverEffect(card); return card;
    }

    private void HoverEffect(JPanel card) {
        card.addMouseListener(new MouseAdapter() { 
            @Override public void mouseEntered(MouseEvent e) { 
                try { card.getClass().getField("isHover").set(card, true); } catch(Exception ex){}
                card.setCursor(new Cursor(Cursor.HAND_CURSOR)); 
                card.repaint(); 
            } 
            @Override public void mouseExited(MouseEvent e) { 
                try { card.getClass().getField("isHover").set(card, false); } catch(Exception ex){}
                card.repaint();
            } 
        });
    }

    private String truncate(String text, int limit) {
        if (text == null) return "";
        if (text.length() <= limit) return text;
        return text.substring(0, limit) + "...";
    }

    public void displayNewClass(String id, String subj, String sal, String addr, String time, String req, String tagText, String tagColor) { 
        SwingUtilities.invokeLater(() -> { 
            classModels.add(0, new ClassModel(id, subj, sal, addr, time, req, tagText, tagColor));
            this.quickFilter = "ALL"; 
            updateUIState(); 
            if (mainScrollPane.getViewport() != null) mainScrollPane.getViewport().setViewPosition(new Point(0, 0));
        }); 
    }

    public void markClassAsTaken(String id) {
        SwingUtilities.invokeLater(() -> {
            for (ClassModel m : classModels) if (m.id.equals(id)) { m.isTaken = true; break; }
            updateUIState();
        });
    }

    public void resetClassButton(String id) {
        SwingUtilities.invokeLater(() -> {
            for (ClassModel m : classModels) if (m.id.equals(id)) { m.isTaken = false; break; }
            updateUIState();
        });
    }

    public void appendNotification(String iconUrl, String title, String desc) {}
    public void updateTodoWidget(String payload) {}

    private void applyFilterAndSort() {
        classGridPanel.removeAll(); 
        List<ClassModel> displayList = new ArrayList<>();
        
        for (ClassModel m : classModels) {
            String loc = m.addr.toLowerCase(); String subj = m.subj.toLowerCase(); String time = m.time.toLowerCase(); 
            double sal = parseSalary(m.sal); String tagDesc = m.tagText.toLowerCase();
            
            boolean matchQuick = true;
            if ("HIGH_SALARY".equals(quickFilter)) matchQuick = sal >= 200000; 
            else if ("SAVED".equals(quickFilter)) matchQuick = m.isSaved; 
            else if ("TAKEN".equals(quickFilter)) matchQuick = m.isTaken; 
            else if ("NEAR".equals(quickFilter)) matchQuick = loc.contains("hà đông") || loc.contains("thanh xuân") || loc.contains("cầu giấy") || loc.contains("đống đa") || loc.contains("ba đình");
            
            boolean matchSubj = filterSubject == null || subj.contains(filterSubject.toLowerCase()); 
            boolean matchLoc = filterLocation == null || loc.contains(filterLocation.toLowerCase());
            boolean matchSalary = true; if (filterSalary != null) { if (filterSalary.equals("Dưới 150k")) matchSalary = sal < 150000; else if (filterSalary.equals("150k - 300k")) matchSalary = sal >= 150000 && sal <= 300000; else if (filterSalary.equals("Trên 300k")) matchSalary = sal > 300000; }
            boolean matchSchedule = true; if (filterSchedule != null) { if (filterSchedule.equals("Thứ 2-4-6")) matchSchedule = time.contains("2,4,6"); else if (filterSchedule.equals("Cuối tuần")) matchSchedule = time.contains("t7") || time.contains("cn") || time.contains("thứ 7"); else if (filterSchedule.equals("Buổi tối")) matchSchedule = time.contains("19") || time.contains("20") || time.contains("tối"); }
            boolean matchStatus = true; if (filterStatus != null) { if (filterStatus.equals("Đã lưu")) matchStatus = m.isSaved; else if (filterStatus.equals("Gấp / HOT")) matchStatus = tagDesc.contains("hot") || tagDesc.contains("gấp"); else if (filterStatus.equals("Còn lớp")) matchStatus = !m.isTaken; }
            
            if (matchQuick && matchSubj && matchLoc && matchSalary && matchSchedule && matchStatus) displayList.add(m);
        }

        if (filterSort.contains("Lương cao")) displayList.sort((c1, c2) -> Double.compare(parseSalary(c2.sal), parseSalary(c1.sal))); 
        else if (filterSort.contains("Lương thấp")) displayList.sort((c1, c2) -> Double.compare(parseSalary(c1.sal), parseSalary(c2.sal))); 
        else if (filterSort.contains("Lớp HOT")) displayList.sort((c1, c2) -> { boolean hot1 = c1.tagText.toLowerCase().contains("hot") || c1.tagText.toLowerCase().contains("gấp"); boolean hot2 = c2.tagText.toLowerCase().contains("hot") || c2.tagText.toLowerCase().contains("gấp"); return Boolean.compare(hot2, hot1); });
        
        if (displayList.isEmpty()) { 
            classGridPanel.setLayout(new BorderLayout()); classGridPanel.add(emptyStatePanel, BorderLayout.CENTER); 
        } else { 
            if (isGridView) {
                // ĐÃ CHỈNH SỬA: Đổi sang 5 cột và giảm khoảng cách (gap) xuống 16px
                classGridPanel.setLayout(new GridLayout(0, 5, 16, 16)); 
                for (ClassModel m : displayList) classGridPanel.add(createCompactGridCard(m)); 
            } else {
                classGridPanel.setLayout(new BoxLayout(classGridPanel, BoxLayout.Y_AXIS));
                for (ClassModel m : displayList) { classGridPanel.add(createListCard(m)); classGridPanel.add(Box.createVerticalStrut(12)); }
            }
        }
        classGridPanel.revalidate(); classGridPanel.repaint();
    }

    private void initEmptyState() {
        emptyStatePanel = new JPanel(); emptyStatePanel.setLayout(new BoxLayout(emptyStatePanel, BoxLayout.Y_AXIS)); emptyStatePanel.setOpaque(false);
        JLabel emptyIcon = new JLabel(); setNetworkIcon(emptyIcon, "https://img.icons8.com/fluency/96/empty-box.png", 80, 80); emptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel emptyText = new JLabel("Không tìm thấy lớp học phù hợp."); emptyText.setFont(new Font("Segoe UI", Font.BOLD, 16)); emptyText.setForeground(TEXT); emptyText.setAlignmentX(Component.CENTER_ALIGNMENT);
        PillButton btnClear = new PillButton("Xóa bộ lọc"); btnClear.setBackground(Color.decode("#F0F4FF")); btnClear.setForeground(PRIMARY); btnClear.setBorderColor(PRIMARY); btnClear.setAlignmentX(Component.CENTER_ALIGNMENT); btnClear.addActionListener(e -> resetAllFilters());
        emptyStatePanel.add(Box.createVerticalStrut(80)); emptyStatePanel.add(emptyIcon); emptyStatePanel.add(Box.createVerticalStrut(15)); emptyStatePanel.add(emptyText); emptyStatePanel.add(Box.createVerticalStrut(15)); emptyStatePanel.add(btnClear);
    }

    private void updateUIState() {
        int advancedCount = 0; if (filterSalary != null) advancedCount++; if (filterSchedule != null) advancedCount++; if (filterStatus != null) advancedCount++;
        styleQuickPill(btnAll, "ALL".equals(quickFilter)); styleQuickPill(btnNear, "NEAR".equals(quickFilter)); styleQuickPill(btnHighSalary, "HIGH_SALARY".equals(quickFilter)); styleQuickPill(btnTakenQuick, "TAKEN".equals(quickFilter));
        if (!filterSort.equals("Mới nhất")) { btnSort.setText("Sắp xếp ↓"); btnSort.setBackground(Color.decode("#EFF6FF")); btnSort.setForeground(PRIMARY); btnSort.setBorderColor(PRIMARY); } else { btnSort.setText("Sắp xếp ▼"); btnSort.setBackground(Color.WHITE); btnSort.setForeground(TEXT); btnSort.setBorderColor(BORDER); }
        if (advancedCount > 0) { btnAdvancedFilter.setText(" Lọc (" + advancedCount + ")"); btnAdvancedFilter.setBackground(PRIMARY); btnAdvancedFilter.setForeground(Color.WHITE); btnAdvancedFilter.setBorderColor(PRIMARY); setNetworkIcon(btnAdvancedFilter, "https://img.icons8.com/fluency-systems-regular/30/FFFFFF/filter-and-sort.png", 16, 16); } else { btnAdvancedFilter.setText(" Lọc"); btnAdvancedFilter.setBackground(Color.WHITE); btnAdvancedFilter.setForeground(TEXT); btnAdvancedFilter.setBorderColor(BORDER); setNetworkIcon(btnAdvancedFilter, "https://img.icons8.com/fluency-systems-regular/30/1E293B/filter-and-sort.png", 16, 16); }
        boolean hasFilters = !"ALL".equals(quickFilter) || filterSalary != null || filterSchedule != null || filterStatus != null || filterSubject != null || filterLocation != null;
        renderActiveTags(hasFilters, advancedCount); applyFilterAndSort();
    }

    private void resetAllFilters() { quickFilter = "ALL"; filterSubject = null; filterLocation = null; filterSort = "Mới nhất"; filterSalary = null; filterSchedule = null; filterStatus = null; updateUIState(); }
    
    private void renderActiveTags(boolean hasFilters, int advancedCount) {
        activeTagsPanel.removeAll();
        if (hasFilters) {
            activeTagsPanel.setVisible(true);
            if (!"ALL".equals(quickFilter)) { activeTagsPanel.add(createRemovableTag(getQuickFilterLabel(), () -> { quickFilter = "ALL"; updateUIState(); })); }
            if (filterSubject != null) activeTagsPanel.add(createRemovableTag(filterSubject, () -> { filterSubject = null; updateUIState(); }));
            if (filterLocation != null) activeTagsPanel.add(createRemovableTag(filterLocation, () -> { filterLocation = null; updateUIState(); }));
            if (filterSalary != null) activeTagsPanel.add(createRemovableTag(filterSalary, () -> { filterSalary = null; updateUIState(); }));
            if (filterSchedule != null) activeTagsPanel.add(createRemovableTag(filterSchedule, () -> { filterSchedule = null; updateUIState(); }));
            if (filterStatus != null) activeTagsPanel.add(createRemovableTag(filterStatus, () -> { filterStatus = null; updateUIState(); }));
            JLabel lblClear = new JLabel("  Xóa tất cả"); lblClear.setFont(new Font("Segoe UI", Font.BOLD, 12)); lblClear.setForeground(Color.decode("#EF4444")); lblClear.setCursor(new Cursor(Cursor.HAND_CURSOR)); lblClear.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { resetAllFilters(); } }); activeTagsPanel.add(lblClear);
        } else { activeTagsPanel.setVisible(false); }
        activeTagsPanel.revalidate(); activeTagsPanel.repaint();
    }

    private String getQuickFilterLabel() { switch (quickFilter) { case "NEAR": return "Gần bạn"; case "HIGH_SALARY": return "Lương cao"; case "SAVED": return "Đã lưu"; case "TAKEN": return "Đã nhận"; default: return "Tất cả"; } }

    private void openAdvancedFilterModal() {
        JDialog modal = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Bộ lọc nâng cao", true); modal.setSize(420, 520); modal.setLocationRelativeTo(this); modal.setLayout(new BorderLayout()); modal.getContentPane().setBackground(Color.WHITE);
        JPanel content = new JPanel(); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS)); content.setBackground(Color.WHITE); content.setBorder(new EmptyBorder(22, 26, 22, 26));
        ButtonGroup grpSalary = new ButtonGroup(); JRadioButton sal1 = createModalRadio("Dưới 150k"); JRadioButton sal2 = createModalRadio("150k - 300k"); JRadioButton sal3 = createModalRadio("Trên 300k"); grpSalary.add(sal1); grpSalary.add(sal2); grpSalary.add(sal3);
        ButtonGroup grpSchedule = new ButtonGroup(); JRadioButton sch1 = createModalRadio("Thứ 2-4-6"); JRadioButton sch2 = createModalRadio("Cuối tuần"); JRadioButton sch3 = createModalRadio("Buổi tối"); grpSchedule.add(sch1); grpSchedule.add(sch2); grpSchedule.add(sch3);
        ButtonGroup grpStatus = new ButtonGroup(); JRadioButton st1 = createModalRadio("Còn lớp"); JRadioButton st2 = createModalRadio("Đã lưu"); JRadioButton st3 = createModalRadio("Gấp / HOT"); grpStatus.add(st1); grpStatus.add(st2); grpStatus.add(st3);
        if (filterSalary != null) { if (filterSalary.equals(sal1.getText())) sal1.setSelected(true); else if (filterSalary.equals(sal2.getText())) sal2.setSelected(true); else if (filterSalary.equals(sal3.getText())) sal3.setSelected(true); }
        if (filterSchedule != null) { if (filterSchedule.equals(sch1.getText())) sch1.setSelected(true); else if (filterSchedule.equals(sch2.getText())) sch2.setSelected(true); else if (filterSchedule.equals(sch3.getText())) sch3.setSelected(true); }
        if (filterStatus != null) { if (filterStatus.equals(st1.getText())) st1.setSelected(true); else if (filterStatus.equals(st2.getText())) st2.setSelected(true); else if (filterStatus.equals(st3.getText())) st3.setSelected(true); }
        content.add(createModalSection("Mức lương", new JComponent[]{sal1, sal2, sal3})); content.add(Box.createVerticalStrut(15)); content.add(createModalSection("Lịch học", new JComponent[]{sch1, sch2, sch3})); content.add(Box.createVerticalStrut(15)); content.add(createModalSection("Trạng thái", new JComponent[]{st1, st2, st3}));
        JScrollPane scroll = new JScrollPane(content); scroll.setBorder(null);
        JPanel footer = new JPanel(new BorderLayout()); footer.setBackground(Color.WHITE); footer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER), new EmptyBorder(15, 25, 15, 25)));
        PillButton btnClear = new PillButton("Xóa bộ lọc"); btnClear.setForeground(MUTED); btnClear.setBorderColor(Color.WHITE); btnClear.addActionListener(e -> { grpSalary.clearSelection(); grpSchedule.clearSelection(); grpStatus.clearSelection(); });
        PillButton btnApply = new PillButton("Áp dụng"); btnApply.setBackground(PRIMARY); btnApply.setForeground(Color.WHITE); btnApply.setBorderColor(PRIMARY); btnApply.setPreferredSize(new Dimension(120, 38)); btnApply.addActionListener(e -> { filterSalary = getSelectedRadio(grpSalary); filterSchedule = getSelectedRadio(grpSchedule); filterStatus = getSelectedRadio(grpStatus); updateUIState(); modal.dispose(); });
        footer.add(btnClear, BorderLayout.WEST); footer.add(btnApply, BorderLayout.EAST); modal.add(scroll, BorderLayout.CENTER); modal.add(footer, BorderLayout.SOUTH); modal.setVisible(true);
    }
    
    private void setupDropdownMenu(PillButton badge, String[] options, String type) {
        JPopupMenu popupMenu = new JPopupMenu(); popupMenu.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        for (String option : options) {
            JMenuItem item = new JMenuItem("  " + option + "  "); item.setFont(new Font("Segoe UI", Font.PLAIN, 14)); item.setBackground(Color.WHITE); item.setPreferredSize(new Dimension(190, 38)); item.setCursor(new Cursor(Cursor.HAND_CURSOR));
            item.addActionListener(e -> { if (type.equals("SUBJECT")) { filterSubject = option.equals("Tất cả môn") ? null : option; } else if (type.equals("LOCATION")) { filterLocation = option.equals("Tất cả địa điểm") ? null : option; } else if (type.equals("SORT")) { filterSort = option; } updateUIState(); }); popupMenu.add(item);
        }
        badge.addActionListener(e -> popupMenu.show(badge, 0, badge.getHeight() + 5));
    }

    private void showClassDetailModal(String id, String subj, String sal, String addr, String time, String req, String tagText, String tagColor, ClassModel model) {
        JDialog modal = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chi tiết lớp học", true);
        modal.setSize(520, 600); modal.setLocationRelativeTo(this); modal.setUndecorated(true); modal.setBackground(new Color(0, 0, 0, 0));

        JPanel roundedMainPanel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24); g2.setColor(BORDER); g2.setStroke(new BasicStroke(1.5f)); g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24); g2.dispose(); }
        }; roundedMainPanel.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false); header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(16, 24, 16, 24)));
        JLabel titleLbl = new JLabel("Chi tiết yêu cầu nhận lớp"); titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 18)); titleLbl.setForeground(TEXT); header.add(titleLbl, BorderLayout.WEST);
        JLabel closeBtn = new JLabel(); setNetworkIcon(closeBtn, "https://img.icons8.com/material-rounded/48/9CA3AF/delete-sign.png", 20, 20); closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR)); closeBtn.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { modal.dispose(); } }); header.add(closeBtn, BorderLayout.EAST);

        JPanel body = new JPanel(); body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS)); body.setOpaque(false); body.setBorder(new EmptyBorder(24, 24, 24, 24));
        JLabel subjLbl = new JLabel("<html><div style='width:400px; line-height:1.2;'>" + subj + "</div></html>"); subjLbl.setFont(new Font("Segoe UI", Font.BOLD, 22)); subjLbl.setForeground(PRIMARY_DARK); subjLbl.setAlignmentX(Component.LEFT_ALIGNMENT); body.add(subjLbl);
        
        JPanel tagsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); tagsRow.setOpaque(false); tagsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        tagsRow.add(createTagBadge(id, "#F3F4F6", "#374151")); tagsRow.add(Box.createHorizontalStrut(8)); tagsRow.add(createTagBadge(tagText, tagColor, "#FFFFFF")); 
        body.add(Box.createVerticalStrut(12)); body.add(tagsRow); body.add(Box.createVerticalStrut(24));

        JPanel infoGrid = new JPanel(new GridLayout(0, 1, 0, 16)); infoGrid.setOpaque(false); infoGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoGrid.add(createDetailRow("https://img.icons8.com/color/48/money-bag.png", "Học phí:", sal, SUCCESS, true));
        infoGrid.add(createDetailRow("https://img.icons8.com/color/48/clock--v1.png", "Lịch học:", time, TEXT, false));
        infoGrid.add(createDetailRow("https://img.icons8.com/color/48/people.png", "Sĩ số:", "1 học sinh", TEXT, false));
        infoGrid.add(createDetailRow("https://img.icons8.com/color/48/phone.png", "SĐT Liên hệ:", "0987 654 321", TEXT, true));
        body.add(infoGrid); body.add(Box.createVerticalStrut(24));

        JPanel mapBtn = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8)) {
            boolean isHover = false;
            @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(isHover ? Color.decode("#EFF6FF") : Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12); g2.setColor(BORDER); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12); g2.dispose(); }
        }; mapBtn.setOpaque(false); mapBtn.setCursor(new Cursor(Cursor.HAND_CURSOR)); mapBtn.setAlignmentX(Component.LEFT_ALIGNMENT); mapBtn.setMaximumSize(new Dimension(999, 44));
        JLabel mapIcon = new JLabel(); setNetworkIcon(mapIcon, "https://img.icons8.com/color/48/marker--v1.png", 20, 20); JLabel mapTxt = new JLabel("Xem địa điểm trên Google Maps"); mapTxt.setFont(new Font("Segoe UI", Font.BOLD, 13)); mapTxt.setForeground(PRIMARY_DARK); mapBtn.add(mapIcon); mapBtn.add(mapTxt);
        mapBtn.addMouseListener(new MouseAdapter() { @Override public void mouseEntered(MouseEvent e) { mapBtn.repaint(); } @Override public void mouseExited(MouseEvent e) { mapBtn.repaint(); } @Override public void mouseClicked(MouseEvent e) { showMapPreviewPopup(addr, modal); } });
        body.add(mapBtn); body.add(Box.createVerticalStrut(24));

        JPanel reqHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); reqHeader.setOpaque(false); reqHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel reqIcon = new JLabel(); setNetworkIcon(reqIcon, "https://img.icons8.com/fluency/48/note.png", 20, 20); JLabel reqTitle = new JLabel("Yêu cầu & Ghi chú từ phụ huynh:"); reqTitle.setFont(new Font("Segoe UI", Font.BOLD, 14)); reqTitle.setForeground(TEXT); reqHeader.add(reqIcon); reqHeader.add(reqTitle);
        body.add(reqHeader); body.add(Box.createVerticalStrut(8));

        JPanel reqBox = new JPanel(new BorderLayout()); reqBox.setBackground(Color.decode("#F9FAFB")); reqBox.setBorder(new EmptyBorder(16, 16, 16, 16)); reqBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel reqText = new JLabel("<html><div style='width:380px; line-height:1.5;'>" + req.replace("\n", "<br>") + "</div></html>"); reqText.setFont(new Font("Segoe UI", Font.PLAIN, 14)); reqText.setForeground(MUTED); reqBox.add(reqText, BorderLayout.CENTER);
        body.add(reqBox); body.add(Box.createVerticalStrut(20));
        
        JPanel trustBox = new JPanel(new GridLayout(2, 2, 10, 10)); trustBox.setOpaque(false); trustBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        trustBox.add(createTrustItem("Cam kết chất lượng")); trustBox.add(createTrustItem("Giáo viên kinh nghiệm")); trustBox.add(createTrustItem("Lộ trình rõ ràng")); trustBox.add(createTrustItem("Hỗ trợ tận tâm"));
        body.add(trustBox);

        JScrollPane scrollBody = new JScrollPane(body); scrollBody.setBorder(null); scrollBody.setOpaque(false); scrollBody.getViewport().setOpaque(false); scrollBody.getVerticalScrollBar().setUnitIncrement(16);

        JPanel footer = new JPanel(); footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS)); footer.setOpaque(false); footer.setBorder(new EmptyBorder(16, 24, 20, 24));
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0)); btnRow.setOpaque(false);
        RoundedButton btnCancel = new RoundedButton("Hủy bỏ", 12); btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 14)); btnCancel.setBackground(Color.WHITE); btnCancel.setForeground(MUTED); btnCancel.setBorderColor(BORDER); btnCancel.setPreferredSize(new Dimension(100, 42)); btnCancel.addActionListener(e -> modal.dispose());

        RoundedButton btnConfirm = new RoundedButton("Xác nhận đăng ký", 12); btnConfirm.setFont(new Font("Segoe UI", Font.BOLD, 14)); btnConfirm.setBackground(PRIMARY); btnConfirm.setForeground(Color.WHITE); btnConfirm.setBorderColor(PRIMARY); btnConfirm.setPreferredSize(new Dimension(180, 42));
        btnConfirm.addActionListener(e -> {
            try { 
                NetworkManager.getInstance().sendPacket(new Packet("ACCEPT_CLASS", id)); 
                model.isTaken = true; 
                applyFilterAndSort(); 
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Không thể gửi yêu cầu nhận lớp. Vui lòng thử lại."); }
            modal.dispose();
        });

        btnRow.add(btnCancel); btnRow.add(btnConfirm); footer.add(btnRow);
        
        JPanel securePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0)); securePanel.setOpaque(false);
        JLabel secureIcon = new JLabel(); setNetworkIcon(secureIcon, "https://img.icons8.com/fluency-systems-regular/48/9CA3AF/lock.png", 14, 14);
        JLabel secureLbl = new JLabel("Thông tin của bạn được bảo mật và chỉ dùng để xử lý yêu cầu."); secureLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12)); secureLbl.setForeground(MUTED);
        securePanel.add(secureIcon); securePanel.add(secureLbl);
        footer.add(Box.createVerticalStrut(16)); footer.add(securePanel);

        roundedMainPanel.add(header, BorderLayout.NORTH); roundedMainPanel.add(scrollBody, BorderLayout.CENTER); roundedMainPanel.add(footer, BorderLayout.SOUTH);
        modal.setContentPane(roundedMainPanel); modal.setVisible(true);
    }

    private void showMapPreviewPopup(String address, JDialog parentModal) {
        JDialog mapDialog = new JDialog(parentModal, "Bản đồ", true); mapDialog.setSize(380, 240); mapDialog.setLocationRelativeTo(parentModal); mapDialog.setUndecorated(true); mapDialog.setBackground(new Color(0,0,0,0));
        JPanel bgPanel = new JPanel(new BorderLayout()) { @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20); g2.setColor(BORDER); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20); g2.dispose(); } }; bgPanel.setOpaque(false); bgPanel.setBorder(new EmptyBorder(24, 24, 24, 24));
        JPanel loadingPanel = new JPanel(new BorderLayout()); loadingPanel.setOpaque(false); JLabel lblLoad = new JLabel("Đang tìm vị trí trên bản đồ...", SwingConstants.CENTER); lblLoad.setFont(new Font("Segoe UI", Font.ITALIC, 14)); lblLoad.setForeground(MUTED); loadingPanel.add(lblLoad, BorderLayout.CENTER); bgPanel.add(loadingPanel, BorderLayout.CENTER); mapDialog.setContentPane(bgPanel);
        Timer timer = new Timer(500, e -> {
            bgPanel.removeAll(); JPanel content = new JPanel(); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS)); content.setOpaque(false);
            JLabel iconMap = new JLabel(); setNetworkIcon(iconMap, "https://img.icons8.com/fluency/96/map-marker.png", 48, 48); iconMap.setAlignmentX(Component.CENTER_ALIGNMENT); content.add(iconMap); content.add(Box.createVerticalStrut(10));
            JLabel lblAddr = new JLabel("<html><div style='text-align:center; width:300px;'><b>" + address + "</b></div></html>", SwingConstants.CENTER); lblAddr.setFont(new Font("Segoe UI", Font.PLAIN, 15)); lblAddr.setForeground(TEXT); lblAddr.setAlignmentX(Component.CENTER_ALIGNMENT); content.add(lblAddr); content.add(Box.createVerticalStrut(6));
            JLabel lblDist = new JLabel("Cách bạn: ~3.5 km  |  Thời gian di chuyển: 15 phút", SwingConstants.CENTER); lblDist.setFont(new Font("Segoe UI", Font.PLAIN, 13)); lblDist.setForeground(SUCCESS); lblDist.setAlignmentX(Component.CENTER_ALIGNMENT); content.add(lblDist); content.add(Box.createVerticalStrut(20));
            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0)); btnRow.setOpaque(false);
            RoundedButton btnCopy = new RoundedButton("Sao chép", 8); btnCopy.setBackground(Color.decode("#F3F4F6")); btnCopy.setForeground(TEXT); btnCopy.setBorderColor(BORDER); btnCopy.addActionListener(ev -> { Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(address), null); JOptionPane.showMessageDialog(mapDialog, "Đã sao chép địa chỉ vào bộ nhớ tạm!"); });
            RoundedButton btnOpenMap = new RoundedButton("Mở bản đồ", 8); btnOpenMap.setBackground(PRIMARY); btnOpenMap.setForeground(Color.WHITE); btnOpenMap.setBorderColor(PRIMARY); btnOpenMap.addActionListener(ev -> { try { String encodedAddr = URLEncoder.encode(address, "UTF-8"); Desktop.getDesktop().browse(new URI("https://maps.google.com/?q=" + encodedAddr)); mapDialog.dispose(); } catch(Exception ex) { JOptionPane.showMessageDialog(mapDialog, "Không thể mở ứng dụng bản đồ. Vui lòng thử lại!"); } });
            RoundedButton btnClose = new RoundedButton("Đóng lại", 8); btnClose.setBackground(Color.WHITE); btnClose.setForeground(MUTED); btnClose.setBorderColor(Color.WHITE); btnClose.addActionListener(ev -> mapDialog.dispose());
            btnRow.add(btnCopy); btnRow.add(btnOpenMap); content.add(btnRow); bgPanel.add(btnClose, BorderLayout.NORTH); bgPanel.add(content, BorderLayout.CENTER); bgPanel.revalidate(); bgPanel.repaint();
        }); timer.setRepeats(false); timer.start(); mapDialog.setVisible(true);
    }

    // --- Helpers ---
    private JPanel createDetailRow(String iconUrl, String label, String value, Color valueColor, boolean isValueBold) { JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0)); row.setOpaque(false); JLabel iconLbl = new JLabel(); setNetworkIcon(iconLbl, iconUrl, 24, 24); JLabel titleLbl = new JLabel(label); titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 14)); titleLbl.setForeground(MUTED); titleLbl.setPreferredSize(new Dimension(90, 20)); JLabel valLbl = new JLabel("<html><div style='width: 300px;'>" + value + "</div></html>"); valLbl.setFont(new Font("Segoe UI", isValueBold ? Font.BOLD : Font.PLAIN, 15)); valLbl.setForeground(valueColor); row.add(iconLbl); row.add(titleLbl); row.add(valLbl); return row; }
    private JPanel createTrustItem(String text) { JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)); p.setOpaque(false); JLabel icon = new JLabel(); setNetworkIcon(icon, "https://img.icons8.com/color/48/checked--v1.png", 16, 16); JLabel lbl = new JLabel(text); lbl.setFont(new Font("Segoe UI", Font.BOLD, 12)); lbl.setForeground(SUCCESS); p.add(icon); p.add(lbl); return p; }
    private void styleQuickPill(PillButton btn, boolean active) { if (btn == null) return; if (active) { btn.setBackground(Color.decode("#EFF6FF")); btn.setForeground(PRIMARY); btn.setBorderColor(Color.decode("#BFDBFE")); } else { btn.setBackground(Color.WHITE); btn.setForeground(TEXT); btn.setBorderColor(BORDER); } }
    private JPanel createTagBadge(String text, String bgColor, String fgColor) { return new TagBadge(text, bgColor, fgColor); }
    private JPanel createIconTextRow(String iconUrl, String text, String textColor, int fontSize, boolean isBold) { JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); p.setOpaque(false); p.setAlignmentX(Component.LEFT_ALIGNMENT); JLabel iconLbl = new JLabel(); setNetworkIcon(iconLbl, iconUrl, 18, 18); JLabel txtLbl = new JLabel(text); txtLbl.setFont(new Font("Segoe UI", isBold ? Font.BOLD : Font.PLAIN, fontSize)); txtLbl.setForeground(Color.decode(textColor)); p.add(iconLbl); p.add(txtLbl); return p; }
    private double parseSalary(String salString) { try { String numOnly = salString.replaceAll("[^0-9]", ""); return numOnly.isEmpty() ? 0 : Double.parseDouble(numOnly); } catch (Exception e) { return 0; } }
    private String getSelectedRadio(ButtonGroup grp) { for (java.util.Enumeration<AbstractButton> buttons = grp.getElements(); buttons.hasMoreElements();) { AbstractButton button = buttons.nextElement(); if (button.isSelected()) return button.getText(); } return null; }
    private JRadioButton createModalRadio(String text) { JRadioButton rb = new JRadioButton(text); rb.setFont(new Font("Segoe UI", Font.PLAIN, 14)); rb.setForeground(TEXT); rb.setBackground(Color.WHITE); rb.setFocusPainted(false); rb.setCursor(new Cursor(Cursor.HAND_CURSOR)); return rb; }
    private JPanel createModalSection(String title, JComponent[] items) { JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setBackground(Color.WHITE); JLabel lblTitle = new JLabel(title); lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15)); lblTitle.setForeground(TEXT); p.add(lblTitle); p.add(Box.createVerticalStrut(10)); for (JComponent item : items) { item.setAlignmentX(Component.LEFT_ALIGNMENT); p.add(item); p.add(Box.createVerticalStrut(5)); } return p; }
    private JPanel createRemovableTag(String text, Runnable onRemove) { JPanel tag = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2)) { @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(BORDER); g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight()); g2.dispose(); super.paintComponent(g); } }; tag.setOpaque(false); tag.setBorder(new EmptyBorder(3, 10, 3, 5)); JLabel lbl = new JLabel(text); lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12)); lbl.setForeground(TEXT); JLabel lblClose = new JLabel(" ✕ "); lblClose.setFont(new Font("Segoe UI", Font.BOLD, 10)); lblClose.setForeground(MUTED); lblClose.setCursor(new Cursor(Cursor.HAND_CURSOR)); lblClose.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { onRemove.run(); } }); tag.add(lbl); tag.add(lblClose); return tag; }
    private void setNetworkIcon(JLabel label, String urlStr, int width, int height) { String key = urlStr + "_" + width + "x" + height; if (iconCache.containsKey(key)) { label.setIcon(iconCache.get(key)); return; } new Thread(() -> { try { ImageIcon raw = new ImageIcon(new URL(urlStr)); Image img = raw.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH); ImageIcon scaled = new ImageIcon(img); iconCache.put(key, scaled); SwingUtilities.invokeLater(() -> label.setIcon(scaled)); } catch (Exception ignored) {} }).start(); }
    private void setNetworkIcon(JButton button, String urlStr, int width, int height) { String key = urlStr + "_" + width + "x" + height; if (iconCache.containsKey(key)) { button.setIcon(iconCache.get(key)); return; } new Thread(() -> { try { ImageIcon raw = new ImageIcon(new URL(urlStr)); Image img = raw.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH); ImageIcon scaled = new ImageIcon(img); iconCache.put(key, scaled); SwingUtilities.invokeLater(() -> button.setIcon(scaled)); } catch (Exception ignored) {} }).start(); }

    class TagBadge extends JPanel { private final JLabel lbl; public TagBadge(String text, String bgColor, String fgColor) { setOpaque(false); setBackground(Color.decode(bgColor)); setBorder(new EmptyBorder(4, 12, 4, 12)); setLayout(new BorderLayout()); lbl = new JLabel(text); lbl.setFont(new Font("Segoe UI", Font.BOLD, 10)); lbl.setForeground(Color.decode(fgColor)); add(lbl, BorderLayout.CENTER); } public JLabel getLabel() { return lbl; } @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(getBackground()); g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight()); g2.dispose(); super.paintComponent(g); } }
    class PillButton extends JButton { private Color borderColor = Color.decode("#E5E7EB"); public PillButton(String text) { super(text); setContentAreaFilled(false); setFocusPainted(false); setFocusable(false); setBorder(new EmptyBorder(6, 16, 6, 16)); setCursor(new Cursor(Cursor.HAND_CURSOR)); } public void setBorderColor(Color color) { this.borderColor = color; repaint(); } @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(getBackground()); g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight()); g2.dispose(); super.paintComponent(g); } @Override protected void paintBorder(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(borderColor); g2.setStroke(new BasicStroke(1.2f)); g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, getHeight() - 3, getHeight() - 3); g2.dispose(); } }
    class RoundedButton extends JButton { private Color borderColor = Color.decode("#E5E7EB"); private final int radius; public RoundedButton(String text, int radius) { super(text); this.radius = radius; setContentAreaFilled(false); setFocusPainted(false); setFocusable(false); setBorder(new EmptyBorder(5, 15, 5, 15)); setCursor(new Cursor(Cursor.HAND_CURSOR)); } public void setBorderColor(Color color) { this.borderColor = color; repaint(); } @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(getBackground()); g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius); g2.dispose(); super.paintComponent(g); } @Override protected void paintBorder(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(borderColor); g2.setStroke(new BasicStroke(1.2f)); g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, radius, radius); g2.dispose(); } }
    
    class ImageHeaderPanel extends JPanel {
        private Image image; private final Color fallbackA; private final Color fallbackB;
        public ImageHeaderPanel(String resourcePath, Color fallbackA, Color fallbackB) { this.fallbackA = fallbackA; this.fallbackB = fallbackB; setOpaque(false); setLayout(new BorderLayout()); try { URL url = getClass().getResource(resourcePath); if (url != null) image = new ImageIcon(url).getImage(); } catch (Exception ignored) {} }
        @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
        Object arcObj = getClientProperty("JComponent.arc"); int arc = arcObj instanceof Integer ? (Integer) arcObj : 0;
        Shape oldClip = g2.getClip(); if (arc > 0) { g2.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arc, arc)); }
        if (image != null) { int w = getWidth(); int h = getHeight(); int imgW = image.getWidth(null); int imgH = image.getHeight(null); if (imgW > 0 && imgH > 0) { double scale = Math.max((double) w / imgW, (double) h / imgH); int drawW = (int) (imgW * scale); int drawH = (int) (imgH * scale); int x = (w - drawW) / 2; int y = (h - drawH) / 2; g2.drawImage(image, x, y, drawW, drawH, null); g2.setColor(new Color(17, 24, 39, 42)); g2.fillRect(0, 0, w, h); } } else { GradientPaint gp = new GradientPaint(0, 0, fallbackA, getWidth(), getHeight(), fallbackB); g2.setPaint(gp); g2.fillRect(0, 0, getWidth(), getHeight()); } g2.setClip(oldClip); g2.dispose(); super.paintComponent(g); }
    }

    class WebScrollPanel extends JPanel implements Scrollable {
        public WebScrollPanel(LayoutManager layout) { super(layout); }
        @Override public Dimension getPreferredScrollableViewportSize() { return super.getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return Math.max(24, visibleRect.height / 2); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; } 
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
