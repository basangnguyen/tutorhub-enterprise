package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptStatusTransitionDraftRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2RealSubmitTransitionDraftRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptStatusTransitionDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2RealSubmitTransitionDraftDAO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2RealSubmitReadinessOrchestratorServiceTest {

    private V2RealSubmitReadinessOrchestratorService orchService;
    private MockV2RealSubmitPreflightService mockPreflightService;
    private MockV2RealSubmitTransitionDraftDAO mockTransitionDraftDAO;
    private MockV2RealSubmitAttemptStatusTransitionGateService mockAttemptStatusGateService;
    private MockV2AttemptStatusTransitionDraftDAO mockAttemptStatusDraftDAO;

    @BeforeEach
    public void setup() {
        System.setProperty("tse.v2.realSubmitReadinessOrchestrator.enabled", "true");
        
        mockPreflightService = new MockV2RealSubmitPreflightService();
        mockTransitionDraftDAO = new MockV2RealSubmitTransitionDraftDAO();
        mockAttemptStatusGateService = new MockV2RealSubmitAttemptStatusTransitionGateService();
        mockAttemptStatusDraftDAO = new MockV2AttemptStatusTransitionDraftDAO();
        
        orchService = new V2RealSubmitReadinessOrchestratorService(
            mockPreflightService, mockTransitionDraftDAO, mockAttemptStatusGateService, mockAttemptStatusDraftDAO
        );
    }

    @AfterEach
    public void cleanup() {
        System.clearProperty("tse.v2.realSubmitReadinessOrchestrator.enabled");
    }

    @Test
    public void testFeatureFlagOffReject() {
        System.setProperty("tse.v2.realSubmitReadinessOrchestrator.enabled", "false");
        V2RealSubmitReadinessOrchestratorResult result = orchService.checkReadiness(100, 500L);
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
        assertEquals("NOT_READY", result.getReadinessStatus());
    }

    @Test
    public void testPreflightNotReady() {
        mockPreflightService.mockResult = new V2RealSubmitPreflightResult();
        mockPreflightService.mockResult.setReady(false);
        mockPreflightService.mockResult.setPreflightStatus("NOT_READY");

        V2RealSubmitReadinessOrchestratorResult result = orchService.checkReadiness(100, 500L);
        
        assertTrue(result.isSuccess()); // Call succeeded
        assertFalse(result.isReady());
        assertFalse(result.isPreflightReady());
        assertEquals("NOT_READY", result.getReadinessStatus());
    }

    @Test
    public void testTransitionDraftMissingOrNotReady() {
        mockPreflightService.mockResult = createReadyPreflightResult();
        mockTransitionDraftDAO.mockDraft = null;

        V2RealSubmitReadinessOrchestratorResult result = orchService.checkReadiness(100, 500L);
        
        assertTrue(result.isSuccess());
        assertFalse(result.isReady());
        assertTrue(result.isPreflightReady());
        assertFalse(result.isTransitionDraftReady());
        assertEquals("NOT_READY", result.getReadinessStatus());
    }

    @Test
    public void testAttemptStatusGateNotReady() {
        mockPreflightService.mockResult = createReadyPreflightResult();
        mockTransitionDraftDAO.mockDraft = createTransitionDraftRecord();
        
        V2RealSubmitAttemptStatusTransitionGateResult gateRes = new V2RealSubmitAttemptStatusTransitionGateResult();
        gateRes.setReady(false);
        gateRes.setStatusTransitionGate("NOT_READY");
        mockAttemptStatusGateService.mockResult = gateRes;

        V2RealSubmitReadinessOrchestratorResult result = orchService.checkReadiness(100, 500L);
        
        assertTrue(result.isSuccess());
        assertFalse(result.isReady());
        assertTrue(result.isPreflightReady());
        assertTrue(result.isTransitionDraftReady());
        assertFalse(result.isAttemptStatusGateReady());
        assertEquals("NOT_READY", result.getReadinessStatus());
    }

    @Test
    public void testAttemptStatusTransitionDraftMissingOrNotReady() {
        mockPreflightService.mockResult = createReadyPreflightResult();
        mockTransitionDraftDAO.mockDraft = createTransitionDraftRecord();
        mockAttemptStatusGateService.mockResult = createReadyGateResult();
        mockAttemptStatusDraftDAO.mockDraft = null;

        V2RealSubmitReadinessOrchestratorResult result = orchService.checkReadiness(100, 500L);
        
        assertTrue(result.isSuccess());
        assertFalse(result.isReady());
        assertTrue(result.isPreflightReady());
        assertTrue(result.isTransitionDraftReady());
        assertTrue(result.isAttemptStatusGateReady());
        assertFalse(result.isAttemptStatusTransitionDraftReady());
        assertEquals("NOT_READY", result.getReadinessStatus());
    }

    @Test
    public void testAllGatesReady() {
        mockPreflightService.mockResult = createReadyPreflightResult();
        mockTransitionDraftDAO.mockDraft = createTransitionDraftRecord();
        mockAttemptStatusGateService.mockResult = createReadyGateResult();
        mockAttemptStatusDraftDAO.mockDraft = createAttemptStatusDraftRecord();

        V2RealSubmitReadinessOrchestratorResult result = orchService.checkReadiness(100, 500L);
        
        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertTrue(result.isPreflightReady());
        assertTrue(result.isTransitionDraftReady());
        assertTrue(result.isAttemptStatusGateReady());
        assertTrue(result.isAttemptStatusTransitionDraftReady());
        assertEquals("READY_FOR_REAL_SUBMIT_STATUS_EXECUTION_DRAFT", result.getReadinessStatus());
    }

    @Test
    public void testResultDTONoAnswersOrScore() {
        Field[] fields = V2RealSubmitReadinessOrchestratorResult.class.getDeclaredFields();
        for (Field field : fields) {
            String name = field.getName();
            assertFalse("answers".equals(name) || "selectedOptionId".equals(name) || "answerKey".equals(name) 
                || "isCorrect".equals(name) || "correctOption".equals(name) || "score".equals(name) 
                || "gradingResult".equals(name) || "sessionToken".equals(name) || "keyB64".equals(name) 
                || "plaintext".equals(name), "Unsafe field found: " + name);
        }
    }

    // --- Helpers & Mocks ---
    
    private V2RealSubmitPreflightResult createReadyPreflightResult() {
        V2RealSubmitPreflightResult res = new V2RealSubmitPreflightResult();
        res.setSuccess(true);
        res.setReady(true);
        res.setPreflightStatus("READY_FOR_REAL_SUBMIT_DRAFT");
        res.setUserId(100);
        res.setExamId(10);
        res.setPaperId(20);
        res.setAttemptId("att-xyz");
        res.setPayloadHash("hash");
        return res;
    }

    private V2RealSubmitTransitionDraftRecord createTransitionDraftRecord() {
        V2RealSubmitTransitionDraftRecord rec = new V2RealSubmitTransitionDraftRecord();
        rec.setTransitionDraftStatus("REAL_SUBMIT_TRANSITION_DRAFTED");
        return rec;
    }

    private V2RealSubmitAttemptStatusTransitionGateResult createReadyGateResult() {
        V2RealSubmitAttemptStatusTransitionGateResult res = new V2RealSubmitAttemptStatusTransitionGateResult();
        res.setReady(true);
        res.setStatusTransitionGate("READY_FOR_ATTEMPT_STATUS_TRANSITION_DRAFT");
        return res;
    }

    private V2AttemptStatusTransitionDraftRecord createAttemptStatusDraftRecord() {
        V2AttemptStatusTransitionDraftRecord rec = new V2AttemptStatusTransitionDraftRecord();
        rec.setAttemptStatusTransitionDraftStatus("ATTEMPT_STATUS_TRANSITION_DRAFTED");
        return rec;
    }

    private static class MockV2RealSubmitPreflightService extends V2RealSubmitPreflightService {
        public V2RealSubmitPreflightResult mockResult;
        @Override public V2RealSubmitPreflightResult checkPreflight(int userId, long submitRecordId) { return mockResult; }
    }

    private static class MockV2RealSubmitTransitionDraftDAO extends V2RealSubmitTransitionDraftDAO {
        public V2RealSubmitTransitionDraftRecord mockDraft;
        @Override public Optional<V2RealSubmitTransitionDraftRecord> findBySubmitRecordId(long submitRecordId) {
            return Optional.ofNullable(mockDraft);
        }
    }

    private static class MockV2RealSubmitAttemptStatusTransitionGateService extends V2RealSubmitAttemptStatusTransitionGateService {
        public V2RealSubmitAttemptStatusTransitionGateResult mockResult;
        @Override public V2RealSubmitAttemptStatusTransitionGateResult checkGate(int userId, long submitRecordId) { return mockResult; }
    }

    private static class MockV2AttemptStatusTransitionDraftDAO extends V2AttemptStatusTransitionDraftDAO {
        public V2AttemptStatusTransitionDraftRecord mockDraft;
        @Override public Optional<V2AttemptStatusTransitionDraftRecord> findBySubmitRecordId(long submitRecordId) {
            return Optional.ofNullable(mockDraft);
        }
    }
}
