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

public class V2FinalResultHandoffService {

    private final V2ResultPublicationLedgerDAO ledgerDAO;
    private final V2ExamResultsReadOnlyProbe examResultsProbe;
    private final V2OfficialResultDraftDAO officialResultDraftDAO;
    private final V2ScoreDraftDAO scoreDraftDAO;

    public V2FinalResultHandoffService() {
        this.ledgerDAO = new V2ResultPublicationLedgerDAO();
        this.examResultsProbe = new V2ExamResultsReadOnlyProbeImpl();
        this.officialResultDraftDAO = new V2OfficialResultDraftDAO();
        this.scoreDraftDAO = new V2ScoreDraftDAO();
    }

    public V2FinalResultHandoffService(V2ResultPublicationLedgerDAO ledgerDAO,
                                       V2ExamResultsReadOnlyProbe examResultsProbe,
                                       V2OfficialResultDraftDAO officialResultDraftDAO,
                                       V2ScoreDraftDAO scoreDraftDAO) {
        this.ledgerDAO = ledgerDAO;
        this.examResultsProbe = examResultsProbe;
        this.officialResultDraftDAO = officialResultDraftDAO;
        this.scoreDraftDAO = scoreDraftDAO;
    }

    public V2FinalResultHandoffResult buildHandoff(int userId, long submitRecordId) {
        V2FinalResultHandoffResult result = new V2FinalResultHandoffResult();
        result.setReady(false);
        result.setHandoffStatus("NOT_READY");

        if (!V2SubmitFeatureFlags.isFinalResultHandoffEnabled()) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_FEATURE_DISABLED");
            result.getBlockingReasons().add("V2 Final Result Handoff feature is disabled.");
            return result;
        }

        Optional<V2ResultPublicationLedgerRecord> ledgerOpt = ledgerDAO.findBySubmitRecordId(submitRecordId);
        if (!ledgerOpt.isPresent()) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_LEDGER_MISSING");
            result.getBlockingReasons().add("Publication ledger missing.");
            return result;
        }
        V2ResultPublicationLedgerRecord ledger = ledgerOpt.get();

        if (ledger.getUserId() != userId) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_USER_MISMATCH");
            result.getBlockingReasons().add("User mismatch.");
            return result;
        }

        if (!"EXAM_RESULTS_WRITTEN".equals(ledger.getPublicationStatus())) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_INVALID_PUBLICATION_STATUS");
            result.getBlockingReasons().add("Ledger publication status is not EXAM_RESULTS_WRITTEN.");
            return result;
        }

        if (!examResultsProbe.existsResultForAttempt(ledger.getAttemptId())) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_EXAM_RESULTS_MISSING");
            result.getBlockingReasons().add("exam_results missing.");
            return result;
        }

        Optional<V2OfficialResultDraftRecord> officialOpt = officialResultDraftDAO.findBySubmitRecordId(submitRecordId);
        if (!officialOpt.isPresent()) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_OFFICIAL_DRAFT_MISSING");
            result.getBlockingReasons().add("Official result draft missing.");
            return result;
        }
        V2OfficialResultDraftRecord officialDraft = officialOpt.get();

        Optional<V2ScoreDraftRecord> scoreDraftOpt = scoreDraftDAO.findBySubmitRecordId(submitRecordId);
        if (!scoreDraftOpt.isPresent()) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_SCORE_DRAFT_MISSING");
            result.getBlockingReasons().add("Score draft missing.");
            return result;
        }
        V2ScoreDraftRecord scoreDraft = scoreDraftOpt.get();

        if (Double.compare(ledger.getRawScore(), officialDraft.getRawScore()) != 0 || 
            Double.compare(ledger.getRawScore(), scoreDraft.getRawScore()) != 0) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_SCORE_MISMATCH");
            result.getBlockingReasons().add("Aggregate scores do not match across drafts/ledger.");
            return result;
        }

        // Build successful safe handoff response
        result.setSuccess(true);
        result.setReady(true);
        result.setHandoffStatus("FINAL_RESULT_HANDOFF_READY");
        
        result.setUserId(ledger.getUserId());
        result.setExamId(ledger.getExamId());
        result.setPaperId(ledger.getPaperId());
        result.setAttemptId(ledger.getAttemptId());
        result.setSubmitRecordId(ledger.getSubmitRecordId());
        result.setPublicationLedgerId(ledger.getId());
        result.setOfficialResultDraftId(officialDraft.getId());
        result.setScoreDraftId(scoreDraft.getId());
        result.setPayloadHash(ledger.getPayloadHash());
        
        result.setTotalQuestions(officialDraft.getTotalQuestions());
        result.setAnsweredQuestions(officialDraft.getAnsweredQuestions());
        result.setUnansweredQuestions(officialDraft.getUnansweredQuestions());
        result.setCorrectCount(officialDraft.getCorrectCount());
        result.setIncorrectCount(officialDraft.getIncorrectCount());
        
        result.setRawScore(ledger.getRawScore());
        result.setMaxScore(ledger.getMaxScore());
        result.setPercentage(ledger.getPercentage());
        result.setPublicationStatus(ledger.getPublicationStatus());
        if (ledger.getPublishedAt() != null) {
            result.setPublishedAt(ledger.getPublishedAt().getTime());
        }
        
        return result;
    }
}
