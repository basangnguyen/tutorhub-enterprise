package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;

import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class V2AttemptFinalizationDraftService {
    private static final Logger LOGGER = Logger.getLogger(V2AttemptFinalizationDraftService.class.getName());
    private final V2SubmitRecordDAO recordDAO;

    public V2AttemptFinalizationDraftService() {
        this.recordDAO = new V2SubmitRecordDAO();
    }

    public V2AttemptFinalizationDraftService(V2SubmitRecordDAO recordDAO) {
        this.recordDAO = recordDAO;
    }

    public V2AttemptFinalizationDraftResult createFinalizationDraft(int userId, long submitRecordId) {
        boolean isEnabled = V2SubmitFeatureFlags.isAttemptFinalizationDraftEnabled();
        if (!isEnabled) {
            return new V2AttemptFinalizationDraftResult(false, "ERROR_FEATURE_DISABLED");
        }

        try {
            Optional<V2SubmitRecord> optRecord = recordDAO.findById(submitRecordId);
            if (!optRecord.isPresent()) {
                return new V2AttemptFinalizationDraftResult(false, "ERROR_V2_SUBMIT_RECORD_NOT_FOUND");
            }

            V2SubmitRecord record = optRecord.get();
            if (record.getUserId() != userId) {
                return new V2AttemptFinalizationDraftResult(false, "ERROR_V2_SUBMIT_RECORD_USER_MISMATCH");
            }

            String payloadHash = record.getPayloadHash();
            if (payloadHash == null || !payloadHash.matches("^[a-fA-F0-9]{64}$")) {
                return new V2AttemptFinalizationDraftResult(false, "ERROR_V2_SUBMIT_RECORD_HASH_INVALID");
            }

            String currentStatus = record.getSubmitStatus();
            
            // Handle idempotency
            if ("FINALIZATION_DRAFTED".equals(currentStatus)) {
                return buildSuccessResult(record, currentStatus, currentStatus);
            }

            if (!"RECEIVED_DEBUG".equals(currentStatus) && !"VALIDATED_DEBUG".equals(currentStatus)) {
                return new V2AttemptFinalizationDraftResult(false, "ERROR_V2_SUBMIT_RECORD_STATUS_INVALID");
            }

            String newStatus = "FINALIZATION_DRAFTED";
            boolean updated = recordDAO.updateSubmitStatus(submitRecordId, newStatus);
            if (!updated) {
                return new V2AttemptFinalizationDraftResult(false, "ERROR_V2_FINALIZATION_DRAFT_FAILED");
            }

            return buildSuccessResult(record, currentStatus, newStatus);

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to create finalization draft for record " + submitRecordId, ex);
            return new V2AttemptFinalizationDraftResult(false, "ERROR_V2_FINALIZATION_DRAFT_FAILED");
        }
    }

    private V2AttemptFinalizationDraftResult buildSuccessResult(V2SubmitRecord record, String oldStatus, String newStatus) {
        V2AttemptFinalizationDraftResult result = new V2AttemptFinalizationDraftResult(true, null);
        result.setSubmitRecordId(record.getId());
        result.setExamId(record.getExamId());
        result.setPaperId(record.getPaperId());
        result.setAttemptId(record.getAttemptId());
        result.setPayloadHash(record.getPayloadHash());
        result.setPreviousStatus(oldStatus);
        result.setNewStatus(newStatus);
        result.setFinalizationMode("NO_GRADING_DRAFT");
        result.setCreatedAt(record.getCreatedAt());
        return result;
    }
}
