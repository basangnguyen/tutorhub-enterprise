package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2AttemptFinalizationDraftServiceTest {

    private V2AttemptFinalizationDraftService service;
    private MockV2SubmitRecordDAO mockDAO;

    @BeforeEach
    public void setUp() {
        System.setProperty("tse.v2.attemptFinalizationDraft.enabled", "true");
        mockDAO = new MockV2SubmitRecordDAO();
        service = new V2AttemptFinalizationDraftService(mockDAO);
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.attemptFinalizationDraft.enabled");
    }

    @Test
    public void testFeatureFlagOff() {
        System.setProperty("tse.v2.attemptFinalizationDraft.enabled", "false");
        V2AttemptFinalizationDraftResult result = service.createFinalizationDraft(1, 1L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
        assertFalse(mockDAO.isUpdateCalled());
    }

    @Test
    public void testRecordNotFound() {
        V2AttemptFinalizationDraftResult result = service.createFinalizationDraft(1, 999L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_SUBMIT_RECORD_NOT_FOUND", result.getErrorCode());
        assertFalse(mockDAO.isUpdateCalled());
    }

    @Test
    public void testUserMismatch() {
        V2SubmitRecord record = createValidRecord(1L, 2, "RECEIVED_DEBUG");
        mockDAO.setMockRecord(record);

        V2AttemptFinalizationDraftResult result = service.createFinalizationDraft(1, 1L); // userId mismatch
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_SUBMIT_RECORD_USER_MISMATCH", result.getErrorCode());
        assertFalse(mockDAO.isUpdateCalled());
    }

    @Test
    public void testInvalidPayloadHashLength() {
        V2SubmitRecord record = createValidRecord(1L, 1, "RECEIVED_DEBUG");
        record.setPayloadHash("abc"); // Invalid length
        mockDAO.setMockRecord(record);

        V2AttemptFinalizationDraftResult result = service.createFinalizationDraft(1, 1L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_SUBMIT_RECORD_HASH_INVALID", result.getErrorCode());
        assertFalse(mockDAO.isUpdateCalled());
    }

    @Test
    public void testInvalidCurrentStatus() {
        V2SubmitRecord record = createValidRecord(1L, 1, "FINAL_SUBMITTED");
        mockDAO.setMockRecord(record);

        V2AttemptFinalizationDraftResult result = service.createFinalizationDraft(1, 1L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_SUBMIT_RECORD_STATUS_INVALID", result.getErrorCode());
        assertFalse(mockDAO.isUpdateCalled());
    }

    @Test
    public void testValidReceivedDebug() {
        V2SubmitRecord record = createValidRecord(1L, 1, "RECEIVED_DEBUG");
        mockDAO.setMockRecord(record);

        V2AttemptFinalizationDraftResult result = service.createFinalizationDraft(1, 1L);
        assertTrue(result.isSuccess());
        assertEquals("FINALIZATION_DRAFTED", result.getNewStatus());
        assertEquals("NO_GRADING_DRAFT", result.getFinalizationMode());
        assertTrue(mockDAO.isUpdateCalled());
        assertEquals("FINALIZATION_DRAFTED", mockDAO.getUpdatedStatus());
    }

    @Test
    public void testValidValidatedDebug() {
        V2SubmitRecord record = createValidRecord(1L, 1, "VALIDATED_DEBUG");
        mockDAO.setMockRecord(record);

        V2AttemptFinalizationDraftResult result = service.createFinalizationDraft(1, 1L);
        assertTrue(result.isSuccess());
        assertEquals("FINALIZATION_DRAFTED", result.getNewStatus());
        assertEquals("NO_GRADING_DRAFT", result.getFinalizationMode());
        assertTrue(mockDAO.isUpdateCalled());
        assertEquals("FINALIZATION_DRAFTED", mockDAO.getUpdatedStatus());
    }

    @Test
    public void testIdempotency() {
        V2SubmitRecord record = createValidRecord(1L, 1, "FINALIZATION_DRAFTED");
        mockDAO.setMockRecord(record);

        V2AttemptFinalizationDraftResult result = service.createFinalizationDraft(1, 1L);
        assertTrue(result.isSuccess());
        assertEquals("FINALIZATION_DRAFTED", result.getNewStatus());
        assertEquals("FINALIZATION_DRAFTED", result.getPreviousStatus());
        assertFalse(mockDAO.isUpdateCalled()); // Should not hit DB update again
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

    // Simple Mock DAO for isolating service logic
    private static class MockV2SubmitRecordDAO extends V2SubmitRecordDAO {
        private V2SubmitRecord mockRecord;
        private boolean updateCalled = false;
        private String updatedStatus;

        public void setMockRecord(V2SubmitRecord record) {
            this.mockRecord = record;
        }

        public boolean isUpdateCalled() {
            return updateCalled;
        }

        public String getUpdatedStatus() {
            return updatedStatus;
        }

        @Override
        public Optional<V2SubmitRecord> findById(long id) throws SQLException {
            if (mockRecord != null && mockRecord.getId() == id) {
                return Optional.of(mockRecord);
            }
            return Optional.empty();
        }

        @Override
        public boolean updateSubmitStatus(long submitRecordId, String status) throws SQLException {
            this.updateCalled = true;
            this.updatedStatus = status;
            return true;
        }
        
        // Block all other DB calls just to be safe
        @Override
        public void ensureSchema() throws SQLException { }
    }
}
