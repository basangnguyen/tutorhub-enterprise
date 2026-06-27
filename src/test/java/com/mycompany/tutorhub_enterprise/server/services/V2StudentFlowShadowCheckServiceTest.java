package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.ExamAttempt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class V2StudentFlowShadowCheckServiceTest {

    private V2StudentFlowShadowCheckService service;

    @BeforeEach
    public void setup() {
        System.setProperty("tse.v2.studentFlowShadowIntegration.enabled", "true");
    }

    @AfterEach
    public void teardown() {
        System.clearProperty("tse.v2.studentFlowShadowIntegration.enabled");
    }

    @Test
    public void testFeatureFlagDisabled() {
        System.setProperty("tse.v2.studentFlowShadowIntegration.enabled", "false");
        
        service = new MockShadowCheckService(null);
        V2StudentFlowShadowCheckResult result = service.checkShadowReadiness(1, "att-123");
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
        assertEquals("NOT_READY", result.getShadowStatus());
    }

    @Test
    public void testAttemptNotFound() {
        service = new MockShadowCheckService(null);
        V2StudentFlowShadowCheckResult result = service.checkShadowReadiness(1, "att-123");
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_ATTEMPT_NOT_FOUND", result.getErrorCode());
    }

    @Test
    public void testUserMismatch() {
        ExamAttempt attempt = new ExamAttempt();
        attempt.setId("att-123");
        attempt.setUserId(2); // different user
        attempt.setExamId(10);
        attempt.setPaperId(100);
        attempt.setStatus("IN_PROGRESS");
        
        service = new MockShadowCheckService(attempt);
        V2StudentFlowShadowCheckResult result = service.checkShadowReadiness(1, "att-123");
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_USER_MISMATCH", result.getErrorCode());
    }

    @Test
    public void testMissingMapping() {
        ExamAttempt attempt = new ExamAttempt();
        attempt.setId("att-123");
        attempt.setUserId(1);
        attempt.setExamId(0); // missing mapping
        attempt.setPaperId(100);
        attempt.setStatus("IN_PROGRESS");
        
        service = new MockShadowCheckService(attempt);
        V2StudentFlowShadowCheckResult result = service.checkShadowReadiness(1, "att-123");
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_MISSING_MAPPING", result.getErrorCode());
    }

    @Test
    public void testAlreadySubmitted() {
        ExamAttempt attempt = new ExamAttempt();
        attempt.setId("att-123");
        attempt.setUserId(1);
        attempt.setExamId(10);
        attempt.setPaperId(100);
        attempt.setStatus("SUBMITTED");
        
        service = new MockShadowCheckService(attempt);
        V2StudentFlowShadowCheckResult result = service.checkShadowReadiness(1, "att-123");
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_ATTEMPT_ALREADY_SUBMITTED", result.getErrorCode());
    }

    @Test
    public void testAlreadyCompleted() {
        ExamAttempt attempt = new ExamAttempt();
        attempt.setId("att-123");
        attempt.setUserId(1);
        attempt.setExamId(10);
        attempt.setPaperId(100);
        attempt.setStatus("COMPLETED");
        
        service = new MockShadowCheckService(attempt);
        V2StudentFlowShadowCheckResult result = service.checkShadowReadiness(1, "att-123");
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_ATTEMPT_ALREADY_SUBMITTED", result.getErrorCode());
    }

    @Test
    public void testValidCandidate() {
        ExamAttempt attempt = new ExamAttempt();
        attempt.setId("att-123");
        attempt.setUserId(1);
        attempt.setExamId(10);
        attempt.setPaperId(100);
        attempt.setStatus("IN_PROGRESS"); // Valid state
        
        service = new MockShadowCheckService(attempt);
        V2StudentFlowShadowCheckResult result = service.checkShadowReadiness(1, "att-123");
        
        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertEquals("NONE", result.getErrorCode());
        assertEquals("STUDENT_FLOW_V2_SHADOW_READY", result.getShadowStatus());
    }
    
    // Mock classes to avoid connecting to Neon DB
    private static class MockShadowCheckService extends V2StudentFlowShadowCheckService {
        private final ExamAttempt attemptToReturn;

        public MockShadowCheckService(ExamAttempt attemptToReturn) {
            this.attemptToReturn = attemptToReturn;
        }

        @Override
        protected ExamAttempt getAttemptDetails(String id) {
            return attemptToReturn;
        }
    }
}
