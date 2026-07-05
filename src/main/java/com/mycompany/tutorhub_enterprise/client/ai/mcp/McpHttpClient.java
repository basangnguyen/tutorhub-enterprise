package com.mycompany.tutorhub_enterprise.client.ai.mcp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class McpHttpClient {

    private static final Gson GSON = new Gson();

    private final AtomicLong idCounter = new AtomicLong(1);
    private final Duration timeout;

    public McpHttpClient() {
        this(Duration.ofSeconds(20));
    }

    public McpHttpClient(Duration timeout) {
        this.timeout = timeout == null || timeout.isNegative() || timeout.isZero()
                ? Duration.ofSeconds(20)
                : timeout;
    }

    public List<McpToolDescriptor> listTools(McpServerConfig server) throws Exception {
        JsonObject response = postJsonRpc(server, "tools/list", new JsonObject());
        JsonObject result = response.getAsJsonObject("result");
        if (result == null) {
            throw new IllegalStateException("MCP server returned no result for tools/list");
        }
        JsonArray tools = result.getAsJsonArray("tools");
        List<McpToolDescriptor> descriptors = new ArrayList<>();
        if (tools == null) {
            return descriptors;
        }
        for (JsonElement element : tools) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject tool = element.getAsJsonObject();
            descriptors.add(new McpToolDescriptor(
                    server.getName(),
                    getString(tool, "name"),
                    getString(tool, "description")));
        }
        return descriptors;
    }

    public McpToolCallResult callTool(McpServerConfig server, McpToolCallSpec spec) throws Exception {
        if (server == null) {
            return McpToolCallResult.failure(spec, "MCP server is required");
        }
        if (spec == null) {
            return McpToolCallResult.failure(null, "MCP tool call spec is required");
        }
        JsonObject params = new JsonObject();
        params.addProperty("name", spec.getToolName());
        params.add("arguments", spec.getArguments());
        JsonObject response = postJsonRpc(server, "tools/call", params);
        JsonObject result = response.getAsJsonObject("result");
        if (result == null) {
            return McpToolCallResult.failure(spec, "MCP server returned no result for tools/call");
        }
        return McpToolCallResult.success(spec, formatToolCallResult(result));
    }

    private String formatToolCallResult(JsonObject result) {
        if (result == null) {
            return "";
        }
        JsonArray content = result.getAsJsonArray("content");
        if (content == null || content.size() == 0) {
            return GSON.toJson(result);
        }
        StringBuilder output = new StringBuilder();
        for (JsonElement item : content) {
            if (item == null || !item.isJsonObject()) {
                continue;
            }
            JsonObject node = item.getAsJsonObject();
            String type = getString(node, "type");
            if ("text".equals(type)) {
                if (output.length() > 0) {
                    output.append('\n');
                }
                output.append(getString(node, "text"));
            } else {
                if (output.length() > 0) {
                    output.append('\n');
                }
                output.append(GSON.toJson(node));
            }
        }
        return output.toString().trim();
    }

    private JsonObject postJsonRpc(McpServerConfig server, String method, JsonObject params) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(server.getEndpointUrl()).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout((int) timeout.toMillis());
            conn.setReadTimeout((int) timeout.toMillis());
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "application/json");

            JsonObject request = new JsonObject();
            request.addProperty("jsonrpc", "2.0");
            request.addProperty("id", idCounter.getAndIncrement());
            request.addProperty("method", method);
            request.add("params", params == null ? new JsonObject() : params);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(GSON.toJson(request).getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            String body = readBody(status >= 200 && status < 300
                    ? conn.getInputStream()
                    : conn.getErrorStream());
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("MCP server HTTP " + status + ": " + body);
            }
            JsonObject response = JsonParser.parseString(body).getAsJsonObject();
            JsonObject error = response.getAsJsonObject("error");
            if (error != null) {
                throw new IllegalStateException("MCP JSON-RPC error: " + getString(error, "message"));
            }
            return response;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String readBody(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private String getString(JsonObject obj, String key) {
        JsonElement element = obj == null ? null : obj.get(key);
        if (element == null || element.isJsonNull()) {
            return "";
        }
        return element.getAsString();
    }
}
