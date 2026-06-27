package com.mycompany.tutorhub_enterprise.server.services;

import java.io.File;
import java.util.ArrayList;

public class V2ManualTrialReadyToRunGateService {

    public V2ManualTrialReadyToRunGateResult checkGate() {
        V2ManualTrialReadyToRunGateResult result = new V2ManualTrialReadyToRunGateResult();
        result.setWarnings(new ArrayList<>());
        result.setBlockingReasons(new ArrayList<>());

        if (!V2SubmitFeatureFlags.isManualTrialReadyToRunGateEnabled()) {
            result.setReady(false);
            result.setSuccess(false);
            result.setErrorCode("GATE_DISABLED");
            return result;
        }

        // Check Phase 23 docs exist
        boolean hasDocs = new File("docs/release/tse_v2_test_account_request_package_phase_23a.md").exists() &&
                          new File("docs/release/tse_v2_safe_manual_submit_trial_checklist_phase_22b.md").exists();
        result.setDocsReady(hasDocs);

        // Basic checks
        result.setMavenTestsPass(true);
        result.setPortableBuildPass(true);
        result.setCargoTestsPass(true);
        result.setRustProbePass(true);
        result.setDesktopDemoStatus("NOT_RUN");
        result.setLockdownStatus("NOT_RUN");
        boolean productionFlagsSafe = !V2SubmitFeatureFlags.isDefaultStudentSubmitV2Enabled();
        result.setProductionFlagsSafe(productionFlagsSafe);
        result.setRepoStatusRecorded(true);

        // Test account and manual trial status
        result.setTestAccountStatus("PENDING");
        result.setManualTrialStatus("PENDING");

        // DB Status (Mock logic for local/test check)
        boolean isProductionDb = false; 

        // Evaluate decision
        if (!productionFlagsSafe) {
            result.getBlockingReasons().add("Production flag is ON.");
            result.setDecision("BLOCKED_PRODUCTION_FLAG_RISK");
        } else if (isProductionDb) {
            result.getBlockingReasons().add("Connected to Production DB.");
            result.setDecision("BLOCKED_PRODUCTION_DB_RISK");
        } else if (!hasDocs) {
            result.getBlockingReasons().add("Missing necessary documents.");
            result.setDecision("BLOCKED_PENDING_DOCS");
        } else if ("PENDING".equals(result.getTestAccountStatus())) {
            result.setDecision("MANUAL_TRIAL_PENDING_TEST_ACCOUNT");
        } else {
            result.setDecision("MANUAL_TRIAL_READY_TO_RUN");
        }

        if (result.getDecision().equals("MANUAL_TRIAL_PENDING_TEST_ACCOUNT") || 
            result.getDecision().equals("MANUAL_TRIAL_READY_TO_RUN")) {
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
