package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class V2InMemoryPipelineSimulationServiceTest {

    private V2InMemoryPipelineSimulationService service;

    @BeforeEach
    public void setUp() {
        System.setProperty("tse.v2.inMemoryPipelineSimulation.enabled", "true");
        System.setProperty("tse.v2.manualCandidateFullChainDryRun.enabled", "true");
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.inMemoryPipelineSimulation.enabled");
        System.clearProperty("tse.v2.manualCandidateFullChainDryRun.enabled");
    }

    @Test
    public void testSimulate_FlagDisabled() {
        System.setProperty("tse.v2.inMemoryPipelineSimulation.enabled", "false");
        service = new V2InMemoryPipelineSimulationService(null, null, null);
        V2InMemoryPipelineSimulationResult result = service.simulate(1, "ATT-123", "{}");

        assertFalse(result.isSimulated());
        assertFalse(result.isReady());
        assertTrue(result.getBlockingReasons().contains("tse.v2.inMemoryPipelineSimulation.enabled is false"));
    }

    @Test
    public void testSimulate_GateNotReady() {
        V2ManualCandidateFullChainDryRunGateService mockGate = new V2ManualCandidateFullChainDryRunGateService(null, null, null, null) {
            @Override
            public V2ManualCandidateFullChainDryRunGateResult checkGate(int userId, String attemptId, String payloadJson) {
                V2ManualCandidateFullChainDryRunGateResult res = new V2ManualCandidateFullChainDryRunGateResult();
                res.setReady(false);
                res.setBlockingReasons(new ArrayList<>());
                return res;
            }
        };

        service = new V2InMemoryPipelineSimulationService(mockGate, null, null);
        V2InMemoryPipelineSimulationResult result = service.simulate(1, "ATT-123", "{}");

        assertFalse(result.isSimulated());
        assertFalse(result.isReady());
    }

    @Test
    public void testSimulate_SuccessfulSimulation() {
        V2ManualCandidateFullChainDryRunGateService mockGate = new V2ManualCandidateFullChainDryRunGateService(null, null, null, null) {
            @Override
            public V2ManualCandidateFullChainDryRunGateResult checkGate(int userId, String attemptId, String payloadJson) {
                V2ManualCandidateFullChainDryRunGateResult res = new V2ManualCandidateFullChainDryRunGateResult();
                res.setReady(true);
                res.setPaperId(55);
                res.setExamId(66);
                res.setAnswerCount(2);
                res.setBlockingReasons(new ArrayList<>());
                return res;
            }
        };

        V2JsonAnswerPayloadParser mockParser = new V2JsonAnswerPayloadParser() {
            @Override
            public Map<Long, Long> extractAnswers(String payloadJson) {
                Map<Long, Long> map = new HashMap<>();
                map.put(1L, 101L);
                map.put(2L, 201L);
                return map;
            }
        };

        V2DatabaseAnswerKeyResolver mockResolver = new V2DatabaseAnswerKeyResolver() {
            @Override
            public Map<Long, Long> resolveCorrectOptionIds(int paperId) {
                Map<Long, Long> map = new HashMap<>();
                map.put(1L, 101L);
                map.put(2L, 202L); // 1 wrong
                return map;
            }
        };

        service = new V2InMemoryPipelineSimulationService(mockGate, mockParser, mockResolver);
        V2InMemoryPipelineSimulationResult result = service.simulate(1, "ATT-123", "{}");

        assertTrue(result.isReady());
        assertTrue(result.isSimulated());
        assertEquals("IN_MEMORY_PIPELINE_SIMULATION_READY", result.getSimulationStatus());
        assertEquals(7, result.getPlannedStepCount());
        assertTrue(result.getPlannedSteps().contains("PAYLOAD_CONTRACT_VALIDATE"));
        assertTrue(result.getPlannedSteps().contains("PAYLOAD_PARSE"));
        assertTrue(result.getPlannedSteps().contains("ANSWER_KEY_RESOLVE"));
        assertTrue(result.getPlannedSteps().contains("SCORE_DRAFT_COMPUTE_IN_MEMORY"));
        assertTrue(result.getPlannedSteps().contains("SUBMIT_RECORD_CREATE_PLANNED"));
        assertTrue(result.getPlannedSteps().contains("RESULT_PUBLICATION_WRITE_PLANNED"));
        assertTrue(result.getPlannedSteps().contains("FINAL_STATUS_EXECUTION_PLANNED"));
        assertEquals(2, result.getQuestionCount());
    }
}
