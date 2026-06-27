package com.mycompany.tutorhub_enterprise.client.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffBundle;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffQuestion;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class V2ExamHandoffService {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static V2ExamHandoffBundle buildHandoffBundleFromMap(Map<String, Object> data) throws Exception {
        if (data == null) {
            throw new Exception("Response data is null");
        }

        V2ExamHandoffBundle bundle = new V2ExamHandoffBundle();
        bundle.flow = (String) data.get("flow");
        
        if (data.get("examId") instanceof Number) {
            bundle.examId = ((Number) data.get("examId")).intValue();
        }
        if (data.get("paperId") instanceof Number) {
            bundle.paperId = ((Number) data.get("paperId")).intValue();
        }
        bundle.attemptId = (String) data.get("attemptId");
        bundle.sessionToken = (String) data.get("sessionToken");
        
        if (bundle.sessionToken != null && !bundle.sessionToken.isEmpty()) {
            bundle.sessionTokenPresent = true;
            bundle.sessionTokenMasked = "***";
            bundle.sessionTokenHash = hashString(bundle.sessionToken);
        } else {
            bundle.sessionTokenPresent = false;
        }

        bundle.deadlineAt = (String) data.get("deadlineAt");
        if (data.get("durationMinutes") instanceof Number) {
            bundle.durationMinutes = ((Number) data.get("durationMinutes")).intValue();
        }
        bundle.packageHash = (String) data.get("packageHash");
        if (data.get("questionCount") instanceof Number) {
            bundle.questionCount = ((Number) data.get("questionCount")).intValue();
        }
        if (data.get("totalScore") instanceof Number) {
            bundle.totalScore = ((Number) data.get("totalScore")).floatValue();
        }

        // Questions parsing
        Object questionsObj = data.get("questions");
        if (questionsObj != null) {
            String questionsJson = gson.toJson(questionsObj);
            V2ExamHandoffQuestion[] qArr = gson.fromJson(questionsJson, V2ExamHandoffQuestion[].class);
            bundle.questions = java.util.Arrays.asList(qArr);
        }

        bundle.clientBuild = "2I.9.5-3STEP-UX"; // standard
        bundle.createdAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date());

        return bundle;
    }

    private static String hashString(String input) {
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
            return "hash_error";
        }
    }

    public static void validateHandoffBundle(V2ExamHandoffBundle bundle, boolean debugMode) throws Exception {
        if (bundle == null) throw new Exception("Bundle is null");
        if (!"PAPER_START_V2".equals(bundle.flow)) throw new Exception("Invalid flow: " + bundle.flow);
        if (bundle.examId <= 0) throw new Exception("Invalid examId");
        if (bundle.paperId <= 0) throw new Exception("Invalid paperId");
        
        if (!debugMode) {
            if (bundle.attemptId == null || bundle.attemptId.isEmpty()) throw new Exception("attemptId required for non-debug mode");
            if (bundle.sessionToken == null || bundle.sessionToken.isEmpty()) throw new Exception("sessionToken required for non-debug mode");
        }
        
        if (bundle.packageHash == null || bundle.packageHash.isEmpty()) throw new Exception("packageHash is empty");
        if (bundle.questionCount <= 0) throw new Exception("questionCount <= 0");
        if (bundle.questions == null || bundle.questions.isEmpty()) throw new Exception("questions array is empty");
        
        // Security checks
        String json = gson.toJson(bundle);
        if (json.contains("\"isCorrect\"") || json.contains("\"answerKey\"") || json.contains("\"correctOption\"")) {
            throw new Exception("SECURITY VIOLATION: Bundle contains answer fields");
        }
        if (json.contains("\"grading_config\"")) {
            throw new Exception("SECURITY VIOLATION: Bundle contains grading config");
        }
        if (json.contains("\"sessionToken\"") && bundle.sessionToken != null && json.contains(bundle.sessionToken)) {
            throw new Exception("SECURITY VIOLATION: Bundle contains raw session token");
        }
        if (json.contains("\"password\"") || json.contains("\"passwordHash\"")) {
            throw new Exception("SECURITY VIOLATION: Bundle contains password");
        }
    }

    public static String computeHandoffHash(V2ExamHandoffBundle bundle) throws Exception {
        String json = gson.toJson(bundle);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(json.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String writeDebugHandoffArtifact(V2ExamHandoffBundle bundle) {
        try {
            String dirPath = System.getProperty("user.dir") + File.separator + "debug";
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, "v2_handoff_preview.json");
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(bundle, writer);
            }
            
            // Write hash file optionally
            File hashFile = new File(dir, "v2_handoff_preview.sha256");
            try (FileWriter writer = new FileWriter(hashFile)) {
                writer.write(computeHandoffHash(bundle));
            }
            
            return file.getAbsolutePath();
        } catch (Exception e) {
            System.err.println("[V2_HANDOFF_SERVICE] Failed to write debug artifact: " + e.getMessage());
            return null;
        }
    }
}
