package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptStatusExecutionLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptStatusExecutionLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamAttemptStatusDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;

import java.sql.Connection;
import java.util.Optional;

public class V2ManualCandidateSubmitStatusExecutionService {

    private final V2ManualCandidatePublishFinalStatusGateService gateService;
    private final V2SubmitRecordDAO submitRecordDAO;
    private final V2ExamAttemptStatusDAO examAttemptStatusDAO;
    private final V2AttemptStatusExecutionLedgerDAO ledgerDAO;

    public V2ManualCandidateSubmitStatusExecutionService() {
        this.gateService = new V2ManualCandidatePublishFinalStatusGateService();
        this.submitRecordDAO = new V2SubmitRecordDAO();
        this.examAttemptStatusDAO = new V2ExamAttemptStatusDAO();
        this.ledgerDAO = new V2AttemptStatusExecutionLedgerDAO();
    }

    public V2ManualCandidateSubmitStatusExecutionService(
            V2ManualCandidatePublishFinalStatusGateService gateService,
            V2SubmitRecordDAO submitRecordDAO,
            V2ExamAttemptStatusDAO examAttemptStatusDAO,
            V2AttemptStatusExecutionLedgerDAO ledgerDAO) {
        this.gateService = gateService;
        this.submitRecordDAO = submitRecordDAO;
        this.examAttemptStatusDAO = examAttemptStatusDAO;
        this.ledgerDAO = ledgerDAO;
    }

    public V2ManualCandidateSubmitStatusExecutionResult executeSubmitStatus(int userId, long submitRecordId) {
        V2ManualCandidateSubmitStatusExecutionResult result = new V2ManualCandidateSubmitStatusExecutionResult();
        result.setUserId(userId);
        result.setSubmitRecordId(submitRecordId);

        try {
            if (!V2SubmitFeatureFlags.isManualCandidateSubmitStatusExecutionEnabled()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_FEATURE_DISABLED");
                result.addBlockingReason("Manual Candidate Submit Status Execution is disabled.");
                return result;
            }

            // 1. Fetch Submit Record to get payload hash and attemptId
            Optional<V2SubmitRecord> submitRecordOpt = submitRecordDAO.findById(submitRecordId);
            if (submitRecordOpt.isEmpty()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_SUBMIT_RECORD_MISSING");
                result.addBlockingReason("Submit record not found.");
                return result;
            }
            V2SubmitRecord submitRecord = submitRecordOpt.get();

            // 2. Check Gate Readiness
            V2ManualCandidatePublishFinalStatusGateResult gateResult = gateService.checkGate(userId, submitRecord.getAttemptId());
            if (!gateResult.isSuccess() || !gateResult.isReady()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_MANUAL_CANDIDATE_GATE_NOT_READY");
                result.addBlockingReason("Publish Final Status Gate is not ready.");
                return result;
            }

            // Populate metadata
            result.setExamId(submitRecord.getExamId());
            result.setPaperId(submitRecord.getPaperId());
            result.setAttemptId(submitRecord.getAttemptId());

            // 3. Check Current Status
            Optional<String> currentStatusOpt = examAttemptStatusDAO.findAttemptStatus(submitRecord.getAttemptId());
            if (currentStatusOpt.isEmpty()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_ATTEMPT_NOT_FOUND");
                result.addBlockingReason("Exam attempt not found.");
                return result;
            }

            String currentStatus = currentStatusOpt.get();
            ledgerDAO.ensureSchema();
            
            // Check idempotency
            if ("SUBMITTED".equals(currentStatus)) {
                Optional<V2AttemptStatusExecutionLedgerRecord> existingLedgerOpt = ledgerDAO.findBySubmitRecordId(submitRecordId);
                if (existingLedgerOpt.isPresent()) {
                    V2AttemptStatusExecutionLedgerRecord existing = existingLedgerOpt.get();
                    result.setSuccess(true);
                    result.setReady(true);
                    result.setIdempotent(true);
                    result.setExecutionId(existing.getId());
                    result.setActualAttemptStatus("SUBMITTED");
                    result.setExecutionStatus(existing.getExecutionStatus());
                    result.setExecutedAt(existing.getExecutedAt());
                    return result;
                } else {
                    result.setSuccess(false);
                    result.setReady(false);
                    result.setErrorCode("ERROR_UNSAFE_STATE_MISSING_LEDGER");
                    result.addBlockingReason("Status is SUBMITTED but execution ledger is missing.");
                    return result;
                }
            }
            
            if ("COMPLETED".equals(currentStatus) || "GRADED".equals(currentStatus)) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_UNSAFE_STATE_ALREADY_COMPLETED");
                result.addBlockingReason("Status is " + currentStatus + ", which is past SUBMITTED.");
                return result;
            }
            
            if (!("IN_PROGRESS".equals(currentStatus) || "STARTING".equals(currentStatus) || "STARTED".equals(currentStatus) || "DOING".equals(currentStatus) || "IN_EXAM".equals(currentStatus))) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_CURRENT_STATUS_INVALID");
                result.addBlockingReason("Current status cannot transition to SUBMITTED: " + currentStatus);
                return result;
            }

            // 4. Transactional Update & Ledger Insert
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false); // Start Transaction
                try {
                    boolean updated = examAttemptStatusDAO.updateAttemptStatusIfCurrent(conn, submitRecord.getAttemptId(), currentStatus, "SUBMITTED");
                    if (!updated) {
                        conn.rollback();
                        result.setSuccess(false);
                        result.setReady(false);
                        result.setErrorCode("ERROR_UPDATE_FAILED");
                        result.addBlockingReason("Failed to update status via CAS.");
                        return result;
                    }

                    V2AttemptStatusExecutionLedgerRecord ledgerRecord = new V2AttemptStatusExecutionLedgerRecord();
                    ledgerRecord.setSubmitRecordId(submitRecordId);
                    ledgerRecord.setUserId(userId);
                    ledgerRecord.setExamId(submitRecord.getExamId());
                    ledgerRecord.setPaperId(submitRecord.getPaperId());
                    ledgerRecord.setAttemptId(submitRecord.getAttemptId());
                    ledgerRecord.setPayloadHash(submitRecord.getPayloadHash());
                    ledgerRecord.setFromAttemptStatus(currentStatus);
                    ledgerRecord.setTargetAttemptStatus("SUBMITTED");
                    ledgerRecord.setActualAttemptStatus("SUBMITTED");
                    ledgerRecord.setExecutionStatus("ATTEMPT_STATUS_EXECUTED_SUBMITTED");
                    // No AttemptStatusTransitionDraftId since manual flow bypasses it

                    long ledgerId = ledgerDAO.insertExecutionLedger(conn, ledgerRecord);
                    if (ledgerId <= 0) {
                        conn.rollback();
                        result.setSuccess(false);
                        result.setReady(false);
                        result.setErrorCode("ERROR_LEDGER_INSERT_FAILED");
                        result.addBlockingReason("Failed to insert execution ledger.");
                        return result;
                    }

                    conn.commit();

                    result.setSuccess(true);
                    result.setReady(true);
                    result.setIdempotent(false);
                    result.setExecutionId(ledgerId);
                    result.setActualAttemptStatus("SUBMITTED");
                    result.setExecutionStatus("ATTEMPT_STATUS_EXECUTED_SUBMITTED");
                } catch (Exception innerEx) {
                    conn.rollback();
                    throw innerEx;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_EXCEPTION");
            result.addBlockingReason("Exception: " + e.getMessage());
        }

        return result;
    }
}
