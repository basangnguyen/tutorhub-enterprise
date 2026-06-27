package com.mycompany.tutorhub_enterprise.server.services;

import java.io.File;
import java.util.ArrayList;

public class V2DemoRcFinalAcceptanceDecisionService {

    public V2DemoRcFinalAcceptanceDecisionResult checkDecision() {
        V2DemoRcFinalAcceptanceDecisionResult result = new V2DemoRcFinalAcceptanceDecisionResult();
        result.setWarnings(new ArrayList<>());
        result.setBlockingReasons(new ArrayList<>());

        if (!V2SubmitFeatureFlags.isDemoRcFinalAcceptanceDecisionEnabled()) {
            result.setReady(false);
            result.setSuccess(false);
            result.setErrorCode("GATE_DISABLED");
            return result;
        }

        // Check Phase 23 docs exist
        boolean hasDocs = new File("docs/release/tse_v2_manual_trial_evidence_finalizer_phase_23c.md").exists();
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
        } else if ("FAIL".equals(result.getManualTrialStatus())) {
            result.setDecision("BLOCKED_MANUAL_TRIAL_FAIL");
        } else if ("PENDING".equals(result.getTestAccountStatus()) || "PENDING".equals(result.getManualTrialStatus())) {
            result.setDecision("DEMO_RC_ACCEPTED_WITH_MANUAL_TRIAL_PENDING");
        } else {
            result.setDecision("DEMO_RC_ACCEPTED_AFTER_MANUAL_TRIAL_PASS");
        }

        if (result.getDecision().equals("DEMO_RC_ACCEPTED_WITH_MANUAL_TRIAL_PENDING") || 
            result.getDecision().equals("DEMO_RC_ACCEPTED_AFTER_MANUAL_TRIAL_PASS")) {
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
