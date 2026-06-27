package com.mycompany.tutorhub_enterprise.server;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;

public final class AuthService {

    private static final String PASSWORD_LOGIN_FAILED_MESSAGE = "Email hoac mat khau khong dung. Vui long thu lai.";
    private static final String PASSWORD_LOGIN_BLOCKED_MESSAGE = "Ban da thu qua nhieu lan. Vui long thu lai sau it phut.";

    private AuthService() {
    }

    public static LoginSession authenticateWithPassword(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (ServerConfig.isBlank(normalizedEmail) || ServerConfig.isBlank(password)) {
            return LoginSession.failed(PASSWORD_LOGIN_FAILED_MESSAGE);
        }

        String rateLimitIdentifier = AuthRateLimitService.normalizeIdentifier(normalizedEmail);
        if (!AuthRateLimitService.checkAllowed(rateLimitIdentifier)) {
            return LoginSession.failed(PASSWORD_LOGIN_BLOCKED_MESSAGE);
        }

        int userId = DatabaseManager.authenticateByEmail(normalizedEmail, password);
        if (userId == DatabaseManager.AUTH_DATABASE_ERROR) {
            return LoginSession.failed("Khong the ket noi database. Kiem tra cau hinh server.");
        }
        if (userId == DatabaseManager.AUTH_INVALID_PASSWORD_HASH) {
            return recordPasswordLoginFailure(rateLimitIdentifier);
        }
        if (userId == DatabaseManager.AUTH_FAILED) {
            return recordPasswordLoginFailure(rateLimitIdentifier);
        }

        AuthRateLimitService.recordSuccess(rateLimitIdentifier);
        LoginSession session = loadLoginSession(userId, normalizedEmail);
        
        // Generate Server Session (Phase S2)
        com.mycompany.tutorhub_enterprise.models.auth.SessionInfo sessionInfo = SessionService.createSession(userId, "Unknown Device", "Unknown", "1.0");
        if (sessionInfo != null) {
            session = LoginSession.successWithSession(userId, normalizedEmail, session.getRole(), session.getAvatarBase64(), sessionInfo);
        }
        
        return session;
    }

    private static LoginSession recordPasswordLoginFailure(String rateLimitIdentifier) {
        AuthRateLimitService.recordFailure(rateLimitIdentifier);
        if (!AuthRateLimitService.checkAllowed(rateLimitIdentifier)) {
            return LoginSession.failed(PASSWORD_LOGIN_BLOCKED_MESSAGE);
        }
        return LoginSession.failed(PASSWORD_LOGIN_FAILED_MESSAGE);
    }

    public static LoginSession authenticateWithSocialProvider(String provider, String code, String codeVerifier, String redirectUri, String nonce) {
        if ("GOOGLE".equalsIgnoreCase(provider)) {
            try {
                LoginSession session = SocialAuthService.processGoogleLogin(code, codeVerifier, redirectUri, nonce);
                if (session != null && session.isSuccess()) {
                    try {
                        com.mycompany.tutorhub_enterprise.models.auth.SessionInfo sessionInfo = SessionService.createSession(session.getUserId(), "Unknown Device", "Unknown", "1.0");
                        if (sessionInfo != null) {
                            session = LoginSession.successWithSession(session.getUserId(), session.getIdentity(), session.getRole(), session.getAvatarBase64(), sessionInfo);
                        } else {
                            System.out.println("[SESSION] social session creation failed, continuing with dashboardPayload compatibility mode.");
                        }
                    } catch (Exception se) {
                        System.out.println("[SESSION] social session creation failed, continuing with dashboardPayload compatibility mode.");
                    }
                }
                return session;
            } catch (Exception e) {
                return LoginSession.failed(e.getMessage());
            }
        }
        return LoginSession.failed("Provider khong duoc ho tro: " + provider);
    }

    public static AuthResult requestRegistrationOtp(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (!isValidEmail(normalizedEmail)) {
            return AuthResult.fail("Email khong hop le.");
        }
        if (DatabaseManager.isEmailExists(normalizedEmail)) {
            return AuthResult.fail("Email nay da duoc dang ky trong he thong.");
        }

        return issueAndSendEmailOtp(normalizedEmail, OtpService.PURPOSE_REGISTER, "Ma OTP da duoc gui den hop thu cua ban.");
    }

    public static AuthResult verifyAndRegister(String email, String otp, String rawPassword, String fullName) {
        String normalizedEmail = normalizeEmail(email);
        if (!isValidEmail(normalizedEmail) || ServerConfig.isBlank(otp)
                || ServerConfig.isBlank(rawPassword) || ServerConfig.isBlank(fullName)) {
            return AuthResult.fail("Thong tin dang ky khong hop le.");
        }

        PasswordPolicyService.PasswordPolicyResult policy = PasswordPolicyService.validate(rawPassword, normalizedEmail);
        if (!policy.isValid()) {
            return AuthResult.fail(policy.getPublicMessage());
        }

        OtpService.VerifyResult otpResult = OtpService.verify(normalizedEmail, OtpService.PURPOSE_REGISTER, otp);
        if (!otpResult.isSuccess()) {
            return AuthResult.fail("Ma OTP khong chinh xac hoac da het han.");
        }

        boolean registered = DatabaseManager.registerUser(normalizedEmail, rawPassword, fullName.trim(), "TUTOR");
        return registered
                ? AuthResult.ok("Dang ky thanh cong. Hay quay lai dang nhap.")
                : AuthResult.fail("Khong the tao tai khoan luc nay.");
    }

