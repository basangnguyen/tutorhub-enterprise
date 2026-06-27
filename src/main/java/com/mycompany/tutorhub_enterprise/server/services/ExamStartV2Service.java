package com.mycompany.tutorhub_enterprise.server.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.models.exam.Exam;
import com.mycompany.tutorhub_enterprise.models.exam.ExamPaper;
import com.mycompany.tutorhub_enterprise.models.exam.ExamPaperQuestion;
import com.mycompany.tutorhub_enterprise.models.exam.Question;
import com.mycompany.tutorhub_enterprise.models.exam.QuestionOption;
import com.mycompany.tutorhub_enterprise.server.dao.ExamAttemptDAO;
import com.mycompany.tutorhub_enterprise.server.dao.ExamDAO;
import com.mycompany.tutorhub_enterprise.server.dao.ExamPaperDAO;
import com.mycompany.tutorhub_enterprise.server.dao.QuestionDAO;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExamStartV2Service {

    private static boolean isPaperStartV2Enabled() {
        return Boolean.parseBoolean(System.getProperty("tse.paperStartV2.enabled", "false"));
    }

    private static final SecureRandom secureRandom = new SecureRandom();

    public static Packet handleStartRequestV2(int userId, String role, String jsonPayload) {
        Packet res = new Packet();
        res.action = "EXAM_START_RESPONSE_V2";
        
        Gson gson = new GsonBuilder().serializeNulls().create();
        Map<String, Object> reqMap = null;
        try {
            reqMap = gson.fromJson(jsonPayload, Map.class);
        } catch (Exception e) {
            res.success = false;
            res.message = "INVALID_REQUEST";
            return res;
        }

        if (reqMap == null) {
            res.success = false;
            res.message = "INVALID_REQUEST";
            return res;
        }

        String requestId = (String) reqMap.get("requestId");
        res.payload = requestId; // Use payload to hold requestId for client tracking

        if (!isPaperStartV2Enabled()) {
            res.success = false;
            res.message = "FEATURE_DISABLED";
            return res;
        }

        Map<String, Object> reqData = (Map<String, Object>) reqMap.get("data");
        if (reqData == null) {
            res.success = false;
            res.message = "INVALID_REQUEST";
            return res;
        }

        Object examIdObj = reqData.get("examId");
        if (examIdObj == null) {
            res.success = false;
            res.message = "EXAM_NOT_FOUND";
            return res;
        }

        int examId = ((Number) examIdObj).intValue();
        String password = (String) reqData.get("password");
        
        boolean debugMode = false;
        if (reqData.containsKey("debugMode") && reqData.get("debugMode") instanceof Boolean) {
            debugMode = (Boolean) reqData.get("debugMode");
        }

        // 1. Validate exam exists
        Exam exam = ExamDAO.getExamById(examId);
        if (exam == null) {
            res.success = false;
            res.message = "EXAM_NOT_FOUND";
            return res;
        }

        // 2. Validate exam is active
        if (!"ACTIVE".equals(exam.status)) {
            res.success = false;
            res.message = "EXAM_NOT_ACTIVE";
            return res;
        }

        // 3. Validate password (matching legacy flow behavior)
        boolean requiresPassword = false;
        String expectedPassword = "";
        if (exam.securityConfig != null && !exam.securityConfig.trim().isEmpty()) {
            try {
                Map<String, Object> secMap = gson.fromJson(exam.securityConfig, Map.class);
                if (secMap.containsKey("require_password") && secMap.get("require_password") instanceof Boolean) {
                    requiresPassword = (Boolean) secMap.get("require_password");
                }
                if (secMap.containsKey("password")) {
                    expectedPassword = String.valueOf(secMap.get("password"));
                }
            } catch (Exception ex) {
                System.err.println("[TSE_DB_V2] Parse securityConfig error examId=" + exam.id);
            }
        }
        
        if (requiresPassword) {
            if (password == null || !password.equals(expectedPassword)) {
                res.success = false;
                res.message = "INVALID_PASSWORD";
                return res;
            }
        }

        // 4. Validate exam has assigned paper
        Integer paperId = ExamDAO.getAssignedPaperId(examId);
        if (paperId == null) {
            res.success = false;
            res.message = "EXAM_HAS_NO_ASSIGNED_PAPER";
            return res;
        }

        // 5. Validate paper exists
        ExamPaper paper = ExamPaperDAO.getExamPaperById(paperId);
        if (paper == null) {
            res.success = false;
            res.message = "PAPER_NOT_FOUND";
            return res;
        }

        // 6. Validate paper has questions
        List<ExamPaperQuestion> paperQuestions = ExamPaperDAO.listQuestionsByPaper(paperId);
        if (paperQuestions == null || paperQuestions.isEmpty()) {
            res.success = false;
            res.message = "PAPER_HAS_NO_QUESTIONS";
            return res;
        }

        // 7. Build V2 package core data
        Map<String, Object> packageData = new HashMap<>();
        packageData.put("flow", "PAPER_START_V2");
        packageData.put("examId", exam.id);
        packageData.put("paperId", paper.id);
        packageData.put("packageVersion", "tutorhub_exam_package_v2");
        packageData.put("durationMinutes", exam.durationMins);
        packageData.put("questionCount", paperQuestions.size());
        packageData.put("totalScore", paper.totalScore);
        
        List<Map<String, Object>> questionsList = new ArrayList<>();
        
        for (ExamPaperQuestion epq : paperQuestions) {
            Question q = QuestionDAO.getQuestionById(epq.questionId);
            if (q == null) continue;

            Map<String, Object> qMap = new HashMap<>();
            qMap.put("questionId", q.id);
            qMap.put("type", q.questionType);
            qMap.put("content", q.content);
            qMap.put("score", epq.score);
            qMap.put("orderIndex", epq.orderIndex);
            qMap.put("required", epq.required);

            if ("MCQ".equals(q.questionType) || "SINGLE_CHOICE".equals(q.questionType) || "MULTIPLE_CHOICE".equals(q.questionType) || "TRUE_FALSE".equals(q.questionType)) {
                List<QuestionOption> options = QuestionDAO.getOptionsByQuestionId(q.id);
                List<Map<String, Object>> optsList = new ArrayList<>();
                if (options != null) {
                    for (QuestionOption opt : options) {
                        Map<String, Object> oMap = new HashMap<>();
                        oMap.put("optionId", opt.id);
                        oMap.put("optionLabel", opt.optionLabel);
                        oMap.put("content", opt.content);
                        oMap.put("orderIndex", opt.orderIndex);
                        // Security: NEVER put isCorrect or answerKey
                        optsList.add(oMap);
                    }
                }
                qMap.put("options", optsList);
            }
            questionsList.add(qMap);
        }
        
        packageData.put("questions", questionsList);
        
        // 8. Calculate packageHash
        String packageJsonForHash = gson.toJson(packageData);
        String packageHash = computeSHA256(packageJsonForHash);
        packageData.put("packageHash", packageHash);
        packageData.put("debugMode", debugMode);

        if (debugMode) {
            packageData.put("attemptCreated", false);
        } else {
            packageData.put("attemptCreated", true);
            
            // 9. Generate secure session token (32 bytes -> base64 url safe)
            byte[] tokenBytes = new byte[32];
            secureRandom.nextBytes(tokenBytes);
            String sessionToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
            String sessionTokenHash = computeSHA256(sessionToken);
            
            Timestamp deadlineAt = null;
            if (exam.durationMins > 0) {
                deadlineAt = new Timestamp(System.currentTimeMillis() + (exam.durationMins * 60L * 1000L));
            }
            
            String clientInfoJson = gson.toJson(reqData.get("clientInfo"));
            if (clientInfoJson == null || "null".equals(clientInfoJson)) {
                clientInfoJson = "{}";
            }
            
            try {
                String attemptId = ExamAttemptDAO.createAttemptV2(
                        exam.id, paper.id, userId, sessionTokenHash, packageHash, deadlineAt, clientInfoJson);
                        
                packageData.put("attemptId", attemptId);
                packageData.put("sessionToken", sessionToken); // Sent exactly once to client
                if (deadlineAt != null) {
                    packageData.put("deadlineAt", deadlineAt.toString()); // For client reference
                }
                
            } catch (Exception e) {
                System.err.println("[TSE_V2] Failed to create attempt: " + e.getMessage());
                res.success = false;
                res.message = "INTERNAL_SERVER_ERROR";
                return res;
            }
        }

        res.success = true;
        res.data = packageData;
        
        return res;
    }
    
    private static String computeSHA256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
