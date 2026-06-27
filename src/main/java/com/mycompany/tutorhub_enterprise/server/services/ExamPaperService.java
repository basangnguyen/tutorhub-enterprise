package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.models.exam.ExamPaper;
import com.mycompany.tutorhub_enterprise.models.exam.ExamPaperQuestion;
import com.mycompany.tutorhub_enterprise.models.exam.Question;
import com.mycompany.tutorhub_enterprise.server.dao.ExamPaperDAO;
import com.mycompany.tutorhub_enterprise.server.dao.QuestionDAO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExamPaperService {

    public static Packet handleCreateExamPaper(int creatorId, String title, String description) {
        if (title == null || title.trim().isEmpty()) {
            Packet p = new Packet(false, "Tiêu đề đề thi không được để trống");
            p.action = "CREATE_PAPER_FAILED";
            return p;
        }

        int paperId = ExamPaperDAO.createExamPaper(title, description, creatorId);
        if (paperId > 0) {
            Map<String, Object> data = new HashMap<>();
            data.put("paperId", paperId);
            Packet p = new Packet(true, "Tạo đề thi thành công", data);
            p.action = "CREATE_PAPER_SUCCESS";
            return p;
        }

        Packet p = new Packet(false, "Lỗi server khi tạo đề thi");
        p.action = "CREATE_PAPER_FAILED";
        return p;
    }

    public static Packet handleListExamPapers(int creatorId) {
        List<ExamPaper> papers = ExamPaperDAO.listExamPapersByCreator(creatorId);
        Packet p = new Packet(true, "Danh sách đề thi", papers);
        p.action = "LIST_PAPERS_SUCCESS";
        return p;
    }

    public static Packet handleGetExamPaperDetail(int paperId) {
        ExamPaper paper = ExamPaperDAO.getExamPaperById(paperId);
        if (paper == null) {
            Packet p = new Packet(false, "Đề thi không tồn tại");
            p.action = "GET_PAPER_FAILED";
            return p;
        }

        Packet p = new Packet(true, "Thông tin đề thi", paper);
        p.action = "GET_PAPER_SUCCESS";
        return p;
    }

    public static Packet handleAddQuestionToPaper(int creatorId, int paperId, int questionId, float score, int orderIndex, boolean required) {
        ExamPaper paper = ExamPaperDAO.getExamPaperById(paperId);
        if (paper == null) {
            Packet p = new Packet(false, "Đề thi không tồn tại");
            p.action = "ADD_QUESTION_FAILED";
            return p;
        }
        if (paper.creatorId != creatorId) {
            Packet p = new Packet(false, "Bạn không có quyền sửa đề thi này");
            p.action = "ADD_QUESTION_FAILED";
            return p;
        }
        if ("PUBLISHED".equals(paper.status)) {
            Packet p = new Packet(false, "Không thể sửa đề thi đã xuất bản");
            p.action = "ADD_QUESTION_FAILED";
            return p;
        }

        Question q = QuestionDAO.getQuestionById(questionId);
        if (q == null) {
            Packet p = new Packet(false, "Câu hỏi không tồn tại");
            p.action = "ADD_QUESTION_FAILED";
            return p;
        }

        if (score <= 0) {
            Packet p = new Packet(false, "Điểm câu hỏi phải lớn hơn 0");
            p.action = "ADD_QUESTION_FAILED";
            return p;
        }

        if (orderIndex < 0) {
            Packet p = new Packet(false, "Thứ tự không hợp lệ");
            p.action = "ADD_QUESTION_FAILED";
            return p;
        }

        boolean ok = ExamPaperDAO.addQuestionToPaper(paperId, questionId, score, orderIndex, required);
        if (ok) {
            Packet p = new Packet(true, "Thêm câu hỏi vào đề thành công");
            p.action = "ADD_QUESTION_SUCCESS";
            return p;
        }

        Packet p = new Packet(false, "Lỗi server khi thêm câu hỏi");
        p.action = "ADD_QUESTION_FAILED";
        return p;
    }

    public static Packet handleRemoveQuestionFromPaper(int creatorId, int paperId, int questionId) {
        ExamPaper paper = ExamPaperDAO.getExamPaperById(paperId);
        if (paper == null) {
            Packet p = new Packet(false, "Đề thi không tồn tại");
            p.action = "REMOVE_QUESTION_FAILED";
            return p;
        }
        if (paper.creatorId != creatorId) {
            Packet p = new Packet(false, "Bạn không có quyền sửa đề thi này");
            p.action = "REMOVE_QUESTION_FAILED";
            return p;
        }
        if ("PUBLISHED".equals(paper.status)) {
            Packet p = new Packet(false, "Không thể sửa đề thi đã xuất bản");
            p.action = "REMOVE_QUESTION_FAILED";
            return p;
        }

        boolean ok = ExamPaperDAO.removeQuestionFromPaper(paperId, questionId);
        if (ok) {
            Packet p = new Packet(true, "Xóa câu hỏi khỏi đề thành công");
            p.action = "REMOVE_QUESTION_SUCCESS";
            return p;
        }

        Packet p = new Packet(false, "Lỗi server khi xóa câu hỏi");
        p.action = "REMOVE_QUESTION_FAILED";
        return p;
    }

    public static Packet handleListQuestionsInPaper(int paperId) {
        List<ExamPaperQuestion> list = ExamPaperDAO.listQuestionsByPaper(paperId);
        Packet p = new Packet(true, "Danh sách câu hỏi trong đề", list);
        p.action = "LIST_PAPER_QUESTIONS_SUCCESS";
        return p;
    }
}
