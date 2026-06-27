package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitDryRunRecord;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class V2SubmitDryRunPayloadDAO {

    public void ensureSchema() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection(); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS v2_submit_dryrun_payloads (" +
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
                    "validation_status VARCHAR(32) NOT NULL DEFAULT 'VALIDATED', " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            st.execute("CREATE INDEX IF NOT EXISTS idx_v2_submit_dryrun_attempt ON v2_submit_dryrun_payloads(attempt_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_v2_submit_dryrun_payload_hash ON v2_submit_dryrun_payloads(payload_hash)");
        }
    }

    public long insertDryRunRecord(V2SubmitDryRunRecord record) throws SQLException {
        ensureSchema();
        String sql = "INSERT INTO v2_submit_dryrun_payloads " +
                "(user_id, exam_id, paper_id, attempt_id, package_hash, payload_hash, payload_json, " +
                "answered_count, unanswered_count, complete, validation_status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
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
            st.setString(11, record.getValidationStatus());

            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1L;
    }

    public V2SubmitDryRunRecord findById(long id) throws SQLException {
        ensureSchema();
        String sql = "SELECT * FROM v2_submit_dryrun_payloads WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setLong(1, id);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return mapRecord(rs);
                }
            }
        }
        return null;
    }

    public V2SubmitDryRunRecord findLatestByAttemptId(String attemptId) throws SQLException {
        ensureSchema();
        String sql = "SELECT * FROM v2_submit_dryrun_payloads WHERE attempt_id = ? ORDER BY created_at DESC, id DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, attemptId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return mapRecord(rs);
                }
            }
        }
        return null;
    }

    public boolean existsByPayloadHash(String payloadHash) throws SQLException {
        ensureSchema();
        String sql = "SELECT 1 FROM v2_submit_dryrun_payloads WHERE payload_hash = ? LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, payloadHash);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next();
            }
        }
    }

    private V2SubmitDryRunRecord mapRecord(ResultSet rs) throws SQLException {
        V2SubmitDryRunRecord record = new V2SubmitDryRunRecord();
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
        record.setValidationStatus(rs.getString("validation_status"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        record.setCreatedAt(createdAt);
        record.setUpdatedAt(updatedAt);
        return record;
    }
}
