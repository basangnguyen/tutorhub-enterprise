package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.ExamAttempt;
import com.mycompany.tutorhub_enterprise.server.dao.ExamAttemptDAO;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class V2StudentFlowShadowCheckService {

    public V2StudentFlowShadowCheckService() {
    }

    protected ExamAttempt getAttemptDetails(String attemptId) {
        return ExamAttemptDAO.getAttemptDetails(attemptId);
    }

    public V2StudentFlowShadowCheckResult checkShadowReadiness(int userId, String attemptId) {
        V2StudentFlowShadowCheckResult result = new V2StudentFlowShadowCheckResult();
        result.setCheckedAt(Instant.now().toString());
        result.setUserId(userId);
        result.setAttemptId(attemptId);
        List<String> blockingReasons = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (!V2SubmitFeatureFlags.isStudentFlowShadowIntegrationEnabled()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_FEATURE_DISABLED");
            result.setShadowStatus("NOT_READY");
            blockingReasons.add("Student Flow Shadow Integration feature flag is disabled.");
            result.setBlockingReasons(blockingReasons);
            result.setWarnings(warnings);
            return result;
        }

        try {
            ExamAttempt attempt = getAttemptDetails(attemptId);
            if (attempt == null) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_ATTEMPT_NOT_FOUND");
                result.setShadowStatus("NOT_READY");
                blockingReasons.add("Attempt ID not found in database.");
                result.setBlockingReasons(blockingReasons);
                result.setWarnings(warnings);
                return result;
            }

            if (attempt.getUserId() != userId) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_USER_MISMATCH");
                result.setShadowStatus("NOT_READY");
                blockingReasons.add("User ID mismatch for attempt.");
                result.setBlockingReasons(blockingReasons);
                result.setWarnings(warnings);
                return result;
            }

            result.setExamId(attempt.getExamId());
            result.setPaperId(attempt.getPaperId());

            if (attempt.getExamId() <= 0 || attempt.getPaperId() <= 0) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_MISSING_MAPPING");
                result.setShadowStatus("NOT_READY");
                blockingReasons.add("Missing required Exam ID or Paper ID mapping.");
                result.setBlockingReasons(blockingReasons);
                result.setWarnings(warnings);
                return result;
            }

            String currentStatus = attempt.getStatus();
            if ("SUBMITTED".equalsIgnoreCase(currentStatus) || "COMPLETED".equalsIgnoreCase(currentStatus) || "GRADED".equalsIgnoreCase(currentStatus)) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_ATTEMPT_ALREADY_SUBMITTED");
                result.setShadowStatus("NOT_READY");
                blockingReasons.add("Attempt is already submitted or completed.");
                result.setBlockingReasons(blockingReasons);
                result.setWarnings(warnings);
                return result;
            }

            // At this point, the attempt is valid for shadow check (e.g., IN_PROGRESS, STARTED, etc.)
            result.setSuccess(true);
            result.setReady(true);
            result.setErrorCode("NONE");
            result.setShadowStatus("STUDENT_FLOW_V2_SHADOW_READY");
            result.setBlockingReasons(blockingReasons);
            result.setWarnings(warnings);
            return result;

        } catch (Exception e) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_SYSTEM_EXCEPTION");
            result.setShadowStatus("NOT_READY");
            blockingReasons.add("Internal system exception: " + e.getMessage());
            result.setBlockingReasons(blockingReasons);
            result.setWarnings(warnings);
            return result;
        }
    }
}
