package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2FinalAttemptStatusLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2ResultPublicationLedgerRecord;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;
import com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ResultPublicationLedgerDAO;

import java.sql.Connection;
import java.util.Optional;

public class V2FinalAttemptStatusExecutionService {
    public static final String TARGET_FINAL_STATUS = "COMPLETED";

    private final V2FinalAttemptStatusReadinessService readinessService;
    private final V2FinalAttemptStatusDAO attemptStatusDAO;
    private final V2FinalAttemptStatusLedgerDAO ledgerDAO;
    private final V2ResultPublicationLedgerDAO publicationLedgerDAO;

    public V2FinalAttemptStatusExecutionService(
            V2FinalAttemptStatusReadinessService readinessService,
            V2FinalAttemptStatusDAO attemptStatusDAO,
            V2FinalAttemptStatusLedgerDAO ledgerDAO,
            V2ResultPublicationLedgerDAO publicationLedgerDAO) {
        this.readinessService = readinessService;
        this.attemptStatusDAO = attemptStatusDAO;
        this.ledgerDAO = ledgerDAO;
        this.publicationLedgerDAO = publicationLedgerDAO;
    }

    public V2FinalAttemptStatusExecutionResult executeFinalStatus(int userId, long submitRecordId) {
        V2FinalAttemptStatusExecutionResult result = new V2FinalAttemptStatusExecutionResult();
        
        if (!V2SubmitFeatureFlags.isFinalAttemptStatusExecutionEnabled()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_FEATURE_DISABLED");
            return result;
        }

        // 1. Check readiness (this checks EVERYTHING including SUBMITTED current status)
        V2FinalAttemptStatusReadinessResult readiness = readinessService.checkReadiness(userId, submitRecordId);
        
        // Wait, what if it's already COMPLETED? readinessService expects SUBMITTED.
        // We handle idempotency right here if readiness fails due to INVALID_CURRENT_STATUS.
        if (!readiness.isSuccess()) {
            if ("ERROR_FINAL_ATTEMPT_STATUS_INVALID_CURRENT_STATUS".equals(readiness.getErrorCode())) {
                // Could be idempotent success
                // We must manually fetch identifiers in this case because readiness might have aborted
                return handleIdempotency(submitRecordId, result);
            } else {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_FINAL_ATTEMPT_STATUS_NOT_READY: " + readiness.getErrorCode());
                return result;
            }
        }

        String attemptId = readiness.getAttemptId();
        int examId = readiness.getExamId();
        Integer paperId = readiness.getPaperId();

        // 2. Fetch publication ledger to link
        Optional<V2ResultPublicationLedgerRecord> pubLedgerOpt = publicationLedgerDAO.findBySubmitRecordId(submitRecordId);
        if (!pubLedgerOpt.isPresent()) {
            result.setSuccess(false);
            result.setReady(true);
            result.setErrorCode("ERROR_FINAL_ATTEMPT_STATUS_PUB_LEDGER_MISSING");
            return result;
        }

        long pubLedgerId = pubLedgerOpt.get().getId();
        String payloadHash = pubLedgerOpt.get().getPayloadHash(); // Inherit hash for tracking

        // 3. Execution via Transaction
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // A. Update Attempt Status via CAS
            boolean updated = attemptStatusDAO.updateAttemptStatusIfCurrent(conn, attemptId, V2FinalAttemptStatusReadinessService.EXPECTED_CURRENT_STATUS, TARGET_FINAL_STATUS);
            if (!updated) {
                conn.rollback();
                result.setSuccess(false);
                result.setReady(true);
                result.setErrorCode("ERROR_FINAL_ATTEMPT_STATUS_CAS_UPDATE_FAILED");
                return result;
            }

            // B. Insert Ledger
            V2FinalAttemptStatusLedgerRecord record = new V2FinalAttemptStatusLedgerRecord();
            record.setSubmitRecordId(submitRecordId);
            record.setUserId(userId);
            record.setExamId(examId);
            record.setPaperId(paperId);
            record.setAttemptId(attemptId);
            record.setFromStatus(V2FinalAttemptStatusReadinessService.EXPECTED_CURRENT_STATUS);
            record.setToStatus(TARGET_FINAL_STATUS);
            record.setPublicationLedgerId(pubLedgerId);
            record.setPayloadHash(payloadHash);
            record.setStatusUpdateStatus("ATTEMPT_STATUS_COMPLETED");

            long ledgerId = ledgerDAO.insertExecutionLedger(conn, record);
            if (ledgerId <= 0) {
                conn.rollback();
                result.setSuccess(false);
                result.setReady(true);
                result.setErrorCode("ERROR_FINAL_ATTEMPT_STATUS_LEDGER_INSERT_FAILED");
                return result;
            }

            conn.commit();

            result.setSuccess(true);
            result.setReady(true);
            result.setIdempotent(false);
            result.setExecutionLedgerId(ledgerId);
            result.setExecutionStatus("ATTEMPT_STATUS_COMPLETED");
            result.setActualAttemptStatus(TARGET_FINAL_STATUS);

        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ex) { ex.printStackTrace(); }
            }
            result.setSuccess(false);
            result.setReady(true);
            result.setErrorCode("ERROR_FINAL_ATTEMPT_STATUS_EXCEPTION");
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception ex) { ex.printStackTrace(); }
            }
        }

        return result;
    }

    private V2FinalAttemptStatusExecutionResult handleIdempotency(long submitRecordId, V2FinalAttemptStatusExecutionResult result) {
        // Fetch ledger
        Optional<V2FinalAttemptStatusLedgerRecord> ledgerOpt = ledgerDAO.findBySubmitRecordId(submitRecordId);
        if (!ledgerOpt.isPresent()) {
            // Attempt is not SUBMITTED, but ledger doesn't exist. This is UNSAFE.
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_FINAL_ATTEMPT_STATUS_UNSAFE_MISSING_LEDGER");
            return result;
        }

        V2FinalAttemptStatusLedgerRecord ledger = ledgerOpt.get();
        String attemptId = ledger.getAttemptId();

        Optional<String> statusOpt = attemptStatusDAO.findAttemptStatus(attemptId);
        if (!statusOpt.isPresent()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_FINAL_ATTEMPT_STATUS_UNSAFE_ATTEMPT_NOT_FOUND");
            return result;
        }

        String currentStatus = statusOpt.get();
        if (TARGET_FINAL_STATUS.equals(currentStatus)) {
            // Both are COMPLETED -> Idempotent success
            result.setSuccess(true);
            result.setReady(true);
            result.setIdempotent(true);
            result.setExecutionLedgerId(ledger.getId());
            result.setExecutionStatus(ledger.getStatusUpdateStatus());
            result.setActualAttemptStatus(TARGET_FINAL_STATUS);
            return result;
        } else {
            // Ledger exists but attempt is not COMPLETED -> UNSAFE
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_FINAL_ATTEMPT_STATUS_UNSAFE_LEDGER_EXISTS_BUT_ATTEMPT_NOT_COMPLETED");
            return result;
        }
    }
}
