package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

public class Phase11Kto11NTest {

    @BeforeEach
    public void setup() {
        System.clearProperty("tse.v2.defaultStudentSubmitV2.enabled");
        System.clearProperty("tse.v2.studentSubmitRuntimeAdapter.enabled");
        System.clearProperty("tse.v2.studentSubmitV2ExecutionBridge.enabled");
        System.clearProperty("tse.v2.studentSubmitLegacyFallbackRuntimeGuard.enabled");
        System.clearProperty("tse.v2.studentSubmitIntegrationRegressionGate.enabled");
        
        System.clearProperty("tse.v2.studentSubmitE2EHarness.enabled");
        System.clearProperty("tse.v2.studentSubmitFallbackIntegrationTest.enabled");
        System.clearProperty("tse.v2.releaseCandidateRegressionGate.enabled");
    }

    @AfterEach
    public void cleanup() {
        setup();
    }

    // --- PHASE 11K: E2E Harness Service Tests ---

    @Test
    public void testE2EHarnessOff() {
        V2StudentSubmitE2EHarnessService service = new V2StudentSubmitE2EHarnessService();
        V2StudentSubmitE2EHarnessResult r = service.prepareHarness(1, "att-1", "{}");
        assertFalse(r.isSuccess());
        assertEquals("NOT_READY", r.getHarnessStatus());
        assertEquals("HARNESS_DISABLED", r.getErrorCode());
    }

    @Test
    public void testE2EHarnessReadyWhenFlagsEnabled() {
        System.setProperty("tse.v2.studentSubmitE2EHarness.enabled", "true");
        System.setProperty("tse.v2.studentSubmitRuntimeAdapter.enabled", "true");
        System.setProperty("tse.v2.studentSubmitV2ExecutionBridge.enabled", "true");
        System.setProperty("tse.v2.studentSubmitLegacyFallbackRuntimeGuard.enabled", "true");

        V2StudentSubmitE2EHarnessService service = new V2StudentSubmitE2EHarnessService();
        // Valid payload mock
        String validPayload = "{\"attemptId\":\"att-1\",\"paperId\":\"p-1\",\"answers\":[]}";
        V2StudentSubmitE2EHarnessResult r = service.prepareHarness(1, "att-1", validPayload);

        assertTrue(r.isSuccess());
        assertTrue(r.isReady());
        assertTrue(r.isPayloadContractValid());
        assertEquals("STUDENT_SUBMIT_E2E_HARNESS_READY", r.getHarnessStatus());
        // Default V2 is false
        assertFalse(r.isV2DefaultEnabledInTestOnly());
    }

    // --- PHASE 11L: EXAM_SUBMIT E2E Integration Test ---

    // We simulate handleExamSubmitWithOptionalV2Adapter logic 
    // to test EXAM_SUBMIT routing and bridge orchestration
    public static class MockAdapterFacade {
        private final V2StudentSubmitRuntimeAdapterService adapterService;
        private final MockExecutionBridge bridgeService;
        
        public MockAdapterFacade(V2StudentSubmitRuntimeAdapterService adapterService, MockExecutionBridge bridgeService) {
            this.adapterService = adapterService;
            this.bridgeService = bridgeService;
        }

        public String handleExamSubmitWithOptionalV2Adapter(int userId, String attemptId, String payload) {
            V2StudentSubmitRuntimeAdapterResult routeResult = adapterService.resolveRuntimeRoute(userId, attemptId, payload);
            if ("V2_STUDENT_SUBMIT_EXECUTION_BRIDGE".equals(routeResult.getRuntimeRoute())) {
                V2StudentSubmitV2ExecutionBridgeResult bridgeResult = bridgeService.executeBridge(userId, attemptId, payload);
                if (bridgeResult.isSuccess()) {
                    return "V2_SUCCESS";
                } else {
                    return "V2_FAIL";
                }
            } else {
                return "LEGACY_EXECUTION";
            }
        }
    }

