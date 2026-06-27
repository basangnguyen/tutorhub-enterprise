package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class V2StudentSubmitLegacyFallbackServiceTest {

    private V2StudentSubmitLegacyFallbackService service;

    @BeforeEach
    public void setUp() {
        V2StudentSubmitAdapterWiringService stubWiringService = new V2StudentSubmitAdapterWiringService() {
            @Override
            public V2StudentSubmitAdapterWiringResult resolveRoute(int userId, String attemptId, String payloadJson) {
                V2StudentSubmitAdapterWiringResult r = new V2StudentSubmitAdapterWiringResult();
                boolean v2Default = "true".equals(System.getProperty("tse.v2.defaultStudentSubmitV2.enabled"));
                
                if (!v2Default) {
                    r.setSuccess(true);
                    r.setReady(true);
                    r.setResolvedRoute("LEGACY_V1_STUDENT_SUBMIT");
                } else if ("INVALID_JSON".equals(payloadJson)) {
                    r.setSuccess(false);
                    r.setReady(false);
                    r.setResolvedRoute("NOT_READY");
                } else {
                    r.setSuccess(true);
                    r.setReady(true);
                    r.setResolvedRoute("V2_MANUAL_CANDIDATE_PIPELINE");
                }
                return r;
            }
        };

        service = new V2StudentSubmitLegacyFallbackService(stubWiringService);
        System.setProperty("tse.v2.studentSubmitLegacyFallback.enabled", "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.studentSubmitLegacyFallback.enabled");
        System.clearProperty("tse.v2.defaultStudentSubmitV2.enabled");
    }

    @Test
    public void testFlagDisabled() {
        System.setProperty("tse.v2.studentSubmitLegacyFallback.enabled", "false");
        V2StudentSubmitLegacyFallbackResult result = service.checkFallback(1, "ATTEMPT_1", "{}");
        assertFalse(result.isSuccess());
        assertFalse(result.isFallbackAvailable());
    }

    @Test
    public void testV2Disabled_NoFallbackNeeded() {
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
        V2StudentSubmitLegacyFallbackResult result = service.checkFallback(1, "ATTEMPT_1", "{}");
        assertTrue(result.isSuccess());
        assertTrue(result.isFallbackAvailable());
        assertEquals("LEGACY_V1_STUDENT_SUBMIT", result.getFallbackTarget());
        assertFalse(result.isWouldUseFallback());
        assertEquals("V2_DISABLED_BY_DEFAULT_NO_FALLBACK_NEEDED", result.getFallbackReason());
    }

    @Test
    public void testV2Enabled_GatesReady_NoFallbackNeeded() {
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true");
        String payloadJson = "{\"selectedOptionId\": 1, \"answerKey\": \"A\"}";
        V2StudentSubmitLegacyFallbackResult result = service.checkFallback(1, "ATTEMPT_1", payloadJson);
        assertTrue(result.isSuccess());
        assertTrue(result.isFallbackAvailable());
        assertEquals("LEGACY_V1_STUDENT_SUBMIT", result.getFallbackTarget());
        assertFalse(result.isWouldUseFallback());
        assertEquals("V2_READY_NO_FALLBACK_NEEDED", result.getFallbackReason());
    }

    @Test
    public void testV2Enabled_GatesFail_WouldUseFallback() {
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true");
        String payloadJson = "INVALID_JSON";
        V2StudentSubmitLegacyFallbackResult result = service.checkFallback(1, "ATTEMPT_1", payloadJson);
        assertTrue(result.isSuccess());
        assertTrue(result.isFallbackAvailable());
        assertEquals("LEGACY_V1_STUDENT_SUBMIT", result.getFallbackTarget());
        assertTrue(result.isWouldUseFallback());
        assertEquals("V2_GATES_FAILED_FALLBACK_TO_V1", result.getFallbackReason());
        assertTrue(result.getWarnings().size() > 0);
    }
}
