package com.mycompany.tutorhub_enterprise.client.ai.mcp;

import com.mycompany.tutorhub_enterprise.client.ai.permission.AuditLog;
import com.mycompany.tutorhub_enterprise.client.ai.permission.PermissionPolicy;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;
import com.mycompany.tutorhub_enterprise.client.ai.tool.impl.ProposeMcpToolCallTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.impl.RunMcpToolCallTool;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolCallToolsTest {

    @TempDir
    Path tempDir;

    @Test
    void proposesAndRunsMcpToolOnlyAfterApproval() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> requestBody = new AtomicReference<>("");
        server.createContext("/mcp", exchange -> {
            requestBody.set(readRequest(exchange));
            byte[] response = ("{\"jsonrpc\":\"2.0\",\"id\":3,\"result\":{\"content\":["
                    + "{\"type\":\"text\",\"text\":\"Created task\"}]}}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            McpServerRegistry registry = McpServerRegistry.parse("tasks=http://127.0.0.1:"
                    + server.getAddress().getPort() + "/mcp");
            PendingMcpToolCallStore store = new PendingMcpToolCallStore();
            ProposeMcpToolCallTool proposeTool = new ProposeMcpToolCallTool(registry, store);

            ToolCallResult proposal = proposeTool.execute(ToolCallRequest.of("propose_mcp_tool_call", Map.of(
                    "serverName", "tasks",
                    "toolName", "create_task",
                    "argumentsJson", "{\"title\":\"Review phase 10.1\"}",
                    "reason", "Create a tracking task"
            )));

            assertTrue(proposal.isSuccess());
            String callId = proposal.getMetadata().get("mcpCallId");
            assertTrue(store.find(callId).isPresent());

            RunMcpToolCallTool runTool = new RunMcpToolCallTool(
                    registry,
                    store,
                    PermissionPolicy.phase101Defaults(),
                    new AuditLog(tempDir.resolve("audit.log")),
                    new McpHttpClient());
            ToolCallResult blocked = runTool.execute(ToolCallRequest.of("run_mcp_tool_call", Map.of(
                    "mcpCallId", callId
            )));

            assertFalse(blocked.isSuccess());
            assertTrue(blocked.getError().contains("User approval is required"));
            assertTrue(store.find(callId).isPresent());

            ToolCallResult approved = runTool.execute(ToolCallRequest.of("run_mcp_tool_call", Map.of(
                    "mcpCallId", callId,
                    "approved", "true"
            )));

            assertTrue(approved.isSuccess());
            assertEquals("Created task", approved.getOutput());
            assertEquals("completed", approved.getMetadata().get("status"));
            assertTrue(store.find(callId).isEmpty());
            assertTrue(requestBody.get().contains("\"method\":\"tools/call\""));
            assertTrue(requestBody.get().contains("\"name\":\"create_task\""));
            assertTrue(requestBody.get().contains("\"title\":\"Review phase 10.1\""));
        } finally {
            server.stop(0);
        }
    }

    private static String readRequest(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }
}
