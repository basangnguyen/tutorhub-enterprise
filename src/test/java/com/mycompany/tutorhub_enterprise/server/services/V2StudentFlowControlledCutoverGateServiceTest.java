package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class V2StudentFlowControlledCutoverGateServiceTest {

    private V2StudentFlowControlledCutoverGateService service;
    private StubHandoffVerificationService handoffVerificationService;

    @BeforeEach
    void setUp() {
        System.setProperty("tse.v2.studentFlowControlledCutoverGate.enabled", "true");
        
        System.setProperty("tse.v2.manualCandidateActualSubmitPreflight.enabled", "true");
        System.setProperty("tse.v2.manualCandidateSubmitRecordMaterialization.enabled", "true");
        System.setProperty("tse.v2.manualCandidatePublishFinalStatusOrchestratorGate.enabled", "true");
        System.setProperty("tse.v2.manualCandidateSubmitStatusExecution.enabled", "true");
        System.setProperty("tse.v2.manualCandidateScoreOfficialDraftExecution.enabled", "true");
        System.setProperty("tse.v2.manualCandidateExamResultsPublication.enabled", "true");
        System.setProperty("tse.v2.manualCandidateFinalStatusExecution.enabled", "true");
        System.setProperty("tse.v2.manualCandidateResultHandoffVerification.enabled", "true");
        
        System.setProperty("tse.v2.studentFlowShadowIntegration.enabled", "true");
        System.setProperty("tse.v2.studentFlowCutoverReadiness.enabled", "true");
        
        System.setProperty("tse.v2.clientServerNoGradingSubmit.enabled", "false");
        System.setProperty("tse.v2.realSubmitPreflight.enabled", "false");

        handoffVerificationService = new StubHandoffVerificationService();
        service = new V2StudentFlowControlledCutoverGateService(handoffVerificationService);

        handoffVerificationService.result = new V2ManualCandidateResultHandoffVerificationResult.Builder()
                .success(true)
                .ready(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("tse.v2.studentFlowControlledCutoverGate.enabled");
        
        System.clearProperty("tse.v2.manualCandidateActualSubmitPreflight.enabled");
        System.clearProperty("tse.v2.manualCandidateSubmitRecordMaterialization.enabled");
        System.clearProperty("tse.v2.manualCandidatePublishFinalStatusOrchestratorGate.enabled");
        System.clearProperty("tse.v2.manualCandidateSubmitStatusExecution.enabled");
        System.clearProperty("tse.v2.manualCandidateScoreOfficialDraftExecution.enabled");
        System.clearProperty("tse.v2.manualCandidateExamResultsPublication.enabled");
        System.clearProperty("tse.v2.manualCandidateFinalStatusExecution.enabled");
        System.clearProperty("tse.v2.manualCandidateResultHandoffVerification.enabled");
        
        System.clearProperty("tse.v2.studentFlowShadowIntegration.enabled");
        System.clearProperty("tse.v2.studentFlowCutoverReadiness.enabled");
        
        System.clearProperty("tse.v2.clientServerNoGradingSubmit.enabled");
        System.clearProperty("tse.v2.realSubmitPreflight.enabled");
    }

    @Test
    void testFeatureFlagDisabled() {
        System.setProperty("tse.v2.studentFlowControlledCutoverGate.enabled", "false");
        V2StudentFlowControlledCutoverGateResult result = service.checkGate(100, "A-123");
        assertFalse(result.isReady());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
    }

    @Test
    void testMissingDependencyFlag() {
        System.setProperty("tse.v2.manualCandidateFinalStatusExecution.enabled", "false");
        V2StudentFlowControlledCutoverGateResult result = service.checkGate(100, "A-123");
        assertFalse(result.isReady());
        assertTrue(result.getBlockingReasons().stream().anyMatch(r -> r.contains("10M-10T")));
    }

    @Test
    void testMissingShadowIntegration() {
        System.setProperty("tse.v2.studentFlowShadowIntegration.enabled", "false");
        V2StudentFlowControlledCutoverGateResult result = service.checkGate(100, "A-123");
        assertFalse(result.isReady());
        assertTrue(result.getBlockingReasons().stream().anyMatch(r -> r.contains("shadow integration")));
    }

    @Test
    void testMissingCutoverReadiness() {
        System.setProperty("tse.v2.studentFlowCutoverReadiness.enabled", "false");
        V2StudentFlowControlledCutoverGateResult result = service.checkGate(100, "A-123");
        assertFalse(result.isReady());
        assertTrue(result.getBlockingReasons().stream().anyMatch(r -> r.contains("cutover readiness")));
    }

    @Test
    void testDefaultFlowAlreadyEnabled() {
        System.setProperty("tse.v2.realSubmitPreflight.enabled", "true");
        V2StudentFlowControlledCutoverGateResult result = service.checkGate(100, "A-123");
        assertFalse(result.isReady());
        assertTrue(result.isDefaultFlowEnabled());
        assertTrue(result.getBlockingReasons().stream().anyMatch(r -> r.contains("Default student flow is already enabled")));
    }

    @Test
    void testHandoffVerificationFailed() {
        handoffVerificationService.result = new V2ManualCandidateResultHandoffVerificationResult.Builder()
                .success(false)
                .ready(false)
                .errorCode("ERROR_MISSING_PUBLICATION")
                .build();
        V2StudentFlowControlledCutoverGateResult result = service.checkGate(100, "A-123");
        assertFalse(result.isReady());
        assertTrue(result.getBlockingReasons().stream().anyMatch(r -> r.contains("ERROR_MISSING_PUBLICATION")));
    }

    @Test
    void testSuccess() {
        V2StudentFlowControlledCutoverGateResult result = service.checkGate(100, "A-123");
        assertTrue(result.isReady());
        assertFalse(result.isDefaultFlowEnabled());
        assertEquals("READY_FOR_CONTROLLED_STUDENT_FLOW_WIRING", result.getStatus());
        assertTrue(result.getBlockingReasons() == null || result.getBlockingReasons().isEmpty());
    }

    // Stub
    private static class StubHandoffVerificationService extends V2ManualCandidateResultHandoffVerificationService {
        V2ManualCandidateResultHandoffVerificationResult result;

        @Override
        public V2ManualCandidateResultHandoffVerificationResult verifyHandoff(int userId, String attemptId) {
            return result;
        }
    }
}
