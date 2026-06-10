package com.mycompany.tutorhub_enterprise.client.exam.services;

import com.mycompany.tutorhub_enterprise.client.exam.models.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MockTSEExamService implements TSEExamService {
    
    @Override
    public CompletableFuture<TSELoginResult> login(String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            
            if ("admin".equals(username) && "admin".equals(password)) {
                TSEExamContext ctx = new TSEExamContext();
                ctx.username = username;
                ctx.userId = 1;
                ctx.token = "mock-token-12345";
                return new TSELoginResult(true, "Login success", ctx);
            }
            return new TSELoginResult(false, "Invalid credentials", null);
        });
    }

    @Override
    public CompletableFuture<List<TSEExamConfig>> getConfigList(int userId) {
        return CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            
            List<TSEExamConfig> list = new ArrayList<>();
            
            TSEExamConfig config1 = new TSEExamConfig();
            config1.examId = 101;
            config1.examTitle = "Mock Exam - Mathematics";
            config1.durationMinutes = 60;
            config1.configName = "Strict_Lockdown_V1";
            config1.configVersion = "1.0";
            config1.requiresPassword = true;
            config1.status = "PUBLISHED";
            
            TSEExamConfig config2 = new TSEExamConfig();
            config2.examId = 102;
            config2.examTitle = "Mock Exam - Physics";
            config2.durationMinutes = 90;
            config2.configName = "Standard_V2";
            config2.configVersion = "2.0";
            config2.requiresPassword = false;
            config2.status = "PUBLISHED";
            
            list.add(config1);
            list.add(config2);
            
            return list;
        });
    }

    @Override
    public CompletableFuture<TSEStartExamResult> verifyPasswordAndStart(int userId, int examId, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            
            if (password == null || password.isEmpty() || password.equals("123456")) {
                TSEStartExamResult res = new TSEStartExamResult(true, "Password verified");
                res.sessionId = "session-" + System.currentTimeMillis();
                
                // Demo URL or HTML
                res.htmlContent = "<html><body style='font-family:sans-serif; padding:20px;'>" +
                                  "<h1 style='color:#1D4ED8;'>TSE Exam Browser Area (Mocked)</h1>" +
                                  "<p>JCEF is running inside TSE Shell successfully. Exam ID: " + examId + "</p>" +
                                  "</body></html>";
                return res;
            }
            return new TSEStartExamResult(false, "Invalid password");
        });
    }

    @Override
    public CompletableFuture<TSESubmitResult> submitExam(String sessionId, int examId, String payload) {
        return CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            
            TSESubmitResult res = new TSESubmitResult(true, "Submitted successfully");
            res.submitStatus = "RECEIVED";
            return res;
        });
    }
}
