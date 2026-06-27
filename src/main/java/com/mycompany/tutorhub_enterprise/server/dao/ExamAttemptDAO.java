package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

public class ExamAttemptDAO {

    public static String createAttemptV2(int examId, int paperId, int userId, String sessionTokenHash, String packageHash, Timestamp deadlineAt, String clientInfoJson) throws SQLException {
        String attemptId = UUID.randomUUID().toString();
        
        try (Connection conn = DatabaseManager.getConnection()) {
            // Calculate next attempt_no
            int attemptNo = 1;
            String maxAttemptSql = "SELECT MAX(attempt_no) FROM exam_attempts WHERE exam_id = ? AND user_id = ?";
            try (PreparedStatement maxSt = conn.prepareStatement(maxAttemptSql)) {
                maxSt.setInt(1, examId);
                maxSt.setInt(2, userId);
                try (ResultSet rs = maxSt.executeQuery()) {
                    if (rs.next()) {
                        int max = rs.getInt(1);
                        if (!rs.wasNull()) {
                            attemptNo = max + 1;
                        }
                    }
                }
            }

            String sql = "INSERT INTO exam_attempts "
                    + "(id, exam_id, paper_id, user_id, attempt_no, status, deadline_at, session_token_hash, package_hash, client_info_json, started_at, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, 'STARTING', ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
                    
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setString(1, attemptId);
                st.setInt(2, examId);
                st.setInt(3, paperId);
                st.setInt(4, userId);
                st.setInt(5, attemptNo);
                
                if (deadlineAt != null) {
                    st.setTimestamp(6, deadlineAt);
                } else {
                    st.setNull(6, java.sql.Types.TIMESTAMP);
                }
                
                st.setString(7, sessionTokenHash);
                st.setString(8, packageHash);
                st.setString(9, clientInfoJson);
                
                st.executeUpdate();
            }
        }
        return attemptId;
    }

    public static boolean getAttemptById(String attemptId) {
        // Simple mock method for now, just to check if it exists
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT id FROM exam_attempts WHERE id = ?";
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setString(1, attemptId);
                try (ResultSet rs = st.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO] getAttemptById error: " + e.getMessage());
            return false;
        }
    }

    public static String getActiveAttemptForUserExam(int userId, int examId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT id FROM exam_attempts WHERE user_id = ? AND exam_id = ? AND status IN ('STARTING', 'IN_PROGRESS') ORDER BY started_at DESC LIMIT 1";
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setInt(1, userId);
                st.setInt(2, examId);
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("id");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO] getActiveAttemptForUserExam error: " + e.getMessage());
        }
        return null;
    }

    public static boolean markAttemptInProgress(String attemptId) {
        return updateAttemptStatus(attemptId, "IN_PROGRESS");
    }

    public static boolean markAttemptAbandoned(String attemptId) {
        return updateAttemptStatus(attemptId, "ABANDONED");
    }

    private static boolean updateAttemptStatus(String attemptId, String status) {
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "UPDATE exam_attempts SET status = ? WHERE id = ?";
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setString(1, status);
                st.setString(2, attemptId);
                int rows = st.executeUpdate();
                return rows > 0;
            }
        } catch (SQLException e) {
            System.err.println("[DAO] updateAttemptStatus error: " + e.getMessage());
            return false;
        }
    }

    public static com.mycompany.tutorhub_enterprise.models.exam.ExamAttempt getAttemptDetails(String attemptId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT * FROM exam_attempts WHERE id = ?";
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setString(1, attemptId);
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        com.mycompany.tutorhub_enterprise.models.exam.ExamAttempt attempt = new com.mycompany.tutorhub_enterprise.models.exam.ExamAttempt();
                        attempt.setId(rs.getString("id"));
                        attempt.setExamId(rs.getInt("exam_id"));
                        attempt.setPaperId(rs.getInt("paper_id"));
                        attempt.setUserId(rs.getInt("user_id"));
                        attempt.setAttemptNo(rs.getInt("attempt_no"));
                        attempt.setStatus(rs.getString("status"));
                        attempt.setDeadlineAt(rs.getTimestamp("deadline_at"));
                        attempt.setSessionTokenHash(rs.getString("session_token_hash"));
                        attempt.setPackageHash(rs.getString("package_hash"));
                        attempt.setStartedAt(rs.getTimestamp("started_at"));
                        attempt.setCreatedAt(rs.getTimestamp("created_at"));
                        return attempt;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO] getAttemptDetails error: " + e.getMessage());
        }
        return null;
    }
}
