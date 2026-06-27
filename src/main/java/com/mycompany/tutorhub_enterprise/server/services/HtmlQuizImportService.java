package com.mycompany.tutorhub_enterprise.server.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mycompany.tutorhub_enterprise.models.exam.ParsedQuizQuestion;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles the server-side logic for importing HTML quizzes.
 * Parses the request payload, validates questions, and inserts into DB 
 * under a single transaction to ensure atomicity.
 */
public class HtmlQuizImportService {

    private static final Logger LOGGER = Logger.getLogger(HtmlQuizImportService.class.getName());
    private static final int MAX_QUESTIONS = 500;

    public static class ImportRequest {
        public String bankName;
        public String paperTitle;
        public float defaultPoints = 1.0f;
        public boolean createPaper = false;
        public List<ParsedQuizQuestion> questions;
    }

    public static class ImportResult {
        public boolean success;
        public String errorMessage;
        public int bankId = -1;
        public int paperId = -1;
        public int questionCount = 0;

        public static ImportResult fail(String msg) {
            ImportResult r = new ImportResult();
            r.success = false;
            r.errorMessage = msg;
            return r;
        }

        public static ImportResult ok(int bankId, int paperId, int questionCount) {
            ImportResult r = new ImportResult();
            r.success = true;
            r.bankId = bankId;
            r.paperId = paperId;
            r.questionCount = questionCount;
            return r;
        }
    }

    /**
     * Parse the JSON payload and execute the import transaction.
     */
    public ImportResult processImport(String jsonPayload, int userId) {
        if (jsonPayload == null || jsonPayload.trim().isEmpty()) {
            return ImportResult.fail("Payload trống.");
        }

        ImportRequest req;
        try {
            req = new Gson().fromJson(jsonPayload, ImportRequest.class);
        } catch (Exception e) {
            return ImportResult.fail("Dữ liệu JSON không hợp lệ: " + e.getMessage());
        }

        if (req.bankName == null || req.bankName.trim().isEmpty()) {
            return ImportResult.fail("Tên ngân hàng câu hỏi không được để trống.");
        }
        if (req.questions == null || req.questions.isEmpty()) {
            return ImportResult.fail("Không có câu hỏi nào để import.");
        }
        if (req.questions.size() > MAX_QUESTIONS) {
            return ImportResult.fail("Vượt quá giới hạn " + MAX_QUESTIONS + " câu hỏi (hiện có " + req.questions.size() + ").");
        }
        if (req.defaultPoints <= 0) {
            return ImportResult.fail("Điểm mặc định phải lớn hơn 0.");
        }

        // Validate all questions first
        for (int i = 0; i < req.questions.size(); i++) {
            ParsedQuizQuestion q = req.questions.get(i);
            if (q.getSourceIndex() == 0) q.setSourceIndex(i); // fallback if missing
            q.validate();
            if (!q.isValid()) {
                LOGGER.warning("[IMPORT_HTML_QUIZ] Validation failed at index " + i + ": " + q.getValidationError());
                return ImportResult.fail("Câu hỏi số " + (i + 1) + " bị lỗi: " + q.getValidationError());
            }
        }

        LOGGER.info(String.format("[IMPORT_HTML_QUIZ] userId=%d start. questionCount=%d createPaper=%b", 
                userId, req.questions.size(), req.createPaper));

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            try {
                // 1. Create Question Bank
                int bankId = insertQuestionBank(conn, req.bankName, "Imported from HTML", userId);
                
                // 2. Insert Questions and Options
                int[] questionIds = new int[req.questions.size()];
                for (int i = 0; i < req.questions.size(); i++) {
                    ParsedQuizQuestion pq = req.questions.get(i);
                    int questionId = insertQuestion(conn, bankId, pq, req.defaultPoints, i, userId);
                    insertOptions(conn, questionId, pq);
                    questionIds[i] = questionId;
                }

                // 3. Create Exam Paper if requested
                int paperId = -1;
                if (req.createPaper) {
                    String title = (req.paperTitle != null && !req.paperTitle.trim().isEmpty()) ? req.paperTitle : req.bankName;
                    paperId = insertExamPaper(conn, title, "Imported from HTML", userId);
                    insertPaperQuestions(conn, paperId, questionIds, req.defaultPoints);
                    recalculateTotalScore(conn, paperId);
                }

                conn.commit();
                LOGGER.info(String.format("[IMPORT_HTML_QUIZ] userId=%d SUCCESS. bankId=%d paperId=%d", userId, bankId, paperId));
                return ImportResult.ok(bankId, paperId, req.questions.size());

            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "[IMPORT_HTML_QUIZ] Transaction rolled back due to error", e);
                return ImportResult.fail("Lỗi cơ sở dữ liệu khi import. Đã rollback: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "[IMPORT_HTML_QUIZ] Failed to get connection or auto-commit", e);
            return ImportResult.fail("Lỗi kết nối cơ sở dữ liệu: " + e.getMessage());
        }
    }

