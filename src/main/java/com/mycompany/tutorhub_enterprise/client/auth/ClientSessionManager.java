package com.mycompany.tutorhub_enterprise.client.auth;

import com.mycompany.tutorhub_enterprise.models.auth.SessionInfo;

public class ClientSessionManager {
    private static SessionInfo currentSession;
    private static Integer currentUserId;
    private static String currentRole;

    public static void setSession(SessionInfo sessionInfo, Integer userId, String role) {
        currentSession = sessionInfo;
        currentUserId = userId;
        currentRole = role;
        System.out.println("[CLIENT_SESSION] session stored in memory: " + (sessionInfo != null));
    }

    public static SessionInfo getSession() {
        return currentSession;
    }

    public static String getAccessToken() {
        return currentSession != null ? currentSession.getAccessToken() : null;
    }

    public static boolean hasActiveSession() {
        return currentSession != null && currentSession.getAccessToken() != null && !currentSession.getAccessToken().isEmpty();
    }

    public static void clear() {
        currentSession = null;
        currentUserId = null;
        currentRole = null;
        System.out.println("[CLIENT_SESSION] session cleared");
    }
}
