package com.mycompany.tutorhub_enterprise.client.search;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to highlight matched text in search results.
 * Given a display text and a search query, returns HTML with matched portions wrapped in bold tags.
 * Matching is diacritics-insensitive for Vietnamese support.
 */
public final class SearchTextHighlighter {

    private SearchTextHighlighter() {}

    /**
     * Returns HTML string with matched portions in bold.
     * Example: highlight("Mở Lịch học", "lich") → "<html>Mở <b>Lịch</b> học</html>"
     */
    public static String highlight(String text, String query) {
        if (text == null || text.isEmpty()) return "";
        if (query == null || query.isBlank()) return escapeHtml(text);

        String normalizedText = normalize(text);
        String normalizedQuery = normalize(query.trim());

        if (normalizedQuery.isEmpty()) return escapeHtml(text);

        // Find all match positions in the normalized version
        StringBuilder html = new StringBuilder();
        int textLen = text.length();
        int searchFrom = 0;

        while (searchFrom < textLen) {
            int matchStart = normalizedText.indexOf(normalizedQuery, searchFrom);
            if (matchStart < 0) {
                // No more matches — append remainder
                html.append(escapeHtml(text.substring(searchFrom)));
                break;
            }

            int matchEnd = matchStart + normalizedQuery.length();

            // Clamp to string length (safety for multi-byte edge cases)
            if (matchEnd > textLen) matchEnd = textLen;

            // Append text before match
            if (matchStart > searchFrom) {
                html.append(escapeHtml(text.substring(searchFrom, matchStart)));
            }

            // Append matched portion in bold
            html.append("<b>").append(escapeHtml(text.substring(matchStart, matchEnd))).append("</b>");

            searchFrom = matchEnd;
        }

        return html.toString();
    }

    /**
     * Wraps the result from highlight() in HTML tags for use in JLabel.
     */
    public static String highlightForLabel(String text, String query) {
        return "<html>" + highlight(text, query) + "</html>";
    }

    /**
     * Normalizes a string: lowercase, remove diacritics, replace đ→d.
     * This must produce a result of the SAME LENGTH as the input for index mapping.
     */
    private static String normalize(String value) {
        if (value == null || value.isEmpty()) return "";
        // We need char-by-char normalization that preserves length
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == 'đ') { sb.append('d'); continue; }
            if (c == 'Đ') { sb.append('d'); continue; }
            // Decompose, remove combining marks, lowercase
            String decomposed = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD);
            // Take only the base character
            char base = decomposed.charAt(0);
            sb.append(Character.toLowerCase(base));
        }
        return sb.toString();
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }
}
