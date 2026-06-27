package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2ScoreDraftRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ScoreDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;

public class V2ScoreDraftService {

    private final V2GradingPreflightService preflightService;
    private final V2SubmitRecordDAO submitRecordDAO;
    private final V2ScoreDraftDAO scoreDraftDAO;
    private final V2AnswerKeyResolver answerKeyResolver;
    private final V2AnswerPayloadParser payloadParser;

    public V2ScoreDraftService() {
        this.preflightService = new V2GradingPreflightService();
        this.submitRecordDAO = new V2SubmitRecordDAO();
        this.scoreDraftDAO = new V2ScoreDraftDAO();
        this.scoreDraftDAO.ensureTableExists();
        this.answerKeyResolver = new V2DatabaseAnswerKeyResolver();
        this.payloadParser = new V2JsonAnswerPayloadParser();
    }

    public V2ScoreDraftService(
            V2GradingPreflightService preflightService,
            V2SubmitRecordDAO submitRecordDAO,
            V2ScoreDraftDAO scoreDraftDAO,
            V2AnswerKeyResolver answerKeyResolver,
            V2AnswerPayloadParser payloadParser) {
        this.preflightService = preflightService;
        this.submitRecordDAO = submitRecordDAO;
        this.scoreDraftDAO = scoreDraftDAO;
        this.scoreDraftDAO.ensureTableExists();
        this.answerKeyResolver = answerKeyResolver;
        this.payloadParser = payloadParser;
    }

    public V2ScoreDraftResult createScoreDraft(int userId, long submitRecordId) {
        V2ScoreDraftResult result = new V2ScoreDraftResult();
        result.setUserId(userId);
        result.setSubmitRecordId(submitRecordId);

        try {
            if (!V2SubmitFeatureFlags.isScoreDraftEnabled()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_FEATURE_DISABLED");
                result.setScoreDraftStatus("NOT_READY");
                result.addBlockingReason("Score Draft feature flag is disabled.");
                return result;
            }

            V2GradingPreflightResult preflightResult = preflightService.checkPreflight(userId, submitRecordId);
            if (!preflightResult.isSuccess() || !preflightResult.isReady()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_SCORE_DRAFT_PREFLIGHT_NOT_READY");
                result.setScoreDraftStatus("NOT_READY");
                result.addBlockingReason("Grading Preflight failed or not ready.");
                return result;
            }

            result.setExamId(preflightResult.getExamId());
            result.setPaperId(preflightResult.getPaperId());
            result.setAttemptId(preflightResult.getAttemptId());
            result.setPayloadHash(preflightResult.getPayloadHash());

            // Check Idempotency
            Optional<V2ScoreDraftRecord> existingDraftOpt = scoreDraftDAO.findBySubmitRecordId(submitRecordId);
            if (existingDraftOpt.isPresent()) {
                V2ScoreDraftRecord existing = existingDraftOpt.get();
                result.setSuccess(true);
                result.setReady(true);
                result.setScoreDraftId(existing.getId());
                result.setTotalQuestions(existing.getTotalQuestions());
                result.setAnsweredQuestions(existing.getAnsweredQuestions());
                result.setUnansweredQuestions(existing.getUnansweredQuestions());
                result.setCorrectCount(existing.getCorrectCount());
                result.setIncorrectCount(existing.getIncorrectCount());
                result.setRawScore(existing.getRawScore());
                result.setMaxScore(existing.getMaxScore());
                result.setPercentage(existing.getPercentage());
                result.setScoreDraftStatus("SCORE_DRAFTED_SERVER_SIDE");
                result.addWarning("Score draft already exists. Returning idempotent result.");
                return result;
            }

            // Calculation
            Optional<V2SubmitRecord> submitRecordOpt = submitRecordDAO.findById(submitRecordId);
            if (submitRecordOpt.isEmpty()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_SCORE_DRAFT_MISSING_SUBMIT_RECORD");
                result.setScoreDraftStatus("NOT_READY");
                result.addBlockingReason("Submit record not found.");
                return result;
            }

            V2SubmitRecord submitRecord = submitRecordOpt.get();
            
            if (payloadParser == null) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_SCORE_DRAFT_PAYLOAD_PARSER_UNAVAILABLE");
                result.setScoreDraftStatus("NOT_READY");
                result.addBlockingReason("Payload parser is not configured.");
                return result;
            }

            if (answerKeyResolver == null) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_SCORE_DRAFT_ANSWER_KEY_RESOLVER_UNAVAILABLE");
                result.setScoreDraftStatus("NOT_READY");
                result.addBlockingReason("Answer key resolver is not configured.");
                return result;
            }

