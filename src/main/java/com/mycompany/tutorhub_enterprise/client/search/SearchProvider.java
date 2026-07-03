package com.mycompany.tutorhub_enterprise.client.search;

import java.util.List;

public interface SearchProvider {
    String id();
    boolean supports(SearchQuery query);
    List<SearchResult> search(SearchQuery query);
}
