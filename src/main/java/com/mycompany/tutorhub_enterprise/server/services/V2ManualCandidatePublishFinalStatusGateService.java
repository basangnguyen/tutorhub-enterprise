package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2ManualCandidateSubmitRecordLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbe;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbeImpl;
import com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ManualCandidateSubmitRecordLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ResultPublicationLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;

import java.sql.SQLException;
import java.util.Optional;

public class V2ManualCandidatePublishFinalStatusGateService {

    private final V2SubmitRecordDAO submitRecordDAO;
    private final V2ManualCandidateSubmitRecordLedgerDAO ledgerDAO;
    private final V2ScoreDraftDependencyHealthService healthService;
    private final V2ExamResultsReadOnlyProbe examResultsProbe;
    private final V2ResultPublicationLedgerDAO publicationLedgerDAO;
    private final V2FinalAttemptStatusLedgerDAO finalStatusLedgerDAO;

    public V2ManualCandidatePublishFinalStatusGateService() {
        this(
                new V2SubmitRecordDAO(),
                new V2ManualCandidateSubmitRecordLedgerDAO(),
                new V2ScoreDraftDependencyHealthService(),
                new V2ExamResultsReadOnlyProbeImpl(),
                new V2ResultPublicationLedgerDAO(),
                new V2FinalAttemptStatusLedgerDAO()
        );
    }

    public V2ManualCandidatePublishFinalStatusGateService(
            V2SubmitRecordDAO submitRecordDAO,
            V2ManualCandidateSubmitRecordLedgerDAO ledgerDAO,
            V2ScoreDraftDependencyHealthService healthService,
            V2ExamResultsReadOnlyProbe examResultsProbe,
            V2ResultPublicationLedgerDAO publicationLedgerDAO,
            V2FinalAttemptStatusLedgerDAO finalStatusLedgerDAO) {
        this.submitRecordDAO = submitRecordDAO;
        this.ledgerDAO = ledgerDAO;
        this.healthService = healthService;
        this.examResultsProbe = examResultsProbe;
        this.publicationLedgerDAO = publicationLedgerDAO;
        this.finalStatusLedgerDAO = finalStatusLedgerDAO;
    }

    public V2ManualCandidatePublishFinalStatusGateResult checkGate(int userId, String attemptId) {
        V2ManualCandidatePublishFinalStatusGateResult result = new V2ManualCandidatePublishFinalStatusGateResult();
        result.setSuccess(true);
        result.setReady(false);
        result.setUserId(userId);
        result.setAttemptId(attemptId);

        if (!V2SubmitFeatureFlags.isManualCandidatePublishFinalStatusOrchestratorGateEnabled()) {
            result.setGateStatus("NOT_READY");
            result.getBlockingReasons().add("tse.v2.manualCandidatePublishFinalStatusOrchestratorGate.enabled is false");
            return result;
        }

        try {
            // 1. Materialization must exist
            Optional<V2SubmitRecord> submitRecordOpt = submitRecordDAO.findLatestByAttemptId(attemptId);
            Optional<V2ManualCandidateSubmitRecordLedgerRecord> ledgerOpt = ledgerDAO.findByAttemptId(attemptId);

            if (submitRecordOpt.isEmpty() || ledgerOpt.isEmpty()) {
                result.setGateStatus("NOT_READY");
                result.getBlockingReasons().add("Materialized submit record or ledger is missing");
                return result;
            }

            V2SubmitRecord record = submitRecordOpt.get();
            V2ManualCandidateSubmitRecordLedgerRecord ledger = ledgerOpt.get();

            if (!record.getPayloadHash().equals(ledger.getPayloadHash())) {
                result.setGateStatus("NOT_READY");
                result.getBlockingReasons().add("PayloadHash mismatch between submit record and ledger");
                return result;
            }

            // 2. ScoreDraft dependencies must be ready
            V2ScoreDraftDependencyHealthResult healthResult = healthService.checkHealth(userId, attemptId);
            if (!healthResult.isReady()) {
                result.setGateStatus("NOT_READY");
                result.getBlockingReasons().add("ScoreDraft dependencies not ready");
                return result;
            }

            // 3. Post-execution tables should NOT have existing records
            if (examResultsProbe.existsResultForAttempt(attemptId)) {
                result.setGateStatus("NOT_READY");
                result.getBlockingReasons().add("Existing exam_results found for attempt");
                return result;
            }

            if (publicationLedgerDAO.findByAttemptId(attemptId).isPresent()) {
                result.setGateStatus("NOT_READY");
                result.getBlockingReasons().add("Existing publication ledger found for attempt");
                return result;
            }

            if (finalStatusLedgerDAO.existsByAttemptId(attemptId)) {
                result.setGateStatus("NOT_READY");
                result.getBlockingReasons().add("Existing final status ledger found for attempt");
                return result;
            }

            result.setReady(true);
            result.setGateStatus("READY_FOR_MANUAL_CANDIDATE_PUBLISH_FINAL_STATUS_EXECUTION");

        } catch (SQLException e) {
            result.setSuccess(false);
            result.setErrorCode("DB_ERROR");
            result.setGateStatus("ERROR");
            result.getBlockingReasons().add("Database error checking gate conditions");
        }

        return result;
    }
}
