package com.mycompany.tutorhub_enterprise.client.ai.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentModelResponseParserTest {

    @Test
    void parsesToolCallFromFencedJson() {
        AgentModelResponse response = AgentModelResponseParser.parse("""
                ```json
                {"type":"tool_call","tool":"read_file","arguments":{"path":"src/App.java","maxLines":20}}
                ```
                """);

        assertTrue(response.isToolCall());
        assertEquals("read_file", response.getToolCall().getToolName());
        assertEquals("src/App.java", response.getToolCall().getArgument("path"));
        assertEquals("20", response.getToolCall().getArgument("maxLines"));
    }

    @Test
    void parsesFinalAnswerFromJson() {
        AgentModelResponse response = AgentModelResponseParser.parse(
                "{\"type\":\"final\",\"answer\":\"Da kiem tra xong.\"}");

        assertFalse(response.isToolCall());
        assertEquals("Da kiem tra xong.", response.getFinalAnswer());
    }

    @Test
    void convertsPrimitiveArgumentsToStrings() {
        AgentModelResponse response = AgentModelResponseParser.parse(
                "{\"type\":\"tool_call\",\"tool\":\"search_text\",\"arguments\":{\"query\":\"TutorHub\",\"limit\":5,\"regex\":false}}");

        assertTrue(response.isToolCall());
        assertEquals("5", response.getToolCall().getArgument("limit"));
        assertEquals("false", response.getToolCall().getArgument("regex"));
    }

    @Test
    void treatsPlainTextAsFinalAnswer() {
        AgentModelResponse response = AgentModelResponseParser.parse("Tra loi thong thuong.");

        assertFalse(response.isToolCall());
        assertEquals("Tra loi thong thuong.", response.getFinalAnswer());
    }
}
