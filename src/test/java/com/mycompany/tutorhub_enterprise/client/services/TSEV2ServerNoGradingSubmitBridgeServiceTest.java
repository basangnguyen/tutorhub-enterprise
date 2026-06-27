package com.mycompany.tutorhub_enterprise.client.services;

import com.mycompany.tutorhub_enterprise.client.exam.ui.TSEV2SubmitPayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class TSEV2ServerNoGradingSubmitBridgeServiceTest {

    private TSEV2ServerNoGradingSubmitBridgeService bridge;

    @BeforeEach
    public void setUp() {
        System.setProperty("tse.v2.clientServerNoGradingSubmit.enabled", "true");
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.clientServerNoGradingSubmit.enabled");
    }

    private TSEV2SubmitPayload createDummyPayload() {
        TSEV2SubmitPayload p = new TSEV2SubmitPayload();
        p.setPayloadVersion("1.0");
        p.setFlow("PAPER_START_V2");
        p.setExamId(1);
        return p;
    }

    @Test
    public void testSubmitDisabled() {
        System.setProperty("tse.v2.clientServerNoGradingSubmit.enabled", "false");
        AtomicInteger callCount = new AtomicInteger();
        TSEV2ServerSubmitTransport transport = (a, p) -> {
            callCount.incrementAndGet();
            return "should not be called";
        };
        bridge = new TSEV2ServerNoGradingSubmitBridgeService(transport);
        
        TSEV2ServerNoGradingSubmitBridgeResult result = bridge.submitNoGrading(createDummyPayload());
        assertFalse(result.isSuccess());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
        assertEquals(0, callCount.get());
    }

    @Test
    public void testSubmitUsesCanonicalActionAndParsesSuccessResponse() {
        AtomicReference<String> actionRef = new AtomicReference<>();
        TSEV2ServerSubmitTransport transport = (a, p) -> {
            actionRef.set(a);
            return "{\"success\":true,\"closureDraftId\":100,\"ledgerId\":200,\"submitRecordId\":300,"
                    + "\"payloadHash\":\"abc\",\"finalStatus\":\"CLOSURE_DRAFTED_NO_GRADING\",\"idempotent\":true}";
        };
        bridge = new TSEV2ServerNoGradingSubmitBridgeService(transport);
        
        TSEV2ServerNoGradingSubmitBridgeResult result = bridge.submitNoGrading(createDummyPayload());
        assertTrue(result.isSuccess());
        assertEquals(TSEV2ServerNoGradingSubmitBridgeService.ACTION, actionRef.get());
        assertNotEquals("EXAM_SUBMIT", actionRef.get());
        assertEquals(100L, result.getClosureDraftId());
        assertEquals(200L, result.getLedgerId());
        assertEquals(300L, result.getSubmitRecordId());
        assertEquals("abc", result.getPayloadHash());
        assertEquals("CLOSURE_DRAFTED_NO_GRADING", result.getClosureStatus());
        assertEquals("CLOSURE_DRAFTED_NO_GRADING", result.getFinalStatus());
        assertTrue(result.isIdempotent());
    }

    @Test
    public void testSubmitParsesNestedPacketDataResponse() {
        TSEV2ServerSubmitTransport transport = (a, p) ->
                "{\"success\":true,\"data\":\"{\\\"success\\\":true,\\\"closureDraftId\\\":101,"
                        + "\\\"ledgerId\\\":202,\\\"submitRecordId\\\":303,\\\"payloadHash\\\":\\\"def\\\","
                        + "\\\"closureStatus\\\":\\\"CLOSURE_DRAFTED_NO_GRADING\\\"}\"}";
        bridge = new TSEV2ServerNoGradingSubmitBridgeService(transport);

        TSEV2ServerNoGradingSubmitBridgeResult result = bridge.submitNoGrading(createDummyPayload());

        assertTrue(result.isSuccess());
        assertEquals(101L, result.getClosureDraftId());
        assertEquals(202L, result.getLedgerId());
        assertEquals(303L, result.getSubmitRecordId());
        assertEquals("def", result.getPayloadHash());
        assertEquals("CLOSURE_DRAFTED_NO_GRADING", result.getFinalStatus());
    }

    @Test
    public void testSubmitErrorResponse() {
        TSEV2ServerSubmitTransport transport = (a, p) -> "{\"status\":\"ERROR\",\"errorCode\":\"ERROR_ORCHESTRATOR_HALTED\"}";
        bridge = new TSEV2ServerNoGradingSubmitBridgeService(transport);
        
        TSEV2ServerNoGradingSubmitBridgeResult result = bridge.submitNoGrading(createDummyPayload());
        assertFalse(result.isSuccess());
        assertEquals("ERROR_ORCHESTRATOR_HALTED", result.getErrorCode());
    }

    @Test
    public void testSubmitRejectUnsafeResponseScore() {
        TSEV2ServerSubmitTransport transport = (a, p) -> "{\"success\":true,\"score\":10.0}";
        bridge = new TSEV2ServerNoGradingSubmitBridgeService(transport);
        
        TSEV2ServerNoGradingSubmitBridgeResult result = bridge.submitNoGrading(createDummyPayload());
        assertFalse(result.isSuccess());
        assertEquals("ERROR_UNSAFE_RESPONSE_REJECTED", result.getErrorCode());
    }

    @Test
    public void testSubmitRejectUnsafeResponseAnswerKey() {
        TSEV2ServerSubmitTransport transport = (a, p) -> "{\"success\":true,\"answerKey\":\"A,B,C\"}";
        bridge = new TSEV2ServerNoGradingSubmitBridgeService(transport);
        
        TSEV2ServerNoGradingSubmitBridgeResult result = bridge.submitNoGrading(createDummyPayload());
        assertFalse(result.isSuccess());
        assertEquals("ERROR_UNSAFE_RESPONSE_REJECTED", result.getErrorCode());
    }

    @Test
    public void testSubmitRejectUnsafeResponseMarkers() {
        String[] unsafeResponses = {
                "{\"success\":true,\"isCorrect\":true}",
                "{\"success\":true,\"correctOption\":\"A\"}",
                "{\"success\":true,\"selectedOptionId\":11}",
                "{\"success\":true,\"answers\":[]}",
                "{\"success\":true,\"gradingResult\":\"PASS\"}",
                "{\"success\":true,\"sessionToken\":\"token\"}",
                "{\"success\":true,\"keyB64\":\"secret\"}",
                "{\"success\":true,\"plaintext\":\"raw\"}"
        };

        for (String unsafeResponse : unsafeResponses) {
            bridge = new TSEV2ServerNoGradingSubmitBridgeService((a, p) -> unsafeResponse);

            TSEV2ServerNoGradingSubmitBridgeResult result = bridge.submitNoGrading(createDummyPayload());

            assertFalse(result.isSuccess(), "Unsafe response should be rejected: " + unsafeResponse);
            assertEquals("ERROR_UNSAFE_RESPONSE_REJECTED", result.getErrorCode());
        }
    }
}
