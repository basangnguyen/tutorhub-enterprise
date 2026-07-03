package com.mycompany.tutorhub_enterprise.client.search.providers;

import com.mycompany.tutorhub_enterprise.client.search.SearchAction;
import com.mycompany.tutorhub_enterprise.client.search.SearchProvider;
import com.mycompany.tutorhub_enterprise.client.search.SearchQuery;
import com.mycompany.tutorhub_enterprise.client.search.SearchResult;
import com.mycompany.tutorhub_enterprise.client.search.SearchResultType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Provides search results by scanning cached blackboard/whiteboard data.
 */
public class BlackboardSearchProvider implements SearchProvider {

    public static class BoardEntry {
        public final String boardTitle;
        public final String ownerName;
        public final Runnable onSelect;

        public BoardEntry(String boardTitle, String ownerName, Runnable onSelect) {
            this.boardTitle = boardTitle;
            this.ownerName = ownerName;
            this.onSelect = onSelect;
        }
    }

    private final Supplier<List<BoardEntry>> dataSupplier;

    public BlackboardSearchProvider(Supplier<List<BoardEntry>> dataSupplier) {
        this.dataSupplier = dataSupplier;
    }

    @Override
    public String id() {
        return "blackboard";
    }

    @Override
    public boolean supports(SearchQuery query) {
        return query != null && !query.isBlank();
    }

    @Override
    public java.util.concurrent.CompletableFuture<List<SearchResult>> searchAsync(SearchQuery query) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            if (query == null || query.isBlank()) return Collections.emptyList();

            List<BoardEntry> entries = dataSupplier.get();
            if (entries == null || entries.isEmpty()) return Collections.emptyList();

            List<SearchResult> results = new ArrayList<>();
            String normalized = query.getNormalizedText();

            for (BoardEntry entry : entries) {
                String haystack = SearchQuery.of(
                        entry.boardTitle + " " + (entry.ownerName != null ? entry.ownerName : "")
                ).getNormalizedText();

                double score = 0;
                String titleNorm = SearchQuery.of(entry.boardTitle).getNormalizedText();

                if (titleNorm.equals(normalized)) {
                    score = 90;
                } else if (titleNorm.startsWith(normalized)) {
                    score = 70;
                } else if (haystack.contains(normalized)) {
                    score = 50;
                }

                if (score > 0) {
                    String subtitle = entry.ownerName != null && !entry.ownerName.isBlank()
                            ? entry.ownerName : "Bảng vẽ";

                    results.add(SearchResult.builder()
                            .title(entry.boardTitle)
                            .subtitle(subtitle)
                            .type(SearchResultType.BLACKBOARD)
                            .score(score)
                            .iconText("BOARD")
                            .action(entry.onSelect != null ? entry.onSelect::run : SearchAction.noop())
                            .build());
                }
            }

            return results;
        });
    }
}
