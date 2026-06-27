package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2OfficialResultDraftRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2ResultPublicationLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2ScoreDraftRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbe;
import com.mycompany.tutorhub_enterprise.server.dao.V2OfficialResultDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ResultPublicationLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ScoreDraftDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2FinalResultHandoffServiceTest {

    private V2FinalResultHandoffService service;
    private MockLedgerDAO mockLedgerDAO;
    private MockExamResultsProbe mockProbe;
    private MockOfficialResultDraftDAO mockOfficialDraftDAO;
    private MockScoreDraftDAO mockScoreDraftDAO;

    @BeforeEach
    public void setUp() {
        System.setProperty("tse.v2.finalResultHandoff.enabled", "true");

        mockLedgerDAO = new MockLedgerDAO();
        mockProbe = new MockExamResultsProbe();
        mockOfficialDraftDAO = new MockOfficialResultDraftDAO();
        mockScoreDraftDAO = new MockScoreDraftDAO();

        service = new V2FinalResultHandoffService(mockLedgerDAO, mockProbe, mockOfficialDraftDAO, mockScoreDraftDAO);
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.finalResultHandoff.enabled");
    }

    @Test
    public void testFeatureFlagOff_Rejects() {
        System.setProperty("tse.v2.finalResultHandoff.enabled", "false");
        V2FinalResultHandoffResult res = service.buildHandoff(1, 100L);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_FEATURE_DISABLED", res.getErrorCode());
    }

    @Test
    public void testLedgerMissing_Rejects() {
        mockLedgerDAO.record = null;
        V2FinalResultHandoffResult res = service.buildHandoff(1, 100L);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_LEDGER_MISSING", res.getErrorCode());
    }

    @Test
    public void testUserMismatch_Rejects() {
        V2ResultPublicationLedgerRecord ledger = new V2ResultPublicationLedgerRecord();
        ledger.setUserId(99);
        mockLedgerDAO.record = ledger;

        V2FinalResultHandoffResult res = service.buildHandoff(1, 100L);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_USER_MISMATCH", res.getErrorCode());
    }

    @Test
    public void testInvalidPublicationStatus_Rejects() {
        V2ResultPublicationLedgerRecord ledger = new V2ResultPublicationLedgerRecord();
        ledger.setUserId(1);
        ledger.setPublicationStatus("DRAFTED");
        mockLedgerDAO.record = ledger;

        V2FinalResultHandoffResult res = service.buildHandoff(1, 100L);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_INVALID_PUBLICATION_STATUS", res.getErrorCode());
    }

    @Test
    public void testExamResultsMissing_Rejects() {
        V2ResultPublicationLedgerRecord ledger = new V2ResultPublicationLedgerRecord();
        ledger.setUserId(1);
        ledger.setPublicationStatus("EXAM_RESULTS_WRITTEN");
        mockLedgerDAO.record = ledger;
        
        mockProbe.exists = false;

        V2FinalResultHandoffResult res = service.buildHandoff(1, 100L);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_EXAM_RESULTS_MISSING", res.getErrorCode());
    }

    @Test
    public void testOfficialDraftMissing_Rejects() {
        V2ResultPublicationLedgerRecord ledger = new V2ResultPublicationLedgerRecord();
        ledger.setUserId(1);
        ledger.setPublicationStatus("EXAM_RESULTS_WRITTEN");
        mockLedgerDAO.record = ledger;
        
        mockProbe.exists = true;
        mockOfficialDraftDAO.draft = null;

        V2FinalResultHandoffResult res = service.buildHandoff(1, 100L);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_OFFICIAL_DRAFT_MISSING", res.getErrorCode());
    }

    @Test
    public void testScoreDraftMissing_Rejects() {
        V2ResultPublicationLedgerRecord ledger = new V2ResultPublicationLedgerRecord();
        ledger.setUserId(1);
        ledger.setPublicationStatus("EXAM_RESULTS_WRITTEN");
        mockLedgerDAO.record = ledger;
        
        mockProbe.exists = true;
        mockOfficialDraftDAO.draft = new V2OfficialResultDraftRecord();
        mockScoreDraftDAO.draft = null;

        V2FinalResultHandoffResult res = service.buildHandoff(1, 100L);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_SCORE_DRAFT_MISSING", res.getErrorCode());
    }

    @Test
    public void testScoreMismatch_Rejects() {
        V2ResultPublicationLedgerRecord ledger = new V2ResultPublicationLedgerRecord();
        ledger.setUserId(1);
        ledger.setPublicationStatus("EXAM_RESULTS_WRITTEN");
        ledger.setRawScore(10.0);
        mockLedgerDAO.record = ledger;
        
        mockProbe.exists = true;
        
        V2OfficialResultDraftRecord official = new V2OfficialResultDraftRecord();
        official.setRawScore(9.0);
        mockOfficialDraftDAO.draft = official;
        
        V2ScoreDraftRecord score = new V2ScoreDraftRecord();
        score.setRawScore(10.0);
        mockScoreDraftDAO.draft = score;

        V2FinalResultHandoffResult res = service.buildHandoff(1, 100L);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_SCORE_MISMATCH", res.getErrorCode());
    }

    @Test
    public void testValidState_ReadyTrue() {
        V2ResultPublicationLedgerRecord ledger = new V2ResultPublicationLedgerRecord();
        ledger.setUserId(1);
        ledger.setPublicationStatus("EXAM_RESULTS_WRITTEN");
        ledger.setRawScore(10.0);
        ledger.setPayloadHash("hash");
        mockLedgerDAO.record = ledger;
        
        mockProbe.exists = true;
        
        V2OfficialResultDraftRecord official = new V2OfficialResultDraftRecord();
        official.setRawScore(10.0);
        official.setTotalQuestions(20);
        mockOfficialDraftDAO.draft = official;
        
        V2ScoreDraftRecord score = new V2ScoreDraftRecord();
        score.setRawScore(10.0);
        mockScoreDraftDAO.draft = score;

        V2FinalResultHandoffResult res = service.buildHandoff(1, 100L);
        assertTrue(res.isSuccess());
        assertTrue(res.isReady());
        assertEquals("FINAL_RESULT_HANDOFF_READY", res.getHandoffStatus());
        assertEquals("hash", res.getPayloadHash());
        assertEquals(20, res.getTotalQuestions());
        assertEquals(10.0, res.getRawScore());
    }

    // Mocks
    static class MockLedgerDAO extends V2ResultPublicationLedgerDAO {
        V2ResultPublicationLedgerRecord record;
        @Override
        public Optional<V2ResultPublicationLedgerRecord> findBySubmitRecordId(long submitRecordId) {
            return Optional.ofNullable(record);
        }
        @Override
        public boolean insertLedger(Connection conn, V2ResultPublicationLedgerRecord rec) {
            return false;
        }
    }

    static class MockExamResultsProbe implements V2ExamResultsReadOnlyProbe {
        boolean exists;
        @Override
        public boolean existsResultForAttempt(String attemptId) {
            return exists;
        }
    }

    static class MockOfficialResultDraftDAO extends V2OfficialResultDraftDAO {
        V2OfficialResultDraftRecord draft;
        @Override
        public Optional<V2OfficialResultDraftRecord> findBySubmitRecordId(long submitRecordId) {
            return Optional.ofNullable(draft);
        }
    }

    static class MockScoreDraftDAO extends V2ScoreDraftDAO {
        V2ScoreDraftRecord draft;
        @Override
        public Optional<V2ScoreDraftRecord> findBySubmitRecordId(long submitRecordId) {
            return Optional.ofNullable(draft);
        }
    }
}
