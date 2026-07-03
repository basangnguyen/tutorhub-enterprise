package com.mycompany.tutorhub_enterprise.client.search;

public class SearchRanking {

    /**
     * Chấm điểm kết quả tìm kiếm (ranking)
     * Exact Match: 100đ
     * Prefix Match: 80đ
     * Contains Match: 50đ
     * Không khớp: 0đ
     */
    public static double rank(String text, String query) {
        if (text == null || query == null || text.isBlank() || query.isBlank()) {
            return 0;
        }

        String normText = SearchQuery.of(text).getNormalizedText();
        String normQuery = SearchQuery.of(query).getNormalizedText();

        if (normText.equals(normQuery)) {
            return 100.0;
        } else if (normText.startsWith(normQuery)) {
            return 80.0;
        } else if (normText.contains(normQuery)) {
            return 50.0;
        }
        
        return 0.0;
    }
}
