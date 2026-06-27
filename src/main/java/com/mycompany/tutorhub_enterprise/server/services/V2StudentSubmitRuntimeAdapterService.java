package com.mycompany.tutorhub_enterprise.server.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class V2StudentSubmitRuntimeAdapterService {

    private final V2StudentSubmitAdapterWiringService wiringService;

    public V2StudentSubmitRuntimeAdapterService() {
        this(new V2StudentSubmitAdapterWiringService());
    }

    public V2StudentSubmitRuntimeAdapterService(V2StudentSubmitAdapterWiringService wiringService) {
        this.wiringService = wiringService;
    }

    public V2StudentSubmitRuntimeAdapterResult resolveRuntimeRoute(int userId, String attemptId, String payloadJson) {
        V2StudentSubmitRuntimeAdapterResult result = new V2StudentSubmitRuntimeAdapterResult();
        List<String> blockingReasons = new ArrayList<>();
        result.setUserId(userId);
        result.setAttemptId(attemptId);
        result.setCheckedAt(Instant.now());

        boolean v2Default = V2SubmitFeatureFlags.isDefaultStudentSubmitV2Enabled();
        result.setV2DefaultEnabled(v2Default);

        if (!V2SubmitFeatureFlags.isStudentSubmitRuntimeAdapterEnabled()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setRuntimeRoute("LEGACY_V1_STUDENT_SUBMIT");
            result.setRuntimeAdapterStatus("DISABLED_FALLBACK_TO_LEGACY");
            result.setErrorCode("FEATURE_DISABLED");
            blockingReasons.add("StudentSubmitRuntimeAdapter flag is disabled");
            result.setBlockingReasons(blockingReasons);
            return result;
        }

        if (!v2Default) {
            result.setSuccess(true);
            result.setReady(true);
            result.setRuntimeRoute("LEGACY_V1_STUDENT_SUBMIT");
            result.setRuntimeAdapterStatus("V2_DISABLED_BY_DEFAULT");
            return result;
        }

        V2StudentSubmitAdapterWiringResult wiringResult = wiringService.resolveRoute(userId, attemptId, payloadJson);
        if (wiringResult.isSuccess() && "V2_MANUAL_CANDIDATE_PIPELINE".equals(wiringResult.getResolvedRoute())) {
            result.setSuccess(true);
            result.setReady(true);
            result.setRuntimeRoute("V2_STUDENT_SUBMIT_EXECUTION_BRIDGE");
            result.setRuntimeAdapterStatus("V2_GATES_READY");
        } else {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("V2_GATES_FAILED");
            result.setRuntimeRoute("FALLBACK_TO_LEGACY_BEFORE_V2_WRITE");
            result.setRuntimeAdapterStatus("V2_GATES_FAILED_PRE_WRITE");
            if (wiringResult.getBlockingReasons() != null) {
                blockingReasons.addAll(wiringResult.getBlockingReasons());
            }
        }

        result.setBlockingReasons(blockingReasons);
        return result;
    }
}
