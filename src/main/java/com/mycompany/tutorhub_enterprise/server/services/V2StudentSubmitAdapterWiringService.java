package com.mycompany.tutorhub_enterprise.server.services;

import java.util.ArrayList;
import java.util.List;

public class V2StudentSubmitAdapterWiringService {

    private final V2StudentFlowCutoverMappingService mappingService;
    private final V2StudentSubmitAdapterDryRunService adapterDryRunService;
    private final V2StudentFlowControlledCutoverGateService gateService;
    private final V2StudentSubmitUiWiringReadinessService uiWiringReadinessService;

    public V2StudentSubmitAdapterWiringService() {
        this(new V2StudentFlowCutoverMappingService(),
             new V2StudentSubmitAdapterDryRunService(),
             new V2StudentFlowControlledCutoverGateService(),
             new V2StudentSubmitUiWiringReadinessService());
    }

    public V2StudentSubmitAdapterWiringService(
            V2StudentFlowCutoverMappingService mappingService,
            V2StudentSubmitAdapterDryRunService adapterDryRunService,
            V2StudentFlowControlledCutoverGateService gateService,
            V2StudentSubmitUiWiringReadinessService uiWiringReadinessService) {
        this.mappingService = mappingService;
        this.adapterDryRunService = adapterDryRunService;
        this.gateService = gateService;
        this.uiWiringReadinessService = uiWiringReadinessService;
    }

    public V2StudentSubmitAdapterWiringResult resolveRoute(int userId, String attemptId, String payloadJson) {
        V2StudentSubmitAdapterWiringResult result = new V2StudentSubmitAdapterWiringResult();
        List<String> blockingReasons = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        result.setUserId(userId);
        result.setAttemptId(attemptId);
        result.setCurrentSubmitAction("EXAM_SUBMIT");
        
        boolean defaultV2Enabled = V2SubmitFeatureFlags.isDefaultStudentSubmitV2Enabled();
        result.setV2DefaultEnabled(defaultV2Enabled);

        if (!V2SubmitFeatureFlags.isStudentSubmitAdapterWiringEnabled()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("FEATURE_DISABLED");
            result.setResolvedRoute("NOT_READY");
            result.setWiringStatus("DISABLED");
            blockingReasons.add("Adapter wiring feature flag is disabled");
            result.setBlockingReasons(blockingReasons);
            return result;
        }

        if (!defaultV2Enabled) {
            result.setSuccess(true);
            result.setReady(true);
            result.setResolvedRoute("LEGACY_V1_STUDENT_SUBMIT");
            result.setWiringStatus("ROUTED_LEGACY");
            return result;
        }

        // V2 is default, check if V2 is ready
        V2StudentFlowCutoverMappingResult mappingResult = mappingService.inspectMapping();
        V2StudentSubmitAdapterDryRunResult adapterResult = adapterDryRunService.dryRunRoute(userId, attemptId, payloadJson);
        V2StudentFlowControlledCutoverGateResult gateResult = gateService.checkGate(userId, attemptId);

        V2StudentSubmitUiWiringReadinessResult readiness = uiWiringReadinessService.checkReadiness(mappingResult, adapterResult, gateResult);
        
        if (readiness.isSuccess() && readiness.isReady()) {
            result.setSuccess(true);
            result.setReady(true);
            result.setResolvedRoute("V2_MANUAL_CANDIDATE_PIPELINE");
            result.setWiringStatus("ROUTED_V2");
        } else {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("V2_NOT_READY");
            result.setResolvedRoute("NOT_READY");
            result.setWiringStatus("V2_GATES_FAILED");
            blockingReasons.add("V2 UI wiring readiness gates failed");
            if (readiness.getBlockingReasons() != null) {
                blockingReasons.add(readiness.getBlockingReasons());
            }
        }

        result.setBlockingReasons(blockingReasons);
        result.setWarnings(warnings);
        return result;
    }
}
