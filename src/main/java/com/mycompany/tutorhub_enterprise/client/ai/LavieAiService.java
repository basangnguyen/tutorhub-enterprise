package com.mycompany.tutorhub_enterprise.client.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

public class LavieAiService implements AiAgentService {

    private static final String DEFAULT_STREAM_URL =
            "https://hocba299-3-tutorhub-ai.hf.space/api/chat/stream";
    private static final Gson GSON = new Gson();

    private final ExecutorService executor;
    private final String streamUrl;

    public LavieAiService() {
        this(DEFAULT_STREAM_URL);
    }

    public LavieAiService(String streamUrl) {
        this.streamUrl = streamUrl;
        this.executor = Executors.newCachedThreadPool(new ThreadFactory() {
            private int index = 1;
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "lavie-ai-stream-" + index++);
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
        return "Lavie / Hugging Face";
    }

    private void doStream(AiAgentRequest request, AiAgentStreamCallback callback, AtomicBoolean cancelled) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(streamUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(45000);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "text/event-stream, application/json");
            conn.setDoOutput(true);

            JsonObject payload = new JsonObject();
            payload.addProperty("message", request.getMessage());
            payload.addProperty("user_id", request.getUserId());
            payload.addProperty("voice", false);
            payload.addProperty("conversation_id", request.getConversationId());

            byte[] body = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }

            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Lavie server HTTP " + status + ": " + readBody(conn.getErrorStream()));
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while (!cancelled.get() && (line = reader.readLine()) != null) {
                    if (handleLine(line, callback)) {
                        break;
                    }
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

    private boolean handleLine(String line, AiAgentStreamCallback callback) {
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
            if (json.has("content") && !json.get("content").isJsonNull()) {
                callback.onDelta(json.get("content").getAsString());
            }
        } catch (RuntimeException ignored) {
            // Some SSE servers send keepalive or partial metadata lines. Ignore safely.
        }
        return false;
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

}
