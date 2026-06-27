package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2ResultPublicationLedgerRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbe;
import com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ResultPublicationLedgerDAO;

import java.util.Optional;

public class V2FinalAttemptStatusReadinessService {
    public static final String EXPECTED_CURRENT_STATUS = "SUBMITTED";

    private final V2ResultPublicationVerificationService verificationService;
    private final V2FinalResultHandoffService handoffService;
    private final V2ExamResultsReadOnlyProbe examResultsProbe;
    private final V2ResultPublicationLedgerDAO publicationLedgerDAO;
    private final V2FinalAttemptStatusDAO attemptStatusDAO;

    public V2FinalAttemptStatusReadinessService(
            V2ResultPublicationVerificationService verificationService,
            V2FinalResultHandoffService handoffService,
            V2ExamResultsReadOnlyProbe examResultsProbe,
            V2ResultPublicationLedgerDAO publicationLedgerDAO,
            V2FinalAttemptStatusDAO attemptStatusDAO) {
        this.verificationService = verificationService;
        this.handoffService = handoffService;
        this.examResultsProbe = examResultsProbe;
        this.publicationLedgerDAO = publicationLedgerDAO;
        this.attemptStatusDAO = attemptStatusDAO;
    }

    public V2FinalAttemptStatusReadinessResult checkReadiness(int userId, long submitRecordId) {
        V2FinalAttemptStatusReadinessResult result = new V2FinalAttemptStatusReadinessResult();
        
        if (!V2SubmitFeatureFlags.isFinalAttemptStatusReadinessEnabled()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_FEATURE_DISABLED");
            return result;
        }

        // 1. Check Publication Verification
        V2ResultPublicationVerificationResult verificationResult = verificationService.verify(userId, submitRecordId);
        if (!verificationResult.isSuccess() || !verificationResult.isReady()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_FINAL_ATTEMPT_STATUS_PUBLICATION_NOT_VERIFIED");
            return result;
        }

        // Extract identifiers
        String attemptId = verificationResult.getAttemptId();
        int examId = verificationResult.getExamId();
        Integer paperId = verificationResult.getPaperId();

        // 2. Check Final Result Handoff Readiness
        V2FinalResultHandoffResult handoffResult = handoffService.buildHandoff(userId, submitRecordId);
        if (!handoffResult.isSuccess() || !"FINAL_RESULT_HANDOFF_READY".equals(handoffResult.getHandoffStatus())) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_FINAL_ATTEMPT_STATUS_HANDOFF_NOT_READY");
            return result;
        }

        // 3. Verify exam_results exists via probe
        if (!examResultsProbe.existsResultForAttempt(attemptId)) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_FINAL_ATTEMPT_STATUS_EXAM_RESULT_MISSING");
            return result;
        }

        // 4. Verify publication ledger exists
        Optional<V2ResultPublicationLedgerRecord> ledgerOpt = publicationLedgerDAO.findBySubmitRecordId(submitRecordId);
        if (!ledgerOpt.isPresent() || !"RESULT_PUBLISHED".equals(ledgerOpt.get().getPublicationStatus())) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_FINAL_ATTEMPT_STATUS_PUBLICATION_LEDGER_INVALID");
            return result;
        }

        // 5. Check Attempt Status is SUBMITTED
        Optional<String> statusOpt = attemptStatusDAO.findAttemptStatus(attemptId);
        if (!statusOpt.isPresent()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_FINAL_ATTEMPT_STATUS_ATTEMPT_NOT_FOUND");
            return result;
        }
        String currentStatus = statusOpt.get();
        if (!EXPECTED_CURRENT_STATUS.equals(currentStatus)) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_FINAL_ATTEMPT_STATUS_INVALID_CURRENT_STATUS");
            return result;
        }

        // Output Readiness
        result.setSuccess(true);
        result.setReady(true);
        result.setReadinessStatus("READY_FOR_FINAL_ATTEMPT_STATUS_EXECUTION");
        result.setUserId(userId);
        result.setExamId(examId);
        result.setPaperId(paperId);
        result.setAttemptId(attemptId);
        result.setSubmitRecordId(submitRecordId);

        return result;
    }
}
