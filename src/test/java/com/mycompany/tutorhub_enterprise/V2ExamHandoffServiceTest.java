package com.mycompany.tutorhub_enterprise;

import com.google.gson.Gson;
import com.mycompany.tutorhub_enterprise.client.services.V2ExamHandoffService;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffBundle;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class V2ExamHandoffServiceTest {

    @Test
    public void testValidHandoffBundle() throws Exception {
        Map<String, Object> mockData = new HashMap<>();
        mockData.put("flow", "PAPER_START_V2");
        mockData.put("examId", 3);
        mockData.put("paperId", 1);
        mockData.put("packageHash", "mockhash123");
        mockData.put("questionCount", 1);
        
        List<Map<String, Object>> questions = new ArrayList<>();
        Map<String, Object> q1 = new HashMap<>();
        q1.put("questionId", 101);
        q1.put("type", "MCQ");
        questions.add(q1);
        mockData.put("questions", questions);

        // Test debugMode = true (attemptId & sessionToken can be null)
        V2ExamHandoffBundle bundleDebug = V2ExamHandoffService.buildHandoffBundleFromMap(mockData);
        V2ExamHandoffService.validateHandoffBundle(bundleDebug, true);

        // Test debugMode = false (must throw exception if missing tokens)
        try {
            V2ExamHandoffService.validateHandoffBundle(bundleDebug, false);
            fail("Should have thrown exception");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("attemptId required"));
        }

        // Fix missing fields for non-debug mode
        mockData.put("attemptId", "mock-attempt-uuid");
        mockData.put("sessionToken", "mock-session-token");
        V2ExamHandoffBundle bundleProd = V2ExamHandoffService.buildHandoffBundleFromMap(mockData);
        V2ExamHandoffService.validateHandoffBundle(bundleProd, false);

        // Test artifact writing
        String path = V2ExamHandoffService.writeDebugHandoffArtifact(bundleDebug);
        assertNotNull(path);
        File f = new File(path);
        assertTrue(f.exists());
        f.delete();
        new File(path.replace(".json", ".sha256")).delete();
    }

    @Test
    public void testSecurityValidation() throws Exception {
        Map<String, Object> mockData = new HashMap<>();
        mockData.put("flow", "PAPER_START_V2");
        mockData.put("examId", 3);
        mockData.put("paperId", 1);
        mockData.put("packageHash", "mockhash");
        mockData.put("questionCount", 1);
        mockData.put("sessionToken", "RAW_SECRET_TOKEN");
        mockData.put("attemptId", "ATTEMPT_UUID");
        
        List<Map<String, Object>> questions = new ArrayList<>();
        Map<String, Object> q1 = new HashMap<>();
        q1.put("questionId", 101);
        q1.put("type", "MCQ");
        q1.put("isCorrect", true); // DANGEROUS LEAK
        questions.add(q1);
        mockData.put("questions", questions);

        // The DTO mapping should automatically strip unknown fields like isCorrect
        V2ExamHandoffBundle bundle = V2ExamHandoffService.buildHandoffBundleFromMap(mockData);
        V2ExamHandoffService.validateHandoffBundle(bundle, false); // Prod mode
        
        Gson gson = new Gson();
        String json = gson.toJson(bundle);
        assertFalse(json.contains("\"isCorrect\""), "isCorrect must be stripped by DTO");
        assertFalse(json.contains("RAW_SECRET_TOKEN"), "Raw token must not be serialized");
        assertTrue(json.contains("\"sessionTokenPresent\":true") || json.contains("\"sessionTokenPresent\": true"), "Must contain token flag");
        assertTrue(json.contains("\"sessionTokenMasked\":\"***\"") || json.contains("\"sessionTokenMasked\": \"***\""), "Must contain masked token");
    }
}
