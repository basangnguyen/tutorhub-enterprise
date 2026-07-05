package com.mycompany.tutorhub_enterprise.client.ai.mcp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpHttpClientTest {

    @Test
    void listsToolsFromHttpJsonRpcServer() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> requestBody = new AtomicReference<>("");
        server.createContext("/mcp", exchange -> {
            requestBody.set(readRequest(exchange));
            byte[] response = ("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"tools\":["
                    + "{\"name\":\"search_docs\",\"description\":\"Search docs\"}]}}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            McpHttpClient client = new McpHttpClient();
            McpServerConfig config = new McpServerConfig("docs",
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/mcp");

            List<McpToolDescriptor> tools = client.listTools(config);

            assertEquals(1, tools.size());
            assertEquals("docs", tools.get(0).getServerName());
            assertEquals("search_docs", tools.get(0).getName());
            assertTrue(requestBody.get().contains("\"method\":\"tools/list\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void callsToolThroughHttpJsonRpcServer() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> requestBody = new AtomicReference<>("");
        server.createContext("/mcp", exchange -> {
            requestBody.set(readRequest(exchange));
            byte[] response = ("{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":{\"content\":["
                    + "{\"type\":\"text\",\"text\":\"Found 3 notes\"}]}}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            McpHttpClient client = new McpHttpClient();
            McpServerConfig config = new McpServerConfig("docs",
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/mcp");

            McpToolCallResult result = client.callTool(config,
                    new McpToolCallSpec("docs", "search_docs", "{\"query\":\"java\"}", "Search docs"));

            assertTrue(result.isSuccess());
            assertEquals("Found 3 notes", result.getOutput());
            assertTrue(requestBody.get().contains("\"method\":\"tools/call\""));
            assertTrue(requestBody.get().contains("\"name\":\"search_docs\""));
            assertTrue(requestBody.get().contains("\"query\":\"java\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void parsesEnvironmentStyleEndpointList() {
        McpServerRegistry registry = McpServerRegistry.parse("docs=http://localhost:3001/mcp;http://localhost:3002/mcp");

        assertEquals(2, registry.getServers().size());
        assertEquals("docs", registry.getServers().get(0).getName());
        assertEquals("mcp-2", registry.getServers().get(1).getName());
    }

    private static String readRequest(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }
}
