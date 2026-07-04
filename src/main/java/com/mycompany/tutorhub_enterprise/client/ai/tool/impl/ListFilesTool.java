package com.mycompany.tutorhub_enterprise.client.ai.tool.impl;

import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;
import com.mycompany.tutorhub_enterprise.client.ai.tool.AgentTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ListFilesTool implements AgentTool {

    private static final int DEFAULT_DEPTH = 2;
    private static final int MAX_DEPTH = 5;
    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 500;
    private static final Set<String> SKIP_DIRS = Set.of(
            ".git",
            ".idea",
            ".vscode",
            "node_modules",
            "target",
            "build",
            "dist",
            "__pycache__"
    );

    private final WorkspaceBoundary boundary;

    public ListFilesTool(WorkspaceBoundary boundary) {
        this.boundary = boundary;
    }

    @Override
    public String name() {
        return "list_files";
    }

    @Override
    public String description() {
        return "List files and folders inside the selected workspace. Read-only.";
    }

    @Override
    public Map<String, String> parameters() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("path", "Optional relative path. Defaults to workspace root.");
        params.put("depth", "Optional depth from 1 to 5. Defaults to 2.");
        params.put("limit", "Optional max item count from 1 to 500. Defaults to 200.");
        return params;
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        try {
            Path start = boundary.resolveDirectoryOrRoot(request.getArgument("path", "."));
            int depth = request.getIntArgument("depth", DEFAULT_DEPTH, 1, MAX_DEPTH);
            int limit = request.getIntArgument("limit", DEFAULT_LIMIT, 1, MAX_LIMIT);
            List<Path> files = new ArrayList<>();
            collect(start, depth, limit + 1, files);
            files.sort(Comparator.comparing((Path path) -> !Files.isDirectory(path))
                    .thenComparing(path -> boundary.relativize(path).toLowerCase(Locale.ROOT)));

            boolean truncated = files.size() > limit;
            if (truncated) {
                files = new ArrayList<>(files.subList(0, limit));
            }

            StringBuilder output = new StringBuilder();
            output.append("Workspace: ").append(boundary.getWorkspaceRoot()).append('\n');
            output.append("Listing: ").append(boundary.relativize(start)).append('\n');
            output.append("Depth: ").append(depth).append(", limit: ").append(limit).append('\n');
            for (Path path : files) {
                output.append(formatEntry(path)).append('\n');
            }
            if (truncated) {
                output.append("... truncated after ").append(limit).append(" items\n");
            }
            if (files.isEmpty()) {
                output.append("(empty)\n");
            }
            return ToolCallResult.success(output.toString())
                    .withMetadata("items", String.valueOf(files.size()))
                    .withMetadata("truncated", String.valueOf(truncated));
        } catch (SecurityException ex) {
            return ToolCallResult.failure("Access denied: " + ex.getMessage());
        } catch (Exception ex) {
            return ToolCallResult.failure(ex.getMessage());
        }
    }

    private void collect(Path start, int depth, int limit, List<Path> output) throws IOException {
        Files.walkFileTree(start, java.util.EnumSet.noneOf(java.nio.file.FileVisitOption.class), depth,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (!dir.equals(start)) {
                            if (shouldSkip(dir)) {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                            output.add(dir);
                            if (output.size() >= limit) {
                                return FileVisitResult.TERMINATE;
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (!shouldSkip(file)) {
                            output.add(file);
                        }
                        return output.size() >= limit
                                ? FileVisitResult.TERMINATE
                                : FileVisitResult.CONTINUE;
                    }
                });
    }

    private boolean shouldSkip(Path path) {
        if (Files.isSymbolicLink(path) || !boundary.canRead(path)) {
            return true;
        }
        String name = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        return SKIP_DIRS.contains(name);
    }

    private String formatEntry(Path path) {
        String type = Files.isDirectory(path) ? "[D]" : "[F]";
        String relative = boundary.relativize(path);
        if (Files.isDirectory(path) && !relative.endsWith("/")) {
            relative += "/";
        }
        if (Files.isRegularFile(path)) {
            try {
                return type + " " + relative + " (" + Files.size(path) + " bytes)";
            } catch (IOException ignored) {
                return type + " " + relative;
            }
        }
        return type + " " + relative;
    }
}
