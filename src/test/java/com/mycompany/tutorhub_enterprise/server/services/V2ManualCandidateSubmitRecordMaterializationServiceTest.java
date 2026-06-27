package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2ManualCandidateSubmitRecordLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ManualCandidateSubmitRecordLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class V2ManualCandidateSubmitRecordMaterializationServiceTest {

    private V2ManualCandidateActualSubmitPreflightService mockPreflightService;
    private V2AnswerPayloadContractValidator mockValidator;
    private V2SubmitRecordDAO mockSubmitRecordDAO;
    private V2ManualCandidateSubmitRecordLedgerDAO mockLedgerDAO;
    private V2ManualCandidateSubmitRecordMaterializationService service;

    private V2ManualCandidateActualSubmitPreflightResult preflightResultToReturn;
    private V2AnswerPayloadContractValidationResult validatorResultToReturn;
    private Optional<V2SubmitRecord> submitRecordToReturn;
    private Optional<V2ManualCandidateSubmitRecordLedgerRecord> ledgerRecordToReturn;

    @BeforeEach
    void setUp() {
        System.setProperty("tse.v2.manualCandidateSubmitRecordMaterialization.enabled", "true");

        preflightResultToReturn = new V2ManualCandidateActualSubmitPreflightResult();
        validatorResultToReturn = new V2AnswerPayloadContractValidationResult();
        submitRecordToReturn = Optional.empty();
        ledgerRecordToReturn = Optional.empty();

        mockPreflightService = new V2ManualCandidateActualSubmitPreflightService(null, null, null, null, null, null) {
            @Override
            public V2ManualCandidateActualSubmitPreflightResult checkPreflight(int userId, String attemptId, String payloadJson) {
                return preflightResultToReturn;
            }
        };

        mockValidator = new V2AnswerPayloadContractValidator() {
            @Override
            public V2AnswerPayloadContractValidationResult validate(String payloadJson) {
                return validatorResultToReturn;
            }
        };

        mockSubmitRecordDAO = new V2SubmitRecordDAO() {
            @Override
            public Optional<V2SubmitRecord> findLatestByAttemptId(String attemptId) {
                return submitRecordToReturn;
            }
        };

        mockLedgerDAO = new V2ManualCandidateSubmitRecordLedgerDAO() {
            @Override
            public Optional<V2ManualCandidateSubmitRecordLedgerRecord> findByAttemptId(String attemptId) {
                return ledgerRecordToReturn;
            }
        };

        service = new V2ManualCandidateSubmitRecordMaterializationService(
                mockPreflightService,
                mockValidator,
                mockSubmitRecordDAO,
                mockLedgerDAO
        );
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("tse.v2.manualCandidateSubmitRecordMaterialization.enabled");
    }

    @Test
    void testMaterialize_FlagDisabled() {
        System.setProperty("tse.v2.manualCandidateSubmitRecordMaterialization.enabled", "false");
        V2ManualCandidateSubmitRecordMaterializationResult result = service.materializeSubmitRecord(1, "att-1", "{}");
        assertFalse(result.isReady());
        assertEquals("NOT_READY", result.getMaterializationStatus());
    }

    @Test
    void testMaterialize_PreflightNotReady() {
        preflightResultToReturn.setReady(false);
        preflightResultToReturn.setBlockingReasons(new java.util.ArrayList<>(java.util.Arrays.asList("preflight block")));

        V2ManualCandidateSubmitRecordMaterializationResult result = service.materializeSubmitRecord(1, "att-1", "{}");
        assertFalse(result.isReady());
        assertTrue(result.getBlockingReasons().get(0).contains("preflight block"));
    }

    @Test
    void testMaterialize_ContractInvalid() {
        preflightResultToReturn.setReady(true);
        validatorResultToReturn.setValid(false);
        validatorResultToReturn.setBlockingReasons(new java.util.ArrayList<>(java.util.Arrays.asList("invalid format")));

        V2ManualCandidateSubmitRecordMaterializationResult result = service.materializeSubmitRecord(1, "att-1", "{}");
        assertFalse(result.isReady());
        assertTrue(result.getBlockingReasons().contains("Payload contract invalid: invalid format"));
    }

    @Test
    void testMaterialize_IdempotentSuccess() {
        preflightResultToReturn.setReady(true);
        validatorResultToReturn.setValid(true);

        V2SubmitRecord sr = new V2SubmitRecord();
        sr.setId(999L);
        submitRecordToReturn = Optional.of(sr);
        ledgerRecordToReturn = Optional.of(new V2ManualCandidateSubmitRecordLedgerRecord());

        V2ManualCandidateSubmitRecordMaterializationResult result = service.materializeSubmitRecord(1, "att-1", "{}");
        assertTrue(result.isReady());
        assertTrue(result.isIdempotent());
        assertEquals("V2_SUBMIT_RECORD_MATERIALIZED", result.getMaterializationStatus());
        assertEquals(999L, result.getSubmitRecordId());
    }

    @Test
    void testMaterialize_UnsafeState_MissingLedger() {
        preflightResultToReturn.setReady(true);
        validatorResultToReturn.setValid(true);

        submitRecordToReturn = Optional.of(new V2SubmitRecord());
        ledgerRecordToReturn = Optional.empty();

        V2ManualCandidateSubmitRecordMaterializationResult result = service.materializeSubmitRecord(1, "att-1", "{}");
        assertFalse(result.isReady());
        assertEquals("ERROR_UNSAFE_STATE", result.getMaterializationStatus());
        assertTrue(result.getBlockingReasons().contains("V2SubmitRecord exists but ledger is missing"));
    }
}
