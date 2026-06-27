package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class V2DemoRcFinalAcceptanceDecisionServiceTest {

    private V2DemoRcFinalAcceptanceDecisionService service;

    @BeforeEach
    public void setUp() {
        service = new V2DemoRcFinalAcceptanceDecisionService();
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_RC_FINAL_ACCEPTANCE_DECISION);
        System.clearProperty("tse.v2.defaultStudentSubmitV2.enabled");
    }

    @Test
    public void testFlagOff() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_RC_FINAL_ACCEPTANCE_DECISION, "false");
        V2DemoRcFinalAcceptanceDecisionResult result = service.checkDecision();
        assertFalse(result.isReady());
        assertEquals("GATE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testDecisionPendingWithPendingTestAccount() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_RC_FINAL_ACCEPTANCE_DECISION, "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
        
        V2DemoRcFinalAcceptanceDecisionResult result = service.checkDecision();
        
        assertEquals("NOT_RUN", result.getDesktopDemoStatus());
        assertEquals("NOT_RUN", result.getLockdownStatus());
        assertTrue(result.isProductionFlagsSafe());
        assertEquals("PENDING", result.getTestAccountStatus());
        assertEquals("PENDING", result.getManualTrialStatus());

        if (result.isDocsReady()) {
            assertTrue(result.isReady());
            assertEquals("DEMO_RC_ACCEPTED_WITH_MANUAL_TRIAL_PENDING", result.getDecision());
            assertTrue(result.isRepoStatusRecorded());
        } else {
            assertFalse(result.isReady());
            assertEquals("BLOCKED_PENDING_DOCS", result.getErrorCode());
        }
    }
}
