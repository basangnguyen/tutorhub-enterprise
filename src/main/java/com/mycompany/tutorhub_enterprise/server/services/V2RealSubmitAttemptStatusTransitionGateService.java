package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2RealSubmitTransitionDraftRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2RealSubmitTransitionDraftDAO;

import java.util.Optional;

public class V2RealSubmitAttemptStatusTransitionGateService {

    private final V2RealSubmitTransitionDraftDAO draftDAO;
    private final V2RealSubmitPreflightService preflightService;

    public V2RealSubmitAttemptStatusTransitionGateService() {
        this.draftDAO = new V2RealSubmitTransitionDraftDAO();
        this.preflightService = new V2RealSubmitPreflightService();
    }

    public V2RealSubmitAttemptStatusTransitionGateService(V2RealSubmitTransitionDraftDAO draftDAO, V2RealSubmitPreflightService preflightService) {
        this.draftDAO = draftDAO;
        this.preflightService = preflightService;
    }

    public V2RealSubmitAttemptStatusTransitionGateResult checkGate(int userId, long submitRecordId) {
        V2RealSubmitAttemptStatusTransitionGateResult result = new V2RealSubmitAttemptStatusTransitionGateResult();

        try {
            if (!V2SubmitFeatureFlags.isRealSubmitAttemptStatusTransitionGateEnabled()) {
                result.setErrorCode("ERROR_FEATURE_DISABLED");
                result.addBlockingReason("Transition Gate feature flag is disabled.");
                return result;
            }

            // 1. Verify Preflight NO GRADING pipeline
            V2RealSubmitPreflightResult preflightResult = preflightService.checkPreflight(userId, submitRecordId);
            if (!preflightResult.isReady() || !"READY_FOR_REAL_SUBMIT_DRAFT".equals(preflightResult.getPreflightStatus())) {
                result.setErrorCode("ERROR_PREFLIGHT_NOT_READY");
                result.addBlockingReason("Preflight is not in READY_FOR_REAL_SUBMIT_DRAFT status.");
                return result;
            }

            // 2. Verify Transition Draft exists and is correct
            Optional<V2RealSubmitTransitionDraftRecord> draftOpt = draftDAO.findBySubmitRecordId(submitRecordId);
            if (draftOpt.isEmpty()) {
                result.setErrorCode("ERROR_TRANSITION_DRAFT_MISSING");
                result.addBlockingReason("Transition draft does not exist for submitRecordId: " + submitRecordId);
                return result;
            }

            V2RealSubmitTransitionDraftRecord draft = draftOpt.get();

            if (draft.getUserId() != userId) {
                result.setErrorCode("ERROR_USER_MISMATCH");
                result.addBlockingReason("Transition draft belongs to a different user.");
                return result;
            }

            if (!"REAL_SUBMIT_TRANSITION_DRAFTED".equals(draft.getTransitionDraftStatus())) {
                result.setErrorCode("ERROR_INVALID_DRAFT_STATUS");
                result.addBlockingReason("Transition draft status is not REAL_SUBMIT_TRANSITION_DRAFTED.");
                return result;
            }

            // If all checks pass
            result.setReady(true);
            result.setStatusTransitionGate("READY_FOR_ATTEMPT_STATUS_TRANSITION_DRAFT");

        } catch (Exception e) {
            e.printStackTrace();
            result.setErrorCode("ERROR_EXCEPTION");
            result.addBlockingReason("Exception during transition gate check: " + e.getMessage());
        }

        return result;
    }
}
