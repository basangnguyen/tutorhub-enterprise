package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.client.exam.ui.TSEV2SubmitPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class V2ServerSubmitNoGradingOrchestratorServiceTest {

    private MockV2SubmitDryRunValidationService mockValidation;
    private MockV2SubmitDryRunPersistenceService mockPersistence;
    private MockV2SubmitRecordService mockSubmitRecord;
    private MockV2AttemptFinalizationDraftService mockDraft;
    private MockV2AttemptFinalizationLedgerService mockLedger;
    private MockV2AttemptClosureDraftService mockClosure;
    private V2ServerSubmitNoGradingOrchestratorService orchestrator;

    @BeforeEach
    public void setUp() {
        System.setProperty("tse.v2.serverNoGradingOrchestrator.enabled", "true");
        mockValidation = new MockV2SubmitDryRunValidationService();
        mockPersistence = new MockV2SubmitDryRunPersistenceService();
        mockSubmitRecord = new MockV2SubmitRecordService();
        mockDraft = new MockV2AttemptFinalizationDraftService();
        mockLedger = new MockV2AttemptFinalizationLedgerService();
        mockClosure = new MockV2AttemptClosureDraftService();

        orchestrator = new V2ServerSubmitNoGradingOrchestratorService(
                mockValidation, mockPersistence, mockSubmitRecord, mockDraft, mockLedger, mockClosure
        );
    }

    @Test
    public void testDisabledFeature() {
        System.setProperty("tse.v2.serverNoGradingOrchestrator.enabled", "false");
        V2ServerSubmitNoGradingOrchestratorResult result = orchestrator.runNoGradingServerFlow(1, new TSEV2SubmitPayload());
        assertFalse(result.isSuccess());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
        System.setProperty("tse.v2.serverNoGradingOrchestrator.enabled", "true");
    }

    @Test
    public void testValidationFailure() {
        V2SubmitDryRunValidationResult valFail = new V2SubmitDryRunValidationResult();
        valFail.setSuccess(false);
        mockValidation.resultToReturn = valFail;

        V2ServerSubmitNoGradingOrchestratorResult result = orchestrator.runNoGradingServerFlow(1, new TSEV2SubmitPayload());
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_ORCHESTRATION_VALIDATION_FAILED", result.getErrorCode());
        assertEquals(0, result.getStepsCompleted());
    }

    @Test
    public void testFullFlowSuccess() {
        // Step 1
        V2SubmitDryRunValidationResult valSucc = new V2SubmitDryRunValidationResult();
        valSucc.setSuccess(true);
        mockValidation.resultToReturn = valSucc;

        // Step 2
        V2SubmitDryRunPersistenceResult dryRec = new V2SubmitDryRunPersistenceResult();
        dryRec.setSuccess(true);
        dryRec.setRecordId(10L);
        mockPersistence.resultToReturn = dryRec;

        // Step 3
        V2SubmitRecordResult subSucc = new V2SubmitRecordResult(true, null);
        subSucc.setSubmitRecordId(100L);
        mockSubmitRecord.resultToReturn = subSucc;

        // Step 4
        V2AttemptFinalizationDraftResult draftSucc = new V2AttemptFinalizationDraftResult(true, null);
        mockDraft.resultToReturn = draftSucc;

        // Step 5
        V2AttemptFinalizationLedgerResult ledgSucc = new V2AttemptFinalizationLedgerResult();
        ledgSucc.setSuccess(true);
        ledgSucc.setLedgerId(500L);
        mockLedger.resultToReturn = ledgSucc;

        // Step 6
        V2AttemptClosureDraftResult cloSucc = new V2AttemptClosureDraftResult();
        cloSucc.setSuccess(true);
        cloSucc.setClosureDraftId(999L);
        cloSucc.setClosureStatus("CLOSURE_DRAFTED_NO_GRADING");
        mockClosure.resultToReturn = cloSucc;

        V2ServerSubmitNoGradingOrchestratorResult result = orchestrator.runNoGradingServerFlow(1, new TSEV2SubmitPayload());
        assertTrue(result.isSuccess());
        assertEquals(6, result.getStepsCompleted());
        assertEquals("CLOSURE_DRAFTED_NO_GRADING", result.getFinalStatus());
        assertEquals(10L, result.getDryRunRecordId());
        assertEquals(100L, result.getSubmitRecordId());
        assertEquals(500L, result.getLedgerId());
        assertEquals(999L, result.getClosureDraftId());
    }

    // --- Custom Mocks ---

    static class MockV2SubmitDryRunValidationService extends V2SubmitDryRunValidationService {
        V2SubmitDryRunValidationResult resultToReturn;
        @Override
        public V2SubmitDryRunValidationResult validateDryRun(int userId, TSEV2SubmitPayload payload) {
            return resultToReturn;
        }
    }

    static class MockV2SubmitDryRunPersistenceService extends V2SubmitDryRunPersistenceService {
        V2SubmitDryRunPersistenceResult resultToReturn;
        @Override
        public V2SubmitDryRunPersistenceResult persistDryRun(int userId, TSEV2SubmitPayload payload) {
            return resultToReturn;
        }
    }

    static class MockV2SubmitRecordService extends V2SubmitRecordService {
        V2SubmitRecordResult resultToReturn;
        @Override
        public V2SubmitRecordResult createSubmitRecord(int userId, TSEV2SubmitPayload payload) {
            return resultToReturn;
        }
    }

    static class MockV2AttemptFinalizationDraftService extends V2AttemptFinalizationDraftService {
        V2AttemptFinalizationDraftResult resultToReturn;
        @Override
        public V2AttemptFinalizationDraftResult createFinalizationDraft(int userId, long submitRecordId) {
            return resultToReturn;
        }
    }

    static class MockV2AttemptFinalizationLedgerService extends V2AttemptFinalizationLedgerService {
        V2AttemptFinalizationLedgerResult resultToReturn;
        @Override
        public V2AttemptFinalizationLedgerResult createLedgerAfterFinalizationDraft(int userId, long submitRecordId) {
            return resultToReturn;
        }
    }

    static class MockV2AttemptClosureDraftService extends V2AttemptClosureDraftService {
        V2AttemptClosureDraftResult resultToReturn;
        public MockV2AttemptClosureDraftService() {
            super(null, null, null);
        }
        @Override
        public V2AttemptClosureDraftResult createClosureDraft(int userId, long submitRecordId) {
            return resultToReturn;
        }
    }
}
