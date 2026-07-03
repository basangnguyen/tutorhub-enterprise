package com.mycompany.tutorhub_enterprise.client.search;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SearchProvider {
    String id();
    boolean supports(SearchQuery query);
    CompletableFuture<List<SearchResult>> searchAsync(SearchQuery query);
}
