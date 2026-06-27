package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

public class V2SubmitRecordDAO {

    public void ensureSchema() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection(); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS v2_submit_records (" +
                    "id BIGSERIAL PRIMARY KEY, " +
                    "user_id INT NOT NULL, " +
                    "exam_id INT NOT NULL, " +
                    "paper_id INT NOT NULL, " +
                    "attempt_id VARCHAR(64), " +
                    "package_hash VARCHAR(128), " +
                    "payload_hash VARCHAR(128) NOT NULL, " +
                    "payload_json TEXT NOT NULL, " +
                    "answered_count INT NOT NULL, " +
                    "unanswered_count INT NOT NULL, " +
                    "complete BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "submit_status VARCHAR(32) NOT NULL DEFAULT 'RECEIVED_DEBUG', " +
                    "source VARCHAR(32) NOT NULL DEFAULT 'V2_DEBUG', " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            st.execute("CREATE INDEX IF NOT EXISTS idx_v2_submit_records_attempt_id ON v2_submit_records(attempt_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_v2_submit_records_payload_hash ON v2_submit_records(payload_hash)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_v2_submit_records_user_exam ON v2_submit_records(user_id, exam_id)");
        }
    }

    public long insertSubmitRecord(V2SubmitRecord record) throws SQLException {
        ensureSchema();
        try (Connection conn = DatabaseManager.getConnection()) {
            return insertSubmitRecord(conn, record);
        }
    }

    public long insertSubmitRecord(Connection conn, V2SubmitRecord record) throws SQLException {
        ensureSchema();
        String sql = "INSERT INTO v2_submit_records " +
                "(user_id, exam_id, paper_id, attempt_id, package_hash, payload_hash, payload_json, " +
                "answered_count, unanswered_count, complete, submit_status, source, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING id";
        try (PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, record.getUserId());
            st.setInt(2, record.getExamId());
            st.setInt(3, record.getPaperId());
            st.setString(4, record.getAttemptId());
            st.setString(5, record.getPackageHash());
            st.setString(6, record.getPayloadHash());
            st.setString(7, record.getPayloadJson());
            st.setInt(8, record.getAnsweredCount());
            st.setInt(9, record.getUnansweredCount());
            st.setBoolean(10, record.isComplete());
            st.setString(11, record.getSubmitStatus());
            st.setString(12, record.getSource());

            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1L;
    }

    public Optional<V2SubmitRecord> findById(long id) throws SQLException {
        ensureSchema();
        String sql = "SELECT * FROM v2_submit_records WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setLong(1, id);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRecord(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<V2SubmitRecord> findLatestByAttemptId(String attemptId) throws SQLException {
        ensureSchema();
        String sql = "SELECT * FROM v2_submit_records WHERE attempt_id = ? ORDER BY created_at DESC, id DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, attemptId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRecord(rs));
                }
            }
        }
        return Optional.empty();
    }

    public boolean existsByPayloadHash(String payloadHash) throws SQLException {
        ensureSchema();
        String sql = "SELECT 1 FROM v2_submit_records WHERE payload_hash = ? LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, payloadHash);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean updateSubmitStatus(long submitRecordId, String status) throws SQLException {
        ensureSchema();
        String sql = "UPDATE v2_submit_records SET submit_status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, status);
            st.setLong(2, submitRecordId);
            return st.executeUpdate() > 0;
        }
    }

    private V2SubmitRecord mapRecord(ResultSet rs) throws SQLException {
        V2SubmitRecord record = new V2SubmitRecord();
        record.setId(rs.getLong("id"));
        record.setUserId(rs.getInt("user_id"));
        record.setExamId(rs.getInt("exam_id"));
        record.setPaperId(rs.getInt("paper_id"));
        record.setAttemptId(rs.getString("attempt_id"));
        record.setPackageHash(rs.getString("package_hash"));
        record.setPayloadHash(rs.getString("payload_hash"));
        record.setPayloadJson(rs.getString("payload_json"));
        record.setAnsweredCount(rs.getInt("answered_count"));
        record.setUnansweredCount(rs.getInt("unanswered_count"));
        record.setComplete(rs.getBoolean("complete"));
        record.setSubmitStatus(rs.getString("submit_status"));
        record.setSource(rs.getString("source"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        record.setCreatedAt(createdAt);
        record.setUpdatedAt(updatedAt);
        return record;
    }
}
