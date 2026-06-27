package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptFinalizationLedgerRecord;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

public class V2AttemptFinalizationLedgerDAO {

    public void ensureSchema() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection(); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS v2_attempt_finalization_ledger (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "submit_record_id BIGINT NOT NULL, " +
                    "user_id INT NOT NULL, " +
                    "exam_id INT NOT NULL, " +
                    "paper_id INT NOT NULL, " +
                    "attempt_id VARCHAR(64), " +
                    "payload_hash VARCHAR(128) NOT NULL, " +
                    "previous_submit_status VARCHAR(32), " +
                    "finalization_status VARCHAR(32) NOT NULL DEFAULT 'FINALIZATION_DRAFT_ACKNOWLEDGED', " +
                    "finalization_mode VARCHAR(64) NOT NULL DEFAULT 'NO_GRADING_NO_FINAL_SUBMIT', " +
                    "source VARCHAR(32) NOT NULL DEFAULT 'V2_DEBUG', " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            st.execute("CREATE INDEX IF NOT EXISTS idx_v2_finalization_ledger_submit_record_id ON v2_attempt_finalization_ledger(submit_record_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_v2_finalization_ledger_attempt_id ON v2_attempt_finalization_ledger(attempt_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_v2_finalization_ledger_payload_hash ON v2_attempt_finalization_ledger(payload_hash)");
        }
    }

    public long insertLedgerRecord(V2AttemptFinalizationLedgerRecord record) throws SQLException {
        ensureSchema();
        String sql = "INSERT INTO v2_attempt_finalization_ledger " +
                "(submit_record_id, user_id, exam_id, paper_id, attempt_id, payload_hash, " +
                "previous_submit_status, finalization_status, finalization_mode, source, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement st = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.setLong(1, record.getSubmitRecordId());
            st.setInt(2, record.getUserId());
            st.setInt(3, record.getExamId());
            st.setInt(4, record.getPaperId());
            st.setString(5, record.getAttemptId());
            st.setString(6, record.getPayloadHash());
            st.setString(7, record.getPreviousSubmitStatus());
            st.setString(8, record.getFinalizationStatus() == null ? "FINALIZATION_DRAFT_ACKNOWLEDGED" : record.getFinalizationStatus());
            st.setString(9, record.getFinalizationMode() == null ? "NO_GRADING_NO_FINAL_SUBMIT" : record.getFinalizationMode());
            st.setString(10, record.getSource() == null ? "V2_DEBUG" : record.getSource());

            st.executeUpdate();
            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1L;
    }

    public Optional<V2AttemptFinalizationLedgerRecord> findById(long id) throws SQLException {
        ensureSchema();
        String sql = "SELECT * FROM v2_attempt_finalization_ledger WHERE id = ?";
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

    public Optional<V2AttemptFinalizationLedgerRecord> findLatestBySubmitRecordId(long submitRecordId) throws SQLException {
        ensureSchema();
        String sql = "SELECT * FROM v2_attempt_finalization_ledger WHERE submit_record_id = ? ORDER BY created_at DESC, id DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setLong(1, submitRecordId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRecord(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<V2AttemptFinalizationLedgerRecord> findLatestByAttemptId(String attemptId) throws SQLException {
        ensureSchema();
        String sql = "SELECT * FROM v2_attempt_finalization_ledger WHERE attempt_id = ? ORDER BY created_at DESC, id DESC LIMIT 1";
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

    public boolean existsBySubmitRecordId(long submitRecordId) throws SQLException {
        ensureSchema();
        String sql = "SELECT 1 FROM v2_attempt_finalization_ledger WHERE submit_record_id = ? LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setLong(1, submitRecordId);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next();
            }
        }
    }

    private V2AttemptFinalizationLedgerRecord mapRecord(ResultSet rs) throws SQLException {
        V2AttemptFinalizationLedgerRecord record = new V2AttemptFinalizationLedgerRecord();
        record.setId(rs.getLong("id"));
        record.setSubmitRecordId(rs.getLong("submit_record_id"));
        record.setUserId(rs.getInt("user_id"));
        record.setExamId(rs.getInt("exam_id"));
        record.setPaperId(rs.getInt("paper_id"));
        record.setAttemptId(rs.getString("attempt_id"));
        record.setPayloadHash(rs.getString("payload_hash"));
        record.setPreviousSubmitStatus(rs.getString("previous_submit_status"));
        record.setFinalizationStatus(rs.getString("finalization_status"));
        record.setFinalizationMode(rs.getString("finalization_mode"));
        record.setSource(rs.getString("source"));
        record.setCreatedAt(rs.getTimestamp("created_at"));
        return record;
    }
}