    public static class MockExecutionBridge extends V2StudentSubmitV2ExecutionBridgeService {
        private int executedSteps = 0;
        public int getExecutedSteps() { return executedSteps; }

        @Override
        public V2StudentSubmitV2ExecutionBridgeResult executeBridge(int userId, String attemptId, String payloadJson) {
            V2StudentSubmitV2ExecutionBridgeResult r = new V2StudentSubmitV2ExecutionBridgeResult();
            // Simulate 7 stages
            for(int i=0; i<7; i++) executedSteps++;
            r.setSuccess(true);
            return r;
        }
    }

    @Test
    public void testExamSubmitLegacySelectedWhenDefaultFalse() {
        V2StudentSubmitRuntimeAdapterService adapter = new V2StudentSubmitRuntimeAdapterService();
        MockExecutionBridge bridge = new MockExecutionBridge();
        MockAdapterFacade facade = new MockAdapterFacade(adapter, bridge);

        String outcome = facade.handleExamSubmitWithOptionalV2Adapter(1, "att-1", "{}");
        assertEquals("LEGACY_EXECUTION", outcome);
        assertEquals(0, bridge.getExecutedSteps()); // Bridge not called
    }

    @Test
    public void testExamSubmitV2BridgeSelectedWhenDefaultTrueInTest() {
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true");
        System.setProperty("tse.v2.studentSubmitRuntimeAdapter.enabled", "true");
        System.setProperty("tse.v2.studentSubmitV2ExecutionBridge.enabled", "true");
        System.setProperty("tse.v2.studentSubmitLegacyFallbackRuntimeGuard.enabled", "true");
        System.setProperty("tse.v2.studentSubmitIntegrationRegressionGate.enabled", "true");
        System.setProperty("tse.v2.studentSubmitAdapterDryRun.enabled", "true");
        System.setProperty("tse.v2.studentSubmitUiWiringReadiness.enabled", "true");

        V2StudentSubmitAdapterWiringService mockWiring = new V2StudentSubmitAdapterWiringService() {
            @Override
            public V2StudentSubmitAdapterWiringResult resolveRoute(int userId, String attemptId, String payloadJson) {
                V2StudentSubmitAdapterWiringResult r = new V2StudentSubmitAdapterWiringResult();
                r.setSuccess(true);
                r.setResolvedRoute("V2_MANUAL_CANDIDATE_PIPELINE");
                return r;
            }
        };
        V2StudentSubmitRuntimeAdapterService adapter = new V2StudentSubmitRuntimeAdapterService(mockWiring);
        MockExecutionBridge bridge = new MockExecutionBridge();
        MockAdapterFacade facade = new MockAdapterFacade(adapter, bridge);

        String outcome = facade.handleExamSubmitWithOptionalV2Adapter(1, "att-1", "{}");
        assertEquals("V2_SUCCESS", outcome);
        assertEquals(7, bridge.getExecutedSteps()); // Bridge fully executed
    }

    // --- PHASE 11M: Legacy Fallback Runtime Integration Test ---

    @Test
    public void testFallbackAllowedBeforeWrite() {
        System.setProperty("tse.v2.studentSubmitLegacyFallbackRuntimeGuard.enabled", "true");
        
        V2StudentSubmitLegacyFallbackRuntimeGuardService guard = new V2StudentSubmitLegacyFallbackRuntimeGuardService();
        // pre-write: no DB state
        V2StudentSubmitLegacyFallbackRuntimeGuardResult r = guard.checkGuard(1, "att-1");
        
        assertTrue(r.isSuccess());
        assertEquals("PRE_WRITE_FAILURE", r.getFailureZone());
        assertTrue(r.isFallbackAllowed());
    }

