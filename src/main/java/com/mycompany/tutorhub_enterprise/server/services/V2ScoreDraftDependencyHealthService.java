package com.mycompany.tutorhub_enterprise.server.services;

import java.util.Map;

public class V2ScoreDraftDependencyHealthService {

    private final V2AnswerKeyResolver answerKeyResolver;
    private final V2AnswerPayloadParser payloadParser;

    public V2ScoreDraftDependencyHealthService() {
        this.answerKeyResolver = new V2DatabaseAnswerKeyResolver();
        this.payloadParser = new V2JsonAnswerPayloadParser();
    }

    public V2ScoreDraftDependencyHealthService(V2AnswerKeyResolver answerKeyResolver, V2AnswerPayloadParser payloadParser) {
        this.answerKeyResolver = answerKeyResolver;
        this.payloadParser = payloadParser;
    }

    public V2ScoreDraftDependencyHealthResult checkHealth(int userId, String attemptId) {
        V2ScoreDraftDependencyHealthResult result = new V2ScoreDraftDependencyHealthResult();
        result.setUserId(userId);
        result.setAttemptId(attemptId);
        result.setCheckedAt(System.currentTimeMillis());

        if (!V2SubmitFeatureFlags.isScoreDraftDependencyHealthEnabled()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("ERROR_FEATURE_DISABLED");
            result.setHealthStatus("NOT_READY");
            result.addBlockingReason("Dependency health feature flag is disabled.");
            return result;
        }

        boolean parserAvailable = false;
        boolean resolverAvailable = false;

        if (payloadParser != null) {
            // We pass a valid canonical payload snippet to probe availability.
            String probeJson = "{\"payloadVersion\":\"" + V2AnswerPayloadContract.V1_PAYLOAD_VERSION + 
                               "\",\"attemptId\":\"dummy\",\"paperId\":0,\"answers\":[]}";
            Map<Long, Long> probe = payloadParser.extractAnswers(probeJson);
            parserAvailable = (probe != null);
        }

        if (answerKeyResolver != null) {
            // Probe with dummy paperId 0
            Map<Long, Long> probe = answerKeyResolver.resolveCorrectOptionIds(0);
            resolverAvailable = (probe != null);
        }

        result.setPayloadParserAvailable(parserAvailable);
        result.setAnswerKeyResolverAvailable(resolverAvailable);
        result.setSchemaVerified(parserAvailable && resolverAvailable); // Placeholder logic for now

        if (!parserAvailable) {
            result.addBlockingReason("Payload parser is unavailable. Schema pending verification.");
        }
        if (!resolverAvailable) {
            result.addBlockingReason("Answer key resolver is unavailable.");
        }

        if (parserAvailable && resolverAvailable) {
            result.setSuccess(true);
            result.setReady(true);
            result.setHealthStatus("SCORE_DRAFT_DEPENDENCIES_READY");
        } else {
            result.setSuccess(true); // Call succeeded, but system not ready
            result.setReady(false);
            result.setHealthStatus("NOT_READY");
        }

        return result;
    }
}
