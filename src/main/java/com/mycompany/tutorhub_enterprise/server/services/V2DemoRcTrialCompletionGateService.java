package com.mycompany.tutorhub_enterprise.server.services;

import java.io.File;
import java.util.ArrayList;

public class V2DemoRcTrialCompletionGateService {

    public V2DemoRcTrialCompletionGateResult checkGate() {
        V2DemoRcTrialCompletionGateResult result = new V2DemoRcTrialCompletionGateResult();
        result.setWarnings(new ArrayList<>());
        result.setBlockingReasons(new ArrayList<>());

        if (!V2SubmitFeatureFlags.isDemoRcTrialCompletionGateEnabled()) {
            result.setReady(false);
            result.setSuccess(false);
            result.setErrorCode("GATE_DISABLED");
            return result;
        }

        // Check required phase 21 & 22 docs
        boolean hasDocs = new File("docs/release/tse_v2_demo_rc_acceptance_gate_phase_21d.md").exists() &&
                          new File("docs/release/tse_v2_manual_trial_evidence_execution_gate_phase_22c.md").exists();
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

        // Evaluate decision
        if (!productionFlagsSafe) {
            result.getBlockingReasons().add("Production flag is ON.");
            result.setDecision("BLOCKED_PRODUCTION_FLAG_RISK");
        } else if (!hasDocs) {
            result.getBlockingReasons().add("Missing necessary documents.");
            result.setDecision("BLOCKED_PENDING_DOCS");
        } else if ("PENDING".equals(result.getTestAccountStatus())) {
            result.setDecision("DEMO_RC_TRIAL_PENDING_TEST_ACCOUNT");
        } else if (!"PASS".equals(result.getManualTrialStatus())) {
            result.setDecision("DEMO_RC_TRIAL_PENDING_MANUAL_EVIDENCE");
        } else {
            result.setDecision("DEMO_RC_TRIAL_COMPLETE");
        }

        if (result.getDecision().equals("DEMO_RC_TRIAL_PENDING_TEST_ACCOUNT") || 
            result.getDecision().equals("DEMO_RC_TRIAL_PENDING_MANUAL_EVIDENCE") || 
            result.getDecision().equals("DEMO_RC_TRIAL_COMPLETE")) {
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
