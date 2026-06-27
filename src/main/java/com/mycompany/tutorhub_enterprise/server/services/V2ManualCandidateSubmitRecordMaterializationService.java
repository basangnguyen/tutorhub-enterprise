package com.mycompany.tutorhub_enterprise.server.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mycompany.tutorhub_enterprise.models.exam.V2ManualCandidateSubmitRecordLedgerRecord;
import com.mycompany.tutorhub_enterprise.models.exam.V2SubmitRecord;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;
import com.mycompany.tutorhub_enterprise.server.dao.V2ManualCandidateSubmitRecordLedgerDAO;
import com.mycompany.tutorhub_enterprise.server.dao.V2SubmitRecordDAO;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public class V2ManualCandidateSubmitRecordMaterializationService {

    private final V2ManualCandidateActualSubmitPreflightService preflightService;
    private final V2AnswerPayloadContractValidator contractValidator;
    private final V2SubmitRecordDAO submitRecordDAO;
    private final V2ManualCandidateSubmitRecordLedgerDAO ledgerDAO;

    public V2ManualCandidateSubmitRecordMaterializationService() {
        this(
                new V2ManualCandidateActualSubmitPreflightService(),
                new V2AnswerPayloadContractValidator(),
                new V2SubmitRecordDAO(),
                new V2ManualCandidateSubmitRecordLedgerDAO()
        );
    }

    public V2ManualCandidateSubmitRecordMaterializationService(
            V2ManualCandidateActualSubmitPreflightService preflightService,
            V2AnswerPayloadContractValidator contractValidator,
            V2SubmitRecordDAO submitRecordDAO,
            V2ManualCandidateSubmitRecordLedgerDAO ledgerDAO) {
        this.preflightService = preflightService;
        this.contractValidator = contractValidator;
        this.submitRecordDAO = submitRecordDAO;
        this.ledgerDAO = ledgerDAO;
    }

    public V2ManualCandidateSubmitRecordMaterializationResult materializeSubmitRecord(int userId, String attemptId, String payloadJson) {
        V2ManualCandidateSubmitRecordMaterializationResult result = new V2ManualCandidateSubmitRecordMaterializationResult();
        result.setSuccess(true);
        result.setReady(false);
        result.setIdempotent(false);
        result.setUserId(userId);
        result.setAttemptId(attemptId);

        if (!V2SubmitFeatureFlags.isManualCandidateSubmitRecordMaterializationEnabled()) {
            result.setMaterializationStatus("NOT_READY");
            result.getBlockingReasons().add("tse.v2.manualCandidateSubmitRecordMaterialization.enabled is false");
            return result;
        }

        // 1. Check Preflight
        V2ManualCandidateActualSubmitPreflightResult preflightResult = preflightService.checkPreflight(userId, attemptId, payloadJson);
        if (!preflightResult.isReady()) {
            result.setMaterializationStatus("NOT_READY");
            result.getBlockingReasons().add("Preflight not ready: " + String.join(", ", preflightResult.getBlockingReasons()));
            return result;
        }

        result.setExamId(preflightResult.getExamId());
        result.setPaperId(preflightResult.getPaperId());

        // 2. Validate payload contract
        V2AnswerPayloadContractValidationResult contractRes = contractValidator.validate(payloadJson);
        if (!contractRes.isValid()) {
            result.setMaterializationStatus("NOT_READY");
            result.getBlockingReasons().add("Payload contract invalid: " + String.join(", ", contractRes.getBlockingReasons()));
            return result;
        }

        // 3. Compute SHA-256 payloadHash
        String payloadHash = computeSha256(payloadJson);
        result.setPayloadHash(payloadHash);

        try {
            // 4. Check idempotency
            Optional<V2SubmitRecord> existingRecordOpt = submitRecordDAO.findLatestByAttemptId(attemptId);
            Optional<V2ManualCandidateSubmitRecordLedgerRecord> existingLedgerOpt = ledgerDAO.findByAttemptId(attemptId);

            boolean hasRecord = existingRecordOpt.isPresent();
            boolean hasLedger = existingLedgerOpt.isPresent();

            if (hasRecord && hasLedger) {
                result.setReady(true);
                result.setIdempotent(true);
                result.setMaterializationStatus("V2_SUBMIT_RECORD_MATERIALIZED");
                result.setSubmitRecordId(existingRecordOpt.get().getId());
                return result;
            } else if (hasRecord && !hasLedger) {
                result.setMaterializationStatus("ERROR_UNSAFE_STATE");
                result.getBlockingReasons().add("V2SubmitRecord exists but ledger is missing");
                return result;
            } else if (!hasRecord && hasLedger) {
                result.setMaterializationStatus("ERROR_UNSAFE_STATE");
                result.getBlockingReasons().add("Ledger exists but V2SubmitRecord is missing");
                return result;
            }

            // 5. Open transaction and insert
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);

                try {
                    // Create V2SubmitRecord
                    V2SubmitRecord record = new V2SubmitRecord();
                    record.setUserId(userId);
                    record.setExamId(result.getExamId());
                    record.setPaperId(result.getPaperId());
                    record.setAttemptId(attemptId);
                    record.setPackageHash(""); // or some fallback
                    record.setPayloadHash(payloadHash);
                    record.setPayloadJson(payloadJson); // ONLY PLACE WHERE payloadJson IS WRITTEN TO DB
                    record.setAnsweredCount(contractRes.getAnswerCount());
                    record.setUnansweredCount(0); // Optional: Option A ignores unanswered.
                    record.setComplete(true);
                    record.setSubmitStatus("RECEIVED_MANUAL");
                    record.setSource("V2_MANUAL_CANDIDATE");

                    long submitRecordId = submitRecordDAO.insertSubmitRecord(conn, record);
                    if (submitRecordId <= 0) {
                        throw new SQLException("Failed to insert V2SubmitRecord");
                    }

                    // Create Ledger Record
                    V2ManualCandidateSubmitRecordLedgerRecord ledger = new V2ManualCandidateSubmitRecordLedgerRecord();
                    ledger.setAttemptId(attemptId);
                    ledger.setSubmitRecordId(submitRecordId);
                    ledger.setUserId(userId);
                    ledger.setExamId(result.getExamId());
                    ledger.setPaperId(result.getPaperId());
                    ledger.setPayloadHash(payloadHash);
                    ledger.setMaterializationStatus("V2_SUBMIT_RECORD_MATERIALIZED");

                    long ledgerId = ledgerDAO.insertLedger(conn, ledger);
                    if (ledgerId <= 0) {
                        throw new SQLException("Failed to insert materialization ledger");
                    }

                    conn.commit();

                    result.setReady(true);
                    result.setSubmitRecordId(submitRecordId);
                    result.setMaterializationStatus("V2_SUBMIT_RECORD_MATERIALIZED");

                } catch (Exception e) {
                    conn.rollback();
                    result.setSuccess(false);
                    result.setErrorCode("DB_ERROR");
                    result.setMaterializationStatus("ERROR_ROLLBACK");
                    result.getBlockingReasons().add("Transaction rolled back due to error: " + e.getMessage());
                } finally {
                    conn.setAutoCommit(true);
                }
            }

        } catch (SQLException e) {
            result.setSuccess(false);
            result.setErrorCode("DB_ERROR");
            result.setMaterializationStatus("ERROR");
            result.getBlockingReasons().add("Database error checking existing records: " + e.getMessage());
        }

        return result;
    }

    private String computeSha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "0000000000000000000000000000000000000000000000000000000000000000";
        }
    }
}
