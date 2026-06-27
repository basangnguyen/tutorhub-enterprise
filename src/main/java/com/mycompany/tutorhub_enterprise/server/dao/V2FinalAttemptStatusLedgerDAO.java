package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.exam.V2FinalAttemptStatusLedgerRecord;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Optional;

public class V2FinalAttemptStatusLedgerDAO {

    public void ensureSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS v2_final_attempt_status_ledger (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "submit_record_id BIGINT NOT NULL, " +
                "user_id INT NOT NULL, " +
                "exam_id INT NOT NULL, " +
                "paper_id INT, " +
                "attempt_id VARCHAR(255) NOT NULL, " +
                "from_status VARCHAR(50) NOT NULL, " +
                "to_status VARCHAR(50) NOT NULL, " +
                "publication_ledger_id BIGINT NOT NULL, " +
                "payload_hash VARCHAR(255) NOT NULL, " +
                "status_update_status VARCHAR(50) NOT NULL, " +
                "executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE(submit_record_id)" +
                ")";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Optional<V2FinalAttemptStatusLedgerRecord> findBySubmitRecordId(long submitRecordId) {
        String sql = "SELECT * FROM v2_final_attempt_status_ledger WHERE submit_record_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, submitRecordId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                V2FinalAttemptStatusLedgerRecord record = new V2FinalAttemptStatusLedgerRecord();
                record.setId(rs.getLong("id"));
                record.setSubmitRecordId(rs.getLong("submit_record_id"));
                record.setUserId(rs.getInt("user_id"));
                record.setExamId(rs.getInt("exam_id"));
                record.setPaperId(rs.getObject("paper_id") != null ? rs.getInt("paper_id") : null);
                record.setAttemptId(rs.getString("attempt_id"));
                record.setFromStatus(rs.getString("from_status"));
                record.setToStatus(rs.getString("to_status"));
                record.setPublicationLedgerId(rs.getLong("publication_ledger_id"));
                record.setPayloadHash(rs.getString("payload_hash"));
                record.setStatusUpdateStatus(rs.getString("status_update_status"));
                record.setExecutedAt(rs.getTimestamp("executed_at"));
                record.setCreatedAt(rs.getTimestamp("created_at"));
                record.setUpdatedAt(rs.getTimestamp("updated_at"));
                return Optional.of(record);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public boolean existsByAttemptId(String attemptId) {
        String sql = "SELECT 1 FROM v2_final_attempt_status_ledger WHERE attempt_id = ? LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, attemptId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public long insertExecutionLedger(Connection conn, V2FinalAttemptStatusLedgerRecord record) {
        String sql = "INSERT INTO v2_final_attempt_status_ledger (" +
                "submit_record_id, user_id, exam_id, paper_id, attempt_id, " +
                "from_status, to_status, publication_ledger_id, payload_hash, status_update_status" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, record.getSubmitRecordId());
            pstmt.setInt(2, record.getUserId());
            pstmt.setInt(3, record.getExamId());
            if (record.getPaperId() != null) {
                pstmt.setInt(4, record.getPaperId());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            pstmt.setString(5, record.getAttemptId());
            pstmt.setString(6, record.getFromStatus());
            pstmt.setString(7, record.getToStatus());
            pstmt.setLong(8, record.getPublicationLedgerId());
            pstmt.setString(9, record.getPayloadHash());
            pstmt.setString(10, record.getStatusUpdateStatus());

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1L;
    }
}
