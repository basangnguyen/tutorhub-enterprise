package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2ManualCandidateExecutionLedgerRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ManualCandidateExecutionLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;

import java.util.Optional;

public class V2ManualCandidateExecutionAuditService {

    private final V2ManualCandidateExecutionLedgerDAO executionLedgerDAO;
    private final V2SubmitRecordDAO submitRecordDAO;

    public V2ManualCandidateExecutionAuditService() {
        this.executionLedgerDAO = new V2ManualCandidateExecutionLedgerDAO();
        this.executionLedgerDAO.ensureSchema();
        this.submitRecordDAO = new V2SubmitRecordDAO();
    }

    public V2ManualCandidateExecutionAuditService(
            V2ManualCandidateExecutionLedgerDAO executionLedgerDAO,
            V2SubmitRecordDAO submitRecordDAO) {
        this.executionLedgerDAO = executionLedgerDAO;
        if (this.executionLedgerDAO != null) {
            this.executionLedgerDAO.ensureSchema();
        }
        this.submitRecordDAO = submitRecordDAO;
    }

    public V2ManualCandidateExecutionAuditResult auditManualExecution(int userId, String attemptId) {
        V2ManualCandidateExecutionAuditResult result = new V2ManualCandidateExecutionAuditResult();
        result.setUserId(userId);
        result.setAttemptId(attemptId);

        try {
            if (!V2SubmitFeatureFlags.isManualCandidateExecutionAuditEnabled()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_FEATURE_DISABLED");
                result.addBlockingReason("Manual Candidate Execution Audit feature flag is disabled.");
                return result;
            }

            Optional<V2ManualCandidateExecutionLedgerRecord> ledgerOpt = executionLedgerDAO.findByAttemptId(attemptId);
            if (ledgerOpt.isEmpty()) {
                result.setSuccess(true);
                result.setReady(false);
                result.setExecuted(false);
                result.setExecutionStatus("NOT_EXECUTED");
                result.addBlockingReason("No manual execution ledger found for attempt.");
                return result;
            }

            V2ManualCandidateExecutionLedgerRecord ledger = ledgerOpt.get();

            // Additional sanity check: user ID mismatch
            if (ledger.getUserId() != userId) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_USER_MISMATCH");
                result.addBlockingReason("Ledger belongs to a different user.");
                return result;
            }

            result.setSuccess(true);
            result.setReady(true);
            result.setExecuted(true);
            result.setExamId(ledger.getExamId());
            result.setPaperId(ledger.getPaperId());
            result.setManualExecutionLedgerId(ledger.getId());
            result.setExecutionStatus(ledger.getExecutionStatus());
            result.setExecutionMode(ledger.getExecutionMode());

            // Check if PREPARE_ONLY is consistent (should not have submitRecordId)
            if ("PREPARE_ONLY".equals(ledger.getExecutionMode())) {
                if (ledger.getSubmitRecordId() > 0) {
                    result.setSuccess(false);
                    result.setReady(false);
                    result.setErrorCode("ERROR_V2_MANUAL_CANDIDATE_INCONSISTENT_STATE");
                    result.addBlockingReason("PREPARE_ONLY execution ledger has a submitRecordId which is invalid.");
                }
            }

            // If it is a full execution, check submit record exists
            if ("FULL_EXECUTION".equals(ledger.getExecutionMode()) && ledger.getSubmitRecordId() > 0) {
                if (submitRecordDAO.findById(ledger.getSubmitRecordId()).isEmpty()) {
                    result.setSuccess(false);
                    result.setReady(false);
                    result.setErrorCode("ERROR_V2_MANUAL_CANDIDATE_INCONSISTENT_STATE");
                    result.addBlockingReason("Submit record ID exists in ledger but not in database.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_EXCEPTION");
            result.addBlockingReason("Exception during manual execution audit: " + e.getMessage());
        }

        return result;
    }
}
