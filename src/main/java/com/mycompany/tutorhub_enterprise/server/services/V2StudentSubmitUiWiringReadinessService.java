package com.mycompany.tutorhub_enterprise.server.services;

public class V2StudentSubmitUiWiringReadinessService {

    public V2StudentSubmitUiWiringReadinessResult checkReadiness(
            V2StudentFlowCutoverMappingResult mappingResult,
            V2StudentSubmitAdapterDryRunResult adapterResult,
            V2StudentFlowControlledCutoverGateResult cutoverGateResult) {

        V2StudentSubmitUiWiringReadinessResult result = new V2StudentSubmitUiWiringReadinessResult();

        if (!V2SubmitFeatureFlags.isStudentSubmitUiWiringReadinessEnabled()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("FEATURE_DISABLED");
            result.setWiringStatus("NOT_READY");
            result.setBlockingReasons("tse.v2.studentSubmitUiWiringReadiness.enabled is false");
            return result;
        }

        if (mappingResult == null || !mappingResult.isReady()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("MAPPING_NOT_READY");
            result.setWiringStatus("NOT_READY");
            result.setBlockingReasons("Phase 11A Mapping is not ready");
            return result;
        }

        if (adapterResult == null || !adapterResult.isReady()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ADAPTER_NOT_READY");
            result.setWiringStatus("NOT_READY");
            result.setBlockingReasons("Phase 11B Adapter Dry Run is not ready");
            return result;
        }

        if (cutoverGateResult == null || !cutoverGateResult.isReady()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("CONTROLLED_CUTOVER_GATE_NOT_READY");
            result.setWiringStatus("NOT_READY");
            result.setBlockingReasons("Phase 10U Controlled Cutover Gate is not ready");
            return result;
        }

        if (V2SubmitFeatureFlags.isDefaultStudentSubmitV2Enabled()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("DEFAULT_V2_ALREADY_ENABLED");
            result.setWiringStatus("NOT_READY");
            result.setBlockingReasons("tse.v2.defaultStudentSubmitV2.enabled must be false during UI wiring readiness check");
            return result;
        }

        result.setSuccess(true);
        result.setReady(true);
        result.setErrorCode("");
        result.setWiringStatus("READY_FOR_STUDENT_SUBMIT_UI_WIRING");
        return result;
    }
}
