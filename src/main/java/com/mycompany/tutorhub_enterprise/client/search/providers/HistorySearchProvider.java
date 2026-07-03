package com.mycompany.tutorhub_enterprise.client.search.providers;

import com.mycompany.tutorhub_enterprise.client.search.SearchAction;
import com.mycompany.tutorhub_enterprise.client.search.SearchHistoryStore;
import com.mycompany.tutorhub_enterprise.client.search.SearchProvider;
import com.mycompany.tutorhub_enterprise.client.search.SearchQuery;
import com.mycompany.tutorhub_enterprise.client.search.SearchResult;
import com.mycompany.tutorhub_enterprise.client.search.SearchResultType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class HistorySearchProvider implements SearchProvider {

    private final Consumer<String> onSelectHistory;

    public HistorySearchProvider(Consumer<String> onSelectHistory) {
        this.onSelectHistory = onSelectHistory;
    }

    @Override
    public String id() {
        return "history";
    }

    @Override
    public boolean supports(SearchQuery query) {
        return true;
    }

    @Override
    public java.util.concurrent.CompletableFuture<List<SearchResult>> searchAsync(SearchQuery query) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            List<SearchResult> results = new ArrayList<>();
            List<String> history = SearchHistoryStore.getRecentSearches();

            if (history.isEmpty()) {
                return results;
            }

            for (String histQuery : history) {
                // Nếu query rỗng, hiển thị toàn bộ lịch sử. 
                // Nếu có query, chỉ hiển thị lịch sử khớp với query (bỏ qua case/diacritics).
                if (query == null || query.isBlank() || 
                    SearchQuery.of(histQuery).getNormalizedText().contains(query.getNormalizedText())) {
                    
                    results.add(SearchResult.builder()
                            .title(histQuery)
                            .subtitle("Tìm kiếm gần đây")
                            .type(SearchResultType.HISTORY)
                            .score(15.0) // Điểm cao hơn web, thấp hơn command
                            .iconText("HIST")
                            .action(() -> {
                                if (onSelectHistory != null) {
                                    onSelectHistory.accept(histQuery);
                                }
                            })
                            .build());
                }
            }

            return results;
        });
    }
}
