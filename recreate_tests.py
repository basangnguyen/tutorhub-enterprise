import os

def rewrite_final_status_test():
    content = """package com.mycompany.tutorhub_enterprise.server.services;

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
"""
    with open(r"D:\Ban_sao_du_an\src\test\java\com\mycompany\tutorhub_enterprise\server\services\V2ManualCandidateFinalStatusExecutionServiceTest.java", "w", encoding="utf-8") as f:
        f.write(content)

def rewrite_handoff_test():
    content = """package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbeImpl;
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

public class V2ManualCandidateResultHandoffVerificationServiceTest {

    private V2ManualCandidateResultHandoffVerificationService service;
    private StubFinalAttemptStatusDAO attemptStatusDAO;
    private StubSubmitRecordDAO submitRecordDAO;
    private StubScoreDraftDAO scoreDraftDAO;
    private StubOfficialResultDraftDAO officialResultDraftDAO;
    private StubResultPublicationLedgerDAO publicationLedgerDAO;
    private StubFinalAttemptStatusLedgerDAO finalStatusLedgerDAO;
    private StubExamResultDAO examResultDAO;

    @BeforeEach
    void setUp() {
        System.setProperty("tse.v2.manualCandidateResultHandoffVerification.enabled", "true");

        attemptStatusDAO = new StubFinalAttemptStatusDAO();
        submitRecordDAO = new StubSubmitRecordDAO();
        scoreDraftDAO = new StubScoreDraftDAO();
        officialResultDraftDAO = new StubOfficialResultDraftDAO();
        publicationLedgerDAO = new StubResultPublicationLedgerDAO();
        finalStatusLedgerDAO = new StubFinalAttemptStatusLedgerDAO();
        examResultDAO = new StubExamResultDAO();

        service = new V2ManualCandidateResultHandoffVerificationService(
                attemptStatusDAO, submitRecordDAO, scoreDraftDAO, officialResultDraftDAO,
                publicationLedgerDAO, finalStatusLedgerDAO, examResultDAO
        );

        // Setup happy path
        submitRecordDAO.record = new V2SubmitRecord();
        submitRecordDAO.record.setId(10L);
        submitRecordDAO.record.setUserId(100);
        submitRecordDAO.record.setExamId(200);
        submitRecordDAO.record.setPaperId(300);
        submitRecordDAO.record.setAttemptId("A-123");

        attemptStatusDAO.status = "COMPLETED";
        finalStatusLedgerDAO.hasLedger = true;
        examResultDAO.hasResult = true;
        publicationLedgerDAO.hasLedger = true;
        scoreDraftDAO.hasDraft = true;
        officialResultDraftDAO.hasDraft = true;
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("tse.v2.manualCandidateResultHandoffVerification.enabled");
    }

    @Test
    void testHandoffVerifiedSuccess() {
        V2ManualCandidateResultHandoffVerificationResult result = service.verifyHandoff(100, "A-123");
        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertEquals("MANUAL_CANDIDATE_RESULT_HANDOFF_VERIFIED", result.getHandoffStatus());
    }

    // Stubs
    private static class StubFinalAttemptStatusDAO extends V2FinalAttemptStatusDAO {
        String status;
        @Override
        public Optional<String> findAttemptStatus(String attemptId) {
            return Optional.ofNullable(status);
        }
    }

    private static class StubSubmitRecordDAO extends V2SubmitRecordDAO {
        V2SubmitRecord record;
        @Override
        public Optional<V2SubmitRecord> findLatestByAttemptId(String attemptId) {
            return Optional.ofNullable(record);
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
        @Override
        public boolean existsByAttemptId(String attemptId) {
            return hasLedger;
        }
    }

    private static class StubExamResultDAO extends V2ExamResultsReadOnlyProbeImpl {
        boolean hasResult;
        @Override
        public boolean existsResultForAttempt(String attemptId) {
            return hasResult;
        }
    }
}
"""
    with open(r"D:\Ban_sao_du_an\src\test\java\com\mycompany\tutorhub_enterprise\server\services\V2ManualCandidateResultHandoffVerificationServiceTest.java", "w", encoding="utf-8") as f:
        f.write(content)

rewrite_final_status_test()
rewrite_handoff_test()
