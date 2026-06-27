package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.client.exam.ui.TSEV2SubmitAnswerItem;
import com.mycompany.tutorhub_enterprise.client.exam.ui.TSEV2SubmitPayload;
import com.mycompany.tutorhub_enterprise.models.exam.Exam;
import com.mycompany.tutorhub_enterprise.models.exam.ExamPaper;
import com.mycompany.tutorhub_enterprise.models.exam.ExamPaperQuestion;
import com.mycompany.tutorhub_enterprise.server.dao.ExamAttemptDAO;
import com.mycompany.tutorhub_enterprise.server.dao.ExamDAO;
import com.mycompany.tutorhub_enterprise.server.dao.ExamPaperDAO;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class V2SubmitDryRunValidationService {

    protected Exam getExamById(int examId) {
        return ExamDAO.getExamById(examId);
    }

    protected ExamPaper getExamPaperById(int paperId) {
        return ExamPaperDAO.getExamPaperById(paperId);
    }

    protected boolean getAttemptById(String attemptId) {
        return ExamAttemptDAO.getAttemptById(attemptId);
    }

    protected List<ExamPaperQuestion> listQuestionsByPaper(int paperId) {
        return ExamPaperDAO.listQuestionsByPaper(paperId);
    }

    public V2SubmitDryRunValidationResult validateDryRun(int userId, TSEV2SubmitPayload payload) {
        V2SubmitDryRunValidationResult result = new V2SubmitDryRunValidationResult();
        result.setWarnings(new ArrayList<>());
        
        // 1. Check null
        if (payload == null) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_V2_SUBMIT_PAYLOAD_NULL");
            return result;
        }

        // 2. Check flow
        if (!"PAPER_START_V2".equals(payload.getFlow())) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_V2_SUBMIT_FLOW_INVALID");
            return result;
        }

        // 3. Check exam
        Exam exam = getExamById(payload.getExamId());
        if (exam == null) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_V2_SUBMIT_EXAM_NOT_FOUND");
            return result;
        }

        // 4. Check paper
        ExamPaper paper = getExamPaperById(payload.getPaperId());
        if (paper == null) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_V2_SUBMIT_PAPER_MISMATCH");
            return result;
        }
        
        // 5. Check attempt
        if (payload.getAttemptId() != null && !payload.getAttemptId().isEmpty()) {
            boolean attemptExists = getAttemptById(payload.getAttemptId());
            if (!attemptExists) {
                result.getWarnings().add("Attempt ID not found in DB: " + payload.getAttemptId());
            }
        }
        
        // Load questions
        List<ExamPaperQuestion> paperQuestions = listQuestionsByPaper(paper.id);
        
        // 8. Question count match
        if (payload.getQuestionCount() != paperQuestions.size()) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_V2_SUBMIT_QUESTION_COUNT_MISMATCH");
            return result;
        }

        Set<Integer> validQuestionIds = new HashSet<>();
        for (ExamPaperQuestion epq : paperQuestions) {
            validQuestionIds.add(epq.questionId);
        }

        Set<Integer> seenQuestionIds = new HashSet<>();
        int answeredCount = 0;
        
        if (payload.getAnswers() != null) {
            for (TSEV2SubmitAnswerItem item : payload.getAnswers()) {
                // 11. Duplicate check
                if (!seenQuestionIds.add(item.getQuestionId())) {
                    result.setSuccess(false);
                    result.setErrorCode("ERROR_V2_SUBMIT_DUPLICATE_QUESTION");
                    return result;
                }
                
                // 12. Question in paper
                if (!validQuestionIds.contains(item.getQuestionId())) {
                    result.setSuccess(false);
                    result.setErrorCode("ERROR_V2_SUBMIT_QUESTION_NOT_IN_PAPER");
                    return result;
                }
                
                answeredCount++;
                
                // Unsafe payload check per item
                String rawStr = "";
                if (item.getAnsweredAt() != null) {
                    rawStr = String.valueOf(item.getSelectedOptionId()) + item.getAnsweredAt();
                } else {
                    rawStr = String.valueOf(item.getSelectedOptionId());
                }
                if (rawStr.contains("answerKey") || rawStr.contains("isCorrect") || rawStr.contains("correctOption") || rawStr.contains("gradingResult") || rawStr.contains("score")) {
                    result.setSuccess(false);
                    result.setErrorCode("ERROR_V2_SUBMIT_PAYLOAD_UNSAFE");
                    return result;
                }
            }
        }

        // 9. Answered count match
        if (payload.getAnsweredCount() != answeredCount) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_V2_SUBMIT_ANSWERED_COUNT_MISMATCH");
            return result;
        }

        // 10. Unanswered count match
        if (payload.getUnansweredCount() != (payload.getQuestionCount() - answeredCount)) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_V2_SUBMIT_UNANSWERED_COUNT_MISMATCH");
            return result;
        }
        
        // Payload unsafe global check
        String globalRaw = (payload.getPackageHash() != null ? payload.getPackageHash() : "") + 
                           (payload.getDraftSnapshotHash() != null ? payload.getDraftSnapshotHash() : "") + 
                           (payload.getPayloadHash() != null ? payload.getPayloadHash() : "");
                           
        if (globalRaw.contains("answerKey") || globalRaw.contains("isCorrect") || globalRaw.contains("correctOption") || globalRaw.contains("gradingResult") || globalRaw.contains("score")) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_V2_SUBMIT_PAYLOAD_UNSAFE");
            return result;
        }

        // Success
        result.setSuccess(true);
        result.setErrorCode("EXAM_SUBMIT_V2_DRYRUN_VALIDATE_OK");
        result.setExamId(payload.getExamId());
        result.setPaperId(payload.getPaperId());
        result.setAttemptId(payload.getAttemptId());
        result.setQuestionCount(payload.getQuestionCount());
        result.setAnsweredCount(payload.getAnsweredCount());
        result.setUnansweredCount(payload.getUnansweredCount());
        result.setComplete(payload.isComplete());
        result.setPayloadHash(payload.getPayloadHash());
        result.setValidatedAt(Instant.now().toString());

        return result;
    }
}
