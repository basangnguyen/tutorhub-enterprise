package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2OfficialResultDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ResultPublicationLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ScoreDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2ManualCandidateFinalStatusExecutionServiceTest {

    private V2ManualCandidateFinalStatusExecutionService service;
    private StubFinalAttemptStatusDAO attemptStatusDAO;
    private StubSubmitRecordDAO submitRecordDAO;
    private StubScoreDraftDAO scoreDraftDAO;
    private StubOfficialResultDraftDAO officialResultDraftDAO;
    private StubResultPublicationLedgerDAO publicationLedgerDAO;
    private StubFinalAttemptStatusLedgerDAO ledgerDAO;

    @BeforeEach
    void setUp() {
        System.setProperty("tse.v2.manualCandidateFinalStatusExecution.enabled", "true");

        attemptStatusDAO = new StubFinalAttemptStatusDAO();
        submitRecordDAO = new StubSubmitRecordDAO();
        scoreDraftDAO = new StubScoreDraftDAO();
        officialResultDraftDAO = new StubOfficialResultDraftDAO();
        publicationLedgerDAO = new StubResultPublicationLedgerDAO();
        ledgerDAO = new StubFinalAttemptStatusLedgerDAO();

        service = new V2ManualCandidateFinalStatusExecutionService(
                attemptStatusDAO, submitRecordDAO, scoreDraftDAO,
                officialResultDraftDAO, publicationLedgerDAO, ledgerDAO
        );

        // Setup happy path
        attemptStatusDAO.currentStatus = "SUBMITTED";
        attemptStatusDAO.updateSuccess = true;
        
        submitRecordDAO.recordToReturn = new V2SubmitRecord();
        submitRecordDAO.recordToReturn.setId(10L);
        submitRecordDAO.recordToReturn.setUserId(100);
        submitRecordDAO.recordToReturn.setExamId(200);
        submitRecordDAO.recordToReturn.setPaperId(300);
        submitRecordDAO.recordToReturn.setAttemptId("A-123");
        submitRecordDAO.recordToReturn.setPayloadHash("hash");

        scoreDraftDAO.hasDraft = true;
        officialResultDraftDAO.hasDraft = true;
        publicationLedgerDAO.hasLedger = true;
        ledgerDAO.hasLedger = false;
        ledgerDAO.insertSuccess = true;
        ledgerDAO.insertedId = 55L;
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("tse.v2.manualCandidateFinalStatusExecution.enabled");
    }

    @Test
    void testFeatureFlagDisabled() {
        System.setProperty("tse.v2.manualCandidateFinalStatusExecution.enabled", "false");
        V2ManualCandidateFinalStatusExecutionResult result = service.executeFinalStatus(100, "A-123");
        assertFalse(result.isSuccess());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
    }

    @Test
    void testAttemptNotFound() {
        attemptStatusDAO.currentStatus = null;
        V2ManualCandidateFinalStatusExecutionResult result = service.executeFinalStatus(100, "A-123");
        assertFalse(result.isSuccess());
        assertEquals("ERROR_ATTEMPT_NOT_FOUND", result.getErrorCode());
    }

    @Test
    void testExecutionSuccess() {
        V2ManualCandidateFinalStatusExecutionResult result = service.executeFinalStatus(100, "A-123");
        assertTrue(result.isSuccess());
        assertFalse(result.isIdempotent());
        assertEquals("COMPLETED", result.getFinalStatus());
        assertEquals(55L, result.getFinalStatusLedgerId());
        assertEquals("MANUAL_CANDIDATE_FINAL_STATUS_EXECUTED", result.getExecutionStatus());
    }

    // Stubs
    private static class StubFinalAttemptStatusDAO extends V2FinalAttemptStatusDAO {
        String currentStatus;
        boolean updateSuccess;

        @Override
        public Optional<String> findAttemptStatus(String attemptId) {
            return Optional.ofNullable(currentStatus);
        }

        @Override
        public boolean updateAttemptStatusIfCurrent(Connection conn, String attemptId, String expectedCurrentStatus, String targetStatus) {
            return updateSuccess;
        }
    }

    private static class StubSubmitRecordDAO extends V2SubmitRecordDAO {
        V2SubmitRecord recordToReturn;

        @Override
        public Optional<V2SubmitRecord> findLatestByAttemptId(String attemptId) {
            return Optional.ofNullable(recordToReturn);
        }
    }

    private static class StubScoreDraftDAO extends V2ScoreDraftDAO {
        boolean hasDraft;

        @Override
        public Optional<com.mycompany.tutorhub_enterprise.models.exam.V2ScoreDraftRecord> findBySubmitRecordId(long submitRecordId) {
            return hasDraft ? Optional.of(new com.mycompany.tutorhub_enterprise.models.exam.V2ScoreDraftRecord()) : Optional.empty();
        }
    }

    private static class StubOfficialResultDraftDAO extends V2OfficialResultDraftDAO {
        boolean hasDraft;

        @Override
        public Optional<com.mycompany.tutorhub_enterprise.models.exam.V2OfficialResultDraftRecord> findBySubmitRecordId(long submitRecordId) {
            return hasDraft ? Optional.of(new com.mycompany.tutorhub_enterprise.models.exam.V2OfficialResultDraftRecord()) : Optional.empty();
        }
    }

    private static class StubResultPublicationLedgerDAO extends V2ResultPublicationLedgerDAO {
        boolean hasLedger;

        @Override
        public Optional<com.mycompany.tutorhub_enterprise.models.exam.V2ResultPublicationLedgerRecord> findByAttemptId(String attemptId) {
            return hasLedger ? Optional.of(new com.mycompany.tutorhub_enterprise.models.exam.V2ResultPublicationLedgerRecord()) : Optional.empty();
        }
    }

    private static class StubFinalAttemptStatusLedgerDAO extends V2FinalAttemptStatusLedgerDAO {
        boolean hasLedger;
        boolean insertSuccess;
        long insertedId;

        @Override
        public boolean existsByAttemptId(String attemptId) {
            return hasLedger;
        }

        @Override
        public long insertExecutionLedger(Connection conn, com.mycompany.tutorhub_enterprise.models.exam.V2FinalAttemptStatusLedgerRecord record) {
            return insertSuccess ? insertedId : 0L;
        }
    }
}
