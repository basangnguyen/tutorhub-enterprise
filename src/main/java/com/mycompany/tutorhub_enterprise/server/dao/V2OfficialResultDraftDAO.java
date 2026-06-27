package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.exam.V2OfficialResultDraftRecord;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.*;
import java.util.Optional;

public class V2OfficialResultDraftDAO {

    public void ensureTableExists() {
        String query = "CREATE TABLE IF NOT EXISTS v2_official_result_drafts (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "score_draft_id BIGINT NOT NULL, " +
                "submit_record_id BIGINT UNIQUE NOT NULL, " +
                "user_id INT NOT NULL, " +
                "exam_id INT NOT NULL, " +
                "paper_id INT NOT NULL, " +
                "attempt_id VARCHAR(255) NOT NULL, " +
                "payload_hash VARCHAR(64) NOT NULL, " +
                "total_questions INT NOT NULL, " +
                "answered_questions INT NOT NULL, " +
                "unanswered_questions INT NOT NULL, " +
                "correct_count INT NOT NULL, " +
                "incorrect_count INT NOT NULL, " +
                "raw_score DOUBLE NOT NULL, " +
                "max_score DOUBLE NOT NULL, " +
                "percentage DOUBLE NOT NULL, " +
                "score_draft_status VARCHAR(64) NOT NULL, " +
                "score_draft_audit_status VARCHAR(64) NOT NULL, " +
                "official_result_draft_status VARCHAR(64) NOT NULL, " +
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

    public Optional<V2OfficialResultDraftRecord> findBySubmitRecordId(long submitRecordId) {
        String query = "SELECT * FROM v2_official_result_drafts WHERE submit_record_id = ?";
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

    public boolean insertDraft(V2OfficialResultDraftRecord record) {
        String query = "INSERT INTO v2_official_result_drafts " +
                "(score_draft_id, submit_record_id, user_id, exam_id, paper_id, attempt_id, payload_hash, total_questions, answered_questions, " +
                "unanswered_questions, correct_count, incorrect_count, raw_score, max_score, percentage, score_draft_status, score_draft_audit_status, official_result_draft_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, record.getScoreDraftId());
            stmt.setLong(2, record.getSubmitRecordId());
            stmt.setInt(3, record.getUserId());
            stmt.setInt(4, record.getExamId());
            stmt.setInt(5, record.getPaperId());
            stmt.setString(6, record.getAttemptId());
            stmt.setString(7, record.getPayloadHash());
            stmt.setInt(8, record.getTotalQuestions());
            stmt.setInt(9, record.getAnsweredQuestions());
            stmt.setInt(10, record.getUnansweredQuestions());
            stmt.setInt(11, record.getCorrectCount());
            stmt.setInt(12, record.getIncorrectCount());
            stmt.setDouble(13, record.getRawScore());
            stmt.setDouble(14, record.getMaxScore());
            stmt.setDouble(15, record.getPercentage());
            stmt.setString(16, record.getScoreDraftStatus());
            stmt.setString(17, record.getScoreDraftAuditStatus());
            stmt.setString(18, record.getOfficialResultDraftStatus());

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

    private V2OfficialResultDraftRecord mapRow(ResultSet rs) throws SQLException {
        V2OfficialResultDraftRecord record = new V2OfficialResultDraftRecord();
        record.setId(rs.getLong("id"));
        record.setScoreDraftId(rs.getLong("score_draft_id"));
        record.setSubmitRecordId(rs.getLong("submit_record_id"));
        record.setUserId(rs.getInt("user_id"));
        record.setExamId(rs.getInt("exam_id"));
        record.setPaperId(rs.getInt("paper_id"));
        record.setAttemptId(rs.getString("attempt_id"));
        record.setPayloadHash(rs.getString("payload_hash"));
        record.setTotalQuestions(rs.getInt("total_questions"));
        record.setAnsweredQuestions(rs.getInt("answered_questions"));
        record.setUnansweredQuestions(rs.getInt("unanswered_questions"));
        record.setCorrectCount(rs.getInt("correct_count"));
        record.setIncorrectCount(rs.getInt("incorrect_count"));
        record.setRawScore(rs.getDouble("raw_score"));
        record.setMaxScore(rs.getDouble("max_score"));
        record.setPercentage(rs.getDouble("percentage"));
        record.setScoreDraftStatus(rs.getString("score_draft_status"));
        record.setScoreDraftAuditStatus(rs.getString("score_draft_audit_status"));
        record.setOfficialResultDraftStatus(rs.getString("official_result_draft_status"));
        record.setCreatedAt(rs.getTimestamp("created_at"));
        record.setUpdatedAt(rs.getTimestamp("updated_at"));
        return record;
    }
}
