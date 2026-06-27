package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2RealSubmitTransitionDraftRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2RealSubmitTransitionDraftDAO;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

public class V2RealSubmitTransitionDraftService {

    private final V2RealSubmitTransitionDraftDAO transitionDraftDAO;
    private final V2RealSubmitPreflightService preflightService;

    public V2RealSubmitTransitionDraftService() {
        this(new V2RealSubmitTransitionDraftDAO(), new V2RealSubmitPreflightService());
    }

    public V2RealSubmitTransitionDraftService(V2RealSubmitTransitionDraftDAO transitionDraftDAO, V2RealSubmitPreflightService preflightService) {
        this.transitionDraftDAO = transitionDraftDAO;
        this.preflightService = preflightService;
    }

    public V2RealSubmitTransitionDraftResult createDraft(int userId, long submitRecordId) {
        V2RealSubmitTransitionDraftResult result = new V2RealSubmitTransitionDraftResult();
        result.setCreatedAt(Instant.now().toString());
        result.setUserId(userId);
        result.setSubmitRecordId(submitRecordId);

        try {
            // 1. Check feature flag
            if (!V2SubmitFeatureFlags.isRealSubmitTransitionDraftEnabled()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_FEATURE_DISABLED");
                result.setTransitionDraftStatus("NOT_CREATED");
                result.addBlockingReason("V2 Real Submit Transition Draft feature flag is disabled.");
                return result;
            }

            // 2. Check idempotent existence
            Optional<V2RealSubmitTransitionDraftRecord> existingDraft = transitionDraftDAO.findBySubmitRecordId(submitRecordId);
            if (existingDraft.isPresent()) {
                V2RealSubmitTransitionDraftRecord existing = existingDraft.get();
                result.setSuccess(true);
                result.setReady(true);
                result.setIdempotent(true);
                result.setTransitionDraftId(existing.getId());
                result.setExamId(existing.getExamId());
                result.setPaperId(existing.getPaperId());
                result.setAttemptId(existing.getAttemptId());
                result.setLedgerId(existing.getLedgerId());
                result.setClosureDraftId(existing.getClosureDraftId());
                result.setPayloadHash(existing.getPayloadHash());
                result.setPreflightStatus(existing.getPreflightStatus());
                result.setTransitionDraftStatus(existing.getTransitionDraftStatus());
                return result;
            }

            // 3. Check Preflight
            V2RealSubmitPreflightResult preflightResult = preflightService.checkPreflight(userId, submitRecordId);
            
            if (!preflightResult.isSuccess() || !preflightResult.isReady() || !"READY_FOR_REAL_SUBMIT_DRAFT".equals(preflightResult.getPreflightStatus())) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode(preflightResult.getErrorCode() != null ? preflightResult.getErrorCode() : "ERROR_V2_REAL_TRANSITION_PREFLIGHT_NOT_READY");
                result.setTransitionDraftStatus("NOT_CREATED");
                result.setBlockingReasons(preflightResult.getBlockingReasons());
                result.addBlockingReason("Preflight check failed or is not ready.");
                return result;
            }

            // 4. Set fields from preflight
            result.setExamId(preflightResult.getExamId());
            result.setPaperId(preflightResult.getPaperId());
            result.setAttemptId(preflightResult.getAttemptId());
            result.setLedgerId(preflightResult.getLedgerId());
            result.setClosureDraftId(preflightResult.getClosureDraftId());
            result.setPayloadHash(preflightResult.getPayloadHash());
            result.setPreflightStatus(preflightResult.getPreflightStatus());

            // 5. Create Draft Record
            V2RealSubmitTransitionDraftRecord record = new V2RealSubmitTransitionDraftRecord();
            record.setSubmitRecordId(submitRecordId);
            record.setUserId(userId);
            record.setExamId(preflightResult.getExamId());
            record.setPaperId(preflightResult.getPaperId());
            record.setAttemptId(preflightResult.getAttemptId());
            record.setLedgerId(preflightResult.getLedgerId());
            record.setClosureDraftId(preflightResult.getClosureDraftId());
            record.setPayloadHash(preflightResult.getPayloadHash());
            record.setPreflightStatus(preflightResult.getPreflightStatus());
            record.setTransitionDraftStatus("REAL_SUBMIT_TRANSITION_DRAFTED");

            long draftId = transitionDraftDAO.insertDraft(record);

            // 6. Return Success
            result.setSuccess(true);
            result.setReady(true);
            result.setIdempotent(false);
            result.setTransitionDraftId(draftId);
            result.setTransitionDraftStatus("REAL_SUBMIT_TRANSITION_DRAFTED");

        } catch (SQLException e) {
            e.printStackTrace();
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_V2_REAL_TRANSITION_INSERT_FAILED");
            result.setTransitionDraftStatus("NOT_CREATED");
            result.addBlockingReason("Database error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_V2_REAL_TRANSITION_NOT_READY");
            result.setTransitionDraftStatus("NOT_CREATED");
            result.addBlockingReason("Exception: " + e.getMessage());
        }

        return result;
    }
}
