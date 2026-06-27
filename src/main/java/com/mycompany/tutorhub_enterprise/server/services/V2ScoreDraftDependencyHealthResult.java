package com.mycompany.tutorhub_enterprise.server.services;

import java.util.ArrayList;
import java.util.List;

public class V2ScoreDraftDependencyHealthResult {
    private boolean success;
    private boolean ready;
    private String errorCode;
    private int userId;
    private int examId;
    private int paperId;
    private String attemptId;
    private boolean payloadParserAvailable;
    private boolean answerKeyResolverAvailable;
    private boolean schemaVerified;
    private String healthStatus;
    private List<String> warnings = new ArrayList<>();
    private List<String> blockingReasons = new ArrayList<>();
    private long checkedAt;

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
    public boolean isPayloadParserAvailable() { return payloadParserAvailable; }
    public void setPayloadParserAvailable(boolean payloadParserAvailable) { this.payloadParserAvailable = payloadParserAvailable; }
    public boolean isAnswerKeyResolverAvailable() { return answerKeyResolverAvailable; }
    public void setAnswerKeyResolverAvailable(boolean answerKeyResolverAvailable) { this.answerKeyResolverAvailable = answerKeyResolverAvailable; }
    public boolean isSchemaVerified() { return schemaVerified; }
    public void setSchemaVerified(boolean schemaVerified) { this.schemaVerified = schemaVerified; }
    public String getHealthStatus() { return healthStatus; }
    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }
    public List<String> getWarnings() { return warnings; }
    public void addWarning(String warning) { this.warnings.add(warning); }
    public List<String> getBlockingReasons() { return blockingReasons; }
    public void addBlockingReason(String reason) { this.blockingReasons.add(reason); }
    public long getCheckedAt() { return checkedAt; }
    public void setCheckedAt(long checkedAt) { this.checkedAt = checkedAt; }
}
