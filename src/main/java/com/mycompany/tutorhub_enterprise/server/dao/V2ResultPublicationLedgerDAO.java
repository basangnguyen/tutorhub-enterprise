package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.exam.V2ResultPublicationLedgerRecord;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.*;
import java.util.Optional;

public class V2ResultPublicationLedgerDAO {

    public void ensureSchema() {
        String query = "CREATE TABLE IF NOT EXISTS v2_result_publication_ledger (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "submit_record_id BIGINT UNIQUE NOT NULL, " +
                "official_result_draft_id BIGINT NOT NULL, " +
                "score_draft_id BIGINT NOT NULL, " +
                "user_id INT NOT NULL, " +
                "exam_id INT NOT NULL, " +
                "paper_id INT NOT NULL, " +
                "attempt_id VARCHAR(255) NOT NULL, " +
                "payload_hash VARCHAR(64) NOT NULL, " +
                "raw_score DOUBLE NOT NULL, " +
                "max_score DOUBLE NOT NULL, " +
                "percentage DOUBLE NOT NULL, " +
                "publication_status VARCHAR(64) NOT NULL, " +
                "published_at TIMESTAMP, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Optional<V2ResultPublicationLedgerRecord> findBySubmitRecordId(long submitRecordId) {
        String query = "SELECT * FROM v2_result_publication_ledger WHERE submit_record_id = ?";
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

    public Optional<V2ResultPublicationLedgerRecord> findByAttemptId(String attemptId) {
        String query = "SELECT * FROM v2_result_publication_ledger WHERE attempt_id = ? ORDER BY id DESC LIMIT 1";
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

    public boolean insertLedger(Connection conn, V2ResultPublicationLedgerRecord record) {
        String query = "INSERT INTO v2_result_publication_ledger " +
                "(submit_record_id, official_result_draft_id, score_draft_id, user_id, exam_id, paper_id, attempt_id, " +
                "payload_hash, raw_score, max_score, percentage, publication_status, published_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, record.getSubmitRecordId());
            stmt.setLong(2, record.getOfficialResultDraftId());
            stmt.setLong(3, record.getScoreDraftId());
            stmt.setInt(4, record.getUserId());
            stmt.setInt(5, record.getExamId());
            stmt.setInt(6, record.getPaperId());
            stmt.setString(7, record.getAttemptId());
            stmt.setString(8, record.getPayloadHash());
            stmt.setDouble(9, record.getRawScore());
            stmt.setDouble(10, record.getMaxScore());
            stmt.setDouble(11, record.getPercentage());
            stmt.setString(12, record.getPublicationStatus());
            stmt.setTimestamp(13, record.getPublishedAt());

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

    private V2ResultPublicationLedgerRecord mapRow(ResultSet rs) throws SQLException {
        V2ResultPublicationLedgerRecord record = new V2ResultPublicationLedgerRecord();
        record.setId(rs.getLong("id"));
        record.setSubmitRecordId(rs.getLong("submit_record_id"));
        record.setOfficialResultDraftId(rs.getLong("official_result_draft_id"));
        record.setScoreDraftId(rs.getLong("score_draft_id"));
        record.setUserId(rs.getInt("user_id"));
        record.setExamId(rs.getInt("exam_id"));
        record.setPaperId(rs.getInt("paper_id"));
        record.setAttemptId(rs.getString("attempt_id"));
        record.setPayloadHash(rs.getString("payload_hash"));
        record.setRawScore(rs.getDouble("raw_score"));
        record.setMaxScore(rs.getDouble("max_score"));
        record.setPercentage(rs.getDouble("percentage"));
        record.setPublicationStatus(rs.getString("publication_status"));
        record.setPublishedAt(rs.getTimestamp("published_at"));
        record.setCreatedAt(rs.getTimestamp("created_at"));
        record.setUpdatedAt(rs.getTimestamp("updated_at"));
        return record;
    }
}
