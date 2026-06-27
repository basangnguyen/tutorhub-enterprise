package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2OfficialResultDraftRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2ResultPublicationLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2ScoreDraftRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbe;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbeImpl;
import com.mycompany.tutorhub_enterprise.server.dao.V2OfficialResultDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ResultPublicationLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ScoreDraftDAO;

import java.util.Optional;

public class V2ResultPublicationVerificationService {

    private final V2ResultPublicationLedgerDAO ledgerDAO;
    private final V2ExamResultsReadOnlyProbe examResultsProbe;
    private final V2OfficialResultDraftDAO officialResultDraftDAO;
    private final V2ScoreDraftDAO scoreDraftDAO;

    public V2ResultPublicationVerificationService() {
        this.ledgerDAO = new V2ResultPublicationLedgerDAO();
        this.examResultsProbe = new V2ExamResultsReadOnlyProbeImpl();
        this.officialResultDraftDAO = new V2OfficialResultDraftDAO();
        this.scoreDraftDAO = new V2ScoreDraftDAO();
    }

    public V2ResultPublicationVerificationService(V2ResultPublicationLedgerDAO ledgerDAO,
                                                  V2ExamResultsReadOnlyProbe examResultsProbe,
                                                  V2OfficialResultDraftDAO officialResultDraftDAO,
                                                  V2ScoreDraftDAO scoreDraftDAO) {
        this.ledgerDAO = ledgerDAO;
        this.examResultsProbe = examResultsProbe;
        this.officialResultDraftDAO = officialResultDraftDAO;
        this.scoreDraftDAO = scoreDraftDAO;
    }

    public V2ResultPublicationVerificationResult verify(int userId, long submitRecordId) {
        V2ResultPublicationVerificationResult result = new V2ResultPublicationVerificationResult();
        result.setReady(false);
        result.setVerificationStatus("NOT_READY");
        result.setCheckedAt(System.currentTimeMillis());

        if (!V2SubmitFeatureFlags.isResultPublicationVerificationEnabled()) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_FEATURE_DISABLED");
            result.getBlockingReasons().add("V2 Result Publication Verification feature is disabled.");
            return result;
        }

        Optional<V2ResultPublicationLedgerRecord> ledgerOpt = ledgerDAO.findBySubmitRecordId(submitRecordId);
        result.setLedgerExists(ledgerOpt.isPresent());

        Optional<V2OfficialResultDraftRecord> officialOpt = officialResultDraftDAO.findBySubmitRecordId(submitRecordId);
        result.setOfficialDraftExists(officialOpt.isPresent());

        Optional<V2ScoreDraftRecord> scoreOpt = scoreDraftDAO.findBySubmitRecordId(submitRecordId);
        result.setScoreDraftExists(scoreOpt.isPresent());

        if (!result.isLedgerExists()) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_LEDGER_MISSING");
            result.getBlockingReasons().add("Publication ledger missing.");
            return result;
        }
        V2ResultPublicationLedgerRecord ledger = ledgerOpt.get();

        result.setExamResultsExists(examResultsProbe.existsResultForAttempt(ledger.getAttemptId()));

        if (!result.isExamResultsExists()) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_EXAM_RESULTS_MISSING");
            result.getBlockingReasons().add("exam_results missing.");
            return result;
        }

        if (!result.isOfficialDraftExists()) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_OFFICIAL_DRAFT_MISSING");
            result.getBlockingReasons().add("Official result draft missing.");
            return result;
        }
        V2OfficialResultDraftRecord officialDraft = officialOpt.get();

        if (!result.isScoreDraftExists()) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_SCORE_DRAFT_MISSING");
            result.getBlockingReasons().add("Score draft missing.");
            return result;
        }
        V2ScoreDraftRecord scoreDraft = scoreOpt.get();

        if (ledger.getUserId() != userId) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_USER_MISMATCH");
            result.getBlockingReasons().add("User mismatch.");
            return result;
        }

        if (!ledger.getPayloadHash().equals(officialDraft.getPayloadHash())) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_PAYLOAD_HASH_MISMATCH");
            result.getBlockingReasons().add("Payload hashes do not match across official draft and ledger.");
            return result;
        }

        if (!"EXAM_RESULTS_WRITTEN".equals(ledger.getPublicationStatus())) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_INVALID_PUBLICATION_STATUS");
            result.getBlockingReasons().add("Ledger publication status is not EXAM_RESULTS_WRITTEN.");
            return result;
        }

        // Build successful verification
        result.setSuccess(true);
        result.setReady(true);
        result.setVerificationStatus("RESULT_PUBLICATION_VERIFIED");

        result.setUserId(ledger.getUserId());
        result.setExamId(ledger.getExamId());
        result.setPaperId(ledger.getPaperId());
        result.setAttemptId(ledger.getAttemptId());
        result.setSubmitRecordId(ledger.getSubmitRecordId());
        result.setPublicationLedgerId(ledger.getId());
        result.setPayloadHash(ledger.getPayloadHash());
        result.setPublicationStatus(ledger.getPublicationStatus());

        return result;
    }
}
