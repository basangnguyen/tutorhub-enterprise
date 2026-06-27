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

public class V2RealSubmitTransitionDraftServiceTest {

    private V2RealSubmitTransitionDraftService draftService;
    private MockV2RealSubmitTransitionDraftDAO mockDAO;
    private MockV2RealSubmitPreflightService mockPreflightService;

    @BeforeEach
    public void setup() {
        System.setProperty("tse.v2.realSubmitTransitionDraft.enabled", "true");
        System.setProperty("tse.v2.realSubmitPreflight.enabled", "true");
        
        mockDAO = new MockV2RealSubmitTransitionDraftDAO();
        mockPreflightService = new MockV2RealSubmitPreflightService();
        draftService = new V2RealSubmitTransitionDraftService(mockDAO, mockPreflightService);
    }

    @AfterEach
    public void cleanup() {
        System.clearProperty("tse.v2.realSubmitTransitionDraft.enabled");
        System.clearProperty("tse.v2.realSubmitPreflight.enabled");
    }

    @Test
    public void testFeatureFlagOffReject() {
        System.setProperty("tse.v2.realSubmitTransitionDraft.enabled", "false");
        V2RealSubmitTransitionDraftResult result = draftService.createDraft(100, 500L);
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
        assertEquals("NOT_CREATED", result.getTransitionDraftStatus());
    }

    @Test
    public void testPreflightNotReadyReject() {
        mockPreflightService.mockResult = new V2RealSubmitPreflightResult();
        mockPreflightService.mockResult.setSuccess(false);
        mockPreflightService.mockResult.setReady(false);
        mockPreflightService.mockResult.setErrorCode("ERROR_V2_PREFLIGHT_STATUS_NOT_READY");
        mockPreflightService.mockResult.setPreflightStatus("NOT_READY");

        V2RealSubmitTransitionDraftResult result = draftService.createDraft(100, 500L);
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_V2_PREFLIGHT_STATUS_NOT_READY", result.getErrorCode());
        assertEquals("NOT_CREATED", result.getTransitionDraftStatus());
    }

    @Test
    public void testValidPreflightReadyCreateDraftSuccess() {
        mockPreflightService.mockResult = createReadyPreflightResult();

        V2RealSubmitTransitionDraftResult result = draftService.createDraft(100, 500L);
        
        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertFalse(result.isIdempotent());
        assertEquals("REAL_SUBMIT_TRANSITION_DRAFTED", result.getTransitionDraftStatus());
        assertTrue(result.getTransitionDraftId() > 0);
        
        assertNotNull(mockDAO.lastInserted);
        assertEquals(500L, mockDAO.lastInserted.getSubmitRecordId());
        assertEquals(100, mockDAO.lastInserted.getUserId());
    }

    @Test
    public void testDuplicateSubmitRecordIdIdempotent() throws SQLException {
        V2RealSubmitTransitionDraftRecord existing = new V2RealSubmitTransitionDraftRecord();
        existing.setId(999L);
        existing.setSubmitRecordId(500L);
        existing.setUserId(100);
        existing.setTransitionDraftStatus("REAL_SUBMIT_TRANSITION_DRAFTED");
        mockDAO.existingDraft = existing;

        V2RealSubmitTransitionDraftResult result = draftService.createDraft(100, 500L);
        
        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertTrue(result.isIdempotent());
        assertEquals(999L, result.getTransitionDraftId());
        assertEquals("REAL_SUBMIT_TRANSITION_DRAFTED", result.getTransitionDraftStatus());
        
        assertNull(mockDAO.lastInserted, "Should not insert again");
    }

    @Test
    public void testResultDTONoAnswersOrScore() {
        Field[] fields = V2RealSubmitTransitionDraftResult.class.getDeclaredFields();
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
        public V2RealSubmitTransitionDraftRecord existingDraft = null;
        public V2RealSubmitTransitionDraftRecord lastInserted = null;
        public long nextId = 1L;

        @Override
        public Optional<V2RealSubmitTransitionDraftRecord> findBySubmitRecordId(long submitRecordId) throws SQLException {
            if (existingDraft != null && existingDraft.getSubmitRecordId() == submitRecordId) {
                return Optional.of(existingDraft);
            }
            return Optional.empty();
        }

        @Override
        public long insertDraft(V2RealSubmitTransitionDraftRecord record) throws SQLException {
            lastInserted = record;
            record.setId(nextId);
            return nextId++;
        }
    }

    private static class MockV2RealSubmitPreflightService extends V2RealSubmitPreflightService {
        public V2RealSubmitPreflightResult mockResult = null;

        @Override
        public V2RealSubmitPreflightResult checkPreflight(int userId, long submitRecordId) {
            if (mockResult != null) return mockResult;
            
            // default not ready
            V2RealSubmitPreflightResult res = new V2RealSubmitPreflightResult();
            res.setSuccess(false);
            res.setReady(false);
            res.setErrorCode("ERROR_V2_REAL_TRANSITION_PREFLIGHT_NOT_READY");
            res.setPreflightStatus("NOT_READY");
            return res;
        }
    }
}
