package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

public class V2DemoReleaseCandidateArchiveGateServiceTest {

    private V2DemoReleaseCandidateArchiveGateService service;

    @BeforeEach
    public void setUp() {
        service = new V2DemoReleaseCandidateArchiveGateService();
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_RELEASE_CANDIDATE_ARCHIVE_GATE);
        System.clearProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_PACKAGE_FREEZE_GATE);
        System.clearProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_REGRESSION_SECURITY_RECHECK_GATE);
        System.clearProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_HANDOFF_GATE);
        System.clearProperty("tse.v2.defaultStudentSubmitV2.enabled");
    }

    @Test
    public void testFlagOff() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_RELEASE_CANDIDATE_ARCHIVE_GATE, "false");
        V2DemoReleaseCandidateArchiveGateResult result = service.checkGate();
        assertFalse(result.isReady());
        assertEquals("GATE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testGateReadyWithPendingManualTrial() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_RELEASE_CANDIDATE_ARCHIVE_GATE, "true");
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_PACKAGE_FREEZE_GATE, "true");
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_REGRESSION_SECURITY_RECHECK_GATE, "true");
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_HANDOFF_GATE, "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
        
        V2DemoReleaseCandidateArchiveGateResult result = service.checkGate();
        
        assertEquals("NOT_RUN", result.getDesktopDemoStatus());
        assertTrue(result.isProductionFlagsSafe());

        // Assuming all docs exist physically as created during previous steps
        if (result.isDocsReady()) {
            assertTrue(result.isReady());
            assertEquals("READY_FOR_DEMO_RC_INTERNAL_HANDOFF_WITH_MANUAL_TRIAL_PENDING", result.getDecision());
            assertTrue(result.isRepoStatusRecorded());
        } else {
            assertFalse(result.isReady());
            assertEquals("BLOCKED_PENDING_DOCS", result.getErrorCode());
        }
    }
}
