package com.mycompany.tutorhub_enterprise;

import com.google.gson.Gson;
import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.server.db.ExamDatabaseManager;
import com.mycompany.tutorhub_enterprise.server.services.ExamStartV2Service;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;

@Disabled("Requires a real PostgreSQL connection or mock setup")
public class ExamStartV2SessionSmokeTest {

    private static final int MOCK_USER_ID = 999;
    private static final int MOCK_EXAM_ID = 3;

    @BeforeAll
    public static void setup() {
        // Initialize DB
        ExamDatabaseManager.initialize();
        System.setProperty("tse.paperStartV2.enabled", "true");
    }

    @Test
    public void testDebugModeTrue_DoesNotCreateAttempt() {
        System.setProperty("tse.paperStartV2.enabled", "true");
        Map<String, Object> reqData = new HashMap<>();
        reqData.put("examId", MOCK_EXAM_ID);
        reqData.put("debugMode", true);

        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("requestId", "debug-true-req");
        reqMap.put("data", reqData);

        String jsonPayload = new Gson().toJson(reqMap);

        Packet response = ExamStartV2Service.handleStartRequestV2(MOCK_USER_ID, "STUDENT", jsonPayload);
        
        assertTrue(response.success);
        Map<String, Object> data = (Map<String, Object>) response.data;
        assertEquals("PAPER_START_V2", data.get("flow"));
        assertEquals(true, data.get("debugMode"));
        assertEquals(false, data.get("attemptCreated"));
        assertNull(data.get("attemptId"));
        assertNull(data.get("sessionToken"));
    }

    @Test
    public void testDebugModeFalse_CreatesAttempt() {
        System.setProperty("tse.paperStartV2.enabled", "true");
        Map<String, Object> reqData = new HashMap<>();
        reqData.put("examId", MOCK_EXAM_ID);
        reqData.put("debugMode", false);

        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("requestId", "debug-false-req");
        reqMap.put("data", reqData);

        String jsonPayload = new Gson().toJson(reqMap);

        Packet response = ExamStartV2Service.handleStartRequestV2(MOCK_USER_ID, "STUDENT", jsonPayload);
        
        assertTrue(response.success);
        Map<String, Object> data = (Map<String, Object>) response.data;
        assertEquals("PAPER_START_V2", data.get("flow"));
        assertEquals(false, data.get("debugMode"));
        assertEquals(true, data.get("attemptCreated"));
        
        String attemptId = (String) data.get("attemptId");
        String sessionToken = (String) data.get("sessionToken");
        String packageHash = (String) data.get("packageHash");
        
        assertNotNull(attemptId);
        assertNotNull(sessionToken);
        assertNotNull(packageHash);
        
        // Check DB
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT * FROM exam_attempts WHERE id = ?";
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setString(1, attemptId);
                try (ResultSet rs = st.executeQuery()) {
                    assertTrue(rs.next(), "Attempt should exist in DB");
                    assertEquals(MOCK_EXAM_ID, rs.getInt("exam_id"));
                    assertEquals(MOCK_USER_ID, rs.getInt("user_id"));
                    assertEquals("STARTING", rs.getString("status"));
                    
                    String dbTokenHash = rs.getString("session_token_hash");
                    assertNotNull(dbTokenHash);
                    assertNotEquals(sessionToken, dbTokenHash, "Raw token should not be in DB");
                    assertEquals(packageHash, rs.getString("package_hash"));
                }
            }
        } catch (Exception e) {
            fail("DB check failed: " + e.getMessage());
        }
    }

    @Test
    public void testFeatureDisabled() {
        System.setProperty("tse.paperStartV2.enabled", "false");
        Map<String, Object> reqData = new HashMap<>();
        reqData.put("examId", MOCK_EXAM_ID);
        reqData.put("debugMode", false);

        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("requestId", "disabled-req");
        reqMap.put("data", reqData);

        String jsonPayload = new Gson().toJson(reqMap);

        Packet response = ExamStartV2Service.handleStartRequestV2(MOCK_USER_ID, "STUDENT", jsonPayload);
        
        assertFalse(response.success);
        assertEquals("FEATURE_DISABLED", response.message);
        
        // Re-enable for other tests
        System.setProperty("tse.paperStartV2.enabled", "true");
    }
}
