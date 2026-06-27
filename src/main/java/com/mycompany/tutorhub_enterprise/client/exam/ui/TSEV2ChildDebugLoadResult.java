package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyExamRenderModel;

public class TSEV2ChildDebugLoadResult {
    public TSEV2ReadOnlyExamRenderModel renderModel;
    public boolean success;
    public String errorCode;
    public String errorMessage;
    public int examId;
    public int paperId;
    public String attemptId;
    public String packageHash;
    public int questionCount;
    public double totalScore;
    public String deadlineAt;
    
    public boolean metaLoaded;
    public boolean hashVerified;
    public boolean keyFetched;
    public boolean decrypted;
    public boolean parsed;
    public boolean securityValidated;

    // Getters
    public TSEV2ReadOnlyExamRenderModel getRenderModel() { return renderModel; }
    public boolean isSuccess() { return success; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public int getExamId() { return examId; }
    public int getPaperId() { return paperId; }
    public String getAttemptId() { return attemptId; }
    public String getPackageHash() { return packageHash; }
    public int getQuestionCount() { return questionCount; }
    public double getTotalScore() { return totalScore; }
    public String getDeadlineAt() { return deadlineAt; }
    
    public boolean isMetaLoaded() { return metaLoaded; }
    public boolean isHashVerified() { return hashVerified; }
    public boolean isKeyFetched() { return keyFetched; }
    public boolean isDecrypted() { return decrypted; }
    public boolean isParsed() { return parsed; }
    public boolean isSecurityValidated() { return securityValidated; }

    // Setters
    public void setRenderModel(TSEV2ReadOnlyExamRenderModel renderModel) { this.renderModel = renderModel; }
    public void setSuccess(boolean success) { this.success = success; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setExamId(int examId) { this.examId = examId; }
    public void setPaperId(int paperId) { this.paperId = paperId; }
    public void setAttemptId(String attemptId) { this.attemptId = attemptId; }
    public void setPackageHash(String packageHash) { this.packageHash = packageHash; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
    public void setTotalScore(double totalScore) { this.totalScore = totalScore; }
    public void setDeadlineAt(String deadlineAt) { this.deadlineAt = deadlineAt; }
    
    public void setMetaLoaded(boolean metaLoaded) { this.metaLoaded = metaLoaded; }
    public void setHashVerified(boolean hashVerified) { this.hashVerified = hashVerified; }
    public void setKeyFetched(boolean keyFetched) { this.keyFetched = keyFetched; }
    public void setDecrypted(boolean decrypted) { this.decrypted = decrypted; }
    public void setParsed(boolean parsed) { this.parsed = parsed; }
    public void setSecurityValidated(boolean securityValidated) { this.securityValidated = securityValidated; }
}
