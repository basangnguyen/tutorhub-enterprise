package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class V2StudentSubmitAdapterWiringServiceTest {

    private V2StudentSubmitAdapterWiringService service;

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
                if ("INVALID_JSON".equals(payloadJson)) {
                    r.setReady(false);
                } else {
                    r.setReady(true);
                }
                return r;
            }
        };

        V2StudentFlowControlledCutoverGateService stubGate = new V2StudentFlowControlledCutoverGateService() {
            @Override
            public V2StudentFlowControlledCutoverGateResult checkGate(int userId, String attemptId) {
                return new V2StudentFlowControlledCutoverGateResult.Builder().ready(true).build();
            }
        };

        V2StudentSubmitUiWiringReadinessService stubReadinessService = new V2StudentSubmitUiWiringReadinessService() {
            @Override
            public V2StudentSubmitUiWiringReadinessResult checkReadiness(V2StudentFlowCutoverMappingResult mappingResult, V2StudentSubmitAdapterDryRunResult adapterResult, V2StudentFlowControlledCutoverGateResult gateResult) {
                V2StudentSubmitUiWiringReadinessResult r = new V2StudentSubmitUiWiringReadinessResult();
                if (adapterResult != null && !adapterResult.isReady()) {
                    r.setSuccess(false);
                    r.setReady(false);
                } else {
                    r.setSuccess(true);
                    r.setReady(true);
                }
                return r;
            }
        };

        service = new V2StudentSubmitAdapterWiringService(
            stubMapping,
            stubDryRun,
            stubGate,
            stubReadinessService
        );

        System.setProperty("tse.v2.studentSubmitAdapterWiring.enabled", "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.studentSubmitAdapterWiring.enabled");
        System.clearProperty("tse.v2.defaultStudentSubmitV2.enabled");
    }

    @Test
    public void testFlagDisabled() {
        System.setProperty("tse.v2.studentSubmitAdapterWiring.enabled", "false");
        V2StudentSubmitAdapterWiringResult result = service.resolveRoute(1, "ATTEMPT_1", "{}");
        assertFalse(result.isSuccess());
        assertEquals("NOT_READY", result.getResolvedRoute());
        assertEquals("DISABLED", result.getWiringStatus());
    }

    @Test
    public void testV2Disabled_RoutesToLegacy() {
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
        V2StudentSubmitAdapterWiringResult result = service.resolveRoute(1, "ATTEMPT_1", "{}");
        assertTrue(result.isSuccess());
        assertEquals("LEGACY_V1_STUDENT_SUBMIT", result.getResolvedRoute());
        assertEquals("ROUTED_LEGACY", result.getWiringStatus());
    }

    @Test
    public void testV2Enabled_GatesReady_RoutesToV2() {
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true");
        String payloadJson = "{\"selectedOptionId\": 1, \"answerKey\": \"A\"}";
        V2StudentSubmitAdapterWiringResult result = service.resolveRoute(1, "ATTEMPT_1", payloadJson);
        assertTrue(result.isSuccess());
        assertEquals("V2_MANUAL_CANDIDATE_PIPELINE", result.getResolvedRoute());
        assertEquals("ROUTED_V2", result.getWiringStatus());
    }

    @Test
    public void testV2Enabled_GatesFail_NotReady() {
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true");
        String payloadJson = "INVALID_JSON";
        V2StudentSubmitAdapterWiringResult result = service.resolveRoute(1, "ATTEMPT_1", payloadJson);
        assertFalse(result.isSuccess());
        assertEquals("NOT_READY", result.getResolvedRoute());
        assertEquals("V2_GATES_FAILED", result.getWiringStatus());
    }
}
