package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptClosureDraftRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptFinalizationLedgerRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptClosureDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptFinalizationLedgerDAO;

import java.sql.SQLException;
import java.util.Optional;

public class V2AttemptClosureDraftService {

    private final V2AttemptFinalizationLedgerDAO ledgerDAO;
    private final V2AttemptFinalizationLedgerService ledgerService;
    private final V2AttemptClosureDraftDAO closureDAO;

    public V2AttemptClosureDraftService() {
        this.ledgerDAO = new V2AttemptFinalizationLedgerDAO();
        this.ledgerService = new V2AttemptFinalizationLedgerService();
        this.closureDAO = new V2AttemptClosureDraftDAO();
        try {
            this.closureDAO.ensureSchema();
        } catch (SQLException e) {
            System.err.println("[V2_CLOSURE_DRAFT] ensureSchema failed: " + e.getMessage());
        }
    }

    // For testing
    public V2AttemptClosureDraftService(V2AttemptFinalizationLedgerDAO ledgerDAO,
                                        V2AttemptFinalizationLedgerService ledgerService,
                                        V2AttemptClosureDraftDAO closureDAO) {
        this.ledgerDAO = ledgerDAO;
        this.ledgerService = ledgerService;
        this.closureDAO = closureDAO;
    }

    public V2AttemptClosureDraftResult createClosureDraft(int userId, long submitRecordId) {
        V2AttemptClosureDraftResult result = new V2AttemptClosureDraftResult();

        boolean enabled = V2SubmitFeatureFlags.isAttemptClosureDraftEnabled();
        if (!enabled) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_FEATURE_DISABLED");
            return result;
        }

        try {
            // Check if closure draft already exists (idempotent)
            Optional<V2AttemptClosureDraftRecord> existingClosure = closureDAO.findLatestBySubmitRecordId(submitRecordId);
            if (existingClosure.isPresent()) {
                V2AttemptClosureDraftRecord existing = existingClosure.get();
                result.setSuccess(true);
                result.setIdempotent(true);
                result.setClosureDraftId(existing.getId());
                result.setLedgerId(existing.getLedgerId());
                result.setSubmitRecordId(existing.getSubmitRecordId());
                result.setExamId(existing.getExamId());
                result.setPaperId(existing.getPaperId());
                result.setAttemptId(existing.getAttemptId());
                result.setPayloadHash(existing.getPayloadHash());
                result.setClosureStatus(existing.getClosureStatus());
                result.setClosureMode(existing.getClosureMode());
                result.setCreatedAt(existing.getCreatedAt());
                return result;
            }

            // Load finalization ledger
            Optional<V2AttemptFinalizationLedgerRecord> optLedger = ledgerDAO.findLatestBySubmitRecordId(submitRecordId);
            if (!optLedger.isPresent()) {
                // Try creating ledger if missing
                V2AttemptFinalizationLedgerResult ledgerResult = ledgerService.createLedgerAfterFinalizationDraft(userId, submitRecordId);
                if (!ledgerResult.isSuccess()) {
                    result.setSuccess(false);
                    result.setErrorCode("ERROR_V2_CLOSURE_LEDGER_NOT_FOUND");
                    return result;
                }
                optLedger = ledgerDAO.findLatestBySubmitRecordId(submitRecordId);
                if (!optLedger.isPresent()) {
                    result.setSuccess(false);
                    result.setErrorCode("ERROR_V2_CLOSURE_LEDGER_NOT_FOUND");
                    return result;
                }
            }

            V2AttemptFinalizationLedgerRecord ledger = optLedger.get();

            if (ledger.getUserId() != userId) {
                result.setSuccess(false);
                result.setErrorCode("ERROR_V2_CLOSURE_USER_MISMATCH");
                return result;
            }

            if (ledger.getPayloadHash() == null || ledger.getPayloadHash().length() != 64) {
                result.setSuccess(false);
                result.setErrorCode("ERROR_V2_CLOSURE_HASH_INVALID");
                return result;
            }

            V2AttemptClosureDraftRecord newDraft = new V2AttemptClosureDraftRecord();
            newDraft.setLedgerId(ledger.getId());
            newDraft.setSubmitRecordId(ledger.getSubmitRecordId());
            newDraft.setUserId(ledger.getUserId());
            newDraft.setExamId(ledger.getExamId());
            newDraft.setPaperId(ledger.getPaperId());
            newDraft.setAttemptId(ledger.getAttemptId());
            newDraft.setPayloadHash(ledger.getPayloadHash());
            newDraft.setClosureStatus("CLOSURE_DRAFTED_NO_GRADING");
            newDraft.setClosureMode("NO_GRADING_NO_FINAL_SUBMIT");
            newDraft.setSource("V2_DEBUG");

            long draftId = closureDAO.insertClosureDraft(newDraft);

            result.setSuccess(true);
            result.setIdempotent(false);
            result.setClosureDraftId(draftId);
            result.setLedgerId(ledger.getId());
            result.setSubmitRecordId(ledger.getSubmitRecordId());
            result.setExamId(ledger.getExamId());
            result.setPaperId(ledger.getPaperId());
            result.setAttemptId(ledger.getAttemptId());
            result.setPayloadHash(ledger.getPayloadHash());
            result.setClosureStatus("CLOSURE_DRAFTED_NO_GRADING");
            result.setClosureMode("NO_GRADING_NO_FINAL_SUBMIT");
            
            // Re-fetch to get timestamp
            Optional<V2AttemptClosureDraftRecord> inserted = closureDAO.findById(draftId);
            inserted.ifPresent(record -> result.setCreatedAt(record.getCreatedAt()));

            return result;

        } catch (SQLException e) {
            e.printStackTrace();
            result.setSuccess(false);
            result.setErrorCode("ERROR_V2_CLOSURE_INSERT_FAILED");
            return result;
        }
    }
}
