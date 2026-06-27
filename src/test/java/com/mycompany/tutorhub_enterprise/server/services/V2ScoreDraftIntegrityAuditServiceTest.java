package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2ScoreDraftRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ScoreDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2ScoreDraftIntegrityAuditServiceTest {

    private MockScoreDraftDAO mockScoreDraftDAO;
    private MockSubmitRecordDAO mockSubmitRecordDAO;
    private V2ScoreDraftIntegrityAuditService service;

    private static class MockScoreDraftDAO extends V2ScoreDraftDAO {
        public V2ScoreDraftRecord nextReturn = null;
        @Override
        public Optional<V2ScoreDraftRecord> findBySubmitRecordId(long submitRecordId) {
            return Optional.ofNullable(nextReturn);
        }
    }

    private static class MockSubmitRecordDAO extends V2SubmitRecordDAO {
        public V2SubmitRecord nextReturn = null;
        @Override
        public Optional<V2SubmitRecord> findById(long submitRecordId) throws SQLException {
            return Optional.ofNullable(nextReturn);
        }
    }

    @BeforeEach
    public void setUp() {
        mockScoreDraftDAO = new MockScoreDraftDAO();
        mockSubmitRecordDAO = new MockSubmitRecordDAO();

        System.setProperty("tse.v2.scoreDraftIntegrityAudit.enabled", "true");

        service = new V2ScoreDraftIntegrityAuditService(
                new V2SubmitFeatureFlags(),
                mockScoreDraftDAO,
                mockSubmitRecordDAO
        );
    }
    
    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.scoreDraftIntegrityAudit.enabled");
    }

    private V2ScoreDraftRecord createValidDraft(long submitRecordId, int userId) {
        V2ScoreDraftRecord draft = new V2ScoreDraftRecord();
        draft.setId(1L);
        draft.setSubmitRecordId(submitRecordId);
        draft.setUserId(userId);
        draft.setExamId(10);
        draft.setPaperId(20);
        draft.setAttemptId("att_123");
        draft.setTotalQuestions(10);
        draft.setAnsweredQuestions(8);
        draft.setUnansweredQuestions(2);
        draft.setCorrectCount(5);
        draft.setIncorrectCount(3);
        draft.setRawScore(5.0);
        draft.setMaxScore(10.0);
        draft.setPercentage(50.0);
        return draft;
    }

    private V2SubmitRecord createValidSubmitRecord() {
        V2SubmitRecord record = new V2SubmitRecord();
        record.setPayloadHash("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        return record;
    }

    @Test
    public void testAudit_FeatureFlagDisabled_ShouldReject() throws Exception {
        System.setProperty("tse.v2.scoreDraftIntegrityAudit.enabled", "false");

        V2ScoreDraftIntegrityAuditResult result = service.audit(1, 100L);

        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("FEATURE_DISABLED", result.getErrorCode());
        assertEquals("NOT_READY", result.getAuditStatus());
    }

    @Test
    public void testAudit_DraftMissing_ShouldReject() throws Exception {
        mockScoreDraftDAO.nextReturn = null;

        V2ScoreDraftIntegrityAuditResult result = service.audit(1, 100L);

        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("SCORE_DRAFT_NOT_FOUND", result.getErrorCode());
        assertEquals("NOT_READY", result.getAuditStatus());
    }

    @Test
    public void testAudit_UserMismatch_ShouldReject() throws Exception {
        mockScoreDraftDAO.nextReturn = createValidDraft(100L, 2);

        V2ScoreDraftIntegrityAuditResult result = service.audit(1, 100L);

        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("USER_MISMATCH", result.getErrorCode());
        assertEquals("NOT_READY", result.getAuditStatus());
    }

    @Test
    public void testAudit_InvalidPayloadHash_ShouldReject() throws Exception {
        mockScoreDraftDAO.nextReturn = createValidDraft(100L, 1);
        
        V2SubmitRecord submitRecord = new V2SubmitRecord();
        submitRecord.setPayloadHash("invalid_short_hash");
        mockSubmitRecordDAO.nextReturn = submitRecord;

        V2ScoreDraftIntegrityAuditResult result = service.audit(1, 100L);

        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("INVALID_PAYLOAD_HASH", result.getErrorCode());
        assertEquals("NOT_READY", result.getAuditStatus());
    }

    @Test
    public void testAudit_InvalidMathTotals_ShouldReject() throws Exception {
        V2ScoreDraftRecord draft = createValidDraft(100L, 1);
        draft.setTotalQuestions(10);
        draft.setCorrectCount(5);
        draft.setIncorrectCount(4); // 5+4+2 = 11 != 10
        draft.setUnansweredQuestions(2);
        
        mockScoreDraftDAO.nextReturn = draft;
        mockSubmitRecordDAO.nextReturn = createValidSubmitRecord();

        V2ScoreDraftIntegrityAuditResult result = service.audit(1, 100L);

        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("INVALID_TOTALS_MATH", result.getErrorCode());
        assertEquals("NOT_READY", result.getAuditStatus());
    }

    @Test
    public void testAudit_Valid_ShouldBeReady() throws Exception {
        mockScoreDraftDAO.nextReturn = createValidDraft(100L, 1);
        mockSubmitRecordDAO.nextReturn = createValidSubmitRecord();

        V2ScoreDraftIntegrityAuditResult result = service.audit(1, 100L);

        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertNull(result.getErrorCode());
        assertEquals("SCORE_DRAFT_INTEGRITY_READY", result.getAuditStatus());
        assertEquals("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", result.getPayloadHash());
        assertEquals(10, result.getTotalQuestions());
    }
}