            Map<Long, Long> answers = payloadParser.extractAnswers(submitRecord.getPayloadJson());
            if (answers == null) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_SCORE_DRAFT_PAYLOAD_PARSER_UNAVAILABLE");
                result.setScoreDraftStatus("NOT_READY");
                result.addBlockingReason("Payload parser returned unavailable status or failed to parse payload.");
                return result;
            }

            Map<Long, Long> correctOptions = answerKeyResolver.resolveCorrectOptionIds(submitRecord.getPaperId());
            if (correctOptions == null) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_SCORE_DRAFT_ANSWER_KEY_RESOLVER_UNAVAILABLE");
                result.setScoreDraftStatus("NOT_READY");
                result.addBlockingReason("Answer key resolver returned unavailable status or failed to fetch key.");
                return result;
            }

            int totalQuestions = correctOptions.size();
            int answeredQuestions = answers.size();
            int unansweredQuestions = totalQuestions - answeredQuestions;
            if (unansweredQuestions < 0) unansweredQuestions = 0;

            int correctCount = 0;
            for (Map.Entry<Long, Long> entry : answers.entrySet()) {
                Long questionId = entry.getKey();
                Long selectedOptionId = entry.getValue();

                if (correctOptions.containsKey(questionId)) {
                    Long correctOptionId = correctOptions.get(questionId);
                    if (correctOptionId != null && correctOptionId.equals(selectedOptionId)) {
                        correctCount++;
                    }
                }
            }

            int incorrectCount = answeredQuestions - correctCount;
            if (incorrectCount < 0) incorrectCount = 0;

            double rawScore = correctCount; // Simplistic scoring: 1 point per correct answer
            double maxScore = totalQuestions;
            double percentage = 0.0;
            if (maxScore > 0) {
                percentage = (rawScore / maxScore) * 100.0;
            }

            V2ScoreDraftRecord newDraft = new V2ScoreDraftRecord();
            newDraft.setSubmitRecordId(submitRecordId);
            newDraft.setUserId(userId);
            newDraft.setExamId(submitRecord.getExamId());
            newDraft.setPaperId(submitRecord.getPaperId());
            newDraft.setAttemptId(submitRecord.getAttemptId());
            newDraft.setTotalQuestions(totalQuestions);
            newDraft.setAnsweredQuestions(answeredQuestions);
            newDraft.setUnansweredQuestions(unansweredQuestions);
            newDraft.setCorrectCount(correctCount);
            newDraft.setIncorrectCount(incorrectCount);
            newDraft.setRawScore(rawScore);
            newDraft.setMaxScore(maxScore);
            newDraft.setPercentage(percentage);

            boolean inserted = scoreDraftDAO.insert(newDraft);
            if (!inserted) {
                // Potential race condition -> Idempotency fallback
                Optional<V2ScoreDraftRecord> fallbackDraftOpt = scoreDraftDAO.findBySubmitRecordId(submitRecordId);
                if (fallbackDraftOpt.isPresent()) {
                    V2ScoreDraftRecord existing = fallbackDraftOpt.get();
                    result.setSuccess(true);
                    result.setReady(true);
                    result.setScoreDraftId(existing.getId());
                    result.setTotalQuestions(existing.getTotalQuestions());
                    result.setAnsweredQuestions(existing.getAnsweredQuestions());
                    result.setUnansweredQuestions(existing.getUnansweredQuestions());
                    result.setCorrectCount(existing.getCorrectCount());
                    result.setIncorrectCount(existing.getIncorrectCount());
                    result.setRawScore(existing.getRawScore());
                    result.setMaxScore(existing.getMaxScore());
                    result.setPercentage(existing.getPercentage());
                    result.setScoreDraftStatus("SCORE_DRAFTED_SERVER_SIDE");
                    result.addWarning("Score draft inserted by another transaction. Returning idempotent result.");
                    return result;
                } else {
                    result.setSuccess(false);
                    result.setReady(false);
                    result.setErrorCode("ERROR_V2_SCORE_DRAFT_INSERT_FAILED");
                    result.setScoreDraftStatus("NOT_READY");
                    result.addBlockingReason("Failed to insert score draft record into database.");
                    return result;
                }
            }

            result.setSuccess(true);
            result.setReady(true);
            result.setScoreDraftId(newDraft.getId());
            result.setTotalQuestions(totalQuestions);
            result.setAnsweredQuestions(answeredQuestions);
            result.setUnansweredQuestions(unansweredQuestions);
            result.setCorrectCount(correctCount);
            result.setIncorrectCount(incorrectCount);
            result.setRawScore(rawScore);
            result.setMaxScore(maxScore);
            result.setPercentage(percentage);
            result.setScoreDraftStatus("SCORE_DRAFTED_SERVER_SIDE");

        } catch (Exception e) {
            e.printStackTrace();
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_EXCEPTION");
            result.setScoreDraftStatus("NOT_READY");
            result.addBlockingReason("Exception during score draft calculation: " + e.getMessage());
        }

        return result;
    }
}
