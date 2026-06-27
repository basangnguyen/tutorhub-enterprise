package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptClosureDraftRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptFinalizationLedgerRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptClosureDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptFinalizationLedgerDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2AttemptClosureDraftServiceTest {

    private MockV2AttemptFinalizationLedgerDAO mockLedgerDAO;
    private MockV2AttemptFinalizationLedgerService mockLedgerService;
    private MockV2AttemptClosureDraftDAO mockClosureDAO;
    private V2AttemptClosureDraftService closureService;

    @BeforeEach
    public void setUp() {
        System.setProperty("tse.v2.attemptClosureDraft.enabled", "true");
        mockLedgerDAO = new MockV2AttemptFinalizationLedgerDAO();
        mockLedgerService = new MockV2AttemptFinalizationLedgerService();
        mockClosureDAO = new MockV2AttemptClosureDraftDAO();
        closureService = new V2AttemptClosureDraftService(mockLedgerDAO, mockLedgerService, mockClosureDAO);
    }

    @Test
    public void testDisabledFeature() {
        System.setProperty("tse.v2.attemptClosureDraft.enabled", "false");
        V2AttemptClosureDraftResult result = closureService.createClosureDraft(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
        System.setProperty("tse.v2.attemptClosureDraft.enabled", "true");
    }

    @Test
    public void testIdempotent() throws SQLException {
        V2AttemptClosureDraftRecord existing = new V2AttemptClosureDraftRecord();
        existing.setId(5L);
        existing.setSubmitRecordId(100L);
        mockClosureDAO.latestBySubmitRecordId = existing;

        V2AttemptClosureDraftResult result = closureService.createClosureDraft(1, 100L);
        assertTrue(result.isSuccess());
        assertTrue(result.isIdempotent());
        assertEquals(5L, result.getClosureDraftId());
    }

    @Test
    public void testCreateSuccess() throws SQLException {
        mockClosureDAO.latestBySubmitRecordId = null;

        V2AttemptFinalizationLedgerRecord ledger = new V2AttemptFinalizationLedgerRecord();
        ledger.setId(10L);
        ledger.setSubmitRecordId(100L);
        ledger.setUserId(1);
        ledger.setPayloadHash("0123456789012345678901234567890123456789012345678901234567890123");
        mockLedgerDAO.latestBySubmitRecordId = ledger;

        mockClosureDAO.insertedId = 50L;

        V2AttemptClosureDraftResult result = closureService.createClosureDraft(1, 100L);
        assertTrue(result.isSuccess());
        assertFalse(result.isIdempotent());
        assertEquals(50L, result.getClosureDraftId());
        assertEquals("CLOSURE_DRAFTED_NO_GRADING", result.getClosureStatus());
    }

    @Test
    public void testUserMismatch() throws SQLException {
        mockClosureDAO.latestBySubmitRecordId = null;

        V2AttemptFinalizationLedgerRecord ledger = new V2AttemptFinalizationLedgerRecord();
        ledger.setUserId(2); // different user
        mockLedgerDAO.latestBySubmitRecordId = ledger;

        V2AttemptClosureDraftResult result = closureService.createClosureDraft(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_CLOSURE_USER_MISMATCH", result.getErrorCode());
    }

    // --- Custom Mocks ---

    static class MockV2AttemptFinalizationLedgerDAO extends V2AttemptFinalizationLedgerDAO {
        V2AttemptFinalizationLedgerRecord latestBySubmitRecordId = null;
        @Override
        public Optional<V2AttemptFinalizationLedgerRecord> findLatestBySubmitRecordId(long submitRecordId) {
            return Optional.ofNullable(latestBySubmitRecordId);
        }
    }

    static class MockV2AttemptFinalizationLedgerService extends V2AttemptFinalizationLedgerService {
        // No methods needed right now
    }

    static class MockV2AttemptClosureDraftDAO extends V2AttemptClosureDraftDAO {
        V2AttemptClosureDraftRecord latestBySubmitRecordId = null;
        long insertedId = 0L;
        @Override
        public Optional<V2AttemptClosureDraftRecord> findLatestBySubmitRecordId(long submitRecordId) {
            return Optional.ofNullable(latestBySubmitRecordId);
        }
        @Override
        public Optional<V2AttemptClosureDraftRecord> findById(long id) {
            return Optional.empty(); // For test, returning empty is fine as long as it doesn't throw Exception
        }
        @Override
        public long insertClosureDraft(V2AttemptClosureDraftRecord record) throws SQLException {
            return insertedId;
        }
    }
}
