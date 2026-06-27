package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class V2FinalManualTrialResultGateServiceTest {

    private V2FinalManualTrialResultGateService service;

    @BeforeEach
    public void setUp() {
        service = new V2FinalManualTrialResultGateService();
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(V2SubmitFeatureFlags.FLAG_V2_FINAL_MANUAL_TRIAL_RESULT_GATE);
        System.clearProperty("tse.v2.defaultStudentSubmitV2.enabled");
    }

    @Test
    public void testFlagOff() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_FINAL_MANUAL_TRIAL_RESULT_GATE, "false");
        V2FinalManualTrialResultGateResult result = service.checkGate();
        assertFalse(result.isReady());
        assertEquals("GATE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testDecisionBlockedNoSafeTestAccount() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_FINAL_MANUAL_TRIAL_RESULT_GATE, "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
        
        V2FinalManualTrialResultGateResult result = service.checkGate();
        
        assertEquals("NOT_RUN", result.getDesktopDemoStatus());
        assertEquals("NOT_RUN", result.getLockdownStatus());
        assertTrue(result.isProductionFlagsSafe());
        assertEquals("BLOCKED_NO_SAFE_TEST_ACCOUNT", result.getTestAccountStatus());
        assertEquals("BLOCKED_NO_SAFE_TEST_ACCOUNT", result.getManualTrialStatus());

        if (result.isDocsReady()) {
            assertTrue(result.isReady());
            assertEquals("FINAL_MANUAL_TRIAL_BLOCKED_NO_SAFE_TEST_ACCOUNT", result.getDecision());
            assertTrue(result.isRepoStatusRecorded());
        } else {
            assertFalse(result.isReady());
            assertEquals("BLOCKED_PENDING_DOCS", result.getErrorCode());
        }
    }
}
