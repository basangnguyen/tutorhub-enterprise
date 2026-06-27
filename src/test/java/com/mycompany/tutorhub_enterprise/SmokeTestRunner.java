package com.mycompany.tutorhub_enterprise;

import com.google.gson.Gson;
import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.server.db.ExamDatabaseManager;
import com.mycompany.tutorhub_enterprise.server.services.ExamStartV2Service;

import java.util.HashMap;
import java.util.Map;

public class SmokeTestRunner {

    private static final Gson gson = new Gson();

    private static String createPayload(int examId, String password) {
        Map<String, Object> reqData = new HashMap<>();
        reqData.put("examId", examId);
        if (password != null) {
            reqData.put("password", password);
        }
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("requestId", "req-123");
        payloadMap.put("data", reqData);
        return gson.toJson(payloadMap);
    }

    public static void main(String[] args) {
        ExamDatabaseManager.initialize();
        
        System.out.println("========== SMOKE TEST RUNNER ==========");
        System.out.println("FLAG tse.paperStartV2.enabled = " + System.getProperty("tse.paperStartV2.enabled"));
        
        if ("false".equals(System.getProperty("tse.paperStartV2.enabled", "false"))) {
            Packet res = ExamStartV2Service.handleStartRequestV2(1, "STUDENT", createPayload(3, null));
            System.out.println("FLAG_FALSE_TEST: success=" + res.success + " message=" + res.message);
        } else {
            // EXAM_NOT_FOUND
            Packet res1 = ExamStartV2Service.handleStartRequestV2(1, "STUDENT", createPayload(99999, null));
            System.out.println("EXAM_NOT_FOUND_TEST: success=" + res1.success + " message=" + res1.message);
            
            // EXAM_HAS_NO_ASSIGNED_PAPER (Exam ID 1)
            Packet res2 = ExamStartV2Service.handleStartRequestV2(1, "STUDENT", createPayload(1, null));
            System.out.println("NO_PAPER_TEST: success=" + res2.success + " message=" + res2.message);
            
            // Force Assign Paper to Exam 3 for success test
            try {
                int paperId = com.mycompany.tutorhub_enterprise.server.dao.ExamPaperDAO.createExamPaper("Mock Paper", "Test", 1);
                com.mycompany.tutorhub_enterprise.server.dao.ExamPaperDAO.addQuestionToPaper(paperId, 1, 10.0f, 1, true);
                com.mycompany.tutorhub_enterprise.server.dao.ExamDAO.assignPaperToExam(3, paperId);
            } catch (Exception e) {
                System.out.println("Could not prepare mock data: " + e.getMessage());
            }

            // SUCCESS (Exam ID 3)
            Packet res3 = ExamStartV2Service.handleStartRequestV2(1, "STUDENT", createPayload(3, null));
            System.out.println("SUCCESS_TEST: success=" + res3.success + " message=" + res3.message);
            
            if (res3.success) {
                Map<String, Object> data = (Map<String, Object>) res3.data;
                System.out.println("Flow: " + data.get("flow"));
                System.out.println("ExamId: " + data.get("examId"));
                System.out.println("PaperId: " + data.get("paperId"));
                System.out.println("QuestionCount: " + data.get("questionCount"));
                
                String json = gson.toJson(data);
                System.out.println("LEAK_IS_CORRECT: " + json.contains("isCorrect"));
                System.out.println("LEAK_ANSWER_KEY: " + json.contains("answerKey"));
                System.out.println("LEAK_CORRECT_OPTION: " + json.contains("correctOption"));
                System.out.println("LEAK_PASSWORD: " + json.contains("password"));
                System.out.println("LEAK_GRADING_CONFIG: " + json.contains("grading_config"));
            }
        }
    }
}
