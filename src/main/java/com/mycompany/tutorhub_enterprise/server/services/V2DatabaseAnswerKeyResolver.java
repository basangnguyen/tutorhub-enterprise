package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Concrete implementation of V2AnswerKeyResolver fetching from the database.
 * Uses a safe read-only SELECT query.
 */
public class V2DatabaseAnswerKeyResolver implements V2AnswerKeyResolver {

    @Override
    public Map<Long, Long> resolveCorrectOptionIds(int paperId) {
        if (!V2SubmitFeatureFlags.isDatabaseAnswerKeyResolverEnabled()) {
            return null; // Unavailable sentinel
        }

        Map<Long, Long> correctOptions = new HashMap<>();
        String query = "SELECT q.id as question_id, o.id as option_id " +
                       "FROM exam_paper_questions eq " +
                       "JOIN questions q ON eq.question_id = q.id " +
                       "JOIN question_options o ON q.id = o.question_id " +
                       "WHERE eq.paper_id = ? AND o.is_correct = TRUE";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement st = conn.prepareStatement(query)) {

            st.setInt(1, paperId);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    long questionId = rs.getLong("question_id");
                    long optionId = rs.getLong("option_id");
                    correctOptions.put(questionId, optionId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null; // Treat exception as unavailable
        }

        return correctOptions;
    }
}
