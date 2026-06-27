package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptStatusExecutionLedgerRecord;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class V2AttemptStatusExecutionLedgerDAO {

    public void ensureSchema() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS v2_attempt_status_execution_ledger ("
                + "id SERIAL PRIMARY KEY, "
                + "submit_record_id BIGINT UNIQUE NOT NULL, "
                + "user_id INT NOT NULL, "
                + "exam_id INT NOT NULL, "
                + "paper_id INT NOT NULL, "
                + "attempt_id VARCHAR(255) NOT NULL, "
                + "attempt_status_transition_draft_id BIGINT NOT NULL, "
                + "payload_hash VARCHAR(64) NOT NULL, "
                + "from_attempt_status VARCHAR(255), "
                + "target_attempt_status VARCHAR(255), "
                + "actual_attempt_status VARCHAR(255), "
                + "execution_status VARCHAR(255), "
                + "executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ")";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.executeUpdate();
        }
    }

    public Optional<V2AttemptStatusExecutionLedgerRecord> findBySubmitRecordId(long submitRecordId) throws SQLException {
        String sql = "SELECT * FROM v2_attempt_status_execution_ledger WHERE submit_record_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setLong(1, submitRecordId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    V2AttemptStatusExecutionLedgerRecord r = new V2AttemptStatusExecutionLedgerRecord();
                    r.setId(rs.getLong("id"));
                    r.setSubmitRecordId(rs.getLong("submit_record_id"));
                    r.setUserId(rs.getInt("user_id"));
                    r.setExamId(rs.getInt("exam_id"));
                    r.setPaperId(rs.getInt("paper_id"));
                    r.setAttemptId(rs.getString("attempt_id"));
                    r.setAttemptStatusTransitionDraftId(rs.getLong("attempt_status_transition_draft_id"));
                    r.setPayloadHash(rs.getString("payload_hash"));
                    r.setFromAttemptStatus(rs.getString("from_attempt_status"));
                    r.setTargetAttemptStatus(rs.getString("target_attempt_status"));
                    r.setActualAttemptStatus(rs.getString("actual_attempt_status"));
                    r.setExecutionStatus(rs.getString("execution_status"));
                    r.setExecutedAt(rs.getTimestamp("executed_at"));
                    r.setCreatedAt(rs.getTimestamp("created_at"));
                    r.setUpdatedAt(rs.getTimestamp("updated_at"));
                    return Optional.of(r);
                }
            }
        }
        return Optional.empty();
    }

    public long insertExecutionLedger(Connection conn, V2AttemptStatusExecutionLedgerRecord record) throws SQLException {
        String sql = "INSERT INTO v2_attempt_status_execution_ledger "
                + "(submit_record_id, user_id, exam_id, paper_id, attempt_id, attempt_status_transition_draft_id, payload_hash, "
                + "from_attempt_status, target_attempt_status, actual_attempt_status, execution_status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setLong(1, record.getSubmitRecordId());
            pst.setInt(2, record.getUserId());
            pst.setInt(3, record.getExamId());
            pst.setInt(4, record.getPaperId());
            pst.setString(5, record.getAttemptId());
            pst.setLong(6, record.getAttemptStatusTransitionDraftId());
            pst.setString(7, record.getPayloadHash());
            pst.setString(8, record.getFromAttemptStatus());
            pst.setString(9, record.getTargetAttemptStatus());
            pst.setString(10, record.getActualAttemptStatus());
            pst.setString(11, record.getExecutionStatus());

            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1L;
    }
}
