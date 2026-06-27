package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2OfficialResultDraftRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamAttemptStatusDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbe;
import com.mycompany.tutorhub_enterprise.server.dao.V2OfficialResultDraftDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2ResultPublicationReadinessServiceTest {

    private MockOfficialResultDraftDAO mockDraftDAO;
    private MockExamAttemptStatusDAO mockAttemptStatusDAO;
    private MockExamResultsReadOnlyProbe mockResultsProbe;
    private V2ResultPublicationReadinessService service;

    private static class MockOfficialResultDraftDAO extends V2OfficialResultDraftDAO {
        public Optional<V2OfficialResultDraftRecord> nextFindBySubmitReturn = Optional.empty();
        @Override
        public Optional<V2OfficialResultDraftRecord> findBySubmitRecordId(long submitRecordId) {
            return nextFindBySubmitReturn;
        }
    }

    private static class MockExamAttemptStatusDAO extends V2ExamAttemptStatusDAO {
        public Optional<String> nextFindAttemptStatusReturn = Optional.empty();
        @Override
        public Optional<String> findAttemptStatus(String attemptId) throws SQLException {
            return nextFindAttemptStatusReturn;
        }
    }

    private static class MockExamResultsReadOnlyProbe implements V2ExamResultsReadOnlyProbe {
        public boolean nextExistsResultReturn = false;
        @Override
        public boolean existsResultForAttempt(String attemptId) {
            return nextExistsResultReturn;
        }
    }

    @BeforeEach
    public void setUp() {
        mockDraftDAO = new MockOfficialResultDraftDAO();
        mockAttemptStatusDAO = new MockExamAttemptStatusDAO();
        mockResultsProbe = new MockExamResultsReadOnlyProbe();

        System.setProperty("tse.v2.resultPublicationReadiness.enabled", "true");

        service = new V2ResultPublicationReadinessService(
                new V2SubmitFeatureFlags(),
                mockDraftDAO,
                mockAttemptStatusDAO,
                mockResultsProbe
        );
    }
    
    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.resultPublicationReadiness.enabled");
    }

    private V2OfficialResultDraftRecord createValidDraft() {
        V2OfficialResultDraftRecord draft = new V2OfficialResultDraftRecord();
        draft.setId(1L);
        draft.setSubmitRecordId(100L);
        draft.setUserId(1);
        draft.setAttemptId("att_123");
        draft.setOfficialResultDraftStatus("OFFICIAL_RESULT_DRAFTED_PENDING_PUBLICATION");
        return draft;
    }

    @Test
    public void testCheckReadiness_FeatureFlagDisabled_ShouldReject() throws Exception {
        System.setProperty("tse.v2.resultPublicationReadiness.enabled", "false");

        V2ResultPublicationReadinessResult result = service.checkReadiness(1, 100L);

        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("FEATURE_DISABLED", result.getErrorCode());
        assertEquals("NOT_READY", result.getPublicationReadinessStatus());
    }

    @Test
    public void testCheckReadiness_DraftMissing_ShouldReject() throws Exception {
        mockDraftDAO.nextFindBySubmitReturn = Optional.empty();

        V2ResultPublicationReadinessResult result = service.checkReadiness(1, 100L);

        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("OFFICIAL_RESULT_DRAFT_NOT_FOUND", result.getErrorCode());
        assertEquals("NOT_READY", result.getPublicationReadinessStatus());
    }

    @Test
    public void testCheckReadiness_UserMismatch_ShouldReject() throws Exception {
        V2OfficialResultDraftRecord draft = createValidDraft();
        draft.setUserId(99);
        mockDraftDAO.nextFindBySubmitReturn = Optional.of(draft);

        V2ResultPublicationReadinessResult result = service.checkReadiness(1, 100L);

        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("USER_MISMATCH", result.getErrorCode());
        assertEquals("NOT_READY", result.getPublicationReadinessStatus());
    }

    @Test
    public void testCheckReadiness_DraftStatusInvalid_ShouldReject() throws Exception {
        V2OfficialResultDraftRecord draft = createValidDraft();
        draft.setOfficialResultDraftStatus("INVALID_STATUS");
        mockDraftDAO.nextFindBySubmitReturn = Optional.of(draft);

        V2ResultPublicationReadinessResult result = service.checkReadiness(1, 100L);

        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("INVALID_DRAFT_STATUS", result.getErrorCode());
        assertEquals("NOT_READY", result.getPublicationReadinessStatus());
    }

    @Test
    public void testCheckReadiness_AttemptStatusNotSubmitted_ShouldReject() throws Exception {
        mockDraftDAO.nextFindBySubmitReturn = Optional.of(createValidDraft());
        mockAttemptStatusDAO.nextFindAttemptStatusReturn = Optional.of("IN_PROGRESS");

        V2ResultPublicationReadinessResult result = service.checkReadiness(1, 100L);

        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("INVALID_ATTEMPT_STATUS", result.getErrorCode());
        assertEquals("NOT_READY", result.getPublicationReadinessStatus());
    }

    @Test
    public void testCheckReadiness_ExistingExamResultsFound_ShouldReject() throws Exception {
        mockDraftDAO.nextFindBySubmitReturn = Optional.of(createValidDraft());
        mockAttemptStatusDAO.nextFindAttemptStatusReturn = Optional.of("SUBMITTED");
        mockResultsProbe.nextExistsResultReturn = true;

        V2ResultPublicationReadinessResult result = service.checkReadiness(1, 100L);

        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("EXAM_RESULTS_ALREADY_EXIST", result.getErrorCode());
        assertEquals("NOT_READY", result.getPublicationReadinessStatus());
    }

    @Test
    public void testCheckReadiness_ValidAndNoResults_ShouldBeReady() throws Exception {
        mockDraftDAO.nextFindBySubmitReturn = Optional.of(createValidDraft());
        mockAttemptStatusDAO.nextFindAttemptStatusReturn = Optional.of("SUBMITTED");
        mockResultsProbe.nextExistsResultReturn = false;

        V2ResultPublicationReadinessResult result = service.checkReadiness(1, 100L);

        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertNull(result.getErrorCode());
        assertEquals("READY_FOR_EXAM_RESULTS_WRITE_DRAFT", result.getPublicationReadinessStatus());
        assertEquals("SUBMITTED", result.getAttemptStatus());
    }
}
