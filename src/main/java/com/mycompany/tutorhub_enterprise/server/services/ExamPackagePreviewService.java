package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.models.exam.*;
import com.mycompany.tutorhub_enterprise.server.dao.ExamDAO;
import com.mycompany.tutorhub_enterprise.server.dao.ExamPaperDAO;
import com.mycompany.tutorhub_enterprise.server.dao.QuestionDAO;

import java.util.List;

public class ExamPackagePreviewService {

    public static Packet handlePreviewByExamId(int examId) {
        Exam exam = ExamDAO.getExamById(examId);
        if (exam == null) {
            return new Packet(false, "Kỳ thi không tồn tại", "EXAM_PACKAGE_PREVIEW_FAILED");
        }

        Integer paperId = ExamDAO.getAssignedPaperId(examId);
        if (paperId == null) {
            Packet p = new Packet(false, "Exam has no assigned paper");
            p.action = "EXAM_PACKAGE_PREVIEW_FAILED";
            // Return specific error code per requirements
            // However, our Packet model only has success, message, data.
            // I'll append the error code to the message or use a custom map if needed.
            // But standard packet expects the UI to handle it. Let's just return a map if we want to include errorCode,
            // or just use message since action is a failure.
            // Wait, the user specifically asked for:
            // "errorCode": "EXAM_HAS_NO_ASSIGNED_PAPER"
            // We can return a Map in data for error, but standard Packet sets success=false and message.
            // I will use message = "EXAM_HAS_NO_ASSIGNED_PAPER" for strict matching.
            p.message = "EXAM_HAS_NO_ASSIGNED_PAPER";
            return p;
        }

        return buildPreview(exam, paperId);
    }

    public static Packet handlePreviewByPaperId(int paperId) {
        return buildPreview(null, paperId);
    }

    private static Packet buildPreview(Exam exam, int paperId) {
        ExamPaper paper = ExamPaperDAO.getExamPaperById(paperId);
        if (paper == null) {
            Packet p = new Packet(false, "Đề thi không tồn tại");
            p.action = "EXAM_PACKAGE_PREVIEW_FAILED";
            p.message = "PAPER_NOT_FOUND";
            return p;
        }

        List<ExamPaperQuestion> paperQuestions = ExamPaperDAO.listQuestionsByPaper(paperId);
        if (paperQuestions == null || paperQuestions.isEmpty()) {
            Packet p = new Packet(false, "Đề thi chưa có câu hỏi");
            p.action = "EXAM_PACKAGE_PREVIEW_FAILED";
            p.message = "PAPER_HAS_NO_QUESTIONS";
            return p;
        }

        ExamPackagePreview preview = new ExamPackagePreview();
        preview.paperId = paperId;
        preview.paperTitle = paper.title;
        preview.questionCount = paperQuestions.size();
        preview.totalScore = paper.totalScore;

        if (exam != null) {
            preview.examId = exam.id;
            preview.examTitle = exam.title;
            preview.durationMinutes = exam.durationMins;
        } else {
            preview.examId = -1;
            preview.examTitle = "Preview Mode";
            preview.durationMinutes = 0;
        }

        for (ExamPaperQuestion epq : paperQuestions) {
            Question q = QuestionDAO.getQuestionById(epq.questionId);
            if (q == null) continue;

            ExamPackageQuestion pkgQ = new ExamPackageQuestion();
            pkgQ.questionId = q.id;
            pkgQ.type = q.questionType;
            pkgQ.content = q.content;
            pkgQ.score = epq.score;
            pkgQ.orderIndex = epq.orderIndex;
            pkgQ.required = epq.required;

            if ("MCQ".equals(q.questionType) || "SINGLE_CHOICE".equals(q.questionType) || "MULTIPLE_CHOICE".equals(q.questionType) || "TRUE_FALSE".equals(q.questionType)) {
                List<QuestionOption> options = QuestionDAO.getOptionsByQuestionId(q.id);
                if (options != null) {
                    for (QuestionOption opt : options) {
                        ExamPackageOption pkgOpt = new ExamPackageOption();
                        pkgOpt.optionId = opt.id;
                        pkgOpt.optionLabel = opt.optionLabel;
                        pkgOpt.content = opt.content;
                        pkgOpt.orderIndex = opt.orderIndex;
                        // isCorrect is NOT copied
                        pkgQ.options.add(pkgOpt);
                    }
                }
            }

            preview.questions.add(pkgQ);
        }

        Packet p = new Packet(true, "Lấy preview thành công", preview);
        p.action = "EXAM_PACKAGE_PREVIEW_SUCCESS";
        return p;
    }
}
