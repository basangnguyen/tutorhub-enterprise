package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2FinalAttemptStatusLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2ResultPublicationLedgerRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ResultPublicationLedgerDAO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2FinalAttemptStatusExecutionServiceTest {

    private V2FinalAttemptStatusExecutionService service;
    private MockV2FinalAttemptStatusReadinessService mockReadinessService;
    private MockV2FinalAttemptStatusDAO mockStatusDAO;
    private MockV2FinalAttemptStatusLedgerDAO mockLedgerDAO;
    private MockV2ResultPublicationLedgerDAO mockPubLedgerDAO;

    @BeforeEach
    public void setup() {
        System.setProperty("tse.v2.finalAttemptStatusExecution.enabled", "true");
        
        mockReadinessService = new MockV2FinalAttemptStatusReadinessService();
        mockStatusDAO = new MockV2FinalAttemptStatusDAO();
        mockLedgerDAO = new MockV2FinalAttemptStatusLedgerDAO();
        mockPubLedgerDAO = new MockV2ResultPublicationLedgerDAO();
        
        service = new V2FinalAttemptStatusExecutionService(
            mockReadinessService, mockStatusDAO, mockLedgerDAO, mockPubLedgerDAO
        );
    }

    @AfterEach
    public void cleanup() {
        System.clearProperty("tse.v2.finalAttemptStatusExecution.enabled");
    }

    @Test
    public void testFeatureFlagOffReject() {
        System.setProperty("tse.v2.finalAttemptStatusExecution.enabled", "false");
        V2FinalAttemptStatusExecutionResult res = service.executeFinalStatus(100, 500L);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_FEATURE_DISABLED", res.getErrorCode());
    }

    @Test
    public void testReadinessFailsOtherThanCurrentStatusReject() {
        V2FinalAttemptStatusReadinessResult rRes = new V2FinalAttemptStatusReadinessResult();
        rRes.setSuccess(false);
        rRes.setErrorCode("ERROR_FINAL_ATTEMPT_STATUS_EXAM_RESULT_MISSING");
        mockReadinessService.mockResult = rRes;

        V2FinalAttemptStatusExecutionResult res = service.executeFinalStatus(100, 500L);
        assertFalse(res.isSuccess());
        assertTrue(res.getErrorCode().contains("ERROR_FINAL_ATTEMPT_STATUS_EXAM_RESULT_MISSING"));
    }

    @Test
    public void testPublicationLedgerMissingReject() {
        mockReadinessService.mockResult = createValidReadiness();
        mockPubLedgerDAO.mockRecord = null;

        V2FinalAttemptStatusExecutionResult res = service.executeFinalStatus(100, 500L);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_FINAL_ATTEMPT_STATUS_PUB_LEDGER_MISSING", res.getErrorCode());
    }

    @Test
    public void testCasUpdateFailedReject() {
        mockReadinessService.mockResult = createValidReadiness();
        mockPubLedgerDAO.mockRecord = new V2ResultPublicationLedgerRecord();
        mockStatusDAO.mockUpdateSuccess = false;

        V2FinalAttemptStatusExecutionResult res = service.executeFinalStatus(100, 500L);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_FINAL_ATTEMPT_STATUS_CAS_UPDATE_FAILED", res.getErrorCode());
    }

    @Test
    public void testLedgerInsertFailedReject() {
        mockReadinessService.mockResult = createValidReadiness();
        mockPubLedgerDAO.mockRecord = new V2ResultPublicationLedgerRecord();
        mockStatusDAO.mockUpdateSuccess = true;
        mockLedgerDAO.mockInsertId = -1L;

        V2FinalAttemptStatusExecutionResult res = service.executeFinalStatus(100, 500L);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_FINAL_ATTEMPT_STATUS_LEDGER_INSERT_FAILED", res.getErrorCode());
    }

    @Test
    public void testValidExecutionSuccess() {
        mockReadinessService.mockResult = createValidReadiness();
        mockPubLedgerDAO.mockRecord = new V2ResultPublicationLedgerRecord();
        mockPubLedgerDAO.mockRecord.setId(10L);
        mockStatusDAO.mockUpdateSuccess = true;
        mockLedgerDAO.mockInsertId = 99L;

        V2FinalAttemptStatusExecutionResult res = service.executeFinalStatus(100, 500L);
        assertTrue(res.isSuccess());
        assertTrue(res.isReady());
        assertFalse(res.isIdempotent());
        assertEquals(99L, res.getExecutionLedgerId());
        assertEquals("COMPLETED", res.getActualAttemptStatus());
        assertEquals("ATTEMPT_STATUS_COMPLETED", res.getExecutionStatus());
    }

    @Test
    public void testIdempotentSuccess() {
        // Mock readiness fails with INVALID_CURRENT_STATUS
        V2FinalAttemptStatusReadinessResult rRes = new V2FinalAttemptStatusReadinessResult();
        rRes.setSuccess(false);
        rRes.setErrorCode("ERROR_FINAL_ATTEMPT_STATUS_INVALID_CURRENT_STATUS");
        mockReadinessService.mockResult = rRes;

        // Mock ledger exists
        V2FinalAttemptStatusLedgerRecord ledger = new V2FinalAttemptStatusLedgerRecord();
        ledger.setId(88L);
        ledger.setAttemptId("att-123");
        ledger.setStatusUpdateStatus("ATTEMPT_STATUS_COMPLETED");
        mockLedgerDAO.mockRecord = ledger;

        // Mock attempt is COMPLETED
        mockStatusDAO.mockStatus = "COMPLETED";

        V2FinalAttemptStatusExecutionResult res = service.executeFinalStatus(100, 500L);
        assertTrue(res.isSuccess());
        assertTrue(res.isReady());
        assertTrue(res.isIdempotent());
        assertEquals(88L, res.getExecutionLedgerId());
        assertEquals("COMPLETED", res.getActualAttemptStatus());
    }

    @Test
    public void testIdempotentUnsafeMissingLedger() {
        V2FinalAttemptStatusReadinessResult rRes = new V2FinalAttemptStatusReadinessResult();
        rRes.setSuccess(false);
        rRes.setErrorCode("ERROR_FINAL_ATTEMPT_STATUS_INVALID_CURRENT_STATUS");
        mockReadinessService.mockResult = rRes;

        mockLedgerDAO.mockRecord = null; // No ledger

        V2FinalAttemptStatusExecutionResult res = service.executeFinalStatus(100, 500L);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_FINAL_ATTEMPT_STATUS_UNSAFE_MISSING_LEDGER", res.getErrorCode());
    }

    @Test
    public void testIdempotentUnsafeLedgerExistsButAttemptNotCompleted() {
        V2FinalAttemptStatusReadinessResult rRes = new V2FinalAttemptStatusReadinessResult();
        rRes.setSuccess(false);
        rRes.setErrorCode("ERROR_FINAL_ATTEMPT_STATUS_INVALID_CURRENT_STATUS");
        mockReadinessService.mockResult = rRes;

        V2FinalAttemptStatusLedgerRecord ledger = new V2FinalAttemptStatusLedgerRecord();
        ledger.setAttemptId("att-123");
        mockLedgerDAO.mockRecord = ledger;

        mockStatusDAO.mockStatus = "SUBMITTED"; // Not COMPLETED

        V2FinalAttemptStatusExecutionResult res = service.executeFinalStatus(100, 500L);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_FINAL_ATTEMPT_STATUS_UNSAFE_LEDGER_EXISTS_BUT_ATTEMPT_NOT_COMPLETED", res.getErrorCode());
    }

    @Test
    public void testResultDTONoSensitiveFields() {
        Field[] fields = V2FinalAttemptStatusExecutionResult.class.getDeclaredFields();
        for (Field field : fields) {
            String name = field.getName();
            assertFalse("answers".equals(name) || "answerKey".equals(name) 
                || "isCorrect".equals(name) || "correctOption".equals(name) 
                || "perQuestionResults".equals(name) || "score".equals(name), 
                "Unsafe field found: " + name);
        }
    }

    // --- Mocks ---

    private V2FinalAttemptStatusReadinessResult createValidReadiness() {
        V2FinalAttemptStatusReadinessResult r = new V2FinalAttemptStatusReadinessResult();
        r.setSuccess(true);
        r.setReady(true);
        r.setAttemptId("att-999");
        r.setExamId(10);
        r.setPaperId(20);
        return r;
    }

    private static class MockV2FinalAttemptStatusReadinessService extends V2FinalAttemptStatusReadinessService {
        public MockV2FinalAttemptStatusReadinessService() { super(null, null, null, null, null); }
        public V2FinalAttemptStatusReadinessResult mockResult;
        @Override public V2FinalAttemptStatusReadinessResult checkReadiness(int userId, long submitRecordId) { return mockResult; }
    }

    private static class MockV2FinalAttemptStatusDAO extends V2FinalAttemptStatusDAO {
        public boolean mockUpdateSuccess = true;
        public String mockStatus;
        @Override public boolean updateAttemptStatusIfCurrent(Connection conn, String attemptId, String expectedCurrentStatus, String targetStatus) { return mockUpdateSuccess; }
        @Override public Optional<String> findAttemptStatus(String attemptId) { return Optional.ofNullable(mockStatus); }
    }

    private static class MockV2FinalAttemptStatusLedgerDAO extends V2FinalAttemptStatusLedgerDAO {
        public long mockInsertId = 1L;
        public V2FinalAttemptStatusLedgerRecord mockRecord;
        @Override public long insertExecutionLedger(Connection conn, V2FinalAttemptStatusLedgerRecord record) { return mockInsertId; }
        @Override public Optional<V2FinalAttemptStatusLedgerRecord> findBySubmitRecordId(long submitRecordId) { return Optional.ofNullable(mockRecord); }
    }

    private static class MockV2ResultPublicationLedgerDAO extends V2ResultPublicationLedgerDAO {
        public V2ResultPublicationLedgerRecord mockRecord;
        @Override public Optional<V2ResultPublicationLedgerRecord> findBySubmitRecordId(long submitRecordId) { return Optional.ofNullable(mockRecord); }
    }
}
