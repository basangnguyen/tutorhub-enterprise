package com.mycompany.tutorhub_enterprise.client.oauth;

public class OAuthCallbackResult {
    private final String code;
    private final String state;
    private final String error;
    private final String errorDescription;

    public OAuthCallbackResult(String code, String state, String error, String errorDescription) {
        this.code = code;
        this.state = state;
        this.error = error;
        this.errorDescription = errorDescription;
    }

    public String getCode() {
        return code;
    }

    public String getState() {
        return state;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public boolean isSuccess() {
        return code != null && !code.isEmpty() && error == null;
    }
}
