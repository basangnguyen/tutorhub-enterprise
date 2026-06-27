package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2ManualCandidateExecutionLedgerRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ManualCandidateExecutionLedgerDAO;
import java.sql.Timestamp;
import java.util.Optional;

public class V2ManualCandidateSubmitExecutionService {

    private final V2CandidateSubmitOrchestratorGateService gateService;
    private final V2ManualCandidateExecutionLedgerDAO executionLedgerDAO;

    public V2ManualCandidateSubmitExecutionService() {
        this.gateService = new V2CandidateSubmitOrchestratorGateService();
        this.executionLedgerDAO = new V2ManualCandidateExecutionLedgerDAO();
        this.executionLedgerDAO.ensureSchema();
    }

    public V2ManualCandidateSubmitExecutionService(
            V2CandidateSubmitOrchestratorGateService gateService,
            V2ManualCandidateExecutionLedgerDAO executionLedgerDAO) {
        this.gateService = gateService;
        this.executionLedgerDAO = executionLedgerDAO;
        if (this.executionLedgerDAO != null) {
            this.executionLedgerDAO.ensureSchema();
        }
    }

    public V2ManualCandidateSubmitExecutionResult executeManualCandidateSubmit(int userId, String attemptId, String payloadJson) {
        V2ManualCandidateSubmitExecutionResult result = new V2ManualCandidateSubmitExecutionResult();
        result.setUserId(userId);
        result.setAttemptId(attemptId);
        result.setCheckedAt(new Timestamp(System.currentTimeMillis()));

        try {
            if (!V2SubmitFeatureFlags.isManualCandidateSubmitExecutionEnabled()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_FEATURE_DISABLED");
                result.setExecutionStatus("NOT_READY");
                result.addBlockingReason("Manual Candidate Submit Execution feature flag is disabled.");
                return result;
            }

            // Check if already executed/prepared (Idempotency)
            Optional<V2ManualCandidateExecutionLedgerRecord> existingOpt = executionLedgerDAO.findByAttemptId(attemptId);
            if (existingOpt.isPresent()) {
                V2ManualCandidateExecutionLedgerRecord existing = existingOpt.get();
                if ("PREPARE_ONLY".equals(existing.getExecutionMode()) && 
                    "MANUAL_CANDIDATE_V2_SUBMIT_PREPARED_ONLY".equals(existing.getExecutionStatus())) {
                    
                    result.setSuccess(true);
                    result.setReady(true);
                    result.setExecuted(false);
                    result.setIdempotent(true);
                    result.setExamId(existing.getExamId());
                    result.setPaperId(existing.getPaperId());
                    result.setManualExecutionLedgerId(existing.getId());
                    result.setExecutionStatus(existing.getExecutionStatus());
                    result.setExecutionMode(existing.getExecutionMode());
                    return result;
                }
            }

            // Gate Check
            V2CandidateSubmitOrchestratorGateResult gateResult = gateService.checkGate(userId, attemptId);
            if (!gateResult.isSuccess() || !gateResult.isReady()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_MANUAL_CANDIDATE_GATE_NOT_READY");
                result.setExecutionStatus("NOT_READY");
                result.getBlockingReasons().addAll(gateResult.getBlockingReasons());
                result.addBlockingReason("Candidate submit orchestrator gate is not ready.");
                return result;
            }
            
            result.setExamId(gateResult.getExamId());
            result.setPaperId(gateResult.getPaperId());

            // Prepare-only logic: we do not parse payload fully yet as schema is pending
            if (payloadJson == null || payloadJson.trim().isEmpty()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_INVALID_PAYLOAD");
                result.setExecutionStatus("NOT_READY");
                result.addBlockingReason("Payload is missing or empty.");
                return result;
            }

            // Cannot full chain due to pending answerKeyResolver/payloadParser in V2ScoreDraftService
            result.setSuccess(true);
            result.setReady(true);
            result.setExecuted(false); // Prepare only
            result.setExecutionStatus("MANUAL_CANDIDATE_V2_SUBMIT_PREPARED_ONLY");
            result.setExecutionMode("PREPARE_ONLY");
            result.addWarning("Cannot proceed to full execution because answerKeyResolver and payloadParser are pending schema implementation.");

            V2ManualCandidateExecutionLedgerRecord ledgerRecord = new V2ManualCandidateExecutionLedgerRecord();
            ledgerRecord.setAttemptId(attemptId);
            ledgerRecord.setUserId(userId);
            ledgerRecord.setExamId(gateResult.getExamId());
            ledgerRecord.setPaperId(gateResult.getPaperId());
            ledgerRecord.setExecutionStatus("MANUAL_CANDIDATE_V2_SUBMIT_PREPARED_ONLY");
            ledgerRecord.setExecutionMode("PREPARE_ONLY");
            
            boolean inserted = executionLedgerDAO.insertLedger(ledgerRecord);
            if (!inserted) {
                // Double-check idempotency
                Optional<V2ManualCandidateExecutionLedgerRecord> fallbackOpt = executionLedgerDAO.findByAttemptId(attemptId);
                if (fallbackOpt.isPresent()) {
                    V2ManualCandidateExecutionLedgerRecord existing = fallbackOpt.get();
                    result.setIdempotent(true);
                    result.setManualExecutionLedgerId(existing.getId());
                    return result;
                }
                
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_LEDGER_INSERT_FAILED");
                result.setExecutionStatus("NOT_READY");
                result.addBlockingReason("Failed to insert manual execution ledger record.");
                return result;
            }
            
            result.setManualExecutionLedgerId(ledgerRecord.getId());

        } catch (Exception e) {
            e.printStackTrace();
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_EXCEPTION");
            result.setExecutionStatus("NOT_READY");
            result.addBlockingReason("Exception during manual candidate submit execution: " + e.getMessage());
        }

        return result;
    }
}
