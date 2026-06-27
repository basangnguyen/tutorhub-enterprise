package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2ManualCandidateExecutionLedgerRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ManualCandidateExecutionLedgerDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2ManualCandidateSubmitExecutionServiceTest {

    private FakeGateService fakeGateService;
    private FakeLedgerDAO fakeLedgerDAO;
    private V2ManualCandidateSubmitExecutionService service;

    private static class FakeGateService extends V2CandidateSubmitOrchestratorGateService {
        public V2CandidateSubmitOrchestratorGateResult returnResult = new V2CandidateSubmitOrchestratorGateResult();
        @Override
        public V2CandidateSubmitOrchestratorGateResult checkGate(int userId, String attemptId) {
            return returnResult;
        }
    }

    private static class FakeLedgerDAO extends V2ManualCandidateExecutionLedgerDAO {
        public Optional<V2ManualCandidateExecutionLedgerRecord> returnOpt = Optional.empty();
        public boolean insertLedgerCalled = false;
        @Override
        public void ensureSchema() { }
        @Override
        public Optional<V2ManualCandidateExecutionLedgerRecord> findByAttemptId(String attemptId) {
            return returnOpt;
        }
        @Override
        public boolean insertLedger(V2ManualCandidateExecutionLedgerRecord record) {
            insertLedgerCalled = true;
            record.setId(200L);
            return true;
        }
    }

    @BeforeEach
    public void setup() {
        fakeGateService = new FakeGateService();
        fakeLedgerDAO = new FakeLedgerDAO();
        service = new V2ManualCandidateSubmitExecutionService(fakeGateService, fakeLedgerDAO);
        System.setProperty("tse.v2.manualCandidateSubmitExecution.enabled", "true");
    }

    @AfterEach
    public void teardown() {
        System.clearProperty("tse.v2.manualCandidateSubmitExecution.enabled");
    }

    @Test
    public void testFeatureFlagDisabled() {
        System.setProperty("tse.v2.manualCandidateSubmitExecution.enabled", "false");
        V2ManualCandidateSubmitExecutionResult result = service.executeManualCandidateSubmit(1, "attempt-1", "{}");
        assertFalse(result.isSuccess());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testIdempotentReturnIfAlreadyPrepared() {
        V2ManualCandidateExecutionLedgerRecord existing = new V2ManualCandidateExecutionLedgerRecord();
        existing.setId(100L);
        existing.setExecutionMode("PREPARE_ONLY");
        existing.setExecutionStatus("MANUAL_CANDIDATE_V2_SUBMIT_PREPARED_ONLY");
        
        fakeLedgerDAO.returnOpt = Optional.of(existing);
        
        V2ManualCandidateSubmitExecutionResult result = service.executeManualCandidateSubmit(1, "attempt-1", "{}");
        
        assertTrue(result.isSuccess());
        assertTrue(result.isIdempotent());
        assertEquals(100L, result.getManualExecutionLedgerId());
    }

    @Test
    public void testGateNotReady() {
        fakeLedgerDAO.returnOpt = Optional.empty();
        
        V2CandidateSubmitOrchestratorGateResult gateResult = new V2CandidateSubmitOrchestratorGateResult();
        gateResult.setSuccess(false);
        gateResult.setReady(false);
        fakeGateService.returnResult = gateResult;
        
        V2ManualCandidateSubmitExecutionResult result = service.executeManualCandidateSubmit(1, "attempt-1", "{}");
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_MANUAL_CANDIDATE_GATE_NOT_READY", result.getErrorCode());
        assertFalse(fakeLedgerDAO.insertLedgerCalled);
    }

    @Test
    public void testEmptyPayload() {
        fakeLedgerDAO.returnOpt = Optional.empty();
        
        V2CandidateSubmitOrchestratorGateResult gateResult = new V2CandidateSubmitOrchestratorGateResult();
        gateResult.setSuccess(true);
        gateResult.setReady(true);
        fakeGateService.returnResult = gateResult;
        
        V2ManualCandidateSubmitExecutionResult result = service.executeManualCandidateSubmit(1, "attempt-1", "");
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_INVALID_PAYLOAD", result.getErrorCode());
    }

    @Test
    public void testPrepareOnlyExecutionSuccess() {
        fakeLedgerDAO.returnOpt = Optional.empty();
        
        V2CandidateSubmitOrchestratorGateResult gateResult = new V2CandidateSubmitOrchestratorGateResult();
        gateResult.setSuccess(true);
        gateResult.setReady(true);
        gateResult.setExamId(10);
        gateResult.setPaperId(20);
        fakeGateService.returnResult = gateResult;
        
        V2ManualCandidateSubmitExecutionResult result = service.executeManualCandidateSubmit(1, "attempt-1", "{\"answers\":[]}");
        
        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertFalse(result.isExecuted()); // Prepare-only
        assertEquals("MANUAL_CANDIDATE_V2_SUBMIT_PREPARED_ONLY", result.getExecutionStatus());
        assertEquals("PREPARE_ONLY", result.getExecutionMode());
        assertEquals(200L, result.getManualExecutionLedgerId());
        assertTrue(result.getWarnings().size() > 0);
        assertTrue(fakeLedgerDAO.insertLedgerCalled);
    }
}
