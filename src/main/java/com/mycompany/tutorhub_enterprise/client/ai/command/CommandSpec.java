package com.mycompany.tutorhub_enterprise.client.ai.command;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class CommandSpec {

    private final String id;
    private final List<String> tokens;
    private final String workingDirectory;
    private final int timeoutSeconds;
    private final String reason;
    private final Instant createdAt;

    public CommandSpec(List<String> tokens, String workingDirectory, int timeoutSeconds, String reason) {
        this("cmd-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                tokens, workingDirectory, timeoutSeconds, reason, Instant.now());
    }

    public CommandSpec(String id, List<String> tokens, String workingDirectory,
                       int timeoutSeconds, String reason, Instant createdAt) {
        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalArgumentException("Command tokens are required");
        }
        this.id = id == null || id.isBlank()
                ? "cmd-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                : id;
        this.tokens = Collections.unmodifiableList(new ArrayList<>(tokens));
        this.workingDirectory = workingDirectory == null || workingDirectory.isBlank() ? "." : workingDirectory.trim();
        this.timeoutSeconds = Math.max(5, Math.min(300, timeoutSeconds));
        this.reason = reason == null ? "" : reason.trim();
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public String getId() {
        return id;
    }

    public List<String> getTokens() {
        return tokens;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCommandLine() {
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(quoteIfNeeded(token));
        }
        return builder.toString();
    }

    private String quoteIfNeeded(String token) {
        if (token == null) {
            return "";
        }
        if (token.indexOf(' ') < 0 && token.indexOf('\t') < 0) {
            return token;
        }
        return "\"" + token.replace("\"", "\\\"") + "\"";
    }
}
