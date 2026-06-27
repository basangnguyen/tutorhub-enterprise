package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

public class V2DemoPackageFreezeGateServiceTest {

    private V2DemoPackageFreezeGateService service;

    @BeforeEach
    public void setUp() {
        service = new V2DemoPackageFreezeGateService();
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_PACKAGE_FREEZE_GATE);
        System.clearProperty("tse.v2.defaultStudentSubmitV2.enabled");
        System.clearProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_REGRESSION_SECURITY_RECHECK_GATE);
        System.clearProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_HANDOFF_GATE);
    }

    @Test
    public void testFlagOff() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_PACKAGE_FREEZE_GATE, "false");
        V2DemoPackageFreezeGateResult result = service.checkGate();
        assertFalse(result.isReady());
        assertEquals("GATE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testFreezeGateReady() {
        // Enable required flags
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_PACKAGE_FREEZE_GATE, "true");
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_REGRESSION_SECURITY_RECHECK_GATE, "true");
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_HANDOFF_GATE, "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
        
        V2DemoPackageFreezeGateResult result = service.checkGate();
        
        // Ensure that desktop demo is NOT_RUN
        assertEquals("NOT_RUN", result.getDesktopDemoStatus());
        assertTrue(result.isProductionFlagsSafe());

        // We assume docs exist for this test to pass since they were created physically
        if (result.isTrialManifestExists() && result.isTrialChecklistExists() && result.isReleaseNotesExists() &&
            result.isInternalDemoPacketExists() && result.isStudentSubmitTrialEvidenceExists()) {
            
            assertTrue(result.isReady());
            assertEquals("READY_FOR_INTERNAL_DEMO_TRIAL", result.getDecision());
            assertTrue(result.isRepoStatusDirty());
        } else {
            assertFalse(result.isReady());
            assertEquals("BLOCKED_DOCS_MISSING", result.getErrorCode());
        }
    }
}
