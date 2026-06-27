package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2OfficialResultDraftRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2ResultPublicationLedgerRecord;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbe;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsReadOnlyProbeImpl;
import com.mycompany.tutorhub_enterprise.server.dao.V2ExamResultsWriteDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2OfficialResultDraftDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2ResultPublicationLedgerDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

public class V2ResultPublicationService {

    private final V2ResultPublicationReadinessService readinessService;
    private final V2OfficialResultDraftDAO officialResultDraftDAO;
    private final V2ExamResultsReadOnlyProbe examResultsProbe;
    private final V2ResultPublicationLedgerDAO ledgerDAO;
    private final V2ExamResultsWriteDAO examResultsWriteDAO;

    public V2ResultPublicationService() {
        this.readinessService = new V2ResultPublicationReadinessService(
            new V2SubmitFeatureFlags(),
            new V2OfficialResultDraftDAO(),
            new com.mycompany.tutorhub_enterprise.server.dao.V2ExamAttemptStatusDAO(),
            new V2ExamResultsReadOnlyProbeImpl()
        );
        this.officialResultDraftDAO = new V2OfficialResultDraftDAO();
        this.examResultsProbe = new V2ExamResultsReadOnlyProbeImpl();
        this.ledgerDAO = new V2ResultPublicationLedgerDAO();
        this.examResultsWriteDAO = new V2ExamResultsWriteDAO();
    }

    // Constructor for testing
    public V2ResultPublicationService(V2ResultPublicationReadinessService readinessService,
                                      V2OfficialResultDraftDAO officialResultDraftDAO,
                                      V2ExamResultsReadOnlyProbe examResultsProbe,
                                      V2ResultPublicationLedgerDAO ledgerDAO,
                                      V2ExamResultsWriteDAO examResultsWriteDAO) {
        this.readinessService = readinessService;
        this.officialResultDraftDAO = officialResultDraftDAO;
        this.examResultsProbe = examResultsProbe;
        this.ledgerDAO = ledgerDAO;
        this.examResultsWriteDAO = examResultsWriteDAO;
    }

    public V2ResultPublicationResult publishResult(int userId, long submitRecordId) {
        V2ResultPublicationResult result = new V2ResultPublicationResult();
        
        if (!V2SubmitFeatureFlags.isResultPublicationWriteEnabled()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_FEATURE_DISABLED");
            result.getBlockingReasons().add("V2 Result Publication Write feature is disabled.");
            return result;
        }

        V2ResultPublicationReadinessResult readiness = readinessService.checkReadiness(userId, submitRecordId);
        if (!readiness.isReady() || !"READY_FOR_EXAM_RESULTS_WRITE_DRAFT".equals(readiness.getPublicationReadinessStatus())) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_V2_RESULT_PUBLICATION_READINESS_NOT_READY");
            result.getBlockingReasons().add("Attempt is not ready for publication.");
            return result;
        }

        Optional<V2OfficialResultDraftRecord> draftOpt = officialResultDraftDAO.findBySubmitRecordId(submitRecordId);
        if (!draftOpt.isPresent()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_V2_RESULT_PUBLICATION_DRAFT_MISSING");
            result.getBlockingReasons().add("Official result draft missing.");
            return result;
        }

        V2OfficialResultDraftRecord draft = draftOpt.get();
        if (draft.getUserId() != userId) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_V2_RESULT_PUBLICATION_USER_MISMATCH");
            result.getBlockingReasons().add("User mismatch for official result draft.");
            return result;
        }

        boolean resultExists = examResultsProbe.existsResultForAttempt(draft.getAttemptId());
        Optional<V2ResultPublicationLedgerRecord> ledgerOpt = ledgerDAO.findBySubmitRecordId(submitRecordId);
        boolean ledgerExists = ledgerOpt.isPresent();

        if (resultExists && ledgerExists) {
            result.setSuccess(true);
            result.setIdempotent(true);
            result.setReady(true);
            result.setPublicationStatus("EXAM_RESULTS_WRITTEN");
            result.setPublicationLedgerId(ledgerOpt.get().getId());
            return result;
        }

        if (resultExists && !ledgerExists) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_V2_RESULT_PUBLICATION_INCONSISTENT_EXISTING_RESULT");
            result.getBlockingReasons().add("exam_results exists but ledger missing. Unsafe state.");
            return result;
        }

        if (!resultExists && ledgerExists) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_V2_RESULT_PUBLICATION_INCONSISTENT_EXISTING_RESULT");
            result.getBlockingReasons().add("Ledger exists but exam_results missing. Unsafe state.");
            return result;
        }

        // Neither exist, proceed with transaction
        V2ResultPublicationLedgerRecord ledgerRecord = new V2ResultPublicationLedgerRecord();
        ledgerRecord.setSubmitRecordId(submitRecordId);
        ledgerRecord.setOfficialResultDraftId(draft.getId());
        ledgerRecord.setScoreDraftId(draft.getScoreDraftId());
        ledgerRecord.setUserId(draft.getUserId());
        ledgerRecord.setExamId(draft.getExamId());
        ledgerRecord.setPaperId(draft.getPaperId());
        ledgerRecord.setAttemptId(draft.getAttemptId());
        ledgerRecord.setPayloadHash(draft.getPayloadHash());
        ledgerRecord.setRawScore(draft.getRawScore());
        ledgerRecord.setMaxScore(draft.getMaxScore());
        ledgerRecord.setPercentage(draft.getPercentage());
        ledgerRecord.setPublicationStatus("EXAM_RESULTS_WRITTEN");
        ledgerRecord.setPublishedAt(new Timestamp(System.currentTimeMillis()));

        return executeTransaction(submitRecordId, draft, ledgerRecord);
    }

    protected V2ResultPublicationResult executeTransaction(long submitRecordId, V2OfficialResultDraftRecord draft, V2ResultPublicationLedgerRecord ledgerRecord) {
        V2ResultPublicationResult result = new V2ResultPublicationResult();
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean resultInserted = examResultsWriteDAO.insertResultIfAbsent(conn, draft);
                if (!resultInserted) {
                    conn.rollback();
                    result.setSuccess(false);
                    result.setReady(false);
                    result.setErrorCode("ERROR_V2_RESULT_PUBLICATION_INSERT_RESULT_FAILED");
                    result.getBlockingReasons().add("Failed to insert into exam_results.");
                    return result;
                }

                boolean ledgerInserted = ledgerDAO.insertLedger(conn, ledgerRecord);
                if (!ledgerInserted) {
                    conn.rollback();
                    result.setSuccess(false);
                    result.setReady(false);
                    result.setErrorCode("ERROR_V2_RESULT_PUBLICATION_INSERT_LEDGER_FAILED");
                    result.getBlockingReasons().add("Failed to insert publication ledger.");
                    return result;
                }

                conn.commit();
                result.setSuccess(true);
                result.setReady(true);
                result.setIdempotent(false);
                result.setPublicationStatus("EXAM_RESULTS_WRITTEN");
                result.setPublicationLedgerId(ledgerRecord.getId());
                return result;
            } catch (SQLException e) {
                conn.rollback();
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("ERROR_V2_RESULT_PUBLICATION_TRANSACTION_FAILED");
                result.getBlockingReasons().add("Database transaction failed: " + e.getMessage());
                return result;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_V2_RESULT_PUBLICATION_TRANSACTION_FAILED");
            result.getBlockingReasons().add("Could not open database connection: " + e.getMessage());
            return result;
        }
    }
}
