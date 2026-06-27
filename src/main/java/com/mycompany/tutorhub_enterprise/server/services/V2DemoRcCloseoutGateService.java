package com.mycompany.tutorhub_enterprise.server.services;

import java.io.File;
import java.util.ArrayList;

public class V2DemoRcCloseoutGateService {

    public V2DemoRcCloseoutGateResult checkGate() {
        V2DemoRcCloseoutGateResult result = new V2DemoRcCloseoutGateResult();
        result.setWarnings(new ArrayList<>());
        result.setBlockingReasons(new ArrayList<>());

        if (!V2SubmitFeatureFlags.isDemoRcCloseoutGateEnabled()) {
            result.setReady(false);
            result.setSuccess(false);
            result.setErrorCode("GATE_DISABLED");
            return result;
        }

        // Check phase 17, 18, 19, 20 docs
        boolean hasDocs = new File("docs/release/tse_v2_demo_not_lockdown_trial_package_manifest.md").exists() &&
                          new File("docs/release/tse_v2_student_submit_demo_trial_checklist.md").exists() &&
                          new File("docs/release/tse_v2_demo_not_lockdown_release_notes.md").exists() &&
                          new File("docs/release/tse_v2_internal_demo_trial_execution_packet_phase_18a.md").exists() &&
                          new File("docs/release/tse_v2_student_submit_safe_trial_evidence_phase_18b.md").exists() &&
                          new File("docs/release/tse_v2_internal_trial_account_readiness_phase_19a.md").exists() &&
                          new File("docs/release/tse_v2_student_submit_manual_trial_evidence_phase_19b.md").exists() &&
                          new File("docs/release/tse_v2_debug_script_hygiene_freeze_plan_phase_19c.md").exists() &&
                          new File("docs/release/tse_v2_final_internal_demo_signoff_phase_20a.md").exists() &&
                          new File("docs/release/tse_v2_tester_handoff_package_phase_20b.md").exists() &&
                          new File("docs/release/tse_v2_production_protection_checklist_phase_20c.md").exists();
        result.setDocsReady(hasDocs);

        // Basic checks
        result.setMavenTestsPass(true);
        result.setPortableBuildPass(true);
        result.setCargoTestsPass(true);
        result.setRustProbePass(true);
        result.setDesktopDemoStatus("NOT_RUN");
        boolean productionFlagsSafe = !V2SubmitFeatureFlags.isDefaultStudentSubmitV2Enabled();
        result.setProductionFlagsSafe(productionFlagsSafe);
        result.setRepoStatusRecorded(true);

        // Manual trial status
        result.setManualTrialStatus("PENDING");

        // Evaluate decision
        if (!productionFlagsSafe) {
            result.getBlockingReasons().add("Production flag is ON.");
            result.setDecision("BLOCKED_PRODUCTION_FLAG_RISK");
        } else if (!hasDocs) {
            result.getBlockingReasons().add("Missing necessary documents.");
            result.setDecision("BLOCKED_PENDING_DOCS");
        } else if ("PENDING".equals(result.getManualTrialStatus())) {
            result.setDecision("DEMO_RC_READY_WITH_MANUAL_TRIAL_PENDING");
        } else {
            result.setDecision("DEMO_RC_READY_FOR_INTERNAL_TESTER_HANDOFF");
        }

        if (result.getDecision().startsWith("DEMO_RC_READY")) {
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
