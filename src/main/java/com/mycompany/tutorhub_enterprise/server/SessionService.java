package com.mycompany.tutorhub_enterprise.server;

import com.mycompany.tutorhub_enterprise.models.auth.SessionInfo;
import com.mycompany.tutorhub_enterprise.server.dao.SessionDAO;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public class SessionService {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final SessionDAO sessionDAO = new SessionDAO();

    // 2 hours in milliseconds
    private static final long SESSION_LIFETIME_MS = 2 * 60 * 60 * 1000L;

    public static SessionInfo createSession(int userId, String deviceId, String deviceName, String appVersion) {
        String sessionId = UUID.randomUUID().toString();
        
        // Generate a 32-byte secure random token
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String rawAccessToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        // Hash the token
        String accessTokenHash = hashToken(rawAccessToken);
        
        long expiresAt = System.currentTimeMillis() + SESSION_LIFETIME_MS;
        
        // Save to DB
        boolean saved = sessionDAO.createSession(
                sessionId,
                userId,
                accessTokenHash,
                null, // No refresh token for Phase S1/S2 to minimize risk
                deviceId,
                deviceName,
                appVersion,
                expiresAt
        );
        
        if (!saved) {
            return null; // DB failure
        }
        
        // Create the DTO to return to client (contains RAW token)
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setSessionId(sessionId);
        sessionInfo.setAccessToken(rawAccessToken); // Only returned once!
        sessionInfo.setAccessTokenExpiresAt(expiresAt);
        sessionInfo.setTokenType("Opaque");
        // Refresh token fields are left null
        
        return sessionInfo;
    }

    private static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
    public static boolean revokeSession(String sessionId, String rawAccessToken) {
        if (sessionId == null || rawAccessToken == null) {
            return false;
        }
        String accessTokenHash = hashToken(rawAccessToken);
        return sessionDAO.revokeSession(sessionId, accessTokenHash);
    }
}
