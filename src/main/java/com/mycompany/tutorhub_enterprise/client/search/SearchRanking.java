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
            return 0.0;
        }

        String normText = SearchQuery.of(text).getNormalizedText();
        String normQuery = SearchQuery.of(query).getNormalizedText();

        if (normText.equals(normQuery)) {
            return 100.0;
        } else if (normText.startsWith(normQuery)) {
            // Thưởng cho prefix match, ưu tiên từ khóa ngắn khớp với chuỗi ngắn
            double ratio = (double) normQuery.length() / normText.length();
            return 80.0 + (10.0 * ratio);
        } else if (normText.contains(normQuery)) {
            double ratio = (double) normQuery.length() / normText.length();
            return 50.0 + (10.0 * ratio);
        }

        // Fuzzy match (Levenshtein Distance)
        int distance = levenshtein(normText, normQuery);
        // Nếu khác biệt <= 2 ký tự và chiều dài query đủ lớn
        if (normQuery.length() >= 3 && distance <= 2) {
            return 30.0 - (distance * 5.0);
        }
        
        return 0.0;
    }

    private static int levenshtein(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }
}
