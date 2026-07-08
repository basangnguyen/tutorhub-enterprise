package com.mycompany.tutorhub_enterprise.client.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mycompany.tutorhub_enterprise.config.AppConfig;
import javazoom.jl.player.Player;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LavieTextToSpeechService {

    public interface Listener {
        default void onQueued() {
        }

        default void onStarted(String audioUrl) {
        }

        default void onFinished() {
        }

        default void onError(Exception error) {
        }
    }

    private static final Gson GSON = new Gson();
    private static final int MAX_TTS_CHARS = 4_000;

    private final ExecutorService requestExecutor = Executors.newSingleThreadExecutor(namedFactory("lavie-tts-request"));
    private final ExecutorService playerExecutor = Executors.newSingleThreadExecutor(namedFactory("lavie-tts-player"));
    private final Queue<AudioJob> audioQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean playing = new AtomicBoolean(false);
    private volatile Player currentPlayer;

    public void speak(String text, Listener listener) {
        String cleanText = sanitizeForSpeech(text);
        Listener safeListener = listener == null ? new Listener() {
        } : listener;
        if (cleanText.isBlank()) {
            safeListener.onError(new IllegalArgumentException("Không có nội dung để đọc."));
            return;
        }
        safeListener.onQueued();
        requestExecutor.submit(() -> {
            try {
                String audioUrl = requestAudioUrl(cleanText);
                enqueue(audioUrl, safeListener);
            } catch (Exception ex) {
                safeListener.onError(ex);
            }
        });
    }

    public void stop() {
        audioQueue.clear();
        Player player = currentPlayer;
        if (player != null) {
            try {
                player.close();
            } catch (Exception ignored) {
            }
        }
    }

    public void shutdown() {
        stop();
        requestExecutor.shutdownNow();
        playerExecutor.shutdownNow();
    }

    private void enqueue(String audioUrl, Listener listener) {
        if (audioUrl == null || audioUrl.isBlank()) {
            listener.onError(new IllegalStateException("Máy chủ TTS không trả về audio_url."));
            return;
        }
        audioQueue.offer(new AudioJob(audioUrl, listener));
        drainQueue();
    }

    private void drainQueue() {
        if (!playing.compareAndSet(false, true)) {
            return;
        }
        playerExecutor.submit(() -> {
            try {
                AudioJob job;
                while ((job = audioQueue.poll()) != null) {
                    play(job);
                }
            } finally {
                playing.set(false);
                if (!audioQueue.isEmpty()) {
                    drainQueue();
                }
            }
        });
    }

    private void play(AudioJob job) {
        try (InputStream inputStream = new URL(job.audioUrl).openStream()) {
            job.listener.onStarted(job.audioUrl);
            Player player = new Player(inputStream);
            currentPlayer = player;
            player.play();
            job.listener.onFinished();
        } catch (Exception ex) {
            job.listener.onError(ex);
        } finally {
            currentPlayer = null;
        }
    }

    private String requestAudioUrl(String text) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(joinUrl(AppConfig.AI_SERVER_URL, "/api/tts")).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(90_000);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "application/json");
            String hfToken = lavieBearerToken();
            if (!hfToken.isBlank()) {
                conn.setRequestProperty("Authorization", "Bearer " + hfToken);
            }
            conn.setDoOutput(true);

            JsonObject payload = new JsonObject();
            payload.addProperty("text", text);
            byte[] body = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
            try (OutputStream outputStream = conn.getOutputStream()) {
                outputStream.write(body);
            }

            int status = conn.getResponseCode();
            String response = status >= 200 && status < 300
                    ? readBody(conn.getInputStream())
                    : readBody(conn.getErrorStream());
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Lavie TTS HTTP " + status + ": " + response);
            }
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            String audioUrl = json.has("audio_url") && !json.get("audio_url").isJsonNull()
                    ? json.get("audio_url").getAsString()
                    : "";
            if (audioUrl.isBlank()) {
                throw new IllegalStateException("Lavie TTS response thiếu audio_url.");
            }
            return absoluteAudioUrl(audioUrl);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String sanitizeForSpeech(String text) {
        String value = text == null ? "" : text;
        value = value.replace('\uFFFD', ' ');
        value = value.replaceAll("(?s)```.*?```", " Đoạn mã đã được tạo. ");
        value = value.replaceAll("`([^`]+)`", "$1");
        value = value.replaceAll("(?m)^\\s{0,3}#{1,6}\\s*", "");
        value = value.replaceAll("(?m)^\\s{0,3}>\\s?", "");
        value = value.replace("*", "").replace("_", "");
        value = value.replaceAll("\\[([^\\]]+)]\\(([^)]+)\\)", "$1");
        value = value.replaceAll("<[^>]+>", " ");
        value = value.replaceAll("\\s+", " ").trim();
        if (value.length() > MAX_TTS_CHARS) {
            value = value.substring(0, MAX_TTS_CHARS).trim()
                    + ". Nội dung còn lại khá dài, Anh có thể bấm Đọc lại từng phần nếu cần.";
        }
        return value;
    }

    private static String absoluteAudioUrl(String audioUrl) {
        String value = audioUrl == null ? "" : audioUrl.trim().replace("\\/", "/");
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        return joinUrl(AppConfig.AI_SERVER_URL, value);
    }

    private static String lavieBearerToken() {
        String token = System.getProperty("tutorhub.lavie.token", "");
        if (token == null || token.isBlank()) {
            token = System.getenv("TUTORHUB_LAVIE_TOKEN");
        }
        return token == null ? "" : token.trim();
    }

    private static String joinUrl(String base, String path) {
        String cleanBase = base == null ? "" : base.trim();
        String cleanPath = path == null ? "" : path.trim();
        while (cleanBase.endsWith("/")) {
            cleanBase = cleanBase.substring(0, cleanBase.length() - 1);
        }
        if (!cleanPath.startsWith("/")) {
            cleanPath = "/" + cleanPath;
        }
        return cleanBase + cleanPath;
    }

    private static String readBody(InputStream stream) throws Exception {
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
        }
    }

    private static ThreadFactory namedFactory(String name) {
        return new ThreadFactory() {
            private int index = 1;

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, name + "-" + index++);
                thread.setDaemon(true);
                return thread;
            }
        };
    }

    private record AudioJob(String audioUrl, Listener listener) {
    }
}
