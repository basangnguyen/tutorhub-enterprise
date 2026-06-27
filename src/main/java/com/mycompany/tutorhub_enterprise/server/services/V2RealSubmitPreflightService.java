package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptClosureDraftRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptFinalizationLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptClosureDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptFinalizationLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;

import java.time.Instant;
import java.util.Optional;

public class V2RealSubmitPreflightService {

    private final V2SubmitRecordDAO submitRecordDAO;
    private final V2AttemptFinalizationLedgerDAO ledgerDAO;
    private final V2AttemptClosureDraftDAO closureDraftDAO;

    public V2RealSubmitPreflightService() {
        this(new V2SubmitRecordDAO(), new V2AttemptFinalizationLedgerDAO(), new V2AttemptClosureDraftDAO());
    }

    public V2RealSubmitPreflightService(V2SubmitRecordDAO submitRecordDAO, V2AttemptFinalizationLedgerDAO ledgerDAO, V2AttemptClosureDraftDAO closureDraftDAO) {
        this.submitRecordDAO = submitRecordDAO;
        this.ledgerDAO = ledgerDAO;
        this.closureDraftDAO = closureDraftDAO;
    }

    public V2RealSubmitPreflightResult checkPreflight(int userId, long submitRecordId) {
        V2RealSubmitPreflightResult result = new V2RealSubmitPreflightResult();
        result.setCheckedAt(Instant.now().toString());
        result.setUserId(userId);
        result.setSubmitRecordId(submitRecordId);

        try {
            // 1. Check feature flag
            if (!V2SubmitFeatureFlags.isRealSubmitPreflightEnabled()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_FEATURE_DISABLED");
                result.setPreflightStatus("NOT_READY");
                result.addBlockingReason("V2 Real Submit Preflight feature flag is disabled.");
                return result;
            }

            // 2. Check submit record
            Optional<V2SubmitRecord> recordOpt = submitRecordDAO.findById(submitRecordId);
            if (!recordOpt.isPresent()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_PREFLIGHT_SUBMIT_RECORD_NOT_FOUND");
                result.setPreflightStatus("NOT_READY");
                result.addBlockingReason("Submit record not found for id: " + submitRecordId);
                return result;
            }

            V2SubmitRecord record = recordOpt.get();
            result.setExamId(record.getExamId());
            result.setPaperId(record.getPaperId());
            result.setAttemptId(record.getAttemptId());
            result.setPayloadHash(record.getPayloadHash());
            result.setCurrentSubmitStatus(record.getSubmitStatus());

            // 3. User mismatch
            if (record.getUserId() != userId) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_PREFLIGHT_USER_MISMATCH");
                result.setPreflightStatus("NOT_READY");
                result.addBlockingReason("User ID mismatch. Expected " + record.getUserId() + " but got " + userId);
                return result;
            }

            // 4. payloadHash format (SHA-256 hex 64 chars)
            if (record.getPayloadHash() == null || !record.getPayloadHash().matches("^[a-fA-F0-9]{64}$")) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_PREFLIGHT_HASH_INVALID");
                result.setPreflightStatus("NOT_READY");
                result.addBlockingReason("Invalid payload hash format. Must be 64-char SHA-256 hex.");
                return result;
            }

            // 5. Submit record status
            if (!"FINALIZATION_DRAFTED".equals(record.getSubmitStatus()) && !"CLOSURE_DRAFTED".equals(record.getSubmitStatus())) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_PREFLIGHT_STATUS_NOT_READY");
                result.setPreflightStatus("NOT_READY");
                result.addBlockingReason("Submit record status is not ready: " + record.getSubmitStatus());
                return result;
            }

            // 10. Check unsafe payload fields
            String payloadJson = record.getPayloadJson();
            if (payloadJson != null && (
                    payloadJson.contains("answerKey") || 
                    payloadJson.contains("isCorrect") || 
                    payloadJson.contains("correctOption") || 
                    payloadJson.contains("gradingResult") || 
                    payloadJson.contains("score"))) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_PREFLIGHT_UNSAFE_PAYLOAD");
                result.setPreflightStatus("NOT_READY");
                result.addBlockingReason("Payload contains unsafe fields (grading or score).");
                return result;
            }

            // 6. Ledger missing
            Optional<V2AttemptFinalizationLedgerRecord> ledgerOpt = ledgerDAO.findLatestBySubmitRecordId(submitRecordId);
            if (!ledgerOpt.isPresent()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_PREFLIGHT_LEDGER_MISSING");
                result.setPreflightStatus("NOT_READY");
                result.addBlockingReason("Finalization ledger missing.");
                return result;
            }
            V2AttemptFinalizationLedgerRecord ledger = ledgerOpt.get();
            result.setLedgerId(ledger.getId());

            // 7. Closure draft missing
            Optional<V2AttemptClosureDraftRecord> closureOpt = closureDraftDAO.findLatestBySubmitRecordId(submitRecordId);
            if (!closureOpt.isPresent()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_PREFLIGHT_CLOSURE_MISSING");
                result.setPreflightStatus("NOT_READY");
                result.addBlockingReason("Closure draft missing.");
                return result;
            }
            V2AttemptClosureDraftRecord closure = closureOpt.get();
            result.setClosureDraftId(closure.getId());
            result.setClosureStatus(closure.getClosureStatus());

            // 8. Closure status invalid
            if (!"CLOSURE_DRAFTED_NO_GRADING".equals(closure.getClosureStatus())) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_PREFLIGHT_CLOSURE_STATUS_INVALID");
                result.setPreflightStatus("NOT_READY");
                result.addBlockingReason("Closure status is invalid: " + closure.getClosureStatus());
                return result;
            }

            // Passed all checks
            result.setSuccess(true);
            result.setReady(true);
            result.setErrorCode(null);
            result.setPreflightStatus("READY_FOR_REAL_SUBMIT_DRAFT");

        } catch (Exception e) {
            e.printStackTrace();
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_V2_PREFLIGHT_EXCEPTION");
            result.setPreflightStatus("NOT_READY");
            result.addBlockingReason("Exception during preflight: " + e.getMessage());
        }

        return result;
    }
}
