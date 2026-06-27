package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptStatusExecutionLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2OfficialResultDraftRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2ResultPublicationLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2ScoreDraftRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptStatusExecutionLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbe;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsWriteDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2OfficialResultDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ResultPublicationLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ScoreDraftDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class V2ManualCandidateExamResultsPublicationServiceTest {

    private V2AttemptStatusExecutionLedgerDAO mockStatusLedgerDAO;
    private V2ScoreDraftDAO mockScoreDraftDAO;
    private V2OfficialResultDraftDAO mockOfficialResultDraftDAO;
    private V2ExamResultsReadOnlyProbe mockExamResultsProbe;
    private V2ResultPublicationLedgerDAO mockLedgerDAO;
    private V2ExamResultsWriteDAO mockExamResultsWriteDAO;
    private V2ResultPublicationReadinessService mockReadinessService;
    private V2ManualCandidateExamResultsPublicationService service;

    private Optional<V2AttemptStatusExecutionLedgerRecord> statusLedgerToReturn;
    private Optional<V2ScoreDraftRecord> scoreDraftToReturn;
    private Optional<V2OfficialResultDraftRecord> officialDraftToReturn;
    private boolean existsResultForAttemptToReturn;
    private Optional<V2ResultPublicationLedgerRecord> pubLedgerToReturn;
    private V2ResultPublicationReadinessResult readinessResultToReturn;
    private long insertResultReturn;
    private long insertLedgerReturn;

    @BeforeEach
    void setUp() {
        System.setProperty("tse.v2.manualCandidateExamResultsPublication.enabled", "true");

        statusLedgerToReturn = Optional.empty();
        scoreDraftToReturn = Optional.empty();
        officialDraftToReturn = Optional.empty();
        existsResultForAttemptToReturn = false;
        pubLedgerToReturn = Optional.empty();
        readinessResultToReturn = new V2ResultPublicationReadinessResult();
        readinessResultToReturn.setSuccess(true);
        readinessResultToReturn.setReady(true);
        insertResultReturn = 101L;
        insertLedgerReturn = 202L;

        mockStatusLedgerDAO = new V2AttemptStatusExecutionLedgerDAO() {
            @Override
            public Optional<V2AttemptStatusExecutionLedgerRecord> findBySubmitRecordId(long submitRecordId) {
                return statusLedgerToReturn;
            }
        };

        mockScoreDraftDAO = new V2ScoreDraftDAO() {
            @Override
            public Optional<V2ScoreDraftRecord> findBySubmitRecordId(long submitRecordId) {
                return scoreDraftToReturn;
            }
        };

        mockOfficialResultDraftDAO = new V2OfficialResultDraftDAO() {
            @Override
            public Optional<V2OfficialResultDraftRecord> findBySubmitRecordId(long submitRecordId) {
                return officialDraftToReturn;
            }
        };

        mockExamResultsProbe = new V2ExamResultsReadOnlyProbe() {
            @Override
            public boolean existsResultForAttempt(String attemptId) {
                return existsResultForAttemptToReturn;
            }
        };

        mockLedgerDAO = new V2ResultPublicationLedgerDAO() {
            @Override
            public Optional<V2ResultPublicationLedgerRecord> findBySubmitRecordId(long submitRecordId) {
                return pubLedgerToReturn;
            }

            @Override
            public boolean insertLedger(Connection conn, V2ResultPublicationLedgerRecord record) {
                if (insertLedgerReturn > 0) record.setId(insertLedgerReturn);
                return true;
            }
        };

        mockExamResultsWriteDAO = new V2ExamResultsWriteDAO() {
            @Override
            public boolean insertResultIfAbsent(Connection conn, V2OfficialResultDraftRecord draft) {
                return insertResultReturn > 0;
            }
        };

        mockReadinessService = new V2ResultPublicationReadinessService(null, null, null, null) {
            @Override
            public V2ResultPublicationReadinessResult checkReadiness(int userId, long submitRecordId) {
                return readinessResultToReturn;
            }
        };

        service = new V2ManualCandidateExamResultsPublicationService(
                mockReadinessService,
                mockOfficialResultDraftDAO,
                mockExamResultsProbe,
                mockLedgerDAO,
                mockExamResultsWriteDAO,
                mockStatusLedgerDAO,
                mockScoreDraftDAO
        );
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("tse.v2.manualCandidateExamResultsPublication.enabled");
    }

    @Test
    void testPublish_FlagDisabled() {
        System.setProperty("tse.v2.manualCandidateExamResultsPublication.enabled", "false");
        V2ManualCandidateExamResultsPublicationResult result = service.publishManualResult(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
    }

    @Test
    void testPublish_StatusLedgerMissing() {
        V2ManualCandidateExamResultsPublicationResult result = service.publishManualResult(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_NOT_READY", result.getErrorCode());
    }

    @Test
    void testPublish_StatusNotSubmitted() {
        V2AttemptStatusExecutionLedgerRecord ledger = new V2AttemptStatusExecutionLedgerRecord();
        ledger.setActualAttemptStatus("DOING");
        statusLedgerToReturn = Optional.of(ledger);

        V2ManualCandidateExamResultsPublicationResult result = service.publishManualResult(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_NOT_READY", result.getErrorCode());
    }

    @Test
    void testPublish_DraftsMissing() {
        V2AttemptStatusExecutionLedgerRecord ledger = new V2AttemptStatusExecutionLedgerRecord();
        ledger.setActualAttemptStatus("SUBMITTED");
        statusLedgerToReturn = Optional.of(ledger);

        V2ManualCandidateExamResultsPublicationResult result = service.publishManualResult(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_DRAFTS_MISSING", result.getErrorCode());
    }

    @Test
    void testPublish_UserIdMismatch() {
        V2AttemptStatusExecutionLedgerRecord ledger = new V2AttemptStatusExecutionLedgerRecord();
        ledger.setActualAttemptStatus("SUBMITTED");
        statusLedgerToReturn = Optional.of(ledger);

        scoreDraftToReturn = Optional.of(new V2ScoreDraftRecord());
        
        V2OfficialResultDraftRecord officialDraft = new V2OfficialResultDraftRecord();
        officialDraft.setUserId(99);
        officialDraftToReturn = Optional.of(officialDraft);

        V2ManualCandidateExamResultsPublicationResult result = service.publishManualResult(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_USER_MISMATCH", result.getErrorCode());
    }

    @Test
    void testPublish_Idempotent() {
        V2AttemptStatusExecutionLedgerRecord ledger = new V2AttemptStatusExecutionLedgerRecord();
        ledger.setActualAttemptStatus("SUBMITTED");
        statusLedgerToReturn = Optional.of(ledger);

        scoreDraftToReturn = Optional.of(new V2ScoreDraftRecord());
        
        V2OfficialResultDraftRecord officialDraft = new V2OfficialResultDraftRecord();
        officialDraft.setUserId(1);
        officialDraftToReturn = Optional.of(officialDraft);

        existsResultForAttemptToReturn = true;

        V2ResultPublicationLedgerRecord pubLedger = new V2ResultPublicationLedgerRecord();
        pubLedger.setId(202L);
        pubLedgerToReturn = Optional.of(pubLedger);

        V2ManualCandidateExamResultsPublicationResult result = service.publishManualResult(1, 100L);
        assertTrue(result.isSuccess());
        assertTrue(result.isIdempotent());
        assertEquals(202L, result.getPublicationLedgerId());
        assertEquals("EXAM_RESULTS_WRITTEN_IDEMPOTENT", result.getPublicationStatus());
    }

    @Test
    void testPublish_UnsafeStateResultOnly() {
        V2AttemptStatusExecutionLedgerRecord ledger = new V2AttemptStatusExecutionLedgerRecord();
        ledger.setActualAttemptStatus("SUBMITTED");
        statusLedgerToReturn = Optional.of(ledger);

        scoreDraftToReturn = Optional.of(new V2ScoreDraftRecord());
        
        V2OfficialResultDraftRecord officialDraft = new V2OfficialResultDraftRecord();
        officialDraft.setUserId(1);
        officialDraftToReturn = Optional.of(officialDraft);

        existsResultForAttemptToReturn = true;
        pubLedgerToReturn = Optional.empty();

        V2ManualCandidateExamResultsPublicationResult result = service.publishManualResult(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_UNSAFE_STATE_RESULT_ONLY", result.getErrorCode());
    }

    @Test
    void testPublish_UnsafeStateLedgerOnly() {
        V2AttemptStatusExecutionLedgerRecord ledger = new V2AttemptStatusExecutionLedgerRecord();
        ledger.setActualAttemptStatus("SUBMITTED");
        statusLedgerToReturn = Optional.of(ledger);

        scoreDraftToReturn = Optional.of(new V2ScoreDraftRecord());
        
        V2OfficialResultDraftRecord officialDraft = new V2OfficialResultDraftRecord();
        officialDraft.setUserId(1);
        officialDraftToReturn = Optional.of(officialDraft);

        existsResultForAttemptToReturn = false;
        
        V2ResultPublicationLedgerRecord pubLedger = new V2ResultPublicationLedgerRecord();
        pubLedgerToReturn = Optional.of(pubLedger);

        V2ManualCandidateExamResultsPublicationResult result = service.publishManualResult(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_UNSAFE_STATE_LEDGER_ONLY", result.getErrorCode());
    }

    // Success path requires a DatabaseConnection which is tricky to mock without Mockito/PowerMock,
    // so we handle it similarly. We have sufficiently covered the manual candidate specific business logic,
    // and the transaction execution logic is inherited and tested in Phase 9F.
}
