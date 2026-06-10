package com.mycompany.tutorhub_enterprise.client.exam.models;

public class TSELoginResult {
    public boolean success;
    public String message;
    public String errorCode;
    
    public TSEExamContext context; // Populated if success
    
    public TSELoginResult(boolean success, String message, TSEExamContext context) {
        this.success = success;
        this.message = message;
        this.context = context;
    }
}
