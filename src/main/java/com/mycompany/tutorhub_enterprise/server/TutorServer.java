package com.mycompany.tutorhub_enterprise.server;

import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.utils.SerializationUtils;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TutorServer extends WebSocketServer {
    // Đọc Port từ Đám mây (Biến môi trường), nếu không có thì dùng cổng 7860
    private static final int PORT = Integer.parseInt(ServerConfig.get("TUTORHUB_WS_INTERNAL_PORT", "tutorhub.ws.internal.port", null, System.getenv("PORT") != null ? System.getenv("PORT") : "7860"));
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(500);

    public TutorServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("[SERVER] Kết nối mới từ: " + conn.getRemoteSocketAddress());
        // Khởi tạo ClientHandler mới cho kết nối này
        ClientHandler handler = new ClientHandler(conn);
        conn.setAttachment(handler); // Gắn handler vào connection để dùng lại
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("[SERVER] Kết nối đóng: " + conn.getRemoteSocketAddress() + " - Lý do: " + reason);
        ClientHandler handler = conn.getAttachment();
        if (handler != null) {
            handler.onDisconnect();
        }
    }

    private static final com.google.gson.Gson gson = new com.google.gson.Gson();

    @Override
    public void onMessage(WebSocket conn, String message) {
        threadPool.execute(() -> {
            try {
                Packet packet = gson.fromJson(message, Packet.class);
                ClientHandler handler = conn.getAttachment();
                if (handler != null) {
                    handler.setWebClient(true);
                    handler.processClientRequest(packet);
                }
            } catch (Exception e) {
                System.err.println("[SERVER LỖI] Lỗi giải mã JSON từ Web: " + e.getMessage());
            }
        });
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        threadPool.execute(() -> {
            try {
                // Giải mã Byte Array thành đối tượng Packet
                byte[] bytes = new byte[message.remaining()];
                message.get(bytes);
                Packet packet = (Packet) SerializationUtils.deserialize(bytes);
                
                ClientHandler handler = conn.getAttachment();
                if (handler != null) {
                    handler.processClientRequest(packet);
                }
            } catch (Exception e) {
                System.err.println("[SERVER LỖI] Lỗi giải mã gói tin: " + e.getMessage());
            }
        });
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[SERVER LỖI BẤT THƯỜNG] " + ex.getMessage());
        if (conn != null) {
            ClientHandler handler = conn.getAttachment();
            if (handler != null) {
                handler.onDisconnect();
            }
        }
    }

    @Override
    public void onStart() {
        System.out.println("[SERVER] Máy chủ WebSocket đã sẵn sàng và lắng nghe trên cổng " + getPort());
    }

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  HỆ THỐNG ĐIỀU PHỐI GIA SƯ ENTERPRISE (WEBSOCKETS)");
        System.out.println("==========================================================");
        
        TutorServer server = new TutorServer(new InetSocketAddress(PORT));
        com.mycompany.tutorhub_enterprise.server.db.ExamDatabaseManager.initialize();
        server.start();
        System.out.println("[SERVER] Đang khởi động...");
        
        try {
            String clientId = SocialAuthConfig.getGoogleClientId();
            System.out.println("[OAUTH CONFIG] Google Client ID loaded: true");
        } catch (Exception e) {
            System.out.println("[OAUTH CONFIG] Google Client ID loaded: false");
        }
        
        try {
            String clientSecret = SocialAuthConfig.getGoogleClientSecret();
            System.out.println("[OAUTH CONFIG] Google Client Secret loaded: true");
        } catch (Exception e) {
            System.out.println("[OAUTH CONFIG] Google Client Secret loaded: false");
        }
        
        try {
            String fbAppId = SocialAuthConfig.getFacebookAppId();
            System.out.println("[OAUTH CONFIG] Facebook App ID loaded: true");
        } catch (Exception e) {
            System.out.println("[OAUTH CONFIG] Facebook App ID loaded: false");
        }

        try {
            String fbSecret = SocialAuthConfig.getFacebookAppSecret();
            System.out.println("[OAUTH CONFIG] Facebook App Secret loaded: true");
        } catch (Exception e) {
            System.out.println("[OAUTH CONFIG] Facebook App Secret loaded: false");
        }
        
        String redirectPort = ServerConfig.get("TUTORHUB_GOOGLE_REDIRECT_PORT", "tutorhub.google.redirect.port", "google.redirect.port", "8889");
        System.out.println("[OAUTH CONFIG] Redirect Port: " + redirectPort);

        try {
            int fbPort = SocialAuthConfig.getFacebookOAuthHttpPort();
            com.mycompany.tutorhub_enterprise.server.oauth.FacebookOAuthCallbackServer fbServer = new com.mycompany.tutorhub_enterprise.server.oauth.FacebookOAuthCallbackServer(fbPort);
            fbServer.start();
        } catch (Exception e) {
            System.err.println("[OAUTH CONFIG ERROR] Không thể khởi động Facebook Callback Server: " + e.getMessage());
        }
    }
}