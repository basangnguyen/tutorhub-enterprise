package com.mycompany.tutorhub_enterprise.models.auth;

import java.io.Serializable;
import java.util.UUID;

public final class AuthRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String requestId;
    private final String email;
    private final String password;
    private final String otp;
    private final String fullName;
    private final String phone;

    private AuthRequest(String requestId, String email, String password, String otp, String fullName, String phone) {
        this.requestId = requireRequestId(requestId);
        this.email = safe(email);
        this.password = safe(password);
        this.otp = safe(otp);
        this.fullName = safe(fullName);
        this.phone = safe(phone);
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
