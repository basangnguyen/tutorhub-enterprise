package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.exam.V2ScoreDraftRecord;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.*;
import java.util.Optional;

public class V2ScoreDraftDAO {

    public void ensureTableExists() {
        String query = "CREATE TABLE IF NOT EXISTS v2_score_drafts (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "submit_record_id BIGINT UNIQUE NOT NULL, " +
                "user_id INT NOT NULL, " +
                "exam_id INT NOT NULL, " +
                "paper_id INT NOT NULL, " +
                "attempt_id VARCHAR(255) NOT NULL, " +
                "total_questions INT NOT NULL, " +
                "answered_questions INT NOT NULL, " +
                "unanswered_questions INT NOT NULL, " +
                "correct_count INT NOT NULL, " +
                "incorrect_count INT NOT NULL, " +
                "raw_score DOUBLE NOT NULL, " +
                "max_score DOUBLE NOT NULL, " +
                "percentage DOUBLE NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Optional<V2ScoreDraftRecord> findBySubmitRecordId(long submitRecordId) {
        String query = "SELECT * FROM v2_score_drafts WHERE submit_record_id = ?";
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

    public boolean insert(V2ScoreDraftRecord record) {
        String query = "INSERT INTO v2_score_drafts " +
                "(submit_record_id, user_id, exam_id, paper_id, attempt_id, total_questions, answered_questions, " +
                "unanswered_questions, correct_count, incorrect_count, raw_score, max_score, percentage) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, record.getSubmitRecordId());
            stmt.setInt(2, record.getUserId());
            stmt.setInt(3, record.getExamId());
            stmt.setInt(4, record.getPaperId());
            stmt.setString(5, record.getAttemptId());
            stmt.setInt(6, record.getTotalQuestions());
            stmt.setInt(7, record.getAnsweredQuestions());
            stmt.setInt(8, record.getUnansweredQuestions());
            stmt.setInt(9, record.getCorrectCount());
            stmt.setInt(10, record.getIncorrectCount());
            stmt.setDouble(11, record.getRawScore());
            stmt.setDouble(12, record.getMaxScore());
            stmt.setDouble(13, record.getPercentage());

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

    private V2ScoreDraftRecord mapRow(ResultSet rs) throws SQLException {
        V2ScoreDraftRecord record = new V2ScoreDraftRecord();
        record.setId(rs.getLong("id"));
        record.setSubmitRecordId(rs.getLong("submit_record_id"));
        record.setUserId(rs.getInt("user_id"));
        record.setExamId(rs.getInt("exam_id"));
        record.setPaperId(rs.getInt("paper_id"));
        record.setAttemptId(rs.getString("attempt_id"));
        record.setTotalQuestions(rs.getInt("total_questions"));
        record.setAnsweredQuestions(rs.getInt("answered_questions"));
        record.setUnansweredQuestions(rs.getInt("unanswered_questions"));
        record.setCorrectCount(rs.getInt("correct_count"));
        record.setIncorrectCount(rs.getInt("incorrect_count"));
        record.setRawScore(rs.getDouble("raw_score"));
        record.setMaxScore(rs.getDouble("max_score"));
        record.setPercentage(rs.getDouble("percentage"));
        record.setCreatedAt(rs.getTimestamp("created_at"));
        return record;
    }
}
