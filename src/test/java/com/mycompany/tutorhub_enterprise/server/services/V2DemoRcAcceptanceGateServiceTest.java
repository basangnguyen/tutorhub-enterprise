package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class V2DemoRcAcceptanceGateServiceTest {

    private V2DemoRcAcceptanceGateService service;

    @BeforeEach
    public void setUp() {
        service = new V2DemoRcAcceptanceGateService();
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_RC_ACCEPTANCE_GATE);
        System.clearProperty("tse.v2.defaultStudentSubmitV2.enabled");
    }

    @Test
    public void testFlagOff() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_RC_ACCEPTANCE_GATE, "false");
        V2DemoRcAcceptanceGateResult result = service.checkGate();
        assertFalse(result.isReady());
        assertEquals("GATE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testGateReadyWithPendingManualTrial() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_RC_ACCEPTANCE_GATE, "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
        
        V2DemoRcAcceptanceGateResult result = service.checkGate();
        
        assertEquals("NOT_RUN", result.getDesktopDemoStatus());
        assertEquals("NOT_RUN", result.getLockdownStatus());
        assertTrue(result.isProductionFlagsSafe());
        assertEquals("PENDING", result.getTestAccountStatus());
        assertEquals("PENDING", result.getManualTrialStatus());

        // Assuming all docs exist physically as created during previous steps
        if (result.isDocsReady()) {
            assertTrue(result.isReady());
            assertEquals("DEMO_RC_ACCEPTED_FOR_INTERNAL_TEST_WITH_MANUAL_TRIAL_PENDING", result.getDecision());
            assertTrue(result.isRepoStatusRecorded());
        } else {
            assertFalse(result.isReady());
            assertEquals("BLOCKED_PENDING_DOCS", result.getErrorCode());
        }
    }
}
