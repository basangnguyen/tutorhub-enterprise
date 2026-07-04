package com.mycompany.tutorhub_enterprise.client.ai.tool.impl;

import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;
import com.mycompany.tutorhub_enterprise.client.ai.tool.AgentTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class SearchTextTool implements AgentTool {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;
    private static final int MAX_FILE_BYTES = 1024 * 1024;
    private static final int MAX_LINE_LENGTH = 260;
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

    public SearchTextTool(WorkspaceBoundary boundary) {
        this.boundary = boundary;
    }

    @Override
    public String name() {
        return "search_text";
    }

    @Override
    public String description() {
        return "Search text or regex matches inside UTF-8 files in the selected workspace. Read-only.";
    }

    @Override
    public Map<String, String> parameters() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("query", "Required search text or regex.");
        params.put("path", "Optional relative path to search. Defaults to workspace root.");
        params.put("regex", "Optional true/false. Defaults to false.");
        params.put("caseSensitive", "Optional true/false. Defaults to false.");
        params.put("limit", "Optional match count from 1 to 500. Defaults to 100.");
        return params;
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        String query = request.getArgument("query", "").trim();
        if (query.isEmpty()) {
            return ToolCallResult.failure("query is required");
        }
        try {
            Path start = boundary.resolveRequiredPath(request.getArgument("path", "."));
            int limit = request.getIntArgument("limit", DEFAULT_LIMIT, 1, MAX_LIMIT);
            boolean regex = request.getBooleanArgument("regex", false);
            boolean caseSensitive = request.getBooleanArgument("caseSensitive", false);
            SearchMatcher matcher = SearchMatcher.create(query, regex, caseSensitive);
            SearchState state = new SearchState(limit);

            if (Files.isRegularFile(start)) {
                searchFile(start, matcher, state);
            } else if (Files.isDirectory(start)) {
                searchDirectory(start, matcher, state);
            } else {
                return ToolCallResult.failure("Path is not searchable: " + boundary.relativize(start));
            }

            StringBuilder output = new StringBuilder();
            output.append("Query: ").append(query).append('\n');
            output.append("Scope: ").append(boundary.relativize(start)).append('\n');
            output.append("Regex: ").append(regex).append(", caseSensitive: ").append(caseSensitive).append('\n');
            if (state.matches.length() == 0) {
                output.append("(no matches)\n");
            } else {
                output.append(state.matches);
            }
            if (state.truncated) {
                output.append("... truncated after ").append(limit).append(" matches\n");
            }
            return ToolCallResult.success(output.toString())
                    .withMetadata("matches", String.valueOf(state.count))
                    .withMetadata("truncated", String.valueOf(state.truncated));
        } catch (SecurityException ex) {
            return ToolCallResult.failure("Access denied: " + ex.getMessage());
        } catch (PatternSyntaxException ex) {
            return ToolCallResult.failure("Invalid regex: " + ex.getMessage());
        } catch (Exception ex) {
            return ToolCallResult.failure(ex.getMessage());
        }
    }

    private void searchDirectory(Path start, SearchMatcher matcher, SearchState state) throws IOException {
        Files.walkFileTree(start, java.util.EnumSet.noneOf(java.nio.file.FileVisitOption.class), Integer.MAX_VALUE,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (!dir.equals(start) && shouldSkip(dir)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return state.truncated ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (!shouldSkip(file)) {
                            searchFile(file, matcher, state);
                        }
                        return state.truncated ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
                    }
                });
    }

    private void searchFile(Path file, SearchMatcher matcher, SearchState state) throws IOException {
        if (state.truncated || !Files.isRegularFile(file) || Files.size(file) > MAX_FILE_BYTES) {
            return;
        }
        int lineNumber = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (matcher.matches(line)) {
                    state.add(boundary.relativize(file), lineNumber, line);
                    if (state.truncated) {
                        return;
                    }
                }
            }
        } catch (MalformedInputException ignored) {
            // Binary or non-UTF-8 files are ignored for read-only code search.
        }
    }

    private boolean shouldSkip(Path path) {
        if (Files.isSymbolicLink(path) || !boundary.canRead(path)) {
            return true;
        }
        String name = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        return SKIP_DIRS.contains(name);
    }

    private static String truncate(String value) {
        String compact = value.replace('\t', ' ').trim();
        if (compact.length() <= MAX_LINE_LENGTH) {
            return compact;
        }
        return compact.substring(0, MAX_LINE_LENGTH) + "...";
    }

    private interface SearchMatcher {
        boolean matches(String line);

        static SearchMatcher create(String query, boolean regex, boolean caseSensitive) {
            if (regex) {
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
                Pattern pattern = Pattern.compile(query, flags);
                return line -> pattern.matcher(line).find();
            }
            String needle = caseSensitive ? query : query.toLowerCase(Locale.ROOT);
            return line -> {
                String haystack = caseSensitive ? line : line.toLowerCase(Locale.ROOT);
                return haystack.contains(needle);
            };
        }
    }

    private static final class SearchState {
        private final int limit;
        private final StringBuilder matches = new StringBuilder();
        private int count;
        private boolean truncated;

        private SearchState(int limit) {
            this.limit = limit;
        }

        private void add(String file, int lineNumber, String line) {
            if (count >= limit) {
                truncated = true;
                return;
            }
            matches.append(file)
                    .append(':')
                    .append(lineNumber)
                    .append(": ")
                    .append(truncate(line))
                    .append('\n');
            count++;
            if (count >= limit) {
                truncated = true;
            }
        }
    }
}
