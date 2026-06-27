package com.mycompany.tutorhub_enterprise;

import com.mycompany.tutorhub_enterprise.client.services.V2LoopbackKeyHandoffClient;
import com.mycompany.tutorhub_enterprise.client.services.V2LoopbackKeyHandoffServer;
import com.mycompany.tutorhub_enterprise.client.services.V2RuntimeKeyRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2LoopbackKeyHandoffServerTest {

    private V2LoopbackKeyHandoffServer server;
    private V2LoopbackKeyHandoffClient client;

    @BeforeEach
    public void setUp() throws Exception {
        V2RuntimeKeyRegistry.clearForTest();
        server = new V2LoopbackKeyHandoffServer();
        server.start();
        client = new V2LoopbackKeyHandoffClient();
    }

    @AfterEach
    public void tearDown() {
        if (server != null) {
            server.stop();
        }
        V2RuntimeKeyRegistry.clearForTest();
    }

    @Test
    public void testServerBindsToRandomPort() {
        assertTrue(server.getPort() > 0);
    }

    @Test
    public void testValidConsumeRoundTrip() throws Exception {
        String testKeyB64 = Base64.getEncoder().encodeToString("12345678901234567890123456789012".getBytes(StandardCharsets.UTF_8));
        String handoffId = V2RuntimeKeyRegistry.registerKey(testKeyB64, Duration.ofMinutes(5), "TEST_VALID_CONSUME");
        String nonce = "random-nonce-123";

        server.registerExpectedNonce(handoffId, nonce);

        Optional<SecretKey> retrievedKey = client.requestKey("127.0.0.1", server.getPort(), handoffId, nonce);
        
        assertTrue(retrievedKey.isPresent());
        String retrievedKeyB64 = Base64.getEncoder().encodeToString(retrievedKey.get().getEncoded());
        assertEquals(testKeyB64, retrievedKeyB64);
    }

    @Test
    public void testConsumeTwiceFails() throws Exception {
        String testKeyB64 = Base64.getEncoder().encodeToString("12345678901234567890123456789012".getBytes(StandardCharsets.UTF_8));
        String handoffId = V2RuntimeKeyRegistry.registerKey(testKeyB64, Duration.ofMinutes(5), "TEST_CONSUME_TWICE");
        String nonce = "random-nonce-123";

        server.registerExpectedNonce(handoffId, nonce);

        // First consume
        Optional<SecretKey> retrievedKey1 = client.requestKey("127.0.0.1", server.getPort(), handoffId, nonce);
        assertTrue(retrievedKey1.isPresent());

        // Second consume
        Optional<SecretKey> retrievedKey2 = client.requestKey("127.0.0.1", server.getPort(), handoffId, nonce);
        assertFalse(retrievedKey2.isPresent());
    }

    @Test
    public void testWrongNonceFails() throws Exception {
        String testKeyB64 = Base64.getEncoder().encodeToString("12345678901234567890123456789012".getBytes(StandardCharsets.UTF_8));
        String handoffId = V2RuntimeKeyRegistry.registerKey(testKeyB64, Duration.ofMinutes(5), "TEST_WRONG_NONCE");
        String nonce = "real-nonce-123";

        server.registerExpectedNonce(handoffId, nonce);

        Optional<SecretKey> retrievedKey = client.requestKey("127.0.0.1", server.getPort(), handoffId, "fake-nonce-456");
        assertFalse(retrievedKey.isPresent());
    }

    @Test
    public void testUnknownHandoffIdFails() {
        Optional<SecretKey> retrievedKey = client.requestKey("127.0.0.1", server.getPort(), "unknown-id", "any-nonce");
        assertFalse(retrievedKey.isPresent());
    }

    @Test
    public void testGetRequestFails() throws Exception {
        URL url = new URL("http://127.0.0.1:" + server.getPort() + "/v2/handoff/key/consume?handoffId=1&nonce=2");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        int responseCode = conn.getResponseCode();
        assertEquals(400, responseCode);
    }

    @Test
    public void testMissingNonceFails() throws Exception {
        URL url = new URL("http://127.0.0.1:" + server.getPort() + "/v2/handoff/key/consume");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // Missing nonce
        String jsonPayload = "{\"handoffId\":\"some-id\"}";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        assertEquals(400, responseCode);
    }
}
