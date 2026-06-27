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
    public static final String AUTH_SOCIAL_LOGIN = "AUTH_SOCIAL_LOGIN";
    public static final String AUTH_FACEBOOK_START = "AUTH_FACEBOOK_START";
    public static final String AUTH_FACEBOOK_POLL = "AUTH_FACEBOOK_POLL";
    public static final String AUTH_LOGOUT = "AUTH_LOGOUT";
    
    public static final String CALENDAR_SEND_EVENT_INVITE = "CALENDAR_SEND_EVENT_INVITE";
    public static final String CALENDAR_SEND_POLL_INVITE = "CALENDAR_SEND_POLL_INVITE";

    // Locket Actions
    public static final String LOCKET_POST_LIST = "LOCKET_POST_LIST";
    public static final String LOCKET_POST_CREATE = "LOCKET_POST_CREATE";
    public static final String LOCKET_POST_DELETE = "LOCKET_POST_DELETE";
    public static final String LOCKET_POST_REACT = "LOCKET_POST_REACT";
    public static final String LOCKET_COMMENT_LIST = "LOCKET_COMMENT_LIST";
    public static final String LOCKET_COMMENT_CREATE = "LOCKET_COMMENT_CREATE";
    public static final String LOCKET_COMMENT_DELETE = "LOCKET_COMMENT_DELETE";

    public static final String LOCKET_POST_LIST_SUCCESS = "LOCKET_POST_LIST_SUCCESS";
    public static final String LOCKET_POST_CREATE_SUCCESS = "LOCKET_POST_CREATE_SUCCESS";
    public static final String LOCKET_POST_DELETE_SUCCESS = "LOCKET_POST_DELETE_SUCCESS";
    public static final String LOCKET_POST_REACT_SUCCESS = "LOCKET_POST_REACT_SUCCESS";
    public static final String LOCKET_COMMENT_LIST_SUCCESS = "LOCKET_COMMENT_LIST_SUCCESS";
    public static final String LOCKET_COMMENT_CREATE_SUCCESS = "LOCKET_COMMENT_CREATE_SUCCESS";
    public static final String LOCKET_COMMENT_DELETE_SUCCESS = "LOCKET_COMMENT_DELETE_SUCCESS";
    public static final String LOCKET_ERROR = "LOCKET_ERROR";

    // Standard Success/Error Messages
    public static final String MSG_INVALID_CREDENTIALS = "Tên đăng nhập hoặc mật khẩu không đúng.";

    private AuthProtocol() {
    }
}
