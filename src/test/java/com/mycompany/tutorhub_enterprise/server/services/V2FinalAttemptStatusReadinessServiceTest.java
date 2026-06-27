package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2ResultPublicationLedgerRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbe;
import com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ResultPublicationLedgerDAO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2FinalAttemptStatusReadinessServiceTest {

    private V2FinalAttemptStatusReadinessService service;
    private MockV2ResultPublicationVerificationService mockVerificationService;
    private MockV2FinalResultHandoffService mockHandoffService;
    private MockV2ExamResultsReadOnlyProbe mockProbe;
    private MockV2ResultPublicationLedgerDAO mockPubLedgerDAO;
    private MockV2FinalAttemptStatusDAO mockStatusDAO;

    @BeforeEach
    public void setup() {
        System.setProperty("tse.v2.finalAttemptStatusReadiness.enabled", "true");
        
        mockVerificationService = new MockV2ResultPublicationVerificationService();
        mockHandoffService = new MockV2FinalResultHandoffService();
        mockProbe = new MockV2ExamResultsReadOnlyProbe();
        mockPubLedgerDAO = new MockV2ResultPublicationLedgerDAO();
        mockStatusDAO = new MockV2FinalAttemptStatusDAO();
        
        service = new V2FinalAttemptStatusReadinessService(
            mockVerificationService, mockHandoffService, mockProbe, mockPubLedgerDAO, mockStatusDAO
        );
    }

    @AfterEach
    public void cleanup() {
        System.clearProperty("tse.v2.finalAttemptStatusReadiness.enabled");
    }

    @Test
    public void testFeatureFlagOffReject() {
        System.setProperty("tse.v2.finalAttemptStatusReadiness.enabled", "false");
        V2FinalAttemptStatusReadinessResult res = service.checkReadiness(100, 500L);
        assertFalse(res.isSuccess());
        assertFalse(res.isReady());
        assertEquals("ERROR_FEATURE_DISABLED", res.getErrorCode());
    }

    @Test
    public void testVerificationNotVerifiedReject() {
        V2ResultPublicationVerificationResult vRes = new V2ResultPublicationVerificationResult();
        vRes.setSuccess(true);
        vRes.setReady(false);
        mockVerificationService.mockResult = vRes;

        V2FinalAttemptStatusReadinessResult res = service.checkReadiness(100, 500L);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_FINAL_ATTEMPT_STATUS_PUBLICATION_NOT_VERIFIED", res.getErrorCode());
    }

    @Test
    public void testHandoffNotReadyReject() {
        mockVerificationService.mockResult = createValidVerification();
        V2FinalResultHandoffResult hRes = new V2FinalResultHandoffResult();
        hRes.setSuccess(true);
        hRes.setHandoffStatus("NOT_READY");
        mockHandoffService.mockResult = hRes;

        V2FinalAttemptStatusReadinessResult res = service.checkReadiness(100, 500L);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_FINAL_ATTEMPT_STATUS_HANDOFF_NOT_READY", res.getErrorCode());
    }

    @Test
    public void testExamResultsMissingReject() {
        mockVerificationService.mockResult = createValidVerification();
        mockHandoffService.mockResult = createValidHandoff();
        mockProbe.mockExists = false;

        V2FinalAttemptStatusReadinessResult res = service.checkReadiness(100, 500L);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_FINAL_ATTEMPT_STATUS_EXAM_RESULT_MISSING", res.getErrorCode());
    }

    @Test
    public void testPublicationLedgerMissingReject() {
        mockVerificationService.mockResult = createValidVerification();
        mockHandoffService.mockResult = createValidHandoff();
        mockProbe.mockExists = true;
        mockPubLedgerDAO.mockRecord = null;

        V2FinalAttemptStatusReadinessResult res = service.checkReadiness(100, 500L);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_FINAL_ATTEMPT_STATUS_PUBLICATION_LEDGER_INVALID", res.getErrorCode());
    }

    @Test
    public void testCurrentStatusNotSubmittedReject() {
        mockVerificationService.mockResult = createValidVerification();
        mockHandoffService.mockResult = createValidHandoff();
        mockProbe.mockExists = true;
        mockPubLedgerDAO.mockRecord = createValidLedger();
        mockStatusDAO.mockStatus = "IN_PROGRESS";

        V2FinalAttemptStatusReadinessResult res = service.checkReadiness(100, 500L);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_FINAL_ATTEMPT_STATUS_INVALID_CURRENT_STATUS", res.getErrorCode());
    }

    @Test
    public void testValidReadinessSuccess() {
        mockVerificationService.mockResult = createValidVerification();
        mockHandoffService.mockResult = createValidHandoff();
        mockProbe.mockExists = true;
        mockPubLedgerDAO.mockRecord = createValidLedger();
        mockStatusDAO.mockStatus = "SUBMITTED";

        V2FinalAttemptStatusReadinessResult res = service.checkReadiness(100, 500L);
        assertTrue(res.isSuccess());
        assertTrue(res.isReady());
        assertEquals("READY_FOR_FINAL_ATTEMPT_STATUS_EXECUTION", res.getReadinessStatus());
        assertEquals("att-777", res.getAttemptId());
    }

    // --- Mocks ---

    private V2ResultPublicationVerificationResult createValidVerification() {
        V2ResultPublicationVerificationResult r = new V2ResultPublicationVerificationResult();
        r.setSuccess(true);
        r.setReady(true);
        r.setAttemptId("att-777");
        r.setExamId(10);
        r.setPaperId(20);
        return r;
    }

    private V2FinalResultHandoffResult createValidHandoff() {
        V2FinalResultHandoffResult r = new V2FinalResultHandoffResult();
        r.setSuccess(true);
        r.setHandoffStatus("FINAL_RESULT_HANDOFF_READY");
        return r;
    }

    private V2ResultPublicationLedgerRecord createValidLedger() {
        V2ResultPublicationLedgerRecord r = new V2ResultPublicationLedgerRecord();
        r.setPublicationStatus("RESULT_PUBLISHED");
        return r;
    }

    private static class MockV2ResultPublicationVerificationService extends V2ResultPublicationVerificationService {
        public MockV2ResultPublicationVerificationService() { super(); }
        public V2ResultPublicationVerificationResult mockResult;
        @Override public V2ResultPublicationVerificationResult verify(int userId, long submitRecordId) { return mockResult; }
    }

    private static class MockV2FinalResultHandoffService extends V2FinalResultHandoffService {
        public MockV2FinalResultHandoffService() { super(); }
        public V2FinalResultHandoffResult mockResult;
        @Override public V2FinalResultHandoffResult buildHandoff(int userId, long submitRecordId) { return mockResult; }
    }

    private static class MockV2ExamResultsReadOnlyProbe implements V2ExamResultsReadOnlyProbe {
        public boolean mockExists = true;
        @Override public boolean existsResultForAttempt(String attemptId) { return mockExists; }
    }

    private static class MockV2ResultPublicationLedgerDAO extends V2ResultPublicationLedgerDAO {
        public V2ResultPublicationLedgerRecord mockRecord;
        @Override public Optional<V2ResultPublicationLedgerRecord> findBySubmitRecordId(long submitRecordId) { return Optional.ofNullable(mockRecord); }
    }

    private static class MockV2FinalAttemptStatusDAO extends V2FinalAttemptStatusDAO {
        public String mockStatus;
        @Override public Optional<String> findAttemptStatus(String attemptId) { return Optional.ofNullable(mockStatus); }
    }
}
