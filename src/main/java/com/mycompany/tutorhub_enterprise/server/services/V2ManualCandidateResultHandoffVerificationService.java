package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbeImpl;
import com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2OfficialResultDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ResultPublicationLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ScoreDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

public class V2ManualCandidateResultHandoffVerificationService {

    private final V2FinalAttemptStatusDAO attemptStatusDAO;
    private final V2SubmitRecordDAO submitRecordDAO;
    private final V2ScoreDraftDAO scoreDraftDAO;
    private final V2OfficialResultDraftDAO officialResultDraftDAO;
    private final V2ResultPublicationLedgerDAO publicationLedgerDAO;
    private final V2FinalAttemptStatusLedgerDAO finalStatusLedgerDAO;
    private final V2ExamResultsReadOnlyProbeImpl examResultDAO;

    public V2ManualCandidateResultHandoffVerificationService() {
        this(new V2FinalAttemptStatusDAO(), new V2SubmitRecordDAO(), new V2ScoreDraftDAO(),
             new V2OfficialResultDraftDAO(), new V2ResultPublicationLedgerDAO(),
             new V2FinalAttemptStatusLedgerDAO(), new V2ExamResultsReadOnlyProbeImpl());
    }

    public V2ManualCandidateResultHandoffVerificationService(
            V2FinalAttemptStatusDAO attemptStatusDAO,
            V2SubmitRecordDAO submitRecordDAO,
            V2ScoreDraftDAO scoreDraftDAO,
            V2OfficialResultDraftDAO officialResultDraftDAO,
            V2ResultPublicationLedgerDAO publicationLedgerDAO,
            V2FinalAttemptStatusLedgerDAO finalStatusLedgerDAO,
            V2ExamResultsReadOnlyProbeImpl examResultDAO) {
        this.attemptStatusDAO = attemptStatusDAO;
        this.submitRecordDAO = submitRecordDAO;
        this.scoreDraftDAO = scoreDraftDAO;
        this.officialResultDraftDAO = officialResultDraftDAO;
        this.publicationLedgerDAO = publicationLedgerDAO;
        this.finalStatusLedgerDAO = finalStatusLedgerDAO;
        this.examResultDAO = examResultDAO;
    }

    public V2ManualCandidateResultHandoffVerificationResult verifyHandoff(int userId, String attemptId) {
        if (!V2SubmitFeatureFlags.isManualCandidateResultHandoffVerificationEnabled()) {
            return new V2ManualCandidateResultHandoffVerificationResult.Builder()
                    .success(false).ready(false)
                    .errorCode("ERROR_FEATURE_DISABLED")
                    .blockingReasons(Collections.singletonList("Feature flag tse.v2.manualCandidateResultHandoffVerification.enabled is false"))
                    .build();
        }

        Optional<V2SubmitRecord> submitRecordOpt;
        try {
            submitRecordOpt = submitRecordDAO.findLatestByAttemptId(attemptId);
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return new V2ManualCandidateResultHandoffVerificationResult.Builder()
                    .success(false)
                    .errorCode("ERROR_DB_EXCEPTION")
                    .build();
        }
        if (submitRecordOpt.isEmpty()) {
            return new V2ManualCandidateResultHandoffVerificationResult.Builder()
                    .success(false).ready(false)
                    .errorCode("ERROR_SUBMIT_RECORD_MISSING")
                    .blockingReasons(Collections.singletonList("Missing V2SubmitRecord"))
                    .build();
        }
        V2SubmitRecord submitRecord = submitRecordOpt.get();

        if (submitRecord.getUserId() != userId) {
            return new V2ManualCandidateResultHandoffVerificationResult.Builder()
                    .success(false).ready(false)
                    .errorCode("ERROR_USER_MISMATCH")
                    .blockingReasons(Collections.singletonList("Attempt does not belong to user"))
                    .build();
        }

        Optional<String> statusOpt = attemptStatusDAO.findAttemptStatus(attemptId);
        if (statusOpt.isEmpty()) {
            return new V2ManualCandidateResultHandoffVerificationResult.Builder()
                    .success(false).ready(false)
                    .errorCode("ERROR_ATTEMPT_NOT_FOUND")
                    .blockingReasons(Collections.singletonList("Attempt not found"))
                    .build();
        }

        String attemptStatus = statusOpt.get();
        if (!"COMPLETED".equals(attemptStatus)) {
            return new V2ManualCandidateResultHandoffVerificationResult.Builder()
                    .success(false).ready(false)
                    .errorCode("ERROR_NOT_COMPLETED")
                    .blockingReasons(Collections.singletonList("Attempt status is not COMPLETED"))
                    .build();
        }

        if (!finalStatusLedgerDAO.existsByAttemptId(attemptId)) {
            return new V2ManualCandidateResultHandoffVerificationResult.Builder()
                    .success(false).ready(false)
                    .errorCode("ERROR_FINAL_STATUS_LEDGER_MISSING")
                    .blockingReasons(Collections.singletonList("Final status ledger is missing"))
                    .build();
        }

        if (!examResultDAO.existsResultForAttempt(attemptId)) {
            return new V2ManualCandidateResultHandoffVerificationResult.Builder()
                    .success(false).ready(false)
                    .errorCode("ERROR_EXAM_RESULTS_MISSING")
                    .blockingReasons(Collections.singletonList("No result found in exam_results for this user and exam"))
                    .build();
        }

        if (publicationLedgerDAO.findByAttemptId(attemptId).isEmpty()) {
            return new V2ManualCandidateResultHandoffVerificationResult.Builder()
                    .success(false).ready(false)
                    .errorCode("ERROR_PUBLICATION_LEDGER_MISSING")
                    .blockingReasons(Collections.singletonList("Publication ledger is missing"))
                    .build();
        }

        if (!scoreDraftDAO.findBySubmitRecordId(submitRecord.getId()).isPresent()) {
            return new V2ManualCandidateResultHandoffVerificationResult.Builder()
                    .success(false).ready(false)
                    .errorCode("ERROR_SCORE_DRAFT_MISSING")
                    .blockingReasons(Collections.singletonList("Score draft is missing"))
                    .build();
        }

        if (!officialResultDraftDAO.findBySubmitRecordId(submitRecord.getId()).isPresent()) {
            return new V2ManualCandidateResultHandoffVerificationResult.Builder()
                    .success(false).ready(false)
                    .errorCode("ERROR_OFFICIAL_RESULT_DRAFT_MISSING")
                    .blockingReasons(Collections.singletonList("Official result draft is missing"))
                    .build();
        }

        return new V2ManualCandidateResultHandoffVerificationResult.Builder()
                .success(true).ready(true)
                .userId(userId).examId(submitRecord.getExamId()).paperId(String.valueOf(submitRecord.getPaperId()))
                .attemptId(attemptId)
                .handoffStatus("MANUAL_CANDIDATE_RESULT_HANDOFF_VERIFIED")
                .checkedAt(Instant.now())
                .build();
    }
}
