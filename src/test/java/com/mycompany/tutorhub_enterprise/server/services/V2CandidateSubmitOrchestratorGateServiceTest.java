package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class V2CandidateSubmitOrchestratorGateServiceTest {

    private MockGateService service;

    @BeforeEach
    public void setup() {
        System.setProperty("tse.v2.candidateSubmitOrchestratorGate.enabled", "true");
    }

    @AfterEach
    public void teardown() {
        System.clearProperty("tse.v2.candidateSubmitOrchestratorGate.enabled");
    }

    @Test
    public void testFeatureFlagOff() {
        System.setProperty("tse.v2.candidateSubmitOrchestratorGate.enabled", "false");
        service = new MockGateService(true, false, false, false, false);
        V2CandidateSubmitOrchestratorGateResult result = service.checkGate(1, "att-1");
        assertFalse(result.isReady());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testManualCheckNotReady() {
        service = new MockGateService(false, false, false, false, false);
        V2CandidateSubmitOrchestratorGateResult result = service.checkGate(1, "att-1");
        assertFalse(result.isReady());
        assertEquals("ERROR_MANUAL_CHECK_FAILED", result.getErrorCode());
    }

    @Test
    public void testExistingSubmitRecord() {
        service = new MockGateService(true, true, false, false, false);
        V2CandidateSubmitOrchestratorGateResult result = service.checkGate(1, "att-1");
        assertFalse(result.isReady());
        assertEquals("ERROR_EXISTING_SUBMIT_RECORD", result.getErrorCode());
    }

    @Test
    public void testExistingExamResults() {
        service = new MockGateService(true, false, false, true, false);
        V2CandidateSubmitOrchestratorGateResult result = service.checkGate(1, "att-1");
        assertFalse(result.isReady());
        assertEquals("ERROR_EXISTING_EXAM_RESULTS", result.getErrorCode());
    }

    @Test
    public void testExistingLedger() {
        service = new MockGateService(true, false, true, false, false);
        V2CandidateSubmitOrchestratorGateResult result = service.checkGate(1, "att-1");
        assertFalse(result.isReady());
        assertEquals("ERROR_EXISTING_LEDGER", result.getErrorCode());
    }

    @Test
    public void testExistingFinalStatusLedger() {
        service = new MockGateService(true, false, false, false, true);
        V2CandidateSubmitOrchestratorGateResult result = service.checkGate(1, "att-1");
        assertFalse(result.isReady());
        assertEquals("ERROR_EXISTING_LEDGER", result.getErrorCode());
    }

    @Test
    public void testValidGate() {
        service = new MockGateService(true, false, false, false, false);
        V2CandidateSubmitOrchestratorGateResult result = service.checkGate(1, "att-1");
        assertTrue(result.isReady());
        assertEquals("NONE", result.getErrorCode());
        assertEquals("READY_FOR_MANUAL_CANDIDATE_V2_SUBMIT_TRIGGER", result.getOrchestratorGateStatus());
    }

    private static class MockGateService extends V2CandidateSubmitOrchestratorGateService {
        private final boolean manualReady;
        private final boolean submitRecordExists;
        private final boolean ledgerExists;
        private final boolean examResultsExist;
        private final boolean finalStatusLedgerExists;

        public MockGateService(boolean manualReady, boolean submitRecordExists, boolean ledgerExists, boolean examResultsExist, boolean finalStatusLedgerExists) {
            this.manualReady = manualReady;
            this.submitRecordExists = submitRecordExists;
            this.ledgerExists = ledgerExists;
            this.examResultsExist = examResultsExist;
            this.finalStatusLedgerExists = finalStatusLedgerExists;
        }

        @Override
        protected V2ManualCandidateSubmitCheckResult checkManualCandidateSubmit(int userId, String attemptId) {
            V2ManualCandidateSubmitCheckResult res = new V2ManualCandidateSubmitCheckResult();
            res.setReady(manualReady);
            res.setErrorCode(manualReady ? "NONE" : "MANUAL_ERROR");
            res.setCandidateStatus(manualReady ? "MANUAL_READY" : "NOT_READY");
            res.setExamId(10);
            res.setPaperId(100);
            return res;
        }

        @Override
        protected boolean doesSubmitRecordExist(String attemptId) {
            return submitRecordExists;
        }

        @Override
        protected boolean doesLedgerExist(String attemptId) {
            return ledgerExists;
        }

        @Override
        protected boolean doesExamResultsExist(String attemptId) {
            return examResultsExist;
        }

        @Override
        protected boolean doesFinalStatusLedgerExist(String attemptId) {
            return finalStatusLedgerExists;
        }
    }
}
