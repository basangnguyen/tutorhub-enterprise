package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;
import com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2FinalAttemptStatusLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2OfficialResultDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ResultPublicationLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ScoreDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import com.mycompany.tutorhub_enterprise.models.exam.V2FinalAttemptStatusLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2ResultPublicationLedgerRecord;

public class V2ManualCandidateFinalStatusExecutionService {

    private final V2FinalAttemptStatusDAO finalAttemptStatusDAO;
    private final V2SubmitRecordDAO v2SubmitRecordDAO;
    private final V2ScoreDraftDAO scoreDraftDAO;
    private final V2OfficialResultDraftDAO officialResultDraftDAO;
    private final V2ResultPublicationLedgerDAO resultPublicationLedgerDAO;
    private final V2FinalAttemptStatusLedgerDAO finalAttemptStatusLedgerDAO;

    public V2ManualCandidateFinalStatusExecutionService() {
        this(new V2FinalAttemptStatusDAO(), new V2SubmitRecordDAO(), new V2ScoreDraftDAO(),
             new V2OfficialResultDraftDAO(), new V2ResultPublicationLedgerDAO(),
             new V2FinalAttemptStatusLedgerDAO());
    }

    public V2ManualCandidateFinalStatusExecutionService(
            V2FinalAttemptStatusDAO finalAttemptStatusDAO,
            V2SubmitRecordDAO v2SubmitRecordDAO,
            V2ScoreDraftDAO scoreDraftDAO,
            V2OfficialResultDraftDAO officialResultDraftDAO,
            V2ResultPublicationLedgerDAO resultPublicationLedgerDAO,
            V2FinalAttemptStatusLedgerDAO finalAttemptStatusLedgerDAO) {
        this.finalAttemptStatusDAO = finalAttemptStatusDAO;
        this.v2SubmitRecordDAO = v2SubmitRecordDAO;
        this.scoreDraftDAO = scoreDraftDAO;
        this.officialResultDraftDAO = officialResultDraftDAO;
        this.resultPublicationLedgerDAO = resultPublicationLedgerDAO;
        this.finalAttemptStatusLedgerDAO = finalAttemptStatusLedgerDAO;
    }

