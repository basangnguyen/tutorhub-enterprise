package com.mycompany.tutorhub_enterprise.server.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.HashMap;
import java.util.Map;

/**
 * JSON Answer Payload Parser for V2 Manual Candidate Submit.
 * Parses the canonical payload schema when enabled.
 */
public class V2JsonAnswerPayloadParser implements V2AnswerPayloadParser {

    @Override
    public Map<Long, Long> extractAnswers(String payloadJson) {
        if (!V2SubmitFeatureFlags.isJsonAnswerPayloadParserEnabled()) {
            return null; // Unavailable sentinel
        }

        if (payloadJson == null || payloadJson.trim().isEmpty()) {
            return null;
        }

        try {
            JsonElement element = JsonParser.parseString(payloadJson);
            if (!element.isJsonObject()) {
                return null;
            }
            JsonObject root = element.getAsJsonObject();
            
            // Fast check on schema version
            if (!root.has("payloadVersion") || root.get("payloadVersion").isJsonNull()) {
                return null;
            }
            if (!V2AnswerPayloadContract.V1_PAYLOAD_VERSION.equals(root.get("payloadVersion").getAsString())) {
                return null;
            }
            
            if (!root.has("answers") || !root.get("answers").isJsonArray()) {
                return null;
            }
            
            JsonArray answersArray = root.getAsJsonArray("answers");
            Map<Long, Long> result = new HashMap<>();
            
            for (JsonElement itemElement : answersArray) {
                if (!itemElement.isJsonObject()) {
                    return null;
                }
                JsonObject item = itemElement.getAsJsonObject();
                if (!item.has("questionId") || item.get("questionId").isJsonNull() || 
                    !item.has("selectedOptionId") || item.get("selectedOptionId").isJsonNull()) {
                    return null;
                }
                
                long qId = item.get("questionId").getAsLong();
                long oId = item.get("selectedOptionId").getAsLong();
                
                if (result.containsKey(qId)) {
                    // Duplicate questionId
                    return null;
                }
                result.put(qId, oId);
            }
            
            return result;
        } catch (JsonSyntaxException | NumberFormatException | UnsupportedOperationException e) {
            // Malformed JSON or invalid numbers
            return null;
        }
    }
}
