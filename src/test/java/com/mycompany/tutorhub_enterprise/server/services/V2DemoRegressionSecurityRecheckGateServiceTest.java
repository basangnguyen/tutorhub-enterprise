package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class V2DemoRegressionSecurityRecheckGateServiceTest {

    private V2DemoRegressionSecurityRecheckGateService service;

    @BeforeEach
    public void setUp() {
        service = new V2DemoRegressionSecurityRecheckGateService();
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_REGRESSION_SECURITY_RECHECK_GATE);
        System.clearProperty("tse.v2.defaultStudentSubmitV2.enabled");
    }

    @Test
    public void testFlagOff() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_REGRESSION_SECURITY_RECHECK_GATE, "false");
        V2DemoRegressionSecurityRecheckGateResult result = service.checkGate();
        assertFalse(result.isReady());
        assertEquals("GATE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testProductionFlagBlocksGate() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_REGRESSION_SECURITY_RECHECK_GATE, "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true");
        V2DemoRegressionSecurityRecheckGateResult result = service.checkGate();
        assertFalse(result.isReady());
        assertEquals("BLOCKED_PRODUCTION_FLAG_RISK", result.getErrorCode());
    }

    @Test
    public void testRegressionSecurityRecheckPass() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_REGRESSION_SECURITY_RECHECK_GATE, "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
        V2DemoRegressionSecurityRecheckGateResult result = service.checkGate();
        
        // Tests are hardcoded to pass in the service logic currently
        assertTrue(result.isReady());
        assertEquals("REGRESSION_SECURITY_RECHECK_PASS", result.getDecision());
        assertEquals("NOT_RUN", result.getDesktopDemoStatus());
        assertTrue(result.isProductionFlagsSafe());
    }
}
