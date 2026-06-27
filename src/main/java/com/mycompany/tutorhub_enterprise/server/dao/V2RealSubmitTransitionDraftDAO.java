package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.exam.V2RealSubmitTransitionDraftRecord;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class V2RealSubmitTransitionDraftDAO {

    public void ensureSchema() throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS v2_real_submit_transition_drafts (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "submit_record_id BIGINT NOT NULL UNIQUE," +
                "user_id INT NOT NULL," +
                "exam_id INT NOT NULL," +
                "paper_id INT NOT NULL," +
                "attempt_id VARCHAR(64)," +
                "ledger_id BIGINT NOT NULL," +
                "closure_draft_id BIGINT NOT NULL," +
                "payload_hash VARCHAR(64) NOT NULL," +
                "preflight_status VARCHAR(64) NOT NULL," +
                "transition_draft_status VARCHAR(64) NOT NULL DEFAULT 'REAL_SUBMIT_TRANSITION_DRAFTED'," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ");";

        String[] createIndexSQLs = {
                "CREATE INDEX IF NOT EXISTS idx_v2_rst_draft_user_id ON v2_real_submit_transition_drafts(user_id);",
                "CREATE INDEX IF NOT EXISTS idx_v2_rst_draft_attempt_id ON v2_real_submit_transition_drafts(attempt_id);"
        };

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            for (String sql : createIndexSQLs) {
                try {
                    stmt.execute(sql);
                } catch (SQLException e) {
                    // Ignore if IF NOT EXISTS is not supported and index exists
                }
            }
        }
    }

    public long insertDraft(V2RealSubmitTransitionDraftRecord record) throws SQLException {
        String sql = "INSERT INTO v2_real_submit_transition_drafts (submit_record_id, user_id, exam_id, paper_id, attempt_id, ledger_id, closure_draft_id, payload_hash, preflight_status, transition_draft_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setLong(1, record.getSubmitRecordId());
            pstmt.setInt(2, record.getUserId());
            pstmt.setInt(3, record.getExamId());
            pstmt.setInt(4, record.getPaperId());
            pstmt.setString(5, record.getAttemptId());
            pstmt.setLong(6, record.getLedgerId());
            pstmt.setLong(7, record.getClosureDraftId());
            pstmt.setString(8, record.getPayloadHash());
            pstmt.setString(9, record.getPreflightStatus());
            pstmt.setString(10, record.getTransitionDraftStatus() != null ? record.getTransitionDraftStatus() : "REAL_SUBMIT_TRANSITION_DRAFTED");
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating transition draft failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating transition draft failed, no ID obtained.");
                }
            }
        }
    }

    public Optional<V2RealSubmitTransitionDraftRecord> findBySubmitRecordId(long submitRecordId) throws SQLException {
        String sql = "SELECT * FROM v2_real_submit_transition_drafts WHERE submit_record_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, submitRecordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    private V2RealSubmitTransitionDraftRecord mapRow(ResultSet rs) throws SQLException {
        V2RealSubmitTransitionDraftRecord record = new V2RealSubmitTransitionDraftRecord();
        record.setId(rs.getLong("id"));
        record.setSubmitRecordId(rs.getLong("submit_record_id"));
        record.setUserId(rs.getInt("user_id"));
        record.setExamId(rs.getInt("exam_id"));
        record.setPaperId(rs.getInt("paper_id"));
        record.setAttemptId(rs.getString("attempt_id"));
        record.setLedgerId(rs.getLong("ledger_id"));
        record.setClosureDraftId(rs.getLong("closure_draft_id"));
        record.setPayloadHash(rs.getString("payload_hash"));
        record.setPreflightStatus(rs.getString("preflight_status"));
        record.setTransitionDraftStatus(rs.getString("transition_draft_status"));
        record.setCreatedAt(rs.getTimestamp("created_at"));
        record.setUpdatedAt(rs.getTimestamp("updated_at"));
        return record;
    }
}
