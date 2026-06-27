package com.mycompany.tutorhub_enterprise.models.auth;

import java.io.Serializable;

public class SessionInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sessionId;
    private String accessToken;
    private long accessTokenExpiresAt;
    private String refreshToken;
    private long refreshTokenExpiresAt;
    private String tokenType;

    public SessionInfo() {}

    public SessionInfo(String sessionId, String accessToken, long accessTokenExpiresAt, String refreshToken, long refreshTokenExpiresAt, String tokenType) {
        this.sessionId = sessionId;
        this.accessToken = accessToken;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshToken = refreshToken;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
        this.tokenType = tokenType;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public long getAccessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }

    public void setAccessTokenExpiresAt(long accessTokenExpiresAt) {
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public long getRefreshTokenExpiresAt() {
        return refreshTokenExpiresAt;
    }

    public void setRefreshTokenExpiresAt(long refreshTokenExpiresAt) {
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    @Override
    public String toString() {
        return "SessionInfo{" +
                "sessionId='" + sessionId + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", accessTokenExpiresAt=" + accessTokenExpiresAt +
                ", hasAccessToken=" + (accessToken != null && !accessToken.isEmpty()) +
                '}';
    }
}
