package com.mycompany.tutorhub_enterprise.client.oauth;

import com.mycompany.tutorhub_enterprise.client.AuthClient;
import com.mycompany.tutorhub_enterprise.client.LoginFrame;
import com.mycompany.tutorhub_enterprise.client.MainDashboard;
import com.mycompany.tutorhub_enterprise.models.auth.AuthResponse;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class OAuthLoginFlow {
    // Để mock/test khi chưa có DB/Server thực, nếu cần.
    // Thực tế sẽ đọc từ ServerConfig thông qua endpoint (nhưng để an toàn, client có thể được truyền thông tin này lúc runtime)
    // Tạm thời, do yêu cầu không hardcode, ta lấy clientId qua ENV (hoặc truyền từ Server)
    // Client cần ClientID để gọi Google, không cần Secret
    private static LocalCallbackServer currentCallbackServer;

    public static void stop() {
        if (currentCallbackServer != null) {
            currentCallbackServer.stop();
            currentCallbackServer = null;
        }
    }

    public static void startGoogleLogin(LoginFrame parent) {
        if (!com.mycompany.tutorhub_enterprise.client.NetworkManager.getInstance().isConnected()) {
            JOptionPane.showMessageDialog(parent, "Đang kết nối máy chủ, vui lòng thử lại sau", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        System.out.println("[GOOGLE CLIENT] WebSocket ready = true");
        System.out.println("[GOOGLE CLIENT] Starting Google OAuth");

        stop();

        // Đọc ClientID từ biến môi trường hoặc cấu hình chung (Giả sử Client chạy trên máy người dùng, lý tưởng nhất là lấy từ Server)
        // Tuy nhiên, theo luồng OAuth native, Client ID là public và đi kèm trong App.
        String clientId = System.getProperty("tutorhub.google.client.id");
        if (clientId == null || clientId.trim().isEmpty()) {
            clientId = System.getenv("TUTORHUB_GOOGLE_CLIENT_ID");
        }
        if (clientId == null || clientId.trim().isEmpty()) {
            try (java.io.InputStream is = OAuthLoginFlow.class.getResourceAsStream("/client-public.properties")) {
                if (is != null) {
                    java.util.Properties props = new java.util.Properties();
                    props.load(is);
                    clientId = props.getProperty("tutorhub.google.client.id");
                }
            } catch (Exception e) {}
        }
        
        System.out.println("[GOOGLE CLIENT] Client ID loaded = " + (clientId != null && !clientId.trim().isEmpty()));
        if (clientId == null || clientId.trim().isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Thiếu cấu hình TUTORHUB_GOOGLE_CLIENT_ID ở Client.\nVui lòng thiết lập biến môi trường.", "Lỗi Cấu Hình", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // 1. Sinh PKCE, State, Nonce
            String codeVerifier = OAuthPKCE.generateCodeVerifier();
            String codeChallenge = OAuthPKCE.generateCodeChallenge(codeVerifier);
            String state = OAuthPKCE.generateState();
            String nonce = OAuthPKCE.generateNonce();

            // 2. Mở Local Callback Server
            currentCallbackServer = new LocalCallbackServer();
            int port = currentCallbackServer.start();
            System.out.println("[GOOGLE CLIENT] Redirect port = " + port);
            String redirectUri = "http://127.0.0.1:" + port + "/callback";

            // 3. Xây dựng URL Google OAuth
            String authUrl = "https://accounts.google.com/o/oauth2/v2/auth" +
                    "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                    "&response_type=code" +
                    "&scope=openid%20email%20profile" +
                    "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8) +
                    "&nonce=" + URLEncoder.encode(nonce, StandardCharsets.UTF_8) +
                    "&code_challenge=" + URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8) +
                    "&code_challenge_method=S256";

            // 4. Mở trình duyệt
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(authUrl));
            } else {
                currentCallbackServer.stop();
                JOptionPane.showMessageDialog(parent, "Hệ thống không hỗ trợ mở trình duyệt tự động.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 5. Chờ phản hồi (trong luồng riêng để không block UI)
            new Thread(() -> {
                OAuthCallbackResult result = currentCallbackServer.awaitCallback(120_000); // Đợi tối đa 2 phút

                SwingUtilities.invokeLater(() -> {
                    if (!result.isSuccess()) {
                        if (!"cancelled".equals(result.getError())) {
                            JOptionPane.showMessageDialog(parent, "Lỗi đăng nhập: " + result.getErrorDescription(), "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
                        }
                        return;
                    }

                    if (!state.equals(result.getState())) {
                        JOptionPane.showMessageDialog(parent, "State không hợp lệ, có thể là tấn công CSRF.", "Lỗi bảo mật", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // 6. Gửi request lên server
                    try {
                        AuthClient authClient = new AuthClient(); // Hoặc lấy từ instance dùng chung
                        AuthResponse response = authClient.socialLogin("GOOGLE", result.getCode(), codeVerifier, redirectUri, nonce);

                        if (response.isSuccess()) {
                            parent.dispose();
                            String dashboardPayload = response.getDashboardPayload();
                            int uid = 1;
                            String roleStr = "STUDENT";
                            String avatar = "";
                            String name = "SocialUser";
                            if (dashboardPayload != null && dashboardPayload.contains("|")) {
                                try {
                                    String[] parts = dashboardPayload.split("\\|");
                                    uid = Integer.parseInt(parts[1]);
                                    if (parts.length > 2) {
                                        roleStr = parts[2];
                                    }
                                    if (parts.length > 3) {
                                        avatar = parts[3];
                                    }
                                    if (parts.length > 4) {
                                        name = parts[4];
                                    }
                                } catch (Exception ignored) {}
                            }
                            new MainDashboard(uid, name, roleStr, avatar).setVisible(true);
                        } else {
                            JOptionPane.showMessageDialog(parent, "Lỗi từ máy chủ: " + response.getMessage(), "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(parent, "Lỗi khi kết nối với máy chủ: " + ex.getMessage(), "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }).start();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, "Lỗi khởi tạo đăng nhập: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
