package com.mycompany.tutorhub_enterprise;

import com.mycompany.tutorhub_enterprise.server.PasswordPolicyService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PasswordPolicyServiceTest {

    @Test
    void testNullOrEmpty() {
        assertFalse(PasswordPolicyService.validate(null, "user@test.com").isValid());
        assertFalse(PasswordPolicyService.validate("", "user@test.com").isValid());
        assertFalse(PasswordPolicyService.validate("   ", "user@test.com").isValid());
    }

    @Test
    void testLength() {
        assertFalse(PasswordPolicyService.validate("1234567", "user@test.com").isValid());
        
        StringBuilder longPass = new StringBuilder();
        for (int i = 0; i < 129; i++) {
            longPass.append("a");
        }
        assertFalse(PasswordPolicyService.validate(longPass.toString(), "user@test.com").isValid());
        
        // 8 chars is valid
        assertTrue(PasswordPolicyService.validate("12345678a", "user@test.com").isValid());
    }

    @Test
    void testCommonPasswords() {
        assertFalse(PasswordPolicyService.validate("password", "user@test.com").isValid());
        assertFalse(PasswordPolicyService.validate("12345678", "user@test.com").isValid());
        assertFalse(PasswordPolicyService.validate("iloveyou", "user@test.com").isValid());
        assertFalse(PasswordPolicyService.validate("qwerty", "user@test.com").isValid());
        assertFalse(PasswordPolicyService.validate("admin", "user@test.com").isValid());
    }

    @Test
    void testMatchEmailOrUsername() {
        // match exact email
        assertFalse(PasswordPolicyService.validate("user@test.com", "user@test.com").isValid());
        
        // match email case insensitive
        assertFalse(PasswordPolicyService.validate("USER@TEST.COM", "user@test.com").isValid());
        
        // match username part of email
        assertFalse(PasswordPolicyService.validate("user", "user@test.com").isValid());
    }

    @Test
    void testValidPassword() {
        assertTrue(PasswordPolicyService.validate("mySecretPass123!", "user@test.com").isValid());
    }
}
