package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.mycompany.tutorhub_enterprise.client.exam.utils.CryptoUtils;
import com.mycompany.tutorhub_enterprise.client.services.V2LoopbackKeyHandoffServer;
import com.mycompany.tutorhub_enterprise.client.services.V2RuntimeHandoffService;
import com.mycompany.tutorhub_enterprise.client.services.V2RuntimeKeyRegistry;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffBundle;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffQuestion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class TSEV2ChildDebugLoaderTest {

    private V2LoopbackKeyHandoffServer server;
    private int port;
    private String testKeyB64;
    private SecretKey testSecretKey;

    @BeforeEach
    public void setUp() throws Exception {
        V2RuntimeKeyRegistry.clearForTest();
        server = new V2LoopbackKeyHandoffServer();
        server.start();
        port = server.getPort();
        assertTrue(port > 0);
        testKeyB64 = CryptoUtils.generateAESKey();
        byte[] decodedKey = Base64.getDecoder().decode(testKeyB64);
        testSecretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        cleanupFiles();
    }

    @AfterEach
    public void tearDown() {
        if (server != null) {
            server.stop();
        }
        cleanupFiles();
    }

    private void cleanupFiles() {
        File dir = new File(System.getProperty("user.dir"), V2RuntimeHandoffService.RUNTIME_DIR);
        File encFile = new File(dir, V2RuntimeHandoffService.ENC_FILE_NAME);
        File metaFile = new File(dir, V2RuntimeHandoffService.META_FILE_NAME);
        if (encFile.exists()) encFile.delete();
        if (metaFile.exists()) metaFile.delete();
    }

    private V2ExamHandoffBundle createMockBundle() {
        V2ExamHandoffBundle bundle = new V2ExamHandoffBundle();
        bundle.examId = 123;
        bundle.paperId = 456;
        bundle.attemptId = "test-attempt";
        bundle.packageHash = "dummy-hash";
        bundle.deadlineAt = "2026-12-31T23:59:59Z";
        bundle.totalScore = 10.0f;
        bundle.questionCount = 1;
        bundle.sessionToken = "mock_token";
        bundle.questions = new ArrayList<>();
        
        V2ExamHandoffQuestion q = new V2ExamHandoffQuestion();
        q.questionId = 1;
        q.content = "<p>Test question</p>";
        bundle.questions.add(q);
        
        return bundle;
    }

    private TSEChildLaunchArgs createArgs() {
        File dir = new File(System.getProperty("user.dir"), V2RuntimeHandoffService.RUNTIME_DIR);
        File encFile = new File(dir, V2RuntimeHandoffService.ENC_FILE_NAME);
        File metaFile = new File(dir, V2RuntimeHandoffService.META_FILE_NAME);

        TSEChildLaunchArgs args = new TSEChildLaunchArgs();
        args.setMode(TSEChildLaunchArgs.Mode.V2_DEBUG);
        args.setV2HandoffMetaPath(metaFile.getAbsolutePath());
        args.setV2HandoffEncPath(encFile.getAbsolutePath());
        args.setV2DebugOnly(true);
        return args;
    }

    @Test
    public void testValidHandoffLoadsSuccessfully() throws Exception {
        V2ExamHandoffBundle bundle = createMockBundle();
        String handoffId = V2RuntimeKeyRegistry.registerKey(testKeyB64, Duration.ofMinutes(5), "TEST_DEBUG");
        String nonce = "nonce-1";
        server.registerExpectedNonce(handoffId, nonce);
        V2RuntimeHandoffService.createEncryptedRuntimeHandoff(bundle, testKeyB64, true, handoffId, nonce, "127.0.0.1", port);

        TSEChildLaunchArgs args = createArgs();
        TSEV2ChildDebugLoadResult result = TSEV2ChildDebugLoader.load(args);

        assertTrue(result.success, "Result should be success. Error: " + result.errorCode);
        assertNull(result.errorCode);
        assertEquals(123, result.examId);
        assertEquals(456, result.paperId);
        assertEquals("dummy-hash", result.packageHash);
        assertEquals(1, result.questionCount);
        assertEquals(10.0, result.totalScore);

        assertTrue(result.hashVerified);
        assertTrue(result.keyFetched);
        assertTrue(result.decrypted);
        assertTrue(result.parsed);
    }

    @Test
    public void testMissingMetaFails() {
        TSEChildLaunchArgs args = new TSEChildLaunchArgs();
        args.setV2HandoffMetaPath(null);
        args.setV2HandoffEncPath("dummy.enc");

        TSEV2ChildDebugLoadResult result = TSEV2ChildDebugLoader.load(args);
        assertFalse(result.success);
        assertEquals("ERROR_META_MISSING", result.errorCode);
    }

    @Test
    public void testConsumeTwiceFails() throws Exception {
        V2ExamHandoffBundle bundle = createMockBundle();
        String handoffId = V2RuntimeKeyRegistry.registerKey(testKeyB64, Duration.ofMinutes(5), "TEST_DEBUG");
        String nonce = "nonce-1";
        server.registerExpectedNonce(handoffId, nonce);
        V2RuntimeHandoffService.createEncryptedRuntimeHandoff(bundle, testKeyB64, true, handoffId, nonce, "127.0.0.1", port);

        TSEChildLaunchArgs args = createArgs();

        // First consume
        TSEV2ChildDebugLoadResult result1 = TSEV2ChildDebugLoader.load(args);
        assertTrue(result1.success);

        // Second consume should fail because key is deleted
        TSEV2ChildDebugLoadResult result2 = TSEV2ChildDebugLoader.load(args);
        assertFalse(result2.success);
        assertEquals("ERROR_KEY_FETCH_FAILED", result2.errorCode);
    }

    @Test
    public void testHashMismatchFails() throws Exception {
        V2ExamHandoffBundle bundle = createMockBundle();
        String handoffId = V2RuntimeKeyRegistry.registerKey(testKeyB64, Duration.ofMinutes(5), "TEST_DEBUG");
        String nonce = "nonce-1";
        server.registerExpectedNonce(handoffId, nonce);
        V2RuntimeHandoffService.createEncryptedRuntimeHandoff(bundle, testKeyB64, true, handoffId, nonce, "127.0.0.1", port);

        TSEChildLaunchArgs args = createArgs();
        
        // Tamper with the enc file
        Files.write(Paths.get(args.getV2HandoffEncPath()), "tampered".getBytes());

        TSEV2ChildDebugLoadResult result = TSEV2ChildDebugLoader.load(args);
        assertFalse(result.success);
        assertEquals("ERROR_HASH_MISMATCH", result.errorCode);
        assertTrue(result.keyFetched);
        assertFalse(result.hashVerified);
    }

    @Test
    public void testSecurityViolationFails() throws Exception {
        V2ExamHandoffBundle bundle = createMockBundle();
        // Add a field that will trigger security violation
        bundle.questions.get(0).content = "<p>Test</p><!-- isCorrect -->"; 
        
        String handoffId = V2RuntimeKeyRegistry.registerKey(testKeyB64, Duration.ofMinutes(5), "TEST_DEBUG");
        String nonce = "nonce-1";
        server.registerExpectedNonce(handoffId, nonce);
        
        // Disable strict save before encrypt by using the service carefully or modifying the JSON directly
        // The service might block it, so let's check if the service throws an exception:
        try {
            V2RuntimeHandoffService.createEncryptedRuntimeHandoff(bundle, testKeyB64, true, handoffId, nonce, "127.0.0.1", port);
            fail("Service should have blocked this");
        } catch (Exception e) {
            // Service blocks it, which is good. Let's do it manually.
            String json = new com.google.gson.Gson().toJson(bundle);
            String enc = CryptoUtils.encryptWrapper(json, testKeyB64);
            
            File dir = new File(System.getProperty("user.dir"), V2RuntimeHandoffService.RUNTIME_DIR);
            if (!dir.exists()) dir.mkdirs();
            File encFile = new File(dir, V2RuntimeHandoffService.ENC_FILE_NAME);
            Files.write(encFile.toPath(), enc.getBytes());
            
            // Just reuse a valid meta, but tamper hash won't work unless we fix the hash.
            // But we don't even get to parse if hash is wrong.
            // Let's create a meta manually to pass the hash check.
            String sha256 = hashString(enc);
            String metaStr = "{\"parentIpcPort\":" + port + ",\"handoffId\":\"" + handoffId + "\",\"nonce\":\"" + nonce + "\",\"encryptedFileSha256\":\"" + sha256 + "\"}";
            File metaFile = new File(dir, V2RuntimeHandoffService.META_FILE_NAME);
            Files.write(metaFile.toPath(), metaStr.getBytes());
        }

        TSEChildLaunchArgs args = createArgs();

        TSEV2ChildDebugLoadResult result = TSEV2ChildDebugLoader.load(args);
        assertFalse(result.success);
        assertEquals("ERROR_SECURITY_VIOLATION", result.errorCode);
        assertTrue(result.keyFetched);
        assertTrue(result.hashVerified);
        assertTrue(result.decrypted);
        assertFalse(result.parsed);
    }
    
    private static String hashString(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            java.util.Formatter formatter = new java.util.Formatter();
            for (byte b : hash) {
                formatter.format("%02x", b);
            }
            String result = formatter.toString();
            formatter.close();
            return result;
        } catch (Exception e) {
            return "hash_error";
        }
    }
}
