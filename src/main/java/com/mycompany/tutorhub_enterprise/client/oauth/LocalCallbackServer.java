package com.mycompany.tutorhub_enterprise.client.oauth;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

import java.net.InetAddress;

public class LocalCallbackServer {
    private ServerSocket serverSocket;
    private int port;
    private volatile boolean running = false;

    public int start() throws Exception {
        String envPort = System.getProperty("tutorhub.google.redirect.port");
        if (envPort == null || envPort.trim().isEmpty()) {
            envPort = System.getenv("TUTORHUB_GOOGLE_REDIRECT_PORT");
        }
        if (envPort == null || envPort.trim().isEmpty()) {
            try (java.io.InputStream is = getClass().getResourceAsStream("/client-public.properties")) {
                if (is != null) {
                    java.util.Properties props = new java.util.Properties();
                    props.load(is);
                    envPort = props.getProperty("tutorhub.google.redirect.port");
                }
            } catch (Exception e) {}
        }
        int targetPort = 8889;
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                targetPort = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        }

        try {
            serverSocket = new ServerSocket(targetPort, 50, InetAddress.getByName("127.0.0.1"));
            port = serverSocket.getLocalPort();
            running = true;
            return port;
        } catch (Exception e) {
            throw new Exception("Không thể bind HTTP server trên port " + (targetPort == 0 ? "ngẫu nhiên" : targetPort) + " (127.0.0.1). Chi tiết: " + e.getMessage(), e);
        }
    }

    public OAuthCallbackResult awaitCallback(int timeoutMs) {
        try {
            serverSocket.setSoTimeout(timeoutMs);
            while (running) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     OutputStream out = clientSocket.getOutputStream()) {
                    
                    String requestLine = in.readLine();
                    if (requestLine == null) continue;

                    if (requestLine.startsWith("GET /callback")) {
                        String query = "";
                        int queryStart = requestLine.indexOf('?');
                        int spaceIndex = requestLine.indexOf(' ', queryStart > 0 ? queryStart : 4);
                        if (queryStart != -1 && spaceIndex != -1) {
                            query = requestLine.substring(queryStart + 1, spaceIndex);
                        }

                        Map<String, String> params = parseQueryParams(query);
                        
                        String responseBody;
                        
                        if (params.containsKey("error")) {
                            responseBody = "<html><body><h2>Đăng nhập thất bại: " + params.get("error") + "</h2><p>Vui lòng đóng tab này và thử lại.</p></body></html>";
                        } else {
                            // Build self-contained auth-success page with inlined CSS/JS/logo
                            responseBody = buildAuthSuccessPage("google");
                        }
                        
                        String httpResponse = "HTTP/1.1 200 OK\r\n" +
                                              "Content-Length: " + responseBody.getBytes("UTF-8").length + "\r\n" +
                                              "Content-Type: text/html; charset=UTF-8\r\n" +
                                              "Connection: close\r\n\r\n" +
                                              responseBody;
                        out.write(httpResponse.getBytes("UTF-8"));
                        out.flush();

                        return new OAuthCallbackResult(
                                params.get("code"),
                                params.get("state"),
                                params.get("error"),
                                params.get("error_description")
                        );
                    }
                } catch (SocketTimeoutException e) {
                    return new OAuthCallbackResult(null, null, "timeout", "Đã hết thời gian chờ đăng nhập (120s)");
                }
            }
        } catch (Exception e) {
            if (!running) {
                return new OAuthCallbackResult(null, null, "cancelled", "Đã hủy luồng đăng nhập");
            }
            return new OAuthCallbackResult(null, null, "server_error", "Lỗi callback server: " + e.getMessage());
        } finally {
            stop();
        }
        return new OAuthCallbackResult(null, null, "cancelled", "Đã hủy luồng đăng nhập");
    }

    /**
     * Builds a self-contained auth-success HTML page by loading template files
     * from resources (auth-success.html, auth-success.css, auth-success.js)
     * and inlining CSS/JS + converting logo to base64 data URI.
     * This ensures the entire page is served in a single HTTP response,
     * since the callback server stops immediately after returning the result.
     */
    private String buildAuthSuccessPage(String provider) {
        try {
            String htmlTemplate = loadResource("/auth-success/auth-success.html");
            String css = loadResource("/auth-success/auth-success.css");
            String js = loadResource("/auth-success/auth-success.js");
            String logoBase64 = loadLogoAsBase64("/images/logomoi.png");
            
            if (htmlTemplate == null || htmlTemplate.isEmpty()) {
                // Fallback to simple success message
                return "<html><body><h2>Đăng nhập thành công! Bạn có thể đóng tab này.</h2><script>window.close();</script></body></html>";
            }
            
            // Inline CSS: replace <link rel="stylesheet" href="auth-success.css" /> with <style>...</style>
            htmlTemplate = htmlTemplate.replace(
                "<link rel=\"stylesheet\" href=\"auth-success.css\" />",
                "<style>\n" + (css != null ? css : "") + "\n</style>"
            );
            
            // Inline JS: replace <script src="auth-success.js"></script> with inline <script>
            htmlTemplate = htmlTemplate.replace(
                "<script src=\"auth-success.js\"></script>",
                "<script>\n" + (js != null ? js : "") + "\n</script>"
            );
            
            // Replace logo image paths with base64 data URIs
            if (logoBase64 != null && !logoBase64.isEmpty()) {
                String dataUri = "data:image/png;base64," + logoBase64;
                htmlTemplate = htmlTemplate.replace("src=\"../images/logomoi.png\"", "src=\"" + dataUri + "\"");
            }
            
            // Base64 encode SVG icons to make page fully self-contained
            String googleB64 = loadLogoAsBase64("/images/icon/google.svg");
            String facebookB64 = loadLogoAsBase64("/images/icon/facebook.svg");
            String zaloB64 = loadLogoAsBase64("/images/icon/zalo-2.png");
            String instagramB64 = loadLogoAsBase64("/images/icon/instagram.svg");
            
            if (facebookB64 != null) {
                htmlTemplate = htmlTemplate.replace("src=\"/images/icon/facebook.svg\"", "src=\"data:image/svg+xml;base64," + facebookB64 + "\"");
            }
            if (zaloB64 != null) {
                htmlTemplate = htmlTemplate.replace("src=\"/images/icon/zalo-2.png\"", "src=\"data:image/png;base64," + zaloB64 + "\"");
            }
            if (instagramB64 != null) {
                htmlTemplate = htmlTemplate.replace("src=\"/images/icon/instagram.svg\"", "src=\"data:image/svg+xml;base64," + instagramB64 + "\"");
            }
            
            String googleDataUri = googleB64 != null ? "data:image/svg+xml;base64," + googleB64 : "";
            String facebookDataUri = facebookB64 != null ? "data:image/svg+xml;base64," + facebookB64 : "";
            
            // Inject provider info via inline script (before </body>)
            String providerScript = "<script>" +
                "window.__AUTH_PROVIDER = '" + (provider != null ? provider : "") + "';" +
                "(function(){" +
                "  var p = window.__AUTH_PROVIDER;" +
                "  var el = document.getElementById('providerLabel');" +
                "  var nm = document.getElementById('providerName');" +
                "  var icon = document.getElementById('providerIcon');" +
                "  if(el && nm && p) {" +
                "    if(p === 'google') { " +
                "      nm.textContent = 'Google'; " +
                "      if (icon) icon.src = '" + googleDataUri + "'; " +
                "      el.style.display = 'inline-flex'; " +
                "    }" +
                "    else if(p === 'facebook') { " +
                "      nm.textContent = 'Facebook'; " +
                "      if (icon) icon.src = '" + facebookDataUri + "'; " +
                "      el.style.display = 'inline-flex'; " +
                "    }" +
                "  }" +
                "})();" +
                "</script>";
            htmlTemplate = htmlTemplate.replace("</body>", providerScript + "\n</body>");
            
            return htmlTemplate;
            
        } catch (Exception e) {
            System.err.println("[OAUTH] Error building auth-success page: " + e.getMessage());
            return "<html><body><h2>Đăng nhập thành công! Bạn có thể đóng tab này.</h2><script>window.close();</script></body></html>";
        }
    }
    
    /**
     * Load a text resource from the classpath.
     */
    private String loadResource(String path) {
        try (java.io.InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) return null;
            return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Load a binary resource and return it as a Base64-encoded string.
     */
    private String loadLogoAsBase64(String path) {
        try (java.io.InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) return null;
            byte[] bytes = is.readAllBytes();
            return java.util.Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                if (idx > 0) {
                    String key = java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                    String value = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                    params.put(key, value);
                } else if (idx == -1 && pair.length() > 0) {
                    String key = java.net.URLDecoder.decode(pair, "UTF-8");
                    params.put(key, "");
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return params;
    }
}
