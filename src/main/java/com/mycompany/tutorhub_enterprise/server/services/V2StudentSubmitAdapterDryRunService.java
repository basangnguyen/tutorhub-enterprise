package com.mycompany.tutorhub_enterprise.server.services;

public class V2StudentSubmitAdapterDryRunService {

    public V2StudentSubmitAdapterDryRunResult dryRunRoute(int userId, String attemptId, String payloadJson) {
        V2StudentSubmitAdapterDryRunResult result = new V2StudentSubmitAdapterDryRunResult();
        result.setUserId(userId);
        result.setAttemptId(attemptId);
        
        if (!V2SubmitFeatureFlags.isStudentSubmitAdapterDryRunEnabled()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("FEATURE_DISABLED");
            result.setAdapterStatus("NOT_READY");
            result.setBlockingReasons("tse.v2.studentSubmitAdapterDryRun.enabled is false");
            result.setPlannedRoute("NOT_READY");
            return result;
        }

        // Validate payload contract (dry-run only, does not store it)
        V2AnswerPayloadContractValidator validator = new V2AnswerPayloadContractValidator();
        V2AnswerPayloadContractValidationResult validationResult = validator.validate(payloadJson);
        
        if (!validationResult.isValid()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("INVALID_PAYLOAD");
            result.setAdapterStatus("NOT_READY");
            result.setBlockingReasons("Payload failed canonical validation: " + String.join(", ", validationResult.getBlockingReasons()));
            result.setPlannedRoute("NOT_READY");
            return result;
        }

        boolean defaultV2Enabled = V2SubmitFeatureFlags.isDefaultStudentSubmitV2Enabled();
        
        if (!defaultV2Enabled) {
            result.setPlannedRoute("LEGACY_V1_STUDENT_SUBMIT");
        } else {
            result.setPlannedRoute("V2_MANUAL_CANDIDATE_PIPELINE");
        }

        result.setSuccess(true);
        result.setReady(true);
        result.setErrorCode("");
        result.setAdapterStatus("STUDENT_SUBMIT_ADAPTER_DRY_RUN_READY");
        return result;
    }
}
