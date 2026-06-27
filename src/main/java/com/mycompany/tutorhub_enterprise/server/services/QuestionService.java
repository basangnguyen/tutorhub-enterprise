package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.models.exam.Question;
import com.mycompany.tutorhub_enterprise.models.exam.QuestionOption;
import com.mycompany.tutorhub_enterprise.server.dao.QuestionBankDAO;
import com.mycompany.tutorhub_enterprise.server.dao.QuestionDAO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestionService {

    public static Packet handleCreateQuestion(int creatorId, Question question, List<QuestionOption> options) {
        // 1. Validate bankId
        if (question.bankId <= 0 || QuestionBankDAO.getQuestionBankById(question.bankId) == null) {
            Packet p = new Packet(false, "Ngân hàng câu hỏi không tồn tại");
            p.action = "QUESTION_CREATE_FAILED";
            return p;
        }
        
        // 2. Validate content
        if (question.content == null || question.content.trim().isEmpty()) {
            Packet p = new Packet(false, "Nội dung câu hỏi không được để trống");
            p.action = "QUESTION_CREATE_FAILED";
            return p;
        }
        
        // 3. Validate type
        if (question.questionType == null || question.questionType.trim().isEmpty()) {
            Packet p = new Packet(false, "Loại câu hỏi không được để trống");
            p.action = "QUESTION_CREATE_FAILED";
            return p;
        }
        
        // 4. Validate defaultScore
        if (question.defaultScore <= 0) {
            Packet p = new Packet(false, "Điểm mặc định phải lớn hơn 0");
            p.action = "QUESTION_CREATE_FAILED";
            return p;
        }
        
        // Validate options based on type
        if ("SINGLE_CHOICE".equals(question.questionType) || "MCQ".equals(question.questionType)) {
            if (options == null || options.size() < 2) {
                Packet p = new Packet(false, "SINGLE_CHOICE phải có ít nhất 2 lựa chọn");
                p.action = "QUESTION_CREATE_FAILED";
                return p;
            }
            int correctCount = 0;
            for (QuestionOption opt : options) {
                if (opt.content == null || opt.content.trim().isEmpty()) {
                    return new Packet(false, "Nội dung lựa chọn không được để trống", "QUESTION_CREATE_FAILED");
                }
                if (opt.isCorrect) correctCount++;
            }
            if (correctCount != 1) {
                Packet p = new Packet(false, "SINGLE_CHOICE phải có đúng 1 đáp án đúng");
                p.action = "QUESTION_CREATE_FAILED";
                return p;
            }
        } else if ("TRUE_FALSE".equals(question.questionType)) {
            if (options == null || options.size() != 2) {
                Packet p = new Packet(false, "TRUE_FALSE phải có đúng 2 lựa chọn");
                p.action = "QUESTION_CREATE_FAILED";
                return p;
            }
            int correctCount = 0;
            for (QuestionOption opt : options) {
                if (opt.content == null || opt.content.trim().isEmpty()) {
                    Packet p = new Packet(false, "Nội dung lựa chọn không được để trống");
                    p.action = "QUESTION_CREATE_FAILED";
                    return p;
                }
                if (opt.isCorrect) correctCount++;
            }
            if (correctCount != 1) {
                Packet p = new Packet(false, "TRUE_FALSE phải có đúng 1 đáp án đúng");
                p.action = "QUESTION_CREATE_FAILED";
                return p;
            }
        }
        
        question.createdBy = creatorId;
        
        int questionId = QuestionDAO.createQuestion(question, options);
        if (questionId > 0) {
            Map<String, Object> data = new HashMap<>();
            data.put("questionId", questionId);
            Packet p = new Packet(true, "Tạo câu hỏi thành công", data);
            p.action = "QUESTION_CREATE_SUCCESS";
            return p;
        }
        
        Packet p = new Packet(false, "Lỗi server khi tạo câu hỏi");
        p.action = "QUESTION_CREATE_FAILED";
        return p;
    }

    public static Packet handleListQuestionsByBank(int bankId) {
        List<Question> questions = QuestionDAO.listQuestionsByBank(bankId);
        // We may not send isCorrect flag to client depending on the context, but since this is for Question Bank management (creator), it is okay to send it.
        // Actually, for getting full details, we could fetch options for each question here, or let the client request details per question.
        Packet p = new Packet(true, "Danh sách câu hỏi", questions);
        p.action = "QUESTION_LIST_BY_BANK_SUCCESS";
        return p;
    }

    public static Packet handleGetQuestionDetail(int questionId) {
        Question q = QuestionDAO.getQuestionById(questionId);
        if (q == null) {
            Packet p = new Packet(false, "Câu hỏi không tồn tại");
            p.action = "GET_QUESTION_FAILED";
            return p;
        }
        List<QuestionOption> options = QuestionDAO.getOptionsByQuestionId(questionId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("question", q);
        data.put("options", options);
        
        Packet p = new Packet(true, "Thông tin câu hỏi", data);
        p.action = "GET_QUESTION_SUCCESS";
        return p;
    }
}
