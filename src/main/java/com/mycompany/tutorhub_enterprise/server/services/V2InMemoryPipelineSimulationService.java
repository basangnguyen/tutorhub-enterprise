package com.mycompany.tutorhub_enterprise.server.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

public class V2InMemoryPipelineSimulationService {

    private final V2ManualCandidateFullChainDryRunGateService dryRunGateService;
    private final V2JsonAnswerPayloadParser payloadParser;
    private final V2DatabaseAnswerKeyResolver answerKeyResolver;

    public V2InMemoryPipelineSimulationService() {
        this.dryRunGateService = new V2ManualCandidateFullChainDryRunGateService();
        this.payloadParser = new V2JsonAnswerPayloadParser();
        this.answerKeyResolver = new V2DatabaseAnswerKeyResolver();
    }

    public V2InMemoryPipelineSimulationService(
            V2ManualCandidateFullChainDryRunGateService dryRunGateService,
            V2JsonAnswerPayloadParser payloadParser,
            V2DatabaseAnswerKeyResolver answerKeyResolver) {
        this.dryRunGateService = dryRunGateService;
        this.payloadParser = payloadParser;
        this.answerKeyResolver = answerKeyResolver;
    }

    public V2InMemoryPipelineSimulationResult simulate(int userId, String attemptId, String payloadJson) {
        V2InMemoryPipelineSimulationResult result = new V2InMemoryPipelineSimulationResult();
        result.setUserId(userId);
        result.setAttemptId(attemptId);
        result.setCheckedAt(LocalDateTime.now());
        result.setWarnings(new ArrayList<>());
        result.setBlockingReasons(new ArrayList<>());
        result.setPlannedSteps(new ArrayList<>());
        result.setReady(false);
        result.setSimulated(false);
        result.setSuccess(true);
        result.setSimulationStatus("NOT_READY");

        if (!V2SubmitFeatureFlags.isInMemoryPipelineSimulationEnabled()) {
            result.getBlockingReasons().add("tse.v2.inMemoryPipelineSimulation.enabled is false");
            return result;
        }

        // 1. Full-chain dry-run gate check
        V2ManualCandidateFullChainDryRunGateResult gateResult = dryRunGateService.checkGate(userId, attemptId, payloadJson);
        if (gateResult == null || !gateResult.isReady()) {
            result.getBlockingReasons().add("DryRunGate not ready: " + (gateResult != null ? gateResult.getBlockingReasons() : "null"));
            return result;
        }

        result.setExamId(gateResult.getExamId());
        result.setPaperId(gateResult.getPaperId());
        result.setAnswerCount(gateResult.getAnswerCount());
        result.setReady(true);

        result.getPlannedSteps().add("PAYLOAD_CONTRACT_VALIDATE");

        // 2. Parse payload internally
        Map<Long, Long> answers = payloadParser.extractAnswers(payloadJson);
        if (answers == null) {
            result.getBlockingReasons().add("Payload parsing failed or returned null");
            result.setSimulationStatus("SIMULATION_FAILED");
            return result;
        }
        result.getPlannedSteps().add("PAYLOAD_PARSE");

        // 3. Resolve Answer Key
        Map<Long, Long> answerKey = answerKeyResolver.resolveCorrectOptionIds(result.getPaperId());
        if (answerKey == null) {
            result.getBlockingReasons().add("Answer key resolving failed or returned null");
            result.setSimulationStatus("SIMULATION_FAILED");
            return result;
        }
        result.setQuestionCount(answerKey.size());
        result.getPlannedSteps().add("ANSWER_KEY_RESOLVE");

        // 4. Compute aggregate internally
        V2InMemoryScoreDraftCalculator.InternalAggregateResult computeResult = 
            V2InMemoryScoreDraftCalculator.calculateInternal(answers, answerKey, answerKey.size());
        
        if (computeResult == null) {
            result.getBlockingReasons().add("Internal score draft computation failed");
            result.setSimulationStatus("SIMULATION_FAILED");
            return result;
        }
        result.getPlannedSteps().add("SCORE_DRAFT_COMPUTE_IN_MEMORY");

        // 5. Add remaining planned steps (without actually calling write actions)
        result.getPlannedSteps().add("SUBMIT_RECORD_CREATE_PLANNED");
        result.getPlannedSteps().add("RESULT_PUBLICATION_WRITE_PLANNED");
        result.getPlannedSteps().add("FINAL_STATUS_EXECUTION_PLANNED");

        result.setPlannedStepCount(result.getPlannedSteps().size());
        result.setSimulated(true);
        result.setSimulationStatus("IN_MEMORY_PIPELINE_SIMULATION_READY");

        return result;
    }
}
