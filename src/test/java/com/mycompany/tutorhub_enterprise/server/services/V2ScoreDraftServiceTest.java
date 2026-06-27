package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2ScoreDraftRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ScoreDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2ScoreDraftServiceTest {

    private V2ScoreDraftService service;
    private MockPreflightService mockPreflightService;
    private MockSubmitRecordDAO mockSubmitRecordDAO;
    private MockScoreDraftDAO mockScoreDraftDAO;
    private MockAnswerKeyResolver mockAnswerKeyResolver;
    private MockAnswerPayloadParser mockPayloadParser;

    @BeforeEach
    public void setUp() {
        mockPreflightService = new MockPreflightService();
        mockSubmitRecordDAO = new MockSubmitRecordDAO();
        mockScoreDraftDAO = new MockScoreDraftDAO();
        mockAnswerKeyResolver = new MockAnswerKeyResolver();
        mockPayloadParser = new MockAnswerPayloadParser();
        service = new V2ScoreDraftService(mockPreflightService, mockSubmitRecordDAO, mockScoreDraftDAO, mockAnswerKeyResolver, mockPayloadParser);
        System.setProperty("tse.v2.scoreDraft.enabled", "true");
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.scoreDraft.enabled");
    }

    @Test
    public void testFeatureFlagDisabled() {
        System.setProperty("tse.v2.scoreDraft.enabled", "false");
        V2ScoreDraftResult result = service.createScoreDraft(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testPreflightNotReady() {
        V2GradingPreflightResult preflightRes = new V2GradingPreflightResult();
        preflightRes.setSuccess(false);
        mockPreflightService.resultToReturn = preflightRes;

        V2ScoreDraftResult result = service.createScoreDraft(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_SCORE_DRAFT_PREFLIGHT_NOT_READY", result.getErrorCode());
    }

    @Test
    public void testIdempotentExistingDraft() {
        mockPreflightService.resultToReturn = createValidPreflightResult();
        
        V2ScoreDraftRecord existing = new V2ScoreDraftRecord();
        existing.setId(555L);
        existing.setTotalQuestions(10);
        existing.setRawScore(8.0);
        mockScoreDraftDAO.recordToReturn = existing;

        V2ScoreDraftResult result = service.createScoreDraft(1, 100L);
        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertEquals(555L, result.getScoreDraftId());
        assertEquals("SCORE_DRAFTED_SERVER_SIDE", result.getScoreDraftStatus());
        assertEquals(10, result.getTotalQuestions());
        assertEquals(8.0, result.getRawScore());
        assertTrue(result.getWarnings().get(0).contains("already exists"));
    }

    @Test
    public void testCalculation() {
        mockPreflightService.resultToReturn = createValidPreflightResult();
        mockScoreDraftDAO.recordToReturn = null;
        
        V2SubmitRecord submitRecord = new V2SubmitRecord();
        submitRecord.setPayloadJson("{}");
        submitRecord.setExamId(10);
        submitRecord.setPaperId(20);
        submitRecord.setAttemptId("ATTEMPT_1");
        mockSubmitRecordDAO.recordToReturn = submitRecord;

        Map<Long, Long> answers = new HashMap<>();
        answers.put(1L, 101L); // correct
        answers.put(2L, 102L); // incorrect
        // question 3 unanswered
        mockPayloadParser.mapToReturn = answers;

        Map<Long, Long> correctOptions = new HashMap<>();
        correctOptions.put(1L, 101L);
        correctOptions.put(2L, 999L);
        correctOptions.put(3L, 103L);
        mockAnswerKeyResolver.mapToReturn = correctOptions;

        V2ScoreDraftResult result = service.createScoreDraft(1, 100L);
        
        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertEquals(3, result.getTotalQuestions());
        assertEquals(2, result.getAnsweredQuestions());
        assertEquals(1, result.getUnansweredQuestions());
        assertEquals(1, result.getCorrectCount());
        assertEquals(1, result.getIncorrectCount());
        assertEquals(1.0, result.getRawScore());
        assertEquals(3.0, result.getMaxScore());
        assertEquals((1.0/3.0)*100, result.getPercentage(), 0.01);
        
        // Ensure no sensitive fields leak
        // (By design, V2ScoreDraftResult does not have these fields, but we assert what it does have)
        assertNotNull(result.getScoreDraftStatus());
    }

    private V2GradingPreflightResult createValidPreflightResult() {
        V2GradingPreflightResult r = new V2GradingPreflightResult();
        r.setSuccess(true);
        r.setReady(true);
        r.setExamId(10);
        r.setPaperId(20);
        r.setAttemptId("ATTEMPT_1");
        r.setPayloadHash("hash");
        return r;
    }

    // Mocks
    private static class MockPreflightService extends V2GradingPreflightService {
        V2GradingPreflightResult resultToReturn;
        @Override
        public V2GradingPreflightResult checkPreflight(int userId, long submitRecordId) {
            return resultToReturn;
        }
    }

    private static class MockSubmitRecordDAO extends V2SubmitRecordDAO {
        V2SubmitRecord recordToReturn;
        @Override
        public Optional<V2SubmitRecord> findById(long id) {
            return Optional.ofNullable(recordToReturn);
        }
    }

    private static class MockScoreDraftDAO extends V2ScoreDraftDAO {
        V2ScoreDraftRecord recordToReturn;
        @Override
        public void ensureTableExists() {}
        @Override
        public Optional<V2ScoreDraftRecord> findBySubmitRecordId(long submitRecordId) {
            return Optional.ofNullable(recordToReturn);
        }
        @Override
        public boolean insert(V2ScoreDraftRecord record) {
            record.setId(777L);
            return true;
        }
    }

    private static class MockAnswerKeyResolver implements V2AnswerKeyResolver {
        Map<Long, Long> mapToReturn;
        @Override
        public Map<Long, Long> resolveCorrectOptionIds(int paperId) {
            return mapToReturn;
        }
    }

    private static class MockAnswerPayloadParser implements V2AnswerPayloadParser {
        Map<Long, Long> mapToReturn;
        @Override
        public Map<Long, Long> extractAnswers(String payloadJson) {
            return mapToReturn;
        }
    }
}
