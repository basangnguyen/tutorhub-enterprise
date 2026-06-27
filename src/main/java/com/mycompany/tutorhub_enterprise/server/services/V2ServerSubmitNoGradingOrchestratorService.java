package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.client.exam.ui.TSEV2SubmitPayload;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitDryRunRecord;
import com.mycompany.tutorhub_enterprise.server.services.V2SubmitDryRunValidationResult;
import com.mycompany.tutorhub_enterprise.server.services.V2SubmitDryRunPersistenceResult;

public class V2ServerSubmitNoGradingOrchestratorService {

    private final V2SubmitDryRunValidationService validationService;
    private final V2SubmitDryRunPersistenceService persistenceService;
    private final V2SubmitRecordService submitRecordService;
    private final V2AttemptFinalizationDraftService draftService;
    private final V2AttemptFinalizationLedgerService ledgerService;
    private final V2AttemptClosureDraftService closureService;

    public V2ServerSubmitNoGradingOrchestratorService() {
        this.validationService = new V2SubmitDryRunValidationService();
        this.persistenceService = new V2SubmitDryRunPersistenceService();
        this.submitRecordService = new V2SubmitRecordService();
        this.draftService = new V2AttemptFinalizationDraftService();
        this.ledgerService = new V2AttemptFinalizationLedgerService();
        this.closureService = new V2AttemptClosureDraftService();
    }

    // For testing
    public V2ServerSubmitNoGradingOrchestratorService(
            V2SubmitDryRunValidationService validationService,
            V2SubmitDryRunPersistenceService persistenceService,
            V2SubmitRecordService submitRecordService,
            V2AttemptFinalizationDraftService draftService,
            V2AttemptFinalizationLedgerService ledgerService,
            V2AttemptClosureDraftService closureService) {
        this.validationService = validationService;
        this.persistenceService = persistenceService;
        this.submitRecordService = submitRecordService;
        this.draftService = draftService;
        this.ledgerService = ledgerService;
        this.closureService = closureService;
    }

    public V2ServerSubmitNoGradingOrchestratorResult runNoGradingServerFlow(int userId, TSEV2SubmitPayload payload) {
        V2ServerSubmitNoGradingOrchestratorResult result = new V2ServerSubmitNoGradingOrchestratorResult();
        int stepsCompleted = 0;

        boolean enabled = V2SubmitFeatureFlags.isServerNoGradingOrchestratorEnabled();
        if (!enabled) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_FEATURE_DISABLED");
            result.setStepsCompleted(stepsCompleted);
            return result;
        }

        try {
            // Step 1: Validate payload
            V2SubmitDryRunValidationResult validationResult = validationService.validateDryRun(userId, payload);
            if (!validationResult.isSuccess()) {
                result.setSuccess(false);
                result.setErrorCode("ERROR_V2_ORCHESTRATION_VALIDATION_FAILED");
                result.setStepsCompleted(stepsCompleted);
                return result;
            }
            stepsCompleted++;

            // Step 2: Persist dry-run
            V2SubmitDryRunPersistenceResult dryRunResult = persistenceService.persistDryRun(userId, payload);
            if (!dryRunResult.isSuccess() || dryRunResult.getRecordId() <= 0) {
                result.setSuccess(false);
                result.setErrorCode("ERROR_V2_ORCHESTRATION_DRYRUN_PERSIST_FAILED");
                result.setStepsCompleted(stepsCompleted);
                return result;
            }
            result.setDryRunRecordId(dryRunResult.getRecordId());
            stepsCompleted++;

            // Step 3: Create submit record
            V2SubmitRecordResult submitResult = submitRecordService.createSubmitRecord(userId, payload);
            if (!submitResult.isSuccess()) {
                result.setSuccess(false);
                result.setErrorCode("ERROR_V2_ORCHESTRATION_SUBMIT_RECORD_FAILED");
                result.setStepsCompleted(stepsCompleted);
                return result;
            }
            long submitRecordId = submitResult.getSubmitRecordId();
            result.setSubmitRecordId(submitRecordId);
            stepsCompleted++;

            // Step 4: Create finalization draft
            V2AttemptFinalizationDraftResult draftResult = draftService.createFinalizationDraft(userId, submitRecordId);
            if (!draftResult.isSuccess()) {
                result.setSuccess(false);
                result.setErrorCode("ERROR_V2_ORCHESTRATION_FINALIZATION_DRAFT_FAILED");
                result.setStepsCompleted(stepsCompleted);
                return result;
            }
            stepsCompleted++;

            // Step 5: Create finalization ledger
            V2AttemptFinalizationLedgerResult ledgerResult = ledgerService.createLedgerAfterFinalizationDraft(userId, submitRecordId);
            if (!ledgerResult.isSuccess()) {
                result.setSuccess(false);
                result.setErrorCode("ERROR_V2_ORCHESTRATION_LEDGER_FAILED");
                result.setStepsCompleted(stepsCompleted);
                return result;
            }
            long ledgerId = ledgerResult.getLedgerId();
            result.setLedgerId(ledgerId);
            stepsCompleted++;

            // Step 6: Create closure draft
            V2AttemptClosureDraftResult closureResult = closureService.createClosureDraft(userId, submitRecordId);
            if (!closureResult.isSuccess()) {
                result.setSuccess(false);
                result.setErrorCode("ERROR_V2_ORCHESTRATION_CLOSURE_FAILED");
                result.setStepsCompleted(stepsCompleted);
                return result;
            }
            result.setClosureDraftId(closureResult.getClosureDraftId());
            stepsCompleted++;

            // Full success
            result.setSuccess(true);
            result.setExamId(closureResult.getExamId());
            result.setPaperId(closureResult.getPaperId());
            result.setAttemptId(closureResult.getAttemptId());
            result.setPayloadHash(closureResult.getPayloadHash());
            result.setFinalStatus("CLOSURE_DRAFTED_NO_GRADING");
            result.setStepsCompleted(stepsCompleted);
            result.setCreatedAt(closureResult.getCreatedAt());
            return result;

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_V2_ORCHESTRATION_UNSAFE");
            result.setStepsCompleted(stepsCompleted);
            return result;
        }
    }
}
