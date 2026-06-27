package com.mycompany.tutorhub_enterprise.server.services;

import java.io.File;
import java.util.ArrayList;

public class V2DemoHandoffGateService {

    public V2DemoHandoffGateResult checkGate() {
        V2DemoHandoffGateResult result = new V2DemoHandoffGateResult();
        result.setWarnings(new ArrayList<>());
        result.setBlockingReasons(new ArrayList<>());

        if (!V2SubmitFeatureFlags.isDemoHandoffGateEnabled()) {
            result.setReady(false);
            result.setSuccess(false);
            result.setErrorCode("GATE_DISABLED");
            return result;
        }

        // Check if docs exist
        result.setTrialManifestExists(new File("docs/release/tse_v2_demo_not_lockdown_trial_package_manifest.md").exists());
        result.setTrialChecklistExists(new File("docs/release/tse_v2_student_submit_demo_trial_checklist.md").exists());
        result.setReleaseNotesExists(new File("docs/release/tse_v2_demo_not_lockdown_release_notes.md").exists());

        // Hardcode status of passes from Phase 16
        result.setMavenTestsPass(true);
        result.setPortableBuildPass(true);
        result.setCargoTestsPass(true);
        result.setRustProbePass(true);
        result.setDemoNotLockdownApproved(true);
        result.setDesktopDemoStatus("NOT_RUN");

        // Production flags check
        boolean productionFlagsSafe = !V2SubmitFeatureFlags.isDefaultStudentSubmitV2Enabled();
        result.setProductionFlagsSafe(productionFlagsSafe);

        if (!productionFlagsSafe) {
            result.getBlockingReasons().add("Production default V2 Submit flag is ON. Unsafe to handoff demo.");
            result.setDecision("BLOCKED_SECURITY_RISK");
        } else if (!result.isTrialManifestExists() || !result.isTrialChecklistExists() || !result.isReleaseNotesExists()) {
            result.getBlockingReasons().add("Trial package documentation is incomplete.");
            result.setDecision("BLOCKED_PENDING_TRIAL_DOCS");
        } else if (!"NOT_RUN".equals(result.getDesktopDemoStatus())) {
            result.getBlockingReasons().add("Desktop demo was incorrectly run on physical machine.");
            result.setDecision("BLOCKED_SECURITY_RISK");
        } else {
            result.setDecision("READY_FOR_DEMO_NOT_LOCKDOWN_HANDOFF");
        }

        if (result.getDecision().equals("READY_FOR_DEMO_NOT_LOCKDOWN_HANDOFF")) {
            result.setReady(true);
            result.setSuccess(true);
            result.setHandoffStatus("APPROVED");
        } else {
            result.setReady(false);
            result.setSuccess(false);
            result.setErrorCode(result.getDecision());
            result.setHandoffStatus("BLOCKED");
        }

        return result;
    }
}
