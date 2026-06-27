package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2AttemptStatusExecutionLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2OfficialResultDraftRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2ScoreDraftRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2AttemptStatusExecutionLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2OfficialResultDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ScoreDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;

import java.util.Map;
import java.util.Optional;

public class V2ManualCandidateScoreOfficialDraftExecutionService {

    private final V2AttemptStatusExecutionLedgerDAO statusLedgerDAO;
    private final V2SubmitRecordDAO submitRecordDAO;
    private final V2ScoreDraftDAO scoreDraftDAO;
    private final V2OfficialResultDraftDAO officialResultDraftDAO;
    private final V2AnswerKeyResolver answerKeyResolver;
    private final V2AnswerPayloadParser payloadParser;

    public V2ManualCandidateScoreOfficialDraftExecutionService() {
        this.statusLedgerDAO = new V2AttemptStatusExecutionLedgerDAO();
        this.submitRecordDAO = new V2SubmitRecordDAO();
        this.scoreDraftDAO = new V2ScoreDraftDAO();
        this.officialResultDraftDAO = new V2OfficialResultDraftDAO();
        this.answerKeyResolver = new V2DatabaseAnswerKeyResolver();
        this.payloadParser = new V2JsonAnswerPayloadParser();
        
        this.scoreDraftDAO.ensureTableExists();
        this.officialResultDraftDAO.ensureTableExists();
    }

    public V2ManualCandidateScoreOfficialDraftExecutionService(
            V2AttemptStatusExecutionLedgerDAO statusLedgerDAO,
            V2SubmitRecordDAO submitRecordDAO,
            V2ScoreDraftDAO scoreDraftDAO,
            V2OfficialResultDraftDAO officialResultDraftDAO,
            V2AnswerKeyResolver answerKeyResolver,
            V2AnswerPayloadParser payloadParser) {
        this.statusLedgerDAO = statusLedgerDAO;
        this.submitRecordDAO = submitRecordDAO;
        this.scoreDraftDAO = scoreDraftDAO;
        this.officialResultDraftDAO = officialResultDraftDAO;
        this.answerKeyResolver = answerKeyResolver;
        this.payloadParser = payloadParser;
        
        this.scoreDraftDAO.ensureTableExists();
        this.officialResultDraftDAO.ensureTableExists();
    }

    public V2ManualCandidateScoreOfficialDraftExecutionResult executeDrafts(int userId, long submitRecordId) {
        V2ManualCandidateScoreOfficialDraftExecutionResult result = new V2ManualCandidateScoreOfficialDraftExecutionResult();
        result.setUserId(userId);
        result.setSubmitRecordId(submitRecordId);

        try {
            if (!V2SubmitFeatureFlags.isManualCandidateScoreOfficialDraftExecutionEnabled()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_FEATURE_DISABLED");
                result.addBlockingReason("Manual Candidate Score Official Draft Execution is disabled.");
                return result;
            }

            // 1. Validate Gate: Ensure Submit Status is executed
            Optional<V2AttemptStatusExecutionLedgerRecord> statusLedgerOpt = statusLedgerDAO.findBySubmitRecordId(submitRecordId);
            if (statusLedgerOpt.isEmpty()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_STATUS_EXECUTION_LEDGER_MISSING");
                result.addBlockingReason("Attempt Status Execution Ledger missing. Cannot create drafts.");
                return result;
            }
            V2AttemptStatusExecutionLedgerRecord statusLedger = statusLedgerOpt.get();

            if (!"SUBMITTED".equals(statusLedger.getActualAttemptStatus())) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_ATTEMPT_STATUS_INVALID");
                result.addBlockingReason("Attempt status in ledger is not SUBMITTED.");
                return result;
            }

            result.setExamId(statusLedger.getExamId());
            result.setPaperId(statusLedger.getPaperId());
            result.setAttemptId(statusLedger.getAttemptId());

            // 2. Idempotency Check
            Optional<V2ScoreDraftRecord> scoreDraftOpt = scoreDraftDAO.findBySubmitRecordId(submitRecordId);
            Optional<V2OfficialResultDraftRecord> officialDraftOpt = officialResultDraftDAO.findBySubmitRecordId(submitRecordId);

            if (scoreDraftOpt.isPresent() && officialDraftOpt.isPresent()) {
                result.setSuccess(true);
                result.setReady(true);
                result.setIdempotent(true);
                result.setScoreDraftId(scoreDraftOpt.get().getId());
                result.setOfficialResultDraftId(officialDraftOpt.get().getId());
                result.setDraftExecutionStatus("DRAFTS_CREATED_IDEMPOTENT");
                return result;
            }

            if (scoreDraftOpt.isPresent() && officialDraftOpt.isEmpty()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_UNSAFE_STATE_SCORE_DRAFT_ONLY");
                result.addBlockingReason("Score draft exists but official draft is missing. Inconsistent state.");
                return result;
            }

            if (scoreDraftOpt.isEmpty() && officialDraftOpt.isPresent()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_UNSAFE_STATE_OFFICIAL_DRAFT_ONLY");
                result.addBlockingReason("Official draft exists but score draft is missing. Inconsistent state.");
                return result;
            }

            // 3. Ensure Services are available
            if (payloadParser == null) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_PAYLOAD_PARSER_UNAVAILABLE");
                result.addBlockingReason("Payload parser is not configured.");
                return result;
            }

