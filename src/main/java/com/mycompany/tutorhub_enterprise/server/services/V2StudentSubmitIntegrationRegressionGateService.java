package com.mycompany.tutorhub_enterprise.server.services;

public class V2StudentSubmitIntegrationRegressionGateService {
    public V2StudentSubmitIntegrationRegressionGateResult checkGate() {
        V2StudentSubmitIntegrationRegressionGateResult result = new V2StudentSubmitIntegrationRegressionGateResult();
        
        if (!V2SubmitFeatureFlags.isStudentSubmitIntegrationRegressionGateEnabled()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setRegressionStatus("DISABLED");
            return result;
        }

        boolean defaultV2Enabled = V2SubmitFeatureFlags.isDefaultStudentSubmitV2Enabled();
        result.setDefaultV2Enabled(defaultV2Enabled);

        if (defaultV2Enabled) {
            result.setSuccess(false);
            result.setReady(false);
            result.setRegressionStatus("NOT_READY_V2_IS_DEFAULT");
            return result;
        }

        result.setSuccess(true);
        result.setReady(true);
        result.setRegressionStatus("STUDENT_SUBMIT_INTEGRATION_REGRESSION_READY");
        return result;
    }
}
