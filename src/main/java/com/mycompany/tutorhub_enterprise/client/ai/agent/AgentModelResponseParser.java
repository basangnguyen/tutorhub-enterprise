package com.mycompany.tutorhub_enterprise.client.ai.agent;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AgentModelResponseParser {

    private static final Gson GSON = new Gson();

    private AgentModelResponseParser() {
    }

    public static AgentModelResponse parse(String rawText) {
        String raw = rawText == null ? "" : rawText.trim();
        if (raw.isEmpty()) {
            return AgentModelResponse.finalAnswer("", "");
        }

        String candidate = extractJsonObject(raw);
        if (candidate == null) {
            return AgentModelResponse.finalAnswer(raw, raw);
        }

        try {
            JsonElement parsed = JsonParser.parseString(candidate);
            if (!parsed.isJsonObject()) {
                return AgentModelResponse.finalAnswer(raw, raw);
            }
            JsonObject object = parsed.getAsJsonObject();
            String type = getString(object, "type");
            if ("tool_call".equalsIgnoreCase(type) || object.has("tool") || object.has("toolName")) {
                String toolName = getString(object, object.has("toolName") ? "toolName" : "tool");
                Map<String, String> arguments = parseArguments(object.get("arguments"));
                return AgentModelResponse.toolCall(ToolCallRequest.of(toolName, arguments), raw);
            }
            if ("final".equalsIgnoreCase(type) || object.has("answer")) {
                String answer = getString(object, "answer");
                return AgentModelResponse.finalAnswer(answer.isEmpty() ? raw : answer, raw);
            }
        } catch (RuntimeException ignored) {
            return AgentModelResponse.finalAnswer(raw, raw);
        }
        return AgentModelResponse.finalAnswer(raw, raw);
    }

    private static Map<String, String> parseArguments(JsonElement element) {
        Map<String, String> arguments = new LinkedHashMap<>();
        if (element == null || !element.isJsonObject()) {
            return arguments;
        }
        JsonObject object = element.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            JsonElement value = entry.getValue();
            if (value == null || value.isJsonNull()) {
                continue;
            }
            if (value.isJsonPrimitive()) {
                arguments.put(entry.getKey(), value.getAsJsonPrimitive().getAsString());
            } else {
                arguments.put(entry.getKey(), GSON.toJson(value));
            }
        }
        return arguments;
    }

    private static String extractJsonObject(String raw) {
        String fenced = extractFencedJson(raw);
        if (fenced != null) {
            return fenced;
        }
        int start = raw.indexOf('{');
        if (start < 0) {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < raw.length(); i++) {
            char ch = raw.charAt(i);
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
            } else if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return raw.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private static String extractFencedJson(String raw) {
        int fenceStart = raw.indexOf("```");
        if (fenceStart < 0) {
            return null;
        }
        int contentStart = raw.indexOf('\n', fenceStart + 3);
        if (contentStart < 0) {
            return null;
        }
        int fenceEnd = raw.indexOf("```", contentStart + 1);
        if (fenceEnd < 0) {
            return null;
        }
        String header = raw.substring(fenceStart + 3, contentStart).trim();
        if (!header.isEmpty() && !"json".equalsIgnoreCase(header)) {
            return null;
        }
        return raw.substring(contentStart + 1, fenceEnd).trim();
    }

    private static String getString(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (RuntimeException ex) {
            return "";
        }
    }
}