    private int insertQuestionBank(Connection conn, String name, String description, int creatorId) throws SQLException {
        String sql = "INSERT INTO question_banks (name, description, creator_id) VALUES (?, ?, ?) RETURNING id";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, name);
            pst.setString(2, description);
            pst.setInt(3, creatorId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                throw new SQLException("Failed to retrieve generated ID for question bank.");
            }
        }
    }

    private int insertQuestion(Connection conn, int bankId, ParsedQuizQuestion pq, float points, int sortOrder, int creatorId) throws SQLException {
        String sql = "INSERT INTO questions (bank_id, question_type, points, content, explanation, sort_order, creator_id, category, difficulty) " +
                     "VALUES (?, 'MCQ', ?, ?, ?, ?, ?, 'Imported', 'Medium') RETURNING id";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, bankId);
            pst.setFloat(2, points);
            pst.setString(3, pq.getQuestion());
            pst.setString(4, pq.getExplanation());
            pst.setInt(5, sortOrder);
            pst.setInt(6, creatorId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                throw new SQLException("Failed to retrieve generated ID for question.");
            }
        }
    }

    private void insertOptions(Connection conn, int questionId, ParsedQuizQuestion pq) throws SQLException {
        String sql = "INSERT INTO question_options (question_id, option_label, content, is_correct, order_index) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            int index = 0;
            String correctKey = pq.getCorrectAnswer() != null ? pq.getCorrectAnswer().trim().toLowerCase() : "";
            for (Map.Entry<String, String> entry : pq.getAnswers().entrySet()) {
                String key = entry.getKey().trim().toLowerCase();
                boolean isCorrect = key.equals(correctKey);
                
                pst.setInt(1, questionId);
                pst.setString(2, key.toUpperCase()); // Store as A, B, C, D
                pst.setString(3, entry.getValue());
                pst.setBoolean(4, isCorrect);
                pst.setInt(5, index++);
                pst.addBatch();
            }
            pst.executeBatch();
        }
    }

    private int insertExamPaper(Connection conn, String title, String description, int creatorId) throws SQLException {
        String sql = "INSERT INTO exam_papers (title, description, creator_id, status) VALUES (?, ?, ?, 'DRAFT') RETURNING id";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, title);
            pst.setString(2, description);
            pst.setInt(3, creatorId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                throw new SQLException("Failed to retrieve generated ID for exam paper.");
            }
        }
    }

    private void insertPaperQuestions(Connection conn, int paperId, int[] questionIds, float defaultPoints) throws SQLException {
        String sql = "INSERT INTO exam_paper_questions (paper_id, question_id, points, order_idx, is_required) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            for (int i = 0; i < questionIds.length; i++) {
                pst.setInt(1, paperId);
                pst.setInt(2, questionIds[i]);
                pst.setFloat(3, defaultPoints);
                pst.setInt(4, i);
                pst.setBoolean(5, true);
                pst.addBatch();
            }
            pst.executeBatch();
        }
    }
    
    private void recalculateTotalScore(Connection conn, int paperId) throws SQLException {
        String sql = "UPDATE exam_papers SET total_score = (SELECT COALESCE(SUM(points), 0) FROM exam_paper_questions WHERE paper_id = ?) WHERE id = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, paperId);
            pst.setInt(2, paperId);
            pst.executeUpdate();
        }
    }
}
