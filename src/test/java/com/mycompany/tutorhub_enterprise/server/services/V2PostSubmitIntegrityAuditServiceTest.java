package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptStatusExecutionLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptStatusExecutionLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamAttemptStatusDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2PostSubmitIntegrityAuditServiceTest {

    private V2PostSubmitIntegrityAuditService service;
    private MockSubmitRecordDAO mockSubmitRecordDAO;
    private MockLedgerDAO mockLedgerDAO;
    private MockStatusDAO mockStatusDAO;

    @BeforeEach
    public void setUp() {
        mockSubmitRecordDAO = new MockSubmitRecordDAO();
        mockLedgerDAO = new MockLedgerDAO();
        mockStatusDAO = new MockStatusDAO();
        service = new V2PostSubmitIntegrityAuditService(mockSubmitRecordDAO, mockLedgerDAO, mockStatusDAO);
        System.setProperty("tse.v2.postSubmitIntegrityAudit.enabled", "true");
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.postSubmitIntegrityAudit.enabled");
    }

    @Test
    public void testFeatureFlagDisabled() {
        System.setProperty("tse.v2.postSubmitIntegrityAudit.enabled", "false");
        V2PostSubmitIntegrityAuditResult result = service.audit(1, 100L);
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
        assertEquals("NOT_READY", result.getAuditStatus());
    }

    @Test
    public void testSubmitRecordMissing() {
        V2PostSubmitIntegrityAuditResult result = service.audit(1, 999L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_POST_SUBMIT_AUDIT_MISSING_SUBMIT_RECORD", result.getErrorCode());
    }

    @Test
    public void testUserMismatch() {
        mockSubmitRecordDAO.recordToReturn = createValidRecord();
        V2PostSubmitIntegrityAuditResult result = service.audit(2, 100L); // wrong user
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_POST_SUBMIT_AUDIT_USER_MISMATCH", result.getErrorCode());
    }

    @Test
    public void testInvalidHash() {
        V2SubmitRecord record = createValidRecord();
        record.setPayloadHash("short");
        mockSubmitRecordDAO.recordToReturn = record;

        V2PostSubmitIntegrityAuditResult result = service.audit(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_POST_SUBMIT_AUDIT_INVALID_HASH", result.getErrorCode());
    }

    @Test
    public void testMissingLedger() {
        mockSubmitRecordDAO.recordToReturn = createValidRecord();
        mockLedgerDAO.recordToReturn = null;

        V2PostSubmitIntegrityAuditResult result = service.audit(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_POST_SUBMIT_AUDIT_MISSING_LEDGER", result.getErrorCode());
        assertFalse(result.isLedgerExists());
    }

    @Test
    public void testNotSubmittedStatus() {
        mockSubmitRecordDAO.recordToReturn = createValidRecord();
        mockLedgerDAO.recordToReturn = new V2AttemptStatusExecutionLedgerRecord();
        mockStatusDAO.statusToReturn = "IN_PROGRESS";

        V2PostSubmitIntegrityAuditResult result = service.audit(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_POST_SUBMIT_AUDIT_NOT_SUBMITTED", result.getErrorCode());
    }

    @Test
    public void testValidReady() {
        mockSubmitRecordDAO.recordToReturn = createValidRecord();
        mockLedgerDAO.recordToReturn = new V2AttemptStatusExecutionLedgerRecord();
        mockStatusDAO.statusToReturn = "SUBMITTED";

        V2PostSubmitIntegrityAuditResult result = service.audit(1, 100L);
        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertEquals("POST_SUBMIT_INTEGRITY_READY", result.getAuditStatus());
    }

    private V2SubmitRecord createValidRecord() {
        V2SubmitRecord rec = new V2SubmitRecord();
        rec.setId(100L);
        rec.setUserId(1);
        rec.setExamId(10);
        rec.setPaperId(20);
        rec.setAttemptId("ATTEMPT_1");
        rec.setPayloadHash("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        return rec;
    }

    // Mocks
    private static class MockSubmitRecordDAO extends V2SubmitRecordDAO {
        V2SubmitRecord recordToReturn;
        @Override
        public Optional<V2SubmitRecord> findById(long id) {
            return recordToReturn != null && recordToReturn.getId() == id ? Optional.of(recordToReturn) : Optional.empty();
        }
    }

    private static class MockLedgerDAO extends V2AttemptStatusExecutionLedgerDAO {
        V2AttemptStatusExecutionLedgerRecord recordToReturn;
        @Override
        public Optional<V2AttemptStatusExecutionLedgerRecord> findBySubmitRecordId(long submitRecordId) {
            return Optional.ofNullable(recordToReturn);
        }
    }

    private static class MockStatusDAO extends V2ExamAttemptStatusDAO {
        String statusToReturn;
        @Override
        public Optional<String> findAttemptStatus(String attemptId) {
            return Optional.ofNullable(statusToReturn);
        }
    }
}
