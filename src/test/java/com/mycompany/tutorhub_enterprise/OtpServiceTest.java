package com.mycompany.tutorhub_enterprise;

import com.mycompany.tutorhub_enterprise.server.OtpService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpServiceTest {

    @Test
    void issuedOtpCanBeVerifiedOnlyOnce() {
        String target = uniqueEmail("once");
        OtpService.IssueResult issue = OtpService.issue(target, OtpService.PURPOSE_REGISTER);

        assertTrue(issue.isIssued());
        assertTrue(OtpService.verify(target, OtpService.PURPOSE_REGISTER, issue.getCode()).isSuccess());
        assertFalse(OtpService.verify(target, OtpService.PURPOSE_REGISTER, issue.getCode()).isSuccess());
    }

    @Test
    void issueIsRateLimitedForSameTargetAndPurpose() {
        String target = uniqueEmail("rate");
        OtpService.IssueResult first = OtpService.issue(target, OtpService.PURPOSE_RESET_PASSWORD);
        OtpService.IssueResult second = OtpService.issue(target, OtpService.PURPOSE_RESET_PASSWORD);

        assertTrue(first.isIssued());
        assertFalse(second.isIssued());
        assertTrue(second.getRetryAfterSeconds() > 0);
    }

    @Test
    void invalidAttemptsEventuallyLockTheChallenge() {
        String target = uniqueEmail("attempts");
        OtpService.IssueResult issue = OtpService.issue(target, OtpService.PURPOSE_REGISTER);

        for (int i = 0; i < 5; i++) {
            assertFalse(OtpService.verify(target, OtpService.PURPOSE_REGISTER, "000000").isSuccess());
        }

        assertFalse(OtpService.verify(target, OtpService.PURPOSE_REGISTER, issue.getCode()).isSuccess());
    }

    private static String uniqueEmail(String label) {
        return label + "-" + System.nanoTime() + "@example.com";
    }
}
