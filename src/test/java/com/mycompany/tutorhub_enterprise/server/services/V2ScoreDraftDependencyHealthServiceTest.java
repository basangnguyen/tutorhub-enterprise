package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class V2ScoreDraftDependencyHealthServiceTest {

    private FakeAnswerKeyResolver fakeResolver;
    private FakePayloadParser fakeParser;
    private V2ScoreDraftDependencyHealthService healthService;

    private static class FakeAnswerKeyResolver implements V2AnswerKeyResolver {
        public Map<Long, Long> returnMap = new HashMap<>();
        @Override
        public Map<Long, Long> resolveCorrectOptionIds(int paperId) {
            return returnMap;
        }
    }

    private static class FakePayloadParser implements V2AnswerPayloadParser {
        public Map<Long, Long> returnMap = new HashMap<>();
        @Override
        public Map<Long, Long> extractAnswers(String payloadJson) {
            return returnMap;
        }
    }

    @BeforeEach
    public void setup() {
        fakeResolver = new FakeAnswerKeyResolver();
        fakeParser = new FakePayloadParser();
        healthService = new V2ScoreDraftDependencyHealthService(fakeResolver, fakeParser);
        System.setProperty("tse.v2.scoreDraftDependencyHealth.enabled", "true");
    }

    @AfterEach
    public void teardown() {
        System.clearProperty("tse.v2.scoreDraftDependencyHealth.enabled");
    }

    @Test
    public void testFeatureFlagDisabled() {
        System.setProperty("tse.v2.scoreDraftDependencyHealth.enabled", "false");
        V2ScoreDraftDependencyHealthResult result = healthService.checkHealth(1, "attempt-1");
        assertFalse(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("ERROR_FEATURE_DISABLED", result.getErrorCode());
    }

    @Test
    public void testDependenciesUnavailable() {
        fakeResolver.returnMap = null;
        fakeParser.returnMap = null;
        V2ScoreDraftDependencyHealthResult result = healthService.checkHealth(1, "attempt-1");
        assertTrue(result.isSuccess());
        assertFalse(result.isReady());
        assertEquals("NOT_READY", result.getHealthStatus());
        assertFalse(result.isAnswerKeyResolverAvailable());
        assertFalse(result.isPayloadParserAvailable());
    }

    @Test
    public void testDependenciesAvailable() {
        fakeResolver.returnMap = new HashMap<>();
        fakeParser.returnMap = new HashMap<>();
        V2ScoreDraftDependencyHealthResult result = healthService.checkHealth(1, "attempt-1");
        assertTrue(result.isSuccess());
        assertTrue(result.isReady());
        assertEquals("SCORE_DRAFT_DEPENDENCIES_READY", result.getHealthStatus());
        assertTrue(result.isAnswerKeyResolverAvailable());
        assertTrue(result.isPayloadParserAvailable());
    }
}
