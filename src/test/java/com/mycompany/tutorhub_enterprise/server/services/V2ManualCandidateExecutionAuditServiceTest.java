package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2ManualCandidateExecutionLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ManualCandidateExecutionLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2ManualCandidateExecutionAuditServiceTest {

    private FakeLedgerDAO fakeLedgerDAO;
    private FakeSubmitRecordDAO fakeSubmitRecordDAO;
    private V2ManualCandidateExecutionAuditService service;

    private static class FakeLedgerDAO extends V2ManualCandidateExecutionLedgerDAO {
        public Optional<V2ManualCandidateExecutionLedgerRecord> returnOpt = Optional.empty();
        @Override
        public void ensureSchema() { }
        @Override
        public Optional<V2ManualCandidateExecutionLedgerRecord> findByAttemptId(String attemptId) {
            return returnOpt;
        }
    }

    private static class FakeSubmitRecordDAO extends V2SubmitRecordDAO {
        public Optional<V2SubmitRecord> returnOpt = Optional.empty();
        @Override
        public Optional<V2SubmitRecord> findById(long submitRecordId) {
            return returnOpt;
        }
    }

    @BeforeEach
    public void setup() {
        fakeLedgerDAO = new FakeLedgerDAO();
        fakeSubmitRecordDAO = new FakeSubmitRecordDAO();
        service = new V2ManualCandidateExecutionAuditService(fakeLedgerDAO, fakeSubmitRecordDAO);
        System.setProperty("tse.v2.manualCandidateExecutionAudit.enabled", "true");
    }

    @AfterEach
    public void teardown() {
        System.clearProperty("tse.v2.manualCandidateExecutionAudit.enabled");
    }

    @Test
    public void testFeatureFlagDisabled() {
        System.setProperty("tse.v2.manualCandidateExecutionAudit.enabled", "false");
        V2ManualCandidateExecutionAuditResult result = service.auditManualExecution(1, "attempt-1");
        assertFalse(result.isSuccess());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testNoLedgerFound() {
        fakeLedgerDAO.returnOpt = Optional.empty();
        V2ManualCandidateExecutionAuditResult result = service.auditManualExecution(1, "attempt-1");
        assertTrue(result.isSuccess());
        assertFalse(result.isReady());
        assertFalse(result.isExecuted());
        assertEquals("NOT_EXECUTED", result.getExecutionStatus());
    }

    @Test
    public void testUserMismatch() {
        V2ManualCandidateExecutionLedgerRecord ledger = new V2ManualCandidateExecutionLedgerRecord();
        ledger.setUserId(2); // different user
        fakeLedgerDAO.returnOpt = Optional.of(ledger);
        
        V2ManualCandidateExecutionAuditResult result = service.auditManualExecution(1, "attempt-1");
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_USER_MISMATCH", result.getErrorCode());
    }

    @Test
    public void testPrepareOnlyWithInvalidSubmitRecordId() {
        V2ManualCandidateExecutionLedgerRecord ledger = new V2ManualCandidateExecutionLedgerRecord();
        ledger.setUserId(1);
        ledger.setExecutionMode("PREPARE_ONLY");
        ledger.setSubmitRecordId(100L); // invalid for prepare only
        fakeLedgerDAO.returnOpt = Optional.of(ledger);
        
        V2ManualCandidateExecutionAuditResult result = service.auditManualExecution(1, "attempt-1");
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_MANUAL_CANDIDATE_INCONSISTENT_STATE", result.getErrorCode());
    }

    @Test
    public void testFullExecutionWithMissingSubmitRecord() {
        V2ManualCandidateExecutionLedgerRecord ledger = new V2ManualCandidateExecutionLedgerRecord();
        ledger.setUserId(1);
        ledger.setExecutionMode("FULL_EXECUTION");
        ledger.setSubmitRecordId(100L);
        fakeLedgerDAO.returnOpt = Optional.of(ledger);
        fakeSubmitRecordDAO.returnOpt = Optional.empty();
        
        V2ManualCandidateExecutionAuditResult result = service.auditManualExecution(1, "attempt-1");
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_MANUAL_CANDIDATE_INCONSISTENT_STATE", result.getErrorCode());
    }

    @Test
    public void testPrepareOnlySuccess() {
        V2ManualCandidateExecutionLedgerRecord ledger = new V2ManualCandidateExecutionLedgerRecord();
        ledger.setUserId(1);
        ledger.setExamId(10);
        ledger.setPaperId(20);
        ledger.setId(300L);
        ledger.setExecutionMode("PREPARE_ONLY");
        ledger.setExecutionStatus("MANUAL_CANDIDATE_V2_SUBMIT_PREPARED_ONLY");
        ledger.setSubmitRecordId(0L); // valid
        fakeLedgerDAO.returnOpt = Optional.of(ledger);
        
        V2ManualCandidateExecutionAuditResult result = service.auditManualExecution(1, "attempt-1");
        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertTrue(result.isExecuted());
        assertEquals("PREPARE_ONLY", result.getExecutionMode());
        assertEquals("MANUAL_CANDIDATE_V2_SUBMIT_PREPARED_ONLY", result.getExecutionStatus());
        assertEquals(300L, result.getManualExecutionLedgerId());
    }
}
