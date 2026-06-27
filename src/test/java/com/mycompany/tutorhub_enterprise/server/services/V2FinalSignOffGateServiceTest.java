package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class V2FinalSignOffGateServiceTest {

    private V2FinalSignOffGateService gateService;

    @BeforeEach
    public void setUp() {
        gateService = new V2FinalSignOffGateService();
        System.setProperty("tse.v2.finalSignOffGate.enabled", "true");
        System.setProperty("tse.v2.finalReleaseChecklistGate.enabled", "true");
        System.setProperty("tse.v2.studentSubmitIntegrationRegressionGate.enabled", "true");
        System.setProperty("tse.v2.studentSubmitE2EHarness.enabled", "true");
        System.setProperty("tse.v2.studentSubmitFallbackIntegrationTest.enabled", "true");
        System.setProperty("tse.v2.releaseCandidateRegressionGate.enabled", "true");
        System.clearProperty("tse.v2.defaultStudentSubmitV2.enabled");
    }

    @Test
    public void testGateDisabled_ReturnsNotReady() {
        System.setProperty("tse.v2.finalSignOffGate.enabled", "false");
        V2FinalSignOffGateResult result = gateService.checkGate();
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("FEATURE_DISABLED", result.getErrorCode());
        assertEquals("NOT_READY", result.getFinalSignOffStatus());
    }

    @Test
    public void testProductionFlagUnsafe_BlocksGate() {
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true");
        V2FinalSignOffGateResult result = gateService.checkGate();
        
        assertFalse(result.isProductionFlagsSafe());
        assertFalse(result.isSuccess());
        assertTrue(result.getBlockingReasons().stream().anyMatch(r -> r.contains("UNSAFE")));
    }

    @Test
    public void testGateReadyWhenEverythingPasses() {
        V2FinalSignOffGateResult result = gateService.checkGate();
        
        // Assertions based on whether files exist
        if (result.isReleaseManifestExists() && result.isVmSmokePacketExists() && result.isDebugCleanupPolicyExists() && result.isFinalReleaseChecklistReady()) {
            assertTrue(result.isSuccess());
            assertTrue(result.isReady());
            assertEquals("V2_SUBMIT_READY_FOR_FINAL_REVIEW", result.getFinalSignOffStatus());
        } else {
            assertFalse(result.isSuccess());
            assertFalse(result.isReady());
            assertEquals("FINAL_SIGN_OFF_FAILED", result.getErrorCode());
        }
        
        // DTO Safety check
        // There should be no payloadJson or answer methods in result class
        assertFalse(hasMethod(result.getClass(), "getPayloadJson"));
        assertFalse(hasMethod(result.getClass(), "getScore"));
        assertFalse(hasMethod(result.getClass(), "getAnswerKey"));
        assertFalse(hasMethod(result.getClass(), "getAnswers"));
        assertFalse(hasMethod(result.getClass(), "getPerQuestionResults"));
    }
    
    @Test
    public void testVmSmokeNotExecuted() {
        V2FinalSignOffGateResult result = gateService.checkGate();
        
        assertFalse(result.isVmSmokeExecuted());
        assertEquals("PENDING_NOT_RUN", result.getVmSmokeStatus());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("VM Smoke Execution is pending")));
    }

    private boolean hasMethod(Class<?> clazz, String methodName) {
        try {
            clazz.getMethod(methodName);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
