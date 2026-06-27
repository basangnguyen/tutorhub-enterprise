package com.mycompany.tutorhub_enterprise.server.services;

/**
 * Defines the canonical internal schema for V2 manual candidate payload.
 *
 * This is the canonical V2 manual candidate payload schema, not a claim that
 * older payloadJson already uses this shape.
 *
 * The schema matches TSEV2SubmitPayload:
 * {
 *   "payloadVersion": "TSE_V2_ANSWER_PAYLOAD_V1",
 *   "attemptId": "...",
 *   "paperId": 123,
 *   "answers": [
 *     {
 *       "questionId": 1,
 *       "selectedOptionId": 2
 *     }
 *   ]
 * }
 *
 * Option A is selected for unanswered questions: Unanswered questions do NOT
 * appear in the answers array.
 */
public class V2AnswerPayloadContract {
    public static final String V1_PAYLOAD_VERSION = "TSE_V2_ANSWER_PAYLOAD_V1";
    
    // Statuses
    public static final String STATUS_VALID = "V2_ANSWER_PAYLOAD_CONTRACT_VALID";
    public static final String STATUS_NOT_READY = "NOT_READY";
    
    // Errors
    public static final String ERROR_MALFORMED = "ERROR_V2_ANSWER_PAYLOAD_MALFORMED";
    public static final String ERROR_DUPLICATE_QUESTION = "ERROR_V2_ANSWER_PAYLOAD_DUPLICATE_QUESTION";
    public static final String ERROR_UNSUPPORTED_VERSION = "ERROR_V2_ANSWER_PAYLOAD_UNSUPPORTED_VERSION";
    public static final String ERROR_MISSING_REQUIRED_FIELD = "ERROR_V2_ANSWER_PAYLOAD_MISSING_REQUIRED_FIELD";
}
