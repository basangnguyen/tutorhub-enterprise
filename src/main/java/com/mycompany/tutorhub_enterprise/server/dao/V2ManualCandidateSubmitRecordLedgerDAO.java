package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.exam.V2ManualCandidateSubmitRecordLedgerRecord;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class V2ManualCandidateSubmitRecordLedgerDAO {

    public void ensureSchema() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection(); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS v2_manual_candidate_submit_record_ledger (" +
                    "id BIGSERIAL PRIMARY KEY, " +
                    "attempt_id VARCHAR(64) UNIQUE NOT NULL, " +
                    "submit_record_id BIGINT UNIQUE, " +
                    "user_id INT NOT NULL, " +
                    "exam_id INT NOT NULL, " +
                    "paper_id INT NOT NULL, " +
                    "payload_hash VARCHAR(64) NOT NULL, " +
                    "materialization_status VARCHAR(64) NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");
        }
    }

    public long insertLedger(Connection conn, V2ManualCandidateSubmitRecordLedgerRecord record) throws SQLException {
        ensureSchema();
        String sql = "INSERT INTO v2_manual_candidate_submit_record_ledger " +
                "(attempt_id, submit_record_id, user_id, exam_id, paper_id, payload_hash, materialization_status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING id";
        try (PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, record.getAttemptId());
            if (record.getSubmitRecordId() != null) {
                st.setLong(2, record.getSubmitRecordId());
            } else {
                st.setNull(2, java.sql.Types.BIGINT);
            }
            st.setInt(3, record.getUserId());
            st.setInt(4, record.getExamId());
            st.setInt(5, record.getPaperId());
            st.setString(6, record.getPayloadHash());
            st.setString(7, record.getMaterializationStatus());

            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1L;
    }

    public Optional<V2ManualCandidateSubmitRecordLedgerRecord> findByAttemptId(String attemptId) throws SQLException {
        ensureSchema();
        String sql = "SELECT * FROM v2_manual_candidate_submit_record_ledger WHERE attempt_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, attemptId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    V2ManualCandidateSubmitRecordLedgerRecord record = new V2ManualCandidateSubmitRecordLedgerRecord();
                    record.setId(rs.getLong("id"));
                    record.setAttemptId(rs.getString("attempt_id"));
                    long srId = rs.getLong("submit_record_id");
                    if (!rs.wasNull()) {
                        record.setSubmitRecordId(srId);
                    }
                    record.setUserId(rs.getInt("user_id"));
                    record.setExamId(rs.getInt("exam_id"));
                    record.setPaperId(rs.getInt("paper_id"));
                    record.setPayloadHash(rs.getString("payload_hash"));
                    record.setMaterializationStatus(rs.getString("materialization_status"));
                    record.setCreatedAt(rs.getTimestamp("created_at"));
                    record.setUpdatedAt(rs.getTimestamp("updated_at"));
                    return Optional.of(record);
                }
            }
        }
        return Optional.empty();
    }
}
