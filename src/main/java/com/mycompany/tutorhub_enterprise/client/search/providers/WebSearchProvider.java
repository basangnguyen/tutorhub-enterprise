package com.mycompany.tutorhub_enterprise.client.search.providers;

import com.mycompany.tutorhub_enterprise.client.search.SearchAction;
import com.mycompany.tutorhub_enterprise.client.search.SearchProvider;
import com.mycompany.tutorhub_enterprise.client.search.SearchQuery;
import com.mycompany.tutorhub_enterprise.client.search.SearchResult;
import com.mycompany.tutorhub_enterprise.client.search.SearchResultType;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WebSearchProvider implements SearchProvider {

    @Override
    public String id() {
        return "web_fallback";
    }

    @Override
    public boolean supports(SearchQuery query) {
        return query != null && !query.isBlank();
    }

    @Override
    public List<SearchResult> search(SearchQuery query) {
        List<SearchResult> results = new ArrayList<>();
        if (query == null || query.isBlank()) {
            return results;
        }

        String rawText = query.getRawText();
        results.add(SearchResult.builder()
                .title("Tìm trên Google: " + rawText)
                .subtitle("Mở trình duyệt web để tìm kiếm")
                .type(SearchResultType.WEB)
                .score(-20.0) // Điểm âm để luôn xếp dưới cùng
                .iconText("WEB")
                .action(() -> openGoogleSearch(rawText))
                .build());

        return results;
    }

    private void openGoogleSearch(String queryText) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                String encodedQuery = URLEncoder.encode(queryText, StandardCharsets.UTF_8);
                Desktop.getDesktop().browse(new URI("https://www.google.com/search?q=" + encodedQuery));
            } else {
                System.err.println("Desktop browsing is not supported.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
