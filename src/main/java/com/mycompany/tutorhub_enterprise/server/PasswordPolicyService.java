package com.mycompany.tutorhub_enterprise.server;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class PasswordPolicyService {

    private static final Set<String> COMMON_PASSWORDS = new HashSet<>(Arrays.asList(
        "password", "123456", "1234567", "12345678", "123456789", "1234567890",
        "qwerty", "admin", "letmein", "iloveyou", "welcome", "abc123", "111111", "000000"
    ));

    private PasswordPolicyService() {}

    public static class PasswordPolicyResult {
        private final boolean valid;
        private final String publicMessage;

        public PasswordPolicyResult(boolean valid, String publicMessage) {
            this.valid = valid;
            this.publicMessage = publicMessage;
        }

        public boolean isValid() { return valid; }
        public String getPublicMessage() { return publicMessage; }
    }

    public static PasswordPolicyResult validate(String password, String emailOrUsername) {
        if (password == null || password.trim().isEmpty()) {
            return new PasswordPolicyResult(false, "Mat khau khong duoc de trong.");
        }

        String trimmedPassword = password.trim();

        if (trimmedPassword.length() < 8) {
            return new PasswordPolicyResult(false, "Mat khau phai co it nhat 8 ky tu.");
        }

        if (trimmedPassword.length() > 128) {
            return new PasswordPolicyResult(false, "Mat khau qua dai (toi da 128 ky tu).");
        }

        String lowerPassword = trimmedPassword.toLowerCase();
        
        if (emailOrUsername != null) {
            String lowerEmail = emailOrUsername.trim().toLowerCase();
            // If the password matches the email (e.g. they type "ba***@gmail.com") or username (e.g. "basang")
            if (!lowerEmail.isEmpty() && (lowerPassword.equals(lowerEmail) || lowerEmail.startsWith(lowerPassword + "@"))) {
                // If they use just the username part of the email, we should probably also block it.
                // The prompt says: "Không được trùng email/username sau khi normalize lowercase". We will just do equals.
                return new PasswordPolicyResult(false, "Mat khau khong duoc trung voi email.");
            }
        }

        if (COMMON_PASSWORDS.contains(lowerPassword)) {
            return new PasswordPolicyResult(false, "Mat khau qua pho bien. Vui long chon mat khau kho doan hon.");
        }

        return new PasswordPolicyResult(true, "Hop le");
    }
}
