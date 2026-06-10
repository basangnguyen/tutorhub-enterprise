package com.mycompany.tutorhub_enterprise.client;

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

public final class InputFilters {

    private InputFilters() {
    }

    public static void installOtpFilter(JTextComponent field) {
        install(field, new RegexLimitFilter("\\d*", 6));
    }

    public static void installPhoneFilter(JTextComponent field) {
        install(field, new RegexLimitFilter("[0-9+\\-\\s()]*", 18));
    }

    public static boolean isValidOtp(String value) {
        return value != null && value.trim().matches("\\d{6}");
    }

    public static boolean isValidPhone(String value) {
        if (value == null) {
            return false;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.length() >= 9 && digits.length() <= 15;
    }

    private static void install(JTextComponent field, DocumentFilter filter) {
        if (field.getDocument() instanceof AbstractDocument document) {
            document.setDocumentFilter(filter);
        }
    }

    private static final class RegexLimitFilter extends DocumentFilter {
        private final String regex;
        private final int maxLength;

        private RegexLimitFilter(String regex, int maxLength) {
            this.regex = regex;
            this.maxLength = maxLength;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            replace(fb, offset, 0, string, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            String value = text == null ? "" : text;
            String current = fb.getDocument().getText(0, fb.getDocument().getLength());
            String next = current.substring(0, offset) + value + current.substring(offset + length);
            if (next.length() <= maxLength && next.matches(regex)) {
                super.replace(fb, offset, length, value, attrs);
            }
        }
    }
}