    public V2ManualCandidateFinalStatusExecutionResult executeFinalStatus(int userId, String attemptId) {
        if (!V2SubmitFeatureFlags.isManualCandidateFinalStatusExecutionEnabled()) {
            return new V2ManualCandidateFinalStatusExecutionResult.Builder()
                    .success(false).ready(false)
                    .errorCode("ERROR_FEATURE_DISABLED")
                    .blockingReasons(Collections.singletonList("Feature flag tse.v2.manualCandidateFinalStatusExecution.enabled is false"))
                    .build();
        }

        // Get Attempt Status
        Optional<String> attemptStatusOpt = finalAttemptStatusDAO.findAttemptStatus(attemptId);
        if (attemptStatusOpt.isEmpty()) {
            return new V2ManualCandidateFinalStatusExecutionResult.Builder()
                    .success(false).ready(false)
                    .errorCode("ERROR_ATTEMPT_NOT_FOUND")
                    .blockingReasons(Collections.singletonList("Attempt not found: " + attemptId))
                    .build();
        }
        String attemptStatus = attemptStatusOpt.get();

        // Get Submit Record for context
        Optional<V2SubmitRecord> submitRecordOpt;
        try {
            submitRecordOpt = v2SubmitRecordDAO.findLatestByAttemptId(attemptId);
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return new V2ManualCandidateFinalStatusExecutionResult.Builder()
                    .success(false)
                    .errorCode("ERROR_DB_EXCEPTION")
                    .build();
        }
        if (submitRecordOpt.isEmpty()) {
            return new V2ManualCandidateFinalStatusExecutionResult.Builder()
                    .success(false).ready(false)
                    .errorCode("ERROR_SUBMIT_RECORD_MISSING")
                    .blockingReasons(Collections.singletonList("Missing V2SubmitRecord"))
                    .build();
        }
        V2SubmitRecord submitRecord = submitRecordOpt.get();

        if (submitRecord.getUserId() != userId) {
            return new V2ManualCandidateFinalStatusExecutionResult.Builder()
                    .success(false).ready(false)
                    .errorCode("ERROR_USER_MISMATCH")
                    .blockingReasons(Collections.singletonList("Attempt does not belong to user"))
                    .build();
        }

        // Check idempotent success condition
        boolean hasLedger = finalAttemptStatusLedgerDAO.existsByAttemptId(attemptId);
        if ("COMPLETED".equals(attemptStatus)) {
            if (hasLedger) {
                return new V2ManualCandidateFinalStatusExecutionResult.Builder()
                        .success(true).ready(true).idempotent(true)
                        .userId(userId).examId(submitRecord.getExamId()).paperId(String.valueOf(submitRecord.getPaperId()))
                        .attemptId(attemptId)
                        .finalStatus("COMPLETED")
                        .executionStatus("IDEMPOTENT_SUCCESS")
                        .executedAt(Instant.now())
                        .build();
            } else {
                return new V2ManualCandidateFinalStatusExecutionResult.Builder()
                        .success(false).ready(false).idempotent(false)
                        .errorCode("ERROR_UNSAFE_STATE_COMPLETED_BUT_NO_LEDGER")
                        .blockingReasons(Collections.singletonList("Attempt is COMPLETED but no final status ledger exists"))
                        .build();
            }
        }

        if (!"SUBMITTED".equals(attemptStatus)) {
            return new V2ManualCandidateFinalStatusExecutionResult.Builder()
                    .success(false).ready(false)
                    .errorCode("ERROR_INVALID_ATTEMPT_STATUS")
                    .blockingReasons(Collections.singletonList("Status must be SUBMITTED to execute final status"))
                    .build();
        }

        if (hasLedger) {
            return new V2ManualCandidateFinalStatusExecutionResult.Builder()
                    .success(false).ready(false)
                    .errorCode("ERROR_UNSAFE_STATE_LEDGER_BUT_NOT_COMPLETED")
                    .blockingReasons(Collections.singletonList("Final status ledger exists but attempt is not COMPLETED"))
                    .build();
        }

        if (!scoreDraftDAO.findBySubmitRecordId(submitRecord.getId()).isPresent()) {
            return new V2ManualCandidateFinalStatusExecutionResult.Builder()
                    .success(false).ready(false)
                    .errorCode("ERROR_SCORE_DRAFT_MISSING")
                    .blockingReasons(Collections.singletonList("Missing score draft"))
                    .build();
        }

        if (!officialResultDraftDAO.findBySubmitRecordId(submitRecord.getId()).isPresent()) {
            return new V2ManualCandidateFinalStatusExecutionResult.Builder()
                    .success(false).ready(false)
                    .errorCode("ERROR_OFFICIAL_RESULT_DRAFT_MISSING")
                    .blockingReasons(Collections.singletonList("Missing official result draft"))
                    .build();
        }

        if (resultPublicationLedgerDAO.findByAttemptId(attemptId).isEmpty()) {
            return new V2ManualCandidateFinalStatusExecutionResult.Builder()
                    .success(false).ready(false)
                    .errorCode("ERROR_PUBLICATION_LEDGER_MISSING")
                    .blockingReasons(Collections.singletonList("Missing publication ledger (exam_results not published)"))
                    .build();
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean updated = finalAttemptStatusDAO.updateAttemptStatusIfCurrent(conn, attemptId, "SUBMITTED", "COMPLETED");
                if (!updated) {
                    conn.rollback();
                    return new V2ManualCandidateFinalStatusExecutionResult.Builder()
                            .success(false).ready(false)
                            .errorCode("ERROR_CONCURRENT_STATUS_UPDATE")
                            .blockingReasons(Collections.singletonList("Concurrent update of attempt status"))
                            .build();
                }

                Optional<V2ResultPublicationLedgerRecord> pubLedgerOpt = resultPublicationLedgerDAO.findByAttemptId(attemptId);
                    long pubLedgerId = pubLedgerOpt.isPresent() ? pubLedgerOpt.get().getId() : 0L;
                    V2FinalAttemptStatusLedgerRecord ledgerRecord = new V2FinalAttemptStatusLedgerRecord();
                    ledgerRecord.setSubmitRecordId(submitRecord.getId());
                    ledgerRecord.setUserId(userId);
                    ledgerRecord.setExamId(submitRecord.getExamId());
                    ledgerRecord.setPaperId(submitRecord.getPaperId());
                    ledgerRecord.setAttemptId(attemptId);
                    ledgerRecord.setFromStatus("SUBMITTED");
                    ledgerRecord.setToStatus("COMPLETED");
                    ledgerRecord.setPublicationLedgerId(pubLedgerId);
                    ledgerRecord.setPayloadHash(submitRecord.getPayloadHash());
                    ledgerRecord.setStatusUpdateStatus("MANUAL_CANDIDATE_FINAL_STATUS_EXECUTION");
                    long ledgerId = finalAttemptStatusLedgerDAO.insertExecutionLedger(conn, ledgerRecord);
                if (ledgerId <= 0) {
                    conn.rollback();
                    return new V2ManualCandidateFinalStatusExecutionResult.Builder()
                            .success(false).ready(false)
                            .errorCode("ERROR_INSERT_LEDGER_FAILED")
                            .blockingReasons(Collections.singletonList("Failed to insert final status ledger"))
                            .build();
                }

                conn.commit();

                return new V2ManualCandidateFinalStatusExecutionResult.Builder()
                        .success(true).ready(true).idempotent(false)
                        .userId(userId).examId(submitRecord.getExamId()).paperId(String.valueOf(submitRecord.getPaperId()))
                        .attemptId(attemptId)
                        .finalStatus("COMPLETED")
                        .finalStatusLedgerId(ledgerId)
                        .executionStatus("MANUAL_CANDIDATE_FINAL_STATUS_EXECUTED")
                        .executedAt(Instant.now())
                        .build();

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return new V2ManualCandidateFinalStatusExecutionResult.Builder()
                        .success(false).ready(false)
                        .errorCode("ERROR_TRANSACTION_FAILED")
                        .blockingReasons(Collections.singletonList("Transaction failed: " + e.getMessage()))
                        .build();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new V2ManualCandidateFinalStatusExecutionResult.Builder()
                    .success(false).ready(false)
                    .errorCode("ERROR_DATABASE")
                    .blockingReasons(Collections.singletonList("Database connection error"))
                    .build();
        }
    }
}
