package com.mycompany.tutorhub_enterprise.client.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

public final class OpenAiCompatibleAiAgentService implements AiAgentService {

    private static final Gson GSON = new Gson();

    private final ExecutorService executor;
    private final String baseUrl;
    private final String model;
    private final String apiKey;

    public OpenAiCompatibleAiAgentService(String baseUrl, String model, String apiKey) {
        this.baseUrl = cleanOrDefault(baseUrl, AiAgentProviderConfig.DEFAULT_OPENAI_BASE_URL);
        this.model = cleanOrDefault(model, AiAgentProviderConfig.DEFAULT_OPENAI_MODEL);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.executor = Executors.newCachedThreadPool(new ThreadFactory() {
            private int index = 1;

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "openai-compatible-ai-stream-" + index++);
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    @Override
    public AiAgentStreamHandle streamChat(AiAgentRequest request, AiAgentStreamCallback callback) {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        executor.submit(() -> doStream(request, callback, cancelled));
        return () -> cancelled.set(true);
    }

    @Override
    public String getProviderName() {
        return "OpenAI-compatible (" + model + " @ " + baseUrl + ")";
    }

    private void doStream(AiAgentRequest request, AiAgentStreamCallback callback, AtomicBoolean cancelled) {
        HttpURLConnection conn = null;
        try {
            requireApiKeyWhenRemote();
            conn = (HttpURLConnection) chatCompletionsUrl().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(90000);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "text/event-stream, application/json");
            if (!apiKey.isBlank()) {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
            conn.setDoOutput(true);

            byte[] body = GSON.toJson(buildPayload(request)).getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }

            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("OpenAI-compatible endpoint HTTP " + status + ": "
                        + readBody(conn.getErrorStream()));
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder nonSseBody = new StringBuilder();
                String line;
                boolean sawSse = false;
                while (!cancelled.get() && (line = reader.readLine()) != null) {
                    if (line.startsWith("data:") || "[DONE]".equals(line.trim())) {
                        sawSse = true;
                        if (handleSseLine(line, callback)) {
                            break;
                        }
                    } else if (!line.isBlank()) {
                        nonSseBody.append(line).append('\n');
                    }
                }
                if (!sawSse && nonSseBody.length() > 0 && !cancelled.get()) {
                    emitNonStreamingResponse(nonSseBody.toString(), callback);
                }
            }

            if (!cancelled.get()) {
                callback.onComplete();
            }
        } catch (Exception ex) {
            if (!cancelled.get()) {
                callback.onError(ex);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private JsonObject buildPayload(AiAgentRequest request) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", model);
        payload.addProperty("stream", true);
        payload.addProperty("temperature", 0.2);

        JsonArray messages = new JsonArray();
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        String prompt = AiPromptComposer.compose(request);
        JsonArray multimodalContent = buildMultimodalContent(request, prompt);
        if (multimodalContent == null) {
            userMessage.addProperty("content", prompt);
        } else {
            userMessage.add("content", multimodalContent);
        }
        messages.add(userMessage);
        payload.add("messages", messages);
        return payload;
    }

    private JsonArray buildMultimodalContent(AiAgentRequest request, String prompt) {
        JsonArray imageParts = buildImageParts(request);
        if (imageParts.size() == 0) {
            return null;
        }
        JsonArray content = new JsonArray();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("type", "text");
        textPart.addProperty("text", prompt == null ? "" : prompt);
        content.add(textPart);
        for (JsonElement imagePart : imageParts) {
            content.add(imagePart);
        }
        return content;
    }

    private JsonArray buildImageParts(AiAgentRequest request) {
        JsonArray parts = new JsonArray();
        if (request == null || request.getMetadata() == null) {
            return parts;
        }
        String raw = request.getMetadata().getOrDefault(AiPromptComposer.METADATA_ATTACHMENTS_JSON, "");
        if (raw == null || raw.trim().isEmpty() || "[]".equals(raw.trim())) {
            return parts;
        }
        try {
            JsonElement parsed = JsonParser.parseString(raw);
            if (!parsed.isJsonArray()) {
                return parts;
            }
            for (JsonElement element : parsed.getAsJsonArray()) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject attachment = element.getAsJsonObject();
                String kind = stringValue(attachment, "kind");
                String dataUrl = stringValue(attachment, "dataUrl");
                if (!"image".equals(kind) || !isInlineImageUrl(dataUrl)) {
                    continue;
                }
                JsonObject imageUrl = new JsonObject();
                imageUrl.addProperty("url", dataUrl);

                JsonObject part = new JsonObject();
                part.addProperty("type", "image_url");
                part.add("image_url", imageUrl);
                parts.add(part);
            }
        } catch (RuntimeException ignored) {
            // Keep the request text-only if attachment metadata cannot be parsed.
        }
        return parts;
    }

    private boolean isInlineImageUrl(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("data:image/") && lower.contains(";base64,");
    }

    private boolean handleSseLine(String line, AiAgentStreamCallback callback) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        String data = line.trim();
        if (data.startsWith("data:")) {
            data = data.substring("data:".length()).trim();
        }
        if (data.isEmpty()) {
            return false;
        }
        if ("[DONE]".equals(data)) {
            return true;
        }
        try {
            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                return false;
            }
            JsonObject choice = choices.get(0).getAsJsonObject();
            JsonObject delta = choice.getAsJsonObject("delta");
            if (delta != null && hasString(delta, "content")) {
                callback.onDelta(delta.get("content").getAsString());
                return false;
            }
            JsonObject message = choice.getAsJsonObject("message");
            if (message != null && hasString(message, "content")) {
                callback.onDelta(message.get("content").getAsString());
            }
        } catch (RuntimeException ignored) {
            // Ignore keep-alive and provider-specific metadata lines.
        }
        return false;
    }

    private void emitNonStreamingResponse(String responseBody, AiAgentStreamCallback callback) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                return;
            }
            JsonObject choice = choices.get(0).getAsJsonObject();
            JsonObject message = choice.getAsJsonObject("message");
            if (message != null && hasString(message, "content")) {
                callback.onDelta(message.get("content").getAsString());
            }
        } catch (RuntimeException ignored) {
            // If a compatible provider returns an unknown JSON shape, surface no partial text.
        }
    }

    private URL chatCompletionsUrl() throws Exception {
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (normalized.endsWith("/chat/completions")) {
            return new URL(normalized);
        }
        return new URL(normalized + "/chat/completions");
    }

    private void requireApiKeyWhenRemote() {
        if (!apiKey.isBlank()) {
            return;
        }
        String lower = baseUrl.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://localhost")
                || lower.startsWith("http://127.0.0.1")
                || lower.startsWith("http://[::1]")) {
            return;
        }
        throw new IllegalStateException("OpenAI-compatible API key is required for remote endpoints. "
                + "Set TUTORHUB_OPENAI_API_KEY or enter a key in the provider panel for this session.");
    }

    private boolean hasString(JsonObject obj, String key) {
        JsonElement element = obj == null ? null : obj.get(key);
        return element != null && !element.isJsonNull() && element.isJsonPrimitive();
    }

    private String stringValue(JsonObject obj, String key) {
        if (!hasString(obj, key)) {
            return "";
        }
        return obj.get(key).getAsString();
    }

    private String readBody(InputStream stream) {
        if (stream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private String cleanOrDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }
}
