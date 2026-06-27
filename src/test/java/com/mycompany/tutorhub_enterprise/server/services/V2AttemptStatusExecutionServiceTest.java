package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptStatusExecutionLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptStatusTransitionDraftRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptStatusExecutionLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptStatusTransitionDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamAttemptStatusDAO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2AttemptStatusExecutionServiceTest {

    private V2AttemptStatusExecutionService executionService;
    private MockV2RealSubmitReadinessOrchestratorService mockOrchService;
    private MockV2AttemptStatusTransitionDraftDAO mockDraftDAO;
    private MockV2ExamAttemptStatusDAO mockAttemptStatusDAO;
    private MockV2AttemptStatusExecutionLedgerDAO mockLedgerDAO;

    @BeforeEach
    public void setup() {
        System.setProperty("tse.v2.attemptStatusExecution.enabled", "true");
        
        mockOrchService = new MockV2RealSubmitReadinessOrchestratorService();
        mockDraftDAO = new MockV2AttemptStatusTransitionDraftDAO();
        mockAttemptStatusDAO = new MockV2ExamAttemptStatusDAO();
        mockLedgerDAO = new MockV2AttemptStatusExecutionLedgerDAO();
        
        executionService = new V2AttemptStatusExecutionService(
            mockOrchService, mockDraftDAO, mockAttemptStatusDAO, mockLedgerDAO
        );
    }

    @AfterEach
    public void cleanup() {
        System.clearProperty("tse.v2.attemptStatusExecution.enabled");
    }

    @Test
    public void testFeatureFlagOffReject() {
        System.setProperty("tse.v2.attemptStatusExecution.enabled", "false");
        V2AttemptStatusExecutionResult result = executionService.executeSubmittedStatus(100, 500L);
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testReadinessNotReadyReject() {
        mockOrchService.mockResult = new V2RealSubmitReadinessOrchestratorResult();
        mockOrchService.mockResult.setReady(false);
        mockOrchService.mockResult.setReadinessStatus("NOT_READY");

        V2AttemptStatusExecutionResult result = executionService.executeSubmittedStatus(100, 500L);
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_V2_ATTEMPT_STATUS_EXECUTION_NOT_READY", result.getErrorCode());
    }

    @Test
    public void testDraftMissingReject() {
        mockOrchService.mockResult = createReadyOrchResult();
        mockDraftDAO.mockDraft = null;

        V2AttemptStatusExecutionResult result = executionService.executeSubmittedStatus(100, 500L);
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_V2_ATTEMPT_STATUS_EXECUTION_DRAFT_MISSING", result.getErrorCode());
    }

    @Test
    public void testUserMismatchReject() {
        mockOrchService.mockResult = createReadyOrchResult();
        V2AttemptStatusTransitionDraftRecord draft = createValidDraft();
        draft.setUserId(999); // mismatch
        mockDraftDAO.mockDraft = draft;

        V2AttemptStatusExecutionResult result = executionService.executeSubmittedStatus(100, 500L);
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_V2_ATTEMPT_STATUS_EXECUTION_USER_MISMATCH", result.getErrorCode());
    }

    @Test
    public void testTargetStatusInvalidReject() {
        mockOrchService.mockResult = createReadyOrchResult();
        V2AttemptStatusTransitionDraftRecord draft = createValidDraft();
        draft.setTargetAttemptStatus("INVALID_STATUS");
        mockDraftDAO.mockDraft = draft;

        V2AttemptStatusExecutionResult result = executionService.executeSubmittedStatus(100, 500L);
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_V2_ATTEMPT_STATUS_EXECUTION_TARGET_INVALID", result.getErrorCode());
    }

    @Test
    public void testCurrentStatusNotAllowedReject() {
        mockOrchService.mockResult = createReadyOrchResult();
        mockDraftDAO.mockDraft = createValidDraft();
        mockAttemptStatusDAO.mockCurrentStatus = "COMPLETED";

        V2AttemptStatusExecutionResult result = executionService.executeSubmittedStatus(100, 500L);
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_V2_ATTEMPT_STATUS_EXECUTION_CURRENT_STATUS_INVALID", result.getErrorCode());
    }

    @Test
    public void testUpdateFailedReject() {
        mockOrchService.mockResult = createReadyOrchResult();
        mockDraftDAO.mockDraft = createValidDraft();
        mockAttemptStatusDAO.mockCurrentStatus = "IN_PROGRESS";
        mockAttemptStatusDAO.mockUpdateSuccess = false; // Simulate CAS failure

        V2AttemptStatusExecutionResult result = executionService.executeSubmittedStatus(100, 500L);
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_V2_ATTEMPT_STATUS_EXECUTION_UPDATE_FAILED", result.getErrorCode());
    }

    @Test
    public void testLedgerInsertFailedReject() {
        mockOrchService.mockResult = createReadyOrchResult();
        mockDraftDAO.mockDraft = createValidDraft();
        mockAttemptStatusDAO.mockCurrentStatus = "IN_PROGRESS";
        mockAttemptStatusDAO.mockUpdateSuccess = true;
        mockLedgerDAO.mockInsertLedgerId = -1L; // Simulate insert failure

        V2AttemptStatusExecutionResult result = executionService.executeSubmittedStatus(100, 500L);
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_V2_ATTEMPT_STATUS_EXECUTION_LEDGER_INSERT_FAILED", result.getErrorCode());
    }

    @Test
    public void testAlreadySubmittedWithLedgerIdempotentSuccess() {
        mockOrchService.mockResult = createReadyOrchResult();
        mockDraftDAO.mockDraft = createValidDraft();
        mockAttemptStatusDAO.mockCurrentStatus = "SUBMITTED";
        mockLedgerDAO.mockExistingLedger = new V2AttemptStatusExecutionLedgerRecord();
        mockLedgerDAO.mockExistingLedger.setId(77L);

        V2AttemptStatusExecutionResult result = executionService.executeSubmittedStatus(100, 500L);
        
        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertTrue(result.isIdempotent());
        assertEquals(77L, result.getExecutionId());
        assertEquals("ATTEMPT_STATUS_EXECUTED_SUBMITTED", result.getExecutionStatus());
    }

    @Test
    public void testAlreadySubmittedMissingLedgerRejectUnsafe() {
        mockOrchService.mockResult = createReadyOrchResult();
        mockDraftDAO.mockDraft = createValidDraft();
        mockAttemptStatusDAO.mockCurrentStatus = "SUBMITTED";
        mockLedgerDAO.mockExistingLedger = null;

        V2AttemptStatusExecutionResult result = executionService.executeSubmittedStatus(100, 500L);
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_V2_ATTEMPT_STATUS_EXECUTION_MISSING_LEDGER", result.getErrorCode());
    }

    @Test
    public void testValidExecutionSuccess() {
        mockOrchService.mockResult = createReadyOrchResult();
        mockDraftDAO.mockDraft = createValidDraft();
        mockAttemptStatusDAO.mockCurrentStatus = "IN_PROGRESS";
        mockAttemptStatusDAO.mockUpdateSuccess = true;
        mockLedgerDAO.mockInsertLedgerId = 99L;

        V2AttemptStatusExecutionResult result = executionService.executeSubmittedStatus(100, 500L);
        
        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertFalse(result.isIdempotent());
        assertEquals(99L, result.getExecutionId());
        assertEquals("ATTEMPT_STATUS_EXECUTED_SUBMITTED", result.getExecutionStatus());
        assertEquals("SUBMITTED", result.getActualAttemptStatus());
    }

    @Test
    public void testResultDTONoAnswersOrScore() {
        Field[] fields = V2AttemptStatusExecutionResult.class.getDeclaredFields();
        for (Field field : fields) {
            String name = field.getName();
            assertFalse("answers".equals(name) || "selectedOptionId".equals(name) || "answerKey".equals(name) 
                || "isCorrect".equals(name) || "correctOption".equals(name) || "score".equals(name) 
                || "gradingResult".equals(name) || "sessionToken".equals(name) || "keyB64".equals(name) 
                || "plaintext".equals(name), "Unsafe field found: " + name);
        }
    }

    // --- Helpers & Mocks ---

    private V2RealSubmitReadinessOrchestratorResult createReadyOrchResult() {
        V2RealSubmitReadinessOrchestratorResult res = new V2RealSubmitReadinessOrchestratorResult();
        res.setReady(true);
        res.setReadinessStatus("READY_FOR_REAL_SUBMIT_STATUS_EXECUTION_DRAFT");
        return res;
    }

    private V2AttemptStatusTransitionDraftRecord createValidDraft() {
        V2AttemptStatusTransitionDraftRecord draft = new V2AttemptStatusTransitionDraftRecord();
        draft.setAttemptStatusTransitionDraftStatus("ATTEMPT_STATUS_TRANSITION_DRAFTED");
        draft.setTargetAttemptStatus("SUBMITTED");
        draft.setUserId(100);
        draft.setExamId(10);
        draft.setPaperId(20);
        draft.setAttemptId("att-xyz");
        draft.setPayloadHash("hash");
        return draft;
    }

    private static class MockV2RealSubmitReadinessOrchestratorService extends V2RealSubmitReadinessOrchestratorService {
        public V2RealSubmitReadinessOrchestratorResult mockResult;
        @Override public V2RealSubmitReadinessOrchestratorResult checkReadiness(int userId, long submitRecordId) { return mockResult; }
    }

    private static class MockV2AttemptStatusTransitionDraftDAO extends V2AttemptStatusTransitionDraftDAO {
        public V2AttemptStatusTransitionDraftRecord mockDraft;
        @Override public Optional<V2AttemptStatusTransitionDraftRecord> findBySubmitRecordId(long submitRecordId) { return Optional.ofNullable(mockDraft); }
    }

    private static class MockV2ExamAttemptStatusDAO extends V2ExamAttemptStatusDAO {
        public String mockCurrentStatus;
        public boolean mockUpdateSuccess;
        
        @Override public Optional<String> findAttemptStatus(String attemptId) { return Optional.ofNullable(mockCurrentStatus); }
        @Override public boolean updateAttemptStatusIfCurrent(Connection conn, String attemptId, String expectedCurrentStatus, String targetStatus) { return mockUpdateSuccess; }
    }

    private static class MockV2AttemptStatusExecutionLedgerDAO extends V2AttemptStatusExecutionLedgerDAO {
        public V2AttemptStatusExecutionLedgerRecord mockExistingLedger;
        public long mockInsertLedgerId = 1L;

        @Override public void ensureSchema() {}
        @Override public Optional<V2AttemptStatusExecutionLedgerRecord> findBySubmitRecordId(long submitRecordId) { return Optional.ofNullable(mockExistingLedger); }
        @Override public long insertExecutionLedger(Connection conn, V2AttemptStatusExecutionLedgerRecord record) { return mockInsertLedgerId; }
    }
}
