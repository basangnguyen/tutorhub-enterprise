package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2OfficialResultDraftRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2ResultPublicationLedgerRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbe;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsWriteDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2OfficialResultDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ResultPublicationLedgerDAO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2ResultPublicationServiceTest {

    private V2ResultPublicationService service;
    private MockReadinessService mockReadiness;
    private MockOfficialResultDraftDAO mockDraftDAO;
    private MockExamResultsProbe mockProbe;
    private MockLedgerDAO mockLedgerDAO;
    private MockExamResultsWriteDAO mockWriteDAO;

    @BeforeEach
    public void setUp() {
        System.setProperty("tse.v2.resultPublicationWrite.enabled", "true");

        mockReadiness = new MockReadinessService();
        mockDraftDAO = new MockOfficialResultDraftDAO();
        mockProbe = new MockExamResultsProbe();
        mockLedgerDAO = new MockLedgerDAO();
        mockWriteDAO = new MockExamResultsWriteDAO();

        service = new TestableV2ResultPublicationService(mockReadiness, mockDraftDAO, mockProbe, mockLedgerDAO, mockWriteDAO);
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.resultPublicationWrite.enabled");
    }

    @Test
    public void testFeatureFlagOff_Rejects() {
        System.setProperty("tse.v2.resultPublicationWrite.enabled", "false");
        V2ResultPublicationResult res = service.publishResult(1, 100);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_FEATURE_DISABLED", res.getErrorCode());
    }

    @Test
    public void testReadinessNotReady_Rejects() {
        mockReadiness.ready = false;
        V2ResultPublicationResult res = service.publishResult(1, 100);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_V2_RESULT_PUBLICATION_READINESS_NOT_READY", res.getErrorCode());
    }

    @Test
    public void testOfficialResultDraftMissing_Rejects() {
        mockReadiness.ready = true;
        mockDraftDAO.draft = null;
        V2ResultPublicationResult res = service.publishResult(1, 100);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_V2_RESULT_PUBLICATION_DRAFT_MISSING", res.getErrorCode());
    }

    @Test
    public void testUserMismatch_Rejects() {
        mockReadiness.ready = true;
        V2OfficialResultDraftRecord draft = new V2OfficialResultDraftRecord();
        draft.setUserId(99); // different from 1
        mockDraftDAO.draft = draft;
        V2ResultPublicationResult res = service.publishResult(1, 100);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_V2_RESULT_PUBLICATION_USER_MISMATCH", res.getErrorCode());
    }

    @Test
    public void testExistingExamResultsAndExistingLedger_IdempotentSuccess() {
        mockReadiness.ready = true;
        V2OfficialResultDraftRecord draft = new V2OfficialResultDraftRecord();
        draft.setUserId(1);
        mockDraftDAO.draft = draft;

        mockProbe.exists = true;
        mockLedgerDAO.record = new V2ResultPublicationLedgerRecord();
        mockLedgerDAO.record.setId(10);

        V2ResultPublicationResult res = service.publishResult(1, 100);
        assertTrue(res.isSuccess());
        assertTrue(res.isIdempotent());
        assertEquals("EXAM_RESULTS_WRITTEN", res.getPublicationStatus());
        assertEquals(10L, res.getPublicationLedgerId());
    }

    @Test
    public void testExistingExamResultsButMissingLedger_RejectUnsafe() {
        mockReadiness.ready = true;
        V2OfficialResultDraftRecord draft = new V2OfficialResultDraftRecord();
        draft.setUserId(1);
        mockDraftDAO.draft = draft;

        mockProbe.exists = true;
        mockLedgerDAO.record = null;

        V2ResultPublicationResult res = service.publishResult(1, 100);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_V2_RESULT_PUBLICATION_INCONSISTENT_EXISTING_RESULT", res.getErrorCode());
    }

    @Test
    public void testMissingExamResultsButExistingLedger_RejectUnsafe() {
        mockReadiness.ready = true;
        V2OfficialResultDraftRecord draft = new V2OfficialResultDraftRecord();
        draft.setUserId(1);
        mockDraftDAO.draft = draft;

        mockProbe.exists = false;
        mockLedgerDAO.record = new V2ResultPublicationLedgerRecord();

        V2ResultPublicationResult res = service.publishResult(1, 100);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_V2_RESULT_PUBLICATION_INCONSISTENT_EXISTING_RESULT", res.getErrorCode());
    }

    @Test
    public void testValid_InsertsExamResultsAndLedger() {
        mockReadiness.ready = true;
        V2OfficialResultDraftRecord draft = new V2OfficialResultDraftRecord();
        draft.setUserId(1);
        mockDraftDAO.draft = draft;

        mockProbe.exists = false;
        mockLedgerDAO.record = null;

        TestableV2ResultPublicationService testService = (TestableV2ResultPublicationService) service;
        testService.mockTxSuccess = true;
        
        V2ResultPublicationResult res = testService.publishResult(1, 100);
        assertTrue(res.isSuccess());
        assertTrue(res.isReady());
        assertFalse(res.isIdempotent());
        assertEquals("EXAM_RESULTS_WRITTEN", res.getPublicationStatus());
    }

    @Test
    public void testValid_InsertExamResultsFails() {
        mockReadiness.ready = true;
        V2OfficialResultDraftRecord draft = new V2OfficialResultDraftRecord();
        draft.setUserId(1);
        mockDraftDAO.draft = draft;

        mockProbe.exists = false;
        mockLedgerDAO.record = null;

        TestableV2ResultPublicationService testService = (TestableV2ResultPublicationService) service;
        testService.mockTxSuccess = false;
        testService.mockTxErrorCode = "ERROR_V2_RESULT_PUBLICATION_INSERT_RESULT_FAILED";
        
        V2ResultPublicationResult res = testService.publishResult(1, 100);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_V2_RESULT_PUBLICATION_INSERT_RESULT_FAILED", res.getErrorCode());
    }

    @Test
    public void testValid_InsertLedgerFails() {
        mockReadiness.ready = true;
        V2OfficialResultDraftRecord draft = new V2OfficialResultDraftRecord();
        draft.setUserId(1);
        mockDraftDAO.draft = draft;

        mockProbe.exists = false;
        mockLedgerDAO.record = null;

        TestableV2ResultPublicationService testService = (TestableV2ResultPublicationService) service;
        testService.mockTxSuccess = false;
        testService.mockTxErrorCode = "ERROR_V2_RESULT_PUBLICATION_INSERT_LEDGER_FAILED";
        
        V2ResultPublicationResult res = testService.publishResult(1, 100);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_V2_RESULT_PUBLICATION_INSERT_LEDGER_FAILED", res.getErrorCode());
    }

    static class TestableV2ResultPublicationService extends V2ResultPublicationService {
        public boolean mockTxSuccess;
        public String mockTxErrorCode;

        public TestableV2ResultPublicationService(V2ResultPublicationReadinessService r, V2OfficialResultDraftDAO d, V2ExamResultsReadOnlyProbe p, V2ResultPublicationLedgerDAO l, V2ExamResultsWriteDAO w) {
            super(r, d, p, l, w);
        }

        @Override
        protected V2ResultPublicationResult executeTransaction(long submitRecordId, V2OfficialResultDraftRecord draft, V2ResultPublicationLedgerRecord ledgerRecord) {
            V2ResultPublicationResult result = new V2ResultPublicationResult();
            if (mockTxSuccess) {
                result.setSuccess(true);
                result.setReady(true);
                result.setIdempotent(false);
                result.setPublicationStatus("EXAM_RESULTS_WRITTEN");
                result.setPublicationLedgerId(999L);
            } else {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode(mockTxErrorCode);
            }
            return result;
        }
    }

    // Custom Mock Classes
    static class MockReadinessService extends V2ResultPublicationReadinessService {
        boolean ready;
        public MockReadinessService() {
            super(null, null, null, null);
        }
        @Override
        public V2ResultPublicationReadinessResult checkReadiness(int userId, long submitRecordId) {
            V2ResultPublicationReadinessResult r = new V2ResultPublicationReadinessResult();
            r.setReady(ready);
            if (ready) r.setPublicationReadinessStatus("READY_FOR_EXAM_RESULTS_WRITE_DRAFT");
            return r;
        }
    }

    static class MockOfficialResultDraftDAO extends V2OfficialResultDraftDAO {
        V2OfficialResultDraftRecord draft;
        @Override
        public Optional<V2OfficialResultDraftRecord> findBySubmitRecordId(long submitRecordId) {
            return Optional.ofNullable(draft);
        }
    }

    static class MockExamResultsProbe implements V2ExamResultsReadOnlyProbe {
        boolean exists;
        @Override
        public boolean existsResultForAttempt(String attemptId) {
            return exists;
        }
    }

    static class MockLedgerDAO extends V2ResultPublicationLedgerDAO {
        V2ResultPublicationLedgerRecord record;
        boolean insertSuccess;
        @Override
        public Optional<V2ResultPublicationLedgerRecord> findBySubmitRecordId(long submitRecordId) {
            return Optional.ofNullable(record);
        }
        @Override
        public boolean insertLedger(Connection conn, V2ResultPublicationLedgerRecord rec) {
            return insertSuccess;
        }
    }

    static class MockExamResultsWriteDAO extends V2ExamResultsWriteDAO {
        boolean insertSuccess;
        @Override
        public boolean insertResultIfAbsent(Connection conn, V2OfficialResultDraftRecord draft) {
            return insertSuccess;
        }
    }
}
