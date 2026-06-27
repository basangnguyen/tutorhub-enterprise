package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2RealSubmitTransitionDraftRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2RealSubmitTransitionDraftDAO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2RealSubmitAttemptStatusTransitionGateServiceTest {

    private V2RealSubmitAttemptStatusTransitionGateService gateService;
    private MockV2RealSubmitTransitionDraftDAO mockDraftDAO;
    private MockV2RealSubmitPreflightService mockPreflightService;

    @BeforeEach
    public void setup() {
        System.setProperty("tse.v2.realSubmitAttemptStatusTransitionGate.enabled", "true");
        
        mockDraftDAO = new MockV2RealSubmitTransitionDraftDAO();
        mockPreflightService = new MockV2RealSubmitPreflightService();
        gateService = new V2RealSubmitAttemptStatusTransitionGateService(mockDraftDAO, mockPreflightService);
    }

    @AfterEach
    public void cleanup() {
        System.clearProperty("tse.v2.realSubmitAttemptStatusTransitionGate.enabled");
    }

    @Test
    public void testFeatureFlagOffReject() {
        System.setProperty("tse.v2.realSubmitAttemptStatusTransitionGate.enabled", "false");
        V2RealSubmitAttemptStatusTransitionGateResult result = gateService.checkGate(100, 500L);
        
        assertFalse(result.isReady());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
        assertEquals("NOT_READY", result.getStatusTransitionGate());
    }

    @Test
    public void testPreflightNotReadyReject() {
        mockPreflightService.mockResult = new V2RealSubmitPreflightResult();
        mockPreflightService.mockResult.setReady(false);
        mockPreflightService.mockResult.setPreflightStatus("NOT_READY");

        V2RealSubmitAttemptStatusTransitionGateResult result = gateService.checkGate(100, 500L);
        
        assertFalse(result.isReady());
        assertEquals("ERROR_PREFLIGHT_NOT_READY", result.getErrorCode());
    }

    @Test
    public void testTransitionDraftMissingReject() {
        mockPreflightService.mockResult = createReadyPreflightResult();
        mockDraftDAO.mockDraft = null; // missing

        V2RealSubmitAttemptStatusTransitionGateResult result = gateService.checkGate(100, 500L);
        
        assertFalse(result.isReady());
        assertEquals("ERROR_TRANSITION_DRAFT_MISSING", result.getErrorCode());
    }

    @Test
    public void testUserMismatchReject() {
        mockPreflightService.mockResult = createReadyPreflightResult();
        
        V2RealSubmitTransitionDraftRecord draft = new V2RealSubmitTransitionDraftRecord();
        draft.setSubmitRecordId(500L);
        draft.setUserId(999); // different user
        draft.setTransitionDraftStatus("REAL_SUBMIT_TRANSITION_DRAFTED");
        mockDraftDAO.mockDraft = draft;

        V2RealSubmitAttemptStatusTransitionGateResult result = gateService.checkGate(100, 500L);
        
        assertFalse(result.isReady());
        assertEquals("ERROR_USER_MISMATCH", result.getErrorCode());
    }

    @Test
    public void testInvalidDraftStatusReject() {
        mockPreflightService.mockResult = createReadyPreflightResult();
        
        V2RealSubmitTransitionDraftRecord draft = new V2RealSubmitTransitionDraftRecord();
        draft.setSubmitRecordId(500L);
        draft.setUserId(100);
        draft.setTransitionDraftStatus("INVALID_STATUS");
        mockDraftDAO.mockDraft = draft;

        V2RealSubmitAttemptStatusTransitionGateResult result = gateService.checkGate(100, 500L);
        
        assertFalse(result.isReady());
        assertEquals("ERROR_INVALID_DRAFT_STATUS", result.getErrorCode());
    }

    @Test
    public void testValidTransitionDraftReady() {
        mockPreflightService.mockResult = createReadyPreflightResult();
        
        V2RealSubmitTransitionDraftRecord draft = new V2RealSubmitTransitionDraftRecord();
        draft.setSubmitRecordId(500L);
        draft.setUserId(100);
        draft.setTransitionDraftStatus("REAL_SUBMIT_TRANSITION_DRAFTED");
        mockDraftDAO.mockDraft = draft;

        V2RealSubmitAttemptStatusTransitionGateResult result = gateService.checkGate(100, 500L);
        
        assertTrue(result.isReady());
        assertNull(result.getErrorCode());
        assertEquals("READY_FOR_ATTEMPT_STATUS_TRANSITION_DRAFT", result.getStatusTransitionGate());
    }

    @Test
    public void testResultDTONoAnswersOrScore() {
        Field[] fields = V2RealSubmitAttemptStatusTransitionGateResult.class.getDeclaredFields();
        for (Field field : fields) {
            String name = field.getName();
            assertFalse("answers".equals(name) || "selectedOptionId".equals(name) || "answerKey".equals(name) 
                || "isCorrect".equals(name) || "correctOption".equals(name) || "score".equals(name) 
                || "gradingResult".equals(name) || "sessionToken".equals(name) || "keyB64".equals(name) 
                || "plaintext".equals(name), "Unsafe field found: " + name);
        }
    }

    private V2RealSubmitPreflightResult createReadyPreflightResult() {
        V2RealSubmitPreflightResult res = new V2RealSubmitPreflightResult();
        res.setSuccess(true);
        res.setReady(true);
        res.setPreflightStatus("READY_FOR_REAL_SUBMIT_DRAFT");
        res.setUserId(100);
        res.setSubmitRecordId(500L);
        res.setExamId(10);
        res.setPaperId(20);
        res.setAttemptId("att-xyz");
        res.setLedgerId(300L);
        res.setClosureDraftId(400L);
        res.setPayloadHash("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890");
        return res;
    }

    // --- Mock Classes ---

    private static class MockV2RealSubmitTransitionDraftDAO extends V2RealSubmitTransitionDraftDAO {
        public V2RealSubmitTransitionDraftRecord mockDraft = null;

        @Override
        public Optional<V2RealSubmitTransitionDraftRecord> findBySubmitRecordId(long submitRecordId) throws SQLException {
            if (mockDraft != null && mockDraft.getSubmitRecordId() == submitRecordId) {
                return Optional.of(mockDraft);
            }
            return Optional.empty();
        }

        @Override
        public long insertDraft(V2RealSubmitTransitionDraftRecord record) throws SQLException {
            return 1L;
        }
    }

    private static class MockV2RealSubmitPreflightService extends V2RealSubmitPreflightService {
        public V2RealSubmitPreflightResult mockResult = null;

        @Override
        public V2RealSubmitPreflightResult checkPreflight(int userId, long submitRecordId) {
            if (mockResult != null) return mockResult;
            
            V2RealSubmitPreflightResult res = new V2RealSubmitPreflightResult();
            res.setSuccess(false);
            res.setReady(false);
            res.setErrorCode("ERROR_V2_PREFLIGHT_NOT_READY");
            res.setPreflightStatus("NOT_READY");
            return res;
        }
    }
}
