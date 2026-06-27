package com.mycompany.tutorhub_enterprise;

import com.mycompany.tutorhub_enterprise.client.services.V2RuntimeKeyRegistry;
import com.mycompany.tutorhub_enterprise.client.exam.utils.CryptoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2RuntimeKeyRegistryTest {

    @BeforeEach
    public void setup() {
        V2RuntimeKeyRegistry.clearForTest();
    }

    @Test
    public void testRegisterAndConsumeSuccess() throws Exception {
        String key = CryptoUtils.generateAESKey();
        String handoffId = V2RuntimeKeyRegistry.registerKey(key, Duration.ofMinutes(5), "TEST");
        
        assertNotNull(handoffId);
        assertTrue(V2RuntimeKeyRegistry.hasKey(handoffId));
        assertEquals(1, V2RuntimeKeyRegistry.size());

        Optional<String> retrieved = V2RuntimeKeyRegistry.consumeKey(handoffId);
        assertTrue(retrieved.isPresent());
        assertEquals(key, retrieved.get());
    }

    @Test
    public void testConsumeConsumesOnlyOnce() throws Exception {
        String key = CryptoUtils.generateAESKey();
        String handoffId = V2RuntimeKeyRegistry.registerKey(key, Duration.ofMinutes(5), "TEST");
        
        Optional<String> retrieved1 = V2RuntimeKeyRegistry.consumeKey(handoffId);
        assertTrue(retrieved1.isPresent());

        // Second consume should fail
        Optional<String> retrieved2 = V2RuntimeKeyRegistry.consumeKey(handoffId);
        assertFalse(retrieved2.isPresent());
        assertFalse(V2RuntimeKeyRegistry.hasKey(handoffId));
        assertEquals(0, V2RuntimeKeyRegistry.size());
    }

    @Test
    public void testTtlExpired() throws Exception {
        String key = CryptoUtils.generateAESKey();
        // Set negative TTL so it expires immediately
        String handoffId = V2RuntimeKeyRegistry.registerKey(key, Duration.ofMillis(-1), "TEST");
        
        assertFalse(V2RuntimeKeyRegistry.hasKey(handoffId));
        Optional<String> retrieved = V2RuntimeKeyRegistry.consumeKey(handoffId);
        assertFalse(retrieved.isPresent());
    }

    @Test
    public void testPurgeExpired() throws Exception {
        String key = CryptoUtils.generateAESKey();
        V2RuntimeKeyRegistry.registerKey(key, Duration.ofMillis(-1), "TEST");
        V2RuntimeKeyRegistry.registerKey(key, Duration.ofMinutes(5), "TEST2");

        // size() auto calls purgeExpired()
        assertEquals(1, V2RuntimeKeyRegistry.size());
    }
}
