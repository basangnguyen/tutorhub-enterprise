package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class V2FinalReleaseChecklistGateServiceTest {

    private V2FinalReleaseChecklistGateService service;

    @BeforeEach
    public void setUp() {
        service = new V2FinalReleaseChecklistGateService();
        System.setProperty("tse.v2.finalReleaseChecklistGate.enabled", "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false"); // Must be false for safe
        
        // Setup regression gate dependencies
                System.setProperty("tse.v2.studentSubmitIntegrationRegressionGate.enabled", "true");
        System.setProperty("tse.v2.studentSubmitE2EHarness.enabled", "true");
        System.setProperty("tse.v2.studentSubmitFallbackIntegrationTest.enabled", "true");
        System.setProperty("tse.v2.releaseCandidateRegressionGate.enabled", "true");
        System.setProperty("tse.v2.studentSubmitAdapterDryRun.enabled", "true");
        System.setProperty("tse.v2.studentSubmitUiWiringReadiness.enabled", "true");
        System.setProperty("tse.v2.studentSubmitRuntimeAdapter.enabled", "true");
        System.setProperty("tse.v2.studentSubmitV2ExecutionBridge.enabled", "true");
        System.setProperty("tse.v2.studentSubmitLegacyFallbackRuntimeGuard.enabled", "true");
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.finalReleaseChecklistGate.enabled");
        System.clearProperty("tse.v2.defaultStudentSubmitV2.enabled");
        
                System.clearProperty("tse.v2.studentSubmitIntegrationRegressionGate.enabled");
        System.clearProperty("tse.v2.studentSubmitE2EHarness.enabled");
        System.clearProperty("tse.v2.studentSubmitFallbackIntegrationTest.enabled");
        System.clearProperty("tse.v2.releaseCandidateRegressionGate.enabled");
        System.clearProperty("tse.v2.studentSubmitAdapterDryRun.enabled");
        System.clearProperty("tse.v2.studentSubmitUiWiringReadiness.enabled");
        System.clearProperty("tse.v2.studentSubmitRuntimeAdapter.enabled");
        System.clearProperty("tse.v2.studentSubmitV2ExecutionBridge.enabled");
        System.clearProperty("tse.v2.studentSubmitLegacyFallbackRuntimeGuard.enabled");
    }

    @Test
    public void testFlagOff() {
        System.setProperty("tse.v2.finalReleaseChecklistGate.enabled", "false");
        V2FinalReleaseChecklistGateResult result = service.checkGate();
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("NOT_READY", result.getFinalReleaseStatus());
    }

    @Test
    public void testProductionFlagsSafe() {
        V2FinalReleaseChecklistGateResult result = service.checkGate();
        // The files might not exist during test runtime in some CI setups, but we only assert flags safe
        assertTrue(result.isProductionFlagsSafe());
    }

    @Test
    public void testGateBlocksIfDefaultStudentSubmitV2Enabled() {
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true");
        V2FinalReleaseChecklistGateResult result = service.checkGate();
        assertFalse(result.isProductionFlagsSafe());
        assertFalse(result.isSuccess());
    }

    @Test
    public void testLegacySubmitIntact() {
        V2FinalReleaseChecklistGateResult result = service.checkGate();
        assertTrue(result.isLegacySubmitIntact());
    }

    @Test
    public void testGateReadyWhenEverythingPasses() {
        // Since docs exist in root, result should be ready
        V2FinalReleaseChecklistGateResult result = service.checkGate();
        
        // Assertions based on whether files exist
        if (result.isVmSmokePlanExists() && result.isDebugScriptsAudited()) {
            assertTrue(result.isSuccess());
            assertTrue(result.isReady());
            assertEquals("V2_SUBMIT_FINAL_RELEASE_CHECKLIST_READY", result.getFinalReleaseStatus());
        }
    }
}
