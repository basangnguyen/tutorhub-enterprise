package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.exam.Question;
import com.mycompany.tutorhub_enterprise.models.exam.QuestionOption;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class QuestionDAO {

    public static int createQuestion(Question question, List<QuestionOption> options) {
        String sqlQuestion = "INSERT INTO questions (bank_id, question_type, category, difficulty, points, content, explanation, sort_order, creator_id) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        String sqlOption = "INSERT INTO question_options (question_id, option_label, content, is_correct, order_index) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            
            int questionId = -1;
            try (PreparedStatement pstQ = conn.prepareStatement(sqlQuestion)) {
                if (question.bankId > 0) pstQ.setInt(1, question.bankId); else pstQ.setNull(1, java.sql.Types.INTEGER);
                pstQ.setString(2, question.questionType);
                pstQ.setString(3, question.category);
                pstQ.setString(4, question.difficulty);
                pstQ.setFloat(5, question.defaultScore > 0 ? question.defaultScore : question.points);
                pstQ.setString(6, question.content);
                pstQ.setString(7, question.explanation);
                pstQ.setInt(8, question.sortOrder);
                pstQ.setInt(9, question.createdBy);
                
                ResultSet rs = pstQ.executeQuery();
                if (rs.next()) {
                    questionId = rs.getInt(1);
                }
            }

            if (questionId > 0 && options != null && !options.isEmpty()) {
                try (PreparedStatement pstO = conn.prepareStatement(sqlOption)) {
                    for (QuestionOption opt : options) {
                        pstO.setInt(1, questionId);
                        pstO.setString(2, opt.optionLabel);
                        pstO.setString(3, opt.content);
                        pstO.setBoolean(4, opt.isCorrect);
                        pstO.setInt(5, opt.orderIndex);
                        pstO.addBatch();
                    }
                    pstO.executeBatch();
                }
            }

            conn.commit();
            conn.setAutoCommit(true);
            return questionId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<Question> listQuestionsByBank(int bankId) {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT * FROM questions WHERE bank_id = ? ORDER BY sort_order ASC, id ASC";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, bankId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Question q = new Question();
                q.id = rs.getInt("id");
                q.examId = rs.getInt("exam_id");
                q.bankId = rs.getInt("bank_id");
                q.questionType = rs.getString("question_type");
                q.category = rs.getString("category");
                q.difficulty = rs.getString("difficulty");
                q.points = rs.getFloat("points");
                q.defaultScore = rs.getFloat("points");
                q.content = rs.getString("content");
                q.explanation = rs.getString("explanation");
                q.sortOrder = rs.getInt("sort_order");
                q.createdBy = rs.getInt("creator_id");
                q.createdAt = rs.getTimestamp("created_at");
                q.updatedAt = rs.getTimestamp("updated_at");
                list.add(q);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public static Question getQuestionById(int questionId) {
        String sql = "SELECT * FROM questions WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, questionId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                Question q = new Question();
                q.id = rs.getInt("id");
                q.examId = rs.getInt("exam_id");
                q.bankId = rs.getInt("bank_id");
                q.questionType = rs.getString("question_type");
                q.category = rs.getString("category");
                q.difficulty = rs.getString("difficulty");
                q.points = rs.getFloat("points");
                q.defaultScore = rs.getFloat("points");
                q.content = rs.getString("content");
                q.explanation = rs.getString("explanation");
                q.sortOrder = rs.getInt("sort_order");
                q.createdBy = rs.getInt("creator_id");
                q.createdAt = rs.getTimestamp("created_at");
                q.updatedAt = rs.getTimestamp("updated_at");
                return q;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public static List<QuestionOption> getOptionsByQuestionId(int questionId) {
        List<QuestionOption> list = new ArrayList<>();
        String sql = "SELECT * FROM question_options WHERE question_id = ? ORDER BY order_index ASC";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, questionId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                QuestionOption opt = new QuestionOption();
                opt.id = rs.getInt("id");
                opt.questionId = rs.getInt("question_id");
                opt.optionLabel = rs.getString("option_label");
                opt.content = rs.getString("content");
                opt.isCorrect = rs.getBoolean("is_correct");
                opt.orderIndex = rs.getInt("order_index");
                list.add(opt);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public static boolean updateQuestion(Question question, List<QuestionOption> options) {
        String sqlQuestion = "UPDATE questions SET bank_id = ?, question_type = ?, category = ?, difficulty = ?, points = ?, content = ?, explanation = ?, sort_order = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        String sqlDeleteOptions = "DELETE FROM question_options WHERE question_id = ?";
        String sqlOption = "INSERT INTO question_options (question_id, option_label, content, is_correct, order_index) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstQ = conn.prepareStatement(sqlQuestion)) {
                if (question.bankId > 0) pstQ.setInt(1, question.bankId); else pstQ.setNull(1, java.sql.Types.INTEGER);
                pstQ.setString(2, question.questionType);
                pstQ.setString(3, question.category);
                pstQ.setString(4, question.difficulty);
                pstQ.setFloat(5, question.defaultScore > 0 ? question.defaultScore : question.points);
                pstQ.setString(6, question.content);
                pstQ.setString(7, question.explanation);
                pstQ.setInt(8, question.sortOrder);
                pstQ.setInt(9, question.id);
                pstQ.executeUpdate();
            }

            try (PreparedStatement pstD = conn.prepareStatement(sqlDeleteOptions)) {
                pstD.setInt(1, question.id);
                pstD.executeUpdate();
            }

            if (options != null && !options.isEmpty()) {
                try (PreparedStatement pstO = conn.prepareStatement(sqlOption)) {
                    for (QuestionOption opt : options) {
                        pstO.setInt(1, question.id);
                        pstO.setString(2, opt.optionLabel);
                        pstO.setString(3, opt.content);
                        pstO.setBoolean(4, opt.isCorrect);
                        pstO.setInt(5, opt.orderIndex);
                        pstO.addBatch();
                    }
                    pstO.executeBatch();
                }
            }

            conn.commit();
            conn.setAutoCommit(true);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean deleteQuestion(int questionId) {
        String sql = "DELETE FROM questions WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, questionId);
            return pst.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
