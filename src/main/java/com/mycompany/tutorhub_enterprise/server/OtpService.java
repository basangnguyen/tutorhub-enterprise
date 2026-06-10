package com.mycompany.tutorhub_enterprise.server;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.mindrot.jbcrypt.BCrypt;

public final class OtpService {

    public static final String PURPOSE_REGISTER = "REGISTER";
    public static final String PURPOSE_RESET_PASSWORD = "RESET_PASSWORD";
    public static final String PURPOSE_SMS_LOGIN = "SMS_LOGIN";
    public static final String PURPOSE_VERIFY_PHONE = "VERIFY_PHONE";

    private static final int OTP_DIGITS = 6;
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final long OTP_TTL_MILLIS = Duration.ofMinutes(10).toMillis();
    private static final long ISSUE_COOLDOWN_MILLIS = Duration.ofSeconds(60).toMillis();

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ConcurrentHashMap<String, Challenge> challenges = new ConcurrentHashMap<>();

    private OtpService() {
    }

    public static IssueResult issue(String target, String purpose) {
        String key = key(target, purpose);
        long now = System.currentTimeMillis();
        Challenge existing = challenges.get(key);
        if (existing != null && !existing.isExpired(now) && existing.nextIssueAtMillis > now) {
            long seconds = Math.max(1, (existing.nextIssueAtMillis - now + 999) / 1000);
            return IssueResult.rateLimited(seconds);
        }

        String code = generateCode();
        Challenge challenge = new Challenge(
                BCrypt.hashpw(code, BCrypt.gensalt(10)),
                now + OTP_TTL_MILLIS,
                now + ISSUE_COOLDOWN_MILLIS
        );
        challenges.put(key, challenge);
        return IssueResult.issued(code, OTP_TTL_MILLIS / 1000);
    }

    public static VerifyResult verify(String target, String purpose, String inputCode) {
        String key = key(target, purpose);
        Challenge challenge = challenges.get(key);
        long now = System.currentTimeMillis();
        if (challenge == null) {
            return VerifyResult.notFound();
        }
        if (challenge.isExpired(now)) {
            challenges.remove(key);
            return VerifyResult.expired();
        }
        if (challenge.failedAttempts >= MAX_VERIFY_ATTEMPTS) {
            challenges.remove(key);
            return VerifyResult.tooManyAttempts();
        }
        if (ServerConfig.isBlank(inputCode) || !BCrypt.checkpw(inputCode.trim(), challenge.codeHash)) {
            challenge.failedAttempts++;
            if (challenge.failedAttempts >= MAX_VERIFY_ATTEMPTS) {
                challenges.remove(key);
                return VerifyResult.tooManyAttempts();
            }
            return VerifyResult.invalid(MAX_VERIFY_ATTEMPTS - challenge.failedAttempts);
        }

        challenges.remove(key);
        return VerifyResult.success();
    }

    public static void clear(String target, String purpose) {
        challenges.remove(key(target, purpose));
    }

    private static String key(String target, String purpose) {
        return purpose + ":" + normalizeTarget(target);
    }

    private static String normalizeTarget(String target) {
        return target == null ? "" : target.trim().toLowerCase(Locale.ROOT);
    }

    private static String generateCode() {
        int bound = (int) Math.pow(10, OTP_DIGITS);
        return String.format("%0" + OTP_DIGITS + "d", RANDOM.nextInt(bound));
    }

    private static final class Challenge {
        private final String codeHash;
        private final long expiresAtMillis;
        private final long nextIssueAtMillis;
        private int failedAttempts;

        private Challenge(String codeHash, long expiresAtMillis, long nextIssueAtMillis) {
            this.codeHash = codeHash;
            this.expiresAtMillis = expiresAtMillis;
            this.nextIssueAtMillis = nextIssueAtMillis;
        }

        private boolean isExpired(long now) {
            return now > expiresAtMillis;
        }
    }

    public static final class IssueResult {
        private final boolean issued;
        private final String code;
        private final long retryAfterSeconds;
        private final long expiresInSeconds;

        private IssueResult(boolean issued, String code, long retryAfterSeconds, long expiresInSeconds) {
            this.issued = issued;
            this.code = code;
            this.retryAfterSeconds = retryAfterSeconds;
            this.expiresInSeconds = expiresInSeconds;
        }

        public static IssueResult issued(String code, long expiresInSeconds) {
            return new IssueResult(true, code, 0, expiresInSeconds);
        }

        public static IssueResult rateLimited(long retryAfterSeconds) {
            return new IssueResult(false, null, retryAfterSeconds, 0);
        }

        public boolean isIssued() {
            return issued;
        }

        public String getCode() {
            return code;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }

        public long getExpiresInSeconds() {
            return expiresInSeconds;
        }
    }

    public static final class VerifyResult {
        private final boolean success;
        private final String message;
        private final int attemptsRemaining;

        private VerifyResult(boolean success, String message, int attemptsRemaining) {
            this.success = success;
            this.message = message;
            this.attemptsRemaining = attemptsRemaining;
        }

        public static VerifyResult success() {
            return new VerifyResult(true, "OTP verified.", MAX_VERIFY_ATTEMPTS);
        }

        public static VerifyResult invalid(int attemptsRemaining) {
            return new VerifyResult(false, "Invalid OTP.", attemptsRemaining);
        }

        public static VerifyResult expired() {
            return new VerifyResult(false, "OTP expired.", 0);
        }

        public static VerifyResult notFound() {
            return new VerifyResult(false, "OTP not found.", 0);
        }

        public static VerifyResult tooManyAttempts() {
            return new VerifyResult(false, "Too many OTP attempts.", 0);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getAttemptsRemaining() {
            return attemptsRemaining;
        }
    }
}
