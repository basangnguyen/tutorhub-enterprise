package com.mycompany.tutorhub_enterprise.server;

public class SocialAuthConfig {
    public static String getGoogleClientId() {
        String clientId = ServerConfig.get("TUTORHUB_GOOGLE_CLIENT_ID", "tutorhub.google.client.id", "google.client.id", null);
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalStateException("Thiếu cấu hình TUTORHUB_GOOGLE_CLIENT_ID. Hãy set environment variable hoặc thêm vào file properties.");
        }
        return clientId;
    }

    public static String getGoogleClientSecret() {
        String secret = ServerConfig.get("TUTORHUB_GOOGLE_CLIENT_SECRET", "tutorhub.google.client.secret", "google.client.secret", null);
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("Thiếu cấu hình TUTORHUB_GOOGLE_CLIENT_SECRET. Hãy set environment variable hoặc thêm vào file properties.");
        }
        return secret;
    }

    public static String getFacebookAppId() {
        String appId = ServerConfig.get("TUTORHUB_FACEBOOK_APP_ID", "tutorhub.facebook.app.id", null);
        if (appId == null || appId.trim().isEmpty()) {
            throw new IllegalStateException("Thiếu cấu hình TUTORHUB_FACEBOOK_APP_ID. Hãy set environment variable hoặc thêm vào file properties.");
        }
        return appId;
    }

    public static String getFacebookAppSecret() {
        String secret = ServerConfig.get("TUTORHUB_FACEBOOK_APP_SECRET", "tutorhub.facebook.app.secret", null);
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("Thiếu cấu hình TUTORHUB_FACEBOOK_APP_SECRET. Hãy set environment variable hoặc thêm vào file properties.");
        }
        return secret;
    }

    public static String getFacebookRedirectUri() {
        return ServerConfig.get("TUTORHUB_FACEBOOK_REDIRECT_URI", "tutorhub.facebook.redirect.uri", "facebook.redirect.uri", "http://127.0.0.1:7861/oauth/facebook/callback");
    }

    public static int getFacebookOAuthHttpPort() {
        String portStr = ServerConfig.get("TUTORHUB_FACEBOOK_OAUTH_HTTP_PORT", "tutorhub.facebook.oauth.http.port", "facebook.oauth.http.port", "7861");
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            return 7861;
        }
    }

    public static String getFacebookWorkerSharedSecret() {
        String secret = ServerConfig.get("TUTORHUB_FACEBOOK_WORKER_SHARED_SECRET", "tutorhub.facebook.worker.shared.secret", null);
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("Thiếu cấu hình TUTORHUB_FACEBOOK_WORKER_SHARED_SECRET.");
        }
        return secret;
    }

    public static boolean isFacebookConfigured() {
        String appId = ServerConfig.get("TUTORHUB_FACEBOOK_APP_ID", "tutorhub.facebook.app.id", null);
        String secret = ServerConfig.get("TUTORHUB_FACEBOOK_APP_SECRET", "tutorhub.facebook.app.secret", null);
        return appId != null && !appId.trim().isEmpty() && secret != null && !secret.trim().isEmpty();
    }
}
