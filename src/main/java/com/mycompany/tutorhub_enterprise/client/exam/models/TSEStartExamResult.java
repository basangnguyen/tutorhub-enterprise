package com.mycompany.tutorhub_enterprise.client.exam.models;

public class TSEStartExamResult {
    public boolean success;
    public String message;
    public String errorCode;
    
    public String sessionId;
    public String examUrl;
    public String htmlContent;
    public int questionCount;
    
    public TSEStartExamResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
