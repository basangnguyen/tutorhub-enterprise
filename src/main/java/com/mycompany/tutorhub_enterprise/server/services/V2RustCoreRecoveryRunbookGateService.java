package com.mycompany.tutorhub_enterprise.server.services;

import java.io.File;

public class V2RustCoreRecoveryRunbookGateService {

    public V2RustCoreRecoveryRunbookGateResult checkGate() {
        if (!V2SubmitFeatureFlags.isRustCoreRecoveryRunbookGateEnabled()) {
            return new V2RustCoreRecoveryRunbookGateResult(false, "NOT_READY", false);
        }

        File runbook = new File("docs/rust_lockdown_core/tse_desktop_isolation_recovery_abort_runbook_phase_15d.md");
        if (!runbook.exists()) {
            return new V2RustCoreRecoveryRunbookGateResult(false, "RUNBOOK_MISSING", false);
        }

        return new V2RustCoreRecoveryRunbookGateResult(true, "RUST_CORE_RECOVERY_RUNBOOK_READY", true);
    }
}