            if (answerKeyResolver == null) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_ANSWER_KEY_RESOLVER_UNAVAILABLE");
                result.addBlockingReason("Answer key resolver is not configured.");
                return result;
            }

            // 4. Fetch Submit Record
            Optional<V2SubmitRecord> submitRecordOpt = submitRecordDAO.findById(submitRecordId);
            if (submitRecordOpt.isEmpty()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_SUBMIT_RECORD_MISSING");
                result.addBlockingReason("Submit record not found.");
                return result;
            }
            V2SubmitRecord submitRecord = submitRecordOpt.get();

            // 5. Parse Payload & Resolve Key
            Map<Long, Long> answers = payloadParser.extractAnswers(submitRecord.getPayloadJson());
            if (answers == null) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_PAYLOAD_PARSER_UNAVAILABLE");
                result.addBlockingReason("Payload parser returned unavailable status.");
                return result;
            }

            Map<Long, Long> correctOptions = answerKeyResolver.resolveCorrectOptionIds(submitRecord.getPaperId());
            if (correctOptions == null) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_ANSWER_KEY_RESOLVER_UNAVAILABLE");
                result.addBlockingReason("Answer key resolver returned unavailable status.");
                return result;
            }

            // 6. Compute internally (no exposure to DTO)
            int totalQuestions = correctOptions.size();
            int answeredQuestions = answers.size();
            int unansweredQuestions = Math.max(0, totalQuestions - answeredQuestions);

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

            int incorrectCount = Math.max(0, answeredQuestions - correctCount);
            double rawScore = correctCount;
            double maxScore = totalQuestions;
            double percentage = maxScore > 0 ? (rawScore / maxScore) * 100.0 : 0.0;

            // 7. Insert Score Draft
            V2ScoreDraftRecord scoreDraft = new V2ScoreDraftRecord();
            scoreDraft.setSubmitRecordId(submitRecordId);
            scoreDraft.setUserId(userId);
            scoreDraft.setExamId(submitRecord.getExamId());
            scoreDraft.setPaperId(submitRecord.getPaperId());
            scoreDraft.setAttemptId(submitRecord.getAttemptId());
            scoreDraft.setTotalQuestions(totalQuestions);
            scoreDraft.setAnsweredQuestions(answeredQuestions);
            scoreDraft.setUnansweredQuestions(unansweredQuestions);
            scoreDraft.setCorrectCount(correctCount);
            scoreDraft.setIncorrectCount(incorrectCount);
            scoreDraft.setRawScore(rawScore);
            scoreDraft.setMaxScore(maxScore);
            scoreDraft.setPercentage(percentage);

            boolean scoreDraftInserted = scoreDraftDAO.insert(scoreDraft);
            if (!scoreDraftInserted) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_SCORE_DRAFT_INSERT_FAILED");
                result.addBlockingReason("Failed to insert score draft.");
                return result;
            }

            // 8. Insert Official Result Draft
            V2OfficialResultDraftRecord officialDraft = new V2OfficialResultDraftRecord();
            officialDraft.setScoreDraftId(scoreDraft.getId());
            officialDraft.setSubmitRecordId(submitRecordId);
            officialDraft.setUserId(userId);
            officialDraft.setExamId(submitRecord.getExamId());
            officialDraft.setPaperId(submitRecord.getPaperId());
            officialDraft.setAttemptId(submitRecord.getAttemptId());
            officialDraft.setPayloadHash(submitRecord.getPayloadHash());
            officialDraft.setTotalQuestions(totalQuestions);
            officialDraft.setAnsweredQuestions(answeredQuestions);
            officialDraft.setUnansweredQuestions(unansweredQuestions);
            officialDraft.setCorrectCount(correctCount);
            officialDraft.setIncorrectCount(incorrectCount);
            officialDraft.setRawScore(rawScore);
            officialDraft.setMaxScore(maxScore);
            officialDraft.setPercentage(percentage);
            officialDraft.setScoreDraftStatus("SCORE_DRAFTED_SERVER_SIDE");
            officialDraft.setScoreDraftAuditStatus("AUDIT_NOT_APPLICABLE_MANUAL_FLOW");
            officialDraft.setOfficialResultDraftStatus("OFFICIAL_RESULT_DRAFTED_PENDING_PUBLICATION");

            boolean officialDraftInserted = officialResultDraftDAO.insertDraft(officialDraft);
            if (!officialDraftInserted) {
                // Return failure. Since score draft inserted but this failed, next invocation will hit UNSAFE state.
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_OFFICIAL_DRAFT_INSERT_FAILED");
                result.addBlockingReason("Failed to insert official result draft.");
                return result;
            }

            result.setSuccess(true);
            result.setReady(true);
            result.setIdempotent(false);
            result.setScoreDraftId(scoreDraft.getId());
            result.setOfficialResultDraftId(officialDraft.getId());
            result.setDraftExecutionStatus("MANUAL_DRAFTS_CREATED");

        } catch (Exception e) {
            e.printStackTrace();
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_EXCEPTION");
            result.addBlockingReason("Exception: " + e.getMessage());
        }

        return result;
    }
}
