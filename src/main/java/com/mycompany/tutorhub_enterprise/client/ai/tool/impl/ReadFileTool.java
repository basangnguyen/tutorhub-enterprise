package com.mycompany.tutorhub_enterprise.client.ai.tool.impl;

import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;
import com.mycompany.tutorhub_enterprise.client.ai.tool.AgentTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ReadFileTool implements AgentTool {

    private static final int DEFAULT_MAX_LINES = 240;
    private static final int MAX_LINES = 500;
    private static final int MAX_PREVIEW_BYTES = 4096;

    private final WorkspaceBoundary boundary;

    public ReadFileTool(WorkspaceBoundary boundary) {
        this.boundary = boundary;
    }

    @Override
    public String name() {
        return "read_file";
    }

    @Override
    public String description() {
        return "Read a UTF-8 text file inside the selected workspace with line numbers. Read-only.";
    }

    @Override
    public Map<String, String> parameters() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("path", "Required relative path to a text file.");
        params.put("startLine", "Optional 1-based line number. Defaults to 1.");
        params.put("maxLines", "Optional line count from 1 to 500. Defaults to 240.");
        return params;
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        try {
            Path file = boundary.resolveRequiredPath(request.getArgument("path"));
            if (!Files.isRegularFile(file)) {
                return ToolCallResult.failure("Path is not a regular file: " + boundary.relativize(file));
            }
            if (looksBinary(file)) {
                return ToolCallResult.failure("Refusing to read binary file: " + boundary.relativize(file));
            }

            int startLine = request.getIntArgument("startLine", 1, 1, Integer.MAX_VALUE);
            int maxLines = request.getIntArgument("maxLines", DEFAULT_MAX_LINES, 1, MAX_LINES);
            return readText(file, startLine, maxLines);
        } catch (SecurityException ex) {
            return ToolCallResult.failure("Access denied: " + ex.getMessage());
        } catch (MalformedInputException ex) {
            return ToolCallResult.failure("File is not valid UTF-8 text");
        } catch (Exception ex) {
            return ToolCallResult.failure(ex.getMessage());
        }
    }

    private ToolCallResult readText(Path file, int startLine, int maxLines) throws IOException {
        StringBuilder output = new StringBuilder();
        output.append("File: ").append(boundary.relativize(file)).append('\n');
        output.append("Start line: ").append(startLine).append(", max lines: ").append(maxLines).append('\n');

        int currentLine = 0;
        int emitted = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                currentLine++;
                if (currentLine < startLine) {
                    continue;
                }
                if (emitted >= maxLines) {
                    output.append("... truncated after ").append(maxLines).append(" lines\n");
                    break;
                }
                output.append(currentLine).append(" | ").append(line).append('\n');
                emitted++;
            }
        }
        if (emitted == 0) {
            output.append("(no lines at or after requested start line)\n");
        }
        return ToolCallResult.success(output.toString())
                .withMetadata("lines", String.valueOf(emitted))
                .withMetadata("file", boundary.relativize(file));
    }

    private boolean looksBinary(Path file) throws IOException {
        byte[] bytes;
        try (var in = Files.newInputStream(file)) {
            bytes = in.readNBytes(MAX_PREVIEW_BYTES);
        }
        for (byte value : bytes) {
            int unsigned = value & 0xFF;
            if (unsigned == 0) {
                return true;
            }
        }
        return false;
    }
}
