package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptClosureDraftRecord;

import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class V2AttemptClosureDraftDAO {

    public void ensureSchema() throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS v2_attempt_closure_drafts (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "ledger_id BIGINT NOT NULL," +
                "submit_record_id BIGINT NOT NULL," +
                "user_id INT NOT NULL," +
                "exam_id INT NOT NULL," +
                "paper_id INT NOT NULL," +
                "attempt_id VARCHAR(64)," +
                "payload_hash VARCHAR(128) NOT NULL," +
                "closure_status VARCHAR(64) NOT NULL DEFAULT 'CLOSURE_DRAFTED_NO_GRADING'," +
                "closure_mode VARCHAR(64) NOT NULL DEFAULT 'NO_GRADING_NO_FINAL_SUBMIT'," +
                "source VARCHAR(32) NOT NULL DEFAULT 'V2_DEBUG'," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        String[] createIndexSQLs = {
                "CREATE INDEX IF NOT EXISTS idx_v2_attempt_closure_ledger_id ON v2_attempt_closure_drafts(ledger_id);",
                "CREATE INDEX IF NOT EXISTS idx_v2_attempt_closure_submit_record_id ON v2_attempt_closure_drafts(submit_record_id);",
                "CREATE INDEX IF NOT EXISTS idx_v2_attempt_closure_attempt_id ON v2_attempt_closure_drafts(attempt_id);",
                "CREATE INDEX IF NOT EXISTS idx_v2_attempt_closure_payload_hash ON v2_attempt_closure_drafts(payload_hash);"
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

    public long insertClosureDraft(V2AttemptClosureDraftRecord record) throws SQLException {
        String sql = "INSERT INTO v2_attempt_closure_drafts (ledger_id, submit_record_id, user_id, exam_id, paper_id, attempt_id, payload_hash, closure_status, closure_mode, source) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setLong(1, record.getLedgerId());
            pstmt.setLong(2, record.getSubmitRecordId());
            pstmt.setInt(3, record.getUserId());
            pstmt.setInt(4, record.getExamId());
            pstmt.setInt(5, record.getPaperId());
            pstmt.setString(6, record.getAttemptId());
            pstmt.setString(7, record.getPayloadHash());
            pstmt.setString(8, record.getClosureStatus() != null ? record.getClosureStatus() : "CLOSURE_DRAFTED_NO_GRADING");
            pstmt.setString(9, record.getClosureMode() != null ? record.getClosureMode() : "NO_GRADING_NO_FINAL_SUBMIT");
            pstmt.setString(10, record.getSource() != null ? record.getSource() : "V2_DEBUG");
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating closure draft failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating closure draft failed, no ID obtained.");
                }
            }
        }
    }

    public Optional<V2AttemptClosureDraftRecord> findById(long id) throws SQLException {
        String sql = "SELECT * FROM v2_attempt_closure_drafts WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<V2AttemptClosureDraftRecord> findLatestBySubmitRecordId(long submitRecordId) throws SQLException {
        String sql = "SELECT * FROM v2_attempt_closure_drafts WHERE submit_record_id = ? ORDER BY id DESC LIMIT 1";
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

    public Optional<V2AttemptClosureDraftRecord> findLatestByLedgerId(long ledgerId) throws SQLException {
        String sql = "SELECT * FROM v2_attempt_closure_drafts WHERE ledger_id = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, ledgerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<V2AttemptClosureDraftRecord> findLatestByAttemptId(String attemptId) throws SQLException {
        String sql = "SELECT * FROM v2_attempt_closure_drafts WHERE attempt_id = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, attemptId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public boolean existsBySubmitRecordId(long submitRecordId) throws SQLException {
        String sql = "SELECT 1 FROM v2_attempt_closure_drafts WHERE submit_record_id = ? LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, submitRecordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private V2AttemptClosureDraftRecord mapRow(ResultSet rs) throws SQLException {
        V2AttemptClosureDraftRecord record = new V2AttemptClosureDraftRecord();
        record.setId(rs.getLong("id"));
        record.setLedgerId(rs.getLong("ledger_id"));
        record.setSubmitRecordId(rs.getLong("submit_record_id"));
        record.setUserId(rs.getInt("user_id"));
        record.setExamId(rs.getInt("exam_id"));
        record.setPaperId(rs.getInt("paper_id"));
        record.setAttemptId(rs.getString("attempt_id"));
        record.setPayloadHash(rs.getString("payload_hash"));
        record.setClosureStatus(rs.getString("closure_status"));
        record.setClosureMode(rs.getString("closure_mode"));
        record.setSource(rs.getString("source"));
        record.setCreatedAt(rs.getTimestamp("created_at"));
        return record;
    }
}