    public static AuthResult requestPasswordResetOtp(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (!isValidEmail(normalizedEmail)) {
            return AuthResult.fail("Email khong hop le.");
        }
        
        String genericMessage = "Neu tai khoan ton tai, ma xac thuc se duoc gui den email cua ban.";

        if (!DatabaseManager.isEmailExists(normalizedEmail)) {
            System.out.println("[AUTH_RESET] request processed with generic response");
            return AuthResult.ok(genericMessage);
        }

        System.out.println("[AUTH_RESET] password reset requested");
        AuthResult result = issueAndSendEmailOtp(normalizedEmail, OtpService.PURPOSE_RESET_PASSWORD, genericMessage);
        
        if (!result.isSuccess()) {
            System.out.println("[AUTH_RESET] internal issue/send failed: " + result.getMessage());
        }
        return AuthResult.ok(genericMessage);
    }

    public static AuthResult verifyAndResetPassword(String email, String otp, String newPassword) {
        String normalizedEmail = normalizeEmail(email);
        if (!isValidEmail(normalizedEmail) || ServerConfig.isBlank(otp) || ServerConfig.isBlank(newPassword)) {
            return AuthResult.fail("Thong tin khoi phuc mat khau khong hop le.");
        }

        PasswordPolicyService.PasswordPolicyResult policy = PasswordPolicyService.validate(newPassword, normalizedEmail);
        if (!policy.isValid()) {
            return AuthResult.fail(policy.getPublicMessage());
        }

        OtpService.VerifyResult otpResult = OtpService.verify(normalizedEmail, OtpService.PURPOSE_RESET_PASSWORD, otp);
        if (!otpResult.isSuccess()) {
            return AuthResult.fail("Ma OTP khong chinh xac hoac da het han.");
        }

        boolean reset = DatabaseManager.resetPassword(normalizedEmail, newPassword);
        return reset
                ? AuthResult.ok("Mat khau da duoc cap nhat.")
                : AuthResult.fail("Khong the cap nhat mat khau luc nay.");
    }

    public static AuthResult requestSmsLoginOtp(String phone) {
        String normalizedPhone = DatabaseManager.normalizePhone(phone);
        if (!isValidNormalizedPhone(normalizedPhone)) {
            return AuthResult.fail("So dien thoai khong hop le.");
        }

        int userId = DatabaseManager.findVerifiedUserIdByPhone(normalizedPhone);
        if (userId == -1) {
            return AuthResult.fail("So dien thoai chua duoc xac minh voi tai khoan TutorHub.");
        }

        boolean sent = SmsService.sendOtpSms(normalizedPhone);
        return sent
                ? AuthResult.ok("Ma OTP da duoc gui den so dien thoai.")
                : AuthResult.fail("Khong the gui SMS OTP. Vui long thu lai sau.");
    }

    public static LoginSession verifySmsLogin(String phone, String otp) {
        String normalizedPhone = DatabaseManager.normalizePhone(phone);
        if (!isValidNormalizedPhone(normalizedPhone) || ServerConfig.isBlank(otp)) {
            return LoginSession.failed("Du lieu SMS login khong hop le.");
        }

        int userId = DatabaseManager.findVerifiedUserIdByPhone(normalizedPhone);
        if (userId == -1) {
            return LoginSession.failed("So dien thoai chua duoc xac minh voi tai khoan TutorHub.");
        }

        if (!SmsService.verifyOtp(normalizedPhone, otp)) {
            return LoginSession.failed("Ma OTP khong chinh xac hoac da het han.");
        }

        return loadLoginSession(userId, normalizedPhone);
    }

    public static AuthResult requestPhoneVerificationOtp(int userId, String phone) {
        if (userId == -1) {
            return AuthResult.fail("Ban can dang nhap truoc khi xac minh so dien thoai.");
        }

        String normalizedPhone = DatabaseManager.normalizePhone(phone);
        if (!isValidNormalizedPhone(normalizedPhone)) {
            return AuthResult.fail("So dien thoai khong hop le.");
        }
        if (!DatabaseManager.isPhoneLinkedToUser(userId, normalizedPhone)) {
            return AuthResult.fail("Vui long luu so dien thoai vao ho so truoc khi xac minh.");
        }

        boolean sent = SmsService.sendOtpSms(normalizedPhone, OtpService.PURPOSE_VERIFY_PHONE);
        return sent
                ? AuthResult.ok("Ma OTP xac minh da duoc gui.")
                : AuthResult.fail("Khong the gui SMS OTP. Vui long thu lai sau.");
    }

