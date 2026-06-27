package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class Phase11Gto11JTest {

    @BeforeEach
    void setUp() {
        System.setProperty("tse.v2.studentSubmitRuntimeAdapter.enabled", "false");
        System.setProperty("tse.v2.studentSubmitV2ExecutionBridge.enabled", "false");
        System.setProperty("tse.v2.studentSubmitLegacyFallbackRuntimeGuard.enabled", "false");
        System.setProperty("tse.v2.studentSubmitIntegrationRegressionGate.enabled", "false");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
        System.setProperty("tse.v2.studentSubmitAdapterWiring.enabled", "false");
        
        System.setProperty("tse.v2.studentFlowCutoverMapping.enabled", "false");
        System.setProperty("tse.v2.studentSubmitAdapterDryRun.enabled", "false");
        System.setProperty("tse.v2.studentSubmitUiWiringReadiness.enabled", "false");
    }

    // --- V2StudentSubmitRuntimeAdapterService Tests ---
    
    @Test
    void testRuntimeAdapter_FeatureDisabled_ReturnsLegacy() {
        V2StudentSubmitRuntimeAdapterService adapterService = new V2StudentSubmitRuntimeAdapterService();
        V2StudentSubmitRuntimeAdapterResult result = adapterService.resolveRuntimeRoute(1, "att1", "{}");
        assertFalse(result.isSuccess());
        assertEquals("LEGACY_V1_STUDENT_SUBMIT", result.getRuntimeRoute());
        assertEquals("DISABLED_FALLBACK_TO_LEGACY", result.getRuntimeAdapterStatus());
    }
    
    @Test
    void testRuntimeAdapter_V2DefaultDisabled_ReturnsLegacy() {
        System.setProperty("tse.v2.studentSubmitRuntimeAdapter.enabled", "true");
        V2StudentSubmitRuntimeAdapterService adapterService = new V2StudentSubmitRuntimeAdapterService();
        V2StudentSubmitRuntimeAdapterResult result = adapterService.resolveRuntimeRoute(1, "att1", "{}");
        assertTrue(result.isSuccess());
        assertEquals("LEGACY_V1_STUDENT_SUBMIT", result.getRuntimeRoute());
        assertEquals("V2_DISABLED_BY_DEFAULT", result.getRuntimeAdapterStatus());
    }
    
    @Test
    void testRuntimeAdapter_V2DefaultEnabled_WiringReturnsPipeline_ReturnsBridgeRoute() {
        System.setProperty("tse.v2.studentSubmitRuntimeAdapter.enabled", "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true");
        System.setProperty("tse.v2.studentSubmitAdapterWiring.enabled", "true");
        System.setProperty("tse.v2.studentFlowCutoverMapping.enabled", "true");
        System.setProperty("tse.v2.studentSubmitAdapterDryRun.enabled", "true");
        System.setProperty("tse.v2.studentSubmitUiWiringReadiness.enabled", "true");
        
        V2StudentSubmitRuntimeAdapterService adapterService = new V2StudentSubmitRuntimeAdapterService(new MockAdapterWiringService(true, "V2_MANUAL_CANDIDATE_PIPELINE"));
        V2StudentSubmitRuntimeAdapterResult result = adapterService.resolveRuntimeRoute(1, "att1", "{}");
        assertTrue(result.isSuccess());
        assertEquals("V2_STUDENT_SUBMIT_EXECUTION_BRIDGE", result.getRuntimeRoute());
    }

    @Test
    void testRuntimeAdapter_V2DefaultEnabled_WiringFails_ReturnsFallbackBeforeWrite() {
        System.setProperty("tse.v2.studentSubmitRuntimeAdapter.enabled", "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true");
        
        V2StudentSubmitRuntimeAdapterService adapterService = new V2StudentSubmitRuntimeAdapterService(new MockAdapterWiringService(false, "ERROR"));
        V2StudentSubmitRuntimeAdapterResult result = adapterService.resolveRuntimeRoute(1, "att1", "{}");
        assertFalse(result.isSuccess());
        assertEquals("FALLBACK_TO_LEGACY_BEFORE_V2_WRITE", result.getRuntimeRoute());
        assertEquals("V2_GATES_FAILED_PRE_WRITE", result.getRuntimeAdapterStatus());
    }
    
    // --- V2StudentSubmitV2ExecutionBridgeService Tests ---
    
    @Test
    void testExecutionBridge_FeatureDisabled_ReturnsError() {
        V2StudentSubmitV2ExecutionBridgeService bridgeService = new V2StudentSubmitV2ExecutionBridgeService();
        V2StudentSubmitV2ExecutionBridgeResult result = bridgeService.executeBridge(1, "att1", "{}");
        assertFalse(result.isSuccess());
        assertEquals("DISABLED", result.getBridgeStatus());
    }
    
    @Test
    void testExecutionBridge_DefaultV2Disabled_ReturnsError() {
        System.setProperty("tse.v2.studentSubmitV2ExecutionBridge.enabled", "true");
        V2StudentSubmitV2ExecutionBridgeService bridgeService = new V2StudentSubmitV2ExecutionBridgeService();
        V2StudentSubmitV2ExecutionBridgeResult result = bridgeService.executeBridge(1, "att1", "{}");
        assertFalse(result.isSuccess());
        assertEquals("V2_DEFAULT_DISABLED", result.getErrorCode());
    }
    
    @Test
    void testExecutionBridge_AllStepsSucceed_ReturnsSuccess() {
        System.setProperty("tse.v2.studentSubmitV2ExecutionBridge.enabled", "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true");
        
        V2StudentSubmitV2ExecutionBridgeService bridgeService = new V2StudentSubmitV2ExecutionBridgeService(
            new MockPreflightService(true),
            new MockMaterializationService(true, 100),
            new MockStatusExecutionService(true),
            new MockScoreService(true),
            new MockPublicationService(true, 200),
            new MockFinalStatusService(true),
            new MockVerificationService(true)
        );
        V2StudentSubmitV2ExecutionBridgeResult result = bridgeService.executeBridge(1, "att1", "{}");
        assertTrue(result.isSuccess());
        assertTrue(result.isExecuted());
        assertEquals(7, result.getExecutedStepCount());
        assertEquals(100, result.getSubmitRecordId());
        assertEquals(200, result.getExamResultId());
        assertEquals("COMPLETED", result.getFinalStatus());
    }
    
    @Test
    void testExecutionBridge_PreflightFails_ReturnsFailure() {
        System.setProperty("tse.v2.studentSubmitV2ExecutionBridge.enabled", "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true");
        
        V2StudentSubmitV2ExecutionBridgeService bridgeService = new V2StudentSubmitV2ExecutionBridgeService(
            new MockPreflightService(false),
            new MockMaterializationService(true, 100),
            new MockStatusExecutionService(true),
            new MockScoreService(true),
            new MockPublicationService(true, 200),
            new MockFinalStatusService(true),
            new MockVerificationService(true)
        );
        V2StudentSubmitV2ExecutionBridgeResult result = bridgeService.executeBridge(1, "att1", "{}");
        assertFalse(result.isSuccess());
        assertEquals("PREFLIGHT_FAILED", result.getErrorCode());
        assertEquals(0, result.getExecutedStepCount());
    }

    @Test
    void testExecutionBridge_MaterializationFails_ReturnsFailure() {
        System.setProperty("tse.v2.studentSubmitV2ExecutionBridge.enabled", "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true");
        
        V2StudentSubmitV2ExecutionBridgeService bridgeService = new V2StudentSubmitV2ExecutionBridgeService(
            new MockPreflightService(true),
            new MockMaterializationService(false, 0),
            new MockStatusExecutionService(true),
            new MockScoreService(true),
            new MockPublicationService(true, 200),
            new MockFinalStatusService(true),
            new MockVerificationService(true)
        );
        V2StudentSubmitV2ExecutionBridgeResult result = bridgeService.executeBridge(1, "att1", "{}");
        assertFalse(result.isSuccess());
        assertEquals("MATERIALIZATION_FAILED", result.getErrorCode());
        assertEquals(1, result.getExecutedStepCount());
    }
    
    // --- V2StudentSubmitLegacyFallbackRuntimeGuardService Tests ---
    
    @Test
    void testFallbackGuard_FeatureDisabled_ReturnsForbidden() {
        V2StudentSubmitLegacyFallbackRuntimeGuardService guardService = new V2StudentSubmitLegacyFallbackRuntimeGuardService();
        V2StudentSubmitLegacyFallbackRuntimeGuardResult result = guardService.checkGuard(1, "att1");
        assertFalse(result.isFallbackAllowed());
        assertTrue(result.isFallbackForbidden());
        assertEquals("FEATURE_DISABLED", result.getFallbackReason());
    }
    
    @Test
    void testFallbackGuard_NoWrites_ReturnsAllowed() {
        System.setProperty("tse.v2.studentSubmitLegacyFallbackRuntimeGuard.enabled", "true");
        
        V2StudentSubmitLegacyFallbackRuntimeGuardService guardService = new V2StudentSubmitLegacyFallbackRuntimeGuardService(
            new MockSubmitRecordDAO(false),
            new MockAttemptStatusDAO(null),
            new MockScoreDraftDAO(),
            new MockOfficialResultDraftDAO(),
            new MockExamResultsProbe(false),
            new MockPublicationLedgerDAO(false),
            new MockFinalStatusLedgerDAO(false)
        );
        
        V2StudentSubmitLegacyFallbackRuntimeGuardResult result = guardService.checkGuard(1, "att1");
        assertTrue(result.isFallbackAllowed());
        assertFalse(result.isFallbackForbidden());
        assertEquals("PRE_WRITE_FAILURE", result.getFailureZone());
        assertEquals("LEGACY_V1_STUDENT_SUBMIT", result.getTargetRoute());
    }
    
    @Test
    void testFallbackGuard_HasSubmitRecord_ReturnsForbidden() {
        System.setProperty("tse.v2.studentSubmitLegacyFallbackRuntimeGuard.enabled", "true");
        
        V2StudentSubmitLegacyFallbackRuntimeGuardService guardService = new V2StudentSubmitLegacyFallbackRuntimeGuardService(
            new MockSubmitRecordDAO(true),
            new MockAttemptStatusDAO(null),
            new MockScoreDraftDAO(),
            new MockOfficialResultDraftDAO(),
            new MockExamResultsProbe(false),
            new MockPublicationLedgerDAO(false),
            new MockFinalStatusLedgerDAO(false)
        );
        
        V2StudentSubmitLegacyFallbackRuntimeGuardResult result = guardService.checkGuard(1, "att1");
        assertFalse(result.isFallbackAllowed());
        assertTrue(result.isFallbackForbidden());
        assertEquals("POST_WRITE_FAILURE", result.getFailureZone());
        assertEquals("SAFE_ERROR_RETURN", result.getTargetRoute());
    }

    @Test
    void testFallbackGuard_HasAttemptStatusSubmitted_ReturnsForbidden() {
        System.setProperty("tse.v2.studentSubmitLegacyFallbackRuntimeGuard.enabled", "true");
        
        V2StudentSubmitLegacyFallbackRuntimeGuardService guardService = new V2StudentSubmitLegacyFallbackRuntimeGuardService(
            new MockSubmitRecordDAO(false),
            new MockAttemptStatusDAO("SUBMITTED"),
            new MockScoreDraftDAO(),
            new MockOfficialResultDraftDAO(),
            new MockExamResultsProbe(false),
            new MockPublicationLedgerDAO(false),
            new MockFinalStatusLedgerDAO(false)
        );
        
        V2StudentSubmitLegacyFallbackRuntimeGuardResult result = guardService.checkGuard(1, "att1");
        assertFalse(result.isFallbackAllowed());
        assertTrue(result.isFallbackForbidden());
        assertEquals("POST_WRITE_FAILURE", result.getFailureZone());
    }

    @Test
    void testFallbackGuard_HasFinalStatusLedger_ReturnsForbidden() {
        System.setProperty("tse.v2.studentSubmitLegacyFallbackRuntimeGuard.enabled", "true");
        
        V2StudentSubmitLegacyFallbackRuntimeGuardService guardService = new V2StudentSubmitLegacyFallbackRuntimeGuardService(
            new MockSubmitRecordDAO(false),
            new MockAttemptStatusDAO(null),
            new MockScoreDraftDAO(),
            new MockOfficialResultDraftDAO(),
            new MockExamResultsProbe(false),
            new MockPublicationLedgerDAO(false),
            new MockFinalStatusLedgerDAO(true)
        );
        
        V2StudentSubmitLegacyFallbackRuntimeGuardResult result = guardService.checkGuard(1, "att1");
        assertFalse(result.isFallbackAllowed());
        assertTrue(result.isFallbackForbidden());
        assertEquals("POST_WRITE_FAILURE", result.getFailureZone());
    }

    @Test
    void testFallbackGuard_ExceptionInCheck_ReturnsForbidden() {
        System.setProperty("tse.v2.studentSubmitLegacyFallbackRuntimeGuard.enabled", "true");
        
        V2StudentSubmitLegacyFallbackRuntimeGuardService guardService = new V2StudentSubmitLegacyFallbackRuntimeGuardService(
            new MockSubmitRecordDAO(false) {
                @Override
                public java.util.Optional<com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord> findLatestByAttemptId(String attemptId) throws java.sql.SQLException {
                    throw new RuntimeException("DB Error");
                }
            },
            new MockAttemptStatusDAO(null),
            new MockScoreDraftDAO(),
            new MockOfficialResultDraftDAO(),
            new MockExamResultsProbe(false),
            new MockPublicationLedgerDAO(false),
            new MockFinalStatusLedgerDAO(false)
        );
        
        V2StudentSubmitLegacyFallbackRuntimeGuardResult result = guardService.checkGuard(1, "att1");
        assertFalse(result.isFallbackAllowed());
        assertTrue(result.isFallbackForbidden());
        assertEquals("POST_WRITE_FAILURE", result.getFailureZone());
    }
    
    // --- V2StudentSubmitIntegrationRegressionGateService Tests ---
    
    @Test
    void testRegressionGate_FeatureDisabled_ReturnsDisabled() {
        V2StudentSubmitIntegrationRegressionGateService gateService = new V2StudentSubmitIntegrationRegressionGateService();
        V2StudentSubmitIntegrationRegressionGateResult result = gateService.checkGate();
        assertFalse(result.isSuccess());
        assertEquals("DISABLED", result.getRegressionStatus());
    }
    
    @Test
    void testRegressionGate_DefaultV2Enabled_ReturnsNotReady() {
        System.setProperty("tse.v2.studentSubmitIntegrationRegressionGate.enabled", "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true");
        V2StudentSubmitIntegrationRegressionGateService gateService = new V2StudentSubmitIntegrationRegressionGateService();
        V2StudentSubmitIntegrationRegressionGateResult result = gateService.checkGate();
        assertFalse(result.isSuccess());
        assertEquals("NOT_READY_V2_IS_DEFAULT", result.getRegressionStatus());
    }
    
    @Test
    void testRegressionGate_DefaultV2Disabled_ReturnsReady() {
        System.setProperty("tse.v2.studentSubmitIntegrationRegressionGate.enabled", "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
        V2StudentSubmitIntegrationRegressionGateService gateService = new V2StudentSubmitIntegrationRegressionGateService();
        V2StudentSubmitIntegrationRegressionGateResult result = gateService.checkGate();
        assertTrue(result.isSuccess());
        assertEquals("STUDENT_SUBMIT_INTEGRATION_REGRESSION_READY", result.getRegressionStatus());
    }

    // --- Mock Classes ---
    
    private static class MockAdapterWiringService extends V2StudentSubmitAdapterWiringService {
        private boolean success;
        private String route;
        public MockAdapterWiringService(boolean success, String route) {
            this.success = success;
            this.route = route;
        }
        @Override
        public V2StudentSubmitAdapterWiringResult resolveRoute(int userId, String attemptId, String payloadJson) {
            V2StudentSubmitAdapterWiringResult result = new V2StudentSubmitAdapterWiringResult();
            result.setSuccess(success);
            result.setResolvedRoute(route);
            return result;
        }
    }
    
    private static class MockPreflightService extends V2ManualCandidateActualSubmitPreflightService {
        private boolean success;
        public MockPreflightService(boolean success) { this.success = success; }
        @Override
        public V2ManualCandidateActualSubmitPreflightResult checkPreflight(int u, String a, String p) {
            V2ManualCandidateActualSubmitPreflightResult r = new V2ManualCandidateActualSubmitPreflightResult();
            r.setSuccess(success);
            return r;
        }
    }
    
    private static class MockMaterializationService extends V2ManualCandidateSubmitRecordMaterializationService {
        private boolean success;
        private int id;
        public MockMaterializationService(boolean success, int id) { this.success = success; this.id = id; }
        @Override
        public V2ManualCandidateSubmitRecordMaterializationResult materializeSubmitRecord(int u, String a, String p) {
            V2ManualCandidateSubmitRecordMaterializationResult r = new V2ManualCandidateSubmitRecordMaterializationResult();
            r.setSuccess(success);
            r.setSubmitRecordId((long)id);
            return r;
        }
    }

    private static class MockStatusExecutionService extends V2ManualCandidateSubmitStatusExecutionService {
        private boolean success;
        public MockStatusExecutionService(boolean success) { this.success = success; }
        @Override
        public V2ManualCandidateSubmitStatusExecutionResult executeSubmitStatus(int u, long s) {
            V2ManualCandidateSubmitStatusExecutionResult r = new V2ManualCandidateSubmitStatusExecutionResult();
            r.setSuccess(success);
            return r;
        }
    }

    private static class MockScoreService extends V2ManualCandidateScoreOfficialDraftExecutionService {
        private boolean success;
        public MockScoreService(boolean success) { this.success = success; }
        @Override
        public V2ManualCandidateScoreOfficialDraftExecutionResult executeDrafts(int u, long s) {
            V2ManualCandidateScoreOfficialDraftExecutionResult r = new V2ManualCandidateScoreOfficialDraftExecutionResult();
            r.setSuccess(success);
            return r;
        }
    }

    private static class MockPublicationService extends V2ManualCandidateExamResultsPublicationService {
        private boolean success;
        private int id;
        public MockPublicationService(boolean success, int id) { this.success = success; this.id = id; }
        @Override
        public V2ManualCandidateExamResultsPublicationResult publishManualResult(int u, long s) {
            V2ManualCandidateExamResultsPublicationResult r = new V2ManualCandidateExamResultsPublicationResult();
            r.setSuccess(success);
            r.setExamResultId((long)id);
            return r;
        }
    }

    private static class MockFinalStatusService extends V2ManualCandidateFinalStatusExecutionService {
        private boolean success;
        public MockFinalStatusService(boolean success) { this.success = success; }
        @Override
        public V2ManualCandidateFinalStatusExecutionResult executeFinalStatus(int u, String a) {
            return new V2ManualCandidateFinalStatusExecutionResult.Builder()
                    .success(success)
                    .build();
        }
    }

    private static class MockVerificationService extends V2ManualCandidateResultHandoffVerificationService {
        private boolean success;
        public MockVerificationService(boolean success) { this.success = success; }
        @Override
        public V2ManualCandidateResultHandoffVerificationResult verifyHandoff(int u, String a) {
            return new V2ManualCandidateResultHandoffVerificationResult.Builder()
                    .success(success)
                    .build();
        }
    }

    private static class MockSubmitRecordDAO extends com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO {
        private boolean hasRecord;
        public MockSubmitRecordDAO(boolean hasRecord) { this.hasRecord = hasRecord; }
        @Override
        public java.util.Optional<com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord> findLatestByAttemptId(String attemptId) throws java.sql.SQLException {
            if (hasRecord) return java.util.Optional.of(new com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord());
            return java.util.Optional.empty();
        }
    }

    private static class MockAttemptStatusDAO extends com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusDAO {
        private String status;
        public MockAttemptStatusDAO(String status) { this.status = status; }
        @Override
        public java.util.Optional<String> findAttemptStatus(String attemptId) {
            if (status != null) return java.util.Optional.of(status);
            return java.util.Optional.empty();
        }
    }

    private static class MockScoreDraftDAO extends com.mycompany.tutorhub_enterprise.server.dao.V2ScoreDraftDAO {}
    private static class MockOfficialResultDraftDAO extends com.mycompany.tutorhub_enterprise.server.dao.V2OfficialResultDraftDAO {}
    
    private static class MockExamResultsProbe extends com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbeImpl {
        private boolean exists;
        public MockExamResultsProbe(boolean exists) { this.exists = exists; }
        @Override
        public boolean existsResultForAttempt(String attemptId) { return exists; }
    }

    private static class MockPublicationLedgerDAO extends com.mycompany.tutorhub_enterprise.server.dao.V2ResultPublicationLedgerDAO {
        private boolean hasRecords;
        public MockPublicationLedgerDAO(boolean hasRecords) { this.hasRecords = hasRecords; }
        @Override
        public java.util.Optional<com.mycompany.tutorhub_enterprise.models.exam.V2ResultPublicationLedgerRecord> findByAttemptId(String attemptId) {
            if (hasRecords) {
                return java.util.Optional.of(new com.mycompany.tutorhub_enterprise.models.exam.V2ResultPublicationLedgerRecord());
            }
            return java.util.Optional.empty();
        }
    }

    private static class MockFinalStatusLedgerDAO extends com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusLedgerDAO {
        private boolean exists;
        public MockFinalStatusLedgerDAO(boolean exists) { this.exists = exists; }
        @Override
        public boolean existsByAttemptId(String attemptId) { return exists; }
    }
}
