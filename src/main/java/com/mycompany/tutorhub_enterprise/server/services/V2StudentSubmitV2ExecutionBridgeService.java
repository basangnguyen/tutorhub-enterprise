package com.mycompany.tutorhub_enterprise.server.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class V2StudentSubmitV2ExecutionBridgeService {

    private final V2ManualCandidateActualSubmitPreflightService preflightService;
    private final V2ManualCandidateSubmitRecordMaterializationService materializationService;
    private final V2ManualCandidateSubmitStatusExecutionService statusService;
    private final V2ManualCandidateScoreOfficialDraftExecutionService scoreService;
    private final V2ManualCandidateExamResultsPublicationService publicationService;
    private final V2ManualCandidateFinalStatusExecutionService finalStatusService;
    private final V2ManualCandidateResultHandoffVerificationService handoffService;

    public V2StudentSubmitV2ExecutionBridgeService() {
        this(new V2ManualCandidateActualSubmitPreflightService(),
             new V2ManualCandidateSubmitRecordMaterializationService(),
             new V2ManualCandidateSubmitStatusExecutionService(),
             new V2ManualCandidateScoreOfficialDraftExecutionService(),
             new V2ManualCandidateExamResultsPublicationService(),
             new V2ManualCandidateFinalStatusExecutionService(),
             new V2ManualCandidateResultHandoffVerificationService());
    }

    public V2StudentSubmitV2ExecutionBridgeService(
            V2ManualCandidateActualSubmitPreflightService preflightService,
            V2ManualCandidateSubmitRecordMaterializationService materializationService,
            V2ManualCandidateSubmitStatusExecutionService statusService,
            V2ManualCandidateScoreOfficialDraftExecutionService scoreService,
            V2ManualCandidateExamResultsPublicationService publicationService,
            V2ManualCandidateFinalStatusExecutionService finalStatusService,
            V2ManualCandidateResultHandoffVerificationService handoffService) {
        this.preflightService = preflightService;
        this.materializationService = materializationService;
        this.statusService = statusService;
        this.scoreService = scoreService;
        this.publicationService = publicationService;
        this.finalStatusService = finalStatusService;
        this.handoffService = handoffService;
    }

    public V2StudentSubmitV2ExecutionBridgeResult executeBridge(int userId, String attemptId, String payloadJson) {
        V2StudentSubmitV2ExecutionBridgeResult result = new V2StudentSubmitV2ExecutionBridgeResult();
        List<String> blockingReasons = new ArrayList<>();
        result.setUserId(userId);
        result.setAttemptId(attemptId);
        result.setExecutedAt(Instant.now());

        if (!V2SubmitFeatureFlags.isStudentSubmitV2ExecutionBridgeEnabled()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("FEATURE_DISABLED");
            result.setBridgeStatus("DISABLED");
            blockingReasons.add("V2StudentSubmitV2ExecutionBridge flag is disabled");
            result.setBlockingReasons(blockingReasons);
            return result;
        }

        if (!V2SubmitFeatureFlags.isDefaultStudentSubmitV2Enabled()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("V2_DEFAULT_DISABLED");
            result.setBridgeStatus("DISABLED");
            blockingReasons.add("Default V2 is disabled");
            result.setBlockingReasons(blockingReasons);
            return result;
        }

        int stepCount = 0;

        // 1. Preflight
        V2ManualCandidateActualSubmitPreflightResult preflightResult = preflightService.checkPreflight(userId, attemptId, payloadJson);
        if (!preflightResult.isSuccess()) {
            return buildFailure(result, "PREFLIGHT_FAILED", "Preflight failed", preflightResult.getErrorCode(), stepCount);
        }
        stepCount++;

        // 2. Materialize
        V2ManualCandidateSubmitRecordMaterializationResult matResult = materializationService.materializeSubmitRecord(userId, attemptId, payloadJson);
        if (!matResult.isSuccess()) {
            return buildFailure(result, "MATERIALIZATION_FAILED", "Materialization failed", matResult.getErrorCode(), stepCount);
        }
        result.setSubmitRecordId(matResult.getSubmitRecordId());
        stepCount++;

        // 3. Status
        V2ManualCandidateSubmitStatusExecutionResult statusResult = statusService.executeSubmitStatus(userId, result.getSubmitRecordId());
        if (!statusResult.isSuccess()) {
            return buildFailure(result, "STATUS_EXECUTION_FAILED", "Status execution failed", statusResult.getErrorCode(), stepCount);
        }
        stepCount++;

        // 4. Score/Official draft
        V2ManualCandidateScoreOfficialDraftExecutionResult scoreResult = scoreService.executeDrafts(userId, result.getSubmitRecordId());
        if (!scoreResult.isSuccess()) {
            return buildFailure(result, "SCORE_EXECUTION_FAILED", "Score execution failed", scoreResult.getErrorCode(), stepCount);
        }
        stepCount++;

        // 5. Exam Results
        V2ManualCandidateExamResultsPublicationResult pubResult = publicationService.publishManualResult(userId, result.getSubmitRecordId());
        if (!pubResult.isSuccess()) {
            return buildFailure(result, "PUBLICATION_FAILED", "Publication failed", pubResult.getErrorCode(), stepCount);
        }
        result.setExamResultId(pubResult.getExamResultId());
        stepCount++;

        // 6. Final Status
        V2ManualCandidateFinalStatusExecutionResult finalResult = finalStatusService.executeFinalStatus(userId, attemptId);
        if (!finalResult.isSuccess()) {
            return buildFailure(result, "FINAL_STATUS_FAILED", "Final status execution failed", finalResult.getErrorCode(), stepCount);
        }
        result.setFinalStatus("COMPLETED");
        stepCount++;

        // 7. Verify
        V2ManualCandidateResultHandoffVerificationResult verifyResult = handoffService.verifyHandoff(userId, attemptId);
        if (!verifyResult.isSuccess()) {
            return buildFailure(result, "VERIFICATION_FAILED", "Handoff verification failed", verifyResult.getErrorCode(), stepCount);
        }
        stepCount++;

        result.setSuccess(true);
        result.setReady(true);
        result.setExecuted(true);
        result.setBridgeStatus("SUCCESS");
        result.setExecutedStepCount(stepCount);
        return result;
    }

    private V2StudentSubmitV2ExecutionBridgeResult buildFailure(V2StudentSubmitV2ExecutionBridgeResult result, String errorCode, String bridgeStatus, String underlyingError, int stepCount) {
        result.setSuccess(false);
        result.setReady(false);
        result.setExecuted(false);
        result.setErrorCode(errorCode);
        result.setBridgeStatus(bridgeStatus);
        result.setExecutedStepCount(stepCount);
        List<String> blockingReasons = new ArrayList<>();
        blockingReasons.add(underlyingError);
        result.setBlockingReasons(blockingReasons);
        return result;
    }
}
