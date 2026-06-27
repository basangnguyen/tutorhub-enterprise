package com.mycompany.tutorhub_enterprise.server.services;

import java.time.Instant;

import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbe;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbeImpl;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ResultPublicationLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusLedgerDAO;

public class V2CandidateSubmitOrchestratorGateService {

    private final V2ManualCandidateSubmitCheckService manualCheckService;
    private final V2ExamResultsReadOnlyProbe examResultsProbe;
    private final V2FinalAttemptStatusLedgerDAO finalStatusLedgerDAO;
    private final V2SubmitRecordDAO submitRecordDAO;
    private final V2ResultPublicationLedgerDAO publicationLedgerDAO;

    public V2CandidateSubmitOrchestratorGateService() {
        this.manualCheckService = new V2ManualCandidateSubmitCheckService();
        this.examResultsProbe = new V2ExamResultsReadOnlyProbeImpl();
        this.finalStatusLedgerDAO = new V2FinalAttemptStatusLedgerDAO();
        this.submitRecordDAO = new V2SubmitRecordDAO();
        this.publicationLedgerDAO = new V2ResultPublicationLedgerDAO();
    }

    protected V2ManualCandidateSubmitCheckResult checkManualCandidateSubmit(int userId, String attemptId) {
        return manualCheckService.checkCandidateSubmit(userId, attemptId);
    }

    protected boolean doesSubmitRecordExist(String attemptId) {
        try {
            return submitRecordDAO.findLatestByAttemptId(attemptId).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean doesLedgerExist(String attemptId) {
        try {
            return publicationLedgerDAO.findByAttemptId(attemptId).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean doesExamResultsExist(String attemptId) {
        return examResultsProbe.existsResultForAttempt(attemptId);
    }

    protected boolean doesFinalStatusLedgerExist(String attemptId) {
        return finalStatusLedgerDAO.existsByAttemptId(attemptId);
    }

    public V2CandidateSubmitOrchestratorGateResult checkGate(int userId, String attemptId) {
        V2CandidateSubmitOrchestratorGateResult result = new V2CandidateSubmitOrchestratorGateResult();
        result.setCheckedAt(Instant.now().toString());
        result.setAttemptId(attemptId);
        result.setUserId(userId);

        if (!V2SubmitFeatureFlags.isCandidateSubmitOrchestratorGateEnabled()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_FEATURE_DISABLED");
            result.setOrchestratorGateStatus("NOT_READY");
            result.addBlockingReason("Candidate Submit Orchestrator Gate flag is disabled.");
            return result;
        }

        V2ManualCandidateSubmitCheckResult manualResult = checkManualCandidateSubmit(userId, attemptId);
        if (!manualResult.isReady()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_MANUAL_CHECK_FAILED");
            result.setOrchestratorGateStatus("NOT_READY");
            result.addBlockingReason("Manual candidate check is not ready: " + manualResult.getErrorCode());
            return result;
        }

        result.setExamId(manualResult.getExamId());
        result.setPaperId(manualResult.getPaperId());

        if (doesSubmitRecordExist(attemptId)) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_EXISTING_SUBMIT_RECORD");
            result.setOrchestratorGateStatus("NOT_READY");
            result.addBlockingReason("An existing published submit record was found for this attempt.");
            return result;
        }

        if (doesExamResultsExist(attemptId)) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_EXISTING_EXAM_RESULTS");
            result.setOrchestratorGateStatus("NOT_READY");
            result.addBlockingReason("Exam results already exist for this attempt.");
            return result;
        }

        if (doesLedgerExist(attemptId) || doesFinalStatusLedgerExist(attemptId)) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_EXISTING_LEDGER");
            result.setOrchestratorGateStatus("NOT_READY");
            result.addBlockingReason("An existing finalization or status ledger was found for this attempt.");
            return result;
        }

        result.setSuccess(true);
        result.setReady(true);
        result.setOrchestratorGateStatus("READY_FOR_MANUAL_CANDIDATE_V2_SUBMIT_TRIGGER");
        result.setCandidateStatus(manualResult.getCandidateStatus());

        return result;
    }
}
