package com.mycompany.tutorhub_enterprise.server.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class V2ManualCandidateFullChainDryRunGateService {

    private final V2ManualCandidateSubmitCheckService manualCandidateCheckService;
    private final V2CandidateSubmitOrchestratorGateService orchestratorGateService;
    private final V2AnswerPayloadContractValidator contractValidator;
    private final V2ScoreDraftDependencyHealthService dependencyHealthService;

    public V2ManualCandidateFullChainDryRunGateService() {
        this.manualCandidateCheckService = new V2ManualCandidateSubmitCheckService();
        this.orchestratorGateService = new V2CandidateSubmitOrchestratorGateService();
        this.contractValidator = new V2AnswerPayloadContractValidator();
        this.dependencyHealthService = new V2ScoreDraftDependencyHealthService();
    }

    public V2ManualCandidateFullChainDryRunGateService(
            V2ManualCandidateSubmitCheckService manualCandidateCheckService,
            V2CandidateSubmitOrchestratorGateService orchestratorGateService,
            V2AnswerPayloadContractValidator contractValidator,
            V2ScoreDraftDependencyHealthService dependencyHealthService) {
        this.manualCandidateCheckService = manualCandidateCheckService;
        this.orchestratorGateService = orchestratorGateService;
        this.contractValidator = contractValidator;
        this.dependencyHealthService = dependencyHealthService;
    }

    public V2ManualCandidateFullChainDryRunGateResult checkGate(int userId, String attemptId, String payloadJson) {
        V2ManualCandidateFullChainDryRunGateResult result = new V2ManualCandidateFullChainDryRunGateResult();
        result.setUserId(userId);
        result.setAttemptId(attemptId);
        result.setCheckedAt(LocalDateTime.now());
        result.setWarnings(new ArrayList<>());
        result.setBlockingReasons(new ArrayList<>());
        result.setReady(false);
        result.setSuccess(true);
        result.setDryRunGateStatus("NOT_READY");

        if (!V2SubmitFeatureFlags.isManualCandidateFullChainDryRunEnabled()) {
            result.getBlockingReasons().add("tse.v2.manualCandidateFullChainDryRun.enabled is false");
            return result;
        }

        // 1. Manual candidate submit check
        V2ManualCandidateSubmitCheckResult manualCheck = manualCandidateCheckService.checkCandidateSubmit(userId, attemptId);
        if (manualCheck == null || !manualCheck.isReady()) {
            result.getBlockingReasons().add("ManualCandidateSubmitCheck not ready");
            return result;
        }
        result.setExamId(manualCheck.getExamId());
        result.setPaperId(manualCheck.getPaperId());

        // 2. Orchestrator gate check
        V2CandidateSubmitOrchestratorGateResult orchCheck = orchestratorGateService.checkGate(userId, attemptId);
        if (orchCheck == null || !orchCheck.isReady()) {
            result.getBlockingReasons().add("CandidateSubmitOrchestratorGate not ready");
            return result;
        }

        // 3. Payload contract valid
        V2AnswerPayloadContractValidationResult contractCheck = contractValidator.validate(payloadJson);
        if (contractCheck == null || !contractCheck.isValid()) {
            result.getBlockingReasons().add("AnswerPayloadContract invalid: " + (contractCheck != null ? contractCheck.getErrorCode() : "null"));
            return result;
        }
        result.setAnswerCount(contractCheck.getAnswerCount() != null ? contractCheck.getAnswerCount() : 0);

        // 4. Parser enabled
        if (!V2SubmitFeatureFlags.isJsonAnswerPayloadParserEnabled()) {
            result.getBlockingReasons().add("tse.v2.jsonAnswerPayloadParser.enabled is false");
            return result;
        }

        // 5. Dependency health (Parser + Resolver)
        V2ScoreDraftDependencyHealthResult healthCheck = dependencyHealthService.checkHealth(userId, attemptId);
        if (healthCheck == null || !healthCheck.isReady()) {
            result.getBlockingReasons().add("ScoreDraftDependencies not ready");
            return result;
        }

        result.setReady(true);
        result.setDryRunGateStatus("READY_FOR_IN_MEMORY_PIPELINE_SIMULATION");
        return result;
    }
}
