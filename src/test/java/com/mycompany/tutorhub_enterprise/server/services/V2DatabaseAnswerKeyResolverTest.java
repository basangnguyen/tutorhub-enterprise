package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class V2DatabaseAnswerKeyResolverTest {

    private V2DatabaseAnswerKeyResolver resolver;

    @BeforeEach
    public void setup() {
        resolver = new V2DatabaseAnswerKeyResolver();
    }

    @AfterEach
    public void teardown() {
        System.clearProperty("tse.v2.databaseAnswerKeyResolver.enabled");
    }

    @Test
    public void testUnavailableWhenFlagDisabled() {
        System.setProperty("tse.v2.databaseAnswerKeyResolver.enabled", "false");
        Map<Long, Long> result = resolver.resolveCorrectOptionIds(1);
        assertNull(result, "Resolver should return null when disabled");
    }

    @Test
    public void testDatabaseResolverOnlySelectsAndFailsGracefullyWithoutDB() {
        System.setProperty("tse.v2.databaseAnswerKeyResolver.enabled", "true");
        // Since there is no database running in this unit test context,
        // it should catch the SQLException and return null gracefully instead of crashing.
        Map<Long, Long> result = resolver.resolveCorrectOptionIds(1);
        assertTrue(result == null || result.isEmpty(), "Resolver should return null gracefully if DB connection fails, or empty map if test DB is empty");
    }
}
