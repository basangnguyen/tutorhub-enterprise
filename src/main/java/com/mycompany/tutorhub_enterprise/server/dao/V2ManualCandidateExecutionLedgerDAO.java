package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.exam.V2ManualCandidateExecutionLedgerRecord;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.*;
import java.util.Optional;

public class V2ManualCandidateExecutionLedgerDAO {

    public void ensureSchema() {
        String query = "CREATE TABLE IF NOT EXISTS v2_manual_candidate_execution_ledger (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "attempt_id VARCHAR(255) NOT NULL, " +
                "submit_record_id BIGINT, " +
                "user_id INT NOT NULL, " +
                "exam_id INT NOT NULL, " +
                "paper_id INT NOT NULL, " +
                "publication_ledger_id BIGINT, " +
                "final_status_ledger_id BIGINT, " +
                "execution_status VARCHAR(64) NOT NULL, " +
                "execution_mode VARCHAR(64) NOT NULL, " +
                "error_code VARCHAR(255), " +
                "blocking_reason_summary TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "UNIQUE KEY uk_manual_exec_attempt (attempt_id)" +
                ")";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Optional<V2ManualCandidateExecutionLedgerRecord> findByAttemptId(String attemptId) {
        String query = "SELECT * FROM v2_manual_candidate_execution_ledger WHERE attempt_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, attemptId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<V2ManualCandidateExecutionLedgerRecord> findBySubmitRecordId(long submitRecordId) {
        String query = "SELECT * FROM v2_manual_candidate_execution_ledger WHERE submit_record_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setLong(1, submitRecordId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public boolean insertLedger(V2ManualCandidateExecutionLedgerRecord record) {
        String query = "INSERT INTO v2_manual_candidate_execution_ledger " +
                "(attempt_id, submit_record_id, user_id, exam_id, paper_id, publication_ledger_id, final_status_ledger_id, " +
                "execution_status, execution_mode, error_code, blocking_reason_summary) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, record.getAttemptId());
            if (record.getSubmitRecordId() > 0) {
                stmt.setLong(2, record.getSubmitRecordId());
            } else {
                stmt.setNull(2, Types.BIGINT);
            }
            stmt.setInt(3, record.getUserId());
            stmt.setInt(4, record.getExamId());
            stmt.setInt(5, record.getPaperId());
            if (record.getPublicationLedgerId() > 0) {
                stmt.setLong(6, record.getPublicationLedgerId());
            } else {
                stmt.setNull(6, Types.BIGINT);
            }
            if (record.getFinalStatusLedgerId() > 0) {
                stmt.setLong(7, record.getFinalStatusLedgerId());
            } else {
                stmt.setNull(7, Types.BIGINT);
            }
            stmt.setString(8, record.getExecutionStatus());
            stmt.setString(9, record.getExecutionMode());
            stmt.setString(10, record.getErrorCode());
            stmt.setString(11, record.getBlockingReasonSummary());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        record.setId(rs.getLong(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            if (!e.getMessage().contains("Duplicate entry")) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private V2ManualCandidateExecutionLedgerRecord mapRow(ResultSet rs) throws SQLException {
        V2ManualCandidateExecutionLedgerRecord record = new V2ManualCandidateExecutionLedgerRecord();
        record.setId(rs.getLong("id"));
        record.setAttemptId(rs.getString("attempt_id"));
        record.setSubmitRecordId(rs.getLong("submit_record_id"));
        if (rs.wasNull()) {
            record.setSubmitRecordId(0);
        }
        record.setUserId(rs.getInt("user_id"));
        record.setExamId(rs.getInt("exam_id"));
        record.setPaperId(rs.getInt("paper_id"));
        record.setPublicationLedgerId(rs.getLong("publication_ledger_id"));
        if (rs.wasNull()) {
            record.setPublicationLedgerId(0);
        }
        record.setFinalStatusLedgerId(rs.getLong("final_status_ledger_id"));
        if (rs.wasNull()) {
            record.setFinalStatusLedgerId(0);
        }
        record.setExecutionStatus(rs.getString("execution_status"));
        record.setExecutionMode(rs.getString("execution_mode"));
        record.setErrorCode(rs.getString("error_code"));
        record.setBlockingReasonSummary(rs.getString("blocking_reason_summary"));
        record.setCreatedAt(rs.getTimestamp("created_at"));
        record.setUpdatedAt(rs.getTimestamp("updated_at"));
        return record;
    }
}
