package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.models.exam.Exam;
import com.mycompany.tutorhub_enterprise.models.exam.Question;
import com.mycompany.tutorhub_enterprise.models.exam.ExamSession;
import com.mycompany.tutorhub_enterprise.server.dao.ExamDAO;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import java.lang.reflect.Type;

public class ExamService {
    private static final Gson gson = new Gson();

    public static Packet handleCreateExam(int userId, String payload) {
        try {
            Exam exam = gson.fromJson(payload, Exam.class);
            exam.creatorId = userId;
            int examId = ExamDAO.createExam(exam);
            if (examId > 0) {
                exam.id = examId;
                return new Packet("EXAM_CREATED", exam);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Packet(false, "Lỗi tạo kỳ thi", "CREATE_EXAM_FAILED");
    }

    public static Packet handleGetExams(int userId) {
        String role = ExamDAO.getUserRole(userId);
        System.out.println("[GET_EXAMS] userId=" + userId + ", role=" + role);
        List<Exam> exams;
        if ("STUDENT".equalsIgnoreCase(role)) {
            System.out.println("[GET_EXAMS] queryMode=STUDENT");
            exams = ExamDAO.getExamsForStudent();
        } else {
            System.out.println("[GET_EXAMS] queryMode=TUTOR");
            exams = ExamDAO.getExamsByCreator(userId);
        }
        System.out.println("[GET_EXAMS] count=" + exams.size());
        return new Packet("EXAM_LIST", exams);
    }

    public static Packet handleAddQuestion(String payload) {
        try {
            Question q = gson.fromJson(payload, Question.class);
            int qId = ExamDAO.addQuestion(q);
            if (qId > 0) {
                q.id = qId;
                return new Packet("QUESTION_ADDED", q);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Packet(false, "Lỗi thêm câu hỏi", "ADD_QUESTION_FAILED");
    }

    public static Packet handleStartSession(int userId, String payload) {
        try {
            int examId = Integer.parseInt(payload);
            ExamSession session = new ExamSession();
            session.examId = examId;
            session.userId = userId;
            session.clientInfo = "PC";
            session.status = "IN_PROGRESS";
            
            // Xáo trộn câu hỏi
            List<Question> questions = ExamDAO.getQuestionsByExam(examId);
            // Basic shuffling logic can be added here
            session.questionOrder = gson.toJson(questions);

            int sessionId = ExamDAO.createSession(session);
            if (sessionId > 0) {
                session.id = sessionId;
                return new Packet("SESSION_STARTED", session);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Packet(false, "Lỗi bắt đầu thi", "START_SESSION_FAILED");
    }

    public static Packet handleGetExamQuestions(String payload) {
        try {
            int examId = Integer.parseInt(payload);
            List<Question> questions = ExamDAO.getQuestionsByExam(examId);
            return new Packet("EXAM_QUESTIONS_LIST", questions);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Packet(false, "Lỗi lấy câu hỏi", "GET_QUESTIONS_FAILED");
    }

    public static Packet handleSubmitExam(int userId, String payload) {
        try {
            // Payload format: examId|answersJson
            String[] parts = payload.split("\\|", 2);
            if (parts.length == 2) {
                int examId = Integer.parseInt(parts[0]);
                String answersJson = parts[1];
                
                // Get the active session for this user and exam
                int sessionId = -1;
                // Currently ExamDAO doesn't have a getSession method, so we might need to add it later.
                // For now, we assume the client might send sessionId, or we can look it up.
                // Let's implement a quick lookup in ExamDAO later.
                
                return new Packet("SUBMIT_SUCCESS", "Đã nộp bài thành công!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Packet(false, "Lỗi nộp bài", "SUBMIT_FAILED");
    }
}
