package com.mycompany.tutorhub_enterprise;

import com.google.gson.Gson;
import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;
import com.mycompany.tutorhub_enterprise.server.db.ExamDatabaseManager;
import com.mycompany.tutorhub_enterprise.server.services.ExamStartV2Service;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;

@Disabled("Requires real PostgreSQL DB")
public class ExamStartV2SecurityRegressionTest {

    private static final int MOCK_USER_ID = 100;
    private static final int MOCK_EXAM_ID = 3;

    @BeforeAll
    public static void setup() {
        ExamDatabaseManager.initialize();
        System.setProperty("tse.paperStartV2.enabled", "true");
    }

    @Test
    public void testSchemaHasCorrectColumns() throws Exception {
        try (Connection conn = DatabaseManager.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getColumns(null, null, "exam_attempts", null);
            List<String> cols = new ArrayList<>();
            while (rs.next()) {
                cols.add(rs.getString("COLUMN_NAME").toLowerCase());
            }

            assertTrue(cols.contains("id"));
            assertTrue(cols.contains("paper_id"));
            assertTrue(cols.contains("attempt_no"));
            assertTrue(cols.contains("deadline_at"));
            assertTrue(cols.contains("session_token_hash"));
            assertTrue(cols.contains("client_nonce"));
            assertTrue(cols.contains("package_hash"));
            assertTrue(cols.contains("client_info_json"));
            assertTrue(cols.contains("created_at"));
            
            // SECURITY: Raw token MUST NOT exist
            assertFalse(cols.contains("session_token"));
            assertFalse(cols.contains("token"));
            assertFalse(cols.contains("raw_token"));
        }
    }

    @Test
    public void testTokenAndPackageSecurity() {
        System.setProperty("tse.paperStartV2.enabled", "true");
        Map<String, Object> reqData = new HashMap<>();
        reqData.put("examId", MOCK_EXAM_ID);
        reqData.put("debugMode", false);

        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("requestId", "security-test-req");
        reqMap.put("data", reqData);

        String jsonPayload = new Gson().toJson(reqMap);

        Packet response = ExamStartV2Service.handleStartRequestV2(MOCK_USER_ID, "STUDENT", jsonPayload);
        assertTrue(response.success);

        Map<String, Object> data = (Map<String, Object>) response.data;
        assertEquals(false, data.get("debugMode"));
        
        String sessionToken = (String) data.get("sessionToken");
        String attemptId = (String) data.get("attemptId");
        String packageHash = (String) data.get("packageHash");

        assertNotNull(sessionToken);
        assertNotNull(attemptId);
        assertNotNull(packageHash);

        // Verify JSON string format for package doesn't leak secrets
        String jsonDump = new Gson().toJson(data);
        assertFalse(jsonDump.contains("isCorrect"), "Package leaked isCorrect");
        assertFalse(jsonDump.contains("answerKey"), "Package leaked answerKey");
        assertFalse(jsonDump.contains("correctOption"), "Package leaked correctOption");
        assertFalse(jsonDump.contains("grading_config"), "Package leaked grading config");
        assertFalse(jsonDump.contains("passwordHash"), "Package leaked password");
    }
}
