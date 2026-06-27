package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptFinalizationLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptFinalizationLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;

import java.sql.SQLException;
import java.util.Optional;

public class V2AttemptFinalizationLedgerService {

    private final V2SubmitRecordDAO submitRecordDAO;
    private final V2AttemptFinalizationDraftService finalizationDraftService;
    private final V2AttemptFinalizationLedgerDAO ledgerDAO;

    public V2AttemptFinalizationLedgerService() {
        this.submitRecordDAO = new V2SubmitRecordDAO();
        this.finalizationDraftService = new V2AttemptFinalizationDraftService();
        this.ledgerDAO = new V2AttemptFinalizationLedgerDAO();
    }

    // For testing
    public V2AttemptFinalizationLedgerService(V2SubmitRecordDAO submitRecordDAO,
                                              V2AttemptFinalizationDraftService finalizationDraftService,
                                              V2AttemptFinalizationLedgerDAO ledgerDAO) {
        this.submitRecordDAO = submitRecordDAO;
        this.finalizationDraftService = finalizationDraftService;
        this.ledgerDAO = ledgerDAO;
    }

    public V2AttemptFinalizationLedgerResult createLedgerAfterFinalizationDraft(int userId, long submitRecordId) {
        V2AttemptFinalizationLedgerResult result = new V2AttemptFinalizationLedgerResult();

        boolean enabled = V2SubmitFeatureFlags.isAttemptFinalizationLedgerEnabled();
        if (!enabled) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_FEATURE_DISABLED");
            return result;
        }

        try {
            Optional<V2SubmitRecord> submitRecordOpt = submitRecordDAO.findById(submitRecordId);
            if (submitRecordOpt.isEmpty()) {
                result.setSuccess(false);
                result.setErrorCode("ERROR_V2_SUBMIT_RECORD_NOT_FOUND");
                return result;
            }

            V2SubmitRecord submitRecord = submitRecordOpt.get();

            if (submitRecord.getUserId() != userId) {
                result.setSuccess(false);
                result.setErrorCode("ERROR_V2_SUBMIT_RECORD_USER_MISMATCH");
                return result;
            }

            if (submitRecord.getPayloadHash() == null || submitRecord.getPayloadHash().length() != 64) {
                result.setSuccess(false);
                result.setErrorCode("ERROR_V2_SUBMIT_RECORD_HASH_INVALID");
                return result;
            }

            // Reject if status is FINALIZATION_REJECTED
            if ("FINALIZATION_REJECTED".equals(submitRecord.getSubmitStatus())) {
                result.setSuccess(false);
                result.setErrorCode("ERROR_V2_FINALIZATION_LEDGER_UNSAFE");
                return result;
            }

            // Check existing ledger for idempotency
            Optional<V2AttemptFinalizationLedgerRecord> existingLedgerOpt = ledgerDAO.findLatestBySubmitRecordId(submitRecordId);
            if (existingLedgerOpt.isPresent()) {
                V2AttemptFinalizationLedgerRecord existing = existingLedgerOpt.get();
                populateResultFromLedger(result, existing);
                result.setSuccess(true);
                result.setIdempotent(true);
                return result;
            }

            // Ensure FINALIZATION_DRAFTED status
            if (!"FINALIZATION_DRAFTED".equals(submitRecord.getSubmitStatus())) {
                V2AttemptFinalizationDraftResult draftResult = finalizationDraftService.createFinalizationDraft(userId, submitRecordId);
                if (!draftResult.isSuccess()) {
                    result.setSuccess(false);
                    result.setErrorCode("ERROR_V2_FINALIZATION_DRAFT_FAILED");
                    return result;
                }
            }

            // Create ledger record
            V2AttemptFinalizationLedgerRecord newLedger = new V2AttemptFinalizationLedgerRecord();
            newLedger.setSubmitRecordId(submitRecordId);
            newLedger.setUserId(userId);
            newLedger.setExamId(submitRecord.getExamId());
            newLedger.setPaperId(submitRecord.getPaperId());
            newLedger.setAttemptId(submitRecord.getAttemptId());
            newLedger.setPayloadHash(submitRecord.getPayloadHash());
            newLedger.setPreviousSubmitStatus(submitRecord.getSubmitStatus()); // now it is FINALIZATION_DRAFTED
            newLedger.setFinalizationStatus("FINALIZATION_DRAFT_ACKNOWLEDGED");
            newLedger.setFinalizationMode("NO_GRADING_NO_FINAL_SUBMIT");
            newLedger.setSource("V2_DEBUG");

            long ledgerId = ledgerDAO.insertLedgerRecord(newLedger);
            if (ledgerId <= 0) {
                result.setSuccess(false);
                result.setErrorCode("ERROR_V2_FINALIZATION_LEDGER_INSERT_FAILED");
                return result;
            }

            // Return new ledger info
            Optional<V2AttemptFinalizationLedgerRecord> insertedOpt = ledgerDAO.findById(ledgerId);
            if (insertedOpt.isPresent()) {
                populateResultFromLedger(result, insertedOpt.get());
                result.setSuccess(true);
                result.setIdempotent(false);
            } else {
                result.setSuccess(false);
                result.setErrorCode("ERROR_V2_FINALIZATION_LEDGER_INSERT_FAILED");
            }

        } catch (SQLException e) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_V2_FINALIZATION_LEDGER_INSERT_FAILED");
            e.printStackTrace();
        }

        return result;
    }

    private void populateResultFromLedger(V2AttemptFinalizationLedgerResult result, V2AttemptFinalizationLedgerRecord ledger) {
        result.setLedgerId(ledger.getId());
        result.setSubmitRecordId(ledger.getSubmitRecordId());
        result.setExamId(ledger.getExamId());
        result.setPaperId(ledger.getPaperId());
        result.setAttemptId(ledger.getAttemptId());
        result.setPayloadHash(ledger.getPayloadHash());
        result.setPreviousSubmitStatus(ledger.getPreviousSubmitStatus());
        result.setFinalizationStatus(ledger.getFinalizationStatus());
        result.setFinalizationMode(ledger.getFinalizationMode());
        result.setCreatedAt(ledger.getCreatedAt());
    }
}
