package com.mycompany.tutorhub_enterprise.client;

import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.models.auth.AuthResponse;
import com.mycompany.tutorhub_enterprise.utils.SerializationUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class NetworkManager {
    private static volatile NetworkManager instance;
    private TutorWebSocketClient webSocketClient;
    private BlockingQueue<Packet> packetQueue = new LinkedBlockingQueue<>();

    private NetworkManager() {}

    public static NetworkManager getInstance() {
        if (instance == null) {
            synchronized (NetworkManager.class) {
                if (instance == null) {
                    instance = new NetworkManager();
                }
            }
        }
        return instance;
    }

    // Production can override this with -Dtutorhub.websocket.url=... or TUTORHUB_WS_URL.
    private static final String CLOUD_WEBSOCKET_URL = "wss://hocba299-3-tutorhub-core.hf.space";
    private static final String WS_URL_PROPERTY = "tutorhub.websocket.url";
    private static final String WS_URL_ENV = "TUTORHUB_WS_URL";

    public void connect(String host, int port) throws Exception {
        if (!isConnected()) {
            // URL kết nối chuẩn của WebSocket (ws:// hoặc wss://)
            // LƯU Ý: Đã bỏ qua host/port cũ để luôn trỏ tới CLOUD
            URI serverUri = resolveServerUri(host, port);
            packetQueue.clear();
            webSocketClient = new TutorWebSocketClient(serverUri, packetQueue);
            
            // connectBlocking() sẽ chặn thread hiện tại cho đến khi kết nối thành công hoặc thất bại
            boolean connected = webSocketClient.connectBlocking(30000, TimeUnit.MILLISECONDS);
            if (!connected) {
                throw new Exception("Lỗi: Không thể kết nối tới Cloud Server ở " + serverUri);
            }
            System.out.println("[NETWORK] Đã kết nối tới CLOUD SERVER: " + serverUri);
        }
    }



    private URI resolveServerUri(String host, int port) throws Exception {
        String configuredUrl = System.getProperty(WS_URL_PROPERTY);
        if (configuredUrl == null || configuredUrl.trim().isEmpty()) {
            configuredUrl = System.getenv(WS_URL_ENV);
        }
        if (configuredUrl != null && !configuredUrl.trim().isEmpty()) {
            return toWebSocketUri(configuredUrl.trim(), 7860);
        }

        // Return local server uri instead of forcing cloud
        return toWebSocketUri(host != null ? host : "localhost", 7860);
    }



    private URI toWebSocketUri(String value, int port) throws Exception {
        if (value.startsWith("ws://") || value.startsWith("wss://")) {
            return new URI(value);
        }
        int resolvedPort = port > 0 ? port : 7860;
        return new URI("ws://" + value + ":" + resolvedPort);
    }

    public boolean isConnected() {
        return webSocketClient != null && webSocketClient.isOpen();
    }

    public void disconnect() {
        try {
            if (webSocketClient != null && !webSocketClient.isClosed()) {
                webSocketClient.closeBlocking();
                System.out.println("[NETWORK] Đã ngắt kết nối an toàn.");
            }
        } catch (Exception e) {
            System.err.println("[NETWORK ERROR] Lỗi khi đóng kết nối: " + e.getMessage());
        } finally {
            webSocketClient = null;
            packetQueue.clear();
        }
    }

    public void sendPacket(Packet packet) throws Exception {
        if (isConnected()) {
            byte[] data = SerializationUtils.serialize(packet);
            webSocketClient.send(data);
        } else {
            throw new Exception("Lỗi: Chưa kết nối tới Server!");
        }
    }

    public Packet receivePacket() throws Exception {
        if (isConnected()) {
            // take() sẽ chặn (block) thread UI hiện tại cho đến khi có gói tin được bơm vào Queue
            // Việc này giúp 100% Code Giao diện (UI) cũ hoạt động bình thường mà không cần sửa logic
            return packetQueue.take();
        }
        throw new Exception("Lỗi: Mất kết nối tới Server!");
    }

    public Packet receivePacket(String expectedAction, String expectedRequestId, long timeoutMillis) throws Exception {
        if (!isConnected()) {
            throw new Exception("Loi: Mat ket noi toi Server!");
        }

        long deadline = System.currentTimeMillis() + Math.max(1, timeoutMillis);
        List<Packet> deferred = new ArrayList<>();
        try {
            while (System.currentTimeMillis() < deadline) {
                long remaining = Math.max(1, deadline - System.currentTimeMillis());
                Packet packet = packetQueue.poll(remaining, TimeUnit.MILLISECONDS);
                if (packet == null) {
                    break;
                }
                if (matches(packet, expectedAction, expectedRequestId)) {
                    return packet;
                }
                deferred.add(packet);
            }
        } finally {
            for (Packet packet : deferred) {
                packetQueue.offer(packet);
            }
        }

        throw new Exception("Timeout waiting for response: " + expectedAction);
    }

    private boolean matches(Packet packet, String expectedAction, String expectedRequestId) {
        if (expectedAction != null && !expectedAction.equals(packet.action)) {
            return false;
        }
        if (expectedRequestId == null || expectedRequestId.trim().isEmpty()) {
            return true;
        }
        return packet.data instanceof AuthResponse response
                && expectedRequestId.equals(response.getRequestId());
    }

    // Class nội bộ quản lý các sự kiện WebSocket
    private class TutorWebSocketClient extends WebSocketClient {
        private final BlockingQueue<Packet> queue;

        public TutorWebSocketClient(URI serverUri, BlockingQueue<Packet> queue) {
            super(serverUri);
            this.queue = queue;
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            System.out.println("[WEBSOCKET] Đã mở luồng kết nối.");
        }

        @Override
        public void onMessage(String message) {
            // Hệ thống dùng Byte Array thay vì String để tương thích hoàn toàn Object cũ
        }

        @Override
        public void onMessage(ByteBuffer message) {
            try {
                byte[] bytes = new byte[message.remaining()];
                message.get(bytes);
                Packet packet = (Packet) SerializationUtils.deserialize(bytes);
                // Đưa gói tin vào Hàng đợi để hàm receivePacket() nhặt lấy
                queue.put(packet);
            } catch (Exception e) {
                System.err.println("[WEBSOCKET LỖI] Lỗi parse gói tin: " + e.getMessage());
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            System.out.println("[WEBSOCKET] Đã đóng kết nối: " + reason);
        }

        @Override
        public void onError(Exception ex) {
            System.err.println("[WEBSOCKET LỖI BẤT THƯỜNG] " + ex.getMessage());
        }
    }
}
