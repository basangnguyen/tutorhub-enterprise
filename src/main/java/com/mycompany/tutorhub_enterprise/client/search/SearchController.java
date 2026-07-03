package com.mycompany.tutorhub_enterprise.client.search;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SearchController {
    private final List<SearchProvider> providers = new CopyOnWriteArrayList<>();

    public void registerProvider(SearchProvider provider) {
        providers.add(provider);
    }

    public List<SearchResult> executeSearch(SearchQuery query) {
        List<SearchResult> allResults = new ArrayList<>();
        
        for (SearchProvider provider : providers) {
            if (provider.supports(query)) {
                List<SearchResult> results = provider.search(query);
                if (results != null) {
                    allResults.addAll(results);
                }
            }
        }
        
        // Sắp xếp kết quả theo điểm (score) giảm dần
        allResults.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        return allResults;
    }
}
