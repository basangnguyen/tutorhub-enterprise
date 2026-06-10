package com.mycompany.tutorhub_enterprise.models.auth;

public final class AuthProtocol {
    public static final String RESPONSE = "AUTH_RESPONSE";

    public static final String LOGIN = "AUTH_LOGIN";
    public static final String REQUEST_REGISTRATION_OTP = "AUTH_REQUEST_REGISTRATION_OTP";
    public static final String VERIFY_AND_REGISTER = "AUTH_VERIFY_AND_REGISTER";
    public static final String REQUEST_PASSWORD_RESET_OTP = "AUTH_REQUEST_PASSWORD_RESET_OTP";
    public static final String VERIFY_AND_RESET_PASSWORD = "AUTH_VERIFY_AND_RESET_PASSWORD";
    public static final String REQUEST_SMS_LOGIN_OTP = "AUTH_REQUEST_SMS_LOGIN_OTP";
    public static final String VERIFY_SMS_LOGIN = "AUTH_VERIFY_SMS_LOGIN";
    public static final String REQUEST_PHONE_VERIFICATION_OTP = "AUTH_REQUEST_PHONE_VERIFICATION_OTP";
    public static final String VERIFY_PHONE_OTP = "AUTH_VERIFY_PHONE_OTP";

    private AuthProtocol() {
    }
}
