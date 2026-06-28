package com.mycompany.tutorhub_enterprise.client.quizhub;

/**
 * Exception dùng chung cho toàn bộ module QuizHub (import, storage, service).
 * QuizHubBridge sẽ bắt exception này (và mọi RuntimeException khác) để trả JSON
 * {"ok":false,"message":"..."} thay vì làm crash phía WebView/JS.
 */
public class QuizHubException extends RuntimeException {

    public QuizHubException(String message) {
        super(message);
    }

    public QuizHubException(String message, Throwable cause) {
        super(message, cause);
    }
}
