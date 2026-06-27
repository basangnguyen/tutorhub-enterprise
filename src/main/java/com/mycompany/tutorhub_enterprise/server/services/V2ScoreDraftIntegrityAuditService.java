package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2ScoreDraftRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ScoreDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;

import java.util.Optional;

public class V2ScoreDraftIntegrityAuditService {
    private final V2SubmitFeatureFlags featureFlags;
    private final V2ScoreDraftDAO scoreDraftDAO;
    private final V2SubmitRecordDAO submitRecordDAO;

    public V2ScoreDraftIntegrityAuditService(
            V2SubmitFeatureFlags featureFlags,
            V2ScoreDraftDAO scoreDraftDAO,
            V2SubmitRecordDAO submitRecordDAO) {
        this.featureFlags = featureFlags;
        this.scoreDraftDAO = scoreDraftDAO;
        this.submitRecordDAO = submitRecordDAO;
    }

    public V2ScoreDraftIntegrityAuditResult audit(int userId, long submitRecordId) {
        V2ScoreDraftIntegrityAuditResult result = new V2ScoreDraftIntegrityAuditResult();
        result.setSubmitRecordId(submitRecordId);
        result.setUserId(userId);
        result.setSuccess(false);
        result.setReady(false);

        if (!featureFlags.isScoreDraftIntegrityAuditEnabled()) {
            result.setErrorCode("FEATURE_DISABLED");
            result.setAuditStatus("NOT_READY");
            result.addBlockingReason("tse.v2.scoreDraftIntegrityAudit.enabled is false");
            return result;
        }

        Optional<V2ScoreDraftRecord> draftOpt = scoreDraftDAO.findBySubmitRecordId(submitRecordId);
        if (!draftOpt.isPresent()) {
            result.setErrorCode("SCORE_DRAFT_NOT_FOUND");
            result.setAuditStatus("NOT_READY");
            result.addBlockingReason("Score draft not found for submitRecordId: " + submitRecordId);
            return result;
        }

        V2ScoreDraftRecord draft = draftOpt.get();
        result.setScoreDraftId(draft.getId());
        result.setExamId(draft.getExamId());
        result.setPaperId(draft.getPaperId());
        result.setAttemptId(draft.getAttemptId());
        result.setTotalQuestions(draft.getTotalQuestions());
        result.setAnsweredQuestions(draft.getAnsweredQuestions());
        result.setUnansweredQuestions(draft.getUnansweredQuestions());
        result.setCorrectCount(draft.getCorrectCount());
        result.setIncorrectCount(draft.getIncorrectCount());
        result.setRawScore(draft.getRawScore());
        result.setMaxScore(draft.getMaxScore());
        result.setPercentage(draft.getPercentage());
        result.setScoreDraftStatus("SCORE_DRAFTED_SERVER_SIDE");

        if (draft.getUserId() != userId) {
            result.setErrorCode("USER_MISMATCH");
            result.setAuditStatus("NOT_READY");
            result.addBlockingReason("Score draft belongs to a different user");
            return result;
        }

        Optional<V2SubmitRecord> submitRecordOpt;
        try {
            submitRecordOpt = submitRecordDAO.findById(submitRecordId);
        } catch (Exception e) {
            result.setErrorCode("DATABASE_ERROR");
            result.setAuditStatus("NOT_READY");
            result.addBlockingReason("Database error: " + e.getMessage());
            return result;
        }

        if (!submitRecordOpt.isPresent()) {
            result.setErrorCode("SUBMIT_RECORD_NOT_FOUND");
            result.setAuditStatus("NOT_READY");
            result.addBlockingReason("Submit record not found");
            return result;
        }

        String payloadHash = submitRecordOpt.get().getPayloadHash();
        result.setPayloadHash(payloadHash);
        
        if (payloadHash == null || payloadHash.length() != 64) {
            result.setErrorCode("INVALID_PAYLOAD_HASH");
            result.setAuditStatus("NOT_READY");
            result.addBlockingReason("Payload hash is null or not 64 chars");
            return result;
        }

        if (draft.getTotalQuestions() < draft.getAnsweredQuestions()) {
            result.setErrorCode("INVALID_TOTALS_MATH");
            result.setAuditStatus("NOT_READY");
            result.addBlockingReason("Total questions cannot be less than answered questions");
            return result;
        }

        if (draft.getTotalQuestions() != (draft.getCorrectCount() + draft.getIncorrectCount() + draft.getUnansweredQuestions())) {
            result.setErrorCode("INVALID_TOTALS_MATH");
            result.setAuditStatus("NOT_READY");
            result.addBlockingReason("Math mismatch: total != correct + incorrect + unanswered");
            return result;
        }
        
        if (draft.getPercentage() < 0 || draft.getPercentage() > 100) {
            result.setErrorCode("INVALID_PERCENTAGE");
            result.setAuditStatus("NOT_READY");
            result.addBlockingReason("Percentage out of range [0, 100]");
            return result;
        }

        result.setSuccess(true);
        result.setReady(true);
        result.setAuditStatus("SCORE_DRAFT_INTEGRITY_READY");
        return result;
    }
}
