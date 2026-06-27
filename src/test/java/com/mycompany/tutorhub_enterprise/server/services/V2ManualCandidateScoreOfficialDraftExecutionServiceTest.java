package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptStatusExecutionLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2OfficialResultDraftRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2ScoreDraftRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptStatusExecutionLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2OfficialResultDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ScoreDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class V2ManualCandidateScoreOfficialDraftExecutionServiceTest {

    private V2AttemptStatusExecutionLedgerDAO mockStatusLedgerDAO;
    private V2SubmitRecordDAO mockSubmitRecordDAO;
    private V2ScoreDraftDAO mockScoreDraftDAO;
    private V2OfficialResultDraftDAO mockOfficialResultDraftDAO;
    private V2AnswerKeyResolver mockAnswerKeyResolver;
    private V2AnswerPayloadParser mockPayloadParser;
    private V2ManualCandidateScoreOfficialDraftExecutionService service;

    private Optional<V2AttemptStatusExecutionLedgerRecord> statusLedgerToReturn;
    private Optional<V2SubmitRecord> submitRecordToReturn;
    private Optional<V2ScoreDraftRecord> scoreDraftToReturn;
    private Optional<V2OfficialResultDraftRecord> officialDraftToReturn;
    private Map<Long, Long> parsedAnswersToReturn;
    private Map<Long, Long> correctOptionsToReturn;
    private boolean insertScoreDraftResult;
    private boolean insertOfficialDraftResult;

    @BeforeEach
    void setUp() {
        System.setProperty("tse.v2.manualCandidateScoreOfficialDraftExecution.enabled", "true");

        statusLedgerToReturn = Optional.empty();
        submitRecordToReturn = Optional.empty();
        scoreDraftToReturn = Optional.empty();
        officialDraftToReturn = Optional.empty();
        parsedAnswersToReturn = new HashMap<>();
        correctOptionsToReturn = new HashMap<>();
        insertScoreDraftResult = true;
        insertOfficialDraftResult = true;

        mockStatusLedgerDAO = new V2AttemptStatusExecutionLedgerDAO() {
            @Override
            public Optional<V2AttemptStatusExecutionLedgerRecord> findBySubmitRecordId(long submitRecordId) {
                return statusLedgerToReturn;
            }
        };

        mockSubmitRecordDAO = new V2SubmitRecordDAO() {
            @Override
            public Optional<V2SubmitRecord> findById(long id) {
                return submitRecordToReturn;
            }
        };

        mockScoreDraftDAO = new V2ScoreDraftDAO() {
            @Override
            public void ensureTableExists() {}

            @Override
            public Optional<V2ScoreDraftRecord> findBySubmitRecordId(long submitRecordId) {
                return scoreDraftToReturn;
            }

            @Override
            public boolean insert(V2ScoreDraftRecord record) {
                if (insertScoreDraftResult) record.setId(101L);
                return insertScoreDraftResult;
            }
        };

        mockOfficialResultDraftDAO = new V2OfficialResultDraftDAO() {
            @Override
            public void ensureTableExists() {}

            @Override
            public Optional<V2OfficialResultDraftRecord> findBySubmitRecordId(long submitRecordId) {
                return officialDraftToReturn;
            }

            @Override
            public boolean insertDraft(V2OfficialResultDraftRecord record) {
                if (insertOfficialDraftResult) record.setId(202L);
                return insertOfficialDraftResult;
            }
        };

        mockAnswerKeyResolver = new V2AnswerKeyResolver() {
            @Override
            public Map<Long, Long> resolveCorrectOptionIds(int paperId) {
                return correctOptionsToReturn;
            }
        };

        mockPayloadParser = new V2AnswerPayloadParser() {
            @Override
            public Map<Long, Long> extractAnswers(String payloadJson) {
                return parsedAnswersToReturn;
            }
        };

        service = new V2ManualCandidateScoreOfficialDraftExecutionService(
                mockStatusLedgerDAO,
                mockSubmitRecordDAO,
                mockScoreDraftDAO,
                mockOfficialResultDraftDAO,
                mockAnswerKeyResolver,
                mockPayloadParser
        );
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("tse.v2.manualCandidateScoreOfficialDraftExecution.enabled");
    }

    @Test
    void testExecute_FlagDisabled() {
        System.setProperty("tse.v2.manualCandidateScoreOfficialDraftExecution.enabled", "false");
        V2ManualCandidateScoreOfficialDraftExecutionResult result = service.executeDrafts(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
    }

    @Test
    void testExecute_StatusLedgerMissing() {
        V2ManualCandidateScoreOfficialDraftExecutionResult result = service.executeDrafts(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_STATUS_EXECUTION_LEDGER_MISSING", result.getErrorCode());
    }

    @Test
    void testExecute_StatusLedgerInvalid() {
        V2AttemptStatusExecutionLedgerRecord ledger = new V2AttemptStatusExecutionLedgerRecord();
        ledger.setActualAttemptStatus("STARTED");
        statusLedgerToReturn = Optional.of(ledger);

        V2ManualCandidateScoreOfficialDraftExecutionResult result = service.executeDrafts(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_ATTEMPT_STATUS_INVALID", result.getErrorCode());
    }

    @Test
    void testExecute_Idempotent() {
        V2AttemptStatusExecutionLedgerRecord ledger = new V2AttemptStatusExecutionLedgerRecord();
        ledger.setActualAttemptStatus("SUBMITTED");
        statusLedgerToReturn = Optional.of(ledger);

        V2ScoreDraftRecord scoreDraft = new V2ScoreDraftRecord();
        scoreDraft.setId(101L);
        scoreDraftToReturn = Optional.of(scoreDraft);

        V2OfficialResultDraftRecord officialDraft = new V2OfficialResultDraftRecord();
        officialDraft.setId(202L);
        officialDraftToReturn = Optional.of(officialDraft);

        V2ManualCandidateScoreOfficialDraftExecutionResult result = service.executeDrafts(1, 100L);
        assertTrue(result.isSuccess());
        assertTrue(result.isIdempotent());
        assertEquals(101L, result.getScoreDraftId());
        assertEquals(202L, result.getOfficialResultDraftId());
    }

    @Test
    void testExecute_UnsafeStateScoreDraftOnly() {
        V2AttemptStatusExecutionLedgerRecord ledger = new V2AttemptStatusExecutionLedgerRecord();
        ledger.setActualAttemptStatus("SUBMITTED");
        statusLedgerToReturn = Optional.of(ledger);

        V2ScoreDraftRecord scoreDraft = new V2ScoreDraftRecord();
        scoreDraftToReturn = Optional.of(scoreDraft);
        officialDraftToReturn = Optional.empty();

        V2ManualCandidateScoreOfficialDraftExecutionResult result = service.executeDrafts(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_UNSAFE_STATE_SCORE_DRAFT_ONLY", result.getErrorCode());
    }

    @Test
    void testExecute_Success() {
        V2AttemptStatusExecutionLedgerRecord ledger = new V2AttemptStatusExecutionLedgerRecord();
        ledger.setActualAttemptStatus("SUBMITTED");
        ledger.setAttemptId("att-1");
        statusLedgerToReturn = Optional.of(ledger);

        V2SubmitRecord sr = new V2SubmitRecord();
        sr.setAttemptId("att-1");
        sr.setPaperId(10);
        sr.setPayloadJson("{}");
        submitRecordToReturn = Optional.of(sr);

        parsedAnswersToReturn.put(1L, 10L);
        correctOptionsToReturn.put(1L, 10L);
        correctOptionsToReturn.put(2L, 20L);

        V2ManualCandidateScoreOfficialDraftExecutionResult result = service.executeDrafts(1, 100L);
        assertTrue(result.isSuccess());
        assertFalse(result.isIdempotent());
        assertEquals(101L, result.getScoreDraftId());
        assertEquals(202L, result.getOfficialResultDraftId());
        assertEquals("MANUAL_DRAFTS_CREATED", result.getDraftExecutionStatus());
    }
}
