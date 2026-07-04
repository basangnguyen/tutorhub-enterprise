package com.mycompany.tutorhub_enterprise.client.ai.agent;

import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AgentsMdLoader {

    private static final String AGENTS_FILE_NAME = "AGENTS.md";
    private static final int MAX_FILE_CHARS = 8000;
    private static final int MAX_TOTAL_CHARS = 12000;

    private AgentsMdLoader() {
    }

    public static ProjectInstructionSnapshot loadForWorkspace(WorkspaceBoundary boundary) {
        if (boundary == null) {
            return ProjectInstructionSnapshot.empty();
        }
        List<ProjectInstruction> instructions = new ArrayList<>();
        Path candidate = boundary.getWorkspaceRoot().resolve(AGENTS_FILE_NAME);
        if (Files.isRegularFile(candidate)) {
            try {
                Path safePath = boundary.validateExistingPath(candidate);
                String content = Files.readString(safePath, StandardCharsets.UTF_8);
                instructions.add(new ProjectInstruction(
                        boundary.relativize(safePath),
                        limit(clean(content), MAX_FILE_CHARS),
                        content.length() > MAX_FILE_CHARS));
            } catch (IOException | SecurityException ignored) {
                // Project instructions are helpful context, but agent mode must still run if they cannot be read.
            }
        }
        return new ProjectInstructionSnapshot(instructions, buildContext(instructions));
    }

    private static String buildContext(List<ProjectInstruction> instructions) {
        if (instructions == null || instructions.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (ProjectInstruction instruction : instructions) {
            if (instruction == null || instruction.getContent().isBlank()) {
                continue;
            }
            String block = "File: " + instruction.getRelativePath() + "\n"
                    + instruction.getContent()
                    + (instruction.isTruncated() ? "\n... AGENTS.md truncated ..." : "")
                    + "\n";
            if (builder.length() + block.length() > MAX_TOTAL_CHARS) {
                break;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(block);
        }
        return builder.toString().trim();
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\u0000", "").replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    private static String limit(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(0, maxChars)).trim();
    }

    public static final class ProjectInstructionSnapshot {
        private final List<ProjectInstruction> instructions;
        private final String context;

        private ProjectInstructionSnapshot(List<ProjectInstruction> instructions, String context) {
            this.instructions = instructions == null ? new ArrayList<>() : new ArrayList<>(instructions);
            this.context = context == null ? "" : context;
        }

        public static ProjectInstructionSnapshot empty() {
            return new ProjectInstructionSnapshot(new ArrayList<>(), "");
        }

        public List<ProjectInstruction> getInstructions() {
            return Collections.unmodifiableList(instructions);
        }

        public String getContext() {
            return context;
        }

        public int getCount() {
            return instructions.size();
        }

        public boolean hasInstructions() {
            return !context.isBlank();
        }
    }

    public static final class ProjectInstruction {
        private final String relativePath;
        private final String content;
        private final boolean truncated;

        private ProjectInstruction(String relativePath, String content, boolean truncated) {
            this.relativePath = relativePath == null ? AGENTS_FILE_NAME : relativePath;
            this.content = content == null ? "" : content;
            this.truncated = truncated;
        }

        public String getRelativePath() {
            return relativePath;
        }

        public String getContent() {
            return content;
        }

        public boolean isTruncated() {
            return truncated;
        }
    }
}
