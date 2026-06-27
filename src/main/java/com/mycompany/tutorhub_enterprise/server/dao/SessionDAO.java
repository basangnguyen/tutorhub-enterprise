package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class SessionDAO {

    public boolean createSession(String sessionId, int userId, String accessTokenHash, String refreshTokenHash, String deviceId, String deviceName, String appVersion, long expiresAt) {
        String sql = "INSERT INTO auth_sessions (id, user_id, access_token_hash, refresh_token_hash, device_id, device_name, app_version, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sessionId);
            pstmt.setInt(2, userId);
            pstmt.setString(3, accessTokenHash);
            pstmt.setString(4, refreshTokenHash);
            pstmt.setString(5, deviceId);
            pstmt.setString(6, deviceName);
            pstmt.setString(7, appVersion);
            pstmt.setTimestamp(8, new Timestamp(expiresAt));
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[SESSION_DAO ERROR] Failed to create session: " + e.getMessage());
            return false;
        }
    }

    public boolean revokeSession(String sessionId, String accessTokenHash) {
        String sql = "UPDATE auth_sessions SET revoked_at = CURRENT_TIMESTAMP WHERE id = ? AND access_token_hash = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sessionId);
            pstmt.setString(2, accessTokenHash);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[SESSION_DAO ERROR] Failed to revoke session: " + e.getMessage());
            return false;
        }
    }
}
