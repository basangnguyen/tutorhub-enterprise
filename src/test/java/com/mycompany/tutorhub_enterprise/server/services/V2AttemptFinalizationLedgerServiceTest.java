package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptFinalizationLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptFinalizationLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2AttemptFinalizationLedgerServiceTest {

    private V2AttemptFinalizationLedgerService service;
    private MockV2SubmitRecordDAO mockSubmitRecordDAO;
    private MockV2AttemptFinalizationLedgerDAO mockLedgerDAO;
    private MockV2AttemptFinalizationDraftService mockDraftService;

    @BeforeEach
    public void setUp() {
        System.setProperty("tse.v2.attemptFinalizationLedger.enabled", "true");
        mockSubmitRecordDAO = new MockV2SubmitRecordDAO();
        mockLedgerDAO = new MockV2AttemptFinalizationLedgerDAO();
        mockDraftService = new MockV2AttemptFinalizationDraftService();
        service = new V2AttemptFinalizationLedgerService(mockSubmitRecordDAO, mockDraftService, mockLedgerDAO);
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.attemptFinalizationLedger.enabled");
    }

    @Test
    public void testFeatureFlagOff() {
        System.setProperty("tse.v2.attemptFinalizationLedger.enabled", "false");
        V2AttemptFinalizationLedgerResult result = service.createLedgerAfterFinalizationDraft(1, 1L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
        assertFalse(mockLedgerDAO.isInsertCalled());
    }

    @Test
    public void testRecordNotFound() {
        V2AttemptFinalizationLedgerResult result = service.createLedgerAfterFinalizationDraft(1, 999L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_SUBMIT_RECORD_NOT_FOUND", result.getErrorCode());
        assertFalse(mockLedgerDAO.isInsertCalled());
    }

    @Test
    public void testUserMismatch() {
        V2SubmitRecord record = createValidRecord(1L, 2, "FINALIZATION_DRAFTED");
        mockSubmitRecordDAO.setMockRecord(record);

        V2AttemptFinalizationLedgerResult result = service.createLedgerAfterFinalizationDraft(1, 1L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_SUBMIT_RECORD_USER_MISMATCH", result.getErrorCode());
        assertFalse(mockLedgerDAO.isInsertCalled());
    }

    @Test
    public void testInvalidPayloadHashLength() {
        V2SubmitRecord record = createValidRecord(1L, 1, "FINALIZATION_DRAFTED");
        record.setPayloadHash("abc");
        mockSubmitRecordDAO.setMockRecord(record);

        V2AttemptFinalizationLedgerResult result = service.createLedgerAfterFinalizationDraft(1, 1L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_SUBMIT_RECORD_HASH_INVALID", result.getErrorCode());
        assertFalse(mockLedgerDAO.isInsertCalled());
    }

    @Test
    public void testFinalizationRejected() {
        V2SubmitRecord record = createValidRecord(1L, 1, "FINALIZATION_REJECTED");
        mockSubmitRecordDAO.setMockRecord(record);

        V2AttemptFinalizationLedgerResult result = service.createLedgerAfterFinalizationDraft(1, 1L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_FINALIZATION_LEDGER_UNSAFE", result.getErrorCode());
        assertFalse(mockLedgerDAO.isInsertCalled());
    }

    @Test
    public void testDraftFailed() {
        V2SubmitRecord record = createValidRecord(1L, 1, "VALIDATED_DEBUG");
        mockSubmitRecordDAO.setMockRecord(record);
        
        mockDraftService.setMockResult(new V2AttemptFinalizationDraftResult(false, "ERROR_DRAFT"));

        V2AttemptFinalizationLedgerResult result = service.createLedgerAfterFinalizationDraft(1, 1L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_FINALIZATION_DRAFT_FAILED", result.getErrorCode());
        assertFalse(mockLedgerDAO.isInsertCalled());
    }

    @Test
    public void testValidCreation() {
        V2SubmitRecord record = createValidRecord(1L, 1, "FINALIZATION_DRAFTED");
        mockSubmitRecordDAO.setMockRecord(record);

        V2AttemptFinalizationLedgerRecord newLedger = new V2AttemptFinalizationLedgerRecord();
        newLedger.setId(10L);
        newLedger.setSubmitRecordId(1L);
        newLedger.setFinalizationStatus("FINALIZATION_DRAFT_ACKNOWLEDGED");
        mockLedgerDAO.setMockInsertedRecord(newLedger);
        mockLedgerDAO.setMockInsertId(10L);

        V2AttemptFinalizationLedgerResult result = service.createLedgerAfterFinalizationDraft(1, 1L);
        assertTrue(result.isSuccess());
        assertEquals("FINALIZATION_DRAFT_ACKNOWLEDGED", result.getFinalizationStatus());
        assertTrue(mockLedgerDAO.isInsertCalled());
        assertFalse(result.isIdempotent());
    }

    @Test
    public void testIdempotency() {
        V2SubmitRecord record = createValidRecord(1L, 1, "FINALIZATION_DRAFTED");
        mockSubmitRecordDAO.setMockRecord(record);

        V2AttemptFinalizationLedgerRecord existingLedger = new V2AttemptFinalizationLedgerRecord();
        existingLedger.setId(10L);
        existingLedger.setSubmitRecordId(1L);
        existingLedger.setFinalizationStatus("FINALIZATION_DRAFT_ACKNOWLEDGED");
        mockLedgerDAO.setMockExistingRecord(existingLedger);

        V2AttemptFinalizationLedgerResult result = service.createLedgerAfterFinalizationDraft(1, 1L);
        assertTrue(result.isSuccess());
        assertTrue(result.isIdempotent());
        assertEquals("FINALIZATION_DRAFT_ACKNOWLEDGED", result.getFinalizationStatus());
        assertFalse(mockLedgerDAO.isInsertCalled());
    }

    private V2SubmitRecord createValidRecord(long id, int userId, String status) {
        V2SubmitRecord record = new V2SubmitRecord();
        record.setId(id);
        record.setUserId(userId);
        record.setExamId(10);
        record.setPaperId(20);
        record.setAttemptId("att-123");
        record.setPayloadHash("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        record.setSubmitStatus(status);
        record.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        return record;
    }

    // Mock DAOs
    private static class MockV2SubmitRecordDAO extends V2SubmitRecordDAO {
        private V2SubmitRecord mockRecord;

        public void setMockRecord(V2SubmitRecord record) {
            this.mockRecord = record;
        }

        @Override
        public Optional<V2SubmitRecord> findById(long id) throws SQLException {
            if (mockRecord != null && mockRecord.getId() == id) {
                return Optional.of(mockRecord);
            }
            return Optional.empty();
        }
    }

    private static class MockV2AttemptFinalizationLedgerDAO extends V2AttemptFinalizationLedgerDAO {
        private boolean insertCalled = false;
        private V2AttemptFinalizationLedgerRecord mockExistingRecord;
        private V2AttemptFinalizationLedgerRecord mockInsertedRecord;
        private long mockInsertId = 0L;

        public boolean isInsertCalled() {
            return insertCalled;
        }

        public void setMockExistingRecord(V2AttemptFinalizationLedgerRecord record) {
            this.mockExistingRecord = record;
        }

        public void setMockInsertedRecord(V2AttemptFinalizationLedgerRecord record) {
            this.mockInsertedRecord = record;
        }

        public void setMockInsertId(long id) {
            this.mockInsertId = id;
        }

        @Override
        public Optional<V2AttemptFinalizationLedgerRecord> findLatestBySubmitRecordId(long submitRecordId) throws SQLException {
            if (mockExistingRecord != null && mockExistingRecord.getSubmitRecordId() == submitRecordId) {
                return Optional.of(mockExistingRecord);
            }
            return Optional.empty();
        }

        @Override
        public long insertLedgerRecord(V2AttemptFinalizationLedgerRecord record) throws SQLException {
            insertCalled = true;
            return mockInsertId;
        }

        @Override
        public Optional<V2AttemptFinalizationLedgerRecord> findById(long id) throws SQLException {
            if (mockInsertedRecord != null && mockInsertedRecord.getId() == id) {
                return Optional.of(mockInsertedRecord);
            }
            return Optional.empty();
        }
    }

    private static class MockV2AttemptFinalizationDraftService extends V2AttemptFinalizationDraftService {
        private V2AttemptFinalizationDraftResult mockResult;

        public void setMockResult(V2AttemptFinalizationDraftResult result) {
            this.mockResult = result;
        }

        @Override
        public V2AttemptFinalizationDraftResult createFinalizationDraft(int userId, long submitRecordId) {
            if (mockResult != null) return mockResult;
            return new V2AttemptFinalizationDraftResult(true, null);
        }
    }
}
