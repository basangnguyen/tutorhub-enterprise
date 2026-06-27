package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.exam.ExamPaper;
import com.mycompany.tutorhub_enterprise.models.exam.ExamPaperQuestion;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ExamPaperDAO {

    public static int createExamPaper(String title, String description, int creatorId) {
        String sql = "INSERT INTO exam_papers (title, description, creator_id, status) VALUES (?, ?, ?, 'DRAFT') RETURNING id";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, title);
            pst.setString(2, description);
            pst.setInt(3, creatorId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<ExamPaper> listExamPapersByCreator(int creatorId) {
        List<ExamPaper> list = new ArrayList<>();
        String sql = "SELECT * FROM exam_papers WHERE creator_id = ? ORDER BY id DESC";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, creatorId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                ExamPaper paper = new ExamPaper();
                paper.id = rs.getInt("id");
                paper.title = rs.getString("title");
                paper.description = rs.getString("description");
                paper.creatorId = rs.getInt("creator_id");
                paper.status = rs.getString("status");
                paper.totalScore = rs.getFloat("total_score");
                paper.createdAt = rs.getTimestamp("created_at");
                paper.updatedAt = rs.getTimestamp("updated_at");
                list.add(paper);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static ExamPaper getExamPaperById(int paperId) {
        String sql = "SELECT * FROM exam_papers WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, paperId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                ExamPaper paper = new ExamPaper();
                paper.id = rs.getInt("id");
                paper.title = rs.getString("title");
                paper.description = rs.getString("description");
                paper.creatorId = rs.getInt("creator_id");
                paper.status = rs.getString("status");
                paper.totalScore = rs.getFloat("total_score");
                paper.createdAt = rs.getTimestamp("created_at");
                paper.updatedAt = rs.getTimestamp("updated_at");
                return paper;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean updateExamPaper(int paperId, String title, String description) {
        String sql = "UPDATE exam_papers SET title = ?, description = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, title);
            pst.setString(2, description);
            pst.setInt(3, paperId);
            return pst.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean deleteExamPaper(int paperId) {
        // Warning: This does a hard delete. Usually not allowed if PUBLISHED. Checked in Service.
        String sql = "DELETE FROM exam_papers WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, paperId);
            return pst.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean addQuestionToPaper(int paperId, int questionId, float score, int orderIndex, boolean isRequired) {
        String sql = "INSERT INTO exam_paper_questions (paper_id, question_id, points, order_idx, is_required) VALUES (?, ?, ?, ?, ?) " +
                     "ON CONFLICT (paper_id, question_id) DO UPDATE SET points = EXCLUDED.points, order_idx = EXCLUDED.order_idx, is_required = EXCLUDED.is_required";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, paperId);
            pst.setInt(2, questionId);
            pst.setFloat(3, score);
            pst.setInt(4, orderIndex);
            pst.setBoolean(5, isRequired);
            boolean ok = pst.executeUpdate() > 0;
            if (ok) recalculateTotalScore(conn, paperId);
            return ok;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean removeQuestionFromPaper(int paperId, int questionId) {
        String sql = "DELETE FROM exam_paper_questions WHERE paper_id = ? AND question_id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, paperId);
            pst.setInt(2, questionId);
            boolean ok = pst.executeUpdate() > 0;
            if (ok) recalculateTotalScore(conn, paperId);
            return ok;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static List<ExamPaperQuestion> listQuestionsByPaper(int paperId) {
        List<ExamPaperQuestion> list = new ArrayList<>();
        String sql = "SELECT * FROM exam_paper_questions WHERE paper_id = ? ORDER BY order_idx ASC";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, paperId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                ExamPaperQuestion eq = new ExamPaperQuestion();
                eq.paperId = rs.getInt("paper_id");
                eq.questionId = rs.getInt("question_id");
                eq.score = rs.getFloat("points");
                eq.orderIndex = rs.getInt("order_idx");
                eq.required = rs.getBoolean("is_required");
                eq.createdAt = rs.getTimestamp("created_at");
                list.add(eq);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private static void recalculateTotalScore(Connection conn, int paperId) {
        String sql = "UPDATE exam_papers SET total_score = (SELECT COALESCE(SUM(points), 0) FROM exam_paper_questions WHERE paper_id = ?) WHERE id = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, paperId);
            pst.setInt(2, paperId);
            pst.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean checkQuestionInPaper(int paperId, int questionId) {
        String sql = "SELECT 1 FROM exam_paper_questions WHERE paper_id = ? AND question_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, paperId);
            pst.setInt(2, questionId);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
