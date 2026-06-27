package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class V2DemoNotLockdownFinalDecisionGateServiceTest {

    private V2DemoNotLockdownFinalDecisionGateService service;

    @BeforeEach
    public void setUp() {
        service = new V2DemoNotLockdownFinalDecisionGateService();
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_NOT_LOCKDOWN_FINAL_DECISION_GATE);
        System.clearProperty("tse.v2.defaultStudentSubmitV2.enabled");
    }

    @Test
    public void testFlagOff() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_NOT_LOCKDOWN_FINAL_DECISION_GATE, "false");
        V2DemoNotLockdownFinalDecisionGateResult result = service.checkGate();
        assertFalse(result.isReady());
        assertEquals("GATE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testProductionFlagOnBlocks() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_NOT_LOCKDOWN_FINAL_DECISION_GATE, "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true");
        
        V2DemoNotLockdownFinalDecisionGateResult result = service.checkGate();
        assertFalse(result.isReady());
        assertEquals("BLOCKED_SECURITY_RISK", result.getErrorCode());
        assertFalse(result.isProductionFlagsSafe());
    }

    @Test
    public void testPassDemoNotLockdownOnly() {
        System.setProperty(V2SubmitFeatureFlags.FLAG_V2_DEMO_NOT_LOCKDOWN_FINAL_DECISION_GATE, "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
        
        V2DemoNotLockdownFinalDecisionGateResult result = service.checkGate();
        assertTrue(result.isReady());
        assertEquals("APPROVED_FOR_DEMO_NOT_LOCKDOWN_ONLY", result.getDecision());
        assertTrue(result.isProbeOnlyMode());
        assertTrue(result.isProductionFlagsSafe());
    }
}
