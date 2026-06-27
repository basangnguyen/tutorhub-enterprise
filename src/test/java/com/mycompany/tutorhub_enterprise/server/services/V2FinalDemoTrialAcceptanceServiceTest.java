package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class V2FinalDemoTrialAcceptanceServiceTest {

    private V2FinalDemoTrialAcceptanceService service;

    @BeforeEach
    public void setUp() {
        service = new V2FinalDemoTrialAcceptanceService();
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(V2SubmitFeatureFlags.FLAG_V2_FINAL_DEMO_TRIAL_ACCEPTANCE);
        System.clearProperty("tse.v2.defaultStudentSubmitV2.enabled");
    }

    @Test
    public void testFlagOff() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_FINAL_DEMO_TRIAL_ACCEPTANCE, "false");
        V2FinalDemoTrialAcceptanceResult result = service.checkGate();
        assertFalse(result.isReady());
        assertEquals("GATE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testDecisionBlockedNoSafeTestEnvironment() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_FINAL_DEMO_TRIAL_ACCEPTANCE, "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
        
        V2FinalDemoTrialAcceptanceResult result = service.checkGate();
        
        assertEquals("NOT_RUN", result.getDesktopDemoStatus());
        assertEquals("NOT_RUN", result.getLockdownStatus());
        assertTrue(result.isProductionFlagsSafe());
        assertEquals("NOT_AVAILABLE", result.getTestAccountStatus());
        assertEquals("BLOCKED_NO_SAFE_TEST_ENVIRONMENT", result.getManualTrialStatus());

        // Assuming docs are present
        if (result.getErrorCode() != null && result.getErrorCode().equals("BLOCKED_PENDING_DOCS")) {
            assertFalse(result.isReady());
        } else {
            assertTrue(result.isReady());
            assertEquals("FINAL_BLOCKED_NO_SAFE_TEST_ENVIRONMENT", result.getDecision());
            assertTrue(result.isRepoStatusRecorded());
        }
    }
}
