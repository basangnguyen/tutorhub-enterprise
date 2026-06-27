package com.mycompany.tutorhub_enterprise.server.services;

import java.util.ArrayList;
import java.util.List;

public class V2ResultPublicationResult {
    private boolean success;
    private boolean ready;
    private boolean idempotent;
    private String errorCode;
    private long publicationLedgerId;
    private long officialResultDraftId;
    private long scoreDraftId;
    private long submitRecordId;
    private int userId;
    private int examId;
    private int paperId;
    private String attemptId;
    private String payloadHash;
    private double rawScore;
    private double maxScore;
    private double percentage;
    private String publicationStatus;
    private long publishedAt;
    private List<String> warnings = new ArrayList<>();
    private List<String> blockingReasons = new ArrayList<>();

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public boolean isIdempotent() { return idempotent; }
    public void setIdempotent(boolean idempotent) { this.idempotent = idempotent; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public long getPublicationLedgerId() { return publicationLedgerId; }
    public void setPublicationLedgerId(long publicationLedgerId) { this.publicationLedgerId = publicationLedgerId; }

    public long getOfficialResultDraftId() { return officialResultDraftId; }
    public void setOfficialResultDraftId(long officialResultDraftId) { this.officialResultDraftId = officialResultDraftId; }

    public long getScoreDraftId() { return scoreDraftId; }
    public void setScoreDraftId(long scoreDraftId) { this.scoreDraftId = scoreDraftId; }

    public long getSubmitRecordId() { return submitRecordId; }
    public void setSubmitRecordId(long submitRecordId) { this.submitRecordId = submitRecordId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getExamId() { return examId; }
    public void setExamId(int examId) { this.examId = examId; }

    public int getPaperId() { return paperId; }
    public void setPaperId(int paperId) { this.paperId = paperId; }

    public String getAttemptId() { return attemptId; }
    public void setAttemptId(String attemptId) { this.attemptId = attemptId; }

    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }

    public double getRawScore() { return rawScore; }
    public void setRawScore(double rawScore) { this.rawScore = rawScore; }

    public double getMaxScore() { return maxScore; }
    public void setMaxScore(double maxScore) { this.maxScore = maxScore; }

    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }

    public String getPublicationStatus() { return publicationStatus; }
    public void setPublicationStatus(String publicationStatus) { this.publicationStatus = publicationStatus; }

    public long getPublishedAt() { return publishedAt; }
    public void setPublishedAt(long publishedAt) { this.publishedAt = publishedAt; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public List<String> getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(List<String> blockingReasons) { this.blockingReasons = blockingReasons; }
}
