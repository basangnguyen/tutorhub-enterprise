package com.mycompany.tutorhub_enterprise.client.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mycompany.tutorhub_enterprise.client.exam.ui.TSEV2SubmitPayload;

public class TSEV2ServerNoGradingSubmitBridgeService {

    public static final String ACTION = "EXAM_SUBMIT_V2_ORCHESTRATOR_NO_GRADING";
    public static final String FEATURE_FLAG = "tse.v2.clientServerNoGradingSubmit.enabled";

    private final TSEV2ServerSubmitTransport transport;
    private final Gson gson;

    public TSEV2ServerNoGradingSubmitBridgeService(TSEV2ServerSubmitTransport transport) {
        this.transport = transport;
        this.gson = new GsonBuilder().create();
    }

    public TSEV2ServerNoGradingSubmitBridgeResult submitNoGrading(TSEV2SubmitPayload payload) {
        TSEV2ServerNoGradingSubmitBridgeResult result = new TSEV2ServerNoGradingSubmitBridgeResult();

        boolean enabled = com.mycompany.tutorhub_enterprise.server.services.V2SubmitFeatureFlags.isClientServerNoGradingSubmitEnabled();
        if (!enabled) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_FEATURE_DISABLED");
            return result;
        }

        if (payload == null) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_INVALID_PAYLOAD");
            return result;
        }

        try {
            String payloadJson = gson.toJson(payload);
            String responseJson = transport.send(ACTION, payloadJson);

            if (responseJson == null || responseJson.trim().isEmpty()) {
                result.setSuccess(false);
                result.setErrorCode("ERROR_EMPTY_RESPONSE");
                return result;
            }

            if (containsUnsafeResponseMarker(responseJson)) {
                result.setSuccess(false);
                result.setErrorCode("ERROR_UNSAFE_RESPONSE_REJECTED");
                return result;
            }

            JsonObject jsonObj = unwrapResponse(JsonParser.parseString(responseJson).getAsJsonObject());
            
            if (jsonObj.has("status") && "ERROR".equals(jsonObj.get("status").getAsString())) {
                result.setSuccess(false);
                result.setErrorCode(jsonObj.has("errorCode") ? jsonObj.get("errorCode").getAsString() : "UNKNOWN_ERROR");
                return result;
            }
            
            if (jsonObj.has("success") && jsonObj.get("success").getAsBoolean()) {
                result.setSuccess(true);
                if (jsonObj.has("closureDraftId")) result.setClosureDraftId(jsonObj.get("closureDraftId").getAsLong());
                if (jsonObj.has("ledgerId")) result.setLedgerId(jsonObj.get("ledgerId").getAsLong());
                if (jsonObj.has("submitRecordId")) result.setSubmitRecordId(jsonObj.get("submitRecordId").getAsLong());
                if (jsonObj.has("payloadHash")) result.setPayloadHash(jsonObj.get("payloadHash").getAsString());
                if (jsonObj.has("closureStatus")) {
                    result.setClosureStatus(jsonObj.get("closureStatus").getAsString());
                    result.setFinalStatus(jsonObj.get("closureStatus").getAsString());
                }
                if (jsonObj.has("finalStatus")) {
                    result.setFinalStatus(jsonObj.get("finalStatus").getAsString());
                    result.setClosureStatus(jsonObj.get("finalStatus").getAsString());
                }
                if (jsonObj.has("idempotent")) result.setIdempotent(jsonObj.get("idempotent").getAsBoolean());
                return result;
            } else {
                result.setSuccess(false);
                result.setErrorCode(jsonObj.has("errorCode") ? jsonObj.get("errorCode").getAsString() : "UNKNOWN_SERVER_ERROR");
                return result;
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorCode("ERROR_BRIDGE_EXCEPTION");
            return result;
        }
    }

    private static boolean containsUnsafeResponseMarker(String responseJson) {
        String lowerResponse = responseJson.toLowerCase();
        return lowerResponse.contains("answers")
                || lowerResponse.contains("selectedoptionid")
                || lowerResponse.contains("answerkey")
                || lowerResponse.contains("iscorrect")
                || lowerResponse.contains("correctoption")
                || lowerResponse.contains("score")
                || lowerResponse.contains("gradingresult")
                || lowerResponse.contains("sessiontoken")
                || lowerResponse.contains("keyb64")
                || lowerResponse.contains("plaintext");
    }

    private static JsonObject unwrapResponse(JsonObject jsonObj) {
        if (jsonObj.has("data")) {
            if (jsonObj.get("data").isJsonObject()) {
                return jsonObj.getAsJsonObject("data");
            }
            if (jsonObj.get("data").isJsonPrimitive()
                    && jsonObj.get("data").getAsJsonPrimitive().isString()) {
                try {
                    return JsonParser.parseString(jsonObj.get("data").getAsString()).getAsJsonObject();
                } catch (Exception ignored) {
                    return jsonObj;
                }
            }
        }
        return jsonObj;
    }
}
