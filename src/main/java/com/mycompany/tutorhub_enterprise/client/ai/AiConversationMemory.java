package com.mycompany.tutorhub_enterprise.client.ai;

import java.util.ArrayList;
import java.util.List;

public final class AiConversationMemory {

    private static final int MAX_MESSAGES = 16;
    private static final int MAX_CONTEXT_CHARS = 4200;
    private static final int MAX_MESSAGE_CHARS = 900;
    private static final int MAX_COMPACTED_CHARS = 1800;

    private final List<Entry> entries = new ArrayList<>();
    private String compactedSummary = "";

    public synchronized void rememberUser(String content) {
        remember("Người dùng", content);
    }

    public synchronized void rememberAssistant(String content) {
        remember("Lavie", content);
    }

    public synchronized String buildContext() {
        if (entries.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (!compactedSummary.isBlank()) {
            sb.append("Compacted earlier conversation:\n")
                    .append(compactedSummary)
                    .append("\n\nRecent conversation:\n");
        }
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry entry = entries.get(i);
            String line = entry.role + ": " + entry.content + "\n";
            if (sb.length() + line.length() > MAX_CONTEXT_CHARS) {
                break;
            }
            sb.insert(0, line);
        }
        return sb.toString().trim();
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized void clear() {
        entries.clear();
        compactedSummary = "";
    }

    public synchronized boolean hasCompactedSummary() {
        return !compactedSummary.isBlank();
    }

    private void remember(String role, String content) {
        String normalized = normalize(content);
        if (normalized.isEmpty()) {
            return;
        }
        entries.add(new Entry(role, normalized));
        trim();
    }

    private void trim() {
        while (entries.size() > MAX_MESSAGES) {
            compact(entries.remove(0));
        }
    }

    private void compact(Entry entry) {
        if (entry == null || entry.content == null || entry.content.isBlank()) {
            return;
        }
        String line = entry.role + ": " + entry.content;
        compactedSummary = compactedSummary.isBlank() ? line : compactedSummary + "\n" + line;
        if (compactedSummary.length() > MAX_COMPACTED_CHARS) {
            int start = Math.max(0, compactedSummary.length() - MAX_COMPACTED_CHARS);
            compactedSummary = "... earlier compacted context omitted ...\n"
                    + compactedSummary.substring(start).trim();
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= MAX_MESSAGE_CHARS) {
            return normalized;
        }
        return normalized.substring(0, MAX_MESSAGE_CHARS).trim() + "...";
    }

    private static final class Entry {
        private final String role;
        private final String content;

        private Entry(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
