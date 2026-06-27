package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.exam.V2OfficialResultDraftRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class V2ExamResultsWriteDAO {

    /**
     * Inserts an exam result into the legacy exam_results table.
     * Maps total_score -> rawScore and graded_by -> null (Automated V2 Server Grading).
     * 
     * @param conn  The database connection (must be part of a transaction)
     * @param draft The official result draft containing aggregate data
     * @return true if inserted, false otherwise
     */
    public boolean insertResultIfAbsent(Connection conn, V2OfficialResultDraftRecord draft) {
        // Only insert if it doesn't already exist.
        // We do a simple insert. Uniqueness on attempt_id should ideally be handled by constraints,
        // but exam_results schema doesn't have a UNIQUE constraint on attempt_id in ExamDatabaseManager.
        // The V2ResultPublicationService is responsible for calling the ReadOnlyProbe first.
        
        String query = "INSERT INTO exam_results (attempt_id, total_score, graded_by) VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, draft.getAttemptId());
            stmt.setDouble(2, draft.getRawScore());
            stmt.setNull(3, java.sql.Types.INTEGER); // graded_by is nullable INT. Using null for automated server grading.

            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
