package com.mycompany.tutorhub_enterprise.models.auth;

import java.io.Serializable;

public final class AuthResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String requestId;
    private final boolean success;
    private final String message;
    private final String dashboardPayload;
    private SessionInfo sessionInfo;

    public AuthResponse(String requestId, boolean success, String message, String dashboardPayload) {
        this.requestId = requestId == null ? "" : requestId.trim();
        this.success = success;
        this.message = message == null ? "" : message;
        this.dashboardPayload = dashboardPayload == null ? "" : dashboardPayload;
    }

    public static AuthResponse ok(String requestId, String message) {
        return new AuthResponse(requestId, true, message, "");
    }

    public static AuthResponse fail(String requestId, String message) {
        return new AuthResponse(requestId, false, message, "");
    }

    public static AuthResponse login(String requestId, String message, String dashboardPayload) {
        return new AuthResponse(requestId, true, message, dashboardPayload);
    }
    
    public static AuthResponse loginWithSession(String requestId, String message, String dashboardPayload, SessionInfo sessionInfo) {
        AuthResponse response = new AuthResponse(requestId, true, message, dashboardPayload);
        response.setSessionInfo(sessionInfo);
        return response;
    }

    public String getRequestId() {
        return requestId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getDashboardPayload() {
        return dashboardPayload;
    }

    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    public void setSessionInfo(SessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
    }
}
