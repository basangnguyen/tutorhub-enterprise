package com.mycompany.tutorhub_enterprise.client.services;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class V2RuntimeKeyRegistry {
    
    private static class KeyEntry {
        public final String key;
        public final Instant expiresAt;
        public final String purpose;

        public KeyEntry(String key, Instant expiresAt, String purpose) {
            this.key = key;
            this.expiresAt = expiresAt;
            this.purpose = purpose;
        }
    }

    private static final Map<String, KeyEntry> registry = new ConcurrentHashMap<>();

    public static String registerKey(String key, Duration ttl, String purpose) {
        String handoffId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(ttl);
        registry.put(handoffId, new KeyEntry(key, expiresAt, purpose));
        return handoffId;
    }

    public static Optional<String> consumeKey(String handoffId) {
        if (handoffId == null) return Optional.empty();
        KeyEntry entry = registry.remove(handoffId); // removes it
        if (entry != null && Instant.now().isBefore(entry.expiresAt)) {
            return Optional.of(entry.key);
        }
        return Optional.empty();
    }

    public static boolean hasKey(String handoffId) {
        if (handoffId == null) return false;
        KeyEntry entry = registry.get(handoffId);
        return entry != null && Instant.now().isBefore(entry.expiresAt);
    }

    public static void purgeExpired() {
        Instant now = Instant.now();
        registry.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt));
    }

    public static int size() {
        purgeExpired();
        return registry.size();
    }
    
    // reset for tests
    public static void clearForTest() {
        registry.clear();
    }
}
