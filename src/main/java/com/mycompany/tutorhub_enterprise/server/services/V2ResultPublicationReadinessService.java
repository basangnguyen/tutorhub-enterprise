package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2OfficialResultDraftRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamAttemptStatusDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbe;
import com.mycompany.tutorhub_enterprise.server.dao.V2OfficialResultDraftDAO;

import java.util.Optional;

public class V2ResultPublicationReadinessService {
    private final V2SubmitFeatureFlags featureFlags;
    private final V2OfficialResultDraftDAO draftDAO;
    private final V2ExamAttemptStatusDAO attemptStatusDAO;
    private final V2ExamResultsReadOnlyProbe resultsProbe;

    public V2ResultPublicationReadinessService(
            V2SubmitFeatureFlags featureFlags,
            V2OfficialResultDraftDAO draftDAO,
            V2ExamAttemptStatusDAO attemptStatusDAO,
            V2ExamResultsReadOnlyProbe resultsProbe) {
        this.featureFlags = featureFlags;
        this.draftDAO = draftDAO;
        this.attemptStatusDAO = attemptStatusDAO;
        this.resultsProbe = resultsProbe;
    }

    public V2ResultPublicationReadinessResult checkReadiness(int userId, long submitRecordId) {
        V2ResultPublicationReadinessResult result = new V2ResultPublicationReadinessResult();
        result.setSubmitRecordId(submitRecordId);
        result.setUserId(userId);
        result.setSuccess(false);
        result.setReady(false);

        if (!featureFlags.isResultPublicationReadinessEnabled()) {
            result.setErrorCode("FEATURE_DISABLED");
            result.setPublicationReadinessStatus("NOT_READY");
            result.addBlockingReason("tse.v2.resultPublicationReadiness.enabled is false");
            return result;
        }

        Optional<V2OfficialResultDraftRecord> draftOpt = draftDAO.findBySubmitRecordId(submitRecordId);
        if (!draftOpt.isPresent()) {
            result.setErrorCode("OFFICIAL_RESULT_DRAFT_NOT_FOUND");
            result.setPublicationReadinessStatus("NOT_READY");
            result.addBlockingReason("Official result draft not found for submitRecordId: " + submitRecordId);
            return result;
        }

        V2OfficialResultDraftRecord draft = draftOpt.get();
        result.setOfficialResultDraftId(draft.getId());
        result.setScoreDraftId(draft.getScoreDraftId());
        result.setExamId(draft.getExamId());
        result.setPaperId(draft.getPaperId());
        result.setAttemptId(draft.getAttemptId());
        result.setPayloadHash(draft.getPayloadHash());
        result.setScoreDraftStatus(draft.getScoreDraftStatus());
        result.setScoreDraftAuditStatus(draft.getScoreDraftAuditStatus());
        result.setOfficialResultDraftStatus(draft.getOfficialResultDraftStatus());

        if (draft.getUserId() != userId) {
            result.setErrorCode("USER_MISMATCH");
            result.setPublicationReadinessStatus("NOT_READY");
            result.addBlockingReason("Official result draft belongs to a different user");
            return result;
        }

        if (!"OFFICIAL_RESULT_DRAFTED_PENDING_PUBLICATION".equals(draft.getOfficialResultDraftStatus())) {
            result.setErrorCode("INVALID_DRAFT_STATUS");
            result.setPublicationReadinessStatus("NOT_READY");
            result.addBlockingReason("Official result draft status is invalid: " + draft.getOfficialResultDraftStatus());
            return result;
        }

        try {
            Optional<String> statusOpt = attemptStatusDAO.findAttemptStatus(draft.getAttemptId());
            if (!statusOpt.isPresent()) {
                result.setErrorCode("ATTEMPT_NOT_FOUND");
                result.setPublicationReadinessStatus("NOT_READY");
                result.addBlockingReason("Exam attempt not found");
                return result;
            }
            
            String attemptStatus = statusOpt.get();
            result.setAttemptStatus(attemptStatus);

            if (!"SUBMITTED".equals(attemptStatus)) {
                result.setErrorCode("INVALID_ATTEMPT_STATUS");
                result.setPublicationReadinessStatus("NOT_READY");
                result.addBlockingReason("Attempt status is not SUBMITTED");
                return result;
            }

            if (resultsProbe.existsResultForAttempt(draft.getAttemptId())) {
                result.setErrorCode("EXAM_RESULTS_ALREADY_EXIST");
                result.setPublicationReadinessStatus("NOT_READY");
                result.addBlockingReason("Exam results have already been published for this attempt");
                return result;
            }

            result.setSuccess(true);
            result.setReady(true);
            result.setPublicationReadinessStatus("READY_FOR_EXAM_RESULTS_WRITE_DRAFT");
            return result;
        } catch (Exception e) {
            result.setErrorCode("DATABASE_ERROR");
            result.setPublicationReadinessStatus("NOT_READY");
            result.addBlockingReason("Database error: " + e.getMessage());
            return result;
        }
    }
}
