package com.mycompany.tutorhub_enterprise.client.exam.services;

import com.mycompany.tutorhub_enterprise.client.exam.models.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface TSEExamService {
    
    /**
     * Authenticate user for TSE context
     */
    CompletableFuture<TSELoginResult> login(String username, String password);
    
    /**
     * Get available exam configurations for the logged-in user
     */
    CompletableFuture<List<TSEExamConfig>> getConfigList(int userId);
    
    /**
     * Verify password for a specific exam config and retrieve exam session / URL
     */
    CompletableFuture<TSEStartExamResult> verifyPasswordAndStart(int userId, int examId, String password);
    
    /**
     * Submit exam payload
     */
    CompletableFuture<TSESubmitResult> submitExam(String sessionId, int examId, String payload);
}
