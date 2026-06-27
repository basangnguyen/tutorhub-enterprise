package com.mycompany.tutorhub_enterprise.server.oauth;

import com.mycompany.tutorhub_enterprise.server.SocialAuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class FacebookOAuthCallbackServer {
    private HttpServer server;
    private final int port;

    public FacebookOAuthCallbackServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/oauth/facebook/worker-result", new WorkerResultHandler());
        server.createContext("/debug/facebook-connectivity", new DebugConnectivityHandler());
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool()); // creates a default executor
        server.start();
        System.out.println("[OAUTH CONFIG] Facebook Callback Server started on port " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("[OAUTH CONFIG] Facebook Callback Server stopped.");
        }
    }

    static class WorkerResultHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (!"POST".equalsIgnoreCase(t.getRequestMethod())) {
                t.sendResponseHeaders(405, -1);
                return;
            }

            // Read body
            java.io.InputStream is = t.getRequestBody();
            String body = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("[FACEBOOK_OAUTH] Nhận kết quả từ Worker.");

            com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(body, com.google.gson.JsonObject.class);
            String state = json.has("state") && !json.get("state").isJsonNull() ? json.get("state").getAsString() : null;
            String providerUserId = json.has("providerUserId") && !json.get("providerUserId").isJsonNull() ? json.get("providerUserId").getAsString() : null;
            String displayName = json.has("displayName") && !json.get("displayName").isJsonNull() ? json.get("displayName").getAsString() : null;
            String email = json.has("email") && !json.get("email").isJsonNull() ? json.get("email").getAsString() : null;
            String pictureUrl = json.has("pictureUrl") && !json.get("pictureUrl").isJsonNull() ? json.get("pictureUrl").getAsString() : null;
            boolean success = json.has("success") && !json.get("success").isJsonNull() ? json.get("success").getAsBoolean() : false;
            String errorMessage = json.has("errorMessage") && !json.get("errorMessage").isJsonNull() ? json.get("errorMessage").getAsString() : null;
            String signature = json.has("signature") && !json.get("signature").isJsonNull() ? json.get("signature").getAsString() : null;

            int statusCode = 200;
            String responseStr = "OK";

            try {
                if (state == null || signature == null) {
                    throw new Exception("Thiếu tham số state hoặc signature.");
                }

                // Verify signature
                String secret = com.mycompany.tutorhub_enterprise.server.SocialAuthConfig.getFacebookWorkerSharedSecret();
                String payloadToSign = state + "|" + (providerUserId != null ? providerUserId : "") + "|" + success;
                
                javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
                javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
                mac.init(secretKey);
                byte[] digest = mac.doFinal(payloadToSign.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02x", b));
                }
                String expectedSignature = sb.toString();

                if (!expectedSignature.equalsIgnoreCase(signature)) {
                    throw new Exception("Sai chữ ký bảo mật.");
                }

                // Process synchronously
                com.mycompany.tutorhub_enterprise.models.auth.AuthResponse authRes = SocialAuthService.handleWorkerResult(state, providerUserId, displayName, email, pictureUrl, success, errorMessage);
                if (authRes == null) {
                    throw new Exception("Xử lý thất bại, AuthResponse null.");
                }
            } catch (Exception e) {
                statusCode = 500;
                responseStr = "Lỗi: " + e.getMessage();
                System.err.println("[FACEBOOK_OAUTH] Lỗi xử lý worker result: " + e.getMessage());
            }

            byte[] responseBytes = responseStr.getBytes("UTF-8");
            t.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            t.sendResponseHeaders(statusCode, responseBytes.length);
            OutputStream os = t.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }

    static class DebugConnectivityHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response;
            int statusCode = 200;
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                        .version(java.net.http.HttpClient.Version.HTTP_1_1)
                        .connectTimeout(java.time.Duration.ofSeconds(10))
                        .build();

                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("https://graph.facebook.com"))
                        .timeout(java.time.Duration.ofSeconds(20))
                        .GET()
                        .build();

                java.net.http.HttpResponse<String> fbResponse = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                response = "CONNECT_OK\nHTTP Status: " + fbResponse.statusCode() + "\n";
            } catch (Exception e) {
                statusCode = 500;
                response = "CONNECT_FAILED\nError: " + e.getClass().getSimpleName() + " - " + e.getMessage() + "\n";
            }

            byte[] responseBytes = response.getBytes("UTF-8");
            t.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            t.sendResponseHeaders(statusCode, responseBytes.length);
            OutputStream os = t.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }
}
