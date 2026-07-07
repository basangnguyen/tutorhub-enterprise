package com.mycompany.tutorhub_enterprise.client.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the public Lavie Hugging Face Space context used by the older Lavie server.
 * The client keeps a short cache so Gemini/OpenAI-compatible providers receive
 * the same TutorHub persona and internal knowledge without requiring a token.
 */
public final class LavieRemoteContextProvider {

    private static final String DEFAULT_RAW_BASE =
            "https://huggingface.co/spaces/hocbatrolai293/tutorhub-ai/resolve/main";
    private static final String RAW_BASE = trimTrailingSlash(System.getProperty(
            "tutorhub.lavie.hf.rawBase", DEFAULT_RAW_BASE));
    private static final String DEFAULT_USER_ID = "java_user";
    private static final long CACHE_TTL_MS = 5L * 60L * 1000L;
    private static final int CONNECT_TIMEOUT_MS = 6_000;
    private static final int READ_TIMEOUT_MS = 12_000;
    private static final int MAX_USER_MEMORY_CHARS = 8_000;
    private static final int MAX_KNOWLEDGE_CHARS = 14_000;
    private static final int MAX_CONTEXT_CHARS = 22_000;
    private static final Pattern PYTHON_STRING_PATTERN = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Gson PRETTY_GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
    private static final LavieRemoteContextProvider INSTANCE = new LavieRemoteContextProvider();

    private volatile long cachedAtMs;
    private volatile String cachedUserId = "";
    private volatile String cachedContext = "";
    private volatile String cachedError = "";

    private LavieRemoteContextProvider() {
    }

    public static LavieRemoteContextProvider shared() {
        return INSTANCE;
    }

    public String contextFor(String userId) {
        String effectiveUserId = normalizeUserId(userId);
        long now = System.currentTimeMillis();
        String current = cachedContext;
        if (!current.isBlank()
                && effectiveUserId.equals(cachedUserId)
                && now - cachedAtMs < CACHE_TTL_MS) {
            return current;
        }
        synchronized (this) {
            current = cachedContext;
            now = System.currentTimeMillis();
            if (!current.isBlank()
                    && effectiveUserId.equals(cachedUserId)
                    && now - cachedAtMs < CACHE_TTL_MS) {
                return current;
            }
            try {
                cachedContext = loadContext(effectiveUserId);
                cachedUserId = effectiveUserId;
                cachedAtMs = System.currentTimeMillis();
                cachedError = "";
                return cachedContext;
            } catch (Exception ex) {
                cachedError = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                return current == null ? "" : current;
            }
        }
    }

    public String lastError() {
        return cachedError == null ? "" : cachedError;
    }

    private String loadContext(String userId) throws IOException {
        String userMemory = loadUserMemory(userId);
        String knowledgeBase = loadTutorHubKnowledgeBase();
        StringBuilder context = new StringBuilder();
        context.append("REMOTE LAVIE SERVER CONTEXT FROM HUGGING FACE\n");
        context.append("Priority: high. If this context conflicts with generic local instructions, use this context.\n\n");
        if (!userMemory.isBlank()) {
            context.append("[user_memory.json / ").append(userId).append("]\n")
                    .append(limit(userMemory, MAX_USER_MEMORY_CHARS))
                    .append("\n\n");
        }
        if (!knowledgeBase.isBlank()) {
            context.append("[app.py / tutorhub_knowledge_base]\n")
                    .append(limit(knowledgeBase, MAX_KNOWLEDGE_CHARS))
                    .append("\n\n");
        }
        if (userMemory.isBlank() && knowledgeBase.isBlank()) {
            return "";
        }
        context.append("Usage rules:\n")
                .append("- Use the full user memory, not only the first personal-info line.\n")
                .append("- If communication_style, call_user, assistant_self_reference or lavie_behavior_rules exist, follow them.\n")
                .append("- Use the TutorHub knowledge base when answering about TutorHub, Lavie, the founder, internal features, or app behavior.\n")
                .append("- Do not expose raw JSON unless the user explicitly asks for it.");
        return limit(context.toString(), MAX_CONTEXT_CHARS);
    }

    private String loadUserMemory(String userId) throws IOException {
        String json = fetchRaw("user_memory.json");
        JsonElement rootElement = JsonParser.parseString(json);
        if (!rootElement.isJsonObject()) {
            return "";
        }
        JsonObject root = rootElement.getAsJsonObject();
        JsonElement memory = root.get(userId);
        if (memory == null || memory.isJsonNull()) {
            memory = root.get(DEFAULT_USER_ID);
        }
        if (memory == null || memory.isJsonNull()) {
            return "";
        }
        return PRETTY_GSON.toJson(memory);
    }

    private String loadTutorHubKnowledgeBase() throws IOException {
        String appPy = fetchRaw("app.py");
        String block = extractPythonListBlock(appPy, "tutorhub_knowledge_base");
        if (block.isBlank()) {
            return "";
        }
        Matcher matcher = PYTHON_STRING_PATTERN.matcher(block);
        List<String> facts = new ArrayList<>();
        while (matcher.find()) {
            String fact = unescapePythonString(matcher.group(1)).trim();
            if (!fact.isBlank()) {
                facts.add(fact);
            }
        }
        StringBuilder joined = new StringBuilder();
        for (String fact : facts) {
            if (joined.length() > 0) {
                joined.append('\n');
            }
            joined.append("- ").append(fact);
            if (joined.length() >= MAX_KNOWLEDGE_CHARS) {
                break;
            }
        }
        return joined.toString();
    }

    private String extractPythonListBlock(String source, String variableName) {
        if (source == null || source.isBlank() || variableName == null || variableName.isBlank()) {
            return "";
        }
        int variableIndex = source.indexOf(variableName);
        if (variableIndex < 0) {
            return "";
        }
        int start = source.indexOf('[', variableIndex);
        if (start < 0) {
            return "";
        }
        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        for (int i = start; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }
            if (ch == '"') {
                inString = true;
            } else if (ch == '[') {
                depth++;
            } else if (ch == ']') {
                depth--;
                if (depth == 0) {
                    return source.substring(start, i + 1);
                }
            }
        }
        return "";
    }

    private String fetchRaw(String path) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(RAW_BASE + "/" + path).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("Accept", "text/plain, application/json, */*");
            String hfToken = System.getProperty("tutorhub.lavie.token", "");
            if (!hfToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + hfToken);
            }
            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("HTTP " + status + " while reading " + path);
            }
            try (InputStream inputStream = conn.getInputStream()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String unescapePythonString(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (!escaped) {
                if (ch == '\\') {
                    escaped = true;
                } else {
                    sb.append(ch);
                }
                continue;
            }
            switch (ch) {
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case '"' -> sb.append('"');
                case '\\' -> sb.append('\\');
                default -> sb.append(ch);
            }
            escaped = false;
        }
        if (escaped) {
            sb.append('\\');
        }
        return sb.toString();
    }

    private static String normalizeUserId(String userId) {
        String clean = userId == null ? "" : userId.trim();
        return clean.isEmpty() ? DEFAULT_USER_ID : clean;
    }

    private static String trimTrailingSlash(String value) {
        String clean = value == null || value.trim().isEmpty() ? DEFAULT_RAW_BASE : value.trim();
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }

    private static String limit(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(0, maxChars - 80))
                + "\n...[context truncated to keep model prompt stable]...";
    }
}
