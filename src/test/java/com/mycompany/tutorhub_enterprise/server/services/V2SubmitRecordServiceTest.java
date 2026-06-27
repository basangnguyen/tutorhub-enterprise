package com.mycompany.tutorhub_enterprise.server.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mycompany.tutorhub_enterprise.client.exam.ui.TSEV2SubmitAnswerItem;
import com.mycompany.tutorhub_enterprise.client.exam.ui.TSEV2SubmitDryRunMeta;
import com.mycompany.tutorhub_enterprise.client.exam.ui.TSEV2SubmitPayload;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;



public class V2SubmitRecordServiceTest {

    private V2SubmitRecordService service;
    private MockV2SubmitRecordDAO mockDAO;
    private MockV2SubmitDryRunValidationService mockValidationService;

    @BeforeEach
    public void setUp() {
        System.setProperty("tse.v2.submitRecord.enabled", "true");
        mockDAO = new MockV2SubmitRecordDAO();
        mockValidationService = new MockV2SubmitDryRunValidationService();
        service = new V2SubmitRecordService(mockValidationService, mockDAO);
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.submitRecord.enabled");
    }

    @Test
    public void testFeatureFlagOff() {
        System.setProperty("tse.v2.submitRecord.enabled", "false");
        TSEV2SubmitPayload payload = createValidPayload();
        V2SubmitRecordResult res = service.createSubmitRecord(1, payload);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_FEATURE_DISABLED", res.getErrorCode());
        assertEquals(0, mockDAO.getInsertedCount());
    }

    @Test
    public void testValidationFail() {
        V2SubmitDryRunValidationResult failResult = new V2SubmitDryRunValidationResult();
        failResult.setSuccess(false);
        failResult.setErrorCode("ERROR_INVALID_ATTEMPT");
        mockValidationService.setNextResult(failResult);
        TSEV2SubmitPayload payload = createValidPayload();
        V2SubmitRecordResult res = service.createSubmitRecord(1, payload);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_INVALID_ATTEMPT", res.getErrorCode());
        assertEquals(0, mockDAO.getInsertedCount());
    }

    @Test
    public void testInvalidPayloadHash() {
        TSEV2SubmitPayload payload = createValidPayload();
        payload.setPayloadHash("invalid-hash"); // Not 64 chars hex
        V2SubmitRecordResult res = service.createSubmitRecord(1, payload);
        assertFalse(res.isSuccess());
        assertEquals("ERROR_V2_SUBMIT_RECORD_PAYLOAD_HASH_INVALID", res.getErrorCode());
        assertEquals(0, mockDAO.getInsertedCount());
    }

    @Test
    public void testValidPayloadInsert() throws SQLException {
        TSEV2SubmitPayload payload = createValidPayload();
        V2SubmitRecordResult res = service.createSubmitRecord(1, payload);
        assertTrue(res.isSuccess());
        assertEquals(1, mockDAO.getInsertedCount());

        V2SubmitRecord stored = mockDAO.findById(res.getSubmitRecordId()).get();
        assertEquals("RECEIVED_DEBUG", stored.getSubmitStatus());
        assertEquals("V2_DEBUG", stored.getSource());
        assertEquals(payload.getPayloadHash(), stored.getPayloadHash());

        String json = stored.getPayloadJson();
        String lowerJson = json.toLowerCase();
        assertFalse(lowerJson.contains("sessiontoken"), "Should not contain sessionToken");
        assertFalse(lowerJson.contains("keyb64"), "Should not contain keyB64");
        assertFalse(lowerJson.contains("plaintext"), "Should not contain plaintext");
        assertFalse(lowerJson.contains("answerkey"), "Should not contain answerKey");
        assertFalse(lowerJson.contains("iscorrect"), "Should not contain isCorrect");
        assertFalse(lowerJson.contains("correctoption"), "Should not contain correctOption");
        assertFalse(lowerJson.contains("password"), "Should not contain password");
        assertFalse(lowerJson.contains("score"), "Should not contain score");
        assertFalse(lowerJson.contains("gradingresult"), "Should not contain gradingResult");
        
        Gson gson = new GsonBuilder().create();
        String resJson = gson.toJson(res);
        assertFalse(resJson.contains("answers"), "Result DTO should not contain answers");
        assertFalse(resJson.contains("selectedOptionId"), "Result DTO should not contain selectedOptionId");
    }
    
    @Test
    public void testDuplicatePayloadHash() {
        TSEV2SubmitPayload payload = createValidPayload();
        V2SubmitRecordResult res1 = service.createSubmitRecord(1, payload);
        assertTrue(res1.isSuccess());
        
        V2SubmitRecordResult res2 = service.createSubmitRecord(1, payload);
        assertTrue(res2.isSuccess());
        assertEquals(1, mockDAO.getInsertedCount(), "Should allow duplicate debug but not insert new row or return success cleanly");
    }

    private TSEV2SubmitPayload createValidPayload() {
        TSEV2SubmitPayload p = new TSEV2SubmitPayload();
        p.setExamId(10);
        p.setPaperId(20);
        p.setAttemptId("test-attempt");
        p.setPackageHash("package-hash");
        p.setPayloadHash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        p.setComplete(true);
        p.setAnsweredCount(5);
        p.setUnansweredCount(0);
        
        List<TSEV2SubmitAnswerItem> answers = new ArrayList<>();
        TSEV2SubmitAnswerItem a1 = new TSEV2SubmitAnswerItem();
        a1.setQuestionId(1);
        answers.add(a1);
        p.setAnswers(answers);
        return p;
    }

    // Mock DAO to prevent real DB hits
    private static class MockV2SubmitRecordDAO extends V2SubmitRecordDAO {
        private final Map<Long, V2SubmitRecord> storage = new HashMap<>();
        private long idCounter = 1;

        @Override
        public void ensureSchema() {}

        @Override
        public long insertSubmitRecord(V2SubmitRecord record) {
            record.setId(idCounter);
            storage.put(idCounter, record);
            idCounter++;
            return record.getId();
        }

        @Override
        public Optional<V2SubmitRecord> findById(long id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public Optional<V2SubmitRecord> findLatestByAttemptId(String attemptId) {
            return storage.values().stream()
                    .filter(r -> attemptId.equals(r.getAttemptId()))
                    .reduce((first, second) -> second);
        }

        @Override
        public boolean existsByPayloadHash(String payloadHash) {
            return storage.values().stream()
                    .anyMatch(r -> payloadHash.equals(r.getPayloadHash()));
        }

        public int getInsertedCount() {
            return storage.size();
        }
    }

    // Mock Validation Service
    private static class MockV2SubmitDryRunValidationService extends V2SubmitDryRunValidationService {
        private V2SubmitDryRunValidationResult nextResult;
        
        public MockV2SubmitDryRunValidationService() {
            nextResult = new V2SubmitDryRunValidationResult();
            nextResult.setSuccess(true);
        }

        public void setNextResult(V2SubmitDryRunValidationResult result) {
            this.nextResult = result;
        }

        @Override
        public V2SubmitDryRunValidationResult validateDryRun(int userId, TSEV2SubmitPayload payload) {
            return nextResult;
        }
    }
}
