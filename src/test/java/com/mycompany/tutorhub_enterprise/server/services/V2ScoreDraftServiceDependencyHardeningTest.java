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

public class V2ScoreDraftServiceDependencyHardeningTest {

    private FakePreflightService fakePreflightService;
    private FakeSubmitRecordDAO fakeSubmitRecordDAO;
    private FakeScoreDraftDAO fakeScoreDraftDAO;
    private FakeAnswerKeyResolver fakeResolver;
    private FakePayloadParser fakeParser;
    private V2ScoreDraftService service;

    private static class FakePreflightService extends V2GradingPreflightService {
        public V2GradingPreflightResult returnResult = new V2GradingPreflightResult();
        @Override
        public V2GradingPreflightResult checkPreflight(int userId, long submitRecordId) {
            return returnResult;
        }
    }

    private static class FakeSubmitRecordDAO extends V2SubmitRecordDAO {
        public Optional<V2SubmitRecord> returnOpt = Optional.empty();
        @Override
        public Optional<V2SubmitRecord> findById(long submitRecordId) {
            return returnOpt;
        }
    }

    private static class FakeScoreDraftDAO extends V2ScoreDraftDAO {
        public Optional<V2ScoreDraftRecord> returnOpt = Optional.empty();
        public boolean insertReturn = true;
        @Override
        public void ensureTableExists() { }
        @Override
        public Optional<V2ScoreDraftRecord> findBySubmitRecordId(long submitRecordId) {
            return returnOpt;
        }
        @Override
        public boolean insert(V2ScoreDraftRecord record) {
            record.setId(100L);
            return insertReturn;
        }
    }

    private static class FakeAnswerKeyResolver implements V2AnswerKeyResolver {
        public Map<Long, Long> returnMap = new HashMap<>();
        @Override
        public Map<Long, Long> resolveCorrectOptionIds(int paperId) {
            return returnMap;
        }
    }

    private static class FakePayloadParser implements V2AnswerPayloadParser {
        public Map<Long, Long> returnMap = new HashMap<>();
        @Override
        public Map<Long, Long> extractAnswers(String payloadJson) {
            return returnMap;
        }
    }

    @BeforeEach
    public void setup() {
        fakePreflightService = new FakePreflightService();
        fakeSubmitRecordDAO = new FakeSubmitRecordDAO();
        fakeScoreDraftDAO = new FakeScoreDraftDAO();
        fakeResolver = new FakeAnswerKeyResolver();
        fakeParser = new FakePayloadParser();
        service = new V2ScoreDraftService(fakePreflightService, fakeSubmitRecordDAO, fakeScoreDraftDAO, fakeResolver, fakeParser);
        System.setProperty("tse.v2.scoreDraft.enabled", "true");
        
        V2GradingPreflightResult preflightResult = new V2GradingPreflightResult();
        preflightResult.setSuccess(true);
        preflightResult.setReady(true);
        preflightResult.setExamId(10);
        preflightResult.setPaperId(20);
        preflightResult.setAttemptId("attempt-1");
        fakePreflightService.returnResult = preflightResult;

        V2SubmitRecord submitRecord = new V2SubmitRecord();
        submitRecord.setExamId(10);
        submitRecord.setPaperId(20);
        submitRecord.setAttemptId("attempt-1");
        submitRecord.setPayloadJson("{}");
        fakeSubmitRecordDAO.returnOpt = Optional.of(submitRecord);
    }

    @AfterEach
    public void teardown() {
        System.clearProperty("tse.v2.scoreDraft.enabled");
    }

    @Test
    public void testParserNullDependency() {
        service = new V2ScoreDraftService(fakePreflightService, fakeSubmitRecordDAO, fakeScoreDraftDAO, fakeResolver, null);
        V2ScoreDraftResult result = service.createScoreDraft(1, 10L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_SCORE_DRAFT_PAYLOAD_PARSER_UNAVAILABLE", result.getErrorCode());
    }

    @Test
    public void testResolverNullDependency() {
        service = new V2ScoreDraftService(fakePreflightService, fakeSubmitRecordDAO, fakeScoreDraftDAO, null, fakeParser);
        V2ScoreDraftResult result = service.createScoreDraft(1, 10L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_SCORE_DRAFT_ANSWER_KEY_RESOLVER_UNAVAILABLE", result.getErrorCode());
    }

    @Test
    public void testParserReturnsNull() {
        fakeParser.returnMap = null;
        V2ScoreDraftResult result = service.createScoreDraft(1, 10L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_SCORE_DRAFT_PAYLOAD_PARSER_UNAVAILABLE", result.getErrorCode());
    }

    @Test
    public void testResolverReturnsNull() {
        fakeResolver.returnMap = null;
        V2ScoreDraftResult result = service.createScoreDraft(1, 10L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_SCORE_DRAFT_ANSWER_KEY_RESOLVER_UNAVAILABLE", result.getErrorCode());
    }

    @Test
    public void testValidFakePayloadAndFakeAnswerKeyComputesScoreDraft() {
        Map<Long, Long> payloadAnswers = new HashMap<>();
        payloadAnswers.put(101L, 201L); // Correct
        payloadAnswers.put(102L, 202L); // Correct
        payloadAnswers.put(103L, 204L); // Incorrect (correct is 203)
        fakeParser.returnMap = payloadAnswers;

        Map<Long, Long> correctKeys = new HashMap<>();
        correctKeys.put(101L, 201L);
        correctKeys.put(102L, 202L);
        correctKeys.put(103L, 203L);
        correctKeys.put(104L, 205L); // Unanswered
        fakeResolver.returnMap = correctKeys;

        V2ScoreDraftResult result = service.createScoreDraft(1, 10L);
        
        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertEquals(4, result.getTotalQuestions());
        assertEquals(3, result.getAnsweredQuestions());
        assertEquals(1, result.getUnansweredQuestions());
        assertEquals(2, result.getCorrectCount());
        assertEquals(1, result.getIncorrectCount());
        assertEquals(2.0, result.getRawScore());
        assertEquals(4.0, result.getMaxScore());
        assertEquals(50.0, result.getPercentage());
    }
}
