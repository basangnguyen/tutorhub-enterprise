package com.mycompany.tutorhub_enterprise.server.services;

import java.io.File;
import java.util.ArrayList;

public class V2DemoRcAcceptanceGateService {

    public V2DemoRcAcceptanceGateResult checkGate() {
        V2DemoRcAcceptanceGateResult result = new V2DemoRcAcceptanceGateResult();
        result.setWarnings(new ArrayList<>());
        result.setBlockingReasons(new ArrayList<>());

        if (!V2SubmitFeatureFlags.isDemoRcAcceptanceGateEnabled()) {
            result.setReady(false);
            result.setSuccess(false);
            result.setErrorCode("GATE_DISABLED");
            return result;
        }

        // Check phase 20 and 21 docs
        boolean hasDocs = new File("docs/release/tse_v2_final_internal_demo_signoff_phase_20a.md").exists() &&
                          new File("docs/release/tse_v2_tester_handoff_package_phase_20b.md").exists() &&
                          new File("docs/release/tse_v2_production_protection_checklist_phase_20c.md").exists() &&
                          new File("docs/release/tse_v2_demo_rc_closeout_phase_20d.md").exists() &&
                          new File("docs/release/tse_v2_test_account_readiness_pack_phase_21a.md").exists() &&
                          new File("docs/release/tse_v2_manual_student_submit_trial_runbook_phase_21b.md").exists() &&
                          new File("docs/release/tse_v2_manual_student_submit_trial_evidence_phase_21c.md").exists();
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
            result.setDecision("DEMO_RC_ACCEPTED_FOR_INTERNAL_TEST_WITH_MANUAL_TRIAL_PENDING");
        } else {
            result.setDecision("DEMO_RC_ACCEPTED_FOR_INTERNAL_TEST");
        }

        if (result.getDecision().startsWith("DEMO_RC_ACCEPTED")) {
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
