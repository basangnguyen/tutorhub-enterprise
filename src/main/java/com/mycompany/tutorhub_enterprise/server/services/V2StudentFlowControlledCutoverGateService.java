package com.mycompany.tutorhub_enterprise.server.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class V2StudentFlowControlledCutoverGateService {

    private final V2ManualCandidateResultHandoffVerificationService handoffVerificationService;

    public V2StudentFlowControlledCutoverGateService() {
        this(new V2ManualCandidateResultHandoffVerificationService());
    }

    public V2StudentFlowControlledCutoverGateService(
            V2ManualCandidateResultHandoffVerificationService handoffVerificationService) {
        this.handoffVerificationService = handoffVerificationService;
    }

    public V2StudentFlowControlledCutoverGateResult checkGate(int userId, String attemptId) {
        List<String> blockingReasons = new ArrayList<>();
        List<String> pendingChecks = new ArrayList<>();

        if (!V2SubmitFeatureFlags.isStudentFlowControlledCutoverGateEnabled()) {
            blockingReasons.add("Feature flag tse.v2.studentFlowControlledCutoverGate.enabled is false");
            return new V2StudentFlowControlledCutoverGateResult.Builder()
                    .ready(false)
                    .errorCode("ERROR_FEATURE_DISABLED")
                    .blockingReasons(blockingReasons)
                    .build();
        }

        // Check if manual candidate phases 10M -> 10T are enabled
        if (!V2SubmitFeatureFlags.isManualCandidateActualSubmitPreflightEnabled() ||
            !V2SubmitFeatureFlags.isManualCandidateSubmitRecordMaterializationEnabled() ||
            !V2SubmitFeatureFlags.isManualCandidatePublishFinalStatusOrchestratorGateEnabled() ||
            !V2SubmitFeatureFlags.isManualCandidateSubmitStatusExecutionEnabled() ||
            !V2SubmitFeatureFlags.isManualCandidateScoreOfficialDraftExecutionEnabled() ||
            !V2SubmitFeatureFlags.isManualCandidateExamResultsPublicationEnabled() ||
            !V2SubmitFeatureFlags.isManualCandidateFinalStatusExecutionEnabled() ||
            !V2SubmitFeatureFlags.isManualCandidateResultHandoffVerificationEnabled()) {
            blockingReasons.add("One or more manual candidate phases (10M-10T) feature flags are disabled");
        }

        // Student flow shadow check ready
        if (!V2SubmitFeatureFlags.isStudentFlowShadowIntegrationEnabled()) {
            blockingReasons.add("Student flow shadow integration is not enabled");
        }

        // Cutover readiness ready
        if (!V2SubmitFeatureFlags.isStudentFlowCutoverReadinessEnabled()) {
            blockingReasons.add("Student flow cutover readiness is not enabled");
        }

        // No default flow enabled yet
        boolean defaultFlowEnabled = V2SubmitFeatureFlags.isClientServerNoGradingSubmitEnabled() ||
                                     V2SubmitFeatureFlags.isRealSubmitPreflightEnabled();
        if (defaultFlowEnabled) {
            blockingReasons.add("Default student flow is already enabled (should be off for this gate)");
        }

        // Result handoff verified
        V2ManualCandidateResultHandoffVerificationResult handoffResult = handoffVerificationService.verifyHandoff(userId, attemptId);
        if (!handoffResult.isSuccess()) {
            blockingReasons.add("Result handoff verification failed: " + handoffResult.getErrorCode());
        }

        if (!blockingReasons.isEmpty()) {
            return new V2StudentFlowControlledCutoverGateResult.Builder()
                    .ready(false)
                    .defaultFlowEnabled(defaultFlowEnabled)
                    .errorCode("ERROR_GATE_CHECK_FAILED")
                    .status("NOT_READY")
                    .blockingReasons(blockingReasons)
                    .pendingChecks(pendingChecks)
                    .checkedAt(Instant.now())
                    .build();
        }

        return new V2StudentFlowControlledCutoverGateResult.Builder()
                .ready(true)
                .defaultFlowEnabled(false)
                .status("READY_FOR_CONTROLLED_STUDENT_FLOW_WIRING")
                .checkedAt(Instant.now())
                .build();
    }
}
