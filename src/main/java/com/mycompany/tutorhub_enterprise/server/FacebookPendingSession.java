package com.mycompany.tutorhub_enterprise.server;

import com.mycompany.tutorhub_enterprise.models.auth.AuthResponse;

public class FacebookPendingSession {
    public enum Status {
        PENDING, SUCCESS, FAILED
    }

    private final String sessionId;
    private final String state;
    private Status status;
    private AuthResponse payload;
    private String errorMessage;
    private final long createdAt;
    private final long expiresAt;

    public FacebookPendingSession(String sessionId, String state) {
        this.sessionId = sessionId;
        this.state = state;
        this.status = Status.PENDING;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = this.createdAt + (5 * 60 * 1000); // 5 minutes
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getState() {
        return state;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public AuthResponse getPayload() {
        return payload;
    }

    public void setPayload(AuthResponse payload) {
        this.payload = payload;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
