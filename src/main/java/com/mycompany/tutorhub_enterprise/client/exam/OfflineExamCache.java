package com.mycompany.tutorhub_enterprise.client.exam;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class OfflineExamCache {
    private static final String URL = "jdbc:sqlite:tutorhub_exam_cache.db";
    
    static {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            // Create a simple table to store exam drafts
            stmt.execute("CREATE TABLE IF NOT EXISTS exam_drafts (" +
                         "exam_id INTEGER, " +
                         "user_id INTEGER, " +
                         "question_id INTEGER, " +
                         "answer TEXT, " +
                         "PRIMARY KEY(exam_id, user_id, question_id))");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveDraftAnswer(int examId, int userId, int questionId, String answer) {
        String sql = "INSERT INTO exam_drafts (exam_id, user_id, question_id, answer) VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT(exam_id, user_id, question_id) DO UPDATE SET answer = excluded.answer";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, examId);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, questionId);
            pstmt.setString(4, answer);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<Integer, String> getDraftAnswers(int examId, int userId) {
        Map<Integer, String> drafts = new HashMap<>();
        String sql = "SELECT question_id, answer FROM exam_drafts WHERE exam_id = ? AND user_id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, examId);
            pstmt.setInt(2, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                drafts.put(rs.getInt("question_id"), rs.getString("answer"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return drafts;
    }
    
    public static void clearDraft(int examId, int userId) {
        String sql = "DELETE FROM exam_drafts WHERE exam_id = ? AND user_id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, examId);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
