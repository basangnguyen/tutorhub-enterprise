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
 * Provides search results by scanning cached classroom data.
 */
public class ClassSearchProvider implements SearchProvider {

    public static class ClassEntry {
        public final String className;
        public final String subject;
        public final String teacherName;
        public final Runnable onSelect;

        public ClassEntry(String className, String subject, String teacherName, Runnable onSelect) {
            this.className = className;
            this.subject = subject;
            this.teacherName = teacherName;
            this.onSelect = onSelect;
        }
    }

    private final Supplier<List<ClassEntry>> dataSupplier;

    public ClassSearchProvider(Supplier<List<ClassEntry>> dataSupplier) {
        this.dataSupplier = dataSupplier;
    }

    @Override
    public String id() {
        return "class";
    }

    @Override
    public boolean supports(SearchQuery query) {
        return query != null && !query.isBlank();
    }

    @Override
    public java.util.concurrent.CompletableFuture<List<SearchResult>> searchAsync(SearchQuery query) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            if (query == null || query.isBlank()) return Collections.emptyList();

            List<ClassEntry> entries = dataSupplier.get();
            if (entries == null || entries.isEmpty()) return Collections.emptyList();

            List<SearchResult> results = new ArrayList<>();
            String normalized = query.getNormalizedText();

            for (ClassEntry entry : entries) {
                String haystack = SearchQuery.of(
                        entry.className + " " +
                        (entry.subject != null ? entry.subject : "") + " " +
                        (entry.teacherName != null ? entry.teacherName : "")
                ).getNormalizedText();

                double score = 0;
                String classNameNorm = SearchQuery.of(entry.className).getNormalizedText();

                if (classNameNorm.equals(normalized)) {
                    score = 90;
                } else if (classNameNorm.startsWith(normalized)) {
                    score = 70;
                } else if (haystack.contains(normalized)) {
                    score = 50;
                }

                if (score > 0) {
                    String subtitle = entry.subject;
                    if (entry.teacherName != null && !entry.teacherName.isBlank()) {
                        subtitle += " • " + entry.teacherName;
                    }

                    results.add(SearchResult.builder()
                            .title(entry.className)
                            .subtitle(subtitle)
                            .type(SearchResultType.CLASS)
                            .score(score)
                            .iconText("CLS")
                            .action(entry.onSelect != null ? entry.onSelect::run : SearchAction.noop())
                            .build());
                }
            }

            return results;
        });
    }
}
