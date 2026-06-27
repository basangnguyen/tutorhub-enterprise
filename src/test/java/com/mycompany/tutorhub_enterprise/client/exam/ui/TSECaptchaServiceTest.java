package com.mycompany.tutorhub_enterprise.client.exam.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TSECaptchaServiceTest {

    @Test
    void generatedChallengeIsSixDigitsAndDisplayIsSpaced() {
        TSECaptchaService service = new TSECaptchaService();

        assertTrue(service.getChallenge().matches("\\d{6}"));
        assertEquals(11, service.getDisplayText().length());
        assertTrue(service.getDisplayText().matches("\\d \\d \\d \\d \\d \\d"));
    }

    @Test
    void verificationAcceptsWhitespaceAroundOrInsideCode() {
        TSECaptchaService service = new TSECaptchaService();
        String code = service.getChallenge();

        assertTrue(service.verify(code));
        assertTrue(service.verify(" " + service.getDisplayText() + " "));
        assertFalse(service.verify(null));
        assertFalse(service.verify(code.substring(0, 5)));
    }

    @Test
    void regenerateChangesChallengeEventually() {
        TSECaptchaService service = new TSECaptchaService();
        String first = service.getChallenge();

        boolean changed = false;
        for (int i = 0; i < 8; i++) {
            if (!first.equals(service.regenerate())) {
                changed = true;
                break;
            }
        }

        assertTrue(changed);
        assertNotEquals("", service.getChallenge());
    }
}
