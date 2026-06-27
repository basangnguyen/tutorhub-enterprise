package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptStatusExecutionLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptStatusTransitionDraftRecord;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptStatusExecutionLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptStatusTransitionDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamAttemptStatusDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public class V2AttemptStatusExecutionService {

    private final V2RealSubmitReadinessOrchestratorService orchestratorService;
    private final V2AttemptStatusTransitionDraftDAO attemptStatusDraftDAO;
    private final V2ExamAttemptStatusDAO examAttemptStatusDAO;
    private final V2AttemptStatusExecutionLedgerDAO ledgerDAO;

    public V2AttemptStatusExecutionService() {
        this.orchestratorService = new V2RealSubmitReadinessOrchestratorService();
        this.attemptStatusDraftDAO = new V2AttemptStatusTransitionDraftDAO();
        this.examAttemptStatusDAO = new V2ExamAttemptStatusDAO();
        this.ledgerDAO = new V2AttemptStatusExecutionLedgerDAO();
    }

    public V2AttemptStatusExecutionService(
            V2RealSubmitReadinessOrchestratorService orchestratorService,
            V2AttemptStatusTransitionDraftDAO attemptStatusDraftDAO,
            V2ExamAttemptStatusDAO examAttemptStatusDAO,
            V2AttemptStatusExecutionLedgerDAO ledgerDAO) {
        this.orchestratorService = orchestratorService;
        this.attemptStatusDraftDAO = attemptStatusDraftDAO;
        this.examAttemptStatusDAO = examAttemptStatusDAO;
        this.ledgerDAO = ledgerDAO;
    }

    public V2AttemptStatusExecutionResult executeSubmittedStatus(int userId, long submitRecordId) {
        V2AttemptStatusExecutionResult result = new V2AttemptStatusExecutionResult();
        result.setUserId(userId);
        result.setSubmitRecordId(submitRecordId);

        try {
            if (!V2SubmitFeatureFlags.isAttemptStatusExecutionEnabled()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_FEATURE_DISABLED");
                result.addBlockingReason("Attempt Status Execution feature flag is disabled.");
                return result;
            }

            // 1. Check Orchestrator Readiness
            V2RealSubmitReadinessOrchestratorResult orchResult = orchestratorService.checkReadiness(userId, submitRecordId);
            if (!orchResult.isReady() || !"READY_FOR_REAL_SUBMIT_STATUS_EXECUTION_DRAFT".equals(orchResult.getReadinessStatus())) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_ATTEMPT_STATUS_EXECUTION_NOT_READY");
                result.addBlockingReason("Readiness orchestrator rejected the request.");
                return result;
            }

            // 2. Load Attempt Status Transition Draft
            Optional<V2AttemptStatusTransitionDraftRecord> draftOpt = attemptStatusDraftDAO.findBySubmitRecordId(submitRecordId);
            if (draftOpt.isEmpty()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_ATTEMPT_STATUS_EXECUTION_DRAFT_MISSING");
                result.addBlockingReason("Attempt status transition draft not found.");
                return result;
            }

            V2AttemptStatusTransitionDraftRecord draft = draftOpt.get();

            if (!"ATTEMPT_STATUS_TRANSITION_DRAFTED".equals(draft.getAttemptStatusTransitionDraftStatus())) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_ATTEMPT_STATUS_EXECUTION_NOT_READY");
                result.addBlockingReason("Draft is not in ATTEMPT_STATUS_TRANSITION_DRAFTED status.");
                return result;
            }

            if (!"SUBMITTED".equals(draft.getTargetAttemptStatus())) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_ATTEMPT_STATUS_EXECUTION_TARGET_INVALID");
                result.addBlockingReason("Target attempt status is not SUBMITTED.");
                return result;
            }

            if (draft.getUserId() != userId) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_ATTEMPT_STATUS_EXECUTION_USER_MISMATCH");
                result.addBlockingReason("User ID mismatch in draft.");
                return result;
            }

            // Populate some details to result
            result.setExamId(draft.getExamId());
            result.setPaperId(draft.getPaperId());
            result.setAttemptId(draft.getAttemptId());
            result.setPayloadHash(draft.getPayloadHash());

            // 3. Check Current Status
            Optional<String> currentStatusOpt = examAttemptStatusDAO.findAttemptStatus(draft.getAttemptId());
            if (currentStatusOpt.isEmpty()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_ATTEMPT_STATUS_EXECUTION_CURRENT_STATUS_INVALID");
                result.addBlockingReason("Exam attempt not found.");
                return result;
            }

            String currentStatus = currentStatusOpt.get();
            result.setFromAttemptStatus(currentStatus);
            result.setTargetAttemptStatus("SUBMITTED");

            // Idempotency Check: Already SUBMITTED
            if ("SUBMITTED".equals(currentStatus)) {
                ledgerDAO.ensureSchema();
                Optional<V2AttemptStatusExecutionLedgerRecord> existingLedgerOpt = ledgerDAO.findBySubmitRecordId(submitRecordId);
                
                if (existingLedgerOpt.isPresent()) {
                    // Safe idempotent
                    V2AttemptStatusExecutionLedgerRecord existing = existingLedgerOpt.get();
                    result.setSuccess(true);
                    result.setReady(true);
                    result.setIdempotent(true);
                    result.setExecutionId(existing.getId());
                    result.setActualAttemptStatus("SUBMITTED");
                    result.setExecutionStatus("ATTEMPT_STATUS_EXECUTED_SUBMITTED");
                    result.setExecutedAt(existing.getExecutedAt());
                    return result;
                } else {
                    // Unsafe state: Status is SUBMITTED but no ledger!
                    result.setSuccess(false);
                    result.setReady(false);
                    result.setErrorCode("ERROR_V2_ATTEMPT_STATUS_EXECUTION_MISSING_LEDGER");
                    result.addBlockingReason("Attempt is already SUBMITTED but execution ledger is missing. Rejecting as unsafe.");
                    return result;
                }
            }

            // Check if current status is allowed to transition
            if (!("IN_PROGRESS".equals(currentStatus) || "STARTING".equals(currentStatus) || "STARTED".equals(currentStatus) || "DOING".equals(currentStatus) || "IN_EXAM".equals(currentStatus))) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_ATTEMPT_STATUS_EXECUTION_CURRENT_STATUS_INVALID");
                result.addBlockingReason("Current attempt status is not allowed to transition: " + currentStatus);
                return result;
            }

            // 4. Transactional Update & Ledger Insert
            ledgerDAO.ensureSchema();

            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false); // Start Transaction

                try {
                    // Compare-And-Set
                    boolean updated = examAttemptStatusDAO.updateAttemptStatusIfCurrent(conn, draft.getAttemptId(), currentStatus, "SUBMITTED");
                    
                    if (!updated) {
                        conn.rollback();
                        result.setSuccess(false);
                        result.setReady(false);
                        result.setErrorCode("ERROR_V2_ATTEMPT_STATUS_EXECUTION_UPDATE_FAILED");
                        result.addBlockingReason("Failed to update attempt status via Compare-And-Set. Status might have changed.");
                        return result;
                    }

                    // Insert Ledger
                    V2AttemptStatusExecutionLedgerRecord ledgerRecord = new V2AttemptStatusExecutionLedgerRecord();
                    ledgerRecord.setSubmitRecordId(submitRecordId);
                    ledgerRecord.setUserId(userId);
                    ledgerRecord.setExamId(draft.getExamId());
                    ledgerRecord.setPaperId(draft.getPaperId());
                    ledgerRecord.setAttemptId(draft.getAttemptId());
                    ledgerRecord.setAttemptStatusTransitionDraftId(draft.getId());
                    ledgerRecord.setPayloadHash(draft.getPayloadHash());
                    ledgerRecord.setFromAttemptStatus(currentStatus);
                    ledgerRecord.setTargetAttemptStatus("SUBMITTED");
                    ledgerRecord.setActualAttemptStatus("SUBMITTED");
                    ledgerRecord.setExecutionStatus("ATTEMPT_STATUS_EXECUTED_SUBMITTED");

                    long ledgerId = ledgerDAO.insertExecutionLedger(conn, ledgerRecord);
                    if (ledgerId <= 0) {
                        conn.rollback();
                        result.setSuccess(false);
                        result.setReady(false);
                        result.setErrorCode("ERROR_V2_ATTEMPT_STATUS_EXECUTION_LEDGER_INSERT_FAILED");
                        result.addBlockingReason("Failed to insert execution ledger.");
                        return result;
                    }

                    conn.commit(); // Commit Transaction

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
            result.addBlockingReason("Exception during attempt status execution: " + e.getMessage());
        }

        return result;
    }
}
