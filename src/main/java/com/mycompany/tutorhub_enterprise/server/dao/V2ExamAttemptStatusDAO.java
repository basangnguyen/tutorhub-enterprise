package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class V2ExamAttemptStatusDAO {

    public Optional<String> findAttemptStatus(String attemptId) throws SQLException {
        String sql = "SELECT status FROM exam_attempts WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, attemptId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("status"));
                }
            }
        }
        return Optional.empty();
    }

    public boolean updateAttemptStatusIfCurrent(Connection conn, String attemptId, String expectedCurrentStatus, String targetStatus) throws SQLException {
        String sql = "UPDATE exam_attempts SET status = ? WHERE id = ? AND status = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, targetStatus);
            pst.setString(2, attemptId);
            pst.setString(3, expectedCurrentStatus);
            
            int updatedRows = pst.executeUpdate();
            return updatedRows > 0;
        }
    }
}
