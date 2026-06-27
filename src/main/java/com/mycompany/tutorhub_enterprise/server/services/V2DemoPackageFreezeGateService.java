package com.mycompany.tutorhub_enterprise.server.services;

import java.io.File;
import java.util.ArrayList;

public class V2DemoPackageFreezeGateService {

    public V2DemoPackageFreezeGateResult checkGate() {
        V2DemoPackageFreezeGateResult result = new V2DemoPackageFreezeGateResult();
        result.setWarnings(new ArrayList<>());
        result.setBlockingReasons(new ArrayList<>());

        if (!V2SubmitFeatureFlags.isDemoPackageFreezeGateEnabled()) {
            result.setReady(false);
            result.setSuccess(false);
            result.setErrorCode("GATE_DISABLED");
            return result;
        }

        // Docs phase 17
        result.setTrialManifestExists(new File("docs/release/tse_v2_demo_not_lockdown_trial_package_manifest.md").exists());
        result.setTrialChecklistExists(new File("docs/release/tse_v2_student_submit_demo_trial_checklist.md").exists());
        result.setReleaseNotesExists(new File("docs/release/tse_v2_demo_not_lockdown_release_notes.md").exists());
        
        // Docs phase 18
        result.setInternalDemoPacketExists(new File("docs/release/tse_v2_internal_demo_trial_execution_packet_phase_18a.md").exists());
        result.setStudentSubmitTrialEvidenceExists(new File("docs/release/tse_v2_student_submit_safe_trial_evidence_phase_18b.md").exists());

        // Check phase 18c
        V2DemoRegressionSecurityRecheckGateService recheckService = new V2DemoRegressionSecurityRecheckGateService();
        V2DemoRegressionSecurityRecheckGateResult recheckResult = recheckService.checkGate();
        // Assume pass if not blocked (it may be disabled in some tests, but for logic we consider true if it returns PASS)
        result.setRegressionSecurityRecheckPass("REGRESSION_SECURITY_RECHECK_PASS".equals(recheckResult.getDecision()));

        // Check phase 17 demo handoff
        V2DemoHandoffGateService handoffService = new V2DemoHandoffGateService();
        V2DemoHandoffGateResult handoffResult = handoffService.checkGate();
        result.setDemoHandoffGateReady("READY_FOR_DEMO_NOT_LOCKDOWN_HANDOFF".equals(handoffResult.getDecision()));

        result.setDesktopDemoStatus("NOT_RUN");
        boolean productionFlagsSafe = !V2SubmitFeatureFlags.isDefaultStudentSubmitV2Enabled();
        result.setProductionFlagsSafe(productionFlagsSafe);
        result.setRepoStatusDirty(true);

        // Evaluate decision
        if (!productionFlagsSafe) {
            result.getBlockingReasons().add("Production flag is ON.");
            result.setDecision("BLOCKED_SECURITY_RISK");
        } else if (!result.isRegressionSecurityRecheckPass()) {
            result.getBlockingReasons().add("Regression Security Recheck failed.");
            result.setDecision("BLOCKED_SECURITY_RISK");
        } else if (!result.isTrialManifestExists() || !result.isTrialChecklistExists() || !result.isReleaseNotesExists() ||
                   !result.isInternalDemoPacketExists() || !result.isStudentSubmitTrialEvidenceExists()) {
            result.getBlockingReasons().add("Missing necessary documents.");
            result.setDecision("BLOCKED_DOCS_MISSING");
        } else if (!result.isDemoHandoffGateReady()) {
            result.getBlockingReasons().add("Handoff gate is not ready.");
            result.setDecision("BLOCKED_PENDING_EVIDENCE");
        } else {
            result.setDecision("READY_FOR_INTERNAL_DEMO_TRIAL");
        }

        if (result.getDecision().equals("READY_FOR_INTERNAL_DEMO_TRIAL")) {
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
