package com.mycompany.tutorhub_enterprise.client.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mycompany.tutorhub_enterprise.client.exam.utils.CryptoUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

public class V2LoopbackKeyHandoffClient {

    private final Gson gson = new Gson();

    public Optional<SecretKey> requestKey(String host, int port, String handoffId, String nonce) {
        if (host == null || !host.equals("127.0.0.1")) {
            // Security: Only allow localhost
            System.err.println("[TSE_CHILD_IPC] Rejecting non-localhost IPC host: " + host);
            return Optional.empty();
        }

        try {
            URL url = new URL("http://" + host + ":" + port + "/v2/handoff/key/consume");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(3000);
            conn.setDoOutput(true);

            JsonObject req = new JsonObject();
            req.addProperty("handoffId", handoffId);
            req.addProperty("nonce", nonce);
            req.addProperty("clientMode", "V2_DEBUG");

            String jsonPayload = gson.toJson(req);
            byte[] out = jsonPayload.getBytes(StandardCharsets.UTF_8);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(out);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (InputStream is = conn.getInputStream()) {
                    String resBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    JsonObject res = gson.fromJson(resBody, JsonObject.class);
                    if (res.has("success") && res.get("success").getAsBoolean() && res.has("keyB64")) {
                        String keyB64 = res.get("keyB64").getAsString();
                        byte[] decodedKey = Base64.getDecoder().decode(keyB64);
                        SecretKey secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
                        return Optional.of(secretKey);
                    }
                }
            } else {
                try (InputStream errIs = conn.getErrorStream()) {
                    if (errIs != null) {
                        String errBody = new String(errIs.readAllBytes(), StandardCharsets.UTF_8);
                        System.err.println("[TSE_CHILD_IPC] Request failed with code " + responseCode + ", error payload hidden");
                        // We do not log the full errBody to avoid any accidental leak, just the code
                        JsonObject errRes = gson.fromJson(errBody, JsonObject.class);
                        if (errRes.has("errorCode")) {
                            System.err.println("[TSE_CHILD_IPC] Error code: " + errRes.get("errorCode").getAsString());
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("[TSE_CHILD_IPC] Exception during IPC: " + e.getMessage());
        }

        return Optional.empty();
    }
}
