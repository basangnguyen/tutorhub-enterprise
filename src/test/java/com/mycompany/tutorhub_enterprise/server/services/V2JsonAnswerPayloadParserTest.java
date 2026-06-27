package com.mycompany.tutorhub_enterprise.server.services;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class V2JsonAnswerPayloadParserTest {

    private V2JsonAnswerPayloadParser parser;

    @BeforeEach
    public void setUp() {
        parser = new V2JsonAnswerPayloadParser();
        System.setProperty("tse.v2.jsonAnswerPayloadParser.enabled", "true");
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.jsonAnswerPayloadParser.enabled");
    }

    @Test
    public void testFlagOffReturnsNull() {
        System.setProperty("tse.v2.jsonAnswerPayloadParser.enabled", "false");
        String json = "{\"payloadVersion\":\"TSE_V2_ANSWER_PAYLOAD_V1\",\"attemptId\":\"123\",\"paperId\":1,\"answers\":[{\"questionId\":1,\"selectedOptionId\":2}]}";
        assertNull(parser.extractAnswers(json));
    }

    @Test
    public void testValidCanonicalPayloadReturnsInternalMap() {
        String json = "{\"payloadVersion\":\"TSE_V2_ANSWER_PAYLOAD_V1\",\"attemptId\":\"123\",\"paperId\":1,\"answers\":[{\"questionId\":1,\"selectedOptionId\":2}]}";
        Map<Long, Long> result = parser.extractAnswers(json);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Long.valueOf(2), result.get(1L));
    }

    @Test
    public void testMalformedJsonReturnsNull() {
        assertNull(parser.extractAnswers("{malformed"));
    }

    @Test
    public void testMissingSchemaReturnsNull() {
        String json = "{\"attemptId\":\"123\",\"paperId\":1,\"answers\":[{\"questionId\":1,\"selectedOptionId\":2}]}";
        assertNull(parser.extractAnswers(json));
    }

    @Test
    public void testDuplicateQuestionIdReturnsNull() {
        String json = "{\"payloadVersion\":\"TSE_V2_ANSWER_PAYLOAD_V1\",\"attemptId\":\"123\",\"paperId\":1,\"answers\":[{\"questionId\":1,\"selectedOptionId\":2}, {\"questionId\":1,\"selectedOptionId\":3}]}";
        assertNull(parser.extractAnswers(json));
    }
}
