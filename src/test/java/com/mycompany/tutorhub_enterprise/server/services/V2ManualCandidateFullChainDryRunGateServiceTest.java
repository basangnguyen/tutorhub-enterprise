package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class V2ManualCandidateFullChainDryRunGateServiceTest {

    private V2ManualCandidateFullChainDryRunGateService service;

    @BeforeEach
    public void setUp() {
        System.setProperty("tse.v2.manualCandidateFullChainDryRun.enabled", "true");
        System.setProperty("tse.v2.jsonAnswerPayloadParser.enabled", "true");
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.manualCandidateFullChainDryRun.enabled");
        System.clearProperty("tse.v2.jsonAnswerPayloadParser.enabled");
    }

    @Test
    public void testCheckGate_FlagDisabled() {
        System.setProperty("tse.v2.manualCandidateFullChainDryRun.enabled", "false");
        service = new V2ManualCandidateFullChainDryRunGateService(null, null, null, null);
        V2ManualCandidateFullChainDryRunGateResult result = service.checkGate(1, "ATT-123", "{}");

        assertFalse(result.isReady());
        assertTrue(result.getBlockingReasons().contains("tse.v2.manualCandidateFullChainDryRun.enabled is false"));
    }

    @Test
    public void testCheckGate_ManualCheckNotReady() {
        V2ManualCandidateSubmitCheckService mockCheck = new V2ManualCandidateSubmitCheckService() {
            @Override
            public V2ManualCandidateSubmitCheckResult checkCandidateSubmit(int userId, String attemptId) {
                V2ManualCandidateSubmitCheckResult res = new V2ManualCandidateSubmitCheckResult();
                res.setReady(false);
                res.setBlockingReasons(new ArrayList<>());
                return res;
            }
        };

        service = new V2ManualCandidateFullChainDryRunGateService(mockCheck, null, null, null);
        V2ManualCandidateFullChainDryRunGateResult result = service.checkGate(1, "ATT-123", "{}");

        assertFalse(result.isReady());
        assertTrue(result.getBlockingReasons().contains("ManualCandidateSubmitCheck not ready"));
    }

    @Test
    public void testCheckGate_ValidPayloadAndDependencies() {
        V2ManualCandidateSubmitCheckService mockCheck = new V2ManualCandidateSubmitCheckService() {
            @Override
            public V2ManualCandidateSubmitCheckResult checkCandidateSubmit(int userId, String attemptId) {
                V2ManualCandidateSubmitCheckResult res = new V2ManualCandidateSubmitCheckResult();
                res.setReady(true);
                res.setExamId(10);
                res.setPaperId(20);
                res.setBlockingReasons(new ArrayList<>());
                return res;
            }
        };

        V2CandidateSubmitOrchestratorGateService mockOrch = new V2CandidateSubmitOrchestratorGateService() {
            @Override
            public V2CandidateSubmitOrchestratorGateResult checkGate(int userId, String attemptId) {
                V2CandidateSubmitOrchestratorGateResult res = new V2CandidateSubmitOrchestratorGateResult();
                res.setReady(true);
                res.setBlockingReasons(new ArrayList<>());
                return res;
            }
        };

        V2AnswerPayloadContractValidator mockValidator = new V2AnswerPayloadContractValidator() {
            @Override
            public V2AnswerPayloadContractValidationResult validate(String payloadJson) {
                V2AnswerPayloadContractValidationResult res = new V2AnswerPayloadContractValidationResult();
                res.setValid(true);
                res.setAnswerCount(0);
                return res;
            }
        };

        V2ScoreDraftDependencyHealthService mockHealth = new V2ScoreDraftDependencyHealthService() {
            @Override
            public V2ScoreDraftDependencyHealthResult checkHealth(int userId, String attemptId) {
                V2ScoreDraftDependencyHealthResult res = new V2ScoreDraftDependencyHealthResult();
                res.setReady(true);
                return res;
            }
        };

        service = new V2ManualCandidateFullChainDryRunGateService(mockCheck, mockOrch, mockValidator, mockHealth);
        V2ManualCandidateFullChainDryRunGateResult result = service.checkGate(1, "ATT-123", "{}");

        // Assuming DB check passes because conn is null in test without mocking DB
        assertTrue(result.isReady());
        assertEquals("READY_FOR_IN_MEMORY_PIPELINE_SIMULATION", result.getDryRunGateStatus());
    }
}
