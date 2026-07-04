package com.mycompany.tutorhub_enterprise.client.ai;

import com.google.gson.JsonObject;
import com.mycompany.tutorhub_enterprise.client.ai.agent.AgentToolInvocation;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;
import com.mycompany.tutorhub_enterprise.client.ai.ui.ToolCallLogView;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolCallLogViewTest {

    @Test
    void serializesToolInvocationForWebView() {
        AgentToolInvocation invocation = new AgentToolInvocation(
                2,
                ToolCallRequest.of("read_file", Map.of("path", "README.md")),
                ToolCallResult.success("1 | TutorHub").withMetadata("lines", "1"));

        JsonObject json = ToolCallLogView.toJson(invocation);

        assertEquals(2, json.get("turn").getAsInt());
        assertEquals("read_file", json.get("tool").getAsString());
        assertTrue(json.get("success").getAsBoolean());
        assertEquals("README.md", json.getAsJsonObject("arguments").get("path").getAsString());
        assertEquals("1", json.getAsJsonObject("metadata").get("lines").getAsString());
        assertFalse(json.get("truncated").getAsBoolean());
    }
}
