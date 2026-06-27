package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptStatusExecutionLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptStatusExecutionLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamAttemptStatusDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;

import java.util.Optional;

public class V2PostSubmitIntegrityAuditService {

    private final V2SubmitRecordDAO submitRecordDAO;
    private final V2AttemptStatusExecutionLedgerDAO ledgerDAO;
    private final V2ExamAttemptStatusDAO attemptStatusDAO;

    public V2PostSubmitIntegrityAuditService() {
        this.submitRecordDAO = new V2SubmitRecordDAO();
        this.ledgerDAO = new V2AttemptStatusExecutionLedgerDAO();
        this.attemptStatusDAO = new V2ExamAttemptStatusDAO();
    }

    public V2PostSubmitIntegrityAuditService(
            V2SubmitRecordDAO submitRecordDAO,
            V2AttemptStatusExecutionLedgerDAO ledgerDAO,
            V2ExamAttemptStatusDAO attemptStatusDAO) {
        this.submitRecordDAO = submitRecordDAO;
        this.ledgerDAO = ledgerDAO;
        this.attemptStatusDAO = attemptStatusDAO;
    }

    public V2PostSubmitIntegrityAuditResult audit(int userId, long submitRecordId) {
        V2PostSubmitIntegrityAuditResult result = new V2PostSubmitIntegrityAuditResult();
        result.setUserId(userId);
        result.setSubmitRecordId(submitRecordId);

        try {
            if (!V2SubmitFeatureFlags.isPostSubmitIntegrityAuditEnabled()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_FEATURE_DISABLED");
                result.setAuditStatus("NOT_READY");
                result.addBlockingReason("Post-Submit Integrity Audit feature flag is disabled.");
                return result;
            }

            Optional<V2SubmitRecord> submitRecordOpt = submitRecordDAO.findById(submitRecordId);
            if (submitRecordOpt.isEmpty()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_POST_SUBMIT_AUDIT_MISSING_SUBMIT_RECORD");
                result.setAuditStatus("NOT_READY");
                result.addBlockingReason("Submit record not found.");
                return result;
            }

            V2SubmitRecord submitRecord = submitRecordOpt.get();

            if (submitRecord.getUserId() != userId) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_POST_SUBMIT_AUDIT_USER_MISMATCH");
                result.setAuditStatus("NOT_READY");
                result.addBlockingReason("User mismatch for the given submit record.");
                return result;
            }

            result.setExamId(submitRecord.getExamId());
            result.setPaperId(submitRecord.getPaperId());
            result.setAttemptId(submitRecord.getAttemptId());
            result.setPayloadHash(submitRecord.getPayloadHash());

            if (submitRecord.getPayloadHash() == null || submitRecord.getPayloadHash().length() != 64) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_POST_SUBMIT_AUDIT_INVALID_HASH");
                result.setAuditStatus("NOT_READY");
                result.addBlockingReason("Payload hash is not a valid SHA-256.");
                return result;
            }

            // Ledger check
            Optional<V2AttemptStatusExecutionLedgerRecord> ledgerOpt = ledgerDAO.findBySubmitRecordId(submitRecordId);
            if (ledgerOpt.isEmpty()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setLedgerExists(false);
                result.setErrorCode("ERROR_V2_POST_SUBMIT_AUDIT_MISSING_LEDGER");
                result.setAuditStatus("NOT_READY");
                result.addBlockingReason("Execution ledger record missing.");
                return result;
            }

            V2AttemptStatusExecutionLedgerRecord ledger = ledgerOpt.get();
            result.setLedgerExists(true);
            result.setExecutionStatus(ledger.getExecutionStatus());

            // Check actual status
            Optional<String> statusOpt = attemptStatusDAO.findAttemptStatus(submitRecord.getAttemptId());
            if (statusOpt.isEmpty()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_POST_SUBMIT_AUDIT_MISSING_ATTEMPT");
                result.setAuditStatus("NOT_READY");
                result.addBlockingReason("Exam attempt not found in database.");
                return result;
            }

            String status = statusOpt.get();
            result.setAttemptStatus(status);

            if (!"SUBMITTED".equals(status)) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_POST_SUBMIT_AUDIT_NOT_SUBMITTED");
                result.setAuditStatus("NOT_READY");
                result.addBlockingReason("Attempt status is not SUBMITTED.");
                return result;
            }

            result.setSuccess(true);
            result.setReady(true);
            result.setAuditStatus("POST_SUBMIT_INTEGRITY_READY");
            result.setReadinessStatus("POST_SUBMIT_INTEGRITY_READY");

        } catch (Exception e) {
            e.printStackTrace();
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_EXCEPTION");
            result.setAuditStatus("NOT_READY");
            result.addBlockingReason("Exception during post-submit integrity audit: " + e.getMessage());
        }

        return result;
    }
}
