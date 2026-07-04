package com.mycompany.tutorhub_enterprise.client.ai.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mycompany.tutorhub_enterprise.client.ai.agent.AgentToolInvocation;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;

import java.util.List;
import java.util.Map;

public final class ToolCallLogView {

    private static final int MAX_OUTPUT_CHARS = 5000;

    private ToolCallLogView() {
    }

    public static JsonObject toJson(AgentToolInvocation invocation) {
        JsonObject json = new JsonObject();
        if (invocation == null) {
            json.addProperty("turn", 0);
            json.addProperty("tool", "");
            json.addProperty("success", false);
            json.addProperty("error", "Missing tool invocation");
            return json;
        }

        ToolCallResult result = invocation.getResult();
        json.addProperty("turn", invocation.getTurnIndex());
        json.addProperty("tool", invocation.getRequest().getToolName());
        json.add("arguments", toJsonObject(invocation.getRequest().getArguments()));
        json.addProperty("success", result != null && result.isSuccess());

        String output = result == null ? "" : result.getOutput();
        String error = result == null ? "Missing tool result" : result.getError();
        json.addProperty("output", truncate(output));
        json.addProperty("error", error == null ? "" : error);
        json.addProperty("truncated", output != null && output.length() > MAX_OUTPUT_CHARS);
        json.add("metadata", result == null ? new JsonObject() : toJsonObject(result.getMetadata()));
        return json;
    }

    public static JsonArray toJsonArray(List<AgentToolInvocation> invocations) {
        JsonArray array = new JsonArray();
        if (invocations == null) {
            return array;
        }
        for (AgentToolInvocation invocation : invocations) {
            array.add(toJson(invocation));
        }
        return array;
    }

    private static JsonObject toJsonObject(Map<String, String> values) {
        JsonObject object = new JsonObject();
        if (values == null) {
            return object;
        }
        values.forEach((key, value) -> {
            if (key != null) {
                object.addProperty(key, value == null ? "" : value);
            }
        });
        return object;
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= MAX_OUTPUT_CHARS) {
            return value == null ? "" : value;
        }
        return value.substring(0, MAX_OUTPUT_CHARS) + "\n... tool output truncated ...";
    }
}
