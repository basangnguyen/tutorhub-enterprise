package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class V2StudentSubmitRegressionGateServiceTest {

    private V2StudentSubmitRegressionGateService service;

    @BeforeEach
    public void setUp() {
        V2StudentFlowCutoverMappingService stubMapping = new V2StudentFlowCutoverMappingService() {
            @Override
            public V2StudentFlowCutoverMappingResult inspectMapping() {
                V2StudentFlowCutoverMappingResult r = new V2StudentFlowCutoverMappingResult();
                r.setReady(true);
                return r;
            }
        };

        V2StudentSubmitAdapterDryRunService stubDryRun = new V2StudentSubmitAdapterDryRunService() {
            @Override
            public V2StudentSubmitAdapterDryRunResult dryRunRoute(int userId, String attemptId, String payloadJson) {
                V2StudentSubmitAdapterDryRunResult r = new V2StudentSubmitAdapterDryRunResult();
                r.setReady(true);
                return r;
            }
        };

        V2StudentFlowControlledCutoverGateService stubGate = new V2StudentFlowControlledCutoverGateService() {
            @Override
            public V2StudentFlowControlledCutoverGateResult checkGate(int userId, String attemptId) {
                return new V2StudentFlowControlledCutoverGateResult.Builder().ready(true).build();
            }
        };

        V2StudentSubmitUiWiringReadinessService stubUi = new V2StudentSubmitUiWiringReadinessService() {
            @Override
            public V2StudentSubmitUiWiringReadinessResult checkReadiness(V2StudentFlowCutoverMappingResult m, V2StudentSubmitAdapterDryRunResult a, V2StudentFlowControlledCutoverGateResult g) {
                V2StudentSubmitUiWiringReadinessResult r = new V2StudentSubmitUiWiringReadinessResult();
                r.setReady(true);
                return r;
            }
        };

        V2StudentSubmitAdapterWiringService stubWiring = new V2StudentSubmitAdapterWiringService() {
            @Override
            public V2StudentSubmitAdapterWiringResult resolveRoute(int userId, String attemptId, String payloadJson) {
                V2StudentSubmitAdapterWiringResult r = new V2StudentSubmitAdapterWiringResult();
                r.setReady(true);
                return r;
            }
        };

        V2StudentSubmitLegacyFallbackService stubFallback = new V2StudentSubmitLegacyFallbackService() {
            @Override
            public V2StudentSubmitLegacyFallbackResult checkFallback(int userId, String attemptId, String payloadJson) {
                V2StudentSubmitLegacyFallbackResult r = new V2StudentSubmitLegacyFallbackResult();
                r.setReady(true);
                return r;
            }
        };

        service = new V2StudentSubmitRegressionGateService(stubMapping, stubDryRun, stubGate, stubUi, stubWiring, stubFallback);

        System.setProperty("tse.v2.studentSubmitRegressionGate.enabled", "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.studentSubmitRegressionGate.enabled");
        System.clearProperty("tse.v2.defaultStudentSubmitV2.enabled");
    }

    @Test
    public void testFlagDisabled() {
        System.setProperty("tse.v2.studentSubmitRegressionGate.enabled", "false");
        V2StudentSubmitRegressionGateResult result = service.checkRegressionGate(1, "ATTEMPT_1", "{}");
        assertFalse(result.isSuccess());
        assertEquals("NOT_READY", result.getRegressionStatus());
    }

    @Test
    public void testRegressionGatePassesWhenV2DisabledAndAllGatesReady() {
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
        String payloadJson = "{\"selectedOptionId\": 1, \"answerKey\": \"A\"}";
        V2StudentSubmitRegressionGateResult result = service.checkRegressionGate(1, "ATTEMPT_1", payloadJson);
        
        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertEquals("STUDENT_SUBMIT_REGRESSION_GATE_READY", result.getRegressionStatus());
        assertFalse(result.isV2DefaultEnabled());
        assertTrue(result.isLegacySubmitIntact());
        assertTrue(result.isAdapterWiringReady());
        assertTrue(result.isLegacyFallbackReady());
    }

    @Test
    public void testRegressionGateFailsWhenV2Enabled() {
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true");
        String payloadJson = "{\"selectedOptionId\": 1, \"answerKey\": \"A\"}";
        V2StudentSubmitRegressionGateResult result = service.checkRegressionGate(1, "ATTEMPT_1", payloadJson);
        
        assertFalse(result.isSuccess());
        assertEquals("NOT_READY", result.getRegressionStatus());
        assertTrue(result.isV2DefaultEnabled());
    }
}
