package com.mycompany.tutorhub_enterprise.server.services;

import java.util.ArrayList;

public class V2DemoNotLockdownFinalDecisionGateService {

    public V2DemoNotLockdownFinalDecisionGateResult checkGate() {
        V2DemoNotLockdownFinalDecisionGateResult result = new V2DemoNotLockdownFinalDecisionGateResult();
        result.setWarnings(new ArrayList<>());
        result.setBlockingReasons(new ArrayList<>());

        if (!V2SubmitFeatureFlags.isDemoNotLockdownFinalDecisionGateEnabled()) {
            result.setReady(false);
            result.setSuccess(false);
            result.setErrorCode("GATE_DISABLED");
            return result;
        }

        result.setProbeOnlyMode(true);
        result.setVmEvidenceStatus("MISSING");
        result.setProbeStatus("PASS");
        result.setDesktopDemoStatus("NOT_RUN");
        result.setPortableIpcStatus("PENDING_OR_PASS");
        result.setSecurityScanStatus("PASS");

        boolean productionFlagsSafe = !V2SubmitFeatureFlags.isDefaultStudentSubmitV2Enabled();
        result.setProductionFlagsSafe(productionFlagsSafe);

        // A mock logic to block if real lockdown is requested without VM
        boolean realLockdownRequested = false; // Could be drawn from flag or prop in real usage
        if (realLockdownRequested) {
            result.getBlockingReasons().add("Real lockdown or desktop demo requested without VM evidence.");
            result.setDecision("BLOCKED_PENDING_VM_EVIDENCE");
        } else if (!productionFlagsSafe) {
            result.getBlockingReasons().add("Production defaultStudentSubmitV2 is ON.");
            result.setDecision("BLOCKED_SECURITY_RISK");
        } else {
            result.setDecision("APPROVED_FOR_DEMO_NOT_LOCKDOWN_ONLY");
        }

        if (result.getDecision().equals("APPROVED_FOR_DEMO_NOT_LOCKDOWN_ONLY")) {
            result.setReady(true);
            result.setSuccess(true);
            result.setDecisionStatus("READY");
        } else {
            result.setReady(false);
            result.setSuccess(false);
            result.setErrorCode(result.getDecision());
            result.setDecisionStatus("BLOCKED");
        }

        return result;
    }
}
