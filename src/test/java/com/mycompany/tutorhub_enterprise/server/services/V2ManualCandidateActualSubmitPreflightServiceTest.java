package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbe;
import com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ResultPublicationLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class V2ManualCandidateActualSubmitPreflightServiceTest {

    private V2ManualCandidateFullChainDryRunGateService mockDryRunGateService;
    private V2InMemoryPipelineSimulationService mockSimulationService;
    private V2SubmitRecordDAO mockSubmitRecordDAO;
    private V2ExamResultsReadOnlyProbe mockExamResultsProbe;
    private V2ResultPublicationLedgerDAO mockPublicationLedgerDAO;
    private V2FinalAttemptStatusLedgerDAO mockFinalStatusLedgerDAO;
    private V2ManualCandidateActualSubmitPreflightService service;

    private V2ManualCandidateFullChainDryRunGateResult dryRunGateResultToReturn;
    private V2InMemoryPipelineSimulationResult simulationResultToReturn;
    private Optional<V2SubmitRecord> submitRecordToReturn;
    private boolean existsResultForAttemptToReturn;
    private boolean publicationLedgerExists;
    private boolean finalStatusLedgerExists;

    @BeforeEach
    void setUp() {
        System.setProperty("tse.v2.manualCandidateActualSubmitPreflight.enabled", "true");
        dryRunGateResultToReturn = new V2ManualCandidateFullChainDryRunGateResult();
        simulationResultToReturn = new V2InMemoryPipelineSimulationResult();
        submitRecordToReturn = Optional.empty();
        existsResultForAttemptToReturn = false;
        publicationLedgerExists = false;
        finalStatusLedgerExists = false;

        mockDryRunGateService = new V2ManualCandidateFullChainDryRunGateService(null, null, null, null) {
            @Override
            public V2ManualCandidateFullChainDryRunGateResult checkGate(int userId, String attemptId, String payloadJson) {
                return dryRunGateResultToReturn;
            }
        };

        mockSimulationService = new V2InMemoryPipelineSimulationService(null, null, null) {
            @Override
            public V2InMemoryPipelineSimulationResult simulate(int userId, String attemptId, String payloadJson) {
                return simulationResultToReturn;
            }
        };

        mockSubmitRecordDAO = new V2SubmitRecordDAO() {
            @Override
            public Optional<V2SubmitRecord> findLatestByAttemptId(String attemptId) {
                return submitRecordToReturn;
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

        service = new V2ManualCandidateActualSubmitPreflightService(
                mockDryRunGateService,
                mockSimulationService,
                mockSubmitRecordDAO,
                mockExamResultsProbe,
                mockPublicationLedgerDAO,
                mockFinalStatusLedgerDAO
        );
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("tse.v2.manualCandidateActualSubmitPreflight.enabled");
    }

    @Test
    void testPreflight_FlagDisabled() {
        System.setProperty("tse.v2.manualCandidateActualSubmitPreflight.enabled", "false");
        V2ManualCandidateActualSubmitPreflightResult result = service.checkPreflight(1, "att-1", "{}");
        assertFalse(result.isReady());
        assertEquals("NOT_READY", result.getPreflightStatus());
        assertTrue(result.getBlockingReasons().contains("tse.v2.manualCandidateActualSubmitPreflight.enabled is false"));
    }

    @Test
    void testPreflight_DryRunNotReady() {
        dryRunGateResultToReturn.setReady(false);
        dryRunGateResultToReturn.setBlockingReasons(new java.util.ArrayList<>(java.util.Arrays.asList("some dry run error")));

        V2ManualCandidateActualSubmitPreflightResult result = service.checkPreflight(1, "att-1", "{\"attemptId\":\"att-1\"}");
        assertFalse(result.isReady());
        assertTrue(result.getBlockingReasons().get(0).contains("some dry run error"));
    }

    @Test
    void testPreflight_AttemptIdMismatch() {
        dryRunGateResultToReturn.setReady(true);

        String payloadStr = "{\"attemptId\":\"att-2\", \"paperId\":1}";
        V2ManualCandidateActualSubmitPreflightResult result = service.checkPreflight(1, "att-1", payloadStr);
        assertFalse(result.isReady());
        assertTrue(result.getBlockingReasons().contains("attemptId mismatch"));
    }

    @Test
    void testPreflight_PaperIdMismatch() {
        dryRunGateResultToReturn.setReady(true);
        dryRunGateResultToReturn.setPaperId(10);

        String payloadStr = "{\"attemptId\":\"att-1\", \"paperId\":99}";
        V2ManualCandidateActualSubmitPreflightResult result = service.checkPreflight(1, "att-1", payloadStr);
        assertFalse(result.isReady());
        assertTrue(result.getBlockingReasons().contains("paperId mismatch"));
    }

    @Test
    void testPreflight_SimulationFailed() {
        dryRunGateResultToReturn.setReady(true);
        dryRunGateResultToReturn.setPaperId(10);
        simulationResultToReturn.setSuccess(false);

        String payloadStr = "{\"attemptId\":\"att-1\", \"paperId\":10}";
        V2ManualCandidateActualSubmitPreflightResult result = service.checkPreflight(1, "att-1", payloadStr);
        assertFalse(result.isReady());
        assertTrue(result.getBlockingReasons().contains("In-memory pipeline simulation failed"));
    }

    @Test
    void testPreflight_ExistingRecords() {
        dryRunGateResultToReturn.setReady(true);
        dryRunGateResultToReturn.setPaperId(10);
        simulationResultToReturn.setSuccess(true);
        submitRecordToReturn = Optional.of(new V2SubmitRecord());

        String payloadStr = "{\"attemptId\":\"att-1\", \"paperId\":10}";
        V2ManualCandidateActualSubmitPreflightResult result = service.checkPreflight(1, "att-1", payloadStr);
        assertFalse(result.isReady());
        assertTrue(result.getBlockingReasons().contains("Existing V2SubmitRecord found for attempt"));
    }

    @Test
    void testPreflight_Success() {
        dryRunGateResultToReturn.setReady(true);
        dryRunGateResultToReturn.setPaperId(10);
        simulationResultToReturn.setSuccess(true);

        String payloadStr = "{\"attemptId\":\"att-1\", \"paperId\":10}";
        V2ManualCandidateActualSubmitPreflightResult result = service.checkPreflight(1, "att-1", payloadStr);
        assertTrue(result.isReady());
        assertEquals("READY_FOR_V2_SUBMIT_RECORD_MATERIALIZATION", result.getPreflightStatus());
    }
}
