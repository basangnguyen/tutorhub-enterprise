package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.ExamAttempt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class V2ManualCandidateSubmitCheckServiceTest {

    private MockManualService service;

    @BeforeEach
    public void setup() {
        System.setProperty("tse.v2.manualCandidateSubmit.enabled", "true");
        System.setProperty("tse.v2.studentFlowShadowIntegration.enabled", "true");
        System.setProperty("tse.v2.studentFlowCutoverReadiness.enabled", "true");
    }

    @AfterEach
    public void teardown() {
        System.clearProperty("tse.v2.manualCandidateSubmit.enabled");
        System.clearProperty("tse.v2.studentFlowShadowIntegration.enabled");
        System.clearProperty("tse.v2.studentFlowCutoverReadiness.enabled");
    }

    @Test
    public void testFeatureFlagOff() {
        System.setProperty("tse.v2.manualCandidateSubmit.enabled", "false");
        service = new MockManualService(null, true, true);
        V2ManualCandidateSubmitCheckResult result = service.checkCandidateSubmit(1, "att-1");
        assertFalse(result.isReady());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testShadowCheckNotReady() {
        service = new MockManualService(null, false, true);
        V2ManualCandidateSubmitCheckResult result = service.checkCandidateSubmit(1, "att-1");
        assertFalse(result.isReady());
        assertEquals("ERROR_SHADOW_CHECK_FAILED", result.getErrorCode());
    }

    @Test
    public void testCutoverReadinessNotReady() {
        service = new MockManualService(null, true, false);
        V2ManualCandidateSubmitCheckResult result = service.checkCandidateSubmit(1, "att-1");
        assertFalse(result.isReady());
        assertEquals("ERROR_CUTOVER_READINESS_FAILED", result.getErrorCode());
    }

    @Test
    public void testAttemptNotFound() {
        service = new MockManualService(null, true, true);
        V2ManualCandidateSubmitCheckResult result = service.checkCandidateSubmit(1, "att-1");
        assertFalse(result.isReady());
        assertEquals("ERROR_ATTEMPT_NOT_FOUND", result.getErrorCode());
    }

    @Test
    public void testUserMismatch() {
        ExamAttempt attempt = new ExamAttempt();
        attempt.setId("att-1");
        attempt.setUserId(2); // different user
        attempt.setExamId(10);
        attempt.setPaperId(100);
        attempt.setStatus("IN_PROGRESS");
        service = new MockManualService(attempt, true, true);
        V2ManualCandidateSubmitCheckResult result = service.checkCandidateSubmit(1, "att-1");
        assertFalse(result.isReady());
        assertEquals("ERROR_USER_MISMATCH", result.getErrorCode());
    }

    @Test
    public void testMissingMapping() {
        ExamAttempt attempt = new ExamAttempt();
        attempt.setId("att-1");
        attempt.setUserId(1);
        attempt.setExamId(0);
        attempt.setPaperId(100);
        attempt.setStatus("IN_PROGRESS");
        service = new MockManualService(attempt, true, true);
        V2ManualCandidateSubmitCheckResult result = service.checkCandidateSubmit(1, "att-1");
        assertFalse(result.isReady());
        assertEquals("ERROR_MISSING_MAPPING", result.getErrorCode());
    }

    @Test
    public void testSubmittedAttempt() {
        ExamAttempt attempt = new ExamAttempt();
        attempt.setId("att-1");
        attempt.setUserId(1);
        attempt.setExamId(10);
        attempt.setPaperId(100);
        attempt.setStatus("SUBMITTED");
        service = new MockManualService(attempt, true, true);
        V2ManualCandidateSubmitCheckResult result = service.checkCandidateSubmit(1, "att-1");
        assertFalse(result.isReady());
        assertEquals("ERROR_ATTEMPT_ALREADY_FINISHED", result.getErrorCode());
    }

    @Test
    public void testCompletedAttempt() {
        ExamAttempt attempt = new ExamAttempt();
        attempt.setId("att-1");
        attempt.setUserId(1);
        attempt.setExamId(10);
        attempt.setPaperId(100);
        attempt.setStatus("COMPLETED");
        service = new MockManualService(attempt, true, true);
        V2ManualCandidateSubmitCheckResult result = service.checkCandidateSubmit(1, "att-1");
        assertFalse(result.isReady());
        assertEquals("ERROR_ATTEMPT_ALREADY_FINISHED", result.getErrorCode());
    }

    @Test
    public void testGradedAttempt() {
        ExamAttempt attempt = new ExamAttempt();
        attempt.setId("att-1");
        attempt.setUserId(1);
        attempt.setExamId(10);
        attempt.setPaperId(100);
        attempt.setStatus("GRADED");
        service = new MockManualService(attempt, true, true);
        V2ManualCandidateSubmitCheckResult result = service.checkCandidateSubmit(1, "att-1");
        assertFalse(result.isReady());
        assertEquals("ERROR_ATTEMPT_ALREADY_FINISHED", result.getErrorCode());
    }

    @Test
    public void testValidInProgressAttempt() {
        ExamAttempt attempt = new ExamAttempt();
        attempt.setId("att-1");
        attempt.setUserId(1);
        attempt.setExamId(10);
        attempt.setPaperId(100);
        attempt.setStatus("IN_PROGRESS");
        service = new MockManualService(attempt, true, true);
        V2ManualCandidateSubmitCheckResult result = service.checkCandidateSubmit(1, "att-1");
        assertTrue(result.isReady());
        assertEquals("NONE", result.getErrorCode());
        assertEquals("MANUAL_CANDIDATE_SUBMIT_READY", result.getCandidateStatus());
    }

    private static class MockManualService extends V2ManualCandidateSubmitCheckService {
        private final ExamAttempt attemptToReturn;
        private final boolean shadowReady;
        private final boolean cutoverReady;

        public MockManualService(ExamAttempt attemptToReturn, boolean shadowReady, boolean cutoverReady) {
            this.attemptToReturn = attemptToReturn;
            this.shadowReady = shadowReady;
            this.cutoverReady = cutoverReady;
        }

        @Override
        protected ExamAttempt getAttemptDetails(String attemptId) {
            return attemptToReturn;
        }

        @Override
        protected V2StudentFlowShadowCheckResult checkShadowReadiness(int userId, String attemptId) {
            V2StudentFlowShadowCheckResult res = new V2StudentFlowShadowCheckResult();
            res.setReady(shadowReady);
            res.setErrorCode(shadowReady ? "NONE" : "SHADOW_ERROR");
            return res;
        }

        @Override
        protected V2StudentFlowCutoverReadinessResult checkCutoverReadiness(int userId, String attemptId) {
            V2StudentFlowCutoverReadinessResult res = new V2StudentFlowCutoverReadinessResult();
            res.setReady(cutoverReady);
            res.setErrorCode(cutoverReady ? "NONE" : "CUTOVER_ERROR");
            return res;
        }
    }
}
