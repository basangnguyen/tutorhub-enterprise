package com.mycompany.tutorhub_enterprise.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SocialAuthService {
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();
    private static final Gson gson = new Gson();

    private static final ConcurrentHashMap<String, FacebookPendingSession> pendingSessions = new ConcurrentHashMap<>();

    private static String generateAppSecretProof(String accessToken, String appSecret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] digest = mac.doFinal(accessToken.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static JsonObject exchangeGoogleCode(String code, String codeVerifier, String redirectUri) throws Exception {
        boolean clientIdLoaded = false;
        boolean clientSecretLoaded = false;
        String clientId = null;
        String clientSecret = null;
        
        try {
            clientId = SocialAuthConfig.getGoogleClientId();
            clientIdLoaded = true;
            clientSecret = SocialAuthConfig.getGoogleClientSecret();
            clientSecretLoaded = true;
        } catch (Exception e) {
            // Do nothing yet, handle below
        }

        System.out.println("[AUTH] SocialAuthService config check:");
        System.out.println("- provider=GOOGLE");
        System.out.println("- clientIdLoaded=" + clientIdLoaded);
        System.out.println("- clientSecretLoaded=" + clientSecretLoaded);

        if (!clientIdLoaded || !clientSecretLoaded) {
            throw new Exception("Thiếu Google OAuth config phía Server. Hãy cấu hình:\n" +
                    "-Dtutorhub.google.client.id=...\n" +
                    "-Dtutorhub.google.client.secret=...\n" +
                    "hoặc ENV TUTORHUB_GOOGLE_CLIENT_ID / TUTORHUB_GOOGLE_CLIENT_SECRET.");
        }

        String body = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&grant_type=authorization_code" +
                "&code_verifier=" + URLEncoder.encode(codeVerifier, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.err.println("[Google OAuth Error] Token Exchange Failed: " + response.body());
            throw new Exception("Lỗi khi gọi Google Token Endpoint: " + response.statusCode());
        }

        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject verifyGoogleIdToken(String idToken, String expectedNonce) throws Exception {
        // Sử dụng tokeninfo endpoint của Google để verify (đơn giản, Google tự verify signature và expiry)
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.err.println("[Google OAuth Error] ID Token Verification Failed: " + response.body());
            throw new Exception("ID Token không hợp lệ hoặc đã hết hạn.");
        }

        JsonObject payload = gson.fromJson(response.body(), JsonObject.class);

        // Verify iss (issuer)
        String iss = payload.has("iss") ? payload.get("iss").getAsString() : "";
        if (!iss.equals("https://accounts.google.com") && !iss.equals("accounts.google.com")) {
            throw new Exception("ID Token Issuer không hợp lệ.");
        }
        
        // Verify aud (audience)
        String aud = payload.has("aud") ? payload.get("aud").getAsString() : "";
        if (!aud.equals(SocialAuthConfig.getGoogleClientId())) {
            throw new Exception("ID Token Audience không khớp.");
        }

        // Verify nonce
        String nonce = payload.has("nonce") ? payload.get("nonce").getAsString() : "";
        if (!expectedNonce.equals(nonce)) {
            throw new Exception("ID Token Nonce không khớp (có dấu hiệu bị giả mạo).");
        }

        return payload;
    }

    public static AuthService.LoginSession processGoogleLogin(String code, String codeVerifier, String redirectUri, String nonce) throws Exception {
        try {
            // 1. Exchange
            JsonObject tokenResponse = exchangeGoogleCode(code, codeVerifier, redirectUri);
            if (!tokenResponse.has("id_token")) {
                throw new Exception("Không nhận được ID Token từ Google.");
            }
            String idToken = tokenResponse.get("id_token").getAsString();

            // 2. Verify
            JsonObject payload = verifyGoogleIdToken(idToken, nonce);

            String providerUserId = payload.get("sub").getAsString();
            String email = payload.has("email") ? payload.get("email").getAsString() : "";
            boolean emailVerified = payload.has("email_verified") && payload.get("email_verified").getAsBoolean();
            String name = payload.has("name") ? payload.get("name").getAsString() : "";
            String picture = payload.has("picture") ? payload.get("picture").getAsString() : "";

            if (email.isEmpty() || !emailVerified) {
                DatabaseManager.insertLoginAuditLog(-1, "GOOGLE", false, "Email_Not_Verified");
                throw new Exception("Email của bạn chưa được xác minh bởi Google.");
            }

            // 3. Find or create user
            int userId = DatabaseManager.findUserByProviderIdentity("GOOGLE", providerUserId);

            if (userId == -1) {
                // Identity chưa tồn tại, thử tìm user cũ qua email
                userId = DatabaseManager.findUserByEmail(email);
                if (userId == -1) {
                    // Cả user cũ cũng không có -> Tạo user mới (Học viên mới)
                    userId = DatabaseManager.createSocialUser(email, name, "STUDENT", picture);
                }
                
                if (userId > 0) {
                    DatabaseManager.insertAuthIdentity(userId, "GOOGLE", providerUserId, email, emailVerified, name, picture);
                }
            } else {
                DatabaseManager.updateLastSocialLogin("GOOGLE", providerUserId);
            }

            if (userId > 0) {
                DatabaseManager.insertLoginAuditLog(userId, "GOOGLE", true, "");
                return AuthService.loadLoginSession(userId, email);
            } else {
                DatabaseManager.insertLoginAuditLog(-1, "GOOGLE", false, "DB_Create_Failed");
                throw new Exception("Lỗi hệ thống khi tạo tài khoản.");
            }

        } catch (Exception e) {
            System.err.println("[SocialAuthService] Google Login Error: " + e.getMessage());
            throw e;
        }
    }

    public static java.util.Map<String, Object> startFacebookLoginSession() throws Exception {
        if (!SocialAuthConfig.isFacebookConfigured()) {
            throw new Exception("Thiếu Facebook OAuth config phía Server.");
        }
        
        String sessionId = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString(); // Anti CSRF
        FacebookPendingSession session = new FacebookPendingSession(sessionId, state);
        pendingSessions.put(sessionId, session);

        String appId = SocialAuthConfig.getFacebookAppId();
        String redirectUri = SocialAuthConfig.getFacebookRedirectUri();

        String url = "https://www.facebook.com/v19.0/dialog/oauth" +
                "?client_id=" + URLEncoder.encode(appId, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8) +
                "&scope=public_profile";

        System.out.println("[FACEBOOK] AUTH_FACEBOOK_START received");
        System.out.println("[FACEBOOK] App ID loaded: " + (appId != null && !appId.isEmpty()));
        System.out.println("[FACEBOOK] App Secret loaded: " + (SocialAuthConfig.getFacebookAppSecret() != null && !SocialAuthConfig.getFacebookAppSecret().isEmpty()));
        System.out.println("[FACEBOOK] Redirect URI loaded: " + (redirectUri != null && !redirectUri.isEmpty()));
        System.out.println("[FACEBOOK] Pending session created: true");
        System.out.println("[FACEBOOK] Authorization URL created: true");

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("sessionId", sessionId);
        data.put("authorizationUrl", url);
        return data;
    }

    public static FacebookPendingSession pollFacebookLogin(String sessionId) {
        FacebookPendingSession session = pendingSessions.get(sessionId);
        if (session != null) {
            if (session.isExpired()) {
                pendingSessions.remove(sessionId);
                session.setStatus(FacebookPendingSession.Status.FAILED);
                session.setErrorMessage("Phiên đăng nhập đã hết hạn.");
            } else if (session.getStatus() == FacebookPendingSession.Status.SUCCESS || session.getStatus() == FacebookPendingSession.Status.FAILED) {
                pendingSessions.remove(sessionId);
            }
        }
        return session;
    }

    public static com.mycompany.tutorhub_enterprise.models.auth.AuthResponse handleWorkerResult(String state, String providerUserId, String name, String email, String pictureUrl, boolean success, String errorMessage) throws Exception {
        System.out.println("[AUTH] Xử lý Facebook Worker Result với state: " + state);

        FacebookPendingSession targetSession = null;
        for (FacebookPendingSession session : pendingSessions.values()) {
            if (state.equals(session.getState())) {
                targetSession = session;
                break;
            }
        }

        if (targetSession == null) {
            throw new Exception("Không tìm thấy phiên đăng nhập. State không hợp lệ hoặc đã hết hạn.");
        }

        if (targetSession.isExpired()) {
            targetSession.setStatus(FacebookPendingSession.Status.FAILED);
            targetSession.setErrorMessage("Phiên đăng nhập đã hết hạn.");
            // pendingSessions.remove(targetSession.getSessionId()); // Don't remove immediately to let poll get the FAILED state
            throw new Exception("Phiên đăng nhập đã hết hạn.");
        }

        if (!success) {
            targetSession.setStatus(FacebookPendingSession.Status.FAILED);
            targetSession.setErrorMessage(errorMessage != null ? errorMessage : "Không thể kết nối tới Facebook Graph API từ Worker. Vui lòng thử lại sau.");
            // pendingSessions.remove(targetSession.getSessionId());
            return null;
        }

        try {
            if (email == null || email.trim().isEmpty()) {
                email = "fb_" + providerUserId + "@facebook.local";
            }
            if (name == null) name = "";
            if (pictureUrl == null) pictureUrl = "";

            // Find or create user
            System.out.println("[FACEBOOK_OAUTH] DB link started for user: " + providerUserId);
            System.out.println("[FACEBOOK_OAUTH] findIdentity started");
            int userId = DatabaseManager.findUserByProviderIdentity("FACEBOOK", providerUserId);
            System.out.println("[FACEBOOK_OAUTH] findIdentity success, userId=" + userId);

            if (userId == -1) {
                // If it's a real email, we can try linking by email. If it's placeholder, no need.
                if (!email.contains("@facebook.local") && !email.isEmpty()) {
                    System.out.println("[FACEBOOK_OAUTH] findUserByEmail started");
                    userId = DatabaseManager.findUserByEmail(email);
                    System.out.println("[FACEBOOK_OAUTH] findUserByEmail success, userId=" + userId);
                }
                
                if (userId == -1) {
                    System.out.println("[FACEBOOK_OAUTH] createUser started");
                    userId = DatabaseManager.createSocialUser(email, name, "STUDENT", pictureUrl);
                    System.out.println("[FACEBOOK_OAUTH] createUser success, new userId=" + userId);
                }
                
                if (userId > 0) {
                    System.out.println("[FACEBOOK_OAUTH] linkIdentity started");
                    // Always treat email from Facebook as verified if it exists because Facebook verifies it
                    boolean emailVerified = !email.contains("@facebook.local"); 
                    DatabaseManager.insertAuthIdentity(userId, "FACEBOOK", providerUserId, email, emailVerified, name, pictureUrl);
                    System.out.println("[FACEBOOK_OAUTH] linkIdentity success");
                }
            } else {
                System.out.println("[FACEBOOK_OAUTH] updateLastSocialLogin started");
                DatabaseManager.updateLastSocialLogin("FACEBOOK", providerUserId);
                System.out.println("[FACEBOOK_OAUTH] updateLastSocialLogin success");
            }

            if (userId > 0) {
                System.out.println("[FACEBOOK_OAUTH] auditLog started");
                DatabaseManager.insertLoginAuditLog(userId, "FACEBOOK", true, "");
                System.out.println("[FACEBOOK_OAUTH] auditLog success");
                
                System.out.println("[FACEBOOK_OAUTH] loadLoginSession started");
                AuthService.LoginSession sessionData = AuthService.loadLoginSession(userId, email);
                System.out.println("[FACEBOOK_OAUTH] loadLoginSession success");
                
                String dashboardPayload = "DASHBOARD_GO|" + sessionData.getUserId() + "|" + sessionData.getRole() + "|" + sessionData.getAvatarBase64() + "|" + sessionData.getIdentity();
                
                com.mycompany.tutorhub_enterprise.models.auth.SessionInfo sessionInfo = null;
                try {
                    sessionInfo = SessionService.createSession(userId, "Unknown Device", "Unknown", "1.0");
                    if (sessionInfo == null) {
                        System.out.println("[SESSION] social session creation failed, continuing with dashboardPayload compatibility mode.");
                    }
                } catch (Exception se) {
                    System.out.println("[SESSION] social session creation failed, continuing with dashboardPayload compatibility mode.");
                }

                com.mycompany.tutorhub_enterprise.models.auth.AuthResponse authRes = 
                    com.mycompany.tutorhub_enterprise.models.auth.AuthResponse.loginWithSession(
                        "unknown",
                        "Đăng nhập Facebook thành công",
                        dashboardPayload,
                        sessionInfo
                    );
                    
                targetSession.setPayload(authRes);
                targetSession.setStatus(FacebookPendingSession.Status.SUCCESS);
                System.out.println("[FACEBOOK_OAUTH] DB link success for user: " + userId);
                System.out.println("[FACEBOOK_OAUTH] Pending session marked SUCCESS");
                // pendingSessions.remove(targetSession.getSessionId()); // Keep it so poll can pick up SUCCESS
                return authRes;
            } else {
                DatabaseManager.insertLoginAuditLog(-1, "FACEBOOK", false, "DB_Create_Failed");
                targetSession.setStatus(FacebookPendingSession.Status.FAILED);
                targetSession.setErrorMessage("Lỗi tạo hoặc liên kết tài khoản từ Facebook.");
                // pendingSessions.remove(targetSession.getSessionId());
                throw new Exception("Lỗi hệ thống khi tạo tài khoản.");
            }
        } catch (Exception e) {
            targetSession.setStatus(FacebookPendingSession.Status.FAILED);
            targetSession.setErrorMessage("Không thể hoàn tất đăng nhập Facebook: " + e.getMessage());
            // pendingSessions.remove(targetSession.getSessionId());
            System.err.println("[SocialAuthService] Facebook Login Error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
