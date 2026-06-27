package com.mycompany.tutorhub_enterprise.server.services;

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
