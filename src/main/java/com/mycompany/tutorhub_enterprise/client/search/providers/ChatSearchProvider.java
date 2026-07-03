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
 * Provides search results by scanning cached user/conversation data.
 * Uses a Supplier to lazily access the data from ChatTab without tight coupling.
 */
public class ChatSearchProvider implements SearchProvider {

    /**
     * Represents a searchable contact/conversation for the provider.
     */
    public static class ChatEntry {
        public final String displayName;
        public final String lastMessage;
        public final Runnable onSelect;

        public ChatEntry(String displayName, String lastMessage, Runnable onSelect) {
            this.displayName = displayName;
            this.lastMessage = lastMessage;
            this.onSelect = onSelect;
        }
    }

    private final Supplier<List<ChatEntry>> dataSupplier;

    /**
     * @param dataSupplier Provides the current list of chat entries to search through.
     */
    public ChatSearchProvider(Supplier<List<ChatEntry>> dataSupplier) {
        this.dataSupplier = dataSupplier;
    }

    @Override
    public String id() {
        return "chat";
    }

    @Override
    public boolean supports(SearchQuery query) {
        return query != null && !query.isBlank();
    }

    @Override
    public java.util.concurrent.CompletableFuture<List<SearchResult>> searchAsync(SearchQuery query) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            if (query == null || query.isBlank()) return Collections.emptyList();

            List<ChatEntry> entries = dataSupplier.get();
            if (entries == null || entries.isEmpty()) return Collections.emptyList();

            List<SearchResult> results = new ArrayList<>();
            String normalized = query.getNormalizedText();

            for (ChatEntry entry : entries) {
                // Haystack cho phép tìm kiếm mờ trên cả tên hiển thị và tin nhắn cuối (nếu có)
                String haystack = SearchQuery.of(
                        entry.displayName + " " + (entry.lastMessage != null ? entry.lastMessage : "")
                ).getNormalizedText();

                double score = 0;
                String nameNorm = SearchQuery.of(entry.displayName).getNormalizedText();

                if (nameNorm.equals(normalized)) {
                    score = 90; // Trùng khớp hoàn toàn tên
                } else if (nameNorm.startsWith(normalized)) {
                    score = 75; // Tiền tố tên
                } else if (haystack.contains(normalized)) {
                    score = 50; // Chứa ở đâu đó trong tên hoặc nội dung
                }

                if (score > 0) {
                    String subtitle = entry.lastMessage != null && !entry.lastMessage.isBlank()
                            ? entry.lastMessage : "Tin nhắn";

                    results.add(SearchResult.builder()
                            .title(entry.displayName)
                            .subtitle(subtitle)
                            .type(SearchResultType.CHAT)
                            .score(score)
                            .iconText("MSG")
                            .action(entry.onSelect != null ? entry.onSelect::run : SearchAction.noop())
                            .build());
                }
            }

            return results;
        });
    }
}
