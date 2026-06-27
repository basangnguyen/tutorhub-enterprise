package com.mycompany.tutorhub_enterprise.server.services;

import java.io.File;
import java.util.ArrayList;

public class V2RustPhysicalMachineSafeProbeGateService {

    public V2RustPhysicalMachineSafeProbeGateResult checkGate() {
        V2RustPhysicalMachineSafeProbeGateResult result = new V2RustPhysicalMachineSafeProbeGateResult();

        if (!V2SubmitFeatureFlags.isRustPhysicalMachineSafeProbeGateEnabled()) {
            result.setReady(false);
            result.setSuccess(false);
            result.setErrorCode("GATE_DISABLED");
            return result;
        }

        result.setPhysicalMachineDetected(true); // Hardcoded logic since this is physical-machine mode phase
        result.setVmConfirmed(false);
        result.setDesktopDemoAllowed(false);
        result.setProbeOnlyAllowed(true);

        File rustCoreDir = new File("rust-core");
        result.setRustCoreExists(rustCoreDir.exists() && rustCoreDir.isDirectory());

        boolean productionFlagsSafe = !V2SubmitFeatureFlags.isDefaultStudentSubmitV2Enabled();
        result.setProductionFlagsSafe(productionFlagsSafe);

        result.setWarnings(new ArrayList<>());
        result.setBlockingReasons(new ArrayList<>());

        if (!result.isRustCoreExists()) {
            result.getBlockingReasons().add("Rust core directory not found");
        }
        if (!productionFlagsSafe) {
            result.getBlockingReasons().add("Production V2 flag is ON. Unsafe to run.");
        }

        if (result.getBlockingReasons().isEmpty()) {
            result.setPreflightStatus("PHYSICAL_MACHINE_PROBE_ONLY_READY");
            result.setReady(true);
            result.setSuccess(true);
        } else {
            result.setPreflightStatus("PHYSICAL_MACHINE_PROBE_ONLY_NOT_READY");
            result.setReady(false);
            result.setSuccess(false);
            result.setErrorCode("PREFLIGHT_FAILED");
        }

        return result;
    }
}
