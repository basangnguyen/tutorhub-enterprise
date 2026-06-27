package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2GradingPreflightServiceTest {

    private V2GradingPreflightService service;
    private MockAuditService mockAuditService;
    private MockSubmitRecordDAO mockSubmitRecordDAO;
    private MockAnswerKeyResolver mockAnswerKeyResolver;
    private MockAnswerPayloadParser mockPayloadParser;

    @BeforeEach
    public void setUp() {
        mockAuditService = new MockAuditService();
        mockSubmitRecordDAO = new MockSubmitRecordDAO();
        mockAnswerKeyResolver = new MockAnswerKeyResolver();
        mockPayloadParser = new MockAnswerPayloadParser();
        service = new V2GradingPreflightService(mockAuditService, mockSubmitRecordDAO, mockAnswerKeyResolver, mockPayloadParser);
        System.setProperty("tse.v2.gradingPreflight.enabled", "true");
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.gradingPreflight.enabled");
    }

    @Test
    public void testFeatureFlagDisabled() {
        System.setProperty("tse.v2.gradingPreflight.enabled", "false");
        V2GradingPreflightResult result = service.checkPreflight(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testAuditNotReady() {
        V2PostSubmitIntegrityAuditResult auditRes = new V2PostSubmitIntegrityAuditResult();
        auditRes.setSuccess(false);
        mockAuditService.resultToReturn = auditRes;

        V2GradingPreflightResult result = service.checkPreflight(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_GRADING_PREFLIGHT_AUDIT_NOT_READY", result.getErrorCode());
    }

    @Test
    public void testMissingResolver() {
        mockAuditService.resultToReturn = createValidAuditResult();
        service = new V2GradingPreflightService(mockAuditService, mockSubmitRecordDAO, null, mockPayloadParser);

        V2GradingPreflightResult result = service.checkPreflight(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_GRADING_PREFLIGHT_MISSING_RESOLVER", result.getErrorCode());
    }

    @Test
    public void testMissingParser() {
        mockAuditService.resultToReturn = createValidAuditResult();
        service = new V2GradingPreflightService(mockAuditService, mockSubmitRecordDAO, mockAnswerKeyResolver, null);

        V2GradingPreflightResult result = service.checkPreflight(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_GRADING_PREFLIGHT_MISSING_PARSER", result.getErrorCode());
    }

    @Test
    public void testMissingSubmitRecord() {
        mockAuditService.resultToReturn = createValidAuditResult();
        mockSubmitRecordDAO.recordToReturn = null;

        V2GradingPreflightResult result = service.checkPreflight(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_GRADING_PREFLIGHT_MISSING_SUBMIT_RECORD", result.getErrorCode());
    }

    @Test
    public void testEmptyPayload() {
        mockAuditService.resultToReturn = createValidAuditResult();
        V2SubmitRecord record = new V2SubmitRecord();
        record.setPayloadJson(null);
        mockSubmitRecordDAO.recordToReturn = record;

        V2GradingPreflightResult result = service.checkPreflight(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_GRADING_PREFLIGHT_MISSING_PAYLOAD", result.getErrorCode());
    }

    @Test
    public void testInvalidPayload() {
        mockAuditService.resultToReturn = createValidAuditResult();
        V2SubmitRecord record = new V2SubmitRecord();
        record.setPayloadJson("invalid");
        mockSubmitRecordDAO.recordToReturn = record;
        mockPayloadParser.throwException = true;

        V2GradingPreflightResult result = service.checkPreflight(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_GRADING_PREFLIGHT_INVALID_PAYLOAD", result.getErrorCode());
    }

    @Test
    public void testMissingAnswerKey() {
        mockAuditService.resultToReturn = createValidAuditResult();
        V2SubmitRecord record = new V2SubmitRecord();
        record.setPayloadJson("{}");
        mockSubmitRecordDAO.recordToReturn = record;
        mockAnswerKeyResolver.mapToReturn = new HashMap<>(); // empty

        V2GradingPreflightResult result = service.checkPreflight(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_GRADING_PREFLIGHT_MISSING_ANSWER_KEY", result.getErrorCode());
    }

    @Test
    public void testValidPreflight() {
        mockAuditService.resultToReturn = createValidAuditResult();
        V2SubmitRecord record = new V2SubmitRecord();
        record.setPayloadJson("{\"1\":2}");
        mockSubmitRecordDAO.recordToReturn = record;

        Map<Long, Long> answers = new HashMap<>();
        answers.put(1L, 2L);
        mockPayloadParser.mapToReturn = answers;

        Map<Long, Long> correctOptions = new HashMap<>();
        correctOptions.put(1L, 3L);
        correctOptions.put(2L, 4L);
        mockAnswerKeyResolver.mapToReturn = correctOptions;

        V2GradingPreflightResult result = service.checkPreflight(1, 100L);
        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertEquals("READY_FOR_SCORE_DRAFT", result.getGradingPreflightStatus());
        assertEquals(1, result.getAnswerCount());
        assertEquals(2, result.getQuestionCount());
    }

    private V2PostSubmitIntegrityAuditResult createValidAuditResult() {
        V2PostSubmitIntegrityAuditResult r = new V2PostSubmitIntegrityAuditResult();
        r.setSuccess(true);
        r.setReady(true);
        return r;
    }

    // Mocks
    private static class MockAuditService extends V2PostSubmitIntegrityAuditService {
        V2PostSubmitIntegrityAuditResult resultToReturn;
        @Override
        public V2PostSubmitIntegrityAuditResult audit(int userId, long submitRecordId) {
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

    private static class MockAnswerKeyResolver implements V2AnswerKeyResolver {
        Map<Long, Long> mapToReturn;
        @Override
        public Map<Long, Long> resolveCorrectOptionIds(int paperId) {
            return mapToReturn;
        }
    }

    private static class MockAnswerPayloadParser implements V2AnswerPayloadParser {
        Map<Long, Long> mapToReturn;
        boolean throwException = false;
        @Override
        public Map<Long, Long> extractAnswers(String payloadJson) {
            if (throwException) throw new RuntimeException("Parse error");
            return mapToReturn;
        }
    }
}
