package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2ManualCandidateSubmitRecordLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbe;
import com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ManualCandidateSubmitRecordLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ResultPublicationLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class V2ManualCandidatePublishFinalStatusGateServiceTest {

    private V2SubmitRecordDAO mockSubmitRecordDAO;
    private V2ManualCandidateSubmitRecordLedgerDAO mockLedgerDAO;
    private V2ScoreDraftDependencyHealthService mockHealthService;
    private V2ExamResultsReadOnlyProbe mockExamResultsProbe;
    private V2ResultPublicationLedgerDAO mockPublicationLedgerDAO;
    private V2FinalAttemptStatusLedgerDAO mockFinalStatusLedgerDAO;
    private V2ManualCandidatePublishFinalStatusGateService service;

    private Optional<V2SubmitRecord> submitRecordToReturn;
    private Optional<V2ManualCandidateSubmitRecordLedgerRecord> ledgerRecordToReturn;
    private V2ScoreDraftDependencyHealthResult healthResultToReturn;
    private boolean existsResultForAttemptToReturn;
    private boolean publicationLedgerExists;
    private boolean finalStatusLedgerExists;

    @BeforeEach
    void setUp() {
        System.setProperty("tse.v2.manualCandidatePublishFinalStatusOrchestratorGate.enabled", "true");

        submitRecordToReturn = Optional.empty();
        ledgerRecordToReturn = Optional.empty();
        healthResultToReturn = new V2ScoreDraftDependencyHealthResult();
        existsResultForAttemptToReturn = false;
        publicationLedgerExists = false;
        finalStatusLedgerExists = false;

        mockSubmitRecordDAO = new V2SubmitRecordDAO() {
            @Override
            public Optional<V2SubmitRecord> findLatestByAttemptId(String attemptId) {
                return submitRecordToReturn;
            }
        };

        mockLedgerDAO = new V2ManualCandidateSubmitRecordLedgerDAO() {
            @Override
            public Optional<V2ManualCandidateSubmitRecordLedgerRecord> findByAttemptId(String attemptId) {
                return ledgerRecordToReturn;
            }
        };

        mockHealthService = new V2ScoreDraftDependencyHealthService(null, null) {
            @Override
            public V2ScoreDraftDependencyHealthResult checkHealth(int userId, String attemptId) {
                return healthResultToReturn;
            }
        };

        mockExamResultsProbe = new V2ExamResultsReadOnlyProbe() {
            @Override
            public boolean existsResultForAttempt(String attemptId) {
                return existsResultForAttemptToReturn;
            }
        };

        mockPublicationLedgerDAO = new V2ResultPublicationLedgerDAO() {
            @Override
            public Optional<com.mycompany.tutorhub_enterprise.models.exam.V2ResultPublicationLedgerRecord> findByAttemptId(String attemptId) {
                return publicationLedgerExists ? Optional.of(new com.mycompany.tutorhub_enterprise.models.exam.V2ResultPublicationLedgerRecord()) : Optional.empty();
            }
        };

        mockFinalStatusLedgerDAO = new V2FinalAttemptStatusLedgerDAO() {
            @Override
            public boolean existsByAttemptId(String attemptId) {
                return finalStatusLedgerExists;
            }
        };

        service = new V2ManualCandidatePublishFinalStatusGateService(
                mockSubmitRecordDAO,
                mockLedgerDAO,
                mockHealthService,
                mockExamResultsProbe,
                mockPublicationLedgerDAO,
                mockFinalStatusLedgerDAO
        );
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("tse.v2.manualCandidatePublishFinalStatusOrchestratorGate.enabled");
    }

    @Test
    void testGate_FlagDisabled() {
        System.setProperty("tse.v2.manualCandidatePublishFinalStatusOrchestratorGate.enabled", "false");
        V2ManualCandidatePublishFinalStatusGateResult result = service.checkGate(1, "att-1");
        assertFalse(result.isReady());
        assertEquals("NOT_READY", result.getGateStatus());
    }

    @Test
    void testGate_MissingMaterialization() {
        submitRecordToReturn = Optional.empty();
        ledgerRecordToReturn = Optional.empty();

        V2ManualCandidatePublishFinalStatusGateResult result = service.checkGate(1, "att-1");
        assertFalse(result.isReady());
        assertTrue(result.getBlockingReasons().contains("Materialized submit record or ledger is missing"));
    }

    @Test
    void testGate_HashMismatch() {
        V2SubmitRecord sr = new V2SubmitRecord();
        sr.setPayloadHash("hash1");
        V2ManualCandidateSubmitRecordLedgerRecord ledger = new V2ManualCandidateSubmitRecordLedgerRecord();
        ledger.setPayloadHash("hash2");

        submitRecordToReturn = Optional.of(sr);
        ledgerRecordToReturn = Optional.of(ledger);

        V2ManualCandidatePublishFinalStatusGateResult result = service.checkGate(1, "att-1");
        assertFalse(result.isReady());
        assertTrue(result.getBlockingReasons().contains("PayloadHash mismatch between submit record and ledger"));
    }

    @Test
    void testGate_DependenciesNotReady() {
        V2SubmitRecord sr = new V2SubmitRecord();
        sr.setPayloadHash("hash1");
        V2ManualCandidateSubmitRecordLedgerRecord ledger = new V2ManualCandidateSubmitRecordLedgerRecord();
        ledger.setPayloadHash("hash1");

        submitRecordToReturn = Optional.of(sr);
        ledgerRecordToReturn = Optional.of(ledger);

        healthResultToReturn.setReady(false);

        V2ManualCandidatePublishFinalStatusGateResult result = service.checkGate(1, "att-1");
        assertFalse(result.isReady());
        assertTrue(result.getBlockingReasons().contains("ScoreDraft dependencies not ready"));
    }

    @Test
    void testGate_Success() {
        V2SubmitRecord sr = new V2SubmitRecord();
        sr.setPayloadHash("hash1");
        V2ManualCandidateSubmitRecordLedgerRecord ledger = new V2ManualCandidateSubmitRecordLedgerRecord();
        ledger.setPayloadHash("hash1");

        submitRecordToReturn = Optional.of(sr);
        ledgerRecordToReturn = Optional.of(ledger);

        healthResultToReturn.setReady(true);
        existsResultForAttemptToReturn = false;
        publicationLedgerExists = false;
        finalStatusLedgerExists = false;

        V2ManualCandidatePublishFinalStatusGateResult result = service.checkGate(1, "att-1");
        assertTrue(result.isReady());
        assertEquals("READY_FOR_MANUAL_CANDIDATE_PUBLISH_FINAL_STATUS_EXECUTION", result.getGateStatus());
    }
}
