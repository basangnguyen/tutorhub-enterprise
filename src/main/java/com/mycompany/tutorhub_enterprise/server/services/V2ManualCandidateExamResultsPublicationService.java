package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2OfficialResultDraftRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2ResultPublicationLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2ScoreDraftRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptStatusExecutionLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbe;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbeImpl;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsWriteDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2OfficialResultDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ResultPublicationLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ScoreDraftDAO;

import java.sql.Timestamp;
import java.util.Optional;

public class V2ManualCandidateExamResultsPublicationService extends V2ResultPublicationService {

    private final V2AttemptStatusExecutionLedgerDAO statusLedgerDAO;
    private final V2ScoreDraftDAO scoreDraftDAO;
    private final V2OfficialResultDraftDAO officialResultDraftDAO;
    private final V2ExamResultsReadOnlyProbe examResultsProbe;
    private final V2ResultPublicationLedgerDAO ledgerDAO;

    public V2ManualCandidateExamResultsPublicationService() {
        super();
        this.statusLedgerDAO = new V2AttemptStatusExecutionLedgerDAO();
        this.scoreDraftDAO = new V2ScoreDraftDAO();
        this.officialResultDraftDAO = new V2OfficialResultDraftDAO();
        this.examResultsProbe = new V2ExamResultsReadOnlyProbeImpl();
        this.ledgerDAO = new V2ResultPublicationLedgerDAO();
    }

    // For testing
    public V2ManualCandidateExamResultsPublicationService(
            V2ResultPublicationReadinessService readinessService,
            V2OfficialResultDraftDAO officialResultDraftDAO,
            V2ExamResultsReadOnlyProbe examResultsProbe,
            V2ResultPublicationLedgerDAO ledgerDAO,
            V2ExamResultsWriteDAO examResultsWriteDAO,
            V2AttemptStatusExecutionLedgerDAO statusLedgerDAO,
            V2ScoreDraftDAO scoreDraftDAO) {
        super(readinessService, officialResultDraftDAO, examResultsProbe, ledgerDAO, examResultsWriteDAO);
        this.statusLedgerDAO = statusLedgerDAO;
        this.scoreDraftDAO = scoreDraftDAO;
        this.officialResultDraftDAO = officialResultDraftDAO;
        this.examResultsProbe = examResultsProbe;
        this.ledgerDAO = ledgerDAO;
    }

    public V2ManualCandidateExamResultsPublicationResult publishManualResult(int userId, long submitRecordId) {
        V2ManualCandidateExamResultsPublicationResult result = new V2ManualCandidateExamResultsPublicationResult();
        result.setUserId(userId);
        
        try {
            if (!V2SubmitFeatureFlags.isManualCandidateExamResultsPublicationEnabled()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_FEATURE_DISABLED");
                result.addBlockingReason("Manual Candidate Exam Results Publication is disabled.");
                return result;
            }

            // 1. Verify Attempt is SUBMITTED
            var statusLedgerOpt = statusLedgerDAO.findBySubmitRecordId(submitRecordId);
            if (statusLedgerOpt.isEmpty() || !"SUBMITTED".equals(statusLedgerOpt.get().getActualAttemptStatus())) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_NOT_READY");
                result.addBlockingReason("Attempt is not in SUBMITTED state or ledger missing.");
                return result;
            }

            // 2. Fetch Drafts
            Optional<V2ScoreDraftRecord> scoreDraftOpt = scoreDraftDAO.findBySubmitRecordId(submitRecordId);
            Optional<V2OfficialResultDraftRecord> officialDraftOpt = officialResultDraftDAO.findBySubmitRecordId(submitRecordId);

            if (scoreDraftOpt.isEmpty() || officialDraftOpt.isEmpty()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_DRAFTS_MISSING");
                result.addBlockingReason("Score or Official draft missing.");
                return result;
            }

            V2OfficialResultDraftRecord draft = officialDraftOpt.get();
            if (draft.getUserId() != userId) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_USER_MISMATCH");
                result.addBlockingReason("User ID mismatch in draft.");
                return result;
            }

            result.setExamId(draft.getExamId());
            result.setPaperId(draft.getPaperId());
            result.setAttemptId(draft.getAttemptId());

            // 3. Idempotency Check
            boolean resultExists = examResultsProbe.existsResultForAttempt(draft.getAttemptId());
            Optional<V2ResultPublicationLedgerRecord> pubLedgerOpt = ledgerDAO.findBySubmitRecordId(submitRecordId);
            boolean ledgerExists = pubLedgerOpt.isPresent();

            if (resultExists && ledgerExists) {
                result.setSuccess(true);
                result.setReady(true);
                result.setIdempotent(true);
                result.setPublicationStatus("EXAM_RESULTS_WRITTEN_IDEMPOTENT");
                result.setPublicationLedgerId(pubLedgerOpt.get().getId());
                // Notice we do NOT expose the examResultId here to prevent leak, just the publication ledger ID.
                return result;
            }

            if (resultExists && !ledgerExists) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_UNSAFE_STATE_RESULT_ONLY");
                result.addBlockingReason("exam_results exists but ledger missing. Unsafe state.");
                return result;
            }

            if (!resultExists && ledgerExists) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_UNSAFE_STATE_LEDGER_ONLY");
                result.addBlockingReason("Ledger exists but exam_results missing. Unsafe state.");
                return result;
            }

            // 4. Delegate to transaction
            V2ResultPublicationLedgerRecord ledgerRecord = new V2ResultPublicationLedgerRecord();
            ledgerRecord.setSubmitRecordId(submitRecordId);
            ledgerRecord.setOfficialResultDraftId(draft.getId());
            ledgerRecord.setScoreDraftId(draft.getScoreDraftId());
            ledgerRecord.setUserId(draft.getUserId());
            ledgerRecord.setExamId(draft.getExamId());
            ledgerRecord.setPaperId(draft.getPaperId());
            ledgerRecord.setAttemptId(draft.getAttemptId());
            ledgerRecord.setPayloadHash(draft.getPayloadHash());
            ledgerRecord.setRawScore(draft.getRawScore());
            ledgerRecord.setMaxScore(draft.getMaxScore());
            ledgerRecord.setPercentage(draft.getPercentage());
            ledgerRecord.setPublicationStatus("EXAM_RESULTS_WRITTEN");
            ledgerRecord.setPublishedAt(new Timestamp(System.currentTimeMillis()));

            V2ResultPublicationResult parentResult = this.executeTransaction(submitRecordId, draft, ledgerRecord);

            result.setSuccess(parentResult.isSuccess());
            result.setReady(parentResult.isReady());
            result.setIdempotent(parentResult.isIdempotent());
            result.setErrorCode(parentResult.getErrorCode());
            result.setPublicationStatus(parentResult.getPublicationStatus());
            result.setPublicationLedgerId(parentResult.getPublicationLedgerId());
            result.getBlockingReasons().addAll(parentResult.getBlockingReasons());
            result.getWarnings().addAll(parentResult.getWarnings());

        } catch (Exception e) {
            e.printStackTrace();
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_EXCEPTION");
            result.addBlockingReason("Exception: " + e.getMessage());
        }

        return result;
    }
}
