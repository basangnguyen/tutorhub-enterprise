package com.mycompany.tutorhub_enterprise.server.services;

import java.util.ArrayList;
import java.util.List;

public class V2StudentSubmitLegacyFallbackService {

    private final V2StudentSubmitAdapterWiringService adapterWiringService;

    public V2StudentSubmitLegacyFallbackService() {
        this(new V2StudentSubmitAdapterWiringService());
    }

    public V2StudentSubmitLegacyFallbackService(V2StudentSubmitAdapterWiringService adapterWiringService) {
        this.adapterWiringService = adapterWiringService;
    }

    public V2StudentSubmitLegacyFallbackResult checkFallback(int userId, String attemptId, String payloadJson) {
        V2StudentSubmitLegacyFallbackResult result = new V2StudentSubmitLegacyFallbackResult();
        List<String> blockingReasons = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (!V2SubmitFeatureFlags.isStudentSubmitLegacyFallbackEnabled()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("FEATURE_DISABLED");
            result.setFallbackAvailable(false);
            blockingReasons.add("Legacy fallback feature flag is disabled");
            result.setBlockingReasons(blockingReasons);
            return result;
        }

        V2StudentSubmitAdapterWiringResult wiringResult = adapterWiringService.resolveRoute(userId, attemptId, payloadJson);
        
        result.setFallbackAvailable(true);
        result.setFallbackTarget("LEGACY_V1_STUDENT_SUBMIT");
        
        if ("LEGACY_V1_STUDENT_SUBMIT".equals(wiringResult.getResolvedRoute())) {
            result.setWouldUseFallback(false);
            result.setFallbackReason("V2_DISABLED_BY_DEFAULT_NO_FALLBACK_NEEDED");
            result.setSuccess(true);
            result.setReady(true);
        } else if ("NOT_READY".equals(wiringResult.getResolvedRoute())) {
            result.setWouldUseFallback(true);
            result.setFallbackReason("V2_GATES_FAILED_FALLBACK_TO_V1");
            result.setSuccess(true);
            result.setReady(true);
            warnings.add("V2 is default but gates failed. Fallback to V1 would be executed.");
        } else if ("V2_MANUAL_CANDIDATE_PIPELINE".equals(wiringResult.getResolvedRoute())) {
            result.setWouldUseFallback(false);
            result.setFallbackReason("V2_READY_NO_FALLBACK_NEEDED");
            result.setSuccess(true);
            result.setReady(true);
        } else {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("UNKNOWN_ROUTE");
            result.setFallbackAvailable(false);
            result.setWouldUseFallback(false);
            blockingReasons.add("Unknown resolved route from adapter wiring");
        }

        result.setBlockingReasons(blockingReasons);
        result.setWarnings(warnings);
        return result;
    }
}
