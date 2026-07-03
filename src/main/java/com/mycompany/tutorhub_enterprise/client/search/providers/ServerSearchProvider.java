package com.mycompany.tutorhub_enterprise.client.search.providers;

import com.mycompany.tutorhub_enterprise.client.search.SearchProvider;
import com.mycompany.tutorhub_enterprise.client.search.SearchQuery;
import com.mycompany.tutorhub_enterprise.client.search.SearchResult;
import com.mycompany.tutorhub_enterprise.client.search.SearchResultType;
import com.mycompany.tutorhub_enterprise.client.search.SearchAction;
import com.mycompany.tutorhub_enterprise.models.GlobalSearchDto;
import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.client.NetworkManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ServerSearchProvider implements SearchProvider {

    private CompletableFuture<List<SearchResult>> currentFuture;
    private SearchQuery lastQuery;

    private static class CachedResult {
        List<SearchResult> results;
        long timestamp;
        CachedResult(List<SearchResult> results) {
            this.results = results;
            this.timestamp = System.currentTimeMillis();
        }
    }
    private final java.util.Map<String, CachedResult> cache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CACHE_TTL = 5 * 60 * 1000;

    @Override
    public String id() {
        return "server";
    }

    @Override
    public boolean supports(SearchQuery query) {
        // G3: Min query length >= 2
        return query != null && query.getNormalizedText().length() >= 2;
    }

    @Override
    public CompletableFuture<List<SearchResult>> searchAsync(SearchQuery query) {
        if (currentFuture != null && !currentFuture.isDone()) {
            currentFuture.cancel(true);
        }
        
        currentFuture = new CompletableFuture<>();
        this.lastQuery = query;
        String normalizedQuery = query.getNormalizedText();

        CachedResult cached = cache.get(normalizedQuery);
        if (cached != null && (System.currentTimeMillis() - cached.timestamp < CACHE_TTL)) {
            // Trả về cache ngay lập tức nếu còn hạn
            currentFuture.complete(cached.results);
            return currentFuture;
        }
        
        try {
            NetworkManager.getInstance().sendPacket(new Packet("GLOBAL_SEARCH", query.getNormalizedText()));
        } catch (Exception e) {
            currentFuture.complete(Collections.emptyList());
        }
        
        return currentFuture;
    }

    public void onServerResponse(List<GlobalSearchDto> rawResults) {
        if (currentFuture == null || currentFuture.isDone() || currentFuture.isCancelled()) {
            return;
        }

        if (rawResults == null || rawResults.isEmpty()) {
            currentFuture.complete(Collections.emptyList());
            return;
        }

        String q = (lastQuery != null) ? lastQuery.getRawText() : "";
        List<SearchResult> results = new ArrayList<>();
        for (GlobalSearchDto dto : rawResults) {
            SearchResultType type;
            try {
                type = SearchResultType.valueOf(dto.type);
            } catch (Exception e) {
                type = SearchResultType.PROFILE;
            }

            double score = com.mycompany.tutorhub_enterprise.client.search.SearchRanking.rank(dto.title, q);
            // Có thể cộng thêm điểm ưu tiên tùy thuộc vào loại dữ liệu
            if (type == SearchResultType.CLASS) score += 5.0; 

            results.add(SearchResult.builder()
                .title(dto.title)
                .subtitle(dto.subtitle)
                .type(type)
                .score(score) 
                .action(SearchAction.noop()) 
                .build());
        }
        
        // Lưu vào cache
        if (!q.isEmpty()) {
            cache.put(lastQuery.getNormalizedText(), new CachedResult(new ArrayList<>(results)));
        }

        currentFuture.complete(results);
    }
}
