package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;

import java.util.Map;
import java.util.Optional;

public class V2GradingPreflightService {

    private final V2PostSubmitIntegrityAuditService auditService;
    private final V2SubmitRecordDAO submitRecordDAO;
    private final V2AnswerKeyResolver answerKeyResolver;
    private final V2AnswerPayloadParser payloadParser;

    public V2GradingPreflightService() {
        this.auditService = new V2PostSubmitIntegrityAuditService();
        this.submitRecordDAO = new V2SubmitRecordDAO();
        this.answerKeyResolver = null; // Pending schema implementation
        this.payloadParser = null; // Pending schema implementation
    }

    public V2GradingPreflightService(
            V2PostSubmitIntegrityAuditService auditService,
            V2SubmitRecordDAO submitRecordDAO,
            V2AnswerKeyResolver answerKeyResolver,
            V2AnswerPayloadParser payloadParser) {
        this.auditService = auditService;
        this.submitRecordDAO = submitRecordDAO;
        this.answerKeyResolver = answerKeyResolver;
        this.payloadParser = payloadParser;
    }

    public V2GradingPreflightResult checkPreflight(int userId, long submitRecordId) {
        V2GradingPreflightResult result = new V2GradingPreflightResult();
        result.setUserId(userId);
        result.setSubmitRecordId(submitRecordId);

        try {
            if (!V2SubmitFeatureFlags.isGradingPreflightEnabled()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_FEATURE_DISABLED");
                result.setGradingPreflightStatus("NOT_READY");
                result.addBlockingReason("Grading Preflight feature flag is disabled.");
                return result;
            }

            V2PostSubmitIntegrityAuditResult auditResult = auditService.audit(userId, submitRecordId);
            result.setPostSubmitAuditStatus(auditResult.getAuditStatus());

            if (!auditResult.isSuccess() || !auditResult.isReady()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_GRADING_PREFLIGHT_AUDIT_NOT_READY");
                result.setGradingPreflightStatus("NOT_READY");
                result.addBlockingReason("Post-Submit Integrity Audit failed or not ready.");
                return result;
            }

            result.setExamId(auditResult.getExamId());
            result.setPaperId(auditResult.getPaperId());
            result.setAttemptId(auditResult.getAttemptId());
            result.setPayloadHash(auditResult.getPayloadHash());

            if (answerKeyResolver == null) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_GRADING_PREFLIGHT_MISSING_RESOLVER");
                result.setGradingPreflightStatus("NOT_READY");
                result.addBlockingReason("Answer Key Resolver is missing (pending schema verification).");
                return result;
            }

            if (payloadParser == null) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_GRADING_PREFLIGHT_MISSING_PARSER");
                result.setGradingPreflightStatus("NOT_READY");
                result.addBlockingReason("Payload Parser is missing (pending schema verification).");
                return result;
            }

            Optional<V2SubmitRecord> submitRecordOpt = submitRecordDAO.findById(submitRecordId);
            if (submitRecordOpt.isEmpty()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_GRADING_PREFLIGHT_MISSING_SUBMIT_RECORD");
                result.setGradingPreflightStatus("NOT_READY");
                result.addBlockingReason("Submit record not found.");
                return result;
            }

            V2SubmitRecord submitRecord = submitRecordOpt.get();
            String payloadJson = submitRecord.getPayloadJson();
            if (payloadJson == null || payloadJson.isEmpty()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_GRADING_PREFLIGHT_MISSING_PAYLOAD");
                result.setGradingPreflightStatus("NOT_READY");
                result.addBlockingReason("Submitted payload is empty.");
                return result;
            }

            Map<Long, Long> answers;
            try {
                answers = payloadParser.extractAnswers(payloadJson);
            } catch (Exception e) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_GRADING_PREFLIGHT_INVALID_PAYLOAD");
                result.setGradingPreflightStatus("NOT_READY");
                result.addBlockingReason("Failed to parse payload Json safely.");
                return result;
            }

            Map<Long, Long> correctOptions = answerKeyResolver.resolveCorrectOptionIds(submitRecord.getPaperId());
            if (correctOptions == null || correctOptions.isEmpty()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_GRADING_PREFLIGHT_MISSING_ANSWER_KEY");
                result.setGradingPreflightStatus("NOT_READY");
                result.addBlockingReason("Answer key map is empty.");
                return result;
            }

            result.setAnswerCount(answers.size());
            result.setQuestionCount(correctOptions.size());

            result.setSuccess(true);
            result.setReady(true);
            result.setGradingPreflightStatus("READY_FOR_SCORE_DRAFT");

        } catch (Exception e) {
            e.printStackTrace();
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_EXCEPTION");
            result.setGradingPreflightStatus("NOT_READY");
            result.addBlockingReason("Exception during grading preflight: " + e.getMessage());
        }

        return result;
    }
}
