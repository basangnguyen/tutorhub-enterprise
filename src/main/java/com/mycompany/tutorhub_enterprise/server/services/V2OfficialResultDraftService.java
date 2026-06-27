package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2OfficialResultDraftRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2OfficialResultDraftDAO;

import java.util.Optional;

public class V2OfficialResultDraftService {
    private final V2SubmitFeatureFlags featureFlags;
    private final V2ScoreDraftIntegrityAuditService auditService;
    private final V2OfficialResultDraftDAO officialResultDraftDAO;

    public V2OfficialResultDraftService(
            V2SubmitFeatureFlags featureFlags,
            V2ScoreDraftIntegrityAuditService auditService,
            V2OfficialResultDraftDAO officialResultDraftDAO) {
        this.featureFlags = featureFlags;
        this.auditService = auditService;
        this.officialResultDraftDAO = officialResultDraftDAO;
        this.officialResultDraftDAO.ensureTableExists();
    }

    public V2OfficialResultDraftResult createDraft(int userId, long submitRecordId) {
        V2OfficialResultDraftResult result = new V2OfficialResultDraftResult();
        result.setSubmitRecordId(submitRecordId);
        result.setUserId(userId);
        result.setSuccess(false);
        result.setReady(false);
        result.setIdempotent(false);

        if (!featureFlags.isOfficialResultDraftEnabled()) {
            result.setErrorCode("FEATURE_DISABLED");
            result.setOfficialResultDraftStatus("NOT_READY");
            result.addBlockingReason("tse.v2.officialResultDraft.enabled is false");
            return result;
        }

        V2ScoreDraftIntegrityAuditResult auditResult = auditService.audit(userId, submitRecordId);
        if (!auditResult.isReady() || !auditResult.isSuccess()) {
            result.setErrorCode("SCORE_DRAFT_AUDIT_NOT_READY");
            result.setOfficialResultDraftStatus("NOT_READY");
            result.addBlockingReason("Score draft audit failed or not ready");
            if (auditResult.getErrorCode() != null) {
                result.addWarning("Audit Error: " + auditResult.getErrorCode());
            }
            return result;
        }

        Optional<V2OfficialResultDraftRecord> existingOpt = officialResultDraftDAO.findBySubmitRecordId(submitRecordId);
        if (existingOpt.isPresent()) {
            V2OfficialResultDraftRecord existing = existingOpt.get();
            mapRecordToResult(existing, result);
            result.setSuccess(true);
            result.setReady(true);
            result.setIdempotent(true);
            return result;
        }

        V2OfficialResultDraftRecord newRecord = new V2OfficialResultDraftRecord();
        newRecord.setScoreDraftId(auditResult.getScoreDraftId());
        newRecord.setSubmitRecordId(submitRecordId);
        newRecord.setUserId(userId);
        newRecord.setExamId(auditResult.getExamId());
        newRecord.setPaperId(auditResult.getPaperId());
        newRecord.setAttemptId(auditResult.getAttemptId());
        newRecord.setPayloadHash(auditResult.getPayloadHash());
        newRecord.setTotalQuestions(auditResult.getTotalQuestions());
        newRecord.setAnsweredQuestions(auditResult.getAnsweredQuestions());
        newRecord.setUnansweredQuestions(auditResult.getUnansweredQuestions());
        newRecord.setCorrectCount(auditResult.getCorrectCount());
        newRecord.setIncorrectCount(auditResult.getIncorrectCount());
        newRecord.setRawScore(auditResult.getRawScore());
        newRecord.setMaxScore(auditResult.getMaxScore());
        newRecord.setPercentage(auditResult.getPercentage());
        newRecord.setScoreDraftStatus(auditResult.getScoreDraftStatus());
        newRecord.setScoreDraftAuditStatus(auditResult.getAuditStatus());
        newRecord.setOfficialResultDraftStatus("OFFICIAL_RESULT_DRAFTED_PENDING_PUBLICATION");

        boolean inserted = officialResultDraftDAO.insertDraft(newRecord);
        if (!inserted) {
            Optional<V2OfficialResultDraftRecord> retryOpt = officialResultDraftDAO.findBySubmitRecordId(submitRecordId);
            if (retryOpt.isPresent()) {
                mapRecordToResult(retryOpt.get(), result);
                result.setSuccess(true);
                result.setReady(true);
                result.setIdempotent(true);
                return result;
            } else {
                result.setErrorCode("DB_INSERT_FAILED");
                result.setOfficialResultDraftStatus("NOT_READY");
                result.addBlockingReason("Failed to insert official result draft into DB");
                return result;
            }
        }

        mapRecordToResult(newRecord, result);
        result.setSuccess(true);
        result.setReady(true);
        return result;
    }

    private void mapRecordToResult(V2OfficialResultDraftRecord record, V2OfficialResultDraftResult result) {
        result.setOfficialResultDraftId(record.getId());
        result.setScoreDraftId(record.getScoreDraftId());
        result.setExamId(record.getExamId());
        result.setPaperId(record.getPaperId());
        result.setAttemptId(record.getAttemptId());
        result.setPayloadHash(record.getPayloadHash());
        result.setTotalQuestions(record.getTotalQuestions());
        result.setAnsweredQuestions(record.getAnsweredQuestions());
        result.setUnansweredQuestions(record.getUnansweredQuestions());
        result.setCorrectCount(record.getCorrectCount());
        result.setIncorrectCount(record.getIncorrectCount());
        result.setRawScore(record.getRawScore());
        result.setMaxScore(record.getMaxScore());
        result.setPercentage(record.getPercentage());
        result.setScoreDraftStatus(record.getScoreDraftStatus());
        result.setScoreDraftAuditStatus(record.getScoreDraftAuditStatus());
        result.setOfficialResultDraftStatus(record.getOfficialResultDraftStatus());
    }
}
