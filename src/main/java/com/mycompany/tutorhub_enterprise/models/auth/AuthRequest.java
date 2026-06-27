package com.mycompany.tutorhub_enterprise.models.auth;

import java.io.Serializable;
import java.util.UUID;

public final class AuthRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;
    private String email;
    private String password;
    private String otp;
    private String fullName;
    private String phone;
    private String provider;
    private String authorizationCode;
    private String codeVerifier;
    private String redirectUri;
    private String nonce;
    private String sessionId;
    private String accessToken;

    public AuthRequest() {}

    private AuthRequest(String requestId, String email, String password, String otp, String fullName, String phone) {
        this(requestId, email, password, otp, fullName, phone, "", "", "", "", "", "", "");
    }

    private AuthRequest(String requestId, String email, String password, String otp, String fullName, String phone, String provider, String authorizationCode, String codeVerifier, String redirectUri, String nonce, String sessionId, String accessToken) {
        this.requestId = requireRequestId(requestId);
        this.email = safe(email);
        this.password = safe(password);
        this.otp = safe(otp);
        this.fullName = safe(fullName);
        this.phone = safe(phone);
        this.provider = safe(provider);
        this.authorizationCode = safe(authorizationCode);
        this.codeVerifier = safe(codeVerifier);
        this.redirectUri = safe(redirectUri);
        this.nonce = safe(nonce);
        this.sessionId = safe(sessionId);
        this.accessToken = safe(accessToken);
    }

    public static AuthRequest login(String email, String password) {
        return new AuthRequest(newRequestId(), email, password, "", "", "");
    }

    public static AuthRequest registrationOtp(String email) {
        return new AuthRequest(newRequestId(), email, "", "", "", "");
    }

    public static AuthRequest register(String email, String otp, String password, String fullName) {
        return new AuthRequest(newRequestId(), email, password, otp, fullName, "");
    }

    public static AuthRequest passwordResetOtp(String email) {
        return new AuthRequest(newRequestId(), email, "", "", "", "");
    }

    public static AuthRequest resetPassword(String email, String otp, String newPassword) {
        return new AuthRequest(newRequestId(), email, newPassword, otp, "", "");
    }

    public static AuthRequest smsLoginOtp(String phone) {
        return new AuthRequest(newRequestId(), "", "", "", "", phone);
    }

    public static AuthRequest smsLogin(String phone, String otp) {
        return new AuthRequest(newRequestId(), "", "", otp, "", phone);
    }

    public static AuthRequest phoneVerificationOtp(String phone) {
        return new AuthRequest(newRequestId(), "", "", "", "", phone);
    }

    public static AuthRequest verifyPhone(String phone, String otp) {
        return new AuthRequest(newRequestId(), "", "", otp, "", phone);
    }

    public static AuthRequest socialLogin(String provider, String authorizationCode, String codeVerifier, String redirectUri, String nonce) {
        return new AuthRequest(newRequestId(), "", "", "", "", "", provider, authorizationCode, codeVerifier, redirectUri, nonce, "", "");
    }

    public static AuthRequest facebookStart() {
        return new AuthRequest(newRequestId(), "", "", "", "", "", "FACEBOOK", "", "", "", "", "", "");
    }

    public static AuthRequest facebookPoll(String sessionId) {
        return new AuthRequest(newRequestId(), "", "", "", "", "", "FACEBOOK", "", "", "", "", sessionId, "");
    }

    public static AuthRequest logout(String sessionId, String accessToken) {
        return new AuthRequest(newRequestId(), "", "", "", "", "", "", "", "", "", "", sessionId, accessToken);
    }

    public String getRequestId() {
        return requestId;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getOtp() {
        return otp;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getProvider() {
        return provider;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getNonce() {
        return nonce;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    private static String newRequestId() {
        return "auth-" + UUID.randomUUID();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String requireRequestId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return newRequestId();
        }
        return value.trim();
    }
}
