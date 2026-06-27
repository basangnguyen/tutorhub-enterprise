package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class V2StudentFlowCutoverReadinessServiceTest {

    private V2StudentFlowCutoverReadinessService service;

    @BeforeEach
    public void setup() {
        System.setProperty("tse.v2.studentFlowCutoverReadiness.enabled", "true");
        System.setProperty("tse.v2.studentFlowShadowIntegration.enabled", "true");
        System.setProperty("tse.v2.finalResultHandoff.enabled", "true");
        System.setProperty("tse.v2.finalAttemptStatusExecution.enabled", "true");
    }

    @AfterEach
    public void teardown() {
        System.clearProperty("tse.v2.studentFlowCutoverReadiness.enabled");
        System.clearProperty("tse.v2.studentFlowShadowIntegration.enabled");
        System.clearProperty("tse.v2.finalResultHandoff.enabled");
        System.clearProperty("tse.v2.finalAttemptStatusExecution.enabled");
    }

    @Test
    public void testFeatureFlagDisabled() {
        System.setProperty("tse.v2.studentFlowCutoverReadiness.enabled", "false");
        
        service = new V2StudentFlowCutoverReadinessService(new MockShadowCheckService(createSuccessShadowResult()));
        V2StudentFlowCutoverReadinessResult result = service.checkCutoverReadiness(1, "att-123");
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
        assertEquals("NOT_READY", result.getCutoverReadinessStatus());
    }

    @Test
    public void testShadowCheckFailed() {
        V2StudentFlowShadowCheckResult shadowResult = new V2StudentFlowShadowCheckResult();
        shadowResult.setReady(false);
        shadowResult.setBlockingReasons(java.util.Collections.singletonList("Attempt not found"));

        service = new V2StudentFlowCutoverReadinessService(new MockShadowCheckService(shadowResult));
        V2StudentFlowCutoverReadinessResult result = service.checkCutoverReadiness(1, "att-123");
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_SHADOW_CHECK_FAILED", result.getErrorCode());
    }

    @Test
    public void testCutoverValidWithWarningsIfOtherFlagsDisabled() {
        System.setProperty("tse.v2.finalResultHandoff.enabled", "false"); // Should trigger a warning
        
        service = new V2StudentFlowCutoverReadinessService(new MockShadowCheckService(createSuccessShadowResult()));
        V2StudentFlowCutoverReadinessResult result = service.checkCutoverReadiness(1, "att-123");
        
        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertEquals("NONE", result.getErrorCode());
        assertEquals("READY_FOR_MANUAL_V2_STUDENT_FLOW_CANDIDATE", result.getCutoverReadinessStatus());
        assertTrue(result.getWarnings().size() > 0);
        assertTrue(result.getWarnings().get(0).contains("Final Result Handoff is not enabled globally."));
    }

    @Test
    public void testCutoverValidFully() {
        service = new V2StudentFlowCutoverReadinessService(new MockShadowCheckService(createSuccessShadowResult()));
        V2StudentFlowCutoverReadinessResult result = service.checkCutoverReadiness(1, "att-123");
        
        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertEquals("NONE", result.getErrorCode());
        assertEquals("READY_FOR_MANUAL_V2_STUDENT_FLOW_CANDIDATE", result.getCutoverReadinessStatus());
        assertEquals(0, result.getWarnings().size());
    }

    // Mock classes to avoid full execution path
    private V2StudentFlowShadowCheckResult createSuccessShadowResult() {
        V2StudentFlowShadowCheckResult res = new V2StudentFlowShadowCheckResult();
        res.setSuccess(true);
        res.setReady(true);
        return res;
    }

    private static class MockShadowCheckService extends V2StudentFlowShadowCheckService {
        private final V2StudentFlowShadowCheckResult resultToReturn;

        public MockShadowCheckService(V2StudentFlowShadowCheckResult resultToReturn) {
            super(); // using no-arg constructor
            this.resultToReturn = resultToReturn;
        }

        @Override
        public V2StudentFlowShadowCheckResult checkShadowReadiness(int userId, String attemptId) {
            return resultToReturn;
        }
    }
}