    public static AuthResult verifyPhoneOtp(int userId, String phone, String otp) {
        if (userId == -1) {
            return AuthResult.fail("Ban can dang nhap truoc khi xac minh so dien thoai.");
        }

        String normalizedPhone = DatabaseManager.normalizePhone(phone);
        if (!DatabaseManager.isPhoneLinkedToUser(userId, normalizedPhone)) {
            return AuthResult.fail("So dien thoai khong khop voi ho so hien tai.");
        }

        boolean verified = SmsService.verifyOtp(normalizedPhone, OtpService.PURPOSE_VERIFY_PHONE, otp)
                && DatabaseManager.markPhoneVerified(userId, normalizedPhone);
        return verified
                ? AuthResult.ok("So dien thoai da duoc xac minh.")
                : AuthResult.fail("Ma OTP khong chinh xac hoac da het han.");
    }

    private static AuthResult issueAndSendEmailOtp(String email, String purpose, String successMessage) {
        OtpService.IssueResult issue = OtpService.issue(email, purpose);
        if (!issue.isIssued()) {
            return AuthResult.fail("Vui long doi " + issue.getRetryAfterSeconds() + " giay truoc khi gui lai OTP.");
        }

        boolean sent = EmailService.sendOTP(email, issue.getCode());
        if (!sent) {
            OtpService.clear(email, purpose);
            return AuthResult.fail("Hien chua the gui ma xac thuc. Vui long thu lai sau.");
        }

        return AuthResult.ok(successMessage);
    }

    static LoginSession loadLoginSession(int userId, String identity) {
        String role = "TUTOR";
        String avatarBase64 = "NO_AVATAR";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement("SELECT role, avatar_url, full_name FROM users WHERE id = ?")) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                String fetchedRole = rs.getString("role");
                if (!ServerConfig.isBlank(fetchedRole)) {
                    role = fetchedRole.trim().toUpperCase();
                }

                String avatarUrl = rs.getString("avatar_url");
                if (!ServerConfig.isBlank(avatarUrl)) {
                    if (avatarUrl.startsWith("http")) {
                        avatarBase64 = avatarUrl;
                    } else {
                        File avatar = new File(avatarUrl);
                        if (avatar.exists()) {
                            avatarBase64 = Base64.getEncoder().encodeToString(Files.readAllBytes(avatar.toPath()));
                        }
                    }
                }

                String fetchedName = rs.getString("full_name");
                if (fetchedName != null && !fetchedName.trim().isEmpty()) {
                    identity = fetchedName.trim();
                }
            }
        } catch (Exception ex) {
            System.err.println("[AUTH] Failed to load login session: " + ex.getMessage());
        }

        return LoginSession.success(userId, identity, role, avatarBase64);
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim();
    }

    private static boolean isValidEmail(String email) {
        return !ServerConfig.isBlank(email) && email.contains("@") && email.contains(".");
    }

    private static boolean isValidNormalizedPhone(String phone) {
        if (ServerConfig.isBlank(phone)) {
            return false;
        }
        int digits = phone.replaceAll("\\D", "").length();
        return digits >= 9 && digits <= 15;
    }

    public static final class AuthResult {
        private final boolean success;
        private final String message;

        private AuthResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static AuthResult ok(String message) {
            return new AuthResult(true, message);
        }

        public static AuthResult fail(String message) {
            return new AuthResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class LoginSession {
        private final boolean success;
        private final String message;
        private final int userId;
        private final String identity;
        private final String role;
        private final String avatarBase64;
        private final com.mycompany.tutorhub_enterprise.models.auth.SessionInfo sessionInfo;

        private LoginSession(boolean success, String message, int userId, String identity, String role, String avatarBase64, com.mycompany.tutorhub_enterprise.models.auth.SessionInfo sessionInfo) {
            this.success = success;
            this.message = message;
            this.userId = userId;
            this.identity = identity;
            this.role = role;
            this.avatarBase64 = avatarBase64;
            this.sessionInfo = sessionInfo;
        }

        public static LoginSession success(int userId, String identity, String role, String avatarBase64) {
            return new LoginSession(true, "Dang nhap thanh cong.", userId, identity, role, avatarBase64, null);
        }
        
        public static LoginSession successWithSession(int userId, String identity, String role, String avatarBase64, com.mycompany.tutorhub_enterprise.models.auth.SessionInfo sessionInfo) {
            return new LoginSession(true, "Dang nhap thanh cong.", userId, identity, role, avatarBase64, sessionInfo);
        }

        public static LoginSession failed(String message) {
            return new LoginSession(false, message, -1, "", "", "", null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getUserId() {
            return userId;
        }

        public String getIdentity() {
            return identity;
        }

        public String getRole() {
            return role;
        }

        public String getAvatarBase64() {
            return avatarBase64;
        }
        
        public com.mycompany.tutorhub_enterprise.models.auth.SessionInfo getSessionInfo() {
            return sessionInfo;
        }
    }
}
