package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

public class V2DemoHandoffGateServiceTest {

    private V2DemoHandoffGateService service;

    @BeforeEach
    public void setUp() {
        service = new V2DemoHandoffGateService();
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_HANDOFF_GATE);
        System.clearProperty("tse.v2.defaultStudentSubmitV2.enabled");
    }

    @Test
    public void testFlagOff() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_HANDOFF_GATE, "false");
        V2DemoHandoffGateResult result = service.checkGate();
        assertFalse(result.isReady());
        assertEquals("GATE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testProductionFlagBlocksHandoff() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_HANDOFF_GATE, "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true");
        
        V2DemoHandoffGateResult result = service.checkGate();
        assertFalse(result.isReady());
        assertEquals("BLOCKED_SECURITY_RISK", result.getErrorCode());
        assertFalse(result.isProductionFlagsSafe());
    }

    @Test
    public void testReadyForDemoNotLockdownHandoff() throws IOException {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_HANDOFF_GATE, "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
        
        // Assume files exist (they do in the project now)
        V2DemoHandoffGateResult result = service.checkGate();
        assertTrue(result.isProductionFlagsSafe());
        assertEquals("NOT_RUN", result.getDesktopDemoStatus());
        
        if (result.isTrialManifestExists() && result.isTrialChecklistExists() && result.isReleaseNotesExists()) {
            assertTrue(result.isReady());
            assertEquals("READY_FOR_DEMO_NOT_LOCKDOWN_HANDOFF", result.getDecision());
            assertEquals("APPROVED", result.getHandoffStatus());
        } else {
            assertFalse(result.isReady());
            assertEquals("BLOCKED_PENDING_TRIAL_DOCS", result.getErrorCode());
            assertEquals("BLOCKED", result.getHandoffStatus());
        }
    }
}
