package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class V2RustPhysicalMachineSafeProbeGateServiceTest {

    private V2RustPhysicalMachineSafeProbeGateService service;

    @BeforeEach
    public void setUp() {
        service = new V2RustPhysicalMachineSafeProbeGateService();
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(V2SubmitFeatureFlags.FLAG_V2_RUST_PHYSICAL_MACHINE_SAFE_PROBE_GATE);
        System.clearProperty("tse.v2.defaultStudentSubmitV2.enabled");
    }

    @Test
    public void testFlagOff() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_RUST_PHYSICAL_MACHINE_SAFE_PROBE_GATE, "false");
        V2RustPhysicalMachineSafeProbeGateResult result = service.checkGate();
        assertFalse(result.isReady());
        assertEquals("GATE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testPhysicalMachineDesktopDemoAllowedIsFalse() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_RUST_PHYSICAL_MACHINE_SAFE_PROBE_GATE, "true");
        V2RustPhysicalMachineSafeProbeGateResult result = service.checkGate();
        
        // Even if other checks fail, these metadata must be accurate
        assertTrue(result.isPhysicalMachineDetected());
        assertFalse(result.isDesktopDemoAllowed());
        assertTrue(result.isProbeOnlyAllowed());
    }
}
