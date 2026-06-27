package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.ExamAttempt;
import com.mycompany.tutorhub_enterprise.server.dao.ExamAttemptDAO;

import java.time.Instant;

public class V2ManualCandidateSubmitCheckService {

    private final V2StudentFlowShadowCheckService shadowCheckService;
    private final V2StudentFlowCutoverReadinessService cutoverReadinessService;

    public V2ManualCandidateSubmitCheckService() {
        this.shadowCheckService = new V2StudentFlowShadowCheckService();
        this.cutoverReadinessService = new V2StudentFlowCutoverReadinessService();
    }

    protected ExamAttempt getAttemptDetails(String attemptId) {
        return ExamAttemptDAO.getAttemptDetails(attemptId);
    }

    protected V2StudentFlowShadowCheckResult checkShadowReadiness(int userId, String attemptId) {
        return shadowCheckService.checkShadowReadiness(userId, attemptId);
    }

    protected V2StudentFlowCutoverReadinessResult checkCutoverReadiness(int userId, String attemptId) {
        return cutoverReadinessService.checkCutoverReadiness(userId, attemptId);
    }

    public V2ManualCandidateSubmitCheckResult checkCandidateSubmit(int userId, String attemptId) {
        V2ManualCandidateSubmitCheckResult result = new V2ManualCandidateSubmitCheckResult();
        result.setCheckedAt(Instant.now().toString());
        result.setAttemptId(attemptId);
        result.setUserId(userId);

        if (!V2SubmitFeatureFlags.isManualCandidateSubmitEnabled()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_FEATURE_DISABLED");
            result.setCandidateStatus("NOT_READY");
            result.addBlockingReason("Manual Candidate Submit flag is disabled.");
            return result;
        }

        V2StudentFlowShadowCheckResult shadowResult = checkShadowReadiness(userId, attemptId);
        if (!shadowResult.isReady()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_SHADOW_CHECK_FAILED");
            result.setCandidateStatus("NOT_READY");
            result.addBlockingReason("Shadow check is not ready: " + shadowResult.getErrorCode());
            return result;
        }

        V2StudentFlowCutoverReadinessResult cutoverResult = checkCutoverReadiness(userId, attemptId);
        if (!cutoverResult.isReady()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_CUTOVER_READINESS_FAILED");
            result.setCandidateStatus("NOT_READY");
            result.addBlockingReason("Cutover readiness is not ready: " + cutoverResult.getErrorCode());
            return result;
        }

        try {
            ExamAttempt attempt = getAttemptDetails(attemptId);
            if (attempt == null) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_ATTEMPT_NOT_FOUND");
                result.setCandidateStatus("NOT_READY");
                result.addBlockingReason("Attempt not found in database.");
                return result;
            }

            if (attempt.getUserId() != userId) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_USER_MISMATCH");
                result.setCandidateStatus("NOT_READY");
                result.addBlockingReason("Attempt user ID does not match current user.");
                return result;
            }

            result.setExamId(attempt.getExamId());
            result.setPaperId(attempt.getPaperId());

            if (attempt.getExamId() <= 0 || attempt.getPaperId() <= 0) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_MISSING_MAPPING");
                result.setCandidateStatus("NOT_READY");
                result.addBlockingReason("Attempt missing examId or paperId mapping.");
                return result;
            }

            String currentStatus = attempt.getStatus() != null ? attempt.getStatus().toUpperCase() : "UNKNOWN";
            
            if (currentStatus.equals("SUBMITTED") || currentStatus.equals("COMPLETED") || currentStatus.equals("GRADED")) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_ATTEMPT_ALREADY_FINISHED");
                result.setCandidateStatus("NOT_READY");
                result.addBlockingReason("Attempt is already in a finished state: " + currentStatus);
                return result;
            }

            result.setSuccess(true);
            result.setReady(true);
            result.setCandidateStatus("MANUAL_CANDIDATE_SUBMIT_READY");

        } catch (Exception e) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_INTERNAL");
            result.setCandidateStatus("NOT_READY");
            result.addBlockingReason("Internal server error: " + e.getMessage());
        }

        return result;
    }
}
