package com.mycompany.tutorhub_enterprise.server.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class V2StudentFlowCutoverReadinessService {

    private final V2StudentFlowShadowCheckService shadowCheckService;

    public V2StudentFlowCutoverReadinessService() {
        this.shadowCheckService = new V2StudentFlowShadowCheckService();
    }

    public V2StudentFlowCutoverReadinessService(V2StudentFlowShadowCheckService shadowCheckService) {
        this.shadowCheckService = shadowCheckService;
    }

    public V2StudentFlowCutoverReadinessResult checkCutoverReadiness(int userId, String attemptId) {
        V2StudentFlowCutoverReadinessResult result = new V2StudentFlowCutoverReadinessResult();
        result.setCheckedAt(Instant.now().toString());
        List<String> blockingReasons = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (!V2SubmitFeatureFlags.isStudentFlowCutoverReadinessEnabled()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_FEATURE_DISABLED");
            result.setCutoverReadinessStatus("NOT_READY");
            blockingReasons.add("Student Flow Cutover Readiness feature flag is disabled.");
            result.setBlockingReasons(blockingReasons);
            result.setWarnings(warnings);
            return result;
        }

        try {
            V2StudentFlowShadowCheckResult shadowResult = shadowCheckService.checkShadowReadiness(userId, attemptId);
            
            if (!shadowResult.isReady()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_SHADOW_CHECK_FAILED");
                result.setCutoverReadinessStatus("NOT_READY");
                blockingReasons.add("Shadow Check failed: " + shadowResult.getBlockingReasons().toString());
                result.setBlockingReasons(blockingReasons);
                result.setWarnings(warnings);
                return result;
            }

            // Ensure other necessary V2 feature flags are strictly configured (mostly should be disabled for safety in prod if not explicitly tested)
            // But for readiness, we at least verify the methods exist and don't throw exceptions.
            boolean finalHandoffEnabled = V2SubmitFeatureFlags.isFinalResultHandoffEnabled();
            boolean attemptStatusEnabled = V2SubmitFeatureFlags.isFinalAttemptStatusExecutionEnabled();

            if (!finalHandoffEnabled) {
                warnings.add("Final Result Handoff is not enabled globally.");
            }
            if (!attemptStatusEnabled) {
                warnings.add("Final Attempt Status Execution is not enabled globally.");
            }

            // Valid cutover candidate
            result.setSuccess(true);
            result.setReady(true);
            result.setErrorCode("NONE");
            result.setCutoverReadinessStatus("READY_FOR_MANUAL_V2_STUDENT_FLOW_CANDIDATE");
            result.setBlockingReasons(blockingReasons);
            result.setWarnings(warnings);
            return result;

        } catch (Exception e) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_SYSTEM_EXCEPTION");
            result.setCutoverReadinessStatus("NOT_READY");
            blockingReasons.add("Internal system exception: " + e.getMessage());
            result.setBlockingReasons(blockingReasons);
            result.setWarnings(warnings);
            return result;
        }
    }
}
