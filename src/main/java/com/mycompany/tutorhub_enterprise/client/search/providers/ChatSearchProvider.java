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
    public List<SearchResult> search(SearchQuery query) {
        if (query == null || query.isBlank()) return Collections.emptyList();

        List<ChatEntry> entries = dataSupplier.get();
        if (entries == null || entries.isEmpty()) return Collections.emptyList();

        List<SearchResult> results = new ArrayList<>();
        String normalized = query.getNormalizedText();

        for (ChatEntry entry : entries) {
            String nameNorm = SearchQuery.of(entry.displayName).getNormalizedText();
            String msgNorm = entry.lastMessage != null
                    ? SearchQuery.of(entry.lastMessage).getNormalizedText() : "";

            double score = 0;
            if (nameNorm.equals(normalized)) {
                score = 95;  // exact match
            } else if (nameNorm.startsWith(normalized)) {
                score = 75;  // prefix match
            } else if (nameNorm.contains(normalized)) {
                score = 55;  // contains match
            } else if (msgNorm.contains(normalized)) {
                score = 35;  // message contains
            }

            if (score > 0) {
                String subtitle = entry.lastMessage != null && !entry.lastMessage.isBlank()
                        ? entry.lastMessage : "Mở hội thoại";
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
    }
}
