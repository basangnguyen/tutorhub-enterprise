package com.mycompany.tutorhub_enterprise.client.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

public class SearchHistoryStore {

    private static final String PREF_KEY = "tutorhub.search.history";
    private static final int MAX_HISTORY = 5;

    public static List<String> getRecentSearches() {
        Preferences prefs = Preferences.userNodeForPackage(SearchHistoryStore.class);
        String historyStr = prefs.get(PREF_KEY, "");
        if (historyStr.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(historyStr.split("\\|\\|\\|")));
    }

    public static void addSearch(String query) {
        if (query == null || query.isBlank()) {
            return;
        }
        
        String trimmedQuery = query.trim();
        List<String> history = getRecentSearches();
        
        // Remove if it exists to bring it to the top
        history.remove(trimmedQuery);
        history.add(0, trimmedQuery);
        
        if (history.size() > MAX_HISTORY) {
            history = history.subList(0, MAX_HISTORY);
        }
        
        saveHistory(history);
    }

    private static void saveHistory(List<String> history) {
        Preferences prefs = Preferences.userNodeForPackage(SearchHistoryStore.class);
        String historyStr = String.join("|||", history);
        prefs.put(PREF_KEY, historyStr);
    }
    
    public static void clearHistory() {
        Preferences prefs = Preferences.userNodeForPackage(SearchHistoryStore.class);
        prefs.remove(PREF_KEY);
    }
}
