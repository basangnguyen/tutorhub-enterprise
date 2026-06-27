package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptStatusTransitionDraftRecord;

import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class V2AttemptStatusTransitionDraftDAO {

    public void ensureSchema() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS v2_attempt_status_transition_drafts ("
                + "id SERIAL PRIMARY KEY, "
                + "submit_record_id BIGINT UNIQUE NOT NULL, "
                + "user_id INT NOT NULL, "
                + "exam_id INT NOT NULL, "
                + "paper_id INT NOT NULL, "
                + "attempt_id VARCHAR(255) NOT NULL, "
                + "transition_draft_id BIGINT NOT NULL, "
                + "payload_hash VARCHAR(64) NOT NULL, "
                + "preflight_status VARCHAR(255), "
                + "real_submit_transition_draft_status VARCHAR(255), "
                + "attempt_status_gate_status VARCHAR(255), "
                + "attempt_status_transition_draft_status VARCHAR(255), "
                + "from_attempt_status VARCHAR(255), "
                + "target_attempt_status VARCHAR(255), "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ")";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.executeUpdate();
        }
    }

    public Optional<V2AttemptStatusTransitionDraftRecord> findBySubmitRecordId(long submitRecordId) throws SQLException {
        String sql = "SELECT * FROM v2_attempt_status_transition_drafts WHERE submit_record_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setLong(1, submitRecordId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    V2AttemptStatusTransitionDraftRecord r = new V2AttemptStatusTransitionDraftRecord();
                    r.setId(rs.getLong("id"));
                    r.setSubmitRecordId(rs.getLong("submit_record_id"));
                    r.setUserId(rs.getInt("user_id"));
                    r.setExamId(rs.getInt("exam_id"));
                    r.setPaperId(rs.getInt("paper_id"));
                    r.setAttemptId(rs.getString("attempt_id"));
                    r.setTransitionDraftId(rs.getLong("transition_draft_id"));
                    r.setPayloadHash(rs.getString("payload_hash"));
                    r.setPreflightStatus(rs.getString("preflight_status"));
                    r.setRealSubmitTransitionDraftStatus(rs.getString("real_submit_transition_draft_status"));
                    r.setAttemptStatusGateStatus(rs.getString("attempt_status_gate_status"));
                    r.setAttemptStatusTransitionDraftStatus(rs.getString("attempt_status_transition_draft_status"));
                    r.setFromAttemptStatus(rs.getString("from_attempt_status"));
                    r.setTargetAttemptStatus(rs.getString("target_attempt_status"));
                    r.setCreatedAt(rs.getTimestamp("created_at"));
                    r.setUpdatedAt(rs.getTimestamp("updated_at"));
                    return Optional.of(r);
                }
            }
        }
        return Optional.empty();
    }

    public long insertDraft(V2AttemptStatusTransitionDraftRecord record) throws SQLException {
        Optional<V2AttemptStatusTransitionDraftRecord> existing = findBySubmitRecordId(record.getSubmitRecordId());
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        String sql = "INSERT INTO v2_attempt_status_transition_drafts "
                + "(submit_record_id, user_id, exam_id, paper_id, attempt_id, transition_draft_id, payload_hash, "
                + "preflight_status, real_submit_transition_draft_status, attempt_status_gate_status, "
                + "attempt_status_transition_draft_status, from_attempt_status, target_attempt_status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setLong(1, record.getSubmitRecordId());
            pst.setInt(2, record.getUserId());
            pst.setInt(3, record.getExamId());
            pst.setInt(4, record.getPaperId());
            pst.setString(5, record.getAttemptId());
            pst.setLong(6, record.getTransitionDraftId());
            pst.setString(7, record.getPayloadHash());
            pst.setString(8, record.getPreflightStatus());
            pst.setString(9, record.getRealSubmitTransitionDraftStatus());
            pst.setString(10, record.getAttemptStatusGateStatus());
            pst.setString(11, record.getAttemptStatusTransitionDraftStatus());
            pst.setString(12, record.getFromAttemptStatus());
            pst.setString(13, record.getTargetAttemptStatus());

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
