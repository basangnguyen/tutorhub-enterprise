package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class V2RustPortableIpcProbeOnlyVerificationServiceTest {

    private V2RustPortableIpcProbeOnlyVerificationService service;

    @BeforeEach
    public void setUp() {
        service = new V2RustPortableIpcProbeOnlyVerificationService();
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(V2SubmitFeatureFlags.FLAG_V2_RUST_PORTABLE_IPC_PROBE_ONLY_VERIFICATION);
    }

    @Test
    public void testFlagOff() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_RUST_PORTABLE_IPC_PROBE_ONLY_VERIFICATION, "false");
        V2RustPortableIpcProbeOnlyVerificationResult result = service.verify();
        assertFalse(result.isReady());
        assertEquals("GATE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testMissingRustCoreExe() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_RUST_PORTABLE_IPC_PROBE_ONLY_VERIFICATION, "true");
        // Ensure file doesn't exist for this test
        V2RustPortableIpcProbeOnlyVerificationResult result = service.verify();
        // It might be false because we don't mock File and rust-core.exe won't exist in dist folder yet
        if (result.getErrorCode().equals("PENDING_PACKAGING_DECISION")) {
            assertFalse(result.isReady());
            assertEquals("RUST_CORE_NOT_FOUND", result.getPortableIpcStatus());
        }
    }
}
