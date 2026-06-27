package com.mycompany.tutorhub_enterprise.server;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthRateLimitService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration WINDOW = Duration.ofMinutes(10);
    public static final Duration COOLDOWN = Duration.ofMinutes(10);

    private static final ConcurrentHashMap<String, AttemptState> ATTEMPTS = new ConcurrentHashMap<>();
    private static volatile Clock clock = Clock.systemUTC();

    private AuthRateLimitService() {
    }

    public static boolean checkAllowed(String identifier) {
        String key = normalizeIdentifier(identifier);
        if (key.isEmpty()) {
            return true;
        }

        Instant now = now();
        AttemptState state = ATTEMPTS.get(key);
        if (state == null) {
            return true;
        }

        if (state.isBlocked(now)) {
            System.err.println("[AUTH_RATE_LIMIT] login temporarily blocked identifier=" + maskIdentifier(key));
            return false;
        }

        if (state.shouldExpire(now)) {
            ATTEMPTS.remove(key, state);
        }
        return true;
    }

    public static void recordFailure(String identifier) {
        String key = normalizeIdentifier(identifier);
        if (key.isEmpty()) {
            return;
        }

        Instant now = now();
        ATTEMPTS.compute(key, (ignored, current) -> {
            AttemptState state = current;
            if (state == null || state.shouldExpire(now) || state.blockExpired(now)) {
                state = new AttemptState(0, now, null);
            }

            if (state.isBlocked(now)) {
                return state;
            }

            int failures = state.failedAttempts + 1;
            Instant blockedUntil = failures >= MAX_FAILED_ATTEMPTS ? now.plus(COOLDOWN) : null;
            System.err.println("[AUTH_RATE_LIMIT] failed attempt recorded identifier="
                    + maskIdentifier(key) + " attempts=" + failures);
            if (blockedUntil != null) {
                System.err.println("[AUTH_RATE_LIMIT] login temporarily blocked identifier=" + maskIdentifier(key));
            }
            return new AttemptState(failures, state.windowStartedAt, blockedUntil);
        });
    }

    public static void recordSuccess(String identifier) {
        String key = normalizeIdentifier(identifier);
        if (key.isEmpty()) {
            return;
        }

        AttemptState state = ATTEMPTS.get(key);
        if (state != null && state.isBlocked(now())) {
            return;
        }

        AttemptState removed = ATTEMPTS.remove(key);
        if (removed != null) {
            System.err.println("[AUTH_RATE_LIMIT] reset after successful login identifier=" + maskIdentifier(key));
        }
    }

    public static long getRemainingCooldown(String identifier) {
        String key = normalizeIdentifier(identifier);
        if (key.isEmpty()) {
            return 0L;
        }

        AttemptState state = ATTEMPTS.get(key);
        if (state == null || state.blockedUntil == null) {
            return 0L;
        }

        long remainingMillis = Duration.between(now(), state.blockedUntil).toMillis();
        if (remainingMillis <= 0L) {
            return 0L;
        }
        return (remainingMillis + 999L) / 1000L;
    }

    public static String normalizeIdentifier(String identifier) {
        return identifier == null ? "" : identifier.trim().toLowerCase(Locale.ROOT);
    }

    public static String maskIdentifier(String identifier) {
        String normalized = normalizeIdentifier(identifier);
        if (normalized.isEmpty()) {
            return "<blank>";
        }

        int at = normalized.indexOf('@');
        if (at > 0 && at < normalized.length() - 1) {
            String local = normalized.substring(0, at);
            String domain = normalized.substring(at + 1);
            String prefix = local.substring(0, Math.min(2, local.length()));
            return prefix + "***@" + domain;
        }

        String prefix = normalized.substring(0, Math.min(2, normalized.length()));
        return prefix + "***";
    }

    static void resetForTest() {
        ATTEMPTS.clear();
        clock = Clock.systemUTC();
    }

    static void setClockForTest(Clock testClock) {
        clock = testClock == null ? Clock.systemUTC() : testClock;
    }

    private static Instant now() {
        return Instant.now(clock);
    }

    private static final class AttemptState {
        private final int failedAttempts;
        private final Instant windowStartedAt;
        private final Instant blockedUntil;

        private AttemptState(int failedAttempts, Instant windowStartedAt, Instant blockedUntil) {
            this.failedAttempts = failedAttempts;
            this.windowStartedAt = windowStartedAt;
            this.blockedUntil = blockedUntil;
        }

        private boolean isBlocked(Instant now) {
            return blockedUntil != null && now.isBefore(blockedUntil);
        }

        private boolean blockExpired(Instant now) {
            return blockedUntil != null && !now.isBefore(blockedUntil);
        }

        private boolean shouldExpire(Instant now) {
            return blockedUntil == null && !now.isBefore(windowStartedAt.plus(WINDOW));
        }
    }
}
