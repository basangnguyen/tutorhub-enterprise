package com.mycompany.tutorhub_enterprise.client.exam.models;

public class TSESubmitResult {
    public boolean success;
    public String message;
    public String errorCode;
    public String submitStatus;
    
    public TSESubmitResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
