package com.mycompany.tutorhub_enterprise.server;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthRateLimitServiceTest {

    private MutableClock clock;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        AuthRateLimitService.resetForTest();
        AuthRateLimitService.setClockForTest(clock);
    }

    @AfterEach
    void tearDown() {
        AuthRateLimitService.resetForTest();
    }

    @Test
    void firstWrongPasswordDoesNotBlockLogin() {
        String email = "student@example.com";

        assertTrue(AuthRateLimitService.checkAllowed(email));
        AuthRateLimitService.recordFailure(email);

        assertTrue(AuthRateLimitService.checkAllowed(email));
    }

    @Test
    void fifthWrongPasswordStartsCooldown() {
        String email = "student@example.com";

        recordFailures(email, AuthRateLimitService.MAX_FAILED_ATTEMPTS);

        assertFalse(AuthRateLimitService.checkAllowed(email));
        assertTrue(AuthRateLimitService.getRemainingCooldown(email) > 0);
    }

    @Test
    void sixthAttemptDuringCooldownIsBlocked() {
        String email = "student@example.com";

        recordFailures(email, AuthRateLimitService.MAX_FAILED_ATTEMPTS);

        assertFalse(AuthRateLimitService.checkAllowed(email));
        AuthRateLimitService.recordFailure(email);
        assertFalse(AuthRateLimitService.checkAllowed(email));
    }

    @Test
    void correctPasswordCannotBypassActiveCooldown() {
        String email = "student@example.com";

        recordFailures(email, AuthRateLimitService.MAX_FAILED_ATTEMPTS);
        AuthRateLimitService.recordSuccess(email);

        assertFalse(AuthRateLimitService.checkAllowed(email));
    }

    @Test
    void cooldownExpiresAfterConfiguredDuration() {
        String email = "student@example.com";

        recordFailures(email, AuthRateLimitService.MAX_FAILED_ATTEMPTS);
        clock.advance(AuthRateLimitService.COOLDOWN.minusSeconds(1));
        assertFalse(AuthRateLimitService.checkAllowed(email));

        clock.advance(Duration.ofSeconds(1));
        assertTrue(AuthRateLimitService.checkAllowed(email));
    }

    @Test
    void successfulLoginResetsFailureCounter() {
        String email = "student@example.com";

        recordFailures(email, AuthRateLimitService.MAX_FAILED_ATTEMPTS - 1);
        AuthRateLimitService.recordSuccess(email);
        recordFailures(email, AuthRateLimitService.MAX_FAILED_ATTEMPTS - 1);

        assertTrue(AuthRateLimitService.checkAllowed(email));
    }

    @Test
    void identifierIsNormalizedBeforeCountingFailures() {
        recordFailures("  Student@Example.COM  ", AuthRateLimitService.MAX_FAILED_ATTEMPTS);

        assertFalse(AuthRateLimitService.checkAllowed("student@example.com"));
    }

    @Test
    void maskedIdentifierDoesNotExposeFullEmailLocalPart() {
        String masked = AuthRateLimitService.maskIdentifier("basangnguyen12@gmail.com");

        assertFalse(masked.contains("basangnguyen12"));
        assertTrue(masked.startsWith("ba***@"));
    }

    private static void recordFailures(String email, int count) {
        for (int i = 0; i < count; i++) {
            AuthRateLimitService.recordFailure(email);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
