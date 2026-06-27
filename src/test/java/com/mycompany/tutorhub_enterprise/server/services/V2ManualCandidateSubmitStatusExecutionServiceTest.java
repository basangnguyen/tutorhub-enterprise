package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptStatusExecutionLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptStatusExecutionLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamAttemptStatusDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class V2ManualCandidateSubmitStatusExecutionServiceTest {

    private V2ManualCandidatePublishFinalStatusGateService mockGateService;
    private V2SubmitRecordDAO mockSubmitRecordDAO;
    private V2ExamAttemptStatusDAO mockExamAttemptStatusDAO;
    private V2AttemptStatusExecutionLedgerDAO mockLedgerDAO;
    private V2ManualCandidateSubmitStatusExecutionService service;

    private V2ManualCandidatePublishFinalStatusGateResult gateResultToReturn;
    private Optional<V2SubmitRecord> submitRecordToReturn;
    private Optional<String> attemptStatusToReturn;
    private Optional<V2AttemptStatusExecutionLedgerRecord> ledgerRecordToReturn;
    private boolean updateStatusIfCurrentResult;
    private long insertLedgerResult;

    @BeforeEach
    void setUp() {
        System.setProperty("tse.v2.manualCandidateSubmitStatusExecution.enabled", "true");

        gateResultToReturn = new V2ManualCandidatePublishFinalStatusGateResult();
        submitRecordToReturn = Optional.empty();
        attemptStatusToReturn = Optional.empty();
        ledgerRecordToReturn = Optional.empty();
        updateStatusIfCurrentResult = true;
        insertLedgerResult = 1L;

        mockGateService = new V2ManualCandidatePublishFinalStatusGateService(null, null, null, null, null, null) {
            @Override
            public V2ManualCandidatePublishFinalStatusGateResult checkGate(int userId, String attemptId) {
                return gateResultToReturn;
            }
        };

        mockSubmitRecordDAO = new V2SubmitRecordDAO() {
            @Override
            public Optional<V2SubmitRecord> findById(long id) {
                return submitRecordToReturn;
            }
        };

        mockExamAttemptStatusDAO = new V2ExamAttemptStatusDAO() {
            @Override
            public Optional<String> findAttemptStatus(String attemptId) {
                return attemptStatusToReturn;
            }

            @Override
            public boolean updateAttemptStatusIfCurrent(Connection conn, String attemptId, String expectedStatus, String targetStatus) {
                return updateStatusIfCurrentResult;
            }
        };

        mockLedgerDAO = new V2AttemptStatusExecutionLedgerDAO() {
            @Override
            public void ensureSchema() {}

            @Override
            public Optional<V2AttemptStatusExecutionLedgerRecord> findBySubmitRecordId(long submitRecordId) {
                return ledgerRecordToReturn;
            }

            @Override
            public long insertExecutionLedger(Connection conn, V2AttemptStatusExecutionLedgerRecord record) {
                return insertLedgerResult;
            }
        };

        service = new V2ManualCandidateSubmitStatusExecutionService(
                mockGateService,
                mockSubmitRecordDAO,
                mockExamAttemptStatusDAO,
                mockLedgerDAO
        );
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("tse.v2.manualCandidateSubmitStatusExecution.enabled");
    }

    @Test
    void testExecute_FlagDisabled() {
        System.setProperty("tse.v2.manualCandidateSubmitStatusExecution.enabled", "false");
        V2ManualCandidateSubmitStatusExecutionResult result = service.executeSubmitStatus(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
    }

    @Test
    void testExecute_GateNotReady() {
        submitRecordToReturn = Optional.of(new V2SubmitRecord());
        gateResultToReturn.setSuccess(true);
        gateResultToReturn.setReady(false);

        V2ManualCandidateSubmitStatusExecutionResult result = service.executeSubmitStatus(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_MANUAL_CANDIDATE_GATE_NOT_READY", result.getErrorCode());
    }

    @Test
    void testExecute_SubmitRecordMissing() {
        gateResultToReturn.setSuccess(true);
        gateResultToReturn.setReady(true);
        gateResultToReturn.setAttemptId("att-1");
        
        V2ManualCandidateSubmitStatusExecutionResult result = service.executeSubmitStatus(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_SUBMIT_RECORD_MISSING", result.getErrorCode());
    }

    @Test
    void testExecute_AttemptNotFound() {
        gateResultToReturn.setSuccess(true);
        gateResultToReturn.setReady(true);
        gateResultToReturn.setAttemptId("att-1");
        submitRecordToReturn = Optional.of(new V2SubmitRecord());

        V2ManualCandidateSubmitStatusExecutionResult result = service.executeSubmitStatus(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_ATTEMPT_NOT_FOUND", result.getErrorCode());
    }

    @Test
    void testExecute_Idempotent_Success() {
        gateResultToReturn.setSuccess(true);
        gateResultToReturn.setReady(true);
        gateResultToReturn.setAttemptId("att-1");
        submitRecordToReturn = Optional.of(new V2SubmitRecord());
        attemptStatusToReturn = Optional.of("SUBMITTED");
        
        V2AttemptStatusExecutionLedgerRecord ledger = new V2AttemptStatusExecutionLedgerRecord();
        ledger.setId(999L);
        ledger.setExecutionStatus("DONE");
        ledgerRecordToReturn = Optional.of(ledger);

        V2ManualCandidateSubmitStatusExecutionResult result = service.executeSubmitStatus(1, 100L);
        assertTrue(result.isSuccess());
        assertTrue(result.isIdempotent());
        assertEquals(999L, result.getExecutionId());
        assertEquals("SUBMITTED", result.getActualAttemptStatus());
    }

    @Test
    void testExecute_Idempotent_MissingLedger() {
        gateResultToReturn.setSuccess(true);
        gateResultToReturn.setReady(true);
        gateResultToReturn.setAttemptId("att-1");
        submitRecordToReturn = Optional.of(new V2SubmitRecord());
        attemptStatusToReturn = Optional.of("SUBMITTED");
        ledgerRecordToReturn = Optional.empty();

        V2ManualCandidateSubmitStatusExecutionResult result = service.executeSubmitStatus(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_UNSAFE_STATE_MISSING_LEDGER", result.getErrorCode());
    }

    @Test
    void testExecute_AlreadyCompleted() {
        gateResultToReturn.setSuccess(true);
        gateResultToReturn.setReady(true);
        gateResultToReturn.setAttemptId("att-1");
        submitRecordToReturn = Optional.of(new V2SubmitRecord());
        attemptStatusToReturn = Optional.of("COMPLETED");

        V2ManualCandidateSubmitStatusExecutionResult result = service.executeSubmitStatus(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_UNSAFE_STATE_ALREADY_COMPLETED", result.getErrorCode());
    }

    @Test
    void testExecute_InvalidStatus() {
        gateResultToReturn.setSuccess(true);
        gateResultToReturn.setReady(true);
        gateResultToReturn.setAttemptId("att-1");
        submitRecordToReturn = Optional.of(new V2SubmitRecord());
        attemptStatusToReturn = Optional.of("UNKNOWN_STATE");

        V2ManualCandidateSubmitStatusExecutionResult result = service.executeSubmitStatus(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_CURRENT_STATUS_INVALID", result.getErrorCode());
    }

    // Success case testing would require JDBC mocking which is skipped in manual stubs for transactions,
    // but we can let it hit the DatabaseManager.getConnection() block if it doesn't fail immediately,
    // though that might throw exceptions if no DB is available. Since we don't mock Connection here,
    // the success path might throw a SQLException ("No suitable driver" or similar) in the test.
}
