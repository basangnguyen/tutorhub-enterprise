package com.mycompany.tutorhub_enterprise.server.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class V2AnswerPayloadContractValidator {

    public V2AnswerPayloadContractValidationResult validate(String payloadJson) {
        V2AnswerPayloadContractValidationResult result = new V2AnswerPayloadContractValidationResult();
        List<String> blockingReasons = new ArrayList<>();
        
        if (!V2SubmitFeatureFlags.isAnswerPayloadContractEnabled()) {
            result.setSuccess(false);
            result.setValid(false);
            result.setValidationStatus(V2AnswerPayloadContract.STATUS_NOT_READY);
            blockingReasons.add("Answer payload contract validation is disabled");
            result.setBlockingReasons(blockingReasons);
            return result;
        }

        if (payloadJson == null || payloadJson.trim().isEmpty()) {
            return buildErrorResult(V2AnswerPayloadContract.ERROR_MALFORMED, "Payload JSON is empty");
        }

        JsonObject root;
        try {
            JsonElement element = JsonParser.parseString(payloadJson);
            if (!element.isJsonObject()) {
                return buildErrorResult(V2AnswerPayloadContract.ERROR_MALFORMED, "Payload is not a JSON object");
            }
            root = element.getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return buildErrorResult(V2AnswerPayloadContract.ERROR_MALFORMED, "Malformed JSON syntax");
        }

        // Validate payloadVersion
        if (!root.has("payloadVersion") || root.get("payloadVersion").isJsonNull()) {
            return buildErrorResult(V2AnswerPayloadContract.ERROR_MISSING_REQUIRED_FIELD, "Missing payloadVersion");
        }
        String payloadVersion = root.get("payloadVersion").getAsString();
        result.setPayloadVersion(payloadVersion);

        if (!V2AnswerPayloadContract.V1_PAYLOAD_VERSION.equals(payloadVersion)) {
            return buildErrorResult(V2AnswerPayloadContract.ERROR_UNSUPPORTED_VERSION, "Unsupported payloadVersion: " + payloadVersion);
        }

        // Validate attemptId
        if (!root.has("attemptId") || root.get("attemptId").isJsonNull()) {
            return buildErrorResult(V2AnswerPayloadContract.ERROR_MISSING_REQUIRED_FIELD, "Missing attemptId");
        }
        result.setAttemptId(root.get("attemptId").getAsString());

        // Validate paperId
        if (!root.has("paperId") || root.get("paperId").isJsonNull()) {
            return buildErrorResult(V2AnswerPayloadContract.ERROR_MISSING_REQUIRED_FIELD, "Missing paperId");
        }
        try {
            result.setPaperId(root.get("paperId").getAsInt());
        } catch (NumberFormatException | UnsupportedOperationException e) {
            return buildErrorResult(V2AnswerPayloadContract.ERROR_MALFORMED, "paperId is not a valid integer");
        }

        // Validate answers array
        if (!root.has("answers") || root.get("answers").isJsonNull()) {
            return buildErrorResult(V2AnswerPayloadContract.ERROR_MISSING_REQUIRED_FIELD, "Missing answers");
        }
        
        JsonElement answersElement = root.get("answers");
        if (!answersElement.isJsonArray()) {
            return buildErrorResult(V2AnswerPayloadContract.ERROR_MALFORMED, "answers is not a JSON array");
        }

        JsonArray answersArray = answersElement.getAsJsonArray();
        Set<Long> seenQuestionIds = new HashSet<>();
        
        for (JsonElement itemElement : answersArray) {
            if (!itemElement.isJsonObject()) {
                return buildErrorResult(V2AnswerPayloadContract.ERROR_MALFORMED, "Answer item is not an object");
            }
            JsonObject item = itemElement.getAsJsonObject();
            
            if (!item.has("questionId") || item.get("questionId").isJsonNull()) {
                return buildErrorResult(V2AnswerPayloadContract.ERROR_MISSING_REQUIRED_FIELD, "Missing questionId in answer item");
            }
            if (!item.has("selectedOptionId") || item.get("selectedOptionId").isJsonNull()) {
                return buildErrorResult(V2AnswerPayloadContract.ERROR_MISSING_REQUIRED_FIELD, "Missing selectedOptionId in answer item");
            }

            long questionId;
            try {
                questionId = item.get("questionId").getAsLong();
                // selectedOptionId validation without storing it in result
                item.get("selectedOptionId").getAsLong(); 
            } catch (NumberFormatException | UnsupportedOperationException e) {
                return buildErrorResult(V2AnswerPayloadContract.ERROR_MALFORMED, "questionId or selectedOptionId is not a valid number");
            }

            if (!seenQuestionIds.add(questionId)) {
                return buildErrorResult(V2AnswerPayloadContract.ERROR_DUPLICATE_QUESTION, "Duplicate questionId found: " + questionId);
            }
        }

        result.setAnswerCount(seenQuestionIds.size());
        result.setSuccess(true);
        result.setValid(true);
        result.setValidationStatus(V2AnswerPayloadContract.STATUS_VALID);
        return result;
    }

    private V2AnswerPayloadContractValidationResult buildErrorResult(String errorCode, String reason) {
        V2AnswerPayloadContractValidationResult result = new V2AnswerPayloadContractValidationResult();
        result.setSuccess(false);
        result.setValid(false);
        result.setErrorCode(errorCode);
        result.setValidationStatus(V2AnswerPayloadContract.STATUS_NOT_READY);
        
        List<String> blocking = new ArrayList<>();
        blocking.add(reason);
        result.setBlockingReasons(blocking);
        
        return result;
    }
}
