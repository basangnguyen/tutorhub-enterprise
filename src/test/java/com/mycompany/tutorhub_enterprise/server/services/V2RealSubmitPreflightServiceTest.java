package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptClosureDraftRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptFinalizationLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptClosureDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptFinalizationLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class V2RealSubmitPreflightServiceTest {

    private V2RealSubmitPreflightService preflightService;
    
    private V2SubmitRecord mockSubmitRecord;
    private V2AttemptFinalizationLedgerRecord mockLedgerRecord;
    private V2AttemptClosureDraftRecord mockClosureDraftRecord;

    @BeforeEach
    public void setUp() {
        System.setProperty("tse.v2.realSubmitPreflight.enabled", "true");
        
        mockSubmitRecord = null;
        mockLedgerRecord = null;
        mockClosureDraftRecord = null;

        V2SubmitRecordDAO mockSubmitRecordDAO = new V2SubmitRecordDAO() {
            @Override
            public Optional<V2SubmitRecord> findById(long id) {
                if (mockSubmitRecord != null && mockSubmitRecord.getId() == id) {
                    return Optional.of(mockSubmitRecord);
                }
                return Optional.empty();
            }
        };

        V2AttemptFinalizationLedgerDAO mockLedgerDAO = new V2AttemptFinalizationLedgerDAO() {
            @Override
            public Optional<V2AttemptFinalizationLedgerRecord> findLatestBySubmitRecordId(long submitRecordId) {
                if (mockLedgerRecord != null && mockLedgerRecord.getSubmitRecordId() == submitRecordId) {
                    return Optional.of(mockLedgerRecord);
                }
                return Optional.empty();
            }
        };

        V2AttemptClosureDraftDAO mockClosureDAO = new V2AttemptClosureDraftDAO() {
            @Override
            public Optional<V2AttemptClosureDraftRecord> findLatestBySubmitRecordId(long submitRecordId) {
                if (mockClosureDraftRecord != null && mockClosureDraftRecord.getSubmitRecordId() == submitRecordId) {
                    return Optional.of(mockClosureDraftRecord);
                }
                return Optional.empty();
            }
        };

        preflightService = new V2RealSubmitPreflightService(mockSubmitRecordDAO, mockLedgerDAO, mockClosureDAO);
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.realSubmitPreflight.enabled");
    }

    @Test
    public void testFeatureFlagOff_Rejects() {
        System.setProperty("tse.v2.realSubmitPreflight.enabled", "false");
        V2RealSubmitPreflightResult result = preflightService.checkPreflight(1, 1L);
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testSubmitRecordNotFound_Rejects() {
        V2RealSubmitPreflightResult result = preflightService.checkPreflight(1, -999L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_PREFLIGHT_SUBMIT_RECORD_NOT_FOUND", result.getErrorCode());
    }

    @Test
    public void testUserMismatch_Rejects() {
        mockSubmitRecord = new V2SubmitRecord();
        mockSubmitRecord.setId(100L);
        mockSubmitRecord.setUserId(2);
        mockSubmitRecord.setExamId(10);
        mockSubmitRecord.setPaperId(20);
        mockSubmitRecord.setAttemptId(UUID.randomUUID().toString());
        mockSubmitRecord.setPayloadHash("a".repeat(64));
        mockSubmitRecord.setPayloadJson("{}");
        mockSubmitRecord.setSubmitStatus("RECEIVED");

        V2RealSubmitPreflightResult result = preflightService.checkPreflight(1, 100L); // mismatch user 1 vs 2
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_PREFLIGHT_USER_MISMATCH", result.getErrorCode());
    }

    @Test
    public void testInvalidPayloadHash_Rejects() {
        mockSubmitRecord = new V2SubmitRecord();
        mockSubmitRecord.setId(100L);
        mockSubmitRecord.setUserId(1);
        mockSubmitRecord.setPayloadHash("invalidhash");

        V2RealSubmitPreflightResult result = preflightService.checkPreflight(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_PREFLIGHT_HASH_INVALID", result.getErrorCode());
    }

    @Test
    public void testStatusNotReady_Rejects() {
        mockSubmitRecord = new V2SubmitRecord();
        mockSubmitRecord.setId(100L);
        mockSubmitRecord.setUserId(1);
        mockSubmitRecord.setPayloadHash("a".repeat(64));
        mockSubmitRecord.setSubmitStatus("RECEIVED");

        V2RealSubmitPreflightResult result = preflightService.checkPreflight(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_PREFLIGHT_STATUS_NOT_READY", result.getErrorCode());
    }

    @Test
    public void testLedgerMissing_Rejects() {
        mockSubmitRecord = new V2SubmitRecord();
        mockSubmitRecord.setId(100L);
        mockSubmitRecord.setUserId(1);
        mockSubmitRecord.setPayloadHash("a".repeat(64));
        mockSubmitRecord.setSubmitStatus("FINALIZATION_DRAFTED");

        V2RealSubmitPreflightResult result = preflightService.checkPreflight(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_PREFLIGHT_LEDGER_MISSING", result.getErrorCode());
    }

    @Test
    public void testUnsafePayload_Rejects() {
        mockSubmitRecord = new V2SubmitRecord();
        mockSubmitRecord.setId(100L);
        mockSubmitRecord.setUserId(1);
        mockSubmitRecord.setPayloadHash("a".repeat(64));
        mockSubmitRecord.setPayloadJson("{\"score\": 10}");
        mockSubmitRecord.setSubmitStatus("FINALIZATION_DRAFTED");

        V2RealSubmitPreflightResult result = preflightService.checkPreflight(1, 100L);
        assertFalse(result.isSuccess());
        assertEquals("ERROR_V2_PREFLIGHT_UNSAFE_PAYLOAD", result.getErrorCode());
    }

    @Test
    public void testValidPipeline_Ready() {
        String hash = "a".repeat(64);
        
        mockSubmitRecord = new V2SubmitRecord();
        mockSubmitRecord.setId(100L);
        mockSubmitRecord.setUserId(1);
        mockSubmitRecord.setAttemptId(UUID.randomUUID().toString());
        mockSubmitRecord.setPayloadHash(hash);
        mockSubmitRecord.setPayloadJson("{\"safe\":\"yes\"}");
        mockSubmitRecord.setSubmitStatus("FINALIZATION_DRAFTED");

        mockLedgerRecord = new V2AttemptFinalizationLedgerRecord();
        mockLedgerRecord.setId(200L);
        mockLedgerRecord.setSubmitRecordId(100L);
        mockLedgerRecord.setFinalizationStatus("FINALIZATION_LEDGER_NO_GRADING");

        mockClosureDraftRecord = new V2AttemptClosureDraftRecord();
        mockClosureDraftRecord.setId(300L);
        mockClosureDraftRecord.setSubmitRecordId(100L);
        mockClosureDraftRecord.setClosureStatus("CLOSURE_DRAFTED_NO_GRADING");

        V2RealSubmitPreflightResult result = preflightService.checkPreflight(1, 100L);
        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertEquals("READY_FOR_REAL_SUBMIT_DRAFT", result.getPreflightStatus());
    }

    @Test
    public void testResultDTONoUnsafeFields() {
        Field[] fields = V2RealSubmitPreflightResult.class.getDeclaredFields();
        for (Field f : fields) {
            String name = f.getName();
            assertFalse("answers".equals(name) || "selectedOptionId".equals(name) || "answerKey".equals(name) 
                || "isCorrect".equals(name) || "correctOption".equals(name) || "score".equals(name) 
                || "gradingResult".equals(name) || "sessionToken".equals(name) || "keyB64".equals(name) 
                || "plaintext".equals(name), "Unsafe field found: " + name);
        }
    }
}
