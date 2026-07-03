package com.mycompany.tutorhub_enterprise.client.search;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SearchController {
    private final List<SearchProvider> providers = new CopyOnWriteArrayList<>();

    public void registerProvider(SearchProvider provider) {
        providers.add(provider);
    }

    public java.util.concurrent.CompletableFuture<List<SearchResult>> executeSearchAsync(SearchQuery query) {
        List<java.util.concurrent.CompletableFuture<List<SearchResult>>> futures = new ArrayList<>();
        
        for (SearchProvider provider : providers) {
            if (provider.supports(query)) {
                // Thêm timeout 3 giây cho mỗi provider để tránh treo dropdown
                java.util.concurrent.CompletableFuture<List<SearchResult>> future = provider.searchAsync(query)
                        .completeOnTimeout(new ArrayList<>(), 3, java.util.concurrent.TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            ex.printStackTrace();
                            return new ArrayList<>();
                        });
                futures.add(future);
            }
        }
        
        java.util.concurrent.CompletableFuture<Void> allFutures = java.util.concurrent.CompletableFuture.allOf(
                futures.toArray(new java.util.concurrent.CompletableFuture[0])
        );
        
        return allFutures.thenApply(v -> {
            List<SearchResult> allResults = new ArrayList<>();
            for (java.util.concurrent.CompletableFuture<List<SearchResult>> future : futures) {
                List<SearchResult> results = future.join();
                if (results != null) {
                    allResults.addAll(results);
                }
            }
            // Sắp xếp kết quả theo điểm (score) giảm dần
            allResults.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
            return allResults;
        });
    }
}
