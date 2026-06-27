package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2OfficialResultDraftRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2OfficialResultDraftDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2OfficialResultDraftServiceTest {

    private MockAuditService mockAuditService;
    private MockOfficialResultDraftDAO mockDao;
    private V2OfficialResultDraftService service;

    private static class MockAuditService extends V2ScoreDraftIntegrityAuditService {
        public V2ScoreDraftIntegrityAuditResult nextReturn = null;
        public MockAuditService() {
            super(null, null, null);
        }
        @Override
        public V2ScoreDraftIntegrityAuditResult audit(int userId, long submitRecordId) {
            return nextReturn;
        }
    }

    private static class MockOfficialResultDraftDAO extends V2OfficialResultDraftDAO {
        public boolean nextCreateReturn = true;
        public Optional<V2OfficialResultDraftRecord> nextFindBySubmitReturn = Optional.empty();
        
        @Override
        public boolean insertDraft(V2OfficialResultDraftRecord record) {
            return nextCreateReturn;
        }
        
        @Override
        public Optional<V2OfficialResultDraftRecord> findBySubmitRecordId(long submitRecordId) {
            return nextFindBySubmitReturn;
        }
    }

    @BeforeEach
    public void setUp() {
        mockAuditService = new MockAuditService();
        mockDao = new MockOfficialResultDraftDAO();

        System.setProperty("tse.v2.officialResultDraft.enabled", "true");

        service = new V2OfficialResultDraftService(
                new V2SubmitFeatureFlags(),
                mockAuditService,
                mockDao
        );
    }
    
    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.officialResultDraft.enabled");
    }

    private V2ScoreDraftIntegrityAuditResult createValidAuditResult(long submitRecordId) {
        V2ScoreDraftIntegrityAuditResult audit = new V2ScoreDraftIntegrityAuditResult();
        audit.setSuccess(true);
        audit.setReady(true);
        audit.setAuditStatus("SCORE_DRAFT_INTEGRITY_READY");
        audit.setSubmitRecordId(submitRecordId);
        audit.setUserId(1);
        audit.setScoreDraftId(10L);
        audit.setExamId(100);
        audit.setPaperId(200);
        audit.setAttemptId("att_123");
        audit.setPayloadHash("hash123");
        audit.setScoreDraftStatus("SCORE_DRAFTED_SERVER_SIDE");
        return audit;
    }

    @Test
    public void testCreateDraft_FeatureFlagDisabled_ShouldReject() throws Exception {
        System.setProperty("tse.v2.officialResultDraft.enabled", "false");

        V2OfficialResultDraftResult result = service.createDraft(1, 100L);

        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("FEATURE_DISABLED", result.getErrorCode());
        assertEquals("NOT_READY", result.getOfficialResultDraftStatus());
    }

    @Test
    public void testCreateDraft_AuditFails_ShouldReject() throws Exception {
        V2ScoreDraftIntegrityAuditResult failedAudit = new V2ScoreDraftIntegrityAuditResult();
        failedAudit.setSuccess(false);
        failedAudit.setErrorCode("SCORE_DRAFT_AUDIT_NOT_READY");
        mockAuditService.nextReturn = failedAudit;

        V2OfficialResultDraftResult result = service.createDraft(1, 100L);

        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("SCORE_DRAFT_AUDIT_NOT_READY", result.getErrorCode());
        assertEquals("NOT_READY", result.getOfficialResultDraftStatus());
    }

    @Test
    public void testCreateDraft_AlreadyExists_ShouldReturnExisting() throws Exception {
        mockAuditService.nextReturn = createValidAuditResult(100L);

        V2OfficialResultDraftRecord existingDraft = new V2OfficialResultDraftRecord();
        existingDraft.setId(55L);
        existingDraft.setOfficialResultDraftStatus("OFFICIAL_RESULT_DRAFTED_PENDING_PUBLICATION");
        mockDao.nextFindBySubmitReturn = Optional.of(existingDraft);

        V2OfficialResultDraftResult result = service.createDraft(1, 100L);

        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertNull(result.getErrorCode());
        assertEquals("OFFICIAL_RESULT_DRAFTED_PENDING_PUBLICATION", result.getOfficialResultDraftStatus());
        assertEquals(55L, result.getOfficialResultDraftId());
    }

    @Test
    public void testCreateDraft_DbInsertFails_ShouldReject() throws Exception {
        mockAuditService.nextReturn = createValidAuditResult(100L);
        mockDao.nextFindBySubmitReturn = Optional.empty();
        mockDao.nextCreateReturn = false;

        V2OfficialResultDraftResult result = service.createDraft(1, 100L);

        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("DB_INSERT_FAILED", result.getErrorCode());
        assertEquals("NOT_READY", result.getOfficialResultDraftStatus());
    }

    @Test
    public void testCreateDraft_Success() throws Exception {
        mockAuditService.nextReturn = createValidAuditResult(100L);
        mockDao.nextFindBySubmitReturn = Optional.empty();
        mockDao.nextCreateReturn = true;

        V2OfficialResultDraftResult result = service.createDraft(1, 100L);

        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertNull(result.getErrorCode());
        assertEquals("OFFICIAL_RESULT_DRAFTED_PENDING_PUBLICATION", result.getOfficialResultDraftStatus());
        
        assertEquals(10L, result.getScoreDraftId());
        assertEquals(100, result.getExamId());
        assertEquals(200, result.getPaperId());
        assertEquals("att_123", result.getAttemptId());
        assertEquals("hash123", result.getPayloadHash());
    }
}
