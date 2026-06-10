package com.mycompany.tutorhub_enterprise.client;

import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.utils.JcefHelper;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.io.OutputStream;

public class BlackboardFrame extends JFrame {

    private final Color BG_DARK = Color.decode("#0F172A");

    private MainDashboard dashboard;
    private String classId;

    private String currentBoardId = null;
    private String currentBoardTitle = null;
    private String role = "student";
    private boolean allowStudentDraw = false;
    private boolean canDraw = true;

    private CefBrowser cefBrowser;
    private CefClient cefClient;
        private boolean isEditorReady = false;
    private PermissionPanel permissionPanel;
    private String pendingLoadData = null;

    public BlackboardFrame(MainDashboard dashboard, String classId, String role) {
        this(dashboard, classId, role, false);
    }

    public BlackboardFrame(MainDashboard dashboard, String classId, String role, boolean allowStudentDraw) {
        this.dashboard = dashboard;
        this.classId = classId;
        this.role = role;
        this.allowStudentDraw = allowStudentDraw;
        setTitle("Bảng vẽ TutorHub - Toàn Màn Hình");
        setUndecorated(true); // Ẩn thanh viền cửa sổ
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Phóng to toàn màn hình
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_DARK);
        initUI();
    }

    private void initUI() {
        CefApp cefApp = JcefHelper.getCefApp();
        if (cefApp != null) {
            cefClient = cefApp.createClient();
            
            CefMessageRouter msgRouter = CefMessageRouter.create();
            msgRouter.addHandler(new CefMessageRouterHandlerAdapter() {
                @Override
                public boolean onQuery(CefBrowser browser, org.cef.browser.CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
                    if (request.equals("EDITOR_READY")) {
                        handleEditorReady();
                        callback.success("");
                        return true;
                    } else if (request.startsWith("SAVE_DATA:")) {
                        String payload = request.substring("SAVE_DATA:".length());
                        String[] parts = payload.split("\\|\\|\\|THUMBNAIL_SEP\\|\\|\\|");
                        String json = parts[0];
                        String thumb = parts.length > 1 ? parts[1] : "";
                        handleSaveData(json, thumb);
                        callback.success("");
                        return true;
                    } else if (request.startsWith("OPEN_URL:")) {
                        String urlToOpen = request.substring("OPEN_URL:".length());
                        try {
                            java.awt.Desktop.getDesktop().browse(new java.net.URI(urlToOpen));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        callback.success("");
                        return true;
                    } else if (request.equals("OPEN_REEL_COWATCH")) {
                        if (dashboard != null) {
                            BlackboardFrame.this.setVisible(false); // Ẩn frame bảng vẽ
                            dashboard.switchToCard("Class_Locket");
                        }
                        callback.success("");
                        return true;
                    } else if (request.equals("CLOSE_BOARD")) {
                        SwingUtilities.invokeLater(() -> {
                            if(JOptionPane.showConfirmDialog(BlackboardFrame.this, "Bạn có muốn đóng phiên bảng đen này?", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                                BlackboardFrame.this.setVisible(false); // Ẩn Frame đi
                                if (dashboard != null) dashboard.switchToCard("Blackboard"); 
                            }
                        });
                        callback.success("");
                        return true;
                    } else if (request.equals("REQUEST_SAVE_BOARD")) {
                        SwingUtilities.invokeLater(() -> {
                            saveBoardToServer();
                        });
                        callback.success("");
                        return true;
                    }
                    return false;
                }
            }, true);
            cefClient.addMessageRouter(msgRouter);
            
            String url = "";
            try {
                InputStream in = getClass().getResourceAsStream("/html/tldraw_board_v2.html");
                if (in != null) {
                    byte[] htmlBytes = in.readAllBytes();
                    in.close();
                    
                    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
                    server.createContext("/", new HttpHandler() {
                        @Override
                        public void handle(HttpExchange exchange) throws java.io.IOException {
                            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                            exchange.sendResponseHeaders(200, htmlBytes.length);
                            OutputStream os = exchange.getResponseBody();
                            os.write(htmlBytes);
                            os.close();
                        }
                    });
                    server.setExecutor(null);
                    server.start();
                    int port = server.getAddress().getPort();
                    boolean admitted = "teacher".equalsIgnoreCase(this.role);
                    boolean canDraw = "teacher".equalsIgnoreCase(this.role) || this.allowStudentDraw;
                    url = "http://localhost:" + port + "/?role=" + this.role
                            + "&admitted=" + admitted
                            + "&canDraw=" + canDraw;
                } else {
                    boolean admitted = "teacher".equalsIgnoreCase(this.role);
                    boolean canDraw = "teacher".equalsIgnoreCase(this.role) || this.allowStudentDraw;
                    url = getClass().getResource("/html/tldraw_board_v2.html").toExternalForm()
                            + "?role=" + this.role
                            + "&admitted=" + admitted
                            + "&canDraw=" + canDraw;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            cefBrowser = cefClient.createBrowser(url, false, false);
            add(cefBrowser.getUIComponent(), BorderLayout.CENTER);
        } else {
            add(new JLabel("Không thể khởi tạo JCEF Chromium", SwingConstants.CENTER), BorderLayout.CENTER);
        }
    }

    private void handleEditorReady() {
        isEditorReady = true;
        if (pendingLoadData != null) {
            String dataToLoad = pendingLoadData;
            pendingLoadData = null;
            String bId = currentBoardId != null ? escapeJsString(currentBoardId) : "default";
            cefBrowser.executeJavaScript("window.loadBoardData('" + escapeJsString(dataToLoad) + "', '" + bId + "');", cefBrowser.getURL(), 0);
        } else {
            String bId = currentBoardId != null ? escapeJsString(currentBoardId) : "default";
            cefBrowser.executeJavaScript("window.loadBoardData('null', '" + bId + "');", cefBrowser.getURL(), 0);
        }
    }
    
    private void handleSaveData(String jsonStr, String base64Thumbnail) {
        if (jsonStr == null || jsonStr.isEmpty()) return;
        
        SwingUtilities.invokeLater(() -> {
            try {
                String processedJson = jsonStr;
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"dataURL\":\"data:image/(png|jpeg|jpg);base64,([^\"]+)\"");
                java.util.regex.Matcher m = p.matcher(processedJson);
                StringBuffer sb = new StringBuffer();
                while (m.find()) {
                    String ext = m.group(1);
                    String b64 = m.group(2);
                    String remoteUrl = com.mycompany.tutorhub_enterprise.utils.B2Helper.uploadBase64Image(b64, ext);
                    if (remoteUrl != null) {
                        m.appendReplacement(sb, "\"dataURL\":\"" + remoteUrl + "\"");
                    } else {
                        m.appendReplacement(sb, m.group(0));
                    }
                }
                m.appendTail(sb);
                processedJson = sb.toString();
                
                String base64Objects = Base64.getEncoder().encodeToString(processedJson.getBytes("UTF-8"));
                String finalThumb = base64Thumbnail;
                if (finalThumb == null || finalThumb.isEmpty()) {
                    finalThumb = ""; 
                }
                
                String fullData = finalThumb.replace("\r", "").replace("\n", "") + "###" + base64Objects.replace("\r", "").replace("\n", "");
                String payload = (currentBoardId == null) 
                    ? (currentBoardTitle + "|Lớp Học Mặc Định|" + fullData) 
                    : (currentBoardId + "|" + currentBoardTitle + "|Lớp Học Mặc Định|" + fullData);
                    
                com.mycompany.tutorhub_enterprise.client.NetworkManager.getInstance().sendPacket(new Packet(currentBoardId == null ? "SAVE_BOARD" : "UPDATE_BOARD", payload));
                JOptionPane.showMessageDialog(BlackboardFrame.this, "Đang đồng bộ dữ liệu...", "INFO", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(BlackboardFrame.this, "Có lỗi khi tạo dữ liệu lưu!", "ERROR", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private String escapeJsString(String s) {
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    public void resetCanvas() {
        if (JOptionPane.showConfirmDialog(this, "Xoá toàn bộ nội dung bảng vẽ?", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (isEditorReady && cefBrowser != null) {
                cefBrowser.executeJavaScript("window.tldrawEditor.selectAll(); window.tldrawEditor.deleteShapes(window.tldrawEditor.getSelectedShapeIds());", cefBrowser.getURL(), 0);
            }
        }
    }

                public PermissionPanel getPermissionPanel() {
        return this.permissionPanel;
    }

    public void connectToLiveRoom(String lessonId, String classId, String className) {
        this.currentBoardId = classId;
        this.currentBoardTitle = className;
        if (isEditorReady && cefBrowser != null) {
            String bId = escapeJsString(classId);
            cefBrowser.executeJavaScript("window.loadBoardData('null', '" + bId + "');", cefBrowser.getURL(), 0);
        } else {
            pendingLoadData = "null";
        }
        
        // Notify server that lesson started if we are teacher
        if ("teacher".equals(this.role)) {
            try {
                com.mycompany.tutorhub_enterprise.client.NetworkManager.getInstance().sendPacket(
                    new com.mycompany.tutorhub_enterprise.models.Packet("START_LESSON", lessonId)
                );
            } catch (Exception ex) {
                System.err.println("Cannot notify START_LESSON: " + ex.getMessage());
            }
            
            // Setup permission panel
            if (permissionPanel == null && lessonId != null && !lessonId.equals("0")) {
                permissionPanel = new PermissionPanel(lessonId);
                add(permissionPanel, java.awt.BorderLayout.EAST);
                revalidate();
                repaint();
            }
        }
    }

    public void loadBoardData(String boardId, String title, String dbData) {
        this.currentBoardId = boardId;
        this.currentBoardTitle = title;
        
        if (dbData != null && dbData.contains("###")) {
            String[] parts = dbData.split("###");
            if (parts.length > 1) {
                try {
                    String base64Json = parts[1];
                    byte[] decoded = Base64.getDecoder().decode(base64Json);
                    String jsonStr = new String(decoded, "UTF-8");
                    
                    if (isEditorReady && cefBrowser != null) {
                        String bId = boardId != null ? escapeJsString(boardId) : "default";
                        cefBrowser.executeJavaScript("window.loadBoardData('" + escapeJsString(jsonStr) + "', '" + bId + "');", cefBrowser.getURL(), 0);
                    } else {
                        pendingLoadData = jsonStr;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            resetCanvas();
            if (isEditorReady && cefBrowser != null) {
                String bId = boardId != null ? escapeJsString(boardId) : "default";
                cefBrowser.executeJavaScript("window.loadBoardData('null', '" + bId + "');", cefBrowser.getURL(), 0);
            }
        }
    }

    private void saveBoardToServer() {
        String boardName = currentBoardTitle;
        if (currentBoardId == null) {
            boardName = JOptionPane.showInputDialog(this, "Nhập tên bảng vẽ để lưu:", "Lưu bảng", JOptionPane.PLAIN_MESSAGE);
            if (boardName == null || boardName.trim().isEmpty()) return;
        }

        currentBoardTitle = boardName;
        
        if (cefBrowser != null) {
            cefBrowser.executeJavaScript("window.requestSaveBoardAndThumbnail();", cefBrowser.getURL(), 0);
        }
    }

    /**
     * Gọi từ MainDashboard khi server gửi PUBLIC_LESSON_APPROVED.
     * Cập nhật trạng thái admitted trong JS mà không cần tạo lại frame.
     */
    public void triggerAdmitInJS() {
        if (cefBrowser != null) {
            String js = """
                if (window.currentRoom && window.currentRoom.localParticipant) {
                    var meta = window.rosterMetadataCache
                        ? window.rosterMetadataCache[window.currentRoom.localParticipant.identity]
                        : null;
                    if (!meta) meta = { role: 'student', displayName: window.currentUserName || 'Student', isHandRaised: false, isAdmitted: false };
                    meta.isAdmitted = true;
                    if (!window.rosterMetadataCache) window.rosterMetadataCache = {};
                    window.rosterMetadataCache[window.currentRoom.localParticipant.identity] = meta;
                    try { window.currentRoom.localParticipant.setMetadata(JSON.stringify(meta)); } catch(e) {}
                    var payload = JSON.stringify({ type: 'roster_sync_metadata', sender: window.currentRoom.localParticipant.identity, metadata: JSON.stringify(meta) });
                    window.currentRoom.localParticipant.publishData(new TextEncoder().encode(payload), { reliable: true });
                    if (typeof renderRoster === 'function') renderRoster();
                }
                window.isPreAdmitted = true;
                document.getElementById('lobby-overlay').style.display = 'none';
                if (typeof showToast === 'function') showToast('Giáo viên đã duyệt bạn vào lớp!', 5000);
            """;
            cefBrowser.executeJavaScript(js, cefBrowser.getURL(), 0);
        }
    }

    /**
     * Cập nhật quyền vẽ cho student trong runtime.
     */
        public void updateDrawPermission(boolean canDraw) {
        if (isEditorReady && cefBrowser != null) {
            String js = "if(window.tldrawAPI) { window.tldrawAPI.updateInstanceState({ isReadonly: " + (!canDraw) + " }); }";
            cefBrowser.executeJavaScript(js, cefBrowser.getURL(), 0);
        }
    }

        public void handlePermissionUpdate(String payload) {
        String[] parts = payload.split("\\|");
        if (parts.length >= 2) {
            String type = parts[0];
            boolean isEnabled = Boolean.parseBoolean(parts[1]);
            if ("DRAW".equals(type)) {
                this.canDraw = isEnabled;
                updateDrawPermission(isEnabled);
                System.out.println("Draw permission updated to " + isEnabled);
            } else if ("MIC".equals(type) || "CAM".equals(type)) {
                if (isEditorReady && cefBrowser != null) {
                    String js = "if(window.updateMediaPermission) { window.updateMediaPermission('" + type + "', " + isEnabled + "); }";
                    cefBrowser.executeJavaScript(js, cefBrowser.getURL(), 0);
                }
            }
        }
    }

    public void handleBoardPickerResponse(String payload) {
        SwingUtilities.invokeLater(() -> {
            BoardPickerDialog dialog = new BoardPickerDialog(this, payload, new BoardPickerDialog.BoardPickerListener() {
                @Override
                public void onBoardSelected(String dbData) {
                    // Extract board json
                    if (dbData != null && dbData.contains("###")) {
                        String[] parts = dbData.split("###");
                        if (parts.length > 1) {
                            try {
                                String base64Json = parts[1];
                                byte[] decoded = java.util.Base64.getDecoder().decode(base64Json);
                                String jsonStr = new String(decoded, "UTF-8");
                                
                                if (isEditorReady && cefBrowser != null) {
                                    String bId = currentBoardId != null ? escapeJsString(currentBoardId) : "default";
                                    cefBrowser.executeJavaScript("window.loadBoardData('" + escapeJsString(jsonStr) + "', '" + bId + "');", cefBrowser.getURL(), 0);
                                    
                                    // Trigger save so the students also get this update
                                    cefBrowser.executeJavaScript("if(window.tldrawAPI) { window.tldrawAPI.forceSave(); } else { window.triggerSave(); }", cefBrowser.getURL(), 0);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
            dialog.setVisible(true);
        });
    }
}





