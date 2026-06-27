package com.mycompany.tutorhub_enterprise.server.services;

import com.google.gson.Gson;
import com.mycompany.tutorhub_enterprise.client.exam.ui.TSEV2SubmitAnswerItem;
import com.mycompany.tutorhub_enterprise.client.exam.ui.TSEV2SubmitPayload;
import com.mycompany.tutorhub_enterprise.client.exam.ui.TSEV2SubmitPayloadService;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitDryRunRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitDryRunPayloadDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class V2SubmitDryRunPersistenceRegressionTest {

    private static final int USER_ID = 88;

    private FakeValidationService validationService;
    private InMemoryRegressionDAO dao;
    private V2SubmitDryRunPersistenceService service;
    private final Gson gson = new Gson();

    @BeforeEach
    public void setUp() {
        System.clearProperty(V2SubmitDryRunPersistenceService.FEATURE_FLAG);
        validationService = new FakeValidationService();
        dao = new InMemoryRegressionDAO();
        service = new V2SubmitDryRunPersistenceService(validationService, dao, new TSEV2SubmitPayloadService());
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(V2SubmitDryRunPersistenceService.FEATURE_FLAG);
    }

    @Test
    public void regression_flagOffRejectsWithoutDbWrite() {
        TSEV2SubmitPayload payload = validPayload();
        V2SubmitDryRunPersistenceResult result = service.persistDryRun(USER_ID, payload);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
        assertEquals(0, dao.insertCount);
    }

    @Test
    public void regression_validationFailureRejectsWithoutDbWrite() {
        enableFeature();
        validationService.nextResult = validationError("ERROR_V2_SUBMIT_QUESTION_COUNT_MISMATCH");
        V2SubmitDryRunPersistenceResult result = service.persistDryRun(USER_ID, validPayload());
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_SUBMIT_QUESTION_COUNT_MISMATCH", result.getErrorCode());
        assertEquals(0, dao.insertCount);
    }

    @Test
    public void regression_validPayloadInsertsDryRunRecord() {
        enableFeature();
        TSEV2SubmitPayload payload = validPayload();
        V2SubmitDryRunPersistenceResult result = service.persistDryRun(USER_ID, payload);
        assertTrue(result.isSuccess());
        assertEquals("EXAM_SUBMIT_V2_DRYRUN_PERSIST_OK", result.getErrorCode());
        assertEquals(1, dao.insertCount);
    }

    @Test
    public void regression_readBackRecordPayloadHashMatches() {
        enableFeature();
        TSEV2SubmitPayload payload = validPayload();
        service.persistDryRun(USER_ID, payload);
        V2SubmitDryRunRecord stored = dao.lastRecord;
        assertNotNull(stored);
        assertEquals(payload.getPayloadHash(), stored.getPayloadHash());
    }

    @Test
    public void regression_storedPayloadJsonDoesNotContainKeyMaterialOrPlaintextMarkers() {
        enableFeature();
        service.persistDryRun(USER_ID, validPayload());
        String json = dao.lastRecord.getPayloadJson();
        assertFalse(json.contains("sessionToken"));
        assertFalse(json.contains("keyB64"));
        assertFalse(json.contains("plaintextJson"));
        assertFalse(json.contains("plaintext"));
    }

    @Test
    public void regression_storedPayloadJsonDoesNotContainCorrectnessMarkers() {
        enableFeature();
        service.persistDryRun(USER_ID, validPayload());
        String json = dao.lastRecord.getPayloadJson();
        assertFalse(json.contains("answerKey"));
        assertFalse(json.contains("isCorrect"));
        assertFalse(json.contains("correctOption"));
    }

    @Test
    public void regression_storedPayloadJsonDoesNotContainPasswordMarkers() {
        enableFeature();
        service.persistDryRun(USER_ID, validPayload());
        String json = dao.lastRecord.getPayloadJson();
        assertFalse(json.contains("password"));
        assertFalse(json.contains("passwordHash"));
    }

    @Test
    public void regression_storedPayloadJsonDoesNotContainScoringMarkers() {
        enableFeature();
        service.persistDryRun(USER_ID, validPayload());
        String json = dao.lastRecord.getPayloadJson();
        assertFalse(json.contains("score"));
        assertFalse(json.contains("gradingResult"));
    }

    @Test
    public void regression_resultDtoDoesNotExposeAnswersOrSelectedOptionIds() {
        enableFeature();
        V2SubmitDryRunPersistenceResult result = service.persistDryRun(USER_ID, validPayload());
        String json = gson.toJson(result);
        assertFalse(json.contains("answers"));
        assertFalse(json.contains("selectedOptionId"));
    }

    @Test
    public void regression_persistenceDoesNotWriteExamResults() {
        enableFeature();
        service.persistDryRun(USER_ID, validPayload());
        assertEquals(1, dao.insertCount);
        assertEquals(0, dao.examResultsWriteCount);
    }

    @Test
    public void regression_persistenceDoesNotMarkAttemptSubmitted() {
        enableFeature();
        service.persistDryRun(USER_ID, validPayload());
        assertEquals(1, dao.insertCount);
        assertEquals(0, dao.attemptSubmittedUpdateCount);
    }

    @Test
    public void regression_persistenceDoesNotCallLegacySubmitPath() {
        enableFeature();
        service.persistDryRun(USER_ID, validPayload());
        assertEquals(1, dao.insertCount);
        assertEquals(0, dao.legacySubmitCallCount);
    }

    private void enableFeature() {
        System.setProperty(V2SubmitDryRunPersistenceService.FEATURE_FLAG, "true");
    }

    private TSEV2SubmitPayload validPayload() {
        TSEV2SubmitPayload payload = new TSEV2SubmitPayload();
        payload.setPayloadVersion("1.0");
        payload.setFlow("PAPER_START_V2");
        payload.setExamId(10);
        payload.setPaperId(20);
        payload.setAttemptId("attempt-phase-7f");
        payload.setPackageHash("package-hash-7f");
        payload.setQuestionCount(2);
        payload.setAnsweredCount(2);
        payload.setUnansweredCount(0);
        payload.setComplete(true);
        payload.setDraftSnapshotHash("draft-hash-7f");
        payload.setPayloadHash("payload-hash-7f");
        payload.setPreparedAt("2026-06-19T00:00:00Z");

        List<TSEV2SubmitAnswerItem> answers = new ArrayList<>();
        answers.add(new TSEV2SubmitAnswerItem(101, 1001, "2026-06-19T00:00:01Z"));
        answers.add(new TSEV2SubmitAnswerItem(102, 1002, "2026-06-19T00:00:02Z"));
        payload.setAnswers(answers);
        return payload;
    }

    private V2SubmitDryRunValidationResult validationOk(TSEV2SubmitPayload payload) {
        V2SubmitDryRunValidationResult result = new V2SubmitDryRunValidationResult();
        result.setSuccess(true);
        result.setErrorCode("EXAM_SUBMIT_V2_DRYRUN_VALIDATE_OK");
        result.setExamId(payload.getExamId());
        result.setPaperId(payload.getPaperId());
        result.setAttemptId(payload.getAttemptId());
        result.setAnsweredCount(payload.getAnsweredCount());
        result.setUnansweredCount(payload.getUnansweredCount());
        result.setComplete(payload.isComplete());
        result.setPayloadHash(payload.getPayloadHash());
        return result;
    }

    private V2SubmitDryRunValidationResult validationError(String errorCode) {
        V2SubmitDryRunValidationResult result = new V2SubmitDryRunValidationResult();
        result.setSuccess(false);
        result.setErrorCode(errorCode);
        return result;
    }

    private class FakeValidationService extends V2SubmitDryRunValidationService {
        private V2SubmitDryRunValidationResult nextResult;

        @Override
        public V2SubmitDryRunValidationResult validateDryRun(int userId, TSEV2SubmitPayload payload) {
            if (nextResult != null) {
                return nextResult;
            }
            return validationOk(payload);
        }
    }

    private static class InMemoryRegressionDAO extends V2SubmitDryRunPayloadDAO {
        private final Map<Long, V2SubmitDryRunRecord> records = new HashMap<>();
        private int insertCount;
        private int examResultsWriteCount;
        private int attemptSubmittedUpdateCount;
        private int legacySubmitCallCount;
        private V2SubmitDryRunRecord lastRecord;
        private long nextId = 1L;

        @Override
        public void ensureSchema() {
        }

        @Override
        public long insertDryRunRecord(V2SubmitDryRunRecord record) {
            insertCount++;
            record.setId(nextId++);
            records.put(record.getId(), record);
            lastRecord = record;
            return record.getId();
        }

        @Override
        public V2SubmitDryRunRecord findById(long id) {
            return records.get(id);
        }

        @Override
        public V2SubmitDryRunRecord findLatestByAttemptId(String attemptId) {
            V2SubmitDryRunRecord latest = null;
            for (V2SubmitDryRunRecord record : records.values()) {
                if (attemptId != null && attemptId.equals(record.getAttemptId())) {
                    latest = record;
                }
            }
            return latest;
        }

        @Override
        public boolean existsByPayloadHash(String payloadHash) throws SQLException {
            for (V2SubmitDryRunRecord record : records.values()) {
                if (payloadHash != null && payloadHash.equals(record.getPayloadHash())) {
                    return true;
                }
            }
            return false;
        }
    }
}
