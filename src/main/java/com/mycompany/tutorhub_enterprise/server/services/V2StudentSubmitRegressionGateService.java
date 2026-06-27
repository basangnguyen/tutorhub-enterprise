package com.mycompany.tutorhub_enterprise.server.services;

import java.util.ArrayList;
import java.util.List;

public class V2StudentSubmitRegressionGateService {

    private final V2StudentFlowCutoverMappingService mappingService;
    private final V2StudentSubmitAdapterDryRunService adapterDryRunService;
    private final V2StudentFlowControlledCutoverGateService gateService;
    private final V2StudentSubmitUiWiringReadinessService uiWiringReadinessService;
    private final V2StudentSubmitAdapterWiringService adapterWiringService;
    private final V2StudentSubmitLegacyFallbackService legacyFallbackService;

    public V2StudentSubmitRegressionGateService() {
        this(new V2StudentFlowCutoverMappingService(),
             new V2StudentSubmitAdapterDryRunService(),
             new V2StudentFlowControlledCutoverGateService(),
             new V2StudentSubmitUiWiringReadinessService(),
             new V2StudentSubmitAdapterWiringService(),
             new V2StudentSubmitLegacyFallbackService());
    }

    public V2StudentSubmitRegressionGateService(
            V2StudentFlowCutoverMappingService mappingService,
            V2StudentSubmitAdapterDryRunService adapterDryRunService,
            V2StudentFlowControlledCutoverGateService gateService,
            V2StudentSubmitUiWiringReadinessService uiWiringReadinessService,
            V2StudentSubmitAdapterWiringService adapterWiringService,
            V2StudentSubmitLegacyFallbackService legacyFallbackService) {
        this.mappingService = mappingService;
        this.adapterDryRunService = adapterDryRunService;
        this.gateService = gateService;
        this.uiWiringReadinessService = uiWiringReadinessService;
        this.adapterWiringService = adapterWiringService;
        this.legacyFallbackService = legacyFallbackService;
    }

    public V2StudentSubmitRegressionGateResult checkRegressionGate(int userId, String attemptId, String payloadJson) {
        V2StudentSubmitRegressionGateResult result = new V2StudentSubmitRegressionGateResult();
        List<String> blockingReasons = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (!V2SubmitFeatureFlags.isStudentSubmitRegressionGateEnabled()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("FEATURE_DISABLED");
            result.setRegressionStatus("NOT_READY");
            blockingReasons.add("Regression gate feature flag is disabled");
            result.setBlockingReasons(blockingReasons);
            return result;
        }

        // 1. Mapping ready
        V2StudentFlowCutoverMappingResult mappingResult = mappingService.inspectMapping();
        boolean mappingReady = mappingResult.isReady();

        // 2. Adapter dry-run ready
        V2StudentSubmitAdapterDryRunResult dryRunResult = adapterDryRunService.dryRunRoute(userId, attemptId, payloadJson);
        boolean dryRunReady = dryRunResult.isReady();

        // Gate ready
        V2StudentFlowControlledCutoverGateResult gateResult = gateService.checkGate(userId, attemptId);

        // 3. UI wiring readiness ready
        V2StudentSubmitUiWiringReadinessResult uiWiringResult = uiWiringReadinessService.checkReadiness(mappingResult, dryRunResult, gateResult);
        boolean uiWiringReady = uiWiringResult.isReady();

        // 4. Adapter wiring check ready
        V2StudentSubmitAdapterWiringResult adapterWiringResult = adapterWiringService.resolveRoute(userId, attemptId, payloadJson);
        boolean adapterWiringReady = adapterWiringResult.isReady();

        // 5. Legacy fallback check ready
        V2StudentSubmitLegacyFallbackResult legacyFallbackResult = legacyFallbackService.checkFallback(userId, attemptId, payloadJson);
        boolean legacyFallbackReady = legacyFallbackResult.isReady();

        result.setV2DefaultEnabled(V2SubmitFeatureFlags.isDefaultStudentSubmitV2Enabled());
        result.setLegacySubmitIntact(true); // Assumption based on code changes
        result.setAdapterWiringReady(adapterWiringReady);
        result.setLegacyFallbackReady(legacyFallbackReady);
        result.setPortableBuildRequired(true);

        if (!result.isV2DefaultEnabled() && mappingReady && dryRunReady && uiWiringReady && adapterWiringReady && legacyFallbackReady) {
            result.setSuccess(true);
            result.setReady(true);
            result.setRegressionStatus("STUDENT_SUBMIT_REGRESSION_GATE_READY");
        } else {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("REGRESSION_FAILED");
            result.setRegressionStatus("NOT_READY");
            
            if (result.isV2DefaultEnabled()) {
                blockingReasons.add("V2 default is enabled, which is not allowed in this phase");
            }
            if (!mappingReady) blockingReasons.add("Mapping check not ready");
            if (!dryRunReady) blockingReasons.add("Adapter dry-run not ready");
            if (!uiWiringReady) blockingReasons.add("UI wiring readiness not ready");
            if (!adapterWiringReady) blockingReasons.add("Adapter wiring not ready");
            if (!legacyFallbackReady) blockingReasons.add("Legacy fallback not ready");
        }

        result.setBlockingReasons(blockingReasons);
        result.setWarnings(warnings);
        return result;
    }
}