    @Test
    public void testFallbackForbiddenAfterWrite() {
        System.setProperty("tse.v2.studentSubmitLegacyFallbackRuntimeGuard.enabled", "true");

        V2StudentSubmitLegacyFallbackRuntimeGuardService guard = new V2StudentSubmitLegacyFallbackRuntimeGuardService(
            new com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO() {
                @Override
                public java.util.Optional<com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord> findLatestByAttemptId(String attemptId) throws java.sql.SQLException {
                    return java.util.Optional.of(new com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord());
                }
            },
            new com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusDAO(),
            new com.mycompany.tutorhub_enterprise.server.dao.V2ScoreDraftDAO(),
            new com.mycompany.tutorhub_enterprise.server.dao.V2OfficialResultDraftDAO(),
            new com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbeImpl(),
            new com.mycompany.tutorhub_enterprise.server.dao.V2ResultPublicationLedgerDAO(),
            new com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusLedgerDAO()
        );

        V2StudentSubmitLegacyFallbackRuntimeGuardResult r = guard.checkGuard(1, "att-1");
        
        assertTrue(r.isSuccess());
        assertEquals("POST_WRITE_FAILURE", r.getFailureZone());
        assertFalse(r.isFallbackAllowed());
        assertEquals("V2_WRITE_STARTED_FALLBACK_FORBIDDEN", r.getFallbackReason());
    }

    // --- PHASE 11N: Release Candidate Regression Gate Test ---

    @Test
    public void testRegressionGateReadyWhenProductionFlagsSafe() {
        System.setProperty("tse.v2.releaseCandidateRegressionGate.enabled", "true");
        System.setProperty("tse.v2.studentSubmitE2EHarness.enabled", "true");
        System.setProperty("tse.v2.studentSubmitFallbackIntegrationTest.enabled", "true");
        // defaultStudentSubmitV2 is naturally false

        V2ReleaseCandidateRegressionGateService gate = new V2ReleaseCandidateRegressionGateService();
        V2ReleaseCandidateRegressionGateResult r = gate.checkReleaseCandidateRegressionGate();

        assertTrue(r.isSuccess());
        assertTrue(r.isProductionFlagsSafe());
        assertTrue(r.isLegacySubmitIntact());
        assertEquals("V2_SUBMIT_RELEASE_CANDIDATE_REGRESSION_READY", r.getRegressionStatus());
    }

    @Test
    public void testRegressionGateFailsIfProductionDefaultEnabled() {
        System.setProperty("tse.v2.releaseCandidateRegressionGate.enabled", "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true"); // UNSAFE

        V2ReleaseCandidateRegressionGateService gate = new V2ReleaseCandidateRegressionGateService();
        V2ReleaseCandidateRegressionGateResult r = gate.checkReleaseCandidateRegressionGate();

        assertFalse(r.isSuccess());
        assertFalse(r.isProductionFlagsSafe());
        assertEquals("NOT_READY", r.getRegressionStatus());
    }

    // --- V2SubmitFeatureFlags & V2SubmitActions Test ---
    
    @Test
    public void testNewFlagsAndActions() {
        assertFalse(V2SubmitFeatureFlags.isStudentSubmitE2EHarnessEnabled());
        assertFalse(V2SubmitFeatureFlags.isStudentSubmitFallbackIntegrationTestEnabled());
        assertFalse(V2SubmitFeatureFlags.isReleaseCandidateRegressionGateEnabled());

        assertTrue(V2SubmitActions.isValidAction(V2SubmitActions.EXAM_SUBMIT_V2_STUDENT_SUBMIT_E2E_HARNESS_CHECK));
        assertTrue(V2SubmitActions.isValidAction(V2SubmitActions.EXAM_SUBMIT_V2_STUDENT_SUBMIT_FALLBACK_INTEGRATION_CHECK));
        assertTrue(V2SubmitActions.isValidAction(V2SubmitActions.EXAM_SUBMIT_V2_RELEASE_CANDIDATE_REGRESSION_GATE));
    }
}
