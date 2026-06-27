package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptStatusTransitionDraftRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2RealSubmitTransitionDraftRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptStatusTransitionDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2RealSubmitTransitionDraftDAO;

import java.util.Optional;

public class V2RealSubmitReadinessOrchestratorService {

    private final V2RealSubmitPreflightService preflightService;
    private final V2RealSubmitTransitionDraftDAO transitionDraftDAO;
    private final V2RealSubmitAttemptStatusTransitionGateService attemptStatusGateService;
    private final V2AttemptStatusTransitionDraftDAO attemptStatusDraftDAO;

    public V2RealSubmitReadinessOrchestratorService() {
        this.preflightService = new V2RealSubmitPreflightService();
        this.transitionDraftDAO = new V2RealSubmitTransitionDraftDAO();
        this.attemptStatusGateService = new V2RealSubmitAttemptStatusTransitionGateService();
        this.attemptStatusDraftDAO = new V2AttemptStatusTransitionDraftDAO();
    }

    public V2RealSubmitReadinessOrchestratorService(
            V2RealSubmitPreflightService preflightService,
            V2RealSubmitTransitionDraftDAO transitionDraftDAO,
            V2RealSubmitAttemptStatusTransitionGateService attemptStatusGateService,
            V2AttemptStatusTransitionDraftDAO attemptStatusDraftDAO) {
        this.preflightService = preflightService;
        this.transitionDraftDAO = transitionDraftDAO;
        this.attemptStatusGateService = attemptStatusGateService;
        this.attemptStatusDraftDAO = attemptStatusDraftDAO;
    }

    public V2RealSubmitReadinessOrchestratorResult checkReadiness(int userId, long submitRecordId) {
        V2RealSubmitReadinessOrchestratorResult result = new V2RealSubmitReadinessOrchestratorResult();
        result.setUserId(userId);
        result.setSubmitRecordId(submitRecordId);

        try {
            if (!V2SubmitFeatureFlags.isRealSubmitReadinessOrchestratorEnabled()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setReadinessStatus("NOT_READY");
                result.setErrorCode("ERROR_FEATURE_DISABLED");
                result.addBlockingReason("Real Submit Readiness Orchestrator feature flag is disabled.");
                return result;
            }

            // 1. Preflight
            V2RealSubmitPreflightResult preflightResult = preflightService.checkPreflight(userId, submitRecordId);
            result.setPreflightReady(preflightResult.isReady());
            result.setPreflightStatus(preflightResult.getPreflightStatus());
            
            if (!preflightResult.isReady() || !"READY_FOR_REAL_SUBMIT_DRAFT".equals(preflightResult.getPreflightStatus())) {
                result.setSuccess(true);
                result.setReady(false);
                result.setReadinessStatus("NOT_READY");
                result.addBlockingReason("Preflight is not ready.");
                return result;
            }

            // Populate some metadata from preflight which is known good
            result.setExamId(preflightResult.getExamId());
            result.setPaperId(preflightResult.getPaperId());
            result.setAttemptId(preflightResult.getAttemptId());
            result.setPayloadHash(preflightResult.getPayloadHash());

            // 2. Real Submit Transition Draft
            Optional<V2RealSubmitTransitionDraftRecord> transitionDraftOpt = transitionDraftDAO.findBySubmitRecordId(submitRecordId);
            if (transitionDraftOpt.isEmpty()) {
                result.setSuccess(true);
                result.setReady(false);
                result.setReadinessStatus("NOT_READY");
                result.setTransitionDraftReady(false);
                result.addBlockingReason("Real Submit Transition Draft is missing.");
                return result;
            }
            
            V2RealSubmitTransitionDraftRecord transitionDraft = transitionDraftOpt.get();
            result.setTransitionDraftReady("REAL_SUBMIT_TRANSITION_DRAFTED".equals(transitionDraft.getTransitionDraftStatus()));
            result.setTransitionDraftStatus(transitionDraft.getTransitionDraftStatus());
            
            if (!result.isTransitionDraftReady()) {
                result.setSuccess(true);
                result.setReady(false);
                result.setReadinessStatus("NOT_READY");
                result.addBlockingReason("Real Submit Transition Draft is not drafted.");
                return result;
            }

            // 3. Attempt Status Transition Gate
            V2RealSubmitAttemptStatusTransitionGateResult gateResult = attemptStatusGateService.checkGate(userId, submitRecordId);
            result.setAttemptStatusGateReady(gateResult.isReady());
            result.setAttemptStatusGateStatus(gateResult.getStatusTransitionGate());
            
            if (!gateResult.isReady() || !"READY_FOR_ATTEMPT_STATUS_TRANSITION_DRAFT".equals(gateResult.getStatusTransitionGate())) {
                result.setSuccess(true);
                result.setReady(false);
                result.setReadinessStatus("NOT_READY");
                result.addBlockingReason("Attempt Status Transition Gate is not ready.");
                return result;
            }

            // 4. Attempt Status Transition Draft
            Optional<V2AttemptStatusTransitionDraftRecord> attemptStatusDraftOpt = attemptStatusDraftDAO.findBySubmitRecordId(submitRecordId);
            if (attemptStatusDraftOpt.isEmpty()) {
                result.setSuccess(true);
                result.setReady(false);
                result.setReadinessStatus("NOT_READY");
                result.setAttemptStatusTransitionDraftReady(false);
                result.addBlockingReason("Attempt Status Transition Draft is missing.");
                return result;
            }

            V2AttemptStatusTransitionDraftRecord attemptStatusDraft = attemptStatusDraftOpt.get();
            result.setAttemptStatusTransitionDraftReady("ATTEMPT_STATUS_TRANSITION_DRAFTED".equals(attemptStatusDraft.getAttemptStatusTransitionDraftStatus()));
            result.setAttemptStatusTransitionDraftStatus(attemptStatusDraft.getAttemptStatusTransitionDraftStatus());
            
            if (!result.isAttemptStatusTransitionDraftReady()) {
                result.setSuccess(true);
                result.setReady(false);
                result.setReadinessStatus("NOT_READY");
                result.addBlockingReason("Attempt Status Transition Draft is not drafted.");
                return result;
            }

            // If we made it here, everything is ready
            result.setSuccess(true);
            result.setReady(true);
            result.setReadinessStatus("READY_FOR_REAL_SUBMIT_STATUS_EXECUTION_DRAFT");

        } catch (Exception e) {
            e.printStackTrace();
            result.setSuccess(false);
            result.setReady(false);
            result.setReadinessStatus("NOT_READY");
            result.setErrorCode("ERROR_EXCEPTION");
            result.addBlockingReason("Exception during readiness orchestration: " + e.getMessage());
        }

        return result;
    }
}
