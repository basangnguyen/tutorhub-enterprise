package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class V2ManualTrialEvidenceFinalizerServiceTest {

    private V2ManualTrialEvidenceFinalizerService service;

    @BeforeEach
    public void setUp() {
        service = new V2ManualTrialEvidenceFinalizerService();
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(V2SubmitFeatureFlags.FLAG_V2_MANUAL_TRIAL_EVIDENCE_FINALIZER);
        System.clearProperty("tse.v2.defaultStudentSubmitV2.enabled");
    }

    @Test
    public void testFlagOff() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_MANUAL_TRIAL_EVIDENCE_FINALIZER, "false");
        V2ManualTrialEvidenceFinalizerResult result = service.checkFinalizer();
        assertFalse(result.isReady());
        assertEquals("GATE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testFinalizerPendingWithPendingTestAccount() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_MANUAL_TRIAL_EVIDENCE_FINALIZER, "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
        
        V2ManualTrialEvidenceFinalizerResult result = service.checkFinalizer();
        
        assertEquals("NOT_RUN", result.getDesktopDemoStatus());
        assertEquals("NOT_RUN", result.getLockdownStatus());
        assertTrue(result.isProductionFlagsSafe());
        assertEquals("PENDING", result.getTestAccountStatus());
        assertEquals("PENDING", result.getManualTrialStatus());

        if (result.isDocsReady()) {
            assertTrue(result.isReady());
            assertEquals("MANUAL_TRIAL_PENDING_TEST_ACCOUNT", result.getDecision());
            assertTrue(result.isRepoStatusRecorded());
        } else {
            assertFalse(result.isReady());
            assertEquals("BLOCKED_PENDING_DOCS", result.getErrorCode());
        }
    }
}
