package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class V2AnswerPayloadContractValidatorTest {

    private V2AnswerPayloadContractValidator validator;

    @BeforeEach
    public void setUp() {
        validator = new V2AnswerPayloadContractValidator();
        System.setProperty("tse.v2.answerPayloadContract.enabled", "true");
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.answerPayloadContract.enabled");
    }

    @Test
    public void testFlagOffReturnsNotReady() {
        System.setProperty("tse.v2.answerPayloadContract.enabled", "false");
        V2AnswerPayloadContractValidationResult result = validator.validate("{\"payloadVersion\":\"TSE_V2_ANSWER_PAYLOAD_V1\",\"attemptId\":\"123\",\"paperId\":1,\"answers\":[]}");
        assertFalse(result.isSuccess());
        assertFalse(result.isValid());
        assertEquals(V2AnswerPayloadContract.STATUS_NOT_READY, result.getValidationStatus());
    }

    @Test
    public void testValidCanonicalPayload() {
        String json = "{\"payloadVersion\":\"TSE_V2_ANSWER_PAYLOAD_V1\",\"attemptId\":\"123\",\"paperId\":1,\"answers\":[{\"questionId\":1,\"selectedOptionId\":2}]}";
        V2AnswerPayloadContractValidationResult result = validator.validate(json);
        assertTrue(result.isSuccess());
        assertTrue(result.isValid());
        assertEquals(V2AnswerPayloadContract.STATUS_VALID, result.getValidationStatus());
        assertEquals(Integer.valueOf(1), result.getAnswerCount());
    }

    @Test
    public void testMalformedJsonRejects() {
        String json = "{invalid_json";
        V2AnswerPayloadContractValidationResult result = validator.validate(json);
        assertFalse(result.isValid());
        assertEquals(V2AnswerPayloadContract.ERROR_MALFORMED, result.getErrorCode());
    }

    @Test
    public void testMissingPayloadVersionRejects() {
        String json = "{\"attemptId\":\"123\",\"paperId\":1,\"answers\":[]}";
        V2AnswerPayloadContractValidationResult result = validator.validate(json);
        assertFalse(result.isValid());
        assertEquals(V2AnswerPayloadContract.ERROR_MISSING_REQUIRED_FIELD, result.getErrorCode());
    }

    @Test
    public void testUnsupportedPayloadVersionRejects() {
        String json = "{\"payloadVersion\":\"V2\",\"attemptId\":\"123\",\"paperId\":1,\"answers\":[]}";
        V2AnswerPayloadContractValidationResult result = validator.validate(json);
        assertFalse(result.isValid());
        assertEquals(V2AnswerPayloadContract.ERROR_UNSUPPORTED_VERSION, result.getErrorCode());
    }

    @Test
    public void testMissingAttemptIdRejects() {
        String json = "{\"payloadVersion\":\"TSE_V2_ANSWER_PAYLOAD_V1\",\"paperId\":1,\"answers\":[]}";
        V2AnswerPayloadContractValidationResult result = validator.validate(json);
        assertFalse(result.isValid());
        assertEquals(V2AnswerPayloadContract.ERROR_MISSING_REQUIRED_FIELD, result.getErrorCode());
    }

    @Test
    public void testMissingPaperIdRejects() {
        String json = "{\"payloadVersion\":\"TSE_V2_ANSWER_PAYLOAD_V1\",\"attemptId\":\"123\",\"answers\":[]}";
        V2AnswerPayloadContractValidationResult result = validator.validate(json);
        assertFalse(result.isValid());
        assertEquals(V2AnswerPayloadContract.ERROR_MISSING_REQUIRED_FIELD, result.getErrorCode());
    }

    @Test
    public void testMissingAnswersRejects() {
        String json = "{\"payloadVersion\":\"TSE_V2_ANSWER_PAYLOAD_V1\",\"attemptId\":\"123\",\"paperId\":1}";
        V2AnswerPayloadContractValidationResult result = validator.validate(json);
        assertFalse(result.isValid());
        assertEquals(V2AnswerPayloadContract.ERROR_MISSING_REQUIRED_FIELD, result.getErrorCode());
    }

    @Test
    public void testDuplicateQuestionIdRejects() {
        String json = "{\"payloadVersion\":\"TSE_V2_ANSWER_PAYLOAD_V1\",\"attemptId\":\"123\",\"paperId\":1,\"answers\":[{\"questionId\":1,\"selectedOptionId\":2}, {\"questionId\":1,\"selectedOptionId\":3}]}";
        V2AnswerPayloadContractValidationResult result = validator.validate(json);
        assertFalse(result.isValid());
        assertEquals(V2AnswerPayloadContract.ERROR_DUPLICATE_QUESTION, result.getErrorCode());
    }
}
