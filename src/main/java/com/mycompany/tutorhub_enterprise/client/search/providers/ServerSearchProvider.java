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

        List<SearchResult> results = new ArrayList<>();
        for (GlobalSearchDto dto : rawResults) {
            SearchResultType type;
            try {
                type = SearchResultType.valueOf(dto.type);
            } catch (Exception e) {
                type = SearchResultType.PROFILE;
            }

            results.add(SearchResult.builder()
                .title(dto.title)
                .subtitle(dto.subtitle)
                .type(type)
                .score(80) 
                .action(SearchAction.noop()) 
                .build());
        }
        
        currentFuture.complete(results);
    }
}
