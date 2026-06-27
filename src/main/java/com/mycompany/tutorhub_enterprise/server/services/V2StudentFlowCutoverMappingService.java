package com.mycompany.tutorhub_enterprise.server.services;

public class V2StudentFlowCutoverMappingService {

    public V2StudentFlowCutoverMappingResult inspectMapping() {
        V2StudentFlowCutoverMappingResult result = new V2StudentFlowCutoverMappingResult();
        
        if (!V2SubmitFeatureFlags.isStudentFlowCutoverMappingEnabled()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("FEATURE_DISABLED");
            result.setMappingStatus("NOT_READY");
            result.setBlockingReasons("tse.v2.studentFlowCutoverMapping.enabled is false");
            return result;
        }

        // Hardcoded checks based on inspection
        result.setCurrentSubmitAction("EXAM_SUBMIT");
        result.setLegacySubmitDetected(true); // From ClientHandler.java case "EXAM_SUBMIT"
        result.setV2ManualSubmitDetected(true); // From V2SubmitActions constants
        
        boolean defaultV2Enabled = V2SubmitFeatureFlags.isDefaultStudentSubmitV2Enabled();
        result.setDefaultV2Enabled(defaultV2Enabled);

        if (defaultV2Enabled) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("DEFAULT_V2_ALREADY_ENABLED");
            result.setMappingStatus("NOT_READY");
            result.setBlockingReasons("tse.v2.defaultStudentSubmitV2.enabled must be false during mapping phase");
            return result;
        }

        result.setSuccess(true);
        result.setReady(true);
        result.setErrorCode("");
        result.setMappingStatus("STUDENT_FLOW_CUTOVER_MAPPING_READY");
        return result;
    }
}
