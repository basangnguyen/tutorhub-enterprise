package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.models.exam.Exam;
import com.mycompany.tutorhub_enterprise.models.exam.ExamPaper;
import com.mycompany.tutorhub_enterprise.server.dao.ExamDAO;
import com.mycompany.tutorhub_enterprise.server.dao.ExamPaperDAO;

import java.util.HashMap;
import java.util.Map;

public class ExamAssignmentService {

    public static Packet handleAssignPaperToExam(int userId, int examId, int paperId) {
        Exam exam = ExamDAO.getExamById(examId);
        if (exam == null) {
            Packet p = new Packet(false, "Kỳ thi không tồn tại");
            p.action = "EXAM_ASSIGN_PAPER_FAILED";
            return p;
        }
        if (exam.creatorId != userId) {
            Packet p = new Packet(false, "Bạn không có quyền sửa kỳ thi này");
            p.action = "EXAM_ASSIGN_PAPER_FAILED";
            return p;
        }

        // HF-3: Chỉ block khi ARCHIVED, cho phép gán khi DRAFT hoặc ACTIVE
        if ("ARCHIVED".equals(exam.status)) {
            Packet p = new Packet(false, "Không thể gán đề thi cho kỳ thi đã lưu trữ (ARCHIVED)");
            p.action = "EXAM_ASSIGN_PAPER_FAILED";
            return p;
        }

        ExamPaper paper = ExamPaperDAO.getExamPaperById(paperId);
        if (paper == null) {
            Packet p = new Packet(false, "Đề thi không tồn tại");
            p.action = "EXAM_ASSIGN_PAPER_FAILED";
            return p;
        }
        if ("ARCHIVED".equals(paper.status)) {
            Packet p = new Packet(false, "Không thể gán đề thi đã lưu trữ (ARCHIVED)");
            p.action = "EXAM_ASSIGN_PAPER_FAILED";
            return p;
        }

        boolean ok = ExamDAO.assignPaperToExam(examId, paperId);
        if (ok) {
            Map<String, Object> data = new HashMap<>();
            data.put("examId", examId);
            data.put("paperId", paperId);
            Packet p = new Packet(true, "Gán đề thi thành công", data);
            p.action = "EXAM_ASSIGN_PAPER_SUCCESS";
            return p;
        }

        Packet p = new Packet(false, "Lỗi server khi gán đề thi");
        p.action = "EXAM_ASSIGN_PAPER_FAILED";
        return p;
    }

    public static Packet handleUnassignPaperFromExam(int userId, int examId) {
        Exam exam = ExamDAO.getExamById(examId);
        if (exam == null) {
            Packet p = new Packet(false, "Kỳ thi không tồn tại");
            p.action = "EXAM_UNASSIGN_PAPER_FAILED";
            return p;
        }
        if (exam.creatorId != userId) {
            Packet p = new Packet(false, "Bạn không có quyền sửa kỳ thi này");
            p.action = "EXAM_UNASSIGN_PAPER_FAILED";
            return p;
        }

        // HF-3: Chỉ block khi ARCHIVED
        if ("ARCHIVED".equals(exam.status)) {
            Packet p = new Packet(false, "Không thể bỏ gán đề thi của kỳ thi đã lưu trữ (ARCHIVED)");
            p.action = "EXAM_UNASSIGN_PAPER_FAILED";
            return p;
        }

        boolean ok = ExamDAO.unassignPaperFromExam(examId);
        if (ok) {
            Map<String, Object> data = new HashMap<>();
            data.put("examId", examId);
            Packet p = new Packet(true, "Bỏ gán đề thi thành công", data);
            p.action = "EXAM_UNASSIGN_PAPER_SUCCESS";
            return p;
        }

        Packet p = new Packet(false, "Lỗi server khi bỏ gán đề thi");
        p.action = "EXAM_UNASSIGN_PAPER_FAILED";
        return p;
    }

    public static Packet handleGetAssignedPaper(int userId, int examId) {
        Exam exam = ExamDAO.getExamById(examId);
        if (exam == null) {
            Packet p = new Packet(false, "Kỳ thi không tồn tại");
            p.action = "EXAM_GET_ASSIGNED_PAPER_FAILED";
            return p;
        }
        // Cho phép lấy thông tin nếu có quyền xem (ở đây tạm cho creator)
        if (exam.creatorId != userId) {
            Packet p = new Packet(false, "Bạn không có quyền xem kỳ thi này");
            p.action = "EXAM_GET_ASSIGNED_PAPER_FAILED";
            return p;
        }

        Integer paperId = ExamDAO.getAssignedPaperId(examId);
        Map<String, Object> data = new HashMap<>();
        data.put("examId", examId);
        data.put("paperId", paperId);

        Packet p = new Packet(true, "Lấy thông tin đề thi được gán thành công", data);
        p.action = "EXAM_GET_ASSIGNED_PAPER_SUCCESS";
        return p;
    }
}
