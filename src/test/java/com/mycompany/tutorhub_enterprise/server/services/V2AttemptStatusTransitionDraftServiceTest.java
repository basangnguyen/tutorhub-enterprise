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

public class V2AttemptStatusTransitionDraftServiceTest {

    private V2AttemptStatusTransitionDraftService draftService;
    private MockV2AttemptStatusTransitionDraftDAO mockAttemptDraftDAO;
    private MockV2RealSubmitAttemptStatusTransitionGateService mockGateService;
    private MockV2RealSubmitTransitionDraftDAO mockTransitionDraftDAO;

    @BeforeEach
    public void setup() {
        System.setProperty("tse.v2.attemptStatusTransitionDraft.enabled", "true");
        
        mockAttemptDraftDAO = new MockV2AttemptStatusTransitionDraftDAO();
        mockGateService = new MockV2RealSubmitAttemptStatusTransitionGateService();
        mockTransitionDraftDAO = new MockV2RealSubmitTransitionDraftDAO();
        
        draftService = new V2AttemptStatusTransitionDraftService(
            mockAttemptDraftDAO, mockGateService, mockTransitionDraftDAO
        );
    }

    @AfterEach
    public void cleanup() {
        System.clearProperty("tse.v2.attemptStatusTransitionDraft.enabled");
    }

    @Test
    public void testFeatureFlagOffReject() {
        System.setProperty("tse.v2.attemptStatusTransitionDraft.enabled", "false");
        V2AttemptStatusTransitionDraftResult result = draftService.createDraft(100, 500L);
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testGateNotReadyReject() {
        mockGateService.mockResult = new V2RealSubmitAttemptStatusTransitionGateResult();
        mockGateService.mockResult.setReady(false);
        mockGateService.mockResult.setStatusTransitionGate("NOT_READY");

        V2AttemptStatusTransitionDraftResult result = draftService.createDraft(100, 500L);
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_GATE_NOT_READY", result.getErrorCode());
    }

    @Test
    public void testTransitionDraftMissingReject() {
        mockGateService.mockResult = createReadyGateResult();
        mockTransitionDraftDAO.mockDraft = null;

        V2AttemptStatusTransitionDraftResult result = draftService.createDraft(100, 500L);
        
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_TRANSITION_DRAFT_MISSING", result.getErrorCode());
    }

    @Test
    public void testCreateDraftSuccess() {
        mockGateService.mockResult = createReadyGateResult();
        mockTransitionDraftDAO.mockDraft = createTransitionDraftRecord();

        V2AttemptStatusTransitionDraftResult result = draftService.createDraft(100, 500L);
        
        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertFalse(result.isIdempotent());
        assertEquals(1L, result.getDraftId());
        assertEquals("ATTEMPT_STATUS_TRANSITION_DRAFTED", result.getAttemptStatusTransitionDraftStatus());
        assertEquals("SUBMITTED", result.getTargetAttemptStatus());
    }

    @Test
    public void testIdempotentSuccess() {
        mockGateService.mockResult = createReadyGateResult();
        mockTransitionDraftDAO.mockDraft = createTransitionDraftRecord();
        
        V2AttemptStatusTransitionDraftRecord existingDraft = new V2AttemptStatusTransitionDraftRecord();
        existingDraft.setId(99L);
        existingDraft.setSubmitRecordId(500L);
        existingDraft.setAttemptStatusTransitionDraftStatus("ATTEMPT_STATUS_TRANSITION_DRAFTED");
        mockAttemptDraftDAO.mockExistingDraft = existingDraft;

        V2AttemptStatusTransitionDraftResult result = draftService.createDraft(100, 500L);
        
        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertTrue(result.isIdempotent());
        assertEquals(99L, result.getDraftId());
    }

    @Test
    public void testResultDTONoAnswersOrScore() {
        Field[] fields = V2AttemptStatusTransitionDraftResult.class.getDeclaredFields();
        for (Field field : fields) {
            String name = field.getName();
            assertFalse("answers".equals(name) || "selectedOptionId".equals(name) || "answerKey".equals(name) 
                || "isCorrect".equals(name) || "correctOption".equals(name) || "score".equals(name) 
                || "gradingResult".equals(name) || "sessionToken".equals(name) || "keyB64".equals(name) 
                || "plaintext".equals(name), "Unsafe field found: " + name);
        }
    }

    private V2RealSubmitAttemptStatusTransitionGateResult createReadyGateResult() {
        V2RealSubmitAttemptStatusTransitionGateResult res = new V2RealSubmitAttemptStatusTransitionGateResult();
        res.setReady(true);
        res.setStatusTransitionGate("READY_FOR_ATTEMPT_STATUS_TRANSITION_DRAFT");
        return res;
    }
    
    private V2RealSubmitTransitionDraftRecord createTransitionDraftRecord() {
        V2RealSubmitTransitionDraftRecord rec = new V2RealSubmitTransitionDraftRecord();
        rec.setId(10L);
        rec.setSubmitRecordId(500L);
        rec.setUserId(100);
        rec.setExamId(10);
        rec.setPaperId(20);
        rec.setAttemptId("att-xyz");
        rec.setPayloadHash("hash");
        rec.setPreflightStatus("READY");
        rec.setTransitionDraftStatus("REAL_SUBMIT_TRANSITION_DRAFTED");
        return rec;
    }

    // --- Mock Classes ---

    private static class MockV2AttemptStatusTransitionDraftDAO extends V2AttemptStatusTransitionDraftDAO {
        public V2AttemptStatusTransitionDraftRecord mockExistingDraft = null;

        @Override
        public void ensureSchema() throws SQLException { }

        @Override
        public Optional<V2AttemptStatusTransitionDraftRecord> findBySubmitRecordId(long submitRecordId) throws SQLException {
            if (mockExistingDraft != null && mockExistingDraft.getSubmitRecordId() == submitRecordId) {
                return Optional.of(mockExistingDraft);
            }
            return Optional.empty();
        }

        @Override
        public long insertDraft(V2AttemptStatusTransitionDraftRecord record) throws SQLException {
            return 1L;
        }
    }

    private static class MockV2RealSubmitAttemptStatusTransitionGateService extends V2RealSubmitAttemptStatusTransitionGateService {
        public V2RealSubmitAttemptStatusTransitionGateResult mockResult = null;

        @Override
        public V2RealSubmitAttemptStatusTransitionGateResult checkGate(int userId, long submitRecordId) {
            if (mockResult != null) return mockResult;
            V2RealSubmitAttemptStatusTransitionGateResult res = new V2RealSubmitAttemptStatusTransitionGateResult();
            res.setReady(false);
            res.setErrorCode("MOCK_GATE_FAIL");
            res.setStatusTransitionGate("NOT_READY");
            return res;
        }
    }
    
    private static class MockV2RealSubmitTransitionDraftDAO extends V2RealSubmitTransitionDraftDAO {
        public V2RealSubmitTransitionDraftRecord mockDraft = null;
        
        @Override
        public Optional<V2RealSubmitTransitionDraftRecord> findBySubmitRecordId(long submitRecordId) throws SQLException {
            if (mockDraft != null && mockDraft.getSubmitRecordId() == submitRecordId) {
                return Optional.of(mockDraft);
            }
            return Optional.empty();
        }
    }
}
