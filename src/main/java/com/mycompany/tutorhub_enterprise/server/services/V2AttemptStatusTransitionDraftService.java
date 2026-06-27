package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptStatusTransitionDraftRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2RealSubmitTransitionDraftRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptStatusTransitionDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2RealSubmitTransitionDraftDAO;

import java.util.Optional;

public class V2AttemptStatusTransitionDraftService {

    private final V2AttemptStatusTransitionDraftDAO attemptStatusDraftDAO;
    private final V2RealSubmitAttemptStatusTransitionGateService gateService;
    private final V2RealSubmitTransitionDraftDAO transitionDraftDAO;

    public V2AttemptStatusTransitionDraftService() {
        this.attemptStatusDraftDAO = new V2AttemptStatusTransitionDraftDAO();
        this.gateService = new V2RealSubmitAttemptStatusTransitionGateService();
        this.transitionDraftDAO = new V2RealSubmitTransitionDraftDAO();
    }

    public V2AttemptStatusTransitionDraftService(
            V2AttemptStatusTransitionDraftDAO attemptStatusDraftDAO,
            V2RealSubmitAttemptStatusTransitionGateService gateService,
            V2RealSubmitTransitionDraftDAO transitionDraftDAO) {
        this.attemptStatusDraftDAO = attemptStatusDraftDAO;
        this.gateService = gateService;
        this.transitionDraftDAO = transitionDraftDAO;
    }

    public V2AttemptStatusTransitionDraftResult createDraft(int userId, long submitRecordId) {
        V2AttemptStatusTransitionDraftResult result = new V2AttemptStatusTransitionDraftResult();

        try {
            if (!V2SubmitFeatureFlags.isAttemptStatusTransitionDraftEnabled()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_FEATURE_DISABLED");
                result.addBlockingReason("Attempt Status Transition Draft feature flag is disabled.");
                return result;
            }

            // 1. Check Gate
            V2RealSubmitAttemptStatusTransitionGateResult gateResult = gateService.checkGate(userId, submitRecordId);
            if (!gateResult.isReady()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_GATE_NOT_READY");
                result.addBlockingReason("Attempt Status Transition Gate rejected the request.");
                return result;
            }

            // 2. We know transition draft exists from the gate, but we need its data to build our draft
            Optional<V2RealSubmitTransitionDraftRecord> transitionDraftOpt = transitionDraftDAO.findBySubmitRecordId(submitRecordId);
            if (transitionDraftOpt.isEmpty()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_TRANSITION_DRAFT_MISSING");
                result.addBlockingReason("Real submit transition draft not found during attempt status draft creation.");
                return result;
            }

            V2RealSubmitTransitionDraftRecord transitionDraft = transitionDraftOpt.get();

            // 3. Check for idempotent insert
            attemptStatusDraftDAO.ensureSchema();
            Optional<V2AttemptStatusTransitionDraftRecord> existingDraftOpt = attemptStatusDraftDAO.findBySubmitRecordId(submitRecordId);
            if (existingDraftOpt.isPresent()) {
                V2AttemptStatusTransitionDraftRecord existing = existingDraftOpt.get();
                result.setSuccess(true);
                result.setReady(true);
                result.setIdempotent(true);
                result.setDraftId(existing.getId());
                populateResultFromRecord(result, existing);
                return result;
            }

            // 4. Create new Attempt Status Transition Draft Record
            V2AttemptStatusTransitionDraftRecord draftRecord = new V2AttemptStatusTransitionDraftRecord();
            draftRecord.setSubmitRecordId(submitRecordId);
            draftRecord.setUserId(userId);
            draftRecord.setExamId(transitionDraft.getExamId());
            draftRecord.setPaperId(transitionDraft.getPaperId());
            draftRecord.setAttemptId(transitionDraft.getAttemptId());
            draftRecord.setTransitionDraftId(transitionDraft.getId());
            draftRecord.setPayloadHash(transitionDraft.getPayloadHash());
            draftRecord.setPreflightStatus("READY_FOR_REAL_SUBMIT_DRAFT");
            draftRecord.setRealSubmitTransitionDraftStatus(transitionDraft.getTransitionDraftStatus());
            draftRecord.setAttemptStatusGateStatus(gateResult.getStatusTransitionGate());
            draftRecord.setAttemptStatusTransitionDraftStatus("ATTEMPT_STATUS_TRANSITION_DRAFTED");
            
            // Assume fromAttemptStatus is currently IN_PROGRESS or viewed from legacy if needed. 
            // For now, we put "IN_PROGRESS" or "UNKNOWN", and target is "SUBMITTED". 
            // The instructions specify targetAttemptStatus = SUBMITTED.
            draftRecord.setFromAttemptStatus("IN_PROGRESS");
            draftRecord.setTargetAttemptStatus("SUBMITTED"); // METADATA ONLY

            long insertedId = attemptStatusDraftDAO.insertDraft(draftRecord);
            if (insertedId > 0) {
                draftRecord.setId(insertedId);
                result.setSuccess(true);
                result.setReady(true);
                result.setIdempotent(false);
                result.setDraftId(insertedId);
                populateResultFromRecord(result, draftRecord);
            } else {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_INSERT_FAILED");
                result.addBlockingReason("Failed to insert attempt status transition draft.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_EXCEPTION");
            result.addBlockingReason("Exception during attempt status transition draft creation: " + e.getMessage());
        }

        return result;
    }

    private void populateResultFromRecord(V2AttemptStatusTransitionDraftResult result, V2AttemptStatusTransitionDraftRecord record) {
        result.setSubmitRecordId(record.getSubmitRecordId());
        result.setUserId(record.getUserId());
        result.setExamId(record.getExamId());
        result.setPaperId(record.getPaperId());
        result.setAttemptId(record.getAttemptId());
        result.setTransitionDraftId(record.getTransitionDraftId());
        result.setPayloadHash(record.getPayloadHash());
        result.setPreflightStatus(record.getPreflightStatus());
        result.setRealSubmitTransitionDraftStatus(record.getRealSubmitTransitionDraftStatus());
        result.setAttemptStatusGateStatus(record.getAttemptStatusGateStatus());
        result.setAttemptStatusTransitionDraftStatus(record.getAttemptStatusTransitionDraftStatus());
        result.setFromAttemptStatus(record.getFromAttemptStatus());
        result.setTargetAttemptStatus(record.getTargetAttemptStatus());
        result.setCreatedAt(record.getCreatedAt());
    }
}
