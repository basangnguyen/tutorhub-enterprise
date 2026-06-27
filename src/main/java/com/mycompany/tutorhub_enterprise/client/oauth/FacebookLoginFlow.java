package com.mycompany.tutorhub_enterprise.client.oauth;

import com.mycompany.tutorhub_enterprise.client.AuthClient;
import com.mycompany.tutorhub_enterprise.client.LoginFrame;
import com.mycompany.tutorhub_enterprise.client.MainDashboard;
import com.mycompany.tutorhub_enterprise.models.auth.AuthResponse;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

public class FacebookLoginFlow {

    private static boolean facebookLoginInProgress = false;
    private static Timer currentPollTimer;
    private static Timer currentTimeoutTimer;

    public static void stop() {
        if (currentPollTimer != null) {
            currentPollTimer.stop();
            currentPollTimer = null;
        }
        if (currentTimeoutTimer != null) {
            currentTimeoutTimer.stop();
            currentTimeoutTimer = null;
        }
        facebookLoginInProgress = false;
    }

    public static void startFacebookLogin(LoginFrame parent) {
        if (facebookLoginInProgress) {
            return;
        }
        stop();
        facebookLoginInProgress = true;

        try {
            AuthClient authClient = new AuthClient();
            
            // 1. Yêu cầu server tạo phiên Facebook Login
            AuthResponse startRes = authClient.facebookStart();
            if (!startRes.isSuccess()) {
                JOptionPane.showMessageDialog(parent, "Không thể bắt đầu phiên đăng nhập: " + startRes.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Dữ liệu trả về có dạng JSON trong dashboardPayload
            String payload = startRes.getDashboardPayload();
            System.out.println("[FACEBOOK CLIENT] Start response success=" + startRes.isSuccess());
            System.out.println("[FACEBOOK CLIENT] Data class = String (JSON payload)");

            if (payload == null || payload.trim().isEmpty()) {
                JOptionPane.showMessageDialog(parent, "Server không trả sessionId/authorizationUrl cho Facebook Login.\nVui lòng kiểm tra log AUTH_FACEBOOK_START phía server.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                facebookLoginInProgress = false;
                return;
            }

            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.util.Map<String, String> data = gson.fromJson(payload, java.util.Map.class);
            String sessionId = data != null ? data.get("sessionId") : null;
            String authUrl = data != null ? data.get("authorizationUrl") : null;

            System.out.println("[FACEBOOK CLIENT] sessionId loaded=" + (sessionId != null));
            System.out.println("[FACEBOOK CLIENT] authorizationUrl loaded=" + (authUrl != null));

            if (sessionId == null || authUrl == null) {
                JOptionPane.showMessageDialog(parent, "Server không trả sessionId/authorizationUrl cho Facebook Login.\nVui lòng kiểm tra log AUTH_FACEBOOK_START phía server.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                facebookLoginInProgress = false;
                return;
            }

            // 2. Mở trình duyệt
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(authUrl));
            } else {
                JOptionPane.showMessageDialog(parent, "Hệ thống không hỗ trợ mở trình duyệt tự động.\nCopy link sau: " + authUrl, "Lỗi", JOptionPane.ERROR_MESSAGE);
                facebookLoginInProgress = false;
                return;
            }

            // 3. Khởi tạo Polling Timer
            currentPollTimer = new Timer(2000, null); // poll mỗi 2 giây
            currentPollTimer.addActionListener(e -> {
                try {
                    AuthResponse pollRes = authClient.facebookPoll(sessionId);
                    
                    if (pollRes.isSuccess()) {
                        String msg = pollRes.getMessage();
                        if ("PENDING".equals(msg)) {
                            // Vẫn đang chờ người dùng thao tác trên trình duyệt
                        } else {
                            // Thành công
                            stop();
                            String dashboardPayload = pollRes.getDashboardPayload();
                            int uid = 1;
                            String roleStr = "STUDENT";
                            String avatar = "";
                            String name = "SocialUser";
                            if (dashboardPayload != null && dashboardPayload.contains("|")) {
                                try {
                                    String[] dParts = dashboardPayload.split("\\|");
                                    if (dParts.length > 1) {
                                        uid = Integer.parseInt(dParts[1]);
                                    }
                                    if (dParts.length > 2) {
                                        roleStr = dParts[2];
                                    }
                                    if (dParts.length > 3) {
                                        avatar = dParts[3];
                                    }
                                    if (dParts.length > 4) {
                                        name = dParts[4];
                                    }
                                } catch (Exception ignored) {}
                            }
                            parent.dispose();
                            new MainDashboard(uid, name, roleStr, avatar).setVisible(true);
                        }
                    } else {
                        // Thất bại
                        stop();
                        JOptionPane.showMessageDialog(parent, "Đăng nhập thất bại: " + pollRes.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    stop();
                    JOptionPane.showMessageDialog(parent, "Lỗi khi polling máy chủ: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            });
            currentPollTimer.start();
            
            // Timeout sau 5 phút nếu vẫn chưa login
            currentTimeoutTimer = new Timer(5 * 60 * 1000, e -> {
                if (currentPollTimer != null && currentPollTimer.isRunning()) {
                    stop();
                    JOptionPane.showMessageDialog(parent, "Hết thời gian chờ đăng nhập Facebook.", "Lỗi Timeout", JOptionPane.ERROR_MESSAGE);
                }
            });
            currentTimeoutTimer.start();

        } catch (Exception ex) {
            facebookLoginInProgress = false;
            JOptionPane.showMessageDialog(parent, "Lỗi hệ thống khi gọi Facebook Login: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
