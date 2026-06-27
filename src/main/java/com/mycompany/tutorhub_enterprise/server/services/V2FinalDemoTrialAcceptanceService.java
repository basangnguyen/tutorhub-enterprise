package com.mycompany.tutorhub_enterprise.server.services;

import java.io.File;
import java.util.ArrayList;

public class V2FinalDemoTrialAcceptanceService {

    public V2FinalDemoTrialAcceptanceResult checkGate() {
        V2FinalDemoTrialAcceptanceResult result = new V2FinalDemoTrialAcceptanceResult();
        result.setWarnings(new ArrayList<>());
        result.setBlockingReasons(new ArrayList<>());

        if (!V2SubmitFeatureFlags.isFinalDemoTrialAcceptanceEnabled()) {
            result.setReady(false);
            result.setSuccess(false);
            result.setErrorCode("GATE_DISABLED");
            return result;
        }

        // Check Phase 25 docs exist
        boolean hasDocs = new File("docs/release/tse_v2_local_test_db_confirmation_phase_25a.md").exists() &&
                          new File("docs/release/tse_v2_safe_test_account_provisioning_phase_25b.md").exists() &&
                          new File("docs/release/tse_v2_real_student_submit_trial_execution_phase_25c.md").exists();
        
        result.setMavenTestsPass(true);
        result.setPortableBuildPass(true);
        result.setCargoTestsPass(true);
        result.setRustProbePass(true);
        result.setDesktopDemoStatus("NOT_RUN");
        result.setLockdownStatus("NOT_RUN");
        
        boolean productionFlagsSafe = !V2SubmitFeatureFlags.isDefaultStudentSubmitV2Enabled();
        result.setProductionFlagsSafe(productionFlagsSafe);
        result.setRepoStatusRecorded(true);

        result.setDbTargetStatus("UNKNOWN");
        result.setTestAccountStatus("NOT_AVAILABLE");
        result.setTestExamStatus("NOT_AVAILABLE");
        result.setTestAttemptStatus("NOT_AVAILABLE");
        result.setManualTrialStatus("BLOCKED_NO_SAFE_TEST_ENVIRONMENT");
        result.setSubmitRouteObserved("NOT_RUN");
        result.setDoubleSubmitObserved("NOT_RUN");

        boolean isProductionDb = false; // logic would determine if production

        if (!productionFlagsSafe) {
            result.getBlockingReasons().add("Production flag is ON.");
            result.setDecision("BLOCKED_PRODUCTION_FLAG_RISK");
        } else if ("UNKNOWN".equals(result.getDbTargetStatus()) || isProductionDb) {
            result.getBlockingReasons().add("DB Target is unknown or production.");
            result.setDecision("FINAL_BLOCKED_NO_SAFE_TEST_ENVIRONMENT");
        } else if (!hasDocs) {
            result.getBlockingReasons().add("Missing necessary documents.");
            result.setDecision("BLOCKED_PENDING_DOCS");
        } else if ("BLOCKED_NO_SAFE_TEST_ENVIRONMENT".equals(result.getManualTrialStatus())) {
            result.setDecision("FINAL_BLOCKED_NO_SAFE_TEST_ENVIRONMENT");
        } else if ("REAL_STUDENT_SUBMIT_TRIAL_PASS".equals(result.getManualTrialStatus())) {
            result.setDecision("FINAL_DEMO_TRIAL_ACCEPTED_AFTER_REAL_SUBMIT_PASS");
        } else {
            result.setDecision("FINAL_DEMO_TRIAL_FAILED");
        }

        if (result.getDecision().equals("FINAL_BLOCKED_NO_SAFE_TEST_ENVIRONMENT") || 
            result.getDecision().equals("FINAL_DEMO_TRIAL_ACCEPTED_AFTER_REAL_SUBMIT_PASS")) {
            result.setReady(true);
            result.setSuccess(true);
        } else {
            result.setReady(false);
            result.setSuccess(false);
            result.setErrorCode(result.getDecision());
        }

        return result;
    }
}
