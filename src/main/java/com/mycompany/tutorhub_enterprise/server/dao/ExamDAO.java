package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.exam.*;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ExamDAO {
    
    public static int createExam(Exam exam) {
        String sql = "INSERT INTO exams (creator_id, title, description, duration_mins, open_at, close_at, security_config, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?) RETURNING id";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setInt(1, exam.creatorId);
            pst.setString(2, exam.title);
            pst.setString(3, exam.description);
            pst.setInt(4, exam.durationMins > 0 ? exam.durationMins : 60);
            
            if (exam.openAt != null) pst.setTimestamp(5, exam.openAt);
            else pst.setNull(5, java.sql.Types.TIMESTAMP);
            
            if (exam.closeAt != null) pst.setTimestamp(6, exam.closeAt);
            else pst.setNull(6, java.sql.Types.TIMESTAMP);
            
            pst.setString(7, exam.securityConfig);
            pst.setString(8, exam.status != null ? exam.status : "DRAFT");

            
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<Exam> getExamsByCreator(int creatorId) {
        List<Exam> list = new ArrayList<>();
        String sql = "SELECT * FROM exams WHERE creator_id = ? ORDER BY id DESC";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, creatorId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Exam e = new Exam();
                e.id = rs.getInt("id");
                e.creatorId = rs.getInt("creator_id");
                e.title = rs.getString("title");
                e.description = rs.getString("description");
                e.durationMins = rs.getInt("duration_mins");
                e.openAt = rs.getTimestamp("open_at");
                e.closeAt = rs.getTimestamp("close_at");
                e.securityConfig = rs.getString("security_config");
                e.status = rs.getString("status");
                e.createdAt = rs.getTimestamp("created_at");
                e.paperId = rs.getObject("paper_id") != null ? rs.getInt("paper_id") : null;
                list.add(e);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // In current TutorHub exam schema, ACTIVE means available/published for students.
    public static List<Exam> getPublishedExams() {
        List<Exam> list = new ArrayList<>();
        String sql = "SELECT * FROM exams WHERE status = 'ACTIVE' ORDER BY id DESC";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Exam e = new Exam();
                e.id = rs.getInt("id");
                e.creatorId = rs.getInt("creator_id");
                e.title = rs.getString("title");
                e.description = rs.getString("description");
                e.durationMins = rs.getInt("duration_mins");
                e.openAt = rs.getTimestamp("open_at");
                e.closeAt = rs.getTimestamp("close_at");
                e.securityConfig = rs.getString("security_config");
                e.status = rs.getString("status");
                e.createdAt = rs.getTimestamp("created_at");
                e.paperId = rs.getObject("paper_id") != null ? rs.getInt("paper_id") : null;
                list.add(e);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public static Exam getExamById(int examId) {
        String sql = "SELECT * FROM exams WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, examId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                Exam e = new Exam();
                e.id = rs.getInt("id");
                e.creatorId = rs.getInt("creator_id");
                e.title = rs.getString("title");
                e.description = rs.getString("description");
                e.durationMins = rs.getInt("duration_mins");
                e.openAt = rs.getTimestamp("open_at");
                e.closeAt = rs.getTimestamp("close_at");
                e.securityConfig = rs.getString("security_config");
                e.status = rs.getString("status");
                e.createdAt = rs.getTimestamp("created_at");
                e.paperId = rs.getObject("paper_id") != null ? rs.getInt("paper_id") : null;
                return e;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public static int addQuestion(Question q) {
        String sql = "INSERT INTO questions (exam_id, question_type, category, difficulty, points, content, explanation, sort_order) " +
                     "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?) RETURNING id";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, q.examId);
            pst.setString(2, q.questionType);
            pst.setString(3, q.category);
            pst.setString(4, q.difficulty);
            pst.setFloat(5, q.points);
            pst.setString(6, q.content);
            pst.setString(7, q.explanation);
            pst.setInt(8, q.sortOrder);
            
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    public static List<Question> getQuestionsByExam(int examId) {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT * FROM questions WHERE exam_id = ? ORDER BY sort_order ASC, id ASC";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, examId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Question q = new Question();
                q.id = rs.getInt("id");
                q.examId = rs.getInt("exam_id");
                q.questionType = rs.getString("question_type");
                q.category = rs.getString("category");
                q.difficulty = rs.getString("difficulty");
                q.points = rs.getFloat("points");
                q.content = rs.getString("content");
                q.explanation = rs.getString("explanation");
                q.sortOrder = rs.getInt("sort_order");
                list.add(q);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public static List<Exam> getExamsForStudent() {
        List<Exam> list = new ArrayList<>();
        String sql = "SELECT DISTINCT e.* " +
                     "FROM exams e " +
                     "JOIN exam_papers p ON p.id = e.paper_id " +
                     "JOIN exam_paper_questions epq ON epq.paper_id = p.id " +
                     "WHERE e.status = 'ACTIVE' " +
                     "ORDER BY e.id DESC";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Exam e = new Exam();
                e.id = rs.getInt("id");
                e.creatorId = rs.getInt("creator_id");
                e.title = rs.getString("title");
                e.description = rs.getString("description");
                e.durationMins = rs.getInt("duration_mins");
                e.openAt = rs.getTimestamp("open_at");
                e.closeAt = rs.getTimestamp("close_at");
                e.securityConfig = rs.getString("security_config");
                e.status = rs.getString("status");
                e.paperId = rs.getInt("paper_id");
                list.add(e);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static String getUserRole(int userId) {
        String sql = "SELECT role FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public static int createSession(ExamSession session) {
        String sql = "INSERT INTO exam_sessions (exam_id, user_id, status, started_at, client_info, question_order) " +
                     "VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?::jsonb, ?::jsonb) " +
                     "ON CONFLICT (exam_id, user_id) DO UPDATE SET " +
                     "status = excluded.status, started_at = CURRENT_TIMESTAMP, " +
                     "client_info = excluded.client_info, question_order = excluded.question_order " +
                     "RETURNING id";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, session.examId);
            pst.setInt(2, session.userId);
            pst.setString(3, session.status != null ? session.status : "IN_PROGRESS");
            pst.setString(4, session.clientInfo);
            pst.setString(5, session.questionOrder);
            
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    public static ExamSession getSessionById(int sessionId) {
        String sql = "SELECT * FROM exam_sessions WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, sessionId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                ExamSession s = new ExamSession();
                s.id = rs.getInt("id");
                s.examId = rs.getInt("exam_id");
                s.userId = rs.getInt("user_id");
                s.status = rs.getString("status");
                s.startedAt = rs.getTimestamp("started_at");
                s.submittedAt = rs.getTimestamp("submitted_at");
                s.clientInfo = rs.getString("client_info");
                return s;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public static boolean updateSessionStatus(int sessionId, String status) {
        String sql = "UPDATE exam_sessions SET status = ?, submitted_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, status);
            pst.setInt(2, sessionId);
            return pst.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // --- Phase 4B: Paper-to-Exam Assignment ---
    public static boolean assignPaperToExam(int examId, int paperId) {
        String sql = "UPDATE exams SET paper_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, paperId);
            pst.setInt(2, examId);
            return pst.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean unassignPaperFromExam(int examId) {
        String sql = "UPDATE exams SET paper_id = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, examId);
            return pst.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Integer getAssignedPaperId(int examId) {
        String sql = "SELECT paper_id FROM exams WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, examId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                Object pId = rs.getObject("paper_id");
                return pId != null ? ((Number) pId).intValue() : null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Exam getExamWithAssignedPaper(int examId) {
        return getExamById(examId);
    }

    public static boolean saveAnswer(int sessionId, int questionId, String answerData) {
        String sql = "INSERT INTO exam_answers (session_id, question_id, answer_data) VALUES (?, ?, ?::jsonb) " +
                     "ON CONFLICT (session_id, question_id) DO UPDATE SET " +
                     "answer_data = EXCLUDED.answer_data, change_count = exam_answers.change_count + 1, last_updated = CURRENT_TIMESTAMP";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sessionId);
            pstmt.setInt(2, questionId);
            pstmt.setString(3, answerData);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
