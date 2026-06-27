package com.mycompany.tutorhub_enterprise.client.exam.ui;

import java.security.SecureRandom;

/**
 * Small local captcha challenge used by the Secure Exam login screen.
 *
 * This is a UI-side guard only. Server authentication and rate limiting remain
 * the real security boundary.
 */
final class TSECaptchaService {

    private static final char[] DIGITS = "0123456789".toCharArray();
    private static final int DEFAULT_LENGTH = 6;

    private final SecureRandom random;
    private String challenge;

    TSECaptchaService() {
        this(new SecureRandom());
    }

    TSECaptchaService(SecureRandom random) {
        this.random = random != null ? random : new SecureRandom();
        regenerate();
    }

    String regenerate() {
        StringBuilder value = new StringBuilder(DEFAULT_LENGTH);
        for (int i = 0; i < DEFAULT_LENGTH; i++) {
            value.append(DIGITS[random.nextInt(DIGITS.length)]);
        }
        challenge = value.toString();
        return challenge;
    }

    String getChallenge() {
        return challenge;
    }

    String getDisplayText() {
        StringBuilder display = new StringBuilder(DEFAULT_LENGTH * 2 - 1);
        for (int i = 0; i < challenge.length(); i++) {
            if (i > 0) {
                display.append(' ');
            }
            display.append(challenge.charAt(i));
        }
        return display.toString();
    }

    boolean verify(String input) {
        if (input == null) {
            return false;
        }
        return challenge.equals(stripWhitespace(input));
    }

    private String stripWhitespace(String value) {
        StringBuilder normalized = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!Character.isWhitespace(c)) {
                normalized.append(c);
            }
        }
        return normalized.toString();
    }
}
