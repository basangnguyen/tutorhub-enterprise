package com.mycompany.tutorhub_enterprise.client.search;

import java.text.Normalizer;
import java.util.Locale;

public final class SearchQuery {
    private final String rawText;
    private final String normalizedText;

    private SearchQuery(String rawText) {
        this.rawText = rawText == null ? "" : rawText.trim();
        this.normalizedText = normalize(this.rawText);
    }

    public static SearchQuery of(String rawText) {
        return new SearchQuery(rawText);
    }

    public String getRawText() {
        return rawText;
    }

    public String getNormalizedText() {
        return normalizedText;
    }

    public boolean isBlank() {
        return rawText.isBlank();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String decomposed = Normalizer.normalize(value.trim(), Normalizer.Form.NFD);
        return decomposed
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }
}
