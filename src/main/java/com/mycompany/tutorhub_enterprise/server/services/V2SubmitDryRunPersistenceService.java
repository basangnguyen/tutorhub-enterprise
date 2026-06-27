package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.client.exam.ui.TSEV2SubmitPayload;
import com.mycompany.tutorhub_enterprise.client.exam.ui.TSEV2SubmitPayloadService;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitDryRunRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitDryRunPayloadDAO;

import java.time.Instant;
import java.util.Locale;

public class V2SubmitDryRunPersistenceService {

    public static final String FEATURE_FLAG = "tse.v2.submitDryRunPersistence.enabled";

    private final V2SubmitDryRunValidationService validationService;
    private final V2SubmitDryRunPayloadDAO dao;
    private final TSEV2SubmitPayloadService payloadService;

    public V2SubmitDryRunPersistenceService() {
        this(new V2SubmitDryRunValidationService(), new V2SubmitDryRunPayloadDAO(), new TSEV2SubmitPayloadService());
    }

    public V2SubmitDryRunPersistenceService(
            V2SubmitDryRunValidationService validationService,
            V2SubmitDryRunPayloadDAO dao,
            TSEV2SubmitPayloadService payloadService) {
        this.validationService = validationService;
        this.dao = dao;
        this.payloadService = payloadService;
    }

    public V2SubmitDryRunPersistenceResult persistDryRun(int userId, TSEV2SubmitPayload payload) {
        if (!V2SubmitFeatureFlags.isSubmitDryRunPersistenceEnabled()) {
            return V2SubmitDryRunPersistenceResult.error("ERROR_FEATURE_DISABLED");
        }

        V2SubmitDryRunValidationResult validationResult = validationService.validateDryRun(userId, payload);
        if (validationResult == null || !validationResult.isSuccess()) {
            String errorCode = validationResult != null && validationResult.getErrorCode() != null
                    ? validationResult.getErrorCode()
                    : "ERROR_V2_SUBMIT_DRYRUN_VALIDATE_FAILED";
            return V2SubmitDryRunPersistenceResult.error(errorCode);
        }

        String payloadJson = payloadService.toJson(payload);
        if (!isSafeToPersist(payloadJson)) {
            return V2SubmitDryRunPersistenceResult.error("ERROR_V2_SUBMIT_DRYRUN_PAYLOAD_UNSAFE");
        }

        V2SubmitDryRunRecord record = new V2SubmitDryRunRecord();
        record.setUserId(userId);
        record.setExamId(payload.getExamId());
        record.setPaperId(payload.getPaperId());
        record.setAttemptId(payload.getAttemptId());
        record.setPackageHash(payload.getPackageHash());
        record.setPayloadHash(payload.getPayloadHash());
        record.setPayloadJson(payloadJson);
        record.setAnsweredCount(payload.getAnsweredCount());
        record.setUnansweredCount(payload.getUnansweredCount());
        record.setComplete(payload.isComplete());
        record.setValidationStatus("VALIDATED");

        try {
            long recordId = dao.insertDryRunRecord(record);
            if (recordId <= 0L) {
                return V2SubmitDryRunPersistenceResult.error("ERROR_V2_SUBMIT_DRYRUN_PERSIST_FAILED");
            }

            V2SubmitDryRunPersistenceResult result = new V2SubmitDryRunPersistenceResult();
            result.setSuccess(true);
            result.setErrorCode("EXAM_SUBMIT_V2_DRYRUN_PERSIST_OK");
            result.setRecordId(recordId);
            result.setExamId(payload.getExamId());
            result.setPaperId(payload.getPaperId());
            result.setAttemptId(payload.getAttemptId());
            result.setPayloadHash(payload.getPayloadHash());
            result.setAnsweredCount(payload.getAnsweredCount());
            result.setUnansweredCount(payload.getUnansweredCount());
            result.setComplete(payload.isComplete());
            result.setPersistedAt(Instant.now().toString());
            return result;
        } catch (Exception e) {
            return V2SubmitDryRunPersistenceResult.error("ERROR_V2_SUBMIT_DRYRUN_PERSIST_FAILED");
        }
    }

    protected boolean isSafeToPersist(String payloadJson) {
        if (payloadJson == null || payloadJson.trim().isEmpty()) {
            return false;
        }
        String value = payloadJson.toLowerCase(Locale.ROOT);
        return !value.contains("sessiontoken")
                && !value.contains("keyb64")
                && !value.contains("plaintextjson")
                && !value.contains("plaintext")
                && !value.contains("answerkey")
                && !value.contains("iscorrect")
                && !value.contains("correctoption")
                && !value.contains("passwordhash")
                && !value.contains("password")
                && !value.contains("score")
                && !value.contains("gradingresult");
    }
}
