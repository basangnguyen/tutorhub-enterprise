package com.mycompany.tutorhub_enterprise.server.services;

import java.util.ArrayList;
import java.util.List;

public class V2ResultPublicationVerificationResult {
    private boolean success;
    private boolean ready;
    private String errorCode;
    private int userId;
    private int examId;
    private int paperId;
    private String attemptId;
    private long submitRecordId;
    private long publicationLedgerId;
    private String payloadHash;
    private boolean examResultsExists;
    private boolean ledgerExists;
    private boolean officialDraftExists;
    private boolean scoreDraftExists;
    private String publicationStatus;
    private String verificationStatus;
    private long checkedAt;
    private List<String> warnings = new ArrayList<>();
    private List<String> blockingReasons = new ArrayList<>();

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getExamId() { return examId; }
    public void setExamId(int examId) { this.examId = examId; }

    public int getPaperId() { return paperId; }
    public void setPaperId(int paperId) { this.paperId = paperId; }

    public String getAttemptId() { return attemptId; }
    public void setAttemptId(String attemptId) { this.attemptId = attemptId; }

    public long getSubmitRecordId() { return submitRecordId; }
    public void setSubmitRecordId(long submitRecordId) { this.submitRecordId = submitRecordId; }

    public long getPublicationLedgerId() { return publicationLedgerId; }
    public void setPublicationLedgerId(long publicationLedgerId) { this.publicationLedgerId = publicationLedgerId; }

    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }

    public boolean isExamResultsExists() { return examResultsExists; }
    public void setExamResultsExists(boolean examResultsExists) { this.examResultsExists = examResultsExists; }

    public boolean isLedgerExists() { return ledgerExists; }
    public void setLedgerExists(boolean ledgerExists) { this.ledgerExists = ledgerExists; }

    public boolean isOfficialDraftExists() { return officialDraftExists; }
    public void setOfficialDraftExists(boolean officialDraftExists) { this.officialDraftExists = officialDraftExists; }

    public boolean isScoreDraftExists() { return scoreDraftExists; }
    public void setScoreDraftExists(boolean scoreDraftExists) { this.scoreDraftExists = scoreDraftExists; }

    public String getPublicationStatus() { return publicationStatus; }
    public void setPublicationStatus(String publicationStatus) { this.publicationStatus = publicationStatus; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public long getCheckedAt() { return checkedAt; }
    public void setCheckedAt(long checkedAt) { this.checkedAt = checkedAt; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public List<String> getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(List<String> blockingReasons) { this.blockingReasons = blockingReasons; }
}
