package com.mycompany.tutorhub_enterprise.client.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class V2LoopbackKeyHandoffServer {

    private HttpServer server;
    private int port = -1;
    private final ConcurrentHashMap<String, String> expectedNonces = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private ScheduledExecutorService autoStopExecutor;

    public void start() throws IOException {
        // Bind to localhost only, random port (0)
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        
        server.createContext("/v2/handoff/key/consume", new KeyConsumeHandler());
        server.setExecutor(Executors.newFixedThreadPool(2)); // basic executor
        server.start();
        
        this.port = server.getAddress().getPort();
        
        // Auto stop after 5 minutes if not consumed
        autoStopExecutor = Executors.newSingleThreadScheduledExecutor();
        autoStopExecutor.schedule(() -> stop(), 5, TimeUnit.MINUTES);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (autoStopExecutor != null) {
            autoStopExecutor.shutdownNow();
            autoStopExecutor = null;
        }
    }

    public int getPort() {
        return port;
    }

    public void registerExpectedNonce(String handoffId, String nonce) {
        if (handoffId != null && nonce != null) {
            expectedNonces.put(handoffId, nonce);
        }
    }

    private class KeyConsumeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendErrorResponse(exchange, "METHOD_NOT_ALLOWED");
                return;
            }

            try (InputStream is = exchange.getRequestBody()) {
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                
                JsonObject req;
                try {
                    req = gson.fromJson(body, JsonObject.class);
                } catch (JsonSyntaxException e) {
                    sendErrorResponse(exchange, "INVALID_JSON");
                    return;
                }

                if (req == null || !req.has("handoffId") || !req.has("nonce")) {
                    sendErrorResponse(exchange, "MISSING_FIELDS");
                    return;
                }

                String handoffId = req.get("handoffId").getAsString();
                String nonce = req.get("nonce").getAsString();

                String expectedNonce = expectedNonces.get(handoffId);
                if (expectedNonce == null || !expectedNonce.equals(nonce)) {
                    sendErrorResponse(exchange, "INVALID_NONCE_OR_HANDOFF");
                    return;
                }

                // Consume the key from Registry
                Optional<String> keyOpt = V2RuntimeKeyRegistry.consumeKey(handoffId);
                if (keyOpt.isEmpty()) {
                    sendErrorResponse(exchange, "KEY_EXPIRED_OR_ALREADY_CONSUMED");
                    return;
                }

                // Remove the nonce so it can't be used again
                expectedNonces.remove(handoffId);

                // Success
                JsonObject res = new JsonObject();
                res.addProperty("success", true);
                res.addProperty("keyB64", keyOpt.get());
                res.addProperty("handoffId", handoffId);

                sendSuccessResponse(exchange, gson.toJson(res));
                
                // Stop server shortly after successful consume
                if (autoStopExecutor != null && !autoStopExecutor.isShutdown()) {
                    autoStopExecutor.schedule(() -> stop(), 2, TimeUnit.SECONDS);
                }

            } catch (Exception e) {
                e.printStackTrace(); // Minimal debug log without key info
                sendErrorResponse(exchange, "INTERNAL_ERROR");
            }
        }
    }

    private void sendErrorResponse(HttpExchange exchange, String errorCode) throws IOException {
        JsonObject res = new JsonObject();
        res.addProperty("success", false);
        res.addProperty("errorCode", errorCode);
        String responseBody = gson.toJson(res);
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(400, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendSuccessResponse(HttpExchange exchange, String responseBody) throws IOException {
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
