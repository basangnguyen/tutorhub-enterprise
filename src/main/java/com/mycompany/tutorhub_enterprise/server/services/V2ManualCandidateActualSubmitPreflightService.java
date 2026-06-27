package com.mycompany.tutorhub_enterprise.server.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbe;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbeImpl;
import com.mycompany.tutorhub_enterprise.server.dao.V2ResultPublicationLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;

import java.sql.SQLException;
import java.util.Optional;

public class V2ManualCandidateActualSubmitPreflightService {

    private final V2ManualCandidateFullChainDryRunGateService dryRunGateService;
    private final V2InMemoryPipelineSimulationService simulationService;
    private final V2SubmitRecordDAO submitRecordDAO;
    private final V2ExamResultsReadOnlyProbe examResultsProbe;
    private final V2ResultPublicationLedgerDAO publicationLedgerDAO;
    private final V2FinalAttemptStatusLedgerDAO finalStatusLedgerDAO;

    public V2ManualCandidateActualSubmitPreflightService() {
        this(
                new V2ManualCandidateFullChainDryRunGateService(),
                new V2InMemoryPipelineSimulationService(),
                new V2SubmitRecordDAO(),
                new V2ExamResultsReadOnlyProbeImpl(),
                new V2ResultPublicationLedgerDAO(),
                new V2FinalAttemptStatusLedgerDAO()
        );
    }

    public V2ManualCandidateActualSubmitPreflightService(
            V2ManualCandidateFullChainDryRunGateService dryRunGateService,
            V2InMemoryPipelineSimulationService simulationService,
            V2SubmitRecordDAO submitRecordDAO,
            V2ExamResultsReadOnlyProbe examResultsProbe,
            V2ResultPublicationLedgerDAO publicationLedgerDAO,
            V2FinalAttemptStatusLedgerDAO finalStatusLedgerDAO) {
        this.dryRunGateService = dryRunGateService;
        this.simulationService = simulationService;
        this.submitRecordDAO = submitRecordDAO;
        this.examResultsProbe = examResultsProbe;
        this.publicationLedgerDAO = publicationLedgerDAO;
        this.finalStatusLedgerDAO = finalStatusLedgerDAO;
    }

    public V2ManualCandidateActualSubmitPreflightResult checkPreflight(int userId, String attemptId, String payloadJson) {
        V2ManualCandidateActualSubmitPreflightResult result = new V2ManualCandidateActualSubmitPreflightResult();
        result.setSuccess(true);
        result.setUserId(userId);
        result.setAttemptId(attemptId);
        result.setReady(false);

        if (!V2SubmitFeatureFlags.isManualCandidateActualSubmitPreflightEnabled()) {
            result.setPreflightStatus("NOT_READY");
            result.getBlockingReasons().add("tse.v2.manualCandidateActualSubmitPreflight.enabled is false");
            return result;
        }

        // 1. Check Dry-run Gate
        V2ManualCandidateFullChainDryRunGateResult dryRunResult = dryRunGateService.checkGate(userId, attemptId, payloadJson);
        if (!dryRunResult.isReady()) {
            result.setPreflightStatus("NOT_READY");
            result.getBlockingReasons().add("Full-chain dry-run gate not ready: " + String.join(", ", dryRunResult.getBlockingReasons()));
            return result;
        }
        result.setExamId(dryRunResult.getExamId());
        result.setPaperId(dryRunResult.getPaperId());

        // Payload id match checks
        try {
            JsonObject root = JsonParser.parseString(payloadJson).getAsJsonObject();
            String payloadAttemptId = root.has("attemptId") ? root.get("attemptId").getAsString() : "";
            int payloadPaperId = root.has("paperId") ? root.get("paperId").getAsInt() : -1;

            if (!attemptId.equals(payloadAttemptId)) {
                result.setPreflightStatus("NOT_READY");
                result.getBlockingReasons().add("attemptId mismatch");
                return result;
            }
            if (result.getPaperId() != payloadPaperId) {
                result.setPreflightStatus("NOT_READY");
                result.getBlockingReasons().add("paperId mismatch");
                return result;
            }
        } catch (Exception e) {
            result.setPreflightStatus("NOT_READY");
            result.getBlockingReasons().add("Invalid JSON payload");
            return result;
        }

        // 2. Check Simulation
        V2InMemoryPipelineSimulationResult simResult = simulationService.simulate(userId, attemptId, payloadJson);
        if (!simResult.isSuccess()) {
            result.setPreflightStatus("NOT_READY");
            result.getBlockingReasons().add("In-memory pipeline simulation failed");
            return result;
        }
        result.setAnswerCount(simResult.getQuestionCount()); // aggregate internally

        // 3. Existing records check
        try {
            Optional<V2SubmitRecord> existingSubmitRecord = submitRecordDAO.findLatestByAttemptId(attemptId);
            if (existingSubmitRecord.isPresent()) {
                result.setPreflightStatus("NOT_READY");
                result.getBlockingReasons().add("Existing V2SubmitRecord found for attempt");
                return result;
            }

            if (examResultsProbe.existsResultForAttempt(attemptId)) {
                result.setPreflightStatus("NOT_READY");
                result.getBlockingReasons().add("Existing exam_results found for attempt");
                return result;
            }

            if (publicationLedgerDAO.findByAttemptId(attemptId).isPresent()) {
                result.setPreflightStatus("NOT_READY");
                result.getBlockingReasons().add("Existing publication ledger found for attempt");
                return result;
            }

            if (finalStatusLedgerDAO.existsByAttemptId(attemptId)) {
                result.setPreflightStatus("NOT_READY");
                result.getBlockingReasons().add("Existing final status ledger found for attempt");
                return result;
            }
        } catch (SQLException e) {
            result.setSuccess(false);
            result.setErrorCode("DB_ERROR");
            result.setPreflightStatus("ERROR");
            result.getBlockingReasons().add("Database error during preflight checks");
            return result;
        }

        result.setReady(true);
        result.setPreflightStatus("READY_FOR_V2_SUBMIT_RECORD_MATERIALIZATION");
        return result;
    }
}
