package com.mycompany.tutorhub_enterprise.server.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class V2StudentSubmitE2EHarnessService {
    
    public V2StudentSubmitE2EHarnessResult prepareHarness(int userId, String attemptId, String payloadJson) {
        V2StudentSubmitE2EHarnessResult.Builder builder = new V2StudentSubmitE2EHarnessResult.Builder();
        
        if (!V2SubmitFeatureFlags.isStudentSubmitE2EHarnessEnabled()) {
            return builder
                    .success(false)
                    .ready(false)
                    .harnessStatus("NOT_READY")
                    .errorCode("HARNESS_DISABLED")
                    .addBlockingReason("StudentSubmit E2E Harness is disabled")
                    .build();
        }

        boolean defaultV2Enabled = V2SubmitFeatureFlags.isDefaultStudentSubmitV2Enabled();
        boolean adapterReady = V2SubmitFeatureFlags.isStudentSubmitRuntimeAdapterEnabled();
        boolean bridgeReady = V2SubmitFeatureFlags.isStudentSubmitV2ExecutionBridgeEnabled();
        boolean fallbackGuardReady = V2SubmitFeatureFlags.isStudentSubmitLegacyFallbackRuntimeGuardEnabled();
        
        builder.v2DefaultEnabledInTestOnly(defaultV2Enabled)
               .runtimeAdapterReady(adapterReady)
               .executionBridgeReady(bridgeReady)
               .fallbackGuardReady(fallbackGuardReady);

        boolean isPayloadValid = false;
        try {
            if (payloadJson != null && !payloadJson.trim().isEmpty()) {
                JsonObject parsed = new Gson().fromJson(payloadJson, JsonObject.class);
                if (parsed.has("attemptId") && parsed.has("paperId") && parsed.has("answers")) {
                    isPayloadValid = true;
                }
            }
        } catch (JsonSyntaxException e) {
            // Ignored, payload invalid
        }
        builder.payloadContractValid(isPayloadValid);

        if (!isPayloadValid) {
            builder.addWarning("Payload contract validation failed.");
        }

        if (!defaultV2Enabled) {
            builder.addWarning("defaultStudentSubmitV2 is false (Safe for production, but might route to legacy in test).");
        }

        if (adapterReady && bridgeReady && fallbackGuardReady) {
            builder.success(true)
                   .ready(true)
                   .harnessStatus("STUDENT_SUBMIT_E2E_HARNESS_READY");
        } else {
            builder.success(false)
                   .ready(false)
                   .harnessStatus("NOT_READY")
                   .errorCode("GATES_NOT_READY")
                   .addBlockingReason("One or more underlying V2 stages are not enabled.");
        }

        return builder.build();
    }
}
