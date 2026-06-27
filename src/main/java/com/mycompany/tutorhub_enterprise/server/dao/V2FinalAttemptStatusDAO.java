package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class V2FinalAttemptStatusDAO {

    public Optional<String> findAttemptStatus(String attemptId) {
        String sql = "SELECT status FROM exam_attempts WHERE attempt_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, attemptId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.ofNullable(rs.getString("status"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public boolean updateAttemptStatusIfCurrent(Connection conn, String attemptId, String expectedCurrentStatus, String targetStatus) {
        String sql = "UPDATE exam_attempts SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE attempt_id = ? AND status = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, targetStatus);
            pstmt.setString(2, attemptId);
            pstmt.setString(3, expectedCurrentStatus);
            int updatedRows = pstmt.executeUpdate();
            return updatedRows > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
