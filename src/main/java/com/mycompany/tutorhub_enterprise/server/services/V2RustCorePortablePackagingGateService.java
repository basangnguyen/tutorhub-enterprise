package com.mycompany.tutorhub_enterprise.server.services;

public class V2RustCorePortablePackagingGateService {

    public V2RustCorePortablePackagingGateResult checkGate() {
        if (!V2SubmitFeatureFlags.isRustCorePortablePackagingGateEnabled()) {
            return new V2RustCorePortablePackagingGateResult(false, "NOT_READY", false);
        }

        // According to Phase 15 constraints, packaging decision is pending.
        return new V2RustCorePortablePackagingGateResult(true, "PENDING_PACKAGING_DECISION", true);
    }
}
