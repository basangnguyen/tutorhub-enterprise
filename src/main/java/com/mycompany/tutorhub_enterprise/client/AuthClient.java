package com.mycompany.tutorhub_enterprise.client;

import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.models.auth.AuthProtocol;
import com.mycompany.tutorhub_enterprise.models.auth.AuthRequest;
import com.mycompany.tutorhub_enterprise.models.auth.AuthResponse;

public final class AuthClient {
    private static final int DEFAULT_TIMEOUT_MS = 30_000;

    private final NetworkManager network;

    public AuthClient() {
        this(NetworkManager.getInstance());
    }

    AuthClient(NetworkManager network) {
        this.network = network;
    }

    public AuthResponse login(String email, String password) throws Exception {
        return send(AuthProtocol.LOGIN, AuthRequest.login(email, password));
    }

    public AuthResponse requestRegistrationOtp(String email) throws Exception {
        return send(AuthProtocol.REQUEST_REGISTRATION_OTP, AuthRequest.registrationOtp(email));
    }

    public AuthResponse register(String email, String otp, String password, String fullName) throws Exception {
        return send(AuthProtocol.VERIFY_AND_REGISTER, AuthRequest.register(email, otp, password, fullName));
    }

    public AuthResponse requestPasswordResetOtp(String email) throws Exception {
        return send(AuthProtocol.REQUEST_PASSWORD_RESET_OTP, AuthRequest.passwordResetOtp(email));
    }

    public AuthResponse resetPassword(String email, String otp, String newPassword) throws Exception {
        return send(AuthProtocol.VERIFY_AND_RESET_PASSWORD, AuthRequest.resetPassword(email, otp, newPassword));
    }

    public AuthResponse requestSmsLoginOtp(String phone) throws Exception {
        return send(AuthProtocol.REQUEST_SMS_LOGIN_OTP, AuthRequest.smsLoginOtp(phone));
    }

    public AuthResponse verifySmsLogin(String phone, String otp) throws Exception {
        return send(AuthProtocol.VERIFY_SMS_LOGIN, AuthRequest.smsLogin(phone, otp));
    }

    public AuthResponse requestPhoneVerificationOtp(String phone) throws Exception {
        return send(AuthProtocol.REQUEST_PHONE_VERIFICATION_OTP, AuthRequest.phoneVerificationOtp(phone));
    }

    public AuthResponse verifyPhone(String phone, String otp) throws Exception {
        return send(AuthProtocol.VERIFY_PHONE_OTP, AuthRequest.verifyPhone(phone, otp));
    }

    public AuthResponse socialLogin(String provider, String authorizationCode, String codeVerifier, String redirectUri, String nonce) throws Exception {
        return send(AuthProtocol.AUTH_SOCIAL_LOGIN, AuthRequest.socialLogin(provider, authorizationCode, codeVerifier, redirectUri, nonce));
    }

    public AuthResponse facebookStart() throws Exception {
        return send(AuthProtocol.AUTH_FACEBOOK_START, AuthRequest.facebookStart());
    }

    public AuthResponse facebookPoll(String sessionId) throws Exception {
        return send(AuthProtocol.AUTH_FACEBOOK_POLL, AuthRequest.facebookPoll(sessionId));
    }

    public AuthResponse logout(String sessionId, String accessToken) throws Exception {
        return send(AuthProtocol.AUTH_LOGOUT, AuthRequest.logout(sessionId, accessToken));
    }

    public void sendCalendarEventInviteAsync(String jsonPayload) {
        try {
            if (!network.isConnected()) {
                network.connect("localhost", 8888);
            }
            network.sendPacket(new Packet(AuthProtocol.CALENDAR_SEND_EVENT_INVITE, jsonPayload));
        } catch (Exception e) {
            System.err.println("Failed to send calendar event invite async: " + e.getMessage());
        }
    }

    public void sendCalendarPollInviteAsync(String jsonPayload) {
        try {
            if (!network.isConnected()) {
                network.connect("localhost", 8888);
            }
            network.sendPacket(new Packet(AuthProtocol.CALENDAR_SEND_POLL_INVITE, jsonPayload));
        } catch (Exception e) {
            System.err.println("Failed to send calendar poll invite async: " + e.getMessage());
        }
    }



    private AuthResponse send(String action, AuthRequest request) throws Exception {
        if (!network.isConnected()) {
            network.connect("localhost", 8888);
        }

        network.sendPacket(new Packet(action, request));
        Packet packet = network.receivePacket(AuthProtocol.RESPONSE, request.getRequestId(), DEFAULT_TIMEOUT_MS);
        if (packet.data instanceof AuthResponse response) {
            return response;
        }
        throw new Exception("Invalid auth response from server.");
    }
}
